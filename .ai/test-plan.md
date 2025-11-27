# Plan testów — World at War: Turn-Based Strategy

## 1. Wprowadzenie i cele testowania
- Zapewnienie jakości platformy PvP/PvE opartej o Angular 17 oraz Spring Boot 3, w tym komunikacji WebSocket, rankingów i rozgrywek turowych.
- Walidacja zgodności implementacji z dokumentacją `.ai`, specyfikacjami API oraz wymaganiami dotyczącymi motywu PrimeNG Verona.
- Wczesna detekcja regresji podczas integracji frontendu i backendu oraz weryfikacja stabilności infrastruktury (PostgreSQL, Redis, CI/CD).

## 2. Zakres testów
- **Frontend (Angular 17, PrimeNG):** logowanie i rejestracja, wybór trybu gry, ekran rozgrywki (timery, ruchy, surrender, powiadomienia), ranking globalny, dostępność (WCAG), responsywność oraz obsługa reconnectów WebSocket.
- **Backend (Spring Boot 3):** REST API auth/ranking, logika gry i botów, obsługa WebSocket (ruchy, ping/pong, aktualizacje gry), bezpieczeństwo (JWT/OAuth2), Timer Service, obsługa Redis cache i kolejek, migracje Flyway.
- **Baza danych i supabase/migracje:** spójność schematu z encjami, dane referencyjne (rankingi, statystyki), procedury migracji wstecznych.
- **Skrypty automatyczne (PowerShell) i CI/CD:** uruchamianie testów e2e backend, pakowanie obrazów Docker, pipeline GitHub Actions.

## 3. Typy testów
1. **Testy statyczne:** ESLint + Prettier, SonarCloud (Angular i Java), Checkstyle, analiza zależności w `.ai`.
2. **Jednostkowe frontend:** Jest + Angular Testing Library — komponenty UI, usługi WebSocket, formatowanie danych rankingu, guardy routingowe.
3. **Jednostkowe backend:** JUnit 5 + Mockito — logika ruchów, walidacje planów, TimerService, serwisy rankingowe, autoryzacja JWT.
4. **Integracyjne backend:** Spring Boot Test + Testcontainers (PostgreSQL/Redis) — API auth, przepływy rankingowe, kolejki i wsparcie WebSocket handshake.
5. **Testy kontraktowe:** Schematy OpenAPI i kontrakty WebSocket (serializacja `BaseWebSocketMessage`, typy polimorficzne).
6. **Testy e2e frontend:** Cypress — kluczowe user journeys (logowanie → mecz PvE/PvP → ranking), w tym scenariusze reconnect i surrender.
7. **Testy e2e backend (skrypty PowerShell):** `test-pvp-match.ps1`, `test-bot-move.ps1`, `test-win-bot-and-check-ranking.ps1` uruchamiane cyklicznie.
8. **Testy wydajnościowe:** k6/Gatling dla API i WebSocket; testy obciążeniowe rankingów i równoległych meczów.
9. **Testy bezpieczeństwa:** skan JWT, analiza OWASP (autoryzacja WebSocket, rate limiting, CSRF), weryfikacja konfiguracji Spring Security.
10. **Testy niezawodności/chaos:** symulacja utraty połączenia, restartów Redis/PostgreSQL, delayed messages.
11. **Testy migracji i danych:** walidacja Flyway na migawkach supabase, testy rollback oraz weryfikacja integralności rankingów.
12. **Testy regresyjne i smoke:** automat odpalany na każdym merge (lint + unit + integracja + skrócone e2e).

## 4. Scenariusze testowe dla kluczowych funkcjonalności
1. **Autoryzacja i profil:**
   - Rejestracja i logowanie z JWT, odświeżanie tokenu, obsługa błędów (np. wygasły token).
   - Dostęp do chronionych widoków i WebSocket wyłącznie dla autoryzowanych użytkowników.
2. **Matchmaking i tryby gry:**
   - Wybór poziomu bota, uruchomienie meczu z AI i walidacja ruchów.
   - Kolejkowanie PvP, zestawienie graczy, obsługa przypadków braku przeciwnika.
3. **Przepływ ruchów i timer:**
   - Wysłanie ruchu (`MOVE`), przyjęcie (`MOVE_ACCEPTED`), odrzucenie (`MOVE_REJECTED`) oraz aktualizacja stanu gry.
   - Odliczanie czasu tury (wiadomości `TIMER_UPDATE`), timeout i automatyczne zakończenie.
4. **Surrender/reconnect:**
   - Kapitulacja (`SURRENDER`) i poprawne naliczenie punktów rankingowych.
   - Utrata połączenia, ponowne połączenie i odtworzenie stanu gry.
5. **Ranking i ekonomia punktów:**
   - Aktualizacja rankingu po meczu (PvE i PvP), weryfikacja Redis cache, konsystencja w bazie.
   - Widok rankingu w Angular, sortowanie, paginacja, filtry.
6. **Powiadomienia/WebSocket:**
   - Ping/pong, automatyczne reconnecty, obsługa kilku równoległych gier.
   - Odporność na nieobsługiwane typy wiadomości.
7. **Migracje i dane:** uruchomienie Flyway na pustej bazie i na bazie produkcyjnej; testy rollback.
8. **UI/UX PrimeNG:** responsywność, motyw Verona, dostępność (nawigacja klawiaturą, kontrast).

## 5. Środowisko testowe
- **Lokalne DEV:** `docker-compose` z usługami backend + PostgreSQL + Redis, frontend `ng serve`, mocki usług z `.ai`.
- **Środowisko QA/Staging:** zautomatyzowane deploymenty (Docker/DO droplet), certyfikaty TLS, dane zbliżone do produkcyjnych, monitoring Actuator + Prometheus.
- **CI (GitHub Actions):** pipeline odpalający lint → testy jednostkowe → integracyjne (Testcontainers) → Cypress w trybie headless → publikacja raportów SonarCloud.
- **Dane testowe:** konta użytkowników per rola (player, admin), predefiniowane rankingi, scenariusze konfliktowe (równoczesne mecze).

## 6. Narzędzia testowe
- **Frontend:** Jest, Angular Testing Library, Cypress, Storybook Visual Regression (opcjonalnie).
- **Backend:** JUnit 5, Mockito, Testcontainers, WireMock, RestAssured, Spring MockMvc.
- **Wydajność:** k6/Gatling, JMeter (WebSocket plugin).
- **Bezpieczeństwo:** OWASP ZAP, snyk, dependency-check.
- **Infrastruktura:** Docker, docker-compose, GitHub Actions, SonarCloud, Prometheus + Grafana, PowerShell test suites.
- **Zarządzanie testami i defektami:** GitHub Projects/Issues, kanban QA, raporty Allure.

## 7. Harmonogram testów (cykl dwutygodniowy)
1. **Dzień 1-2:** aktualizacja przypadków testowych, przygotowanie danych, sanity lint.
2. **Dzień 3-5:** testy jednostkowe i integracyjne w sprint backlog, weryfikacja nowych funkcji.
3. **Dzień 6-7:** E2E (Cypress + PowerShell), testy regresyjne i smoke na QA.
4. **Dzień 8:** testy wydajnościowe i bezpieczeństwa przy releasach kandydackich.
5. **Dzień 9:** analiza wyników, triage defektów, aktualizacja raportów.
6. **Dzień 10:** zatwierdzenie releasu, aktualizacja dokumentacji `.ai`.

## 8. Kryteria akceptacji testów
- Pokrycie jednostkowe: ≥ 80% kluczowych modułów (game logic, ranking, auth).
- Zero otwartych defektów o priorytecie krytycznym i ≤2 o wysokim przy release.
- Wszystkie scenariusze E2E kluczowych ścieżek zakończone sukcesem.
- Wyniki testów wydajnościowych: czas odpowiedzi API < 200 ms p95, WebSocket utrzymuje ≥ 500 równoległych połączeń bez degradacji.
- Flyway migracje przechodzą na czystej i istniejącej bazie; brak rozbieżności schematu.

## 9. Role i odpowiedzialności
- **QA Lead:** definiuje strategię, priorytetyzuje testy, raportuje metryki do stakeholderów.
- **QA Frontend:** tworzy i utrzymuje testy Jest/Cypress, waliduje UI/UX i dostępność.
- **QA Backend:** odpowiada za JUnit, integracje, testy kontraktowe i wydajnościowe.
- **Automation Engineer:** rozwija framework Cypress/PowerShell, integruje raporty (Allure/Sonar).
- **DevOps:** utrzymuje środowiska QA/CI, monitoruje metryki, wspiera testy chaos/obciążeniowe.
- **Product Owner:** zatwierdza kryteria akceptacji, przyjmuje raporty końcowe.

## 10. Procedury raportowania błędów
1. Defekt zgłaszany w GitHub Issues z szablonem (kroki, oczekiwany/otrzymany wynik, logi, zrzuty ekranu, środowisko).
2. QA przypisuje priorytet i komponent (frontend/backend/infra) oraz linkuje do scenariusza testowego.
3. QA Lead dokonuje triage raz dziennie; krytyczne defekty eskalowane natychmiast do odpowiedzialnego lidera technicznego.
4. Po naprawie: QA wykonuje retest + stosowną regresję; wynik dokumentowany w komentarzu i w raporcie testowym.
5. Raport sprintowy: zestawienie defektów, wskaźniki pokrycia i status scenariuszy, przechowywane w `.ai/implementation-plans`.

