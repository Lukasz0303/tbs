# Architektura UI dla World at War: Turn-Based Strategy

## 1. Przegląd struktury UI

Architektura interfejsu użytkownika dla World at War: Turn-Based Strategy została zaprojektowana jako nowoczesna aplikacja SPA wykorzystująca Angular 17 z architekturą opartą na standalone components i feature modules. Interfejs został zaprojektowany z myślą o wysokiej jakości wizualnej, responsywności i płynnych animacjach, wykorzystując Angular Animations, PrimeNG i CSS transitions.

Aplikacja wspiera wiele języków (i18n) z angielskim jako językiem podstawowym i polskim jako językiem dodatkowym. Implementacja i18n jest realizowana wyłącznie po stronie UI (frontend), backend pozostaje bez zmian i nie wymaga modyfikacji.

Struktura UI jest zorganizowana wokół głównych funkcjonalności produktu: uwierzytelniania (goście i zarejestrowani użytkownicy), rozgrywki (vs bot i PvP), rankingu oraz profilu użytkownika. Wszystkie widoki są zintegrowane z REST API i WebSocket API zgodnie z planem API, zapewniając spójne doświadczenie użytkownika.

**Wymóg motywu UI:** wszystkie elementy interfejsu muszą bazować na motywie PrimeNG Verona (`https://verona.primeng.org/`). Projektowanie, implementacja i walidacja widoków odbywa się z uwzględnieniem tokenów, przestrzeni i komponentów tego motywu bez wprowadzania alternatywnych skórek.

Główny layout aplikacji składa się z NavbarComponent (zawsze widoczny) i router outlet dla dynamicznych widoków. Nawigacja jest intuicyjna i wspiera wszystkie scenariusze użytkownika zdefiniowane w PRD, w tym natychmiastowy dostęp dla gości, rejestrację, logowanie oraz wszystkie tryby gry.

---

## 2. Lista widoków

### 2.1 HomeComponent - Ekran startowy

**Ścieżka**: `/`

**Główny cel**: 
Służy jako punkt wejścia do aplikacji, prezentując użytkownikowi wszystkie dostępne opcje gry i umożliwiając natychmiastowe rozpoczęcie rozgrywki.

**Kluczowe informacje do wyświetlenia**:
- Status użytkownika (gość/zalogowany)
- Ostatnia zapisana gra (jeśli istnieje) - banner z możliwością kontynuacji
- Karty z trybami gry:
  - Graj jako gość (natychmiastowe utworzenie sesji gościa)
  - Graj z botem (przekierowanie do wyboru trybu)
  - Graj PvP (dołączenie do matchmakingu)
- Linki do logowania/rejestracji (dla gości)
- Podstawowe informacje o grze

**Kluczowe komponenty widoku**:
- `GameBannerComponent` - banner z ostatnią zapisaną grą (warunkowo wyświetlany)
- `GameModeCardComponent` - karty z trybami gry (vs bot, PvP, gość)
- `UserStatusIndicatorComponent` - wskaźnik statusu użytkownika

**Integracja API**:
- `GET /api/games?status=in_progress&size=1` - sprawdzenie zapisanej gry
- `POST /api/guests` - utworzenie sesji gościa (jeśli gość)
- `GET /api/auth/me` - sprawdzenie statusu użytkownika

**UX, dostępność i względy bezpieczeństwa**:
- **UX**: Jasne i widoczne przyciski akcji, wizualne wyróżnienie ostatniej gry, intuicyjna nawigacja
- **Dostępność**: ARIA labels dla wszystkich przycisków, keyboard navigation, focus indicators
- **Bezpieczeństwo**: Automatyczne tworzenie sesji gościa tylko na żądanie użytkownika, walidacja statusu przed dostępem do funkcji

**Mapowanie historyjek użytkownika**:
- US-001: Rozpoczęcie gry jako gość - przycisk "Graj jako gość"
- US-012: Automatyczne zapisywanie gier - banner z ostatnią grą

---

### 2.2 AuthLoginComponent - Logowanie

**Ścieżka**: `/auth/login`

**Główny cel**: 
Umożliwienie zarejestrowanym użytkownikom zalogowania się do systemu w celu uzyskania dostępu do pełnego profilu i historii gier.

**Kluczowe informacje do wyświetlenia**:
- Formularz logowania z polami:
  - Email (wymagane, format email)
  - Hasło (wymagane, min. długość)
- Link do rejestracji
- Komunikaty błędów (401, 404)
- Informacja o możliwości gry jako gość

**Kluczowe komponenty widoku**:
- PrimeNG InputText (email)
- PrimeNG Password (hasło)
- PrimeNG Button (submit)
- `ErrorDisplayComponent` - wyświetlanie błędów walidacji

**Integracja API**:
- `POST /api/auth/login` - logowanie użytkownika

**Walidacja**:
- Email: wymagany, format email (reactive forms validators)
- Hasło: wymagane, min. długość

**Po sukcesie**:
- Zapisanie tokenu JWT w localStorage
- Aktualizacja stanu użytkownika (AuthService)
- Przekierowanie do HomeComponent lub ostatniej gry

**UX, dostępność i względy bezpieczeństwa**:
- **UX**: Prosty i czytelny formularz, natychmiastowa walidacja, czytelne komunikaty błędów
- **Dostępność**: ARIA labels, keyboard navigation, focus management, error announcements
- **Bezpieczeństwo**: Hasło ukryte (type="password"), walidacja po stronie klienta i serwera, bezpieczne przechowywanie tokenu

**Mapowanie historyjek użytkownika**:
- US-003: Logowanie zarejestrowanego użytkownika

---

### 2.3 AuthRegisterComponent - Rejestracja

**Ścieżka**: `/auth/register`

**Główny cel**: 
Umożliwienie nowym użytkownikom utworzenia konta w systemie w celu śledzenia postępów i stałego dostępu do profilu.

**Kluczowe informacje do wyświetlenia**:
- Formularz rejestracji z polami:
  - Nazwa użytkownika (wymagana, 3-50 znaków, alfanumeryczne + podkreślniki)
  - Email (wymagany, format email, unikalny)
  - Hasło (wymagane, min. długość, wymagania bezpieczeństwa)
  - Potwierdzenie hasła (wymagane, musi być zgodne z hasłem)
- Link do logowania
- Komunikaty błędów (409, 422)
- Wskaźniki siły hasła

**Kluczowe komponenty widoku**:
- PrimeNG InputText (username, email)
- PrimeNG Password (hasło, potwierdzenie)
- PrimeNG Button (submit)
- `PasswordStrengthIndicatorComponent` - wskaźnik siły hasła
- `ErrorDisplayComponent` - wyświetlanie błędów walidacji

**Integracja API**:
- `POST /api/auth/register` - rejestracja nowego użytkownika

**Walidacja**:
- Nazwa użytkownika: wymagana, 3-50 znaków, pattern `/^[a-zA-Z0-9_]+$/`
- Email: wymagany, format email, unikalny
- Hasło: wymagane, min. długość, wymagania bezpieczeństwa
- Potwierdzenie hasła: wymagane, custom validator sprawdzający zgodność

**Po sukcesie**:
- Zapisanie tokenu JWT w localStorage
- Aktualizacja stanu użytkownika (AuthService)
- Przekierowanie do HomeComponent
- Toast notification z potwierdzeniem rejestracji

**UX, dostępność i względy bezpieczeństwa**:
- **UX**: Krok po kroku walidacja, wizualne wskaźniki siły hasła, czytelne komunikaty błędów
- **Dostępność**: ARIA labels, keyboard navigation, error announcements, focus management
- **Bezpieczeństwo**: Hasła ukryte, walidacja formatu, sprawdzanie unikalności, bezpieczne przechowywanie tokenu

**Mapowanie historyjek użytkownika**:
- US-002: Rejestracja nowego użytkownika

---

### 2.4 GameComponent - Widok gry

**Ścieżka**: `/game/:gameId`

**Główny cel**: 
Wyświetlanie planszy gry i umożliwienie użytkownikowi wykonywania ruchów w grze vs bot lub PvP.

**Kluczowe informacje do wyświetlenia**:
- Plansza gry (dynamiczny rozmiar: 3x3, 4x4, 5x5)
- Informacje o grze:
  - Typ gry (vs_bot / PvP)
  - Przeciwnik (bot / nazwa gracza)
  - Status gry (waiting, in_progress, finished, draw, abandoned)
  - Aktualny gracz (symbol X lub O)
- Timer (dla PvP) - pozostały czas na ruch (10 sekund)
- Liczba tur i aktualna tura
- Przycisk poddania (dla PvP)
- Wizualizacja linii wygranej (jeśli gra zakończona)
- Komunikaty o wyniku (wygrana, przegrana, remis)

**Kluczowe komponenty widoku**:
- `GameBoardComponent` - plansza gry z komórkami
- `GameInfoComponent` - informacje o grze (przeciwnik, status, tury)
- `GameTimerComponent` - timer dla PvP (10 sekund)
- `GameBotIndicatorComponent` - wskaźnik "Bot myśli..." (dla vs_bot)
- `GameResultDialogComponent` - dialog z wynikiem gry
- `SurrenderButtonComponent` - przycisk poddania (dla PvP)

**Integracja API**:
- `GET /api/games/{gameId}` - pobranie stanu gry
- `POST /api/games/{gameId}/moves` - wykonanie ruchu
- `PUT /api/games/{gameId}/status` - poddanie gry
- WebSocket `/ws/game/{gameId}` - komunikacja real-time (PvP)
- `POST /api/games/{gameId}/bot-move` - automatyczny ruch bota (wewnętrzne)

**Stany gry**:
- `waiting` - oczekiwanie na przeciwnika (PvP)
- `in_progress` - gra w toku
- `finished` - gra zakończona (wygrana/przegrana)
- `draw` - remis
- `abandoned` - gra porzucona

**Animacje**:
- Pojawienie się symbolu: scale (0 → 1) + fade-in, 300ms
- Ruch bota: opóźnienie 200ms + animacja symbolu
- Linia wygranej: stroke-dasharray animation, 500ms opóźnienie

**UX, dostępność i względy bezpieczeństwa**:
- **UX**: Intuicyjna plansza, wizualne feedback dla ruchów, czytelne informacje o stanie gry, animacje poprawiające doświadczenie
- **Dostępność**: ARIA labels dla komórek, keyboard navigation (opcjonalne), focus indicators, screen reader support dla statusu
- **Bezpieczeństwo**: Walidacja ruchów po stronie klienta i serwera, blokada podwójnych ruchów, timeout dla PvP

**Mapowanie historyjek użytkownika**:
- US-004: Rozgrywka z botem (łatwy poziom)
- US-005: Rozgrywka z botem (średni poziom)
- US-006: Rozgrywka z botem (trudny poziom)
- US-007: Dołączenie do gry PvP
- US-008: Rozgrywka PvP z funkcjonalnościami
- US-013: Obsługa rozłączeń w PvP
- US-014: Walidacja ruchów w grze

---

### 2.5 GameBoardComponent - Plansza gry

**Lokalizacja**: `components/game/game-board.component.ts` (komponent współdzielony używany w GameComponent)

**Główny cel**: 
Renderowanie planszy gry i obsługa interakcji użytkownika z komórkami planszy.

**Kluczowe informacje do wyświetlenia**:
- Dynamiczna plansza (3x3, 4x4, 5x5) z komórkami
- Symbole X i O w komórkach
- Wizualizacja linii wygranej (jeśli gra zakończona)
- Stany komórek (pusta, zajęta, disabled)

**Kluczowe komponenty widoku**:
- `GameCellComponent` - pojedyncza komórka planszy (używana w *ngFor)

**Implementacja**:
- CSS Grid: `grid-template-columns: repeat(boardSize, 1fr)`
- Komórki jako osobne komponenty `GameCellComponent`
- Stan planszy z `boardState` z API
- Blokada kliknięć na zajęte pola (disabled state)

**Animacje**:
- Pojawienie się symbolu: scale (0 → 1) + fade-in, 300ms
- Linia wygranej: stroke-dasharray animation, 500ms opóźnienie

**UX, dostępność i względy bezpieczeństwa**:
- **UX**: Wizualne wyróżnienie dostępnych komórek, animacje ruchów, czytelna plansza
- **Dostępność**: ARIA labels dla komórek, keyboard navigation (opcjonalne), focus indicators
- **Bezpieczeństwo**: Walidacja kliknięć, blokada podwójnych ruchów

---

### 2.6 MatchmakingComponent - Oczekiwanie na przeciwnika

**Ścieżka**: `/game/matchmaking`

**Główny cel**: 
Wyświetlanie stanu oczekiwania na przeciwnika w kolejce matchmakingu i umożliwienie anulowania kolejki.

**Kluczowe informacje do wyświetlenia**:
- Animacja ładowania
- Wskaźnik postępu
- Szacowany czas oczekiwania
- Informacja o rozmiarze planszy
- Przycisk anulowania kolejki

**Kluczowe komponenty widoku**:
- PrimeNG ProgressSpinner - animacja ładowania
- `MatchmakingStatusComponent` - wskaźnik statusu i czasu oczekiwania
- PrimeNG Button - przycisk anulowania

**Integracja API**:
- `POST /api/v1/matching/queue` - dołączenie do kolejki (automatyczne przy wejściu)
- `DELETE /api/v1/matching/queue` - anulowanie kolejki
- Polling `GET /api/games/{gameId}` co 2 sekundy (alternatywa dla WebSocket) lub WebSocket notification

**Po znalezieniu przeciwnika**:
- Automatyczne przekierowanie do `GameComponent` z `gameId`
- Nawiązanie połączenia WebSocket

**UX, dostępność i względy bezpieczeństwa**:
- **UX**: Wizualne wskaźniki postępu, możliwość anulowania, czytelne informacje o stanie
- **Dostępność**: ARIA labels, screen reader announcements dla zmian statusu
- **Bezpieczeństwo**: Automatyczne opuszczenie kolejki przy rozłączeniu, timeout dla matchmakingu

**Mapowanie historyjek użytkownika**:
- US-007: Dołączenie do gry PvP

---

### 2.7 GameModeSelectionComponent - Wybór trybu vs_bot

**Ścieżka**: `/game/mode-selection`

**Główny cel**: 
Umożliwienie użytkownikowi wyboru rozmiaru planszy i poziomu trudności bota przed rozpoczęciem gry vs bot.

**Kluczowe informacje do wyświetlenia**:
- Wybór rozmiaru planszy (3x3, 4x4, 5x5) - karty z wizualizacją
- Wybór poziomu trudności (łatwy, średni, trudny) - karty z opisem i punktacją:
  - Łatwy: +100 pkt za wygraną
  - Średni: +500 pkt za wygraną
  - Trudny: +1000 pkt za wygraną
- Przycisk rozpoczęcia gry
- Informacje o systemie punktowym

**Kluczowe komponenty widoku**:
- `BoardSizeCardComponent` - karty z rozmiarami planszy
- `DifficultyCardComponent` - karty z poziomami trudności
- PrimeNG Button - przycisk rozpoczęcia gry

**Integracja API**:
- `POST /api/games` - utworzenie gry vs_bot z wybranymi parametrami

**Po utworzeniu gry**:
- Przekierowanie do `GameComponent` z `gameId`

**UX, dostępność i względy bezpieczeństwa**:
- **UX**: Wizualne karty z wyborem, czytelne informacje o punktacji, intuicyjna nawigacja
- **Dostępność**: ARIA labels, keyboard navigation, focus indicators
- **Bezpieczeństwo**: Walidacja wyboru przed utworzeniem gry

**Mapowanie historyjek użytkownika**:
- US-004: Rozgrywka z botem (łatwy poziom)
- US-005: Rozgrywka z botem (średni poziom)
- US-006: Rozgrywka z botem (trudny poziom)

---

### 2.8 LeaderboardComponent - Ranking graczy

**Ścieżka**: `/leaderboard`

**Główny cel**: 
Wyświetlanie globalnego rankingu graczy z możliwością przeglądania pozycji i wyboru przeciwnika.

**Kluczowe informacje do wyświetlenia**:
- Pozycja użytkownika w rankingu (jeśli zarejestrowany) - wyróżniona karta
- Tabela z rankingiem (paginacja):
  - Pozycja w rankingu
  - Nazwa użytkownika
  - Punkty
  - Rozegrane gry
  - Wygrane gry
- Przycisk "Pokaż graczy wokół mnie" (dla zarejestrowanych)
- Możliwość kliknięcia na gracza w rankingu (wyzwanie do gry)
- Filtry i sortowanie (opcjonalne)

**Kluczowe komponenty widoku**:
- PrimeNG Table - tabela z rankingiem
- `UserRankCardComponent` - karta z pozycją użytkownika
- `PlayersAroundDialogComponent` - dialog z graczami wokół użytkownika
- PrimeNG Paginator - paginacja

**Integracja API**:
- `GET /api/rankings/{userId}` - pozycja użytkownika (jeśli zarejestrowany)
- `GET /api/rankings` - lista graczy (paginacja)
- `GET /api/rankings/around/{userId}` - gracze wokół użytkownika
- `POST /api/v1/matching/challenge/{userId}` - wyzwanie gracza do gry

**Interakcje**:
- Kliknięcie na gracza w rankingu → sprawdzenie dostępności → wyzwanie do gry → przekierowanie do GameComponent

**UX, dostępność i względy bezpieczeństwa**:
- **UX**: Wizualne wyróżnienie pozycji użytkownika, czytelna tabela, łatwa nawigacja, możliwość wyboru przeciwnika
- **Dostępność**: ARIA labels dla tabeli, keyboard navigation, screen reader support
- **Bezpieczeństwo**: Sprawdzanie dostępności gracza przed wyzwaniem, walidacja uprawnień

**Mapowanie historyjek użytkownika**:
- US-009: Przeglądanie rankingu graczy
- US-010: Wybór przeciwnika z rankingu

---

### 2.9 ProfileComponent - Profil użytkownika

**Ścieżka**: `/profile`

**Główny cel**: 
Wyświetlanie i zarządzanie profilem użytkownika z podstawowymi informacjami, statystykami i ostatnią grą.

**Kluczowe informacje do wyświetlenia**:
- Podstawowe informacje:
  - Nazwa użytkownika (dla zarejestrowanych)
  - Email (dla zarejestrowanych)
  - Status (gość/zarejestrowany)
- Statystyki:
  - Punkty (totalPoints)
  - Rozegrane gry (gamesPlayed)
  - Wygrane gry (gamesWon)
  - Pozycja w rankingu (jeśli zarejestrowany)
- Ostatnia zapisana gra (jeśli istnieje) - karta z możliwością kontynuacji
- Możliwość edycji nazwy użytkownika (tylko zarejestrowani)
- Zachęta do rejestracji (dla gości)

**Kluczowe komponenty widoku**:
- PrimeNG Cards - karty ze statystykami
- `LastGameCardComponent` - karta z ostatnią grą
- `EditUsernameDialogComponent` - dialog edycji nazwy
- `UserStatsComponent` - komponent ze statystykami

**Integracja API**:
- `GET /api/auth/me` - profil użytkownika
- `GET /api/rankings/{userId}` - pozycja w rankingu (jeśli zarejestrowany)
- `GET /api/games?status=in_progress&size=1` - ostatnia gra
- `PUT /api/v1/users/{userId}` - aktualizacja nazwy użytkownika

**Dla gości**:
- Ograniczone informacje (punkty, rozegrane gry, wygrane)
- Zachęta do rejestracji z linkiem do `/auth/register`

**UX, dostępność i względy bezpieczeństwa**:
- **UX**: Czytelne statystyki, łatwa edycja profilu, wizualne wyróżnienie ważnych informacji
- **Dostępność**: ARIA labels, keyboard navigation, screen reader support
- **Bezpieczeństwo**: Walidacja edycji nazwy użytkownika, sprawdzanie uprawnień, bezpieczne wyświetlanie danych

**Mapowanie historyjek użytkownika**:
- US-011: Zarządzanie profilem gracza
- US-012: Automatyczne zapisywanie gier - kontynuacja ostatniej gry

---

### 2.10 NotFoundComponent - Strona błędu 404

**Ścieżka**: `/404` lub `**` (catch-all)

**Główny cel**: 
Wyświetlanie komunikatu o błędzie, gdy użytkownik próbuje uzyskać dostęp do nieistniejącej strony.

**Kluczowe informacje do wyświetlenia**:
- Komunikat o błędzie 404
- Informacja o nieistniejącej stronie
- Link powrotu do strony głównej
- Link do rankingu lub innych głównych sekcji

**Kluczowe komponenty widoku**:
- PrimeNG Message - komunikat o błędzie
- PrimeNG Button - przycisk powrotu

**UX, dostępność i względy bezpieczeństwa**:
- **UX**: Przyjazny komunikat, łatwa nawigacja z powrotem
- **Dostępność**: ARIA labels, keyboard navigation
- **Bezpieczeństwo**: Brak wrażliwych informacji w komunikacie

---

## 3. Mapa podróży użytkownika

### 3.1 Scenariusz I: Gracz gość → PvP

**Kroki**:
1. **Ekran startowy** (`/`)
   - Użytkownik widzi opcje gry
   - Kliknięcie "Graj jako gość" → `POST /api/guests` (automatyczne utworzenie sesji gościa)
   - Kliknięcie "Graj PvP" → przekierowanie do `/game/matchmaking`

2. **Oczekiwanie na przeciwnika** (`/game/matchmaking`)
   - Wyświetlenie animacji ładowania
   - `POST /api/v1/matching/queue` z boardSize (automatyczne przy wejściu)
   - Polling lub WebSocket czeka na przeciwnika
   - Po znalezieniu: automatyczne przekierowanie do `/game/:gameId`

3. **Widok gry** (`/game/:gameId`)
   - Nawiązanie połączenia WebSocket
   - Wyświetlenie planszy i informacji o grze
   - Wykonywanie ruchów przez WebSocket
   - Timer dla każdego gracza (10 sekund)
   - Po zakończeniu: wyświetlenie wyniku i przekierowanie do home

**Mapowanie historyjek**: US-001, US-007, US-008, US-013

---

### 3.2 Scenariusz II: Rejestracja → Logowanie

**Kroki**:
1. **Ekran startowy** (`/`)
   - Kliknięcie "Zarejestruj się" → `/auth/register`

2. **Rejestracja** (`/auth/register`)
   - Wypełnienie formularza (username, email, hasło, potwierdzenie hasła)
   - Walidacja pól (reactive forms)
   - `POST /api/auth/register`
   - Po sukcesie: automatyczne logowanie i przekierowanie do `/`

3. **Logowanie** (`/auth/login`) - opcjonalne, jeśli użytkownik chce się zalogować później
   - Wypełnienie formularza (email, hasło)
   - `POST /api/auth/login`
   - Po sukcesie: przekierowanie do `/` lub ostatniej gry

**Mapowanie historyjek**: US-002, US-003

---

### 3.3 Scenariusz III: Gracz gość → vs bot

**Kroki**:
1. **Ekran startowy** (`/`)
   - Kliknięcie "Graj jako gość" → `POST /api/guests` (automatyczne utworzenie sesji gościa)
   - Kliknięcie "Graj z botem" → `/game/mode-selection`

2. **Wybór trybu** (`/game/mode-selection`)
   - Wybór rozmiaru planszy (3x3, 4x4, 5x5)
   - Wybór poziomu trudności (łatwy, średni, trudny)
   - `POST /api/games` z parametrami
   - Przekierowanie do `/game/:gameId`

3. **Widok gry** (`/game/:gameId`)
   - Wyświetlenie planszy
   - Wykonywanie ruchów przez REST API (`POST /api/games/{gameId}/moves`)
   - Po ruchu gracza: automatyczny ruch bota (wewnętrzne wywołanie)
   - Po zakończeniu: wyświetlenie wyniku i przekierowanie do home

**Mapowanie historyjek**: US-001, US-004, US-005, US-006

---

### 3.4 Scenariusz IV: Przegląd rankingu → Wybór przeciwnika

**Kroki**:
1. **Ekran startowy** (`/`)
   - Kliknięcie "Ranking" w menu → `/leaderboard`

2. **Ranking** (`/leaderboard`)
   - Wyświetlenie tabeli z rankingiem
   - `GET /api/rankings` z paginacją
   - Kliknięcie na gracza → sprawdzenie dostępności
   - `POST /api/v1/matching/challenge/{userId}` z boardSize
   - Przekierowanie do `/game/:gameId`

3. **Widok gry** (`/game/:gameId`)
   - Standardowa rozgrywka PvP

**Mapowanie historyjek**: US-009, US-010

---

### 3.5 Scenariusz V: Kontynuacja zapisanej gry

**Kroki**:
1. **Ekran startowy** (`/`)
   - Sprawdzenie zapisanej gry (`GET /api/games?status=in_progress&size=1`)
   - Wyświetlenie bannera z ostatnią grą (jeśli istnieje)
   - Kliknięcie "Kontynuuj grę" → `/game/:gameId`

2. **Widok gry** (`/game/:gameId`)
   - Załadowanie stanu gry
   - Kontynuacja rozgrywki

**Mapowanie historyjek**: US-012

---

## 4. Układ i struktura nawigacji

### 4.1 Główny layout

**MainLayoutComponent** zawiera:
- **Header** (NavbarComponent) - zawsze widoczny na górze
  - Logo aplikacji (link do `/`)
  - Menu nawigacyjne:
    - Graj (dropdown: vs bot, PvP)
    - Ranking (link do `/leaderboard`)
    - Profil (link do `/profile`)
  - Wskaźnik statusu użytkownika:
    - "Gość" (dla gości)
    - Nazwa użytkownika (dla zarejestrowanych)
  - Przycisk logowania/wylogowania
- **Router outlet** - miejsce na widoki (zawsze widoczne)
- **Toast container** - PrimeNG Toast dla powiadomień (globalny)

### 4.2 NavbarComponent - Nawigacja główna

**Funkcjonalność**:
- Logo aplikacji (link do `/`)
- Menu nawigacyjne:
  - **Graj** (dropdown menu):
    - "Graj z botem" → `/game/mode-selection`
    - "Graj PvP" → `/game/matchmaking`
  - **Ranking** → `/leaderboard`
  - **Profil** → `/profile`
- Wskaźnik statusu użytkownika:
  - Dla gości: "Gość" + przycisk "Zaloguj się"
  - Dla zarejestrowanych: nazwa użytkownika + przycisk "Wyloguj się"
- Responsywność: collapse do hamburger menu dla mniejszych ekranów

**Komponenty**:
- PrimeNG Menu - menu nawigacyjne
- PrimeNG Avatar - avatar użytkownika (opcjonalne)
- PrimeNG Button - przyciski logowania/wylogowania

**Integracja**:
- AuthService - status użytkownika
- Router - nawigacja

### 4.3 Nawigacja między widokami

**Przepływy nawigacji**:
1. **Home → Game**: Przekierowanie przez router z parametrem `gameId`
2. **Home → Auth**: Przekierowanie do `/auth/login` lub `/auth/register`
3. **Game → Home**: Po zakończeniu gry lub anulowaniu
4. **Matchmaking → Game**: Automatyczne przekierowanie po znalezieniu przeciwnika
5. **Leaderboard → Game**: Przekierowanie po wyzwaniu gracza
6. **Profile → Game**: Przekierowanie po kliknięciu "Kontynuuj grę"

**Guardy routingu**:
- `AuthGuard` - ochrona widoków wymagających uwierzytelnienia (opcjonalne dla MVP)
- `GuestGuard` - automatyczne utworzenie sesji gościa dla widoków publicznych

### 4.4 Breadcrumbs (opcjonalne)

Dla lepszej nawigacji można dodać breadcrumbs:
- Home → Game Mode Selection → Game
- Home → Leaderboard → Game
- Home → Profile

---

## 5. Kluczowe komponenty

### 5.1 NavbarComponent - Nawigacja główna

**Lokalizacja**: `components/navigation/navbar/navbar.component.ts`

**Funkcjonalność**:
- Logo aplikacji (link do home)
- Menu nawigacyjne (Graj, Ranking, Profil)
- Wskaźnik statusu użytkownika (gość/zarejestrowany)
- Przycisk logowania/wylogowania

**Komponenty**:
- PrimeNG Menu - menu nawigacyjne
- PrimeNG Avatar - avatar użytkownika

**Integracja**:
- AuthService - status użytkownika
- Router - nawigacja

---

### 5.2 GameBannerComponent - Banner z ostatnią grą

**Lokalizacja**: `components/game/game-banner.component.ts`

**Funkcjonalność**:
- Wyświetlanie informacji o ostatniej zapisanej grze
- Przycisk "Kontynuuj grę"
- Wyświetlany tylko jeśli gra istnieje

**Dane wyświetlane**:
- Typ gry (vs_bot / PvP)
- Przeciwnik (bot / nazwa gracza)
- Data rozpoczęcia
- Status (in_progress)

**Integracja**:
- GameService - pobranie ostatniej gry

---

### 5.3 LoaderComponent - Wskaźnik ładowania

**Lokalizacja**: `components/ui/loader/loader.component.ts`

**Funkcjonalność**:
- Globalny wskaźnik ładowania
- Używany podczas żądań API
- Animacja spinner (PrimeNG ProgressSpinner)

**Integracja**:
- LoaderService - zarządzanie stanem ładowania

---

### 5.4 ButtonComponent - Przycisk

**Lokalizacja**: `components/ui/button/button.component.ts`

**Funkcjonalność**:
- Standaryzowany przycisk
- Warianty (primary, secondary, danger)
- Rozmiary (small, medium, large)
- Stany (disabled, loading)

---

### 5.5 GameCellComponent - Komórka planszy

**Lokalizacja**: `components/game/game-cell.component.ts`

**Funkcjonalność**:
- Pojedyncza komórka planszy
- Wyświetlanie symbolu (X, O) lub pustej
- Obsługa kliknięć
- Stany (disabled, selected, winning)

**Animacje**:
- Pojawienie się symbolu: scale (0 → 1) + fade-in

---

### 5.6 ErrorDisplayComponent - Wyświetlanie błędów

**Lokalizacja**: `components/ui/error-display.component.ts`

**Funkcjonalność**:
- Wyświetlanie błędów walidacji formularzy
- Komunikaty błędów API
- Toast notifications

**Integracja**:
- ErrorService - centralna obsługa błędów

---

### 5.7 GameTimerComponent - Timer w grze PvP

**Lokalizacja**: `components/game/game-timer.component.ts`

**Funkcjonalność**:
- Wyświetlanie pozostałego czasu na ruch (10 sekund)
- Wizualne ostrzeżenia (warning, danger)
- Animacja pulse dla małej ilości czasu

**Stany**:
- Normal (10-4 sekundy)
- Warning (3-2 sekundy)
- Danger (1 sekunda)

---

### 5.8 GameResultDialogComponent - Dialog z wynikiem gry

**Lokalizacja**: `components/game/game-result-dialog.component.ts`

**Funkcjonalność**:
- Wyświetlanie wyniku gry (wygrana, przegrana, remis)
- Informacje o zdobytych punktach
- Przycisk powrotu do home

**Komponenty**:
- PrimeNG Dialog

---

## 6. Mapowanie historyjek użytkownika do widoków

### US-001: Rozpoczęcie gry jako gość
- **Widok**: HomeComponent
- **Komponent**: Przycisk "Graj jako gość"
- **API**: `POST /api/guests`

### US-002: Rejestracja nowego użytkownika
- **Widok**: AuthRegisterComponent
- **Komponent**: Formularz rejestracji
- **API**: `POST /api/auth/register`

### US-003: Logowanie zarejestrowanego użytkownika
- **Widok**: AuthLoginComponent
- **Komponent**: Formularz logowania
- **API**: `POST /api/auth/login`

### US-004: Rozgrywka z botem (łatwy poziom)
- **Widok**: GameModeSelectionComponent → GameComponent
- **Komponent**: Wybór trybu, plansza gry
- **API**: `POST /api/games`, `POST /api/games/{gameId}/moves`

### US-005: Rozgrywka z botem (średni poziom)
- **Widok**: GameModeSelectionComponent → GameComponent
- **Komponent**: Wybór trybu, plansza gry
- **API**: `POST /api/games`, `POST /api/games/{gameId}/moves`

### US-006: Rozgrywka z botem (trudny poziom)
- **Widok**: GameModeSelectionComponent → GameComponent
- **Komponent**: Wybór trybu, plansza gry
- **API**: `POST /api/games`, `POST /api/games/{gameId}/moves`

### US-007: Dołączenie do gry PvP
- **Widok**: MatchmakingComponent → GameComponent
- **Komponent**: Oczekiwanie na przeciwnika, plansza gry
- **API**: `POST /api/v1/matching/queue`, WebSocket `/ws/game/{gameId}`

### US-008: Rozgrywka PvP z funkcjonalnościami
- **Widok**: GameComponent
- **Komponent**: Plansza gry, timer, przycisk poddania
- **API**: WebSocket `/ws/game/{gameId}`, `PUT /api/games/{gameId}/status`

### US-009: Przeglądanie rankingu graczy
- **Widok**: LeaderboardComponent
- **Komponent**: Tabela rankingu
- **API**: `GET /api/rankings`

### US-010: Wybór przeciwnika z rankingu
- **Widok**: LeaderboardComponent → GameComponent
- **Komponent**: Tabela rankingu, wyzwanie gracza
- **API**: `POST /api/v1/matching/challenge/{userId}`

### US-011: Zarządzanie profilem gracza
- **Widok**: ProfileComponent
- **Komponent**: Profil użytkownika, statystyki
- **API**: `GET /api/auth/me`, `PUT /api/v1/users/{userId}`

### US-012: Automatyczne zapisywanie gier
- **Widok**: HomeComponent, ProfileComponent
- **Komponent**: GameBannerComponent, LastGameCardComponent
- **API**: `GET /api/games?status=in_progress&size=1`

### US-013: Obsługa rozłączeń w PvP
- **Widok**: GameComponent
- **Komponent**: WebSocket reconnect, komunikaty o rozłączeniu
- **API**: WebSocket `/ws/game/{gameId}`

### US-014: Walidacja ruchów w grze
- **Widok**: GameComponent
- **Komponent**: GameBoardComponent, walidacja po stronie klienta
- **API**: `POST /api/games/{gameId}/moves`

### US-015: Responsywność interfejsu
- **Wszystkie widoki**: Responsywność na poziomie komponentów
- **Komponenty**: Adaptacja layoutu dla różnych rozdzielczości

---

## 7. Obsługa błędów i przypadki brzegowe

### 7.1 Stany błędów

**Błędy API**:
- 400 Bad Request: Toast notification z komunikatem
- 401 Unauthorized: Przekierowanie do `/auth/login`
- 403 Forbidden: Toast notification z komunikatem
- 404 Not Found: Toast notification lub przekierowanie do 404
- 409 Conflict: Toast notification z komunikatem (np. nazwa użytkownika już istnieje)
- 422 Unprocessable Entity: Wyświetlenie błędów walidacji w formularzu
- 500 Internal Server Error: Toast notification z komunikatem

**Błędy WebSocket**:
- Rozłączenie: Automatyczna próba reconnect (max 20 sekund)
- Timeout: Komunikat o timeout i przekierowanie do home
- Błąd połączenia: Toast notification z komunikatem

### 7.2 Przypadki brzegowe

**Brak zapisanej gry**:
- GameBannerComponent nie jest wyświetlany
- Brak komunikatu błędu

**Brak przeciwników w matchmakingu**:
- Wyświetlenie komunikatu o braku przeciwników
- Możliwość anulowania kolejki

**Gra zakończona przed wejściem**:
- Sprawdzenie statusu gry przy wejściu
- Przekierowanie do home z komunikatem

**Timeout w grze PvP**:
- Automatyczne zakończenie gry po 20 sekundach nieaktywności
- Komunikat o timeout i przekierowanie do home

**Brak uprawnień**:
- Toast notification z komunikatem
- Przekierowanie do odpowiedniego widoku

---

## 8. Dostępność i UX

### 8.1 Dostępność

**Podstawowe wymagania**:
- ARIA labels dla wszystkich interaktywnych elementów
- Keyboard navigation dla formularzy i nawigacji
- Focus indicators dla wszystkich przycisków
- Alt text dla ikon i obrazów
- Semantic HTML (header, nav, main, footer)
- Screen reader support dla statusu gry i komunikatów

**Kontrast i czytelność**:
- Minimalny kontrast 4.5:1 dla tekstu
- Wizualne wskaźniki focus
- Czytelne czcionki (min. 14px)

### 8.2 UX

**Zasady projektowe**:
- Spójność wizualna we wszystkich widokach
- Natychmiastowy feedback dla akcji użytkownika
- Czytelne komunikaty błędów i sukcesu
- Płynne animacje poprawiające doświadczenie
- Intuicyjna nawigacja

**Optymalizacja wydajności**:
- Lazy loading dla feature modules
- OnPush change detection strategy
- Cache'owanie odpowiedzi API
- Optymalizacja animacji

---

## 9. Bezpieczeństwo

### 9.1 Uwierzytelnianie

- Tokeny JWT przechowywane w localStorage
- Automatyczne odświeżanie tokenów (jeśli wymagane)
- Wylogowanie przy wygaśnięciu tokenu

### 9.2 Walidacja

- Walidacja po stronie klienta (reactive forms)
- Walidacja po stronie serwera (API)
- Sanityzacja danych wejściowych

### 9.3 Ochrona danych

- Brak wrażliwych danych w URL
- Bezpieczne przechowywanie tokenów
- HTTPS dla wszystkich żądań

---

## 10. Wsparcie dla wielu języków (i18n)

### 10.1 Przegląd

Aplikacja wspiera wiele języków (i18n) z angielskim jako językiem podstawowym i polskim jako językiem dodatkowym. Implementacja i18n jest realizowana wyłącznie po stronie UI (frontend) przy użyciu Angular i18n, backend pozostaje bez zmian i nie wymaga modyfikacji.

### 10.2 Języki wspierane

- **Angielski (en)** - język podstawowy, domyślny
- **Polski (pl)** - język dodatkowy

### 10.3 Implementacja

- **Angular i18n** - wykorzystanie wbudowanego systemu i18n Angular 17
- **Pliki tłumaczeń** - pliki JSON z tłumaczeniami dla każdego języka
- **Lokalizacja** - automatyczne wykrywanie języka przeglądarki lub wybór przez użytkownika
- **Przełącznik języka** - komponent w NavbarComponent umożliwiający zmianę języka

### 10.4 Zakres tłumaczeń

Wszystkie teksty w interfejsie użytkownika są tłumaczone:
- Etykiety przycisków
- Komunikaty błędów i sukcesu
- Teksty formularzy
- Komunikaty toast notifications
- Komunikaty WebSocket
- Teksty w komponentach

### 10.5 Backend

Backend pozostaje bez zmian - wszystkie komunikaty błędów i odpowiedzi API są w języku angielskim. Tłumaczenie komunikatów z backendu na język użytkownika odbywa się po stronie frontendu.

### 10.6 Opcjonalne endpointy API

Niektóre endpointy zdefiniowane w API Plan nie są używane w widokach MVP, ale są dostępne do użycia w przyszłości:

- `POST /api/auth/logout` - wylogowanie użytkownika (można dodać do NavbarComponent)
- `GET /api/v1/users/{userId}` - pobranie profilu użytkownika po ID (można użyć do wyświetlania profili innych graczy)
- `POST /api/v1/users/{userId}/last-seen` - aktualizacja ostatniej aktywności (można wywoływać automatycznie dla matchmakingu)
- `GET /api/games/{gameId}/moves` - pobranie historii ruchów (można użyć do wyświetlania historii)
- `GET /api/games/{gameId}/board` - pobranie stanu planszy (specjalizowany endpoint, alternatywa dla `GET /api/games/{gameId}`)

Te endpointy są zdefiniowane w API Plan i gotowe do użycia, ale nie są wymagane dla MVP.

## 11. Podsumowanie

Architektura UI dla World at War: Turn-Based Strategy została zaprojektowana jako kompleksowe rozwiązanie spełniające wszystkie wymagania z PRD, zintegrowane z planem API i uwzględniające najlepsze praktyki UX, dostępności i bezpieczeństwa. Wszystkie historyjki użytkownika (US-001 do US-015) są zmapowane do odpowiednich widoków i komponentów, zapewniając spójne i intuicyjne doświadczenie użytkownika.

Struktura jest skalowalna i gotowa do implementacji, z jasno zdefiniowanymi komponentami, integracjami API i przepływami użytkownika. Wszystkie widoki są zaprojektowane z myślą o responsywności, dostępności i bezpieczeństwie, zapewniając wysokiej jakości interfejs użytkownika dla aplikacji produkcyjnej.

Aplikacja wspiera wiele języków (i18n) z angielskim jako językiem podstawowym i polskim jako językiem dodatkowym, co umożliwia dostępność dla szerszej grupy użytkowników.
