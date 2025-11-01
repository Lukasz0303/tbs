# Plan bazy danych - World at War: Turn-Based Strategy

## 1. Przegląd ogólny

Baza danych PostgreSQL została zaprojektowana dla aplikacji World at War: Turn-Based Strategy, obsługującej gry w kółko i krzyżyk z trybem gry z botem AI (3 poziomy trudności) oraz trybem pvp w czasie rzeczywistym. Schemat wspiera system punktowy, ranking globalny oraz identyfikację użytkowników gości i zarejestrowanych.

### Kluczowe założenia projektowe:
- Tabela `users` obsługująca zarówno gości (identyfikacja przez IP) jak i zarejestrowanych użytkowników
- **Zarządzanie użytkownikami zarejestrowanymi przez Supabase Auth** (`auth.users`)
- Ujednolicony model gier w tabeli `games` dla vs_bot i pvp
- Stan planszy generowany dynamicznie z historii ruchów w tabeli `moves`
- Automatyczna aktualizacja punktów i statystyk przez triggery
- Materialized view dla wydajnych zapytań rankingowych
- Row Level Security (RLS) dla bezpieczeństwa danych z integracją Supabase Auth

---

## 2. Typy danych (ENUM)

### game_type_enum
Typ gry: `vs_bot` | `pvp`

> **Uwaga:** ENUMy w bazie danych używają konwencji lowercase

### game_status_enum
Status gry: `waiting` | `in_progress` | `finished` | `abandoned` | `draw`

### bot_difficulty_enum
Poziom trudności bota: `easy` | `medium` | `hard`
- easy: +100 punktów za wygraną
- medium: +500 punktów za wygraną
- hard: +1000 punktów za wygraną

### player_symbol_enum
Symbol gracza: `x` | `o`

---

## 3. Tabele

### 3.1. Tabela: users

Przechowuje użytkowników zarejestrowanych oraz gości (identyfikowanych przez IP). **Zarządzanie użytkownikami zarejestrowanymi odbywa się przez Supabase Auth (`auth.users`)**.

#### Kolumny:

| Nazwa | Typ | Nullable | Domyślna wartość | Opis |
|-------|-----|----------|------------------|------|
| `id` | BIGSERIAL | NO | - | Klucz podstawowy (auto-increment) |
| `auth_user_id` | UUID | YES | NULL | ID użytkownika w Supabase Auth (foreign key → auth.users.id, tylko dla zarejestrowanych) |
| `username` | VARCHAR(50) | YES | NULL | Nazwa użytkownika (UNIQUE, tylko dla zarejestrowanych) |
| `is_guest` | BOOLEAN | NO | FALSE | TRUE dla gości, FALSE dla zarejestrowanych |
| `ip_address` | INET | YES | NULL | Adres IP gościa (tylko dla gości) |
| `total_points` | BIGINT | NO | 0 | Suma punktów gracza (używane do rankingu) |
| `games_played` | INTEGER | NO | 0 | Liczba rozegranych gier |
| `games_won` | INTEGER | NO | 0 | Liczba wygranych gier |
| `last_seen_at` | TIMESTAMP WITH TIME ZONE | YES | NULL | Ostatnia aktywność (używane do matchmakingu) |
| `created_at` | TIMESTAMP WITH TIME ZONE | NO | NOW() | Data utworzenia |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NO | NOW() | Data ostatniej aktualizacji |

**Uwaga**: Email i hash hasła są zarządzane przez Supabase Auth w tabeli `auth.users`. Dostęp do tych danych odbywa się przez `auth_user_id`.

#### Ograniczenia (Constraints):

1. **users_registered_check** - Zapewnia poprawność danych:
   - Goście: `is_guest = TRUE` AND `auth_user_id IS NULL` AND `username IS NULL` AND `ip_address IS NOT NULL`
   - Zarejestrowani: `is_guest = FALSE` AND `auth_user_id IS NOT NULL` AND `username IS NOT NULL` AND `ip_address IS NULL`

2. **UNIQUE constraints**:
   - `auth_user_id` (NULL values excluded, UNIQUE) - Jeden rekord w `users` na użytkownika w Supabase Auth
   - `username` (NULL values excluded, UNIQUE)

3. **Foreign Keys**:
   - `auth_user_id` → `auth.users.id` ON DELETE CASCADE - Usunięcie użytkownika w Supabase Auth usuwa rekord w `users`

#### Indeksy:

- `idx_users_is_guest` ON `is_guest` - Filtrowanie gości/zarejestrowanych
- `idx_users_auth_user_id` UNIQUE ON `auth_user_id` WHERE `auth_user_id IS NOT NULL` - Łączenie z Supabase Auth
- `idx_users_username` UNIQUE ON `username` WHERE `username IS NOT NULL` - Szybkie wyszukiwanie po nazwie
- `idx_users_ip_address` ON `ip_address` WHERE `ip_address IS NOT NULL` - Identyfikacja gości
- `idx_users_total_points` ON `total_points DESC` - Ranking (wydajne zapytania)
- `idx_users_last_seen_at` ON `last_seen_at DESC` WHERE `last_seen_at IS NOT NULL` - Matchmaking

---

### 3.2. Tabela: games

Przechowuje gry vs_bot i pvp w ujednoliconym modelu.

#### Kolumny:

| Nazwa | Typ | Nullable | Domyślna wartość | Opis |
|-------|-----|----------|------------------|------|
| `id` | BIGSERIAL | NO | - | Klucz podstawowy (auto-increment) |
| `game_type` | game_type_enum | NO | - | Typ gry: vs_bot lub pvp |
| `board_size` | SMALLINT | NO | - | Rozmiar planszy: 3, 4 lub 5 |
| `player1_id` | BIGINT | NO | - | ID gracza 1 (foreign key → users.id) |
| `player2_id` | BIGINT | YES | NULL | ID gracza 2 (foreign key → users.id, NULL dla vs_bot) |
| `bot_difficulty` | bot_difficulty_enum | YES | NULL | Poziom trudności bota (tylko dla vs_bot) |
| `status` | game_status_enum | NO | 'waiting' | Status gry |
| `current_player_symbol` | player_symbol_enum | YES | NULL | Symbol gracza, który wykonuje obecnie ruch |
| `winner_id` | BIGINT | YES | NULL | ID zwycięzcy (foreign key → users.id) |
| `last_move_at` | TIMESTAMP WITH TIME ZONE | YES | NULL | Timestamp ostatniego ruchu (używane do timeout pvp) |
| `created_at` | TIMESTAMP WITH TIME ZONE | NO | NOW() | Data utworzenia |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NO | NOW() | Data ostatniej aktualizacji |
| `finished_at` | TIMESTAMP WITH TIME ZONE | YES | NULL | Data zakończenia (tylko dla finished/abandoned/draw) |

#### Ograniczenia (Constraints):

1. **games_vs_bot_check** - Zapewnia poprawność typu gry:
   - vs_bot: `game_type = 'vs_bot'` AND `player2_id IS NULL` AND `bot_difficulty IS NOT NULL`
   - pvp: `game_type = 'pvp'` AND `player2_id IS NOT NULL` AND `bot_difficulty IS NULL`

2. **games_status_check** - Zapewnia poprawność statusu:
   - waiting: `status = 'waiting'` AND `current_player_symbol IS NULL` AND `winner_id IS NULL`
   - in_progress/finished/abandoned/draw: status w odpowiednim enumie

3. **games_finished_check** - Zapewnia istnienie `finished_at` dla zakończonych gier:
   - Zakończone: `status IN ('finished', 'abandoned', 'draw')` AND `finished_at IS NOT NULL`
   - Aktywne: `status NOT IN ('finished', 'abandoned', 'draw')`

4. **board_size CHECK** - Rozmiar planszy: 3, 4 lub 5

5. **Foreign Keys**:
   - `player1_id` → `users.id` ON DELETE CASCADE
   - `player2_id` → `users.id` ON DELETE CASCADE
   - `winner_id` → `users.id` ON DELETE SET NULL

#### Indeksy:

- `idx_games_game_type` ON `game_type` - Filtrowanie po typie gry
- `idx_games_status` ON `status` - Zapytania o status gry
- `idx_games_player1_id` ON `player1_id` - Gry gracza 1
- `idx_games_player2_id` ON `player2_id` WHERE `player2_id IS NOT NULL` - Gry gracza 2 (pvp)
- `idx_games_status_type` ON (`status`, `game_type`) - Zapytania filtrowane (matchmaking)
- `idx_games_last_move_at` ON `last_move_at` WHERE `last_move_at IS NOT NULL` - Timeout detection
- `idx_games_created_at` ON `created_at DESC` - Sortowanie po dacie utworzenia

---

### 3.3. Tabela: moves

Przechowuje historię ruchów. Stan planszy jest generowany dynamicznie z tej tabeli.

#### Kolumny:

| Nazwa | Typ | Nullable | Domyślna wartość | Opis |
|-------|-----|----------|------------------|------|
| `id` | BIGSERIAL | NO | - | Klucz podstawowy (auto-increment) |
| `game_id` | BIGINT | NO | - | ID gry (foreign key → games.id) |
| `player_id` | BIGINT | YES | NULL | ID gracza (foreign key → users.id, NULL dla ruchów bota) |
| `row` | SMALLINT | NO | - | Wiersz planszy (0-indexed) |
| `col` | SMALLINT | NO | - | Kolumna planszy (0-indexed) |
| `player_symbol` | player_symbol_enum | NO | - | Symbol gracza: 'x' lub 'o' |
| `move_order` | SMALLINT | NO | - | Kolejność ruchu w grze (pozwala odtworzyć sekwencję) |
| `created_at` | TIMESTAMP WITH TIME ZONE | NO | NOW() | Data utworzenia ruchu |

#### Ograniczenia (Constraints):

1. **CHECK constraints**:
   - `row >= 0` - Wiersz musi być nieujemny
   - `col >= 0` - Kolumna musi być nieujemna
   - `move_order > 0` - Kolejność ruchu musi być dodatnia

2. **UNIQUE constraint**:
   - `idx_moves_game_id_position_unique` ON (`game_id`, `row`, `col`) - Zapewnia unikalność pozycji w grze

3. **Foreign Keys**:
   - `game_id` → `games.id` ON DELETE CASCADE
   - `player_id` → `users.id` ON DELETE SET NULL

#### Indeksy:

- `idx_moves_game_id` ON `game_id` - Ruchy w grze
- `idx_moves_game_id_move_order` ON (`game_id`, `move_order`) - Generowanie stanu planszy (sortowanie)
- `idx_moves_player_id` ON `player_id` WHERE `player_id IS NOT NULL` - Ruchy gracza
- `idx_moves_game_id_position_unique` UNIQUE ON (`game_id`, `row`, `col`) - Unikalność pozycji w grze

---

## 4. Relacje między tabelami

### 4.1. USERS → GAMES

**Relacja jeden-do-wielu (1:N)**

- `users.id` ← `games.player1_id` - Użytkownik może być graczem 1 w wielu grach
- `users.id` ← `games.player2_id` - Użytkownik może być graczem 2 w wielu grach (tylko pvp)
- `users.id` ← `games.winner_id` - Użytkownik może wygrać wiele gier

**Akcje ON DELETE**:
- `player1_id`, `player2_id`: CASCADE - Usunięcie użytkownika usuwa jego gry
- `winner_id`: SET NULL - Usunięcie użytkownika pozostawia grę (winner_id = NULL)

### 4.2. GAMES → MOVES

**Relacja jeden-do-wielu (1:N)**

- `games.id` ← `moves.game_id` - Gra może mieć wiele ruchów

**Akcja ON DELETE**: CASCADE - Usunięcie gry usuwa wszystkie ruchy

### 4.3. USERS → MOVES

**Relacja jeden-do-wielu (1:N)**

- `users.id` ← `moves.player_id` - Użytkownik może wykonać wiele ruchów

**Akcja ON DELETE**: SET NULL - Usunięcie użytkownika pozostawia ruchy (player_id = NULL dla botów)

### 4.4. AUTH.USERS → USERS (Supabase Auth)

**Relacja jeden-do-jednego (1:1)**

- `auth.users.id` ← `users.auth_user_id` - Jeden użytkownik w Supabase Auth ma jeden profil w tabeli `users`

**Akcja ON DELETE**: CASCADE - Usunięcie użytkownika w Supabase Auth usuwa jego profil w `users`

**Uwaga**: 
- Tabela `auth.users` jest zarządzana przez Supabase Auth
- Zawiera dane autentykacyjne (email, hash hasła, metadata)
- Tabela `users` zawiera dane profilowe (statystyki, punkty, username) powiązane z `auth.users` przez `auth_user_id`
- Goście (`is_guest = TRUE`) nie mają powiązania z `auth.users` (`auth_user_id = NULL`)

---

## 5. Funkcje pomocnicze

### 5.1. generate_board_state(p_game_id BIGINT)

**Zwraca**: `TEXT[][]`

Generuje stan planszy jako tablicę 2D na podstawie historii ruchów w tabeli `moves`.

**Logika**:
1. Pobiera rozmiar planszy z tabeli `games`
2. Inicjalizuje pustą planszę
3. Wypełnia planszę symbolami z ruchów uporządkowanych przez `move_order`

**Użycie**: Do wyświetlania stanu gry na podstawie historii ruchów

---

### 5.2. is_move_valid(p_game_id BIGINT, p_row SMALLINT, p_col SMALLINT)

**Zwraca**: `BOOLEAN`

Waliduje czy ruch jest poprawny (granice planszy i czy pozycja nie jest zajęta).

**Logika**:
1. Sprawdza czy gra istnieje
2. Sprawdza granice planszy (row, col >= 0 i < board_size)
3. Sprawdza czy pozycja nie jest już zajęta

**Użycie**: Walidacja ruchów przed zapisaniem w tabeli `moves`

---

### 5.3. calculate_game_points(p_game_type game_type_enum, p_bot_difficulty bot_difficulty_enum)

**Zwraca**: `BIGINT`

Oblicza punkty za wygraną na podstawie typu gry i poziomu trudności.

**Zasady punktowe**:
- pvp: +1000 punktów
- vs_bot easy: +100 punktów
- vs_bot medium: +500 punktów
- vs_bot hard: +1000 punktów

**Użycie**: Automatyczne obliczanie punktów przy zakończeniu gry

---

### 5.4. check_pvp_timeout()

**Zwraca**: `INTEGER` (liczba zaktualizowanych gier)

Sprawdza gry pvp z statusem `in_progress`, które przekroczyły limit 20 sekund nieaktywności. Automatycznie:
1. Ustawia status na `finished`
2. Ustawia zwycięzcę (gracz, który nie timeout'ował)
3. Ustawia `finished_at`

**Użycie**: Powinna być wywoływana okresowo przez Spring Scheduled job (np. co 5-10 sekund)

---

### 5.5. get_user_ranking_position(p_user_id BIGINT)

**Zwraca**: `BIGINT`

Zwraca pozycję użytkownika w rankingu (tylko dla zarejestrowanych użytkowników).

**Logika**:
1. Filtruje tylko zarejestrowanych użytkowników (`is_guest = FALSE`)
2. Sortuje według `total_points DESC`, następnie `created_at ASC`
3. Zwraca pozycję (ROW_NUMBER)

**Użycie**: Wyświetlanie pozycji gracza w rankingu

---

### 5.6. refresh_player_rankings()

**Zwraca**: `VOID`

Odświeża materialized view `player_rankings` (CONCURRENTLY dla dostępności podczas odświeżania).

**Użycie**: Powinna być wywoływana okresowo przez Spring Scheduled job (np. co 5-15 minut)

---

### 5.7. update_updated_at_column()

**Zwraca**: `TRIGGER`

Automatycznie aktualizuje kolumnę `updated_at` do bieżącego czasu.

**Użycie**: Trigger BEFORE UPDATE na tabelach `users` i `games`

---

### 5.8. update_game_last_move_at()

**Zwraca**: `TRIGGER`

Aktualizuje `last_move_at` w tabeli `games` przy każdym nowym ruchu.

**Użycie**: Trigger AFTER INSERT na tabeli `moves`

---

### 5.9. update_user_stats_on_game_completion()

**Zwraca**: `TRIGGER`

Automatycznie aktualizuje statystyki użytkownika po zakończeniu gry (status = 'finished').

**Logika**:
1. Oblicza punkty za wygraną (`calculate_game_points`)
2. Aktualizuje `games_played` dla uczestników
3. Aktualizuje `games_won` i `total_points` dla zwycięzcy

**Użycie**: Trigger AFTER UPDATE OF status ON `games`

---

## 6. Triggery

### 6.1. update_users_updated_at

**Tabela**: `users`
**Typ**: BEFORE UPDATE
**Funkcja**: `update_updated_at_column()`
**Zachowanie**: Aktualizuje `updated_at` do bieżącego czasu przed każdą aktualizacją

---

### 6.2. update_games_updated_at

**Tabela**: `games`
**Typ**: BEFORE UPDATE
**Funkcja**: `update_updated_at_column()`
**Zachowanie**: Aktualizuje `updated_at` do bieżącego czasu przed każdą aktualizacją

---

### 6.3. update_game_last_move_timestamp

**Tabela**: `moves`
**Typ**: AFTER INSERT
**Funkcja**: `update_game_last_move_at()`
**Zachowanie**: Aktualizuje `last_move_at` w tabeli `games` po każdym nowym ruchu

---

### 6.4. update_user_stats_on_game_finished

**Tabela**: `games`
**Typ**: AFTER UPDATE OF status
**Funkcja**: `update_user_stats_on_game_completion()`
**Warunek**: `NEW.status = 'finished' AND (OLD.status IS NULL OR OLD.status != 'finished')`
**Zachowanie**: Automatyczna aktualizacja statystyk użytkownika (punkty, rozegrane gry, wygrane)

---

## 7. Widoki

### 7.1. game_summary (VIEW)

Widok łączący tabele `games` i `users`, dostarczający kompleksowe informacje o grze.

**Kolumny**:
- Wszystkie kolumny z tabeli `games`
- `player1_user_id`, `player1_username`, `player1_is_guest`, `player1_auth_user_id` - Dane gracza 1
- `player2_user_id`, `player2_username`, `player2_is_guest`, `player2_auth_user_id` - Dane gracza 2
- `winner_user_id`, `winner_username`, `winner_auth_user_id` - Dane zwycięzcy
- `total_moves` - Liczba ruchów w grze

**Uwaga**: Jeśli potrzebny jest email gracza, można JOINować widok z `auth.users` przez `auth_user_id = auth.users.id`.

**Użycie**: Uproszczenie zapytań wymagających informacji o graczach i grach

---

### 7.2. player_rankings (MATERIALIZED VIEW)

Materialized view z rankingiem graczy (tylko zarejestrowani użytkownicy).

**Kolumny**:
- `id` - ID użytkownika
- `username` - Nazwa użytkownika
- `total_points` - Suma punktów
- `games_played` - Liczba rozegranych gier
- `games_won` - Liczba wygranych gier
- `rank_position` - Pozycja w rankingu (ROW_NUMBER)
- `created_at` - Data utworzenia konta

**Sortowanie**: `total_points DESC`, `created_at ASC`

**Indeksy**:
- `idx_player_rankings_id` UNIQUE ON `id` - Szybki dostęp do konkretnego gracza
- `idx_player_rankings_points` ON `total_points DESC` - Ranking
- `idx_player_rankings_rank` ON `rank_position` - Pozycja w rankingu

**Odświeżanie**: Przez funkcję `refresh_player_rankings()` (CONCURRENTLY)

**Użycie**: Wydajne zapytania rankingowe bez konieczności liczenia pozycji w czasie rzeczywistym

---

## 8. Row Level Security (RLS)

Wszystkie tabele (`users`, `games`, `moves`) mają włączony Row Level Security (RLS).

### 8.1. Polityki SELECT

#### users_select_own
**Tabela**: `users`
**Dostęp**: Użytkownicy mogą zobaczyć:
- Swoje własne dane: `auth_user_id = auth.uid()` (dla zarejestrowanych) lub `id = current_setting('app.guest_user_id', TRUE)::BIGINT` (dla gości)
- Publiczne dane zarejestrowanych użytkowników (dla rankingu): `is_guest = FALSE AND username IS NOT NULL`

**Uwaga**: Supabase Auth automatycznie ustawia `auth.uid()` dla zalogowanych użytkowników.

#### games_select_participant
**Tabela**: `games`
**Dostęp**: Użytkownicy mogą zobaczyć gry, w których uczestniczą:
- `player1_id` lub `player2_id` wskazuje na rekord w `users` gdzie:
  - Dla zarejestrowanych: `users.auth_user_id = auth.uid()`
  - Dla gości: `users.id = current_setting('app.guest_user_id', TRUE)::BIGINT`

#### moves_select_participant
**Tabela**: `moves`
**Dostęp**: Użytkownicy mogą zobaczyć ruchy z gier, w których uczestniczą:
- EXISTS gra gdzie `player1_id` lub `player2_id` wskazuje na rekord w `users` gdzie:
  - Dla zarejestrowanych: `users.auth_user_id = auth.uid()`
  - Dla gości: `users.id = current_setting('app.guest_user_id', TRUE)::BIGINT`

---

### 8.2. Polityki INSERT/UPDATE

#### users_insert_registered
**Tabela**: `users`
**Typ**: INSERT
**Dostęp**: Użytkownicy zarejestrowani mogą tworzyć swój profil:
- WITH CHECK: `auth_user_id = auth.uid()` AND `is_guest = FALSE`

#### users_insert_guest
**Tabela**: `users`
**Typ**: INSERT
**Dostęp**: Goście mogą tworzyć swój profil (przez aplikację):
- WITH CHECK: `is_guest = TRUE` AND `auth_user_id IS NULL` AND `ip_address IS NOT NULL`

#### users_update_own
**Tabela**: `users`
**Typ**: UPDATE
**Dostęp**: Użytkownicy mogą aktualizować swoje dane (walidacja po stronie aplikacji):
- USING: `auth_user_id = auth.uid()` (zarejestrowani) OR `id = current_setting('app.guest_user_id', TRUE)::BIGINT` (goście)
- WITH CHECK: `auth_user_id = auth.uid()` (zarejestrowani) OR `id = current_setting('app.guest_user_id', TRUE)::BIGINT` (goście)

#### games_insert_player1
**Tabela**: `games`
**Typ**: INSERT
**Dostęp**: Użytkownicy mogą tworzyć gry, gdzie są `player1_id`:
- WITH CHECK: EXISTS rekord w `users` gdzie `player1_id = users.id` AND:
  - `users.auth_user_id = auth.uid()` (zarejestrowani) OR
  - `users.id = current_setting('app.guest_user_id', TRUE)::BIGINT` (goście)

#### games_update_participant
**Tabela**: `games`
**Typ**: UPDATE
**Dostęp**: Użytkownicy mogą aktualizować gry, w których uczestniczą:
- USING: EXISTS rekord w `users` gdzie (`player1_id = users.id` OR `player2_id = users.id`) AND:
  - `users.auth_user_id = auth.uid()` (zarejestrowani) OR
  - `users.id = current_setting('app.guest_user_id', TRUE)::BIGINT` (goście)
- WITH CHECK: EXISTS rekord w `users` gdzie (`player1_id = users.id` OR `player2_id = users.id`) AND:
  - `users.auth_user_id = auth.uid()` (zarejestrowani) OR
  - `users.id = current_setting('app.guest_user_id', TRUE)::BIGINT` (goście)

#### moves_insert_participant
**Tabela**: `moves`
**Typ**: INSERT
**Dostęp**: Użytkownicy mogą dodawać ruchy do gier, w których uczestniczą:
- WITH CHECK: EXISTS gra gdzie (`player1_id = users.id` OR `player2_id = users.id`) AND:
  - `users.auth_user_id = auth.uid()` (zarejestrowani) OR
  - `users.id = current_setting('app.guest_user_id', TRUE)::BIGINT` (goście)

---

### 8.3. Konfiguracja RLS

**Integracja z Supabase Auth**:
- Dla **zarejestrowanych użytkowników**: Supabase Auth automatycznie ustawia `auth.uid()` w kontekście zapytań
- Dla **gości**: Aplikacja musi ustawić `current_setting('app.guest_user_id')` przed zapytaniami:
```sql
SET LOCAL app.guest_user_id = '<guest_user_id>';
```

**Uwaga**: 
- Supabase Auth automatycznie obsługuje RLS dla zalogowanych użytkowników przez `auth.uid()`
- Dla gości identyfikowanych przez IP, aplikacja powinna stworzyć/użyć użytkownika gościa i ustawić `app.guest_user_id`
- Integracja z Supabase Auth wymaga odpowiedniej konfiguracji w Supabase Dashboard (włączenie RLS, konfiguracja polityk)

---

## 9. Indeksy - Podsumowanie

### Tabela users
- `idx_users_is_guest` - Filtrowanie gości/zarejestrowanych
- `idx_users_auth_user_id UNIQUE` (partial) - Łączenie z Supabase Auth (auth.users.id)
- `idx_users_username UNIQUE` (partial) - Szybkie wyszukiwanie po nazwie użytkownika
- `idx_users_ip_address` (partial) - Identyfikacja gości
- `idx_users_total_points DESC` - Ranking
- `idx_users_last_seen_at DESC` (partial) - Matchmaking

### Tabela games
- `idx_games_game_type` - Filtrowanie po typie
- `idx_games_status` - Zapytania o status
- `idx_games_player1_id` - Gry gracza 1
- `idx_games_player2_id` (partial) - Gry gracza 2
- `idx_games_status_type` - Zapytania filtrowane (matchmaking)
- `idx_games_last_move_at` (partial) - Timeout detection
- `idx_games_created_at DESC` - Sortowanie po dacie

### Tabela moves
- `idx_moves_game_id` - Ruchy w grze
- `idx_moves_game_id_move_order` - Generowanie stanu planszy
- `idx_moves_player_id` (partial) - Ruchy gracza
- `idx_moves_game_id_position_unique UNIQUE` - Unikalność pozycji

### Materialized view player_rankings
- `idx_player_rankings_id UNIQUE` - Szybki dostęp do gracza
- `idx_player_rankings_points DESC` - Ranking
- `idx_player_rankings_rank` - Pozycja w rankingu

---

## 10. System punktowy

### Automatyczna aktualizacja

System punktowy działa automatycznie przez trigger `update_user_stats_on_game_finished`:

1. **Trigger aktywowany**: Gdy status gry zmienia się na `finished`
2. **Obliczanie punktów**: Funkcja `calculate_game_points()` określa liczbę punktów
3. **Aktualizacja statystyk**:
   - `games_played` +1 dla obu graczy (vs_bot) lub obu graczy (pvp)
   - `games_won` +1 dla zwycięzcy
   - `total_points` + punkty dla zwycięzcy

### Zasady punktowe

| Typ gry | Poziom trudności | Punkty za wygraną |
|---------|------------------|-------------------|
| pvp | - | +1000 |
| vs_bot | easy | +100 |
| vs_bot | medium | +500 |
| vs_bot | hard | +1000 |

---

## 11. System rankingowy

### Ranking globalny (permanentny)

Ranking jest oparty na kolumnie `total_points` w tabeli `users`:
- Sortowanie: `total_points DESC`, następnie `created_at ASC` (starsze konta mają priorytet przy remisie)
- Tylko zarejestrowani użytkownicy (`is_guest = FALSE`) są uwzględnieni w rankingu

### Materialized view player_rankings

Materialized view zapewnia wydajne zapytania rankingowe:
- Zawiera tylko zarejestrowanych użytkowników
- Zawiera pre-obliczoną pozycję (`rank_position`)
- Odświeżanie: przez funkcję `refresh_player_rankings()` (CONCURRENTLY)

**Rekomendacja**: Odświeżanie co 5-15 minut przez Spring Scheduled job

---

## 12. Timeout pvp

### Automatyczne zakończenie gry

Gry pvp są automatycznie kończone po 20 sekundach nieaktywności:

1. **Funkcja**: `check_pvp_timeout()`
2. **Warunki**: 
   - `game_type = 'pvp'`
   - `status = 'in_progress'`
   - `last_move_at < NOW() - INTERVAL '20 seconds'`
3. **Akcje**:
   - Ustawia `status = 'finished'`
   - Ustawia `winner_id` na gracza, który nie timeout'ował
   - Ustawia `finished_at = NOW()`

**Rekomendacja**: Wywoływanie funkcji co 5-10 sekund przez Spring Scheduled job

---

## 13. Uwagi dotyczące wydajności i skalowalności

### Optymalizacje

1. **Indeksy**: Wszystkie zapytania rankingowe i matchmaking są zindeksowane
2. **Materialized view**: Ranking jest pre-obliczony w materialized view
3. **Partial indexes**: Indeksy na kolumnach nullable używają WHERE clause dla mniejszych indeksów
4. **Funkcje w bazie**: Logika biznesowa (obliczanie punktów, walidacja) redukuje obciążenie warstwy aplikacji

### Redis cache (opcjonalne)

Zgodnie z tech stack, sesje WebSocket i cache mogą być przechowywane w Redis (nie w PostgreSQL):
- Sesje WebSocket dla pvp
- Cache rankingu (dla jeszcze szybszych zapytań)
- Stan aktywnych gier

### Skalowalność

Schemat jest zaprojektowany dla:
- 100-500 jednoczesnych użytkowników (wymaganie PRD)
- Wysoka częstotliwość zapytań rankingowych
- Równoczesne gry pvp w czasie rzeczywistym

---

## 14. Decyzje projektowe

### Tabela users dla gości i zarejestrowanych z integracją Supabase Auth

**Uzasadnienie**: 
- Uproszczenie schematu, łatwiejsze zarządzanie, spójne API
- Goście są identyfikowani przez IP
- Zarejestrowani użytkownicy są zarządzani przez Supabase Auth (`auth.users`)
- Tabela `users` przechowuje dodatkowe dane profilowe (statystyki, punkty) powiązane z `auth.users` przez `auth_user_id`
- Autentykacja, hasła i email są zarządzane przez Supabase Auth, co zapewnia bezpieczeństwo i zgodność z best practices

### Stan planszy generowany dynamicznie

**Uzasadnienie**: Eliminuje redundancję danych, zapewnia spójność, łatwiejsze debugowanie (pełna historia ruchów).

### Ujednolicony model gier (vs_bot i pvp)

**Uzasadnienie**: Uproszczenie logiki aplikacji, spójne API, łatwiejsze zapytania.

### Materialized view dla rankingu

**Uzasadnienie**: Wydajność dla częstych zapytań rankingowych, pre-obliczona pozycja, możliwość odświeżania CONCURRENTLY bez blokowania.

### Row Level Security (RLS)

**Uzasadnienie**: Dodatkowa warstwa bezpieczeństwa na poziomie bazy danych, zgodność z best practices PostgreSQL.

---

## 15. Do wdrożenia w aplikacji Spring Boot

### Scheduled Jobs

1. **refresh_player_rankings()** - Odświeżanie rankingu co 5-15 minut
2. **check_pvp_timeout()** - Sprawdzanie timeout pvp co 5-10 sekund

### Konfiguracja RLS z Supabase Auth

**Dla zarejestrowanych użytkowników**:
- Supabase Auth automatycznie ustawia `auth.uid()` w kontekście zapytań
- Polityki RLS używają `auth.uid()` do identyfikacji użytkownika
- Aplikacja nie musi ręcznie ustawiać zmiennych sesji dla zalogowanych użytkowników

**Dla gości**:
- Aplikacja musi ustawić `current_setting('app.guest_user_id')` przed zapytaniami:
```sql
SET LOCAL app.guest_user_id = '<guest_user_id>';
```
- Gość jest identyfikowany przez IP i ma własny rekord w tabeli `users` z `is_guest = TRUE`

### Integracja z Supabase Auth

**Tworzenie profilu użytkownika**:
- Po rejestracji w Supabase Auth, aplikacja powinna utworzyć rekord w tabeli `users` z `auth_user_id` odpowiadającym `auth.users.id`
- Można użyć triggera w bazie danych lub webhook Supabase Auth do automatycznego tworzenia profilu

**Synchronizacja danych**:
- Email i dane autentykacyjne są w `auth.users` (zarządzane przez Supabase Auth)
- Dane profilowe (statystyki, punkty, username) są w tabeli `users`
- Zapytania mogą JOINować `users` z `auth.users` przez `auth_user_id = auth.users.id`

### Integracja z Redis

Sesje WebSocket i cache nie są częścią schematu PostgreSQL - powinny być obsługiwane przez Redis zgodnie z tech stack.

---

## 16. Dokumentacja migracji

Schemat jest wdrożony przez migracje Flyway:
- `V1__init_schema.sql` - Podstawowy schemat (tabele, enumy, indeksy, RLS, funkcje podstawowe)
- `V2__update_points_and_stats.sql` - Automatyczne aktualizacje (funkcje punktów, triggery, materialized view)

**Status**: ✅ Zaimplementowane

---

## 17. Podsumowanie

Schemat bazy danych PostgreSQL został zaprojektowany zgodnie z wymaganiami PRD i najlepszymi praktykami PostgreSQL. Obsługuje:

✅ System użytkowników (goście + zarejestrowani)  
✅ **Integracja z Supabase Auth** dla zarządzania użytkownikami zarejestrowanymi  
✅ Gry vs_bot (3 poziomy trudności) i pvp  
✅ System punktowy z automatyczną aktualizacją  
✅ Ranking globalny z materialized view  
✅ Timeout pvp (20 sekund nieaktywności)  
✅ Row Level Security dla bezpieczeństwa z integracją Supabase Auth  
✅ Wydajne zapytania (indeksy, materialized views)  
✅ Funkcje pomocnicze i triggery dla logiki biznesowej  

Schemat jest gotowy do użycia jako podstawa do tworzenia migracji baz danych i integracji z aplikacją Spring Boot oraz Supabase Auth.

