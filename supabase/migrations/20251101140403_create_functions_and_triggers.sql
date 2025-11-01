-- ==============================================================================
-- migration: create_functions_and_triggers
-- ==============================================================================
-- purpose: tworzy funkcje pomocnicze, triggery i automatyczne aktualizacje
-- affected functions: generate_board_state, is_move_valid, calculate_game_points, 
--                     check_pvp_timeout, get_user_ranking_position, refresh_player_rankings,
--                     update_updated_at_column, update_game_last_move_at, update_user_stats_on_game_completion
-- affected triggers: update_users_updated_at, update_games_updated_at, 
--                    update_game_last_move_timestamp, update_user_stats_on_game_finished
-- affected views: game_summary, player_rankings
-- 
-- szczegóły:
-- - funkcje pomocnicze dla generowania stanu planszy, walidacji ruchów, obliczania punktów
-- - triggery automatycznie aktualizujące updated_at, last_move_at, statystyki użytkowników
-- - widoki dla uproszczenia zapytań (game_summary, player_rankings materialized view)
-- - system automatycznego odświeżania rankingu
-- ==============================================================================

-- ==============================================================================
-- 1. FUNKCJE POMOCNICZE - generowanie stanu planszy, walidacja, obliczenia
-- ==============================================================================

-- generate_board_state: generuje stan planszy jako tablicę 2d na podstawie historii ruchów
create or replace function public.generate_board_state(p_game_id bigint)
returns text[][]
language plpgsql
stable
as $$
declare
    v_board_size smallint;
    v_board text[][];
    v_move record;
begin
    -- pobierz rozmiar planszy
    SELECT board_size INTO v_board_size
    FROM public.games
    WHERE id = p_game_id;
    
    IF v_board_size IS NULL THEN
        RAISE EXCEPTION 'game % does not exist', p_game_id;
    END IF;
    
    -- inicjalizuj pustą planszę
    v_board := array_fill(''::text, ARRAY[v_board_size, v_board_size]);
    
    -- wypełnij planszę symbolami z ruchów
    FOR v_move IN
        SELECT row, col, player_symbol::text
        FROM public.moves
        WHERE game_id = p_game_id
        ORDER BY move_order
    LOOP
        v_board[v_move.row + 1][v_move.col + 1] := v_move.player_symbol;
    END LOOP;
    
    RETURN v_board;
END;
$$;

comment on function public.generate_board_state(bigint) is 'generuje stan planszy jako tablicę 2d na podstawie historii ruchów';

-- is_move_valid: waliduje czy ruch jest poprawny (granice planszy i czy pozycja nie jest zajęta)
create or replace function public.is_move_valid(p_game_id bigint, p_row smallint, p_col smallint)
returns boolean
language plpgsql
stable
as $$
declare
    v_board_size smallint;
begin
    -- sprawdź czy gra istnieje
    SELECT board_size INTO v_board_size
    FROM public.games
    WHERE id = p_game_id;
    
    IF v_board_size IS NULL THEN
        RETURN false;
    END IF;
    
    -- sprawdź granice planszy
    IF p_row < 0 OR p_col < 0 OR p_row >= v_board_size OR p_col >= v_board_size THEN
        RETURN false;
    END IF;
    
    -- sprawdź czy pozycja nie jest już zajęta
    IF EXISTS (
        SELECT 1 FROM public.moves
        WHERE game_id = p_game_id AND row = p_row AND col = p_col
    ) THEN
        RETURN false;
    END IF;
    
    RETURN true;
END;
$$;

comment on function public.is_move_valid(bigint, smallint, smallint) is 'waliduje czy ruch jest poprawny (granice planszy i czy pozycja nie jest zajęta)';

-- calculate_game_points: oblicza punkty za wygraną na podstawie typu gry i poziomu trudności
create or replace function public.calculate_game_points(p_game_type game_type_enum, p_bot_difficulty bot_difficulty_enum)
returns bigint
language plpgsql
stable
as $$
begin
    -- pvp: +1000 punktów
    IF p_game_type = 'pvp' THEN
        RETURN 1000;
    END IF;
    
    -- vs_bot: punkty zależą od poziomu trudności
    IF p_game_type = 'vs_bot' THEN
        IF p_bot_difficulty = 'easy' THEN
            RETURN 100;
        ELSIF p_bot_difficulty = 'medium' THEN
            RETURN 500;
        ELSIF p_bot_difficulty = 'hard' THEN
            RETURN 1000;
        END IF;
    END IF;
    
    -- domyślnie 0 punktów (dla bezpieczeństwa)
    RETURN 0;
END;
$$;

comment on function public.calculate_game_points(game_type_enum, bot_difficulty_enum) is 'oblicza punkty za wygraną: pvp=1000, vs_bot easy=100, medium=500, hard=1000';

-- check_pvp_timeout: sprawdza gry pvp z timeout 20 sekund i automatycznie kończy je
create or replace function public.check_pvp_timeout()
returns integer
language plpgsql
as $$
declare
    v_updated_count integer := 0;
    v_game record;
    v_last_player_id bigint;
begin
    -- znajdź gry pvp z timeout
    FOR v_game IN
        SELECT id, player1_id, player2_id
        FROM public.games
        WHERE game_type = 'pvp'
        AND status = 'in_progress'
        AND last_move_at < NOW() - INTERVAL '20 seconds'
    LOOP
        -- znajdź gracza, który wykonał ostatni ruch (zwycięzca)
        SELECT player_id INTO v_last_player_id
        FROM public.moves
        WHERE game_id = v_game.id
        ORDER BY move_order DESC
        LIMIT 1;
        
        -- zwycięzca to przeciwnik tego, kto wykonał ostatni ruch (bo ten timeout'ował)
        UPDATE public.games
        SET status = 'finished',
            winner_id = CASE
                WHEN v_last_player_id = v_game.player1_id THEN v_game.player2_id
                ELSE v_game.player1_id
            END,
            finished_at = NOW()
        WHERE id = v_game.id;
        
        v_updated_count := v_updated_count + 1;
    END LOOP;
    
    RETURN v_updated_count;
END;
$$;

comment on function public.check_pvp_timeout() is 'sprawdza gry pvp z timeout 20s i automatycznie kończy je - powinna być wywoływana co 5-10s przez spring scheduled job';

-- get_user_ranking_position: zwraca pozycję użytkownika w rankingu (tylko zarejestrowani)
create or replace function public.get_user_ranking_position(p_user_id bigint)
returns bigint
language plpgsql
stable
as $$
declare
    v_position bigint;
begin
    WITH ranked_users AS (
        SELECT 
            id,
            ROW_NUMBER() OVER (
                ORDER BY total_points DESC, created_at ASC
            ) AS rank_position
        FROM public.users
        WHERE is_guest = false
    )
    SELECT rank_position INTO v_position
    FROM ranked_users
    WHERE id = p_user_id;
    
    RETURN COALESCE(v_position, 0);
END;
$$;

comment on function public.get_user_ranking_position(bigint) is 'zwraca pozycję użytkownika w rankingu (tylko zarejestrowani)';

-- ==============================================================================
-- 2. FUNKCJE TRIGGER - automatyczne aktualizacje
-- ==============================================================================

-- update_updated_at_column: automatycznie aktualizuje updated_at do bieżącego czasu
create or replace function public.update_updated_at_column()
returns trigger
language plpgsql
as $$
begin
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

comment on function public.update_updated_at_column() is 'automatycznie aktualizuje updated_at do bieżącego czasu';

-- update_game_last_move_at: aktualizuje last_move_at w tabeli games przy każdym nowym ruchu
create or replace function public.update_game_last_move_at()
returns trigger
language plpgsql
as $$
begin
    UPDATE public.games
    SET last_move_at = NOW()
    WHERE id = NEW.game_id;
    RETURN NEW;
END;
$$;

comment on function public.update_game_last_move_at() is 'aktualizuje last_move_at w tabeli games przy każdym nowym ruchu';

-- update_user_stats_on_game_completion: automatycznie aktualizuje statystyki użytkownika po zakończeniu gry
create or replace function public.update_user_stats_on_game_completion()
returns trigger
language plpgsql
as $$
declare
    v_points bigint;
begin
    -- sprawdź czy gra została zakończona (status zmieniony na finished)
    IF NEW.status = 'finished' AND (OLD.status IS NULL OR OLD.status != 'finished') THEN
        -- oblicz punkty za wygraną
        v_points := public.calculate_game_points(NEW.game_type, NEW.bot_difficulty);
        
        -- aktualizuj games_played dla gracza 1 (zawsze) i gracza 2 (jeśli istnieje)
        UPDATE public.users
        SET games_played = games_played + 1
        WHERE id = NEW.player1_id OR (id = NEW.player2_id AND NEW.player2_id IS NOT NULL);
        
        -- aktualizuj games_won i total_points dla zwycięzcy
        IF NEW.winner_id IS NOT NULL THEN
            UPDATE public.users
            SET games_won = games_won + 1,
                total_points = total_points + v_points
            WHERE id = NEW.winner_id;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$;

comment on function public.update_user_stats_on_game_completion() is 'automatycznie aktualizuje statystyki użytkownika po zakończeniu gry (status=finished)';

-- ==============================================================================
-- 3. TRIGGERY - wiązania funkcji z tabelami
-- ==============================================================================

-- trigger: aktualizacja updated_at w tabeli users
create trigger update_users_updated_at
    before update on public.users
    for each row
    execute function public.update_updated_at_column();

comment on trigger update_users_updated_at on public.users is 'automatycznie aktualizuje updated_at przed każdą aktualizacją users';

-- trigger: aktualizacja updated_at w tabeli games
create trigger update_games_updated_at
    before update on public.games
    for each row
    execute function public.update_updated_at_column();

comment on trigger update_games_updated_at on public.games is 'automatycznie aktualizuje updated_at przed każdą aktualizacją games';

-- trigger: aktualizacja last_move_at w tabeli games po każdym ruchu
create trigger update_game_last_move_timestamp
    after insert on public.moves
    for each row
    execute function public.update_game_last_move_at();

comment on trigger update_game_last_move_timestamp on public.moves is 'aktualizuje last_move_at w tabeli games po każdym nowym ruchu';

-- trigger: aktualizacja statystyk użytkownika po zakończeniu gry
create trigger update_user_stats_on_game_finished
    after update of status on public.games
    for each row
    execute function public.update_user_stats_on_game_completion();

comment on trigger update_user_stats_on_game_finished on public.games is 'automatycznie aktualizuje statystyki użytkownika po zakończeniu gry (status=finished)';

-- ==============================================================================
-- 4. WIDOKI - uproszczenie zapytań
-- ==============================================================================

-- game_summary: widok łączący tabele games i users
create or replace view public.game_summary as
SELECT 
    g.*,
    -- dane gracza 1
    p1.id as player1_user_id,
    p1.username as player1_username,
    p1.is_guest as player1_is_guest,
    p1.auth_user_id as player1_auth_user_id,
    -- dane gracza 2
    p2.id as player2_user_id,
    p2.username as player2_username,
    p2.is_guest as player2_is_guest,
    p2.auth_user_id as player2_auth_user_id,
    -- dane zwycięzcy
    w.id as winner_user_id,
    w.username as winner_username,
    w.auth_user_id as winner_auth_user_id,
    -- liczba ruchów w grze
    (SELECT COUNT(*) FROM public.moves WHERE game_id = g.id) as total_moves
FROM public.games g
LEFT JOIN public.users p1 ON g.player1_id = p1.id
LEFT JOIN public.users p2 ON g.player2_id = p2.id
LEFT JOIN public.users w ON g.winner_id = w.id;

comment on view public.game_summary is 'widok łączący tabele games i users - kompleksowe informacje o grze';

-- ==============================================================================
-- 5. MATERIALIZED VIEW - ranking graczy (pre-obliczony)
-- ==============================================================================

-- player_rankings: materialized view z rankingiem graczy (tylko zarejestrowani)
create materialized view if not exists public.player_rankings as
SELECT 
    id,
    username,
    total_points,
    games_played,
    games_won,
    ROW_NUMBER() OVER (ORDER BY total_points DESC, created_at ASC) as rank_position,
    created_at
FROM public.users
WHERE is_guest = false;

comment on materialized view public.player_rankings is 'ranking graczy (tylko zarejestrowani) - pre-obliczona pozycja';

-- indeksy dla materialized view
create unique index idx_player_rankings_id on public.player_rankings (id);
create index idx_player_rankings_points on public.player_rankings (total_points desc);
create index idx_player_rankings_rank on public.player_rankings (rank_position);

-- refresh_player_rankings: odświeża materialized view (concurrently dla dostępności)
create or replace function public.refresh_player_rankings()
returns void
language plpgsql
as $$
begin
    REFRESH MATERIALIZED VIEW CONCURRENTLY public.player_rankings;
END;
$$;

comment on function public.refresh_player_rankings() is 'odświeża materialized view player_rankings (concurrently) - powinna być wywoływana co 5-15min przez spring scheduled job';

