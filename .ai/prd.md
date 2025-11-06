# Dokument wymagań produktu (PRD) - World at War: Turn-Based Strategy

## 1. Przegląd produktu

World at War: Turn-Based Strategy to aplikacja webowa napisana z wykorzystaniem nowoczesnego, produkcyjnego stacku technologicznego (Angular 17 + Spring Boot 3.x + PostgreSQL + Redis + Docker). Celem jest nie tylko rozrywka, ale i realizacja ambitnego, skalowalnego projektu na miarę dużych komercyjnych rozwiązań.

Aplikacja umożliwia graczom rywalizację z botem AI na trzech poziomach trudności lub z innymi graczami w czasie rzeczywistym. System punktowy i globalny ranking motywują graczy do ciągłego doskonalenia swoich umiejętności strategicznych.

Platforma została zaprojektowana z myślą o wysokiej jakości wizualnej i responsywności, wykorzystując Angular Animations i CSS transitions dla płynnych animacji. Architektura monolityczna z przygotowaniem na przyszłą skalowalność zapewnia stabilność i wydajność dla 100-500 jednoczesnych użytkowników.

## Technologie

Projekt wykorzystuje poniższy, sprawdzony stack technologiczny:

### Frontend
- **Angular 17** — nowoczesny framework SPA; Angular Animations i PrimeNG zapewniają wysokiej jakości UI z płynnymi animacjami.
- **SCSS + CSS Transitions** — rozbudowane stylowanie i przejścia animacji.
- **i18n (Internationalization)** — wsparcie dla wielu języków: angielski (podstawowy), polski (dodatkowy). Implementacja tylko po stronie UI (frontend), backend pozostaje bez zmian.
- **Lintery:** ESLint + Prettier — standaryzacja, jednolity format kodu.
- **Testowanie:** Jest, Angular Testing Library, E2E przez Cypress.

### Backend
- **Java 21 + Spring Boot 3** — wysoki poziom bezpieczeństwa, łatwość skalowania, dojrzała obsługa WebSocket i API.
- **Spring Security z JWT/OAuth2** — silna kontrola dostępu i ochrona endpointów.
- **WebSocket (Spring Websocket)** — real-time PvP, obsługa reconnectów.
- **Redis 7** — wspiera szybkie dane rankingowe i sesje.
- **PostgreSQL 15** — główna baza danych; relacyjna, stabilna, skalowalna.
- **Flyway** — proste, szybkie wersjonowanie/migracje schematów bazy SQL.
- **Monitoring:** Spring Actuator, Prometheus, Grafana. Health endpoints + wizualizacja metryk.
- **Testowanie:** JUnit 5, Mockito (unit, integracyjne), dokumentacja przez Swagger (OpenAPI).
- **Checkstyle** — formatowanie i jakość kodu Java.
- **Analiza statyczna:** SonarCloud (BE/FE) — automatyczna inspekcja kodu i bezpieczeństwa.

### DevOps/Hosting
- **CI/CD** — GitHub Actions (lint, testy, build, deploy)
- **Konteneryzacja:** Docker, docker-compose; szybkie środowiska lokalne i produkcyjne.
- **Hosting:** DigitalOcean (deploy obrazów docker)
- **Repozytoria:** GitHub

#### Uzasadnienie wyboru:
Każdy element stacku wpisuje się w potrzeby projektu: szybki rozwój, bezpieczeństwo, testowalność, stabilność na produkcji i łatwe wdrażanie. Wybrano powszechnie uznawane technologie branżowe o dużej społeczności, wysokiej wydajności i wsparciu praktyk CI/CD, DevOps oraz monitoringu.

## 2. Problem użytkownika

Obecnie brakuje dostępnych, wysokiej jakości platform do gry w strategiczne gry turowe online, które oferują:
- Natychmiastowy dostęp bez konieczności rejestracji (tryb gościa)
- Różnorodne poziomy trudności dla graczy o różnych umiejętnościach
- System rankingowy motywujący do ciągłej gry
- Płynną rozgrywkę wieloosobową w czasie rzeczywistym
- Estetyczny i responsywny interfejs użytkownika

Gracze potrzebują platformy, która pozwoli im szybko dołączyć do gry, rywalizować z innymi graczami lub botem, oraz śledzić swoje postępy poprzez system punktowy i ranking.

## 3. Wymagania funkcjonalne

### Podstawowe funkcjonalności gry
- Gra w kółko i krzyżyk na planszach 3x3, 4x4, 5x5
- Automatyczne wykrywanie wygranej, przegranej lub remisu
- Walidacja ruchów gracza
- Wizualna reprezentacja stanu gry

### System użytkowników
- Tryb gościa z identyfikacją po adresie IP
- Rejestracja nowych użytkowników (nazwa, email, hasło)
- Logowanie zarejestrowanych użytkowników
- Profil gracza z podstawowymi informacjami

### System zapisywania gier
- Automatyczny zapis stanu gry w trybie jednoosobowym
- Możliwość kontynuacji gry po ponownym uruchomieniu
- Automatyczne zakończenie gry po 20 sekundach nieaktywności w PvP

### Bot AI
- Trzy poziomy trudności: łatwy, średni, trudny
- Deterministyczne algorytmy dla każdego poziomu
- Modularna architektura umożliwiająca łatwe rozszerzanie

### System punktowy
- +100 pkt za wygraną z botem (łatwy poziom)
- +500 pkt za wygraną z botem (średni poziom)
- +1000 pkt za wygraną z botem (trudny poziom)
- +1000 pkt za wygraną w PvP

### Ranking i matchmaking
- Globalny ranking graczy (permanentny)
- Losowy system matchmakingu
- Szybkie znajdowanie przeciwników online

### Funkcjonalności PvP
- Możliwość poddania pojedynku
- Timer pokazujący czas pozostały na ruch przeciwnika
- Informacje o liczbie tur i aktualnej turze
- Limit 10 sekund na ruch

## 4. Granice produktu

### W zakresie MVP
- Gra kółko i krzyżyk w rozmiarach 3x3, 4x4, 5x5
- Tryb gościa i rejestracja użytkowników
- Bot AI z trzema poziomami trudności
- PvP z podstawowymi funkcjonalnościami
- System punktowy i ranking
- Profil gracza z podstawowymi informacjami
- Wsparcie dla wielu języków (i18n): angielski (podstawowy), polski (dodatkowy) - tylko po stronie UI

### Poza zakresem MVP
- Zaawansowane mechaniki strategiczne inne niż kółko i krzyżyk
- System powiadomień email
- Wsparcie dla urządzeń mobilnych
- Zaawansowane funkcje bezpieczeństwa i analityki
- System znajomych i zaproszeń
- Chat podczas rozgrywki
- Personalizacja profilu gracza
- Zaawansowane algorytmy AI bota

## 5. Historyjki użytkowników

### US-001: Rozpoczęcie gry jako gość
**Opis:** Jako gość chcę móc natychmiastowo rozpocząć grę bez konieczności rejestracji, aby szybko przetestować aplikację.

**Kryteria akceptacji:**
- Mogę rozpocząć grę bez podawania danych osobowych
- Jestem identyfikowany po adresie IP
- Mam dostęp do wszystkich trybów gry (vs bot, PvP)
- Mogę przeglądać ranking graczy

### US-002: Rejestracja nowego użytkownika
**Opis:** Jako nowy użytkownik chcę móc utworzyć konto, aby móc śledzić swoje postępy i mieć stały dostęp do profilu.

**Kryteria akceptacji:**
- Mogę wprowadzić nazwę użytkownika, email i hasło
- System waliduje poprawność danych (email format, długość hasła)
- Po rejestracji jestem automatycznie zalogowany
- Otrzymuję potwierdzenie utworzenia konta

### US-003: Logowanie zarejestrowanego użytkownika
**Opis:** Jako zarejestrowany użytkownik chcę móc się zalogować, aby uzyskać dostęp do mojego profilu i historii gier.

**Kryteria akceptacji:**
- Mogę wprowadzić email i hasło
- System weryfikuje poprawność danych logowania
- Po zalogowaniu mam dostęp do pełnego profilu
- Mogę kontynuować zapisane gry jednoosobowe

### US-004: Rozgrywka z botem (łatwy poziom)
**Opis:** Jako gracz chcę móc grać z botem na łatwym poziomie, aby nauczyć się podstaw gry i zdobyć pierwsze punkty.

**Kryteria akceptacji:**
- Mogę wybrać rozmiar planszy (3x3, 4x4, 5x5)
- Bot wykonuje losowe, ale poprawne ruchy
- Po wygranej otrzymuję +100 punktów
- Gra jest automatycznie zapisywana

### US-005: Rozgrywka z botem (średni poziom)
**Opis:** Jako gracz chcę móc grać z botem na średnim poziomie, aby stawić czoła większemu wyzwaniu i zdobyć więcej punktów.

**Kryteria akceptacji:**
- Bot stosuje podstawową strategię (blokuje wygrywające ruchy)
- Po wygranej otrzymuję +500 punktów
- Bot nie jest zbyt łatwy ani zbyt trudny
- Gra jest automatycznie zapisywana

### US-006: Rozgrywka z botem (trudny poziom)
**Opis:** Jako doświadczony gracz chcę móc grać z botem na trudnym poziomie, aby przetestować swoje umiejętności i zdobyć maksymalne punkty.

**Kryteria akceptacji:**
- Bot stosuje optymalną strategię (minimax)
- Po wygranej otrzymuję +1000 punktów
- Bot jest bardzo trudnym przeciwnikiem
- Gra jest automatycznie zapisywana

### US-007: Dołączenie do gry PvP
**Opis:** Jako gracz chcę móc dołączyć do gry z innym graczem online, aby rywalizować w czasie rzeczywistym.

**Kryteria akceptacji:**
- System automatycznie znajduje dostępnego przeciwnika
- Mogę wybrać rozmiar planszy przed rozpoczęciem gry
- Gra rozpoczyna się natychmiast po znalezieniu przeciwnika
- Mam 10 sekund na wykonanie ruchu

### US-008: Rozgrywka PvP z funkcjonalnościami
**Opis:** Jako gracz w PvP chcę mieć dostęp do dodatkowych funkcjonalności, aby móc kontrolować przebieg gry.

**Kryteria akceptacji:**
- Mogę poddać pojedynk w dowolnym momencie
- Widzę timer pokazujący czas pozostały na ruch przeciwnika
- Widzę informację o aktualnej turze i liczbie tur
- Po wygranej otrzymuję +1000 punktów

### US-009: Przeglądanie rankingu graczy
**Opis:** Jako gracz chcę móc przeglądać globalny ranking, aby zobaczyć swoje miejsce i porównać się z innymi graczami.

**Kryteria akceptacji:**
- Widzę listę graczy posortowaną według punktów
- Moja pozycja jest wyróżniona wizualnie
- Ranking jest aktualizowany w czasie rzeczywistym
- Mogę zobaczyć podstawowe statystyki innych graczy

### US-010: Wybór przeciwnika z rankingu
**Opis:** Jako gracz chcę móc wybrać konkretnego przeciwnika z rankingu, aby grać z graczem o podobnym poziomie umiejętności.

**Kryteria akceptacji:**
- Mogę kliknąć na gracza w rankingu
- System sprawdza czy gracz jest dostępny online
- Jeśli gracz jest dostępny, gra rozpoczyna się natychmiast
- Jeśli gracz nie jest dostępny, otrzymuję odpowiedni komunikat

### US-011: Zarządzanie profilem gracza
**Opis:** Jako zarejestrowany użytkownik chcę móc przeglądać i zarządzać swoim profilem, aby śledzić swoje postępy.

**Kryteria akceptacji:**
- Widzę swoją nazwę użytkownika
- Widzę aktualne miejsce w rankingu
- Widzę liczbę punktów i rozegranych gier
- Profil jest estetycznie wyróżniony

### US-012: Automatyczne zapisywanie gier
**Opis:** Jako gracz chcę, aby moje gry były automatycznie zapisywane, aby móc je kontynuować po ponownym uruchomieniu aplikacji.

**Kryteria akceptacji:**
- Gry jednoosobowe są automatycznie zapisywane po każdym ruchu
- Mogę kontynuować zapisaną grę po ponownym zalogowaniu
- Gry PvP są automatycznie kończone po 20 sekundach nieaktywności
- System identyfikuje mnie po email (zarejestrowani) lub IP (goście)

### US-013: Obsługa rozłączeń w PvP
**Opis:** Jako gracz w PvP chcę, aby system odpowiednio obsługiwał sytuacje, gdy przeciwnik opuści grę lub straci połączenie.

**Kryteria akceptacji:**
- Gdy przeciwnik opuści grę, otrzymuję automatyczne zwycięstwo
- Mam 20 sekund na powrót do gry po rozłączeniu
- System automatycznie kończy grę po przekroczeniu limitu czasu
- Otrzymuję odpowiednie powiadomienia o zmianie stanu gry

### US-014: Walidacja ruchów w grze
**Opis:** Jako gracz chcę, aby system walidował moje ruchy, aby zapewnić fair play i poprawność rozgrywki.

**Kryteria akceptacji:**
- Nie mogę wykonać ruchu na zajęte pole
- Nie mogę wykonać ruchu poza planszę
- System automatycznie wykrywa wygraną, przegraną lub remis
- Ruchy są walidowane zarówno po stronie klienta jak i serwera

### US-015: Responsywność interfejsu
**Opis:** Jako użytkownik chcę, aby interfejs był responsywny i działał płynnie na różnych rozdzielczościach ekranu.

**Kryteria akceptacji:**
- Interfejs dostosowuje się do różnych rozdzielczości ekranu
- Animacje są płynne i nie wpływają na wydajność
- Wszystkie elementy są czytelne i łatwo dostępne
- Aplikacja działa stabilnie na przeglądarkach PC

## 6. Metryki sukcesu

### Metryki funkcjonalne
- Wszystkie 4 scenariusze użytkownika (I-IV) są w pełni zrealizowane
- 100% historyjek użytkownika (US-001 do US-015) jest testowalnych i zaimplementowanych
- Wszystkie scenariusze są przetestowane przy pomocy testów E2E (Cypress)

### Metryki techniczne
- Aplikacja jest udostępniona publicznie pod adresem URL
- Wydajność: obsługa 100-500 jednoczesnych użytkowników bez spadku wydajności
- Stabilność: system WebSocket z mechanizmami reconnect działa niezawodnie
- Czas odpowiedzi: limit 10 sekund na ruch w grach PvP jest respektowany

### Metryki jakości
- Wysokiej jakości UI z animacjami Angular i CSS transitions
- Responsywność na najwyższym poziomie dla platformy PC
- Pokrycie testami: unit testy (BE + FE) + E2E testy (Cypress)
- Dokumentacja: Swagger API + README + Docker setup

### Metryki użytkownika
- Gracze mogą natychmiastowo rozpocząć grę w trybie gościa
- System rankingowy motywuje do ciągłej gry
- Bot AI oferuje odpowiednie wyzwanie na każdym poziomie trudności
- Matchmaking zapewnia szybkie znajdowanie przeciwników online
