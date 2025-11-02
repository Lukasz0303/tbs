# Migracje bazy danych - World at War: Turn-Based Strategy

Ten folder zawiera migracje bazy danych dla aplikacji World at War: Turn-Based Strategy utworzone przez Supabase CLI.

## Struktura migracji

### 1. `20251101140303_create_enums_and_tables.sql`
Podstawowy schemat bazy danych:
- **Kolumny z CHECK constraints**: `game_type`, `game_status`, `bot_difficulty`, `player_symbol` (użycie VARCHAR zamiast PostgreSQL ENUM dla kompatybilności z Hibernate)
- **Tabele**: `users`, `games`, `moves`
- **Indeksy**: optymalizacja zapytań rankingowych i filtrowania
- **RLS**: Row Level Security włączony dla wszystkich tabel
- **Polityki RLS**: granitowe polityki dla bezpiecznego dostępu do danych

### 2. `20251101140403_create_functions_and_triggers.sql`
Automatyczne aktualizacje i funkcje pomocnicze:
- **Funkcje pomocnicze**: generowanie stanu planszy, walidacja ruchów, obliczanie punktów
- **Funkcje systemowe**: timeout PvP, ranking, automatyczne odświeżanie
- **Triggery**: automatyczna aktualizacja `updated_at`, `last_move_at`, statystyk użytkowników
- **Widoki**: `game_summary` (zwykły), `player_rankings` (materialized view)

### 3. `20251101141720_disable_rls_for_local_dev.sql` ⚠️
**TYMCZASOWE** wyłączenie RLS dla lokalnego developementu:
- Wyłącza Row Level Security dla wszystkich tabel (`users`, `games`, `moves`)
- Pozwala na swobodne programowanie API bez ograniczeń dostępu
- **UWAGA**: Tylko dla środowiska lokalnego! Nie deployować na produkcję!

### 4. `20251101150000_add_email_and_password_to_users.sql` ⚠️
**TYMCZASOWA** implementacja autoryzacji bez Supabase Auth:
- Dodaje pola `email` i `password_hash` do tabeli `users`
- Aktualizuje constraint `users_registered_check` dla nowych pól
- Tworzy indeks na `email` dla szybkich wyszukiwań
- **UWAGA**: Tylko dla lokalnego developmentu! Na produkcji użyj Supabase Auth!

## Kluczowe funkcjonalności

### System użytkowników
- **Goście**: identyfikacja przez IP, `is_guest = true`
- **Zarejestrowani**: integracja z Supabase Auth, `auth_user_id → auth.users.id`
- **Walidacja**: constraints zapewniające poprawność danych

### System gier
- **vs_bot**: gra z botem (3 poziomy trudności: easy=100pkt, medium=500pkt, hard=1000pkt)
- **pvp**: gra z innym graczem (+1000 punktów)
- **Plansze**: 3x3, 4x4, 5x5
- **Statusy**: waiting, in_progress, finished, abandoned, draw

> **Uwaga:** Kolumny enum użyte w aplikacji (game_type, status, bot_difficulty, player_symbol) są typu VARCHAR z CHECK constraints

### System punktowy
- **Automatyczna aktualizacja**: przez trigger `update_user_stats_on_game_finished`
- **Zasady**: pvp=1000pkt, vs_bot easy=100pkt, medium=500pkt, hard=1000pkt
- **Ranking**: oparty na `total_points`, tylko zarejestrowani użytkownicy

### Row Level Security (RLS)
- **Zarejestrowani**: dostęp przez `auth.uid()` automatycznie ustawiany przez Supabase Auth
- **Goście**: dostęp przez aplikację (polityki anon zablokowane dla gości)
- **Bezpieczeństwo**: polityki granularne (SELECT, INSERT, UPDATE) dla każdej roli

⚠️ **UWAGA ŚRODOWISKOWA**: 
- **Lokalnie**: RLS jest **WYŁĄCZONE** dla łatwiejszego programowania API
- **Produkcja**: RLS **MUSI** być włączone dla bezpieczeństwa
- Migracja `20251101141720_disable_rls_for_local_dev.sql` jest tylko dla lokalnego dev

## Uruchamianie migracji

### Lokalnie (Supabase CLI)

```bash
# Uruchom lokalną instancję Supabase
supabase start

# Zastosuj migracje
supabase db reset

# Lub tylko nowe migracje
supabase migration up

# Sprawdź listę zastosowanych migracji (alternatywy):

# Opcja 1: Przez Docker (najprostsza)
docker exec supabase_db_tbs psql -U postgres -d postgres -c "SELECT version, name FROM supabase_migrations.schema_migrations ORDER BY version;"

# Opcja 2: W Supabase Studio (GUI)
# Otwórz http://127.0.0.1:54323 → SQL Editor → uruchom:
# SELECT version, name FROM supabase_migrations.schema_migrations ORDER BY version;

# Opcja 3: Przez psql (jeśli zainstalowane)
# psql postgresql://postgres:postgres@127.0.0.1:54322/postgres -c "SELECT version, name FROM supabase_migrations.schema_migrations ORDER BY version;"
```

### Produkcja (Supabase Dashboard)

⚠️ **WAŻNE**: Przed deploy na produkcję **USUŃ** migrację `20251101141720_disable_rls_for_local_dev.sql` lub utwórz migrację przywracającą RLS!

1. Przejdź do **Database** → **Migrations** w Supabase Dashboard
2. Prześlij pliki migracji lub użyj Supabase CLI:

```bash
# Połącz się z zdalnym projektem
supabase link --project-ref your-project-ref

# Wyślij migracje do produkcji
supabase db push
```

3. Jeśli RLS jest wyłączone, przywróć je:

```sql
-- Przywróć RLS na produkcji
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.games ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.moves ENABLE ROW LEVEL SECURITY;
```

## Funkcje do wywoływania przez aplikację

### Scheduled Jobs (Spring Boot)
Te funkcje powinny być wywoływane okresowo przez Spring Scheduled:

```sql
-- Check PvP timeout (co 5-10 sekund)
SELECT check_pvp_timeout();

-- Refresh rankings (co 5-15 minut)
SELECT refresh_player_rankings();
```

### Funkcje pomocnicze
```sql
-- Generuj stan planszy
SELECT generate_board_state(game_id);

-- Waliduj ruch
SELECT is_move_valid(game_id, row, col);

-- Oblicz punkty
SELECT calculate_game_points(game_type, bot_difficulty);

-- Pobierz pozycję w rankingu
SELECT get_user_ranking_position(user_id);
```

## Uwagi

### Integracja z Supabase Auth (lokalnie wyłączona)
⚠️ **Aktualna implementacja (lokalnie):**
- Autoryzacja przez Spring Security z JWT (bez Supabase Auth)
- Email i password_hash są w tabeli `users` (tylko dla lokalnego developmentu)
- Hasła hashowane przez BCrypt w aplikacji Spring Boot
- Tokeny JWT z blacklistą w Redis
- `auth_user_id` nullable (dla przyszłej integracji z Supabase Auth)

**Na produkcji:**
- Użyj Supabase Auth zamiast własnej implementacji
- Tabela `auth.users` będzie zarządzana przez Supabase Auth
- RLS użyje `auth.uid()` dla automatycznej identyfikacji zarejestrowanych użytkowników

### Goście
- Identyfikowani przez IP i mają własny rekord w tabeli `users`
- Polityki RLS dla anon są zablokowane - dostęp przez aplikację
- Aplikacja powinna zarządzać sesjami gości (Redis/niekoniecznie baza danych)

### Redis Cache
- **Blacklista tokenów JWT** - tokeny wylogowanych użytkowników są przechowywane w Redis
- Sesje WebSocket dla PvP powinny być w Redis
- Cache rankingu (opcjonalnie) dla szybszych zapytań
- Stan aktywnych gier może być cache'owany w Redis

## Wymagania wydajnościowe

Schemat jest zoptymalizowany dla:
- **100-500** jednoczesnych użytkowników
- Wysoka częstotliwość zapytań rankingowych
- Równoczesne gry PvP w czasie rzeczywistym

Optymalizacje:
- Indeksy na wszystkich kluczowych kolumnach
- Materialized view dla rankingu (pre-obliczony)
- Partial indexes dla mniejszych indeksów
- Funkcje w bazie danych redukują obciążenie aplikacji

## Dokumentacja

Pełna dokumentacja schematu bazy danych znajduje się w `.ai/db-plan.md`.

