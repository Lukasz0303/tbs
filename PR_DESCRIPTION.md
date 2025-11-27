# feat: migrate tests to jest/cypress and harden auth cookies

## 📋 Overview

This PR standardises testing across the stack by replacing Karma/Jasmine with Jest plus Angular Testing Library, wiring up a Cypress E2E harness, and documenting the full QA workflow. On the backend it introduces dedicated test dependencies/config, improves the PowerShell bootstrap flow, and tightens authentication cookies (Secure + SameSite=Strict).

## ✨ What's Changed

### New Features

- **Jest-based unit tests** – Added `jest.config.js`, `src/test-setup.ts`, and updated `package.json`, `angular.json`, and `tsconfig*.json` so Angular specs run on Jest with Testing Library matchers.
- **Cypress E2E harness** – Introduced `cypress.config.ts`, support commands (`login`/`logout`), `auth` and `game-flow` journeys, a Cypress-specific README, and `data-cy` hooks on login/game/leaderboard templates.
- **Testing documentation** – Added `TESTING.md`, `CYPRESS_README.md`, `.ai/test-plan.md`, new prompt files, and a `game-component-structure.txt` ASCII map; expanded the main `README.md` testing section and `.ai/tech-stack.md` strategy.
- **CI workflow** – Added `.github/workflows/ci.yml` that runs `npm run test:ci`, builds the Angular bundle, and executes `./gradlew testUnit`/`testIntegration`, publishing coverage/test artifacts for both stacks.

### Core Components

**New Services**

- `RankingRefreshScheduler` now runs under `@Profile("!test")`, preventing background refreshes during automated suites.

**New Controllers**

- `AuthController` and `GuestController` now emit `authToken` cookies with `SameSite=Strict` and honour the secure flag so browser sessions stay scoped.
- `JwtAuthenticationFilter` allows anonymous access to `/api/v1/rankings/**`, keeping ranking endpoints publicly readable without bypassing other guards.

**New Exceptions**

- None.

**New DTOs/Models**

- None.

### Improvements

**Database & Performance**

- `backend/build.gradle` pulls in H2, RestAssured, and Testcontainers (core + PostgreSQL) and adds `testUnit`, `testIntegration`, and `testAll` Gradle tasks with verbose logging.
- Introduced `backend/src/test/resources/application-test.properties` (in-memory DB, Redis toggles, relaxed rate limits) plus `@ActiveProfiles("test")` on Spring tests to keep suites isolated.
- Controller/service tests now request paged data via `PageRequest` and use memoised repositories/move counts to reduce repeated DB hits.

**Error Handling**

- `backend/run-backend.ps1` was rewritten to use PowerShell jobs/timeouts, check ports before spawning Supabase/Redis, reuse existing containers, and emit clearer hints; a root-level `run-backend.ps1` proxy makes invoking it easier from the repo root.

**Security & Validation**

- `application.properties` sets `app.cookie.secure=true`, and login/guest cookies switch to `SameSite=Strict`, requiring HTTPS but eliminating cross-site leakage.

**Code Quality**

- Angular specs (app component, game banner/mode cards, game options, home, websocket service) now rely on Jest mocks instead of Jasmine spies, and templates gained `data-cy` attributes to stabilise Cypress selectors.
- Added a comprehensive `AuthService` HttpClient spec and removed the obsolete `UserControllerIntegrationTest`.
- Backend unit tests instantiate controllers/services explicitly, use lenient Mockito settings only where necessary, and align expectations (e.g., `ConflictException`, TokenBlacklist errors, updated ranking endpoints).

## 🧪 Testing

- `frontend/src/app/services/auth.service.spec.ts` covers guest session creation, login/logout flows, and auth state helpers with `HttpClientTestingModule`.
- Cypress specs (`frontend/cypress/e2e/auth.cy.ts`, `game-flow.cy.ts`) verify form rendering, navigation, and basic journeys using the new custom commands.
- Backend tests now run under the `test` profile (`TbsApplicationTests`, `HealthControllerTest`, etc.), refresh pageable expectations, and add stricter assertions for matchmaking, ranking, logout, and guest flows.

## 🗄️ Database Changes

- None (only test-only configuration in `application-test.properties`).

## 📦 Files Changed

- 61 files changed, 7 685 insertions(+), 1 077 deletions(-) (`git diff origin/main...HEAD --stat`).

## 🔍 Migration Notes

- No Flyway migrations are included. Automated suites now depend on the new Gradle tasks plus `application-test.properties`; local/CI runs must execute `npm test` and `npm run e2e` instead of `ng test`.
- Auth cookies are HTTPS-only by default; override `APP_COOKIE_SECURE=false` (or equivalent property) if you must run over HTTP in dev/test.

## 🔄 Breaking Changes

- Auth cookies now require HTTPS and ship with `SameSite=Strict`. Local setups that still serve the frontend over plain HTTP must flip `app.cookie.secure` or proxy through HTTPS to retain login behaviour.

## ✅ Checklist

- [x] CI workflow runs Jest (`npm run test:ci`) and Gradle (`./gradlew testUnit`, `./gradlew testIntegration`) and uploads reports.
- [x] Secure cookies default to HTTPS with `SameSite=Strict`; override only when absolutely necessary for local dev.
