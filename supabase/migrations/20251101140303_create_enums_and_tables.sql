-- ==============================================================================
-- migration: create_enums_and_tables
-- ==============================================================================
-- purpose: tworzy podstawowy schemat bazy danych - enumy, tabele, indeksy, rls
-- affected tables: users, games, moves
-- affected schemas: public
-- 
-- szczegóły:
-- - tworzy tabele users (goście + zarejestrowani z integracją Supabase Auth), games, moves
-- - używa VARCHAR z CHECK constraints dla enumów (zamiast PostgreSQL ENUM)
-- - dodaje indeksy dla wydajnych zapytań
-- - włącza row level security (rls)
-- - konfiguruje rls policies dla bezpiecznego dostępu do danych
-- ==============================================================================

-- ==============================================================================
-- 2. TABELA: users - użytkownicy (goście i zarejestrowani)
-- ==============================================================================

create table if not exists public.users (
    id bigserial primary key,
    -- integracja z supabase auth: dla zarejestrowanych użytkowników
    auth_user_id uuid references auth.users(id) on delete cascade,
    username varchar(50),
    -- typ użytkownika: gość (true) lub zarejestrowany (false)
    is_guest boolean not null default false,
    -- identyfikacja gości przez ip
    ip_address inet,
    -- statystyki gracza
    total_points bigint not null default 0,
    games_played integer not null default 0,
    games_won integer not null default 0,
    last_seen_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now()
);

comment on table public.users is 'użytkownicy: goście (is_guest=true, identyfikacja przez ip) lub zarejestrowani (auth_user_id→auth.users)';
comment on column public.users.auth_user_id is 'fk do auth.users.id - tylko dla zarejestrowanych';
comment on column public.users.username is 'nazwa użytkownika - tylko dla zarejestrowanych';
comment on column public.users.is_guest is 'true=dla gości, false=dla zarejestrowanych';
comment on column public.users.ip_address is 'adres ip gościa - tylko dla gości';
comment on column public.users.total_points is 'suma punktów gracza (używane do rankingu)';
comment on column public.users.last_seen_at is 'ostatnia aktywność (używane do matchmakingu)';

-- constraint: zapewnia poprawność danych dla gości i zarejestrowanych
alter table public.users
    add constraint users_registered_check
    check (
        -- goście: is_guest=true AND auth_user_id null AND username null AND ip_address not null
        (is_guest = true AND auth_user_id IS NULL AND username IS NULL AND ip_address IS NOT NULL)
        OR
        -- zarejestrowani: is_guest=false AND auth_user_id not null AND username not null AND ip_address null
        (is_guest = false AND auth_user_id IS NOT NULL AND username IS NOT NULL AND ip_address IS NULL)
    );

-- unique constraints: unikalność username i auth_user_id
create unique index idx_users_auth_user_id on public.users (auth_user_id) where auth_user_id is not null;
create unique index idx_users_username on public.users (username) where username is not null;

-- indeksy dla wydajnych zapytań
create index idx_users_is_guest on public.users (is_guest);
create index idx_users_ip_address on public.users (ip_address) where ip_address is not null;
create index idx_users_total_points on public.users (total_points desc);
create index idx_users_last_seen_at on public.users (last_seen_at desc) where last_seen_at is not null;

-- ==============================================================================
-- 3. TABELA: games - gry (vs_bot i pvp w ujednoliconym modelu)
-- ==============================================================================

create table if not exists public.games (
    id bigserial primary key,
    game_type varchar(20) not null check (game_type in ('vs_bot', 'pvp')),
    board_size smallint not null check (board_size in (3, 4, 5)),
    player1_id bigint not null references public.users(id) on delete cascade,
    player2_id bigint references public.users(id) on delete cascade,
    bot_difficulty varchar(20) check (bot_difficulty in ('easy', 'medium', 'hard')),
    status varchar(20) not null default 'waiting',
    current_player_symbol varchar(10) check (current_player_symbol in ('x', 'o')),
    winner_id bigint references public.users(id) on delete set null,
    last_move_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    finished_at timestamp with time zone
);

comment on table public.games is 'gry: vs_bot (z botem) lub pvp (z innym graczem) w ujednoliconym modelu';
comment on column public.games.game_type is 'typ gry: vs_bot lub pvp';
comment on column public.games.board_size is 'rozmiar planszy: 3, 4 lub 5';
comment on column public.games.bot_difficulty is 'poziom trudności bota - tylko dla vs_bot';
comment on column public.games.status is 'status gry';
comment on column public.games.last_move_at is 'timestamp ostatniego ruchu (używane do timeout pvp)';

-- constraint: zapewnia poprawność typu gry
alter table public.games
    add constraint games_vs_bot_check
    check (
        -- vs_bot: game_type='vs_bot' AND player2_id null AND bot_difficulty not null
        (game_type = 'vs_bot' AND player2_id IS NULL AND bot_difficulty IS NOT NULL)
        OR
        -- pvp: game_type='pvp' AND player2_id not null AND bot_difficulty null
        (game_type = 'pvp' AND player2_id IS NOT NULL AND bot_difficulty IS NULL)
    );

-- constraint: zapewnia poprawność statusu gry
alter table public.games
    add constraint games_status_check
    check (
        -- waiting: status='waiting' AND current_player_symbol null AND winner_id null
        (status = 'waiting' AND current_player_symbol IS NULL AND winner_id IS NULL)
        OR
        status IN ('in_progress', 'finished', 'abandoned', 'draw')
    );

-- constraint: zapewnia istnienie finished_at dla zakończonych gier
alter table public.games
    add constraint games_finished_check
    check (
        -- zakończone: status in ('finished','abandoned','draw') AND finished_at not null
        (status IN ('finished', 'abandoned', 'draw') AND finished_at IS NOT NULL)
        OR
        -- aktywne: status not in ('finished','abandoned','draw')
        status NOT IN ('finished', 'abandoned', 'draw')
    );

-- indeksy dla wydajnych zapytań
create index idx_games_game_type on public.games (game_type);
create index idx_games_status on public.games (status);
create index idx_games_player1_id on public.games (player1_id);
create index idx_games_player2_id on public.games (player2_id) where player2_id is not null;
create index idx_games_status_type on public.games (status, game_type);
create index idx_games_last_move_at on public.games (last_move_at) where last_move_at is not null;
create index idx_games_created_at on public.games (created_at desc);

-- ==============================================================================
-- 4. TABELA: moves - historia ruchów (stan planszy generowany dynamicznie)
-- ==============================================================================

create table if not exists public.moves (
    id bigserial primary key,
    game_id bigint not null references public.games(id) on delete cascade,
    player_id bigint references public.users(id) on delete set null,
    row smallint not null check (row >= 0),
    col smallint not null check (col >= 0),
    player_symbol varchar(10) not null check (player_symbol in ('x', 'o')),
    move_order smallint not null check (move_order > 0),
    created_at timestamp with time zone not null default now()
);

comment on table public.moves is 'historia ruchów - stan planszy generowany dynamicznie z tej tabeli';
comment on column public.moves.player_id is 'id gracza wykonującego ruch (null dla ruchów bota)';
comment on column public.moves.row is 'wiersz planszy (0-indexed)';
comment on column public.moves.col is 'kolumna planszy (0-indexed)';
comment on column public.moves.move_order is 'kolejność ruchu w grze (pozwala odtworzyć sekwencję)';

-- unique constraint: unikalność pozycji w grze
create unique index idx_moves_game_id_position_unique on public.moves (game_id, row, col);

-- indeksy dla wydajnych zapytań
create index idx_moves_game_id on public.moves (game_id);
create index idx_moves_game_id_move_order on public.moves (game_id, move_order);
create index idx_moves_player_id on public.moves (player_id) where player_id is not null;

-- ==============================================================================
-- 5. ROW LEVEL SECURITY (RLS) - włączenie dla tabel
-- ==============================================================================

alter table public.users enable row level security;
alter table public.games enable row level security;
alter table public.moves enable row level security;

-- ==============================================================================
-- 6. RLS POLICIES - polityki dostępu dla tabel
-- ==============================================================================

-- ==============================================================================
-- USERS - polityki dostępu
-- ==============================================================================

-- SELECT: zarejestrowani mogą zobaczyć swoje dane i publiczne dane innych zarejestrowanych
create policy users_select_authenticated on public.users
    for select
    using (
        -- własne dane: auth_user_id = auth.uid()
        auth_user_id = auth.uid()
        OR
        -- publiczne dane zarejestrowanych (dla rankingu)
        (is_guest = false AND username IS NOT NULL)
    );

comment on policy users_select_authenticated on public.users is 'zarejestrowani: własne dane + publiczne dane innych zarejestrowanych (ranking)';

-- SELECT: goście mogą zobaczyć publiczne dane zarejestrowanych (ranking)
create policy users_select_anon on public.users
    for select
    using (
        is_guest = false AND username IS NOT NULL
    );

comment on policy users_select_anon on public.users is 'goście: publiczne dane zarejestrowanych (ranking)';

-- INSERT: zarejestrowani mogą tworzyć swoje profile
create policy users_insert_authenticated on public.users
    for insert
    with check (
        auth_user_id = auth.uid() AND is_guest = false
    );

comment on policy users_insert_authenticated on public.users is 'zarejestrowani: tworzenie swojego profilu';

-- INSERT: goście mogą tworzyć swoje profile (przez aplikację)
-- uwaga: goście nie używają anon, więc ta polityka jest tylko dla bezpieczeństwa
create policy users_insert_anon on public.users
    for insert
    with check (
        is_guest = true AND auth_user_id IS NULL AND ip_address IS NOT NULL
    );

comment on policy users_insert_anon on public.users is 'goście: tworzenie swojego profilu (tylko dla bezpieczeństwa rls)';

-- UPDATE: zarejestrowani mogą aktualizować swoje dane
create policy users_update_authenticated on public.users
    for update
    using (auth_user_id = auth.uid())
    with check (auth_user_id = auth.uid());

comment on policy users_update_authenticated on public.users is 'zarejestrowani: aktualizacja swoich danych';

-- ==============================================================================
-- GAMES - polityki dostępu
-- ==============================================================================

-- SELECT: uczestnicy gry mogą zobaczyć gry, w których uczestniczą
create policy games_select_authenticated on public.games
    for select
    using (
        EXISTS (
            SELECT 1 FROM public.users
            WHERE (users.id = games.player1_id OR users.id = games.player2_id)
            AND users.auth_user_id = auth.uid()
        )
    );

comment on policy games_select_authenticated on public.games is 'zarejestrowani: gry, w których uczestniczą';

-- SELECT: goście mogą zobaczyć gry, w których uczestniczą (przez aplikację)
-- uwaga: goście nie używają anon, więc ta polityka jest tylko dla bezpieczeństwa
create policy games_select_anon on public.games
    for select
    using (false);

comment on policy games_select_anon on public.games is 'goście: polityka zablokowana (dostęp przez aplikację)';

-- INSERT: uczestnicy mogą tworzyć gry, gdzie są player1_id
create policy games_insert_authenticated on public.games
    for insert
    with check (
        EXISTS (
            SELECT 1 FROM public.users
            WHERE users.id = player1_id
            AND users.auth_user_id = auth.uid()
        )
    );

comment on policy games_insert_authenticated on public.games is 'zarejestrowani: tworzenie gier, gdzie są player1_id';

-- INSERT: goście mogą tworzyć gry (przez aplikację)
create policy games_insert_anon on public.games
    for insert
    with check (false);

comment on policy games_insert_anon on public.games is 'goście: polityka zablokowana (dostęp przez aplikację)';

-- UPDATE: uczestnicy mogą aktualizować gry, w których uczestniczą
create policy games_update_authenticated on public.games
    for update
    using (
        EXISTS (
            SELECT 1 FROM public.users
            WHERE (users.id = games.player1_id OR users.id = games.player2_id)
            AND users.auth_user_id = auth.uid()
        )
    )
    with check (
        EXISTS (
            SELECT 1 FROM public.users
            WHERE (users.id = games.player1_id OR users.id = games.player2_id)
            AND users.auth_user_id = auth.uid()
        )
    );

comment on policy games_update_authenticated on public.games is 'zarejestrowani: aktualizacja gier, w których uczestniczą';

-- ==============================================================================
-- MOVES - polityki dostępu
-- ==============================================================================

-- SELECT: uczestnicy gry mogą zobaczyć ruchy z gier, w których uczestniczą
create policy moves_select_authenticated on public.moves
    for select
    using (
        EXISTS (
            SELECT 1 FROM public.games, public.users
            WHERE moves.game_id = games.id
            AND (games.player1_id = users.id OR games.player2_id = users.id)
            AND users.auth_user_id = auth.uid()
        )
    );

comment on policy moves_select_authenticated on public.moves is 'zarejestrowani: ruchy z gier, w których uczestniczą';

-- SELECT: goście mogą zobaczyć ruchy (przez aplikację)
create policy moves_select_anon on public.moves
    for select
    using (false);

comment on policy moves_select_anon on public.moves is 'goście: polityka zablokowana (dostęp przez aplikację)';

-- INSERT: uczestnicy mogą dodawać ruchy do gier, w których uczestniczą
create policy moves_insert_authenticated on public.moves
    for insert
    with check (
        EXISTS (
            SELECT 1 FROM public.games, public.users
            WHERE moves.game_id = games.id
            AND (games.player1_id = users.id OR games.player2_id = users.id)
            AND users.auth_user_id = auth.uid()
        )
    );

comment on policy moves_insert_authenticated on public.moves is 'zarejestrowani: dodawanie ruchów do gier, w których uczestniczą';

-- INSERT: goście mogą dodawać ruchy (przez aplikację)
create policy moves_insert_anon on public.moves
    for insert
    with check (false);

comment on policy moves_insert_anon on public.moves is 'goście: polityka zablokowana (dostęp przez aplikację)';

