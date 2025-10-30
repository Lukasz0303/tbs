# Tech Stack — World at War: Turn-Based Strategy

## Frontend
- **Framework:** Angular 17 (z Angular Animations)
  - Nowoczesny framework SPA, zapewniający szybkie i zgodne z obecnymi standardami tworzenie rozbudowanych interfejsów użytkownika.
- **Styling:** SCSS, CSS Transitions
  - SCSS pozwala na organizację i skalowanie stylów, transitions zapewniają płynne animacje.
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