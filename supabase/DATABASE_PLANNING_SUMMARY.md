# Podsumowanie planowania bazy danych - World at War: Turn-Based Strategy

## <conversation_summary>

### <decisions>

1. **Tabela users z obsługą gości i zarejestrowanych użytkowników**
   - Zdecydowano użyć jednej tabeli `users` z flagą `is_guest` (boolean)
   - Kolumna `ip_address` (inet) dla identyfikacji gości
   - Constraint zapewniający poprawność danych (goście mają IP, zarejestrowani mają email/hasło)

2. **Tabela moves z generowaniem stanu planszy z historii ruchów**
   - Stan planszy NIE jest przechowywany w tabeli games
   - Stan planszy jest generowany dynamicznie z tabeli `moves` za pomocą funkcji `generate_board_state()`
   - Tabela moves zawiera: `game_id`, `row`, `col`, `player_symbol`, `move_order`, `created_at`

3. **Tabela games z ujednoliconym modelem dla VS_BOT i PvP**
   - Jedna tabela `games` z kolumną `game_type` (enum: VS_BOT, PVP)
   - Kolumny `player1_id`, `player2_id` (nullable dla VS_BOT)
   - Kolumna `bot_difficulty` tylko dla VS_BOT
   - Bez osobnych tabel dla różnych typów gier

4. **System punktowy z automatyczną aktualizacją**
   - Trigger `update_user_stats_on_game_finished` automatycznie aktualizuje statystyki
   - Funkcja `calculate_game_points()` oblicza punkty na podstawie typu gry i trudności
   - Aktualizacja `total_points`, `games_played`, `games_won` po zakończeniu gry

5. **Materialized view dla rankingu**
   - `player_rankings` dla szybkich zapytań rankingowych
   - Odświeżanie przez funkcję `refresh_player_rankings()` (do użycia przez job)

6. **Funkcja timeoutu PvP**
   - Funkcja `check_pvp_timeout()` sprawdza gry przekraczające 20 sekund nieaktywności
   - Automatycznie ustawia zwycięzcę (gracz, który nie timeout'ował)

7. **Row Level Security (RLS)**
   - Wszystkie tabele mają włączony RLS
   - Polityki SELECT, INSERT, UPDATE dla bezpieczeństwa danych

8. **Środowisko Windows (PowerShell)**
   - Wszystkie instrukcje i skrypty dostosowane do Windows
   - Użycie `wsl` dla skryptów bash (`migration-helper.sh`)
   - Komendy PowerShell dla wszystkich operacji Docker

</decisions>

### <matched_recommendations>

1. ✅ **Tabela users z is_guest i ip_address** - Zaimplementowano zgodnie z rekomendacją
   - Jedna tabela dla gości i zarejestrowanych użytkowników
   - Constraint zapewniający poprawność danych

2. ✅ **Tabela moves z generowaniem stanu planszy** - Zaimplementowano zgodnie z rekomendacją
   - Stan planszy generowany z historii ruchów (funkcja `generate_board_state()`)
   - Historia ruchów w tabeli `moves` z `move_order` dla odtworzenia sekwencji

3. ✅ **Tabela games z game_type enum** - Zaimplementowano zgodnie z rekomendacją
   - Ujednolicony model dla VS_BOT i PvP
   - `player2_id` nullable dla VS_BOT
   - `bot_difficulty` tylko dla VS_BOT

4. ✅ **System punktowy z total_points (BIGINT)** - Zaimplementowano zgodnie z rekomendacją
   - Indeks DESC na `total_points` dla wydajnych zapytań rankingowych
   - Automatyczna aktualizacja przez trigger

5. ✅ **Materialized view dla rankingu** - Zaimplementowano zgodnie z rekomendacją
   - `player_rankings` z pozycją w rankingu
   - Funkcja do odświeżania

6. ✅ **Funkcja timeoutu PvP** - Zaimplementowano zgodnie z rekomendacją
   - `check_pvp_timeout()` sprawdzająca 20 sekund nieaktywności
   - Automatyczne ustawienie zwycięzcy

7. ✅ **Funkcje pomocnicze** - Zaimplementowano zgodnie z rekomendacjami
   - `generate_board_state()` - generuje stan planszy
   - `is_move_valid()` - waliduje ruch
   - `calculate_game_points()` - oblicza punkty
   - `get_user_ranking_position()` - pozycja w rankingu

8. ✅ **Row Level Security (RLS)** - Zaimplementowano zgodnie z rekomendacją
   - Polityki SELECT, INSERT, UPDATE dla wszystkich tabel
   - Bezpieczeństwo na poziomie wierszy

9. ✅ **Indeksy dla wydajności** - Zaimplementowano zgodnie z rekomendacjami
   - Indeksy na `total_points` (DESC), `last_seen_at`, `status`, `game_type`, itp.
   - Optymalizacja zapytań rankingowych i matchmakingu

10. ✅ **Triggery dla integralności danych** - Zaimplementowano zgodnie z rekomendacją
    - Automatyczna aktualizacja `updated_at`
    - Automatyczna aktualizacja `last_move_at`
    - Automatyczna aktualizacja statystyk użytkownika

</matched_recommendations>

### <database_planning_summary>

**Główne wymagania dotyczące schematu bazy danych:**

Schemat bazy danych PostgreSQL został zaprojektowany zgodnie z wymaganiami PRD dla MVP. Baza obsługuje:

1. **System użytkowników:**
   - Użytkownicy zarejestrowani (email, username, password_hash) - autoryzacja przez JWT (lokalnie)
   - Użytkownicy goście (identyfikacja przez IP)
   - Statystyki graczy (punkty, rozegrane gry, wygrane)
   - Hasła hashowane przez BCrypt w aplikacji Spring Boot

2. **System gier:**
   - Gry VS_BOT (3 poziomy trudności: EASY=100pkt, MEDIUM=500pkt, HARD=1000pkt)
   - Gry PvP (1000pkt za wygraną, timeout 20 sekund)
   - Plansze 3x3, 4x4, 5x5
   - Statusy: WAITING, IN_PROGRESS, FINISHED, ABANDONED, DRAW

3. **System punktowy:**
   - Automatyczna aktualizacja punktów po zakończeniu gry
   - Ranking globalny (permanentny)
   - Materialized view dla wydajnych zapytań rankingowych

**Kluczowe encje i ich relacje:**

1. **USERS** (1:N) → **GAMES** (relacja przez player1_id, player2_id, winner_id)
   - Użytkownik może być graczem 1 w wielu grach
   - Użytkownik może być graczem 2 w wielu grach (tylko PvP)
   - Użytkownik może wygrać wiele gier

2. **GAMES** (1:N) → **MOVES** (relacja przez game_id)
   - Gra może mieć wiele ruchów
   - Ruchy są uporządkowane przez `move_order`

3. **USERS** (1:N) → **MOVES** (relacja przez player_id)
   - Użytkownik może wykonać wiele ruchów
   - player_id nullable dla ruchów bota

**Ważne kwestie dotyczące bezpieczeństwa:**

1. **Row Level Security (RLS):**
   - Włączony na wszystkich tabelach (users, games, moves)
   - Polityki SELECT: użytkownicy widzą swoje dane i gry, w których uczestniczą
   - Polityki INSERT/UPDATE: użytkownicy mogą tworzyć gry i dodawać ruchy tylko w swoich grach

2. **Integralność danych:**
   - Constraints zapewniające poprawność danych (np. goście mają IP, zarejestrowani mają email)
   - Foreign keys z odpowiednimi akcjami ON DELETE (CASCADE, SET NULL)
   - Triggery automatycznie aktualizujące powiązane dane

**Ważne kwestie dotyczące skalowalności:**

1. **Indeksowanie:**
   - Indeksy na kolumnach używanych w zapytaniach (total_points DESC, last_seen_at, status, game_type)
   - Indeksy złożone dla optymalizacji zapytań rankingowych i matchmakingu

2. **Materialized views:**
   - `player_rankings` dla szybkich zapytań rankingowych
   - Możliwość okresowego odświeżania przez job w tle

3. **Funkcje i triggery:**
   - Logika biznesowa w bazie danych (obliczanie punktów, aktualizacja statystyk)
   - Redukcja obciążenia warstwy aplikacji

**Utworzone komponenty:**

1. **Migracje:**
   - `V1__init_schema.sql` - podstawowy schemat (tabele, enumy, indeksy, RLS, funkcje podstawowe)
   - `V2__update_points_and_stats.sql` - automatyczne aktualizacje (funkcje punktów, triggery, materialized view)

2. **Tabele:**
   - `users` - użytkownicy i goście
   - `games` - gry VS_BOT i PvP
   - `moves` - historia ruchów

3. **Funkcje:**
   - `generate_board_state()` - generuje stan planszy z historii
   - `is_move_valid()` - waliduje ruch
   - `calculate_game_points()` - oblicza punkty
   - `check_pvp_timeout()` - sprawdza timeout PvP
   - `get_user_ranking_position()` - pozycja w rankingu
   - `refresh_player_rankings()` - odświeża ranking

4. **Triggery:**
   - `update_users_updated_at` - aktualizacja timestamp
   - `update_games_updated_at` - aktualizacja timestamp
   - `update_game_last_move_timestamp` - aktualizacja last_move_at
   - `update_user_stats_on_game_finished` - automatyczna aktualizacja statystyk

5. **Widoki:**
   - `game_summary` - widok z informacjami o grach i graczach
   - `player_rankings` (materialized view) - ranking graczy

6. **Narzędzia pomocnicze:**
   - `migration-helper.sh` - skrypt do zarządzania migracjami (status, new)
   - `DATABASE_SCHEMA.md` - dokumentacja schematu z diagramem ERD
   - `MIGRATION_INSTRUCTIONS.md` - instrukcje uruchomienia migracji

</database_planning_summary>

### <unresolved_issues>

1. **Odświeżanie materialized view player_rankings:**
   - Zdecydowano utworzyć materialized view, ale nie określono częstotliwości odświeżania
   - **Rozwiązanie:** Utworzyć Spring Scheduled job wywołujący `refresh_player_rankings()` co X minut/godzin

2. **Wywoływanie funkcji check_pvp_timeout():**
   - Funkcja timeoutu PvP została utworzona, ale nie określono jak będzie wywoływana
   - **Rozwiązanie:** Utworzyć Spring Scheduled job wywołujący `check_pvp_timeout()` co 5-10 sekund

3. **Zarządzanie sesjami WebSocket:**
   - PRD wymaga obsługi WebSocket dla PvP, ale schemat bazy nie zawiera tabeli sesji
   - **Uwaga:** Sesje mogą być przechowywane w Redis (jak wspomniano w tech stack), nie w PostgreSQL

4. **Historia gier:**
   - Schemat nie zawiera dedykowanej tabeli historii gier (można odczytać z tabeli games)
   - **Uwaga:** Dla MVP może wystarczyć, ale w przyszłości może być potrzebna tabela `game_history` dla lepszego audytu

5. **Optymalizacja zapytań rankingowych:**
   - Materialized view `player_rankings` wymaga okresowego odświeżania
   - **Uwaga:** Może być potrzebna strategia cache'owania w Redis dla jeszcze szybszych zapytań

</unresolved_issues>

## </conversation_summary>

## Status implementacji

✅ **Zrealizowane:**
- Podstawowy schemat bazy danych (V1)
- Automatyczne aktualizacje punktów i statystyk (V2)
- Funkcje pomocnicze i triggery
- Row Level Security
- Dokumentacja i narzędzia pomocnicze

⚠️ **Do wdrożenia w aplikacji Spring Boot:**
- Scheduled job dla `refresh_player_rankings()`
- Scheduled job dla `check_pvp_timeout()`
- Obsługa RLS policies w Spring Security (ustawianie `current_setting('app.user_id')`)

✅ **Zaimplementowane w aplikacji Spring Boot:**
- Autoryzacja JWT z blacklistą tokenów w Redis
- Hashowanie haseł przez BCrypt
- Endpointy: `/api/auth/register`, `/api/auth/login`, `/api/auth/me`, `/api/auth/logout`
- Integracja z tabelą `users` (email, password_hash dla zarejestrowanych)

**Następne kroki:**
1. Integracja schematu bazy danych z aplikacją Spring Boot
2. Implementacja Scheduled jobs dla automatycznego przetwarzania
3. Konfiguracja Spring Security dla RLS
4. Testy jednostkowe i integracyjne migracji

