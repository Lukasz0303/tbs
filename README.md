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
- [Project scope](#project-scope)
- [Project status](#project-status)
- [License](#license)

## Project description
World at War is a modern, production‑grade web application for competitive, turn‑based gameplay. Players can battle an AI bot at three difficulty levels or face other players in real time over WebSocket, earning points and climbing a global ranking. The UI emphasizes high visual quality and responsiveness, with smooth animations. The architecture targets stability and scalability for roughly 100–500 concurrent users.

Key gameplay features for MVP focus on Tic‑Tac‑Toe boards (3x3, 4x4, 5x5), automated win/draw detection, validated moves, and a persistent global ranking with a clear scoring system.

For full product requirements, see the PRD: `.ai/prd.md`. For detailed technology choices and rationale, see the Tech Stack: `.ai/tech-stack.md`.

## Tech stack
- **Frontend**: Angular 17, TypeScript 5, SCSS, Angular Animations, PrimeNG; ESLint + Prettier; Jest + Angular Testing Library; Cypress
- **Backend**: Java 21 + Spring Boot 3.x (monolith), Spring Security (JWT/OAuth2), Spring WebSocket
- **Data/Infra**: **Supabase** (PostgreSQL 17), Redis 7.x
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
Uruchom lokalną instancję Redis (np. przez Docker):
```bash
docker run --name waw-redis -p 6379:6379 -d redis:7
```

### Backend (Spring Boot 3.x)
1. Uruchom backend:
   - Windows (PowerShell):
     ```bash
     cd backend
     .\gradlew.bat bootRun
     ```
   - macOS/Linux:
     ```bash
     cd backend
     ./gradlew bootRun
     ```
2. Aplikacja automatycznie łączy się z Supabase (port 54322) i Redis

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
- `gradlew bootRun` / `./gradlew bootRun`: run the Spring Boot application
- Additional Gradle references are listed in `backend/HELP.md`

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
- CI/CD: Planned via GitHub Actions (lint, tests, build, deploy)
- Monitoring: Spring Actuator + Prometheus + Grafana (planned/ongoing)
- API docs: Swagger/OpenAPI (planned/ongoing)

For broader product goals and success metrics, see `.ai/prd.md`.

## License
MIT © 2025 Łukasz Zieliński

