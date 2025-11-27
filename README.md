## World at War: Turn‑Based Strategy

![Build](https://img.shields.io/badge/build-passing-brightgreen)
![Status](https://img.shields.io/badge/status-active-blue)
![License](https://img.shields.io/badge/license-MIT-blue)

### Table of Contents
- [Project name](#world-at-war-turn-based-strategy)
- [Project description](#project-description)
- [Tech stack](#tech-stack)
- [Database schema](#database-schema)
- [Getting started locally](#getting-started-locally)
- [Available scripts](#available-scripts)
- [Testing](#testing)
- [Project scope](#project-scope)
- [Project status](#project-status)
- [License](#license)

## Project description
World at War is a modern, production‑grade web application for competitive, turn‑based gameplay. Players can battle an AI bot at three difficulty levels or face other players in real time over WebSocket, earning points and climbing a global ranking. The UI emphasizes high visual quality and responsiveness, with smooth animations. The architecture targets stability and scalability for roughly 100–500 concurrent users.

Key gameplay features for MVP focus on Tic‑Tac‑Toe boards (3x3, 4x4, 5x5), automated win/draw detection, validated moves, and a persistent global ranking with a clear scoring system.

**UI theming policy:** całe środowisko frontendowe korzysta wyłącznie z motywu PrimeNG Verona (`https://verona.primeng.org/`). Każdy nowy widok, komponent oraz poprawka stylów musi być projektowana i weryfikowana pod kątem zgodności z tym motywem.

For full product requirements, see the PRD: `.ai/prd.md`. For detailed technology choices and rationale, see the Tech Stack: `.ai/tech-stack.md`.

## Tech stack
- **Frontend**: Angular 17, TypeScript 5, SCSS, Angular Animations, PrimeNG; ESLint + Prettier; Jest + Angular Testing Library; Cypress
- **Backend**: Java 21 + Spring Boot 3.x (monolith), Spring Security (JWT z blacklistą w Redis), BCrypt, Spring WebSocket
- **Data/Infra**: **Supabase** (PostgreSQL 17), Redis 7.x (cache, sesje, blacklista tokenów)
- **Quality/Observability**: SonarCloud, Spring Actuator (+ Prometheus metrics) + Grafana, Swagger/OpenAPI
- **DevOps**: Docker + docker‑compose, GitHub Actions CI/CD

References:
- PRD: `.ai/prd.md`
- Tech stack: `.ai/tech-stack.md`
- Database schema: [DATABASE_DIAGRAM.md](DATABASE_DIAGRAM.md)

## Database schema

**Szybki podgląd schematu bazy danych:** [DATABASE_DIAGRAM.md](DATABASE_DIAGRAM.md)

Projekt używa **Supabase** (PostgreSQL 17) z automatycznymi migracjami. Schemat obejmuje:
- Tabele: `users`, `games`, `moves`
- Relacje: USERS 1:N GAMES, USERS 1:N MOVES, GAMES 1:N MOVES
- Automatyzacje: triggery dla statystyk, timeout pvp, materialized views dla rankingu
- Row Level Security (RLS) z integracją Supabase Auth
- Szczegółowa dokumentacja: [supabase/migrations/README.md](supabase/migrations/README.md)

### Dostępne usługi Supabase (lokalnie)

Po uruchomieniu `npx supabase start`:

- **API URL**: http://127.0.0.1:54321
- **Database URL**: postgresql://postgres:postgres@127.0.0.1:54322/postgres
- **Studio URL**: http://127.0.0.1:54323
- **GraphQL URL**: http://127.0.0.1:54321/graphql/v1
- **Storage URL**: http://127.0.0.1:54321/storage/v1/s3
- **Mailpit URL**: http://127.0.0.1:54324

Sprawdź status: `npx supabase status`

## Getting started locally

### Prerequisites
- Node.js 18+ and npm
- Java 21 (JDK)
- Supabase CLI: `npx supabase` (global install not supported)
- Redis 7.x
- Angular CLI (recommended): `npm i -g @angular/cli`
- Optional: Docker and docker‑compose

### Database (Supabase PostgreSQL)
1. Uruchom lokalną instancję Supabase:
   ```bash
   npx supabase start
   ```
2. To automatycznie uruchomi PostgreSQL, API, Auth, Storage i inne usługi
3. Sprawdź status: `npx supabase status`
4. Dane dostępowe: zobacz sekcję "Dostępne usługi Supabase" powyżej

### Redis
Redis jest używany do cache, sesji i blacklisty tokenów JWT. Uruchom lokalną instancję Redis (np. przez Docker):
```bash
docker run --name waw-redis -p 6379:6379 -d redis:7
```

**Uwaga:** Redis jest wymagany dla działania blacklisty tokenów JWT (przy wylogowaniu).

### Backend (Spring Boot 3.x)
1. Uruchom backend (PowerShell):
   ```powershell
   # Zalecane: użyj skryptu automatyzującego
   .\run-backend.ps1 start
   ```
   
   Skrypt automatycznie:
   - Sprawdzi i uruchomi Supabase (PostgreSQL + Redis)
   - Zbuduje backend
   - Uruchomi aplikację Spring Boot
   - Wyświetli linki do dokumentacji API
   
2. Ręczne uruchomienie:
   - Windows (PowerShell):
     ```powershell
     cd backend
     .\gradlew.bat bootRun
     ```
   - macOS/Linux:
     ```bash
     cd backend
     ./gradlew bootRun
     ```
3. Aplikacja automatycznie łączy się z Supabase (port 54322) i Redis

### Frontend (Angular 17)
1. Install dependencies and run the dev server:
   ```bash
   cd frontend
   npm install
   npm start
   ```
2. Open the app at `http://localhost:4200/`. The app reloads on source changes.

## Available scripts

### Frontend package scripts
From `frontend/package.json`:
- `npm start`: start the Angular dev server (`ng serve`)
- `npm run build`: production build (`ng build`)
- `npm run watch`: watch mode build (`ng build --watch --configuration development`)
- `npm test`: run unit tests (`ng test`)
- `npm run ng <cmd>`: access Angular CLI

### Backend (Gradle)
- **`.\run-backend.ps1 start`** - uruchomienie BE z automatycznym uruchomieniem bazy i Redis
- **`.\run-backend.ps1 restart`** - restart z przebudową i zastosowaniem migracji
- **`.\run-backend.ps1 status`** - sprawdzenie statusu wszystkich serwisów (Supabase, Redis, Backend, Java)
- **`.\run-backend.ps1 logs`** - wyświetlenie ostatnich logów
- **`.\run-backend.ps1 stop`** - zatrzymanie backendu
- `gradlew bootRun` / `./gradlew bootRun`: run the Spring Boot application (ręcznie)
- Additional Gradle references are listed in `backend/HELP.md`

## Testing

### Zakres testów
- **Frontend (Angular + PrimeNG Verona):** logowanie/rejestracja, wybór trybu gry, przebieg ruchów (timery, surrender, reconnect), ranking, responsywność i WCAG w motywie Verona.
- **Backend (Spring Boot + WebSocket):** auth i ranking REST, matchmaking PvE/PvP, walidacja ruchów, timer usługowy, obsługa reconnectów WebSocket, aktualizacja rankingu i blacklisty JWT w Redis.
- **Baza danych i Supabase:** migracje Flyway na czystej/istniejącej bazie, dane referencyjne rankingu, rollbacki, RLS.
- **Automatyzacja i infrastruktura:** skrypty PowerShell do smoke/E2E backendu, pipeline GitHub Actions (lint → unit → integracyjne → Cypress), monitoring Actuator + Prometheus.

### Typy testów
- **Statyczne:** ESLint + Prettier dla Angulara, Checkstyle + SonarCloud dla Javy.
- **Jednostkowe frontend:** Jest + Angular Testing Library (komponenty, usługi WebSocket, guardy).
- **Jednostkowe backend:** JUnit 5 + Mockito (logika gry, ranking, auth).
- **Integracyjne backend:** Spring Boot Test + Testcontainers (PostgreSQL/Redis), RestAssured/Spring MockMvc.
- **Testy kontraktowe:** OpenAPI oraz kontrakty komunikatów WebSocket.
- **E2E frontend:** Cypress (logowanie → mecz PvE/PvP → ranking, reconnect, surrender).
- **E2E backend:** PowerShell (`backend/test-pvp-match.ps1`, `test-bot-move.ps1`, `test-win-bot-and-check-ranking.ps1`) oraz `test-pvp-websocket.ps1`.
- **Wydajnościowe i chaos:** k6/Gatling dla API/WebSocket, scenariusze restartów Redis/PostgreSQL.
- **Bezpieczeństwa:** OWASP ZAP, snyk, dependency-check, audyt JWT/WebSocket.
- **Smoke/regresja:** zestaw lint → unit → integracje → skrócone e2e odpalany cyklicznie, z raportowaniem w Allure.

### Narzędzia testowe i raportowanie
- **Frontend:** Jest, Angular Testing Library, Cypress, Storybook Visual Regression (opcjonalnie).
- **Backend:** JUnit 5, Mockito, Testcontainers, WireMock, RestAssured, Spring MockMvc.
- **Jakość/statyczne:** ESLint, Prettier, Checkstyle, SonarCloud.
- **Wydajność/niezawodność:** k6, Gatling, docker-compose, GitHub Actions runners.
- **Bezpieczeństwo:** OWASP ZAP, snyk, dependency-check.
- **Raporty i obserwowalność:** Allure, Prometheus + Grafana, Spring Actuator health.

### Komendy do uruchamiania testów

#### Frontend - Testy jednostkowe (Jest + Angular Testing Library)

```bash
cd frontend

# Wszystkie testy
npm test

# Tryb watch (automatyczne uruchamianie przy zmianach)
npm run test:watch

# Testy z raportem pokrycia
npm run test:coverage

# Testy w trybie CI
npm run test:ci
```

#### Frontend - Testy E2E (Cypress)

```bash
cd frontend

# Uruchomienie testów E2E (headless)
npm run e2e

# Interaktywny tryb Cypress
npm run e2e:open

# Uruchomienie w trybie headless
npm run e2e:headless
```

**Uwaga:** Aplikacja frontendowa musi być uruchomiona na `http://localhost:4200` oraz backend musi być dostępny.

#### Backend - Testy jednostkowe (JUnit 5 + Mockito)

```bash
cd backend

# Wszystkie testy
./gradlew test              # macOS/Linux
gradlew.bat test            # Windows

# Tylko testy jednostkowe (oznaczone tagiem @Tag("unit"))
./gradlew testUnit          # macOS/Linux
gradlew.bat testUnit        # Windows

# Tylko testy integracyjne (oznaczone tagiem @Tag("integration"))
./gradlew testIntegration   # macOS/Linux
gradlew.bat testIntegration # Windows

# Testy z raportem
./gradlew test --info       # macOS/Linux
gradlew.bat test --info     # Windows
```

**Uwaga:** Testy integracyjne wymagają uruchomionego Docker Desktop (Testcontainers).

#### Backend - Testy E2E (PowerShell smoke PvP/PvE)

```powershell
cd backend

# Test meczu PvP
.\test-pvp-match.ps1

# Test ruchu bota
.\test-bot-move.ps1

# Test wygranej z botem i sprawdzenie rankingu
.\test-win-bot-and-check-ranking.ps1

# Test WebSocket PvP
.\test-pvp-websocket.ps1
```

#### Raporty testów

- **Frontend coverage:** `frontend/coverage/index.html`
- **Backend reports:** `backend/build/reports/tests/test/index.html`
- **Cypress videos/screenshots:** `frontend/cypress/videos/` i `frontend/cypress/screenshots/`

#### Pipeline CI/CD

GitHub Actions uruchamia sekwencję: lint → unit → integracja → Cypress → publikacja raportu SonarCloud po każdym merge request. Wyniki testów dostępne w artefaktach i Allure.

Pełny harmonogram i kryteria opisano w `.ai/test-plan.md`. Szczegółowa dokumentacja testów: [TESTING.md](TESTING.md).

## API Endpoints

### Rankings

- **GET /api/v1/rankings** - Pobranie globalnego rankingu z paginacją
  - Query params: `page`, `size`, `startRank`
  - Szczegóły: [`.ai/implementation-plans/ranking/get-rankings.md`](.ai/implementation-plans/ranking/get-rankings.md)

- **GET /api/v1/rankings/{userId}** - Pobranie szczegółowej pozycji w rankingu dla użytkownika
  - Path param: `userId`
  - Szczegóły: [`.ai/implementation-plans/ranking/get-rankings-userId.md`](.ai/implementation-plans/ranking/get-rankings-userId.md)

- **GET /api/v1/rankings/around/{userId}** - Pobranie rankingów wokół użytkownika
  - Path param: `userId`
  - Query param: `range` (default: 5, max: 10)
  - Szczegóły: [`.ai/implementation-plans/ranking/get-rankings-around-userId.md`](.ai/implementation-plans/ranking/get-rankings-around-userId.md)

- **DELETE /api/v1/rankings/cache** - Czyszczenie cache rankingów z Redis
  - Automatycznie wywoływany przez `run-backend.ps1` przy opcjach `start` i `restart`
  - Szczegóły: [`.ai/implementation-plans/ranking/clear-rankings-cache.md`](.ai/implementation-plans/ranking/clear-rankings-cache.md)

Pełna dokumentacja API dostępna w Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Project scope

### In scope (MVP)
- Tic‑Tac‑Toe gameplay on 3x3, 4x4, 5x5 boards
- Auto detection of win/loss/draw and move validation
- Guest mode (identified by IP) and user registration/login
- AI bot with 3 difficulty levels (easy/medium/hard)
- Real‑time PvP over WebSocket with 10‑second move timer and ability to surrender
- Auto‑save for single‑player games; PvP ends after 20 seconds of inactivity
- Scoring system and persistent global ranking; profile view with basic stats

### Out of scope (for MVP)
- Advanced strategy mechanics beyond Tic‑Tac‑Toe
- Email notifications, friends/invites, in‑game chat, profile personalization
- Advanced security/analytics beyond essentials

## Project status
- Status: Active development; MVP in progress
- Target scale: 100–500 concurrent users
- CI/CD: GitHub Actions pipeline (`.github/workflows/ci.yml`) running Angular and Spring Boot builds/tests on every push/PR
- Monitoring: Spring Actuator + Prometheus + Grafana (planned/ongoing)
- API docs: Swagger/OpenAPI (planned/ongoing)

For broader product goals and success metrics, see `.ai/prd.md`.

## License
MIT © 2025 Łukasz Zieliński

