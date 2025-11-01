# Instrukcja konfiguracji Supabase

## Wymagania

- **WSL2** (Windows Subsystem for Linux 2) - wymagany dla Docker Desktop na Windows
- **Docker Desktop** (musi być zainstalowany i uruchomiony)
- Node.js 18+ i npm 8+
- Supabase CLI (używamy przez `npx`, więc nie wymaga globalnej instalacji)

## Krok 0: Przygotowanie Docker Desktop

**WAŻNE:** Jeśli Docker Desktop nie startuje lub wyświetla błąd, najpierw napraw Docker Desktop zgodnie z instrukcją w [DOCKER_SETUP.md](DOCKER_SETUP.md).

### Wymagania dla Windows:
- Docker Desktop na Windows wymaga WSL2
- Jeśli WSL2 nie jest zainstalowany, zobacz instrukcję instalacji w [DOCKER_SETUP.md](DOCKER_SETUP.md)

## Krok 1: Uruchomienie Docker Desktop

Upewnij się, że Docker Desktop jest uruchomiony przed próbą uruchomienia Supabase.

**Sprawdzenie:**
```bash
docker ps
```

Jeśli polecenie zwraca błąd "Docker Desktop is unable to start", zobacz [DOCKER_SETUP.md](DOCKER_SETUP.md).

## Krok 2: Inicjalizacja projektu Supabase

Projekt Supabase został już zainicjalizowany. Katalog `supabase/` zawiera konfigurację.

## Krok 3: Uruchomienie lokalnej instancji Supabase

W katalogu głównym projektu uruchom:

```bash
npx supabase start
```

To polecenie uruchomi lokalną instancję Supabase z następującymi serwisami:
- **API**: http://127.0.0.1:54321
- **Database**: localhost:54322
- **Studio** (interfejs zarządzania): http://127.0.0.1:54323
- **Inbucket** (serwer email testowy): http://127.0.0.1:54324

## Krok 4: Sprawdzenie statusu

Sprawdź status instancji:

```bash
npx supabase status
```

## Krok 5: Zatrzymanie instancji

Aby zatrzymać lokalną instancję:

```bash
npx supabase stop
```

## Konfiguracja aplikacji

### Backend (Spring Boot)

Backend jest już skonfigurowany do połączenia z lokalną bazą Supabase:
- **URL**: `jdbc:postgresql://127.0.0.1:54322/postgres`
- **Username**: `postgres`
- **Password**: `postgres`

Konfiguracja znajduje się w `backend/src/main/resources/application.properties`.

### Frontend (Angular)

Frontend jest skonfigurowany do używania lokalnego API Supabase:
- **Supabase URL**: `http://127.0.0.1:54321`
- **Anon Key**: Klucz anonimowy (już skonfigurowany w `environment.ts`)

## Migracje bazy danych

Migracje Flyway znajdują się w `backend/src/main/resources/db/migration/`.

Po uruchomieniu Supabase, migracje zostaną automatycznie wykonane podczas startu aplikacji Spring Boot.

## Dostęp do Supabase Studio

Po uruchomieniu `npx supabase start`, możesz otworzyć Supabase Studio pod adresem:
http://127.0.0.1:54323

Studio umożliwia:
- Przeglądanie i zarządzanie tabelami
- Edycję danych
- Wyświetlanie logów
- Konfigurację autentykacji

## Uwagi

- Dane w lokalnej instancji Supabase są przechowywane w kontenerach Docker
- Aby zresetować bazę danych: `npx supabase db reset`
- Hasła domyślne można znaleźć po uruchomieniu `npx supabase status`

