# Tech Stack — World at War: Turn-Based Strategy

## Frontend
- **Framework:** Angular 17 (z Angular Animations)
  - Nowoczesny framework SPA, zapewniający szybkie i zgodne z obecnymi standardami tworzenie rozbudowanych interfejsów użytkownika.
- **Styling:** SCSS, CSS Transitions
  - SCSS pozwala na organizację i skalowanie stylów, transitions zapewniają płynne animacje.
- **Motyw UI:** PrimeNG Verona (`https://verona.primeng.org/`)
  - Jedyny dopuszczalny motyw wizualny; wszystkie komponenty i widoki muszą być z nim spójne.
- **UI/UX:** Angular Animations, PrimeNG
  - Gotowe komponenty + narzędzia do tworzenia wysokiej jakości interfejsów i animacji.
- **Linter:** ESLint + Prettier
  - Automatyczna kontrola jakości kodu i jednolity format na FE.
- **Testowanie jednostkowe:** Jest + Angular Testing Library
  - Sprawdzony ekosystem do testów komponentów i logiki.
- **E2E:** Cypress
  - Testy end-to-end procesu biznesowego MVP i user journeys.

## Backend
- **Język/Framework:** Java 21 + Spring Boot 3.x (monolit)
  - Dojrzały framework; szybki bootstrap, skalowalna architektura, duża społeczność, bezpieczeństwo klasy enterprise.
- **Komunikacja real-time:** WebSocket (Spring Websocket)
  - Obsługa PvP w czasie rzeczywistym i wsparcie reconnectów.
- **Autoryzacja i bezpieczeństwo:** Spring Security (JWT, OAuth2)
  - Bezpieczne uwierzytelnianie użytkowników; ochrona endpointów i komunikacji.
- **Baza danych:** PostgreSQL 15
  - Stabilna, wydajna baza relacyjna; obsługa transakcji i schematów dla rankingów/gier.
- **Wersjonowanie bazy:** Flyway
  - Proste narzędzie do kontrolowania migracji SQL; wersjonowanie schematów w repo.
- **Cache:** Redis 7.x
  - Kolejkowanie, sesje, szybki dostęp do rankingów i stanów gier bez obciążenia bazy głównej.
- **Analiza statyczna:** SonarCloud
  - Wczesne wykrywanie błędów i zapewnienie wysokiej jakości kodu (BE/FE).
- **Monitoring + Health:** Spring Actuator (+ Prometheus Metrics) + Grafana
  - Monitoring działania usługi, konsumpcja metryk przez Prometheus, wyświetlanie w Grafanie. Health endpoints do automatycznego sprawdzania statusu.
- **Testowanie jednostkowe:** JUnit 5 + Mockito
- **Swagger (OpenAPI) i dokumentacja API**
- **Formatowanie:** Checkstyle (Java)

## Ogólne (DevOps, architektura, hosting)
- **CI/CD:** GitHub Actions (pipeline, lint, testy, budowanie obrazu, deploy)
- **Konteneryzacja:** Docker + docker-compose
  - Umożliwia szybki lokalny rozwój oraz deployment na produkcję.
- **Hosting:** DigitalOcean (droplet/k8s, obraz docker)
- **Repozytoria:** GitHub

### Uzasadnienia doboru narzędzi:
- Stack umożliwia szybki (jak na poważny projekt) rozwój MVP oraz późniejsze przejście do dużej produkcji, zachowując bezpieczeństwo, wsparcie społeczności oraz łatwość wdrażania na prod.
- Wybór popularnych narzędzi i frameworków gwarantuje dostępność pomocy i rozszerzalność rozwiązania, przy jednoczesnym zachowaniu wysokiego poziomu bezpieczeństwa oraz stabilności.

## Strategia testowa

### Zakres i cele
- **Frontend:** scenariusze logowania, wybór trybu gry, przebieg tury (ruchy, timery, surrender, reconnect), ranking, dostępność i responsywność motywu PrimeNG Verona.
- **Backend:** REST auth/ranking, boty i walidacja ruchów, obsługa WebSocket (MOVE/TIMER/SURRENDER), timer usługowy, aktualizacja rankingów oraz blacklisty JWT w Redis.
- **Baza danych i Supabase:** migracje Flyway na czystej/istniejącej bazie, integralność schematu, rollbacki, dane referencyjne rankingu.
- **Automatyzacja i infrastruktura:** skrypty PowerShell do smoke/E2E backendu, pipeline GitHub Actions, monitoring Actuator + Prometheus + Grafana.

### Typy testów (wg `.ai/test-plan.md`)
- **Statyczne:** ESLint + Prettier (Angular), Checkstyle + SonarCloud (Java).
- **Jednostkowe frontend:** Jest + Angular Testing Library.
- **Jednostkowe backend:** JUnit 5 + Mockito.
- **Integracyjne backend:** Spring Boot Test + Testcontainers (PostgreSQL/Redis), RestAssured/Spring MockMvc, WireMock.
- **Kontraktowe:** OpenAPI, serializacja/typy wiadomości WebSocket.
- **E2E frontend:** Cypress dla głównych user journeys (logowanie → PvE/PvP → ranking, reconnect, surrender).
- **E2E backend:** PowerShell (`backend/test-pvp-match.ps1`, `test-bot-move.ps1`, `test-win-bot-and-check-ranking.ps1`, `test-pvp-websocket.ps1`).
- **Wydajnościowe/chaos:** k6/Gatling, scenariusze restartów usług (Redis/PostgreSQL).
- **Bezpieczeństwa:** OWASP ZAP, snyk, dependency-check (JWT, WebSocket, rate limiting).
- **Smoke/regresja:** cykliczny zestaw lint → unit → integracja → skrócone e2e uruchamiany na każdym merge.

### Narzędzia i raportowanie
- **Frontend:** Jest, Angular Testing Library, Cypress, Storybook Visual Regression (opcjonalnie).
- **Backend:** JUnit 5, Mockito, Testcontainers, WireMock, RestAssured, Spring MockMvc.
- **Jakość/statyczne:** ESLint, Prettier, Checkstyle, SonarCloud.
- **Wydajność/niezawodność:** k6, Gatling, docker-compose, GitHub Actions runners.
- **Bezpieczeństwo:** OWASP ZAP, snyk, dependency-check.
- **Raporty/obserwowalność:** Allure, GitHub Actions artifacts, Prometheus + Grafana, Spring Actuator.

### Uruchamianie testów
- **Frontend unit:** `cd frontend && npm test`.
- **Backend unit/integracyjne:** `cd backend && ./gradlew test` (Windows: `gradlew.bat test`), z możliwością startu usług pomocniczych przez `npx supabase start` lub `.\run-backend.ps1 start`.
- **Backend e2e (PowerShell):** `cd backend && .\test-pvp-match.ps1` (analogicznie dla pozostałych skryptów).
- **Frontend e2e:** `cd frontend && npx cypress run` (konfiguracja opisana w `.ai/test-plan.md`).
- **Pipeline CI/CD:** GitHub Actions wykonuje sekwencję lint → unit → integracyjne → Cypress → publikacja raportów SonarCloud/Allure przy każdym merge.