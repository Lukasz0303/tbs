# Plan implementacji widoku HomeComponent

> **Źródło**: `.ai/implementation-plans-ui/01_home-component.md`

## 1. Przegląd

HomeComponent to ekran startowy aplikacji World at War: Turn-Based Strategy, służący jako główny punkt wejścia dla użytkowników. Komponent obsługuje zarówno gości, jak i zarejestrowanych użytkowników, umożliwiając natychmiastowe rozpoczęcie rozgrywki lub dostęp do funkcji wymagających rejestracji.

Główne funkcjonalności:
- Wyświetlanie statusu użytkownika (gość/zarejestrowany)
- Sprawdzanie i wyświetlanie zapisanej gry (jeśli istnieje)
- Wybór trybu gry (gość, bot, PvP)
- Nawigacja do rejestracji/logowania dla gości
- Automatyczne utworzenie sesji gościa przy pierwszym wejściu

Komponent realizuje historyjki użytkownika: US-001 (Rozpoczęcie gry jako gość) i US-012 (Automatyczne zapisywanie gier).

## 2. Routing widoku

**Ścieżka routingu**: `/`

**Konfiguracja routingu**:
```typescript
{
  path: '',
  component: HomeComponent,
  pathMatch: 'full'
}
```

**Lokalizacja pliku routingu**: `frontend/src/app/app.routes.ts` lub odpowiedni plik konfiguracji routingu

**Guardy**: Brak (widok publiczny, dostępny dla wszystkich)

## 3. Struktura komponentów

```
HomeComponent (główny komponent)
├── HomeHeaderComponent (opcjonalny, nagłówek z tytułem)
├── GameBannerComponent (warunkowy, wyświetlany gdy istnieje zapisana gra)
│   └── ButtonModule (PrimeNG - przycisk "Kontynuuj grę")
├── GameModeCardComponent (powtarzalny, karty trybów gry)
│   └── ButtonModule (PrimeNG - przycisk wyboru trybu)
└── AuthSectionComponent (warunkowy, wyświetlany tylko dla gości)
    └── ButtonModule (PrimeNG - przyciski logowania/rejestracji)
```

**Hierarchia komponentów**:
- HomeComponent jest komponentem standalone
- GameBannerComponent, GameModeCardComponent, AuthSectionComponent są komponentami współdzielonymi
- Wszystkie komponenty używają PrimeNG do elementów UI

## 4. Szczegóły komponentów

### HomeComponent

**Opis komponentu**: Główny komponent widoku, zarządza stanem użytkownika, zapisaną grą i nawigacją do różnych trybów gry. Obsługuje logikę biznesową związaną z sesją gościa i sprawdzaniem zapisanej gry.

**Główne elementy HTML**:
- Kontener główny (`.home-container`)
- Sekcja nagłówka (`.home-header`) z tytułem i podtytułem
- Warunkowy banner z grą (`<app-game-banner>`)
- Sekcja trybów gry (`.game-modes`) z kartami trybów
- Warunkowa sekcja autoryzacji (`.auth-section`) dla gości

**Obsługiwane zdarzenia**:
- `ngOnInit()` - inicjalizacja komponentu, sprawdzenie zapisanej gry, utworzenie sesji gościa
- `onGameModeSelected(mode: GameMode)` - obsługa wyboru trybu gry
- `onContinueGame(gameId: number)` - obsługa kontynuacji zapisanej gry
- `navigateToLogin()` - nawigacja do strony logowania
- `navigateToRegister()` - nawigacja do strony rejestracji

**Obsługiwana walidacja**:
- Sprawdzenie czy użytkownik jest gościem (przed wyświetleniem sekcji auth)
- Sprawdzenie czy istnieje zapisana gra (przed wyświetleniem bannera)
- Walidacja sesji gościa (przed utworzeniem nowej sesji)

**Typy**:
- `GameMode` - interfejs reprezentujący tryb gry
- `Game` - interfejs reprezentujący grę (zapisana gra)
- `User` - interfejs reprezentujący użytkownika
- `Observable<Game | null>` - Observable z zapisaną grą
- `Observable<boolean>` - Observable ze statusem gościa
- `Observable<User | null>` - Observable z aktualnym użytkownikiem

**Propsy**: Brak (komponent główny, nie przyjmuje propsów)

### GameBannerComponent

**Opis komponentu**: Komponent wyświetlający informacje o ostatniej zapisanej grze z możliwością kontynuacji. Wyświetlany warunkowo tylko gdy istnieje zapisana gra.

**Główne elementy HTML**:
- Kontener bannera (`.game-banner`)
- Informacje o grze (typ gry, data ostatniego ruchu, status)
- Przycisk "Kontynuuj grę" (PrimeNG Button)

**Obsługiwane zdarzenia**:
- `continueGame` - EventEmitter emitujący `gameId` po kliknięciu przycisku

**Obsługiwana walidacja**:
- Sprawdzenie czy gra istnieje (przed wyświetleniem)
- Sprawdzenie czy gra jest w statusie `in_progress` (tylko takie gry można kontynuować)

**Typy**:
- `Game` - interfejs reprezentujący grę

**Propsy**:
- `game: Game` - zapisana gra do wyświetlenia (wymagane)
- `continueGame: EventEmitter<number>` - emisja gameId przy kontynuacji gry

### GameModeCardComponent

**Opis komponentu**: Komponent reprezentujący kartę trybu gry z ikoną, etykietą i opisem. Obsługuje kliknięcie i przekierowanie do odpowiedniego widoku.

**Główne elementy HTML**:
- Kontener karty (`.game-mode-card`)
- Ikona trybu (PrimeNG Icon lub własna ikona)
- Tytuł trybu (`.mode-title`)
- Opis trybu (`.mode-description`)
- Przycisk wyboru (PrimeNG Button)

**Obsługiwane zdarzenia**:
- `modeSelected` - EventEmitter emitujący `GameMode` po kliknięciu karty

**Obsługiwana walidacja**:
- Sprawdzenie czy tryb jest dostępny dla danego użytkownika (gość/zarejestrowany)
- Walidacja routingu (sprawdzenie czy route istnieje)

**Typy**:
- `GameMode` - interfejs reprezentujący tryb gry

**Propsy**:
- `mode: GameMode` - tryb gry do wyświetlenia (wymagane)
- `isGuest: boolean` - status gościa (opcjonalne, domyślnie false)
- `modeSelected: EventEmitter<GameMode>` - emisja wybranego trybu

### AuthSectionComponent (opcjonalny, może być wbudowany w HomeComponent)

**Opis komponentu**: Sekcja wyświetlająca opcje rejestracji i logowania dla użytkowników gości. Wyświetlana warunkowo tylko dla gości.

**Główne elementy HTML**:
- Kontener sekcji (`.auth-section`)
- Tekst zachęcający do rejestracji
- Przycisk "Zaloguj się" (PrimeNG Button)
- Przycisk "Zarejestruj się" (PrimeNG Button)

**Obsługiwane zdarzenia**:
- `loginClick` - EventEmitter emitujący zdarzenie po kliknięciu logowania
- `registerClick` - EventEmitter emitujący zdarzenie po kliknięciu rejestracji

**Obsługiwana walidacja**: Brak (tylko nawigacja)

**Typy**: Brak (komponent prezentacyjny)

**Propsy**: Brak (komponent wbudowany w HomeComponent lub przyjmuje tylko `isGuest: boolean`)

## 5. Typy

### GameMode

```typescript
interface GameMode {
  id: 'guest' | 'bot' | 'pvp';
  label: string;
  description: string;
  icon: string;
  route: string | null;
  availableForGuest: boolean;
  availableForRegistered: boolean;
}
```

**Pola**:
- `id: 'guest' | 'bot' | 'pvp'` - unikalny identyfikator trybu gry
- `label: string` - etykieta wyświetlana użytkownikowi (np. "Graj jako gość")
- `description: string` - opis trybu gry
- `icon: string` - nazwa ikony (PrimeNG lub własna)
- `route: string | null` - ścieżka routingu do widoku trybu (null dla trybu gościa)
- `availableForGuest: boolean` - czy tryb jest dostępny dla gości
- `availableForRegistered: boolean` - czy tryb jest dostępny dla zarejestrowanych użytkowników

### Game

```typescript
interface Game {
  gameId: number;
  gameType: 'vs_bot' | 'pvp';
  boardSize: 3 | 4 | 5;
  status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw';
  player1Id: number;
  player2Id: number | null;
  botDifficulty: 'easy' | 'medium' | 'hard' | null;
  currentPlayerSymbol: 'x' | 'o' | null;
  winnerId: number | null;
  lastMoveAt: string | null;
  createdAt: string;
  updatedAt: string;
  finishedAt: string | null;
  totalMoves: number;
}
```

**Pola**:
- `gameId: number` - unikalny identyfikator gry
- `gameType: 'vs_bot' | 'pvp'` - typ gry (z botem lub PvP)
- `boardSize: 3 | 4 | 5` - rozmiar planszy
- `status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw'` - status gry
- `player1Id: number` - ID pierwszego gracza
- `player2Id: number | null` - ID drugiego gracza (null dla gier z botem)
- `botDifficulty: 'easy' | 'medium' | 'hard' | null` - poziom trudności bota (null dla PvP)
- `currentPlayerSymbol: 'x' | 'o' | null` - symbol aktualnego gracza
- `winnerId: number | null` - ID zwycięzcy (null jeśli gra nie zakończona)
- `lastMoveAt: string | null` - data ostatniego ruchu (ISO 8601)
- `createdAt: string` - data utworzenia gry (ISO 8601)
- `updatedAt: string` - data ostatniej aktualizacji (ISO 8601)
- `finishedAt: string | null` - data zakończenia gry (ISO 8601, null jeśli gra trwa)
- `totalMoves: number` - całkowita liczba wykonanych ruchów

### User

```typescript
interface User {
  userId: number;
  username: string | null;
  email: string | null;
  isGuest: boolean;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
  lastSeenAt: string | null;
}
```

**Pola**:
- `userId: number` - unikalny identyfikator użytkownika
- `username: string | null` - nazwa użytkownika (null dla gości)
- `email: string | null` - adres email (null dla gości)
- `isGuest: boolean` - flaga wskazująca czy użytkownik jest gościem
- `totalPoints: number` - suma punktów użytkownika
- `gamesPlayed: number` - liczba rozegranych gier
- `gamesWon: number` - liczba wygranych gier
- `createdAt: string` - data utworzenia konta (ISO 8601)
- `lastSeenAt: string | null` - data ostatniej aktywności (ISO 8601)

### SavedGameResponse (DTO z API)

```typescript
interface SavedGameResponse {
  content: Game[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
```

**Pola**:
- `content: Game[]` - tablica gier (powinna zawierać maksymalnie 1 element dla zapisanej gry)
- `totalElements: number` - całkowita liczba elementów
- `totalPages: number` - całkowita liczba stron
- `size: number` - rozmiar strony
- `number: number` - numer strony

### GuestSessionResponse (DTO z API)

```typescript
interface GuestSessionResponse {
  userId: number;
  isGuest: boolean;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
}
```

**Pola**:
- `userId: number` - ID utworzonego użytkownika gościa
- `isGuest: boolean` - zawsze true dla sesji gościa
- `totalPoints: number` - początkowa liczba punktów (0)
- `gamesPlayed: number` - początkowa liczba gier (0)
- `gamesWon: number` - początkowa liczba wygranych (0)
- `createdAt: string` - data utworzenia sesji (ISO 8601)

## 6. Zarządzanie stanem

**Strategia zarządzania stanem**: Reactive Forms + RxJS Observables + BehaviorSubject

**Stan komponentu**:
- `savedGame$: BehaviorSubject<Game | null>` - stan zapisanej gry (null jeśli brak)
- `isGuest$: Observable<boolean>` - Observable ze statusem gościa (z AuthService)
- `currentUser$: Observable<User | null>` - Observable z aktualnym użytkownikiem (z AuthService)

**Custom hooki**: Brak (komponent używa standardowych Observable z serwisów)

**Subskrypcje**:
- Subskrypcja do `savedGame$` w template przez `async` pipe
- Subskrypcja do `isGuest$` w template przez `async` pipe
- Subskrypcja do `currentUser$` w template przez `async` pipe
- Subskrypcja do `gameService.getSavedGame()` w `ngOnInit` dla aktualizacji stanu zapisanej gry
- Subskrypcja do `authService.createGuestSession()` w `ngOnInit` dla utworzenia sesji gościa

**Lifecycle hooks**:
- `ngOnInit()` - inicjalizacja: sprawdzenie zapisanej gry, utworzenie sesji gościa
- `ngOnDestroy()` - czyszczenie subskrypcji (jeśli używane bez async pipe)

**Wzorce RxJS**:
- `take(1)` - dla jednorazowych operacji (sprawdzenie statusu gościa)
- `filter()` - dla filtrowania wartości (tylko goście)
- `switchMap()` - dla przełączania między Observable (sprawdzenie statusu → utworzenie sesji)
- `catchError()` - dla obsługi błędów w Observable

## 7. Integracja API

### 7.1 Endpoint: GET /api/games?status=in_progress&size=1

**Cel**: Sprawdzenie czy istnieje zapisana gra w statusie `in_progress` dla bieżącego użytkownika

**Metoda HTTP**: GET

**Parametry zapytania**:
- `status: 'in_progress'` - filtrowanie gier w toku
- `size: 1` - maksymalnie 1 wynik (najnowsza gra)

**Nagłówki**:
- `Authorization: Bearer <JWT_TOKEN>` - token JWT (wymagane dla zarejestrowanych)
- `Accept: application/json`

**Odpowiedź sukcesu (200 OK)**:
```json
{
  "content": [
    {
      "gameId": 42,
      "gameType": "vs_bot",
      "boardSize": 3,
      "status": "in_progress",
      "player1Id": 123,
      "player2Id": null,
      "botDifficulty": "easy",
      "currentPlayerSymbol": "x",
      "winnerId": null,
      "lastMoveAt": "2024-01-20T15:30:00Z",
      "createdAt": "2024-01-20T14:00:00Z",
      "updatedAt": "2024-01-20T15:30:00Z",
      "finishedAt": null,
      "totalMoves": 5
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 1,
  "number": 0
}
```

**Odpowiedź gdy brak zapisanej gry (200 OK)**:
```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "size": 1,
  "number": 0
}
```

**Obsługa w komponencie**:
```typescript
private checkSavedGame(): void {
  this.gameService.getSavedGame().subscribe({
    next: (response) => {
      const game = response.content.length > 0 ? response.content[0] : null;
      this.savedGame$.next(game);
    },
    error: (error) => {
      console.error('Error loading saved game:', error);
      this.savedGame$.next(null);
    }
  });
}
```

### 7.2 Endpoint: POST /api/guests

**Cel**: Utworzenie lub pobranie profilu użytkownika gościa identyfikowanego przez IP

**Metoda HTTP**: POST

**Ciało żądania**:
```json
{
  "ipAddress": "192.168.1.1"
}
```

**Uwaga**: `ipAddress` jest opcjonalne - backend wyciąga IP z żądania jeśli nie podano

**Nagłówki**:
- `Content-Type: application/json`
- `Accept: application/json`

**Odpowiedź sukcesu (200 OK lub 201 Created)**:
```json
{
  "userId": 456,
  "isGuest": true,
  "totalPoints": 0,
  "gamesPlayed": 0,
  "gamesWon": 0,
  "createdAt": "2024-01-20T16:00:00Z"
}
```

**Obsługa w komponencie**:
```typescript
private ensureGuestSession(): void {
  this.isGuest$.pipe(
    take(1),
    filter(isGuest => isGuest),
    switchMap(() => this.authService.createGuestSession())
  ).subscribe({
    next: (response) => {
      console.log('Guest session created:', response);
    },
    error: (error) => {
      console.error('Error creating guest session:', error);
    }
  });
}
```

### 7.3 Serwisy Angular

**AuthService**:
- `isGuest(): Observable<boolean>` - sprawdzenie statusu gościa
- `getCurrentUser(): Observable<User | null>` - pobranie aktualnego użytkownika
- `createGuestSession(): Observable<GuestSessionResponse>` - utworzenie sesji gościa

**GameService**:
- `getSavedGame(): Observable<SavedGameResponse>` - pobranie zapisanej gry

**Router**:
- `navigate(commands: any[]): Promise<boolean>` - nawigacja do innych widoków

## 8. Interakcje użytkownika

### 8.1 Wybór trybu gry

**Scenariusz**: Użytkownik klika na kartę trybu gry

**Kroki**:
1. Użytkownik klika na `GameModeCardComponent`
2. Komponent emituje zdarzenie `modeSelected` z obiektem `GameMode`
3. `HomeComponent` odbiera zdarzenie w metodzie `onGameModeSelected(mode: GameMode)`
4. Jeśli tryb to `'guest'`:
   - Wywołanie `authService.createGuestSession()`
   - Po sukcesie: nawigacja do `/game/mode-selection`
5. Jeśli tryb ma `route`:
   - Bezpośrednia nawigacja do `mode.route`

**Obsługa błędów**:
- Błąd utworzenia sesji gościa: toast notification, możliwość ponowienia
- Błąd nawigacji: przekierowanie do 404

### 8.2 Kontynuacja zapisanej gry

**Scenariusz**: Użytkownik klika przycisk "Kontynuuj grę" w bannerze

**Kroki**:
1. Użytkownik klika przycisk w `GameBannerComponent`
2. Komponent emituje zdarzenie `continueGame` z `gameId`
3. `HomeComponent` odbiera zdarzenie w metodzie `onContinueGame(gameId: number)`
4. Nawigacja do `/game/{gameId}`

**Obsługa błędów**:
- Gra nie istnieje: przekierowanie do HomeComponent z komunikatem błędu
- Brak uprawnień: przekierowanie do HomeComponent z komunikatem błędu

### 8.3 Nawigacja do logowania/rejestracji

**Scenariusz**: Gość klika przycisk "Zaloguj się" lub "Zarejestruj się"

**Kroki**:
1. Użytkownik klika przycisk w sekcji auth
2. `HomeComponent` wywołuje `navigateToLogin()` lub `navigateToRegister()`
3. Nawigacja do `/auth/login` lub `/auth/register`

**Obsługa błędów**: Brak (tylko nawigacja)

### 8.4 Automatyczne utworzenie sesji gościa

**Scenariusz**: Gość wchodzi na stronę główną po raz pierwszy

**Kroki**:
1. `HomeComponent` inicjalizuje się w `ngOnInit()`
2. Sprawdzenie statusu gościa przez `authService.isGuest()`
3. Jeśli użytkownik jest gościem:
   - Wywołanie `authService.createGuestSession()`
   - Zapisanie informacji o sesji w localStorage/sessionStorage
4. Aktualizacja stanu użytkownika w `AuthService`

**Obsługa błędów**:
- Błąd utworzenia sesji: ciche logowanie, możliwość ponowienia przy wyborze trybu gry
- Timeout: toast notification z możliwością ponowienia

## 9. Warunki i walidacja

### 9.1 Warunki wyświetlania komponentów

**GameBannerComponent**:
- Warunek: `savedGame$ | async as game` - wyświetlany tylko gdy `game !== null`
- Dodatkowa walidacja: `game.status === 'in_progress'` - tylko gry w toku można kontynuować

**AuthSectionComponent**:
- Warunek: `isGuest$ | async` - wyświetlany tylko dla gości

**GameModeCardComponent**:
- Warunek: `mode.availableForGuest === true` (dla gości) lub `mode.availableForRegistered === true` (dla zarejestrowanych)
- Walidacja routingu: sprawdzenie czy `mode.route` istnieje przed nawigacją

### 9.2 Walidacja stanu użytkownika

**Sprawdzenie statusu gościa**:
- Metoda: `authService.isGuest()`
- Zwraca: `Observable<boolean>`
- Użycie: warunkowe wyświetlanie sekcji auth, warunkowe utworzenie sesji gościa

**Sprawdzenie zapisanej gry**:
- Metoda: `gameService.getSavedGame()`
- Zwraca: `Observable<SavedGameResponse>`
- Walidacja: sprawdzenie czy `response.content.length > 0` i `response.content[0].status === 'in_progress'`

### 9.3 Walidacja nawigacji

**Przed nawigacją do trybu gry**:
- Sprawdzenie czy użytkownik jest zalogowany (dla trybów wymagających rejestracji)
- Sprawdzenie czy sesja gościa istnieje (dla gości)
- Sprawdzenie czy route istnieje w konfiguracji routingu

**Przed kontynuacją gry**:
- Sprawdzenie czy gra istnieje (`game !== null`)
- Sprawdzenie czy gra jest w statusie `in_progress`
- Sprawdzenie czy użytkownik jest uczestnikiem gry (walidacja po stronie serwera)

### 9.4 Wpływ warunków na stan interfejsu

**Brak zapisanej gry**:
- `GameBannerComponent` nie jest wyświetlany
- Użytkownik widzi tylko opcje wyboru trybu gry

**Istnieje zapisana gra**:
- `GameBannerComponent` jest wyświetlany na górze strony
- Użytkownik może kontynuować grę lub wybrać nowy tryb

**Użytkownik jest gościem**:
- Sekcja auth jest wyświetlana na dole strony
- Sesja gościa jest automatycznie utworzona przy inicjalizacji

**Użytkownik jest zarejestrowany**:
- Sekcja auth nie jest wyświetlana
- Użytkownik widzi pełne opcje trybów gry

## 10. Obsługa błędów

### 10.1 Błędy API

**Błąd pobierania zapisanej gry (500 Internal Server Error)**:
- **Obsługa**: Ciche logowanie błędu, ustawienie `savedGame$.next(null)`
- **Efekt dla użytkownika**: Banner z grą nie jest wyświetlany, użytkownik może kontynuować normalnie
- **Komunikat**: Brak (błąd nie jest krytyczny)

**Błąd utworzenia sesji gościa (400 Bad Request, 500 Internal Server Error)**:
- **Obsługa**: Toast notification z komunikatem błędu, możliwość ponowienia
- **Efekt dla użytkownika**: Użytkownik może spróbować ponownie przy wyborze trybu gry
- **Komunikat**: "Nie udało się utworzyć sesji gościa. Spróbuj ponownie."

**Błąd nawigacji (404 Not Found)**:
- **Obsługa**: Przekierowanie do strony 404 lub powrót do HomeComponent
- **Efekt dla użytkownika**: Wyświetlenie strony błędu lub powrót do strony głównej
- **Komunikat**: "Strona nie została znaleziona"

### 10.2 Błędy walidacji

**Gra nie jest w statusie `in_progress`**:
- **Obsługa**: Banner z grą nie jest wyświetlany (filtrowanie po stronie serwera)
- **Efekt dla użytkownika**: Użytkownik nie widzi opcji kontynuacji zakończonej gry

**Użytkownik nie jest uczestnikiem gry**:
- **Obsługa**: Walidacja po stronie serwera przy próbie kontynuacji, przekierowanie do HomeComponent
- **Efekt dla użytkownika**: Toast notification z komunikatem błędu
- **Komunikat**: "Nie masz uprawnień do tej gry"

### 10.3 Błędy sieci

**Timeout żądania**:
- **Obsługa**: Toast notification z komunikatem błędu, możliwość ponowienia
- **Efekt dla użytkownika**: Użytkownik może spróbować ponownie
- **Komunikat**: "Przekroczono czas oczekiwania. Sprawdź połączenie internetowe."

**Brak połączenia z internetem**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik jest informowany o braku połączenia
- **Komunikat**: "Brak połączenia z internetem. Sprawdź swoje połączenie."

### 10.4 Globalna obsługa błędów

**Error Handler Service** (opcjonalny):
- Centralna obsługa błędów HTTP
- Przechwytywanie błędów 401 (Unauthorized) i przekierowanie do logowania
- Przechwytywanie błędów 403 (Forbidden) i wyświetlenie komunikatu
- Przechwytywanie błędów 500 (Internal Server Error) i wyświetlenie ogólnego komunikatu

**Toast Service** (PrimeNG MessageService):
- Wyświetlanie komunikatów błędów i sukcesu
- Automatyczne znikanie po określonym czasie
- Różne typy komunikatów (error, warning, info, success)

## 11. Kroki implementacji

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów i serwisów**:
- Weryfikacja czy `AuthService` istnieje i ma wymagane metody
- Weryfikacja czy `GameService` istnieje i ma metodę `getSavedGame()`
- Sprawdzenie czy komponenty współdzielone (`GameBannerComponent`, `GameModeCardComponent`) istnieją

**1.2 Utworzenie brakujących komponentów współdzielonych**:
- `GameBannerComponent` w `frontend/src/app/components/game/game-banner.component.ts`
- `GameModeCardComponent` w `frontend/src/app/components/game/game-mode-card.component.ts`

**1.3 Instalacja zależności PrimeNG**:
- Sprawdzenie czy `ButtonModule`, `CardModule` są zainstalowane
- Instalacja brakujących modułów PrimeNG jeśli potrzeba

**1.4 Utworzenie typów TypeScript**:
- Utworzenie pliku `frontend/src/app/models/game.model.ts` z interfejsami `Game`, `GameMode`
- Utworzenie pliku `frontend/src/app/models/user.model.ts` z interfejsem `User`
- Utworzenie pliku `frontend/src/app/models/api.model.ts` z DTO z API

### Krok 2: Implementacja serwisów (jeśli brakuje)

**2.1 Rozszerzenie AuthService**:
- Dodanie metody `isGuest(): Observable<boolean>`
- Dodanie metody `getCurrentUser(): Observable<User | null>`
- Dodanie metody `createGuestSession(): Observable<GuestSessionResponse>`

**2.2 Rozszerzenie GameService**:
- Dodanie metody `getSavedGame(): Observable<SavedGameResponse>`
- Implementacja wywołania API `GET /api/games?status=in_progress&size=1`

**2.3 Testy jednostkowe serwisów**:
- Testy dla `AuthService.isGuest()`
- Testy dla `AuthService.createGuestSession()`
- Testy dla `GameService.getSavedGame()`

### Krok 3: Implementacja komponentów współdzielonych

**3.1 Implementacja GameBannerComponent**:
- Utworzenie komponentu standalone
- Implementacja template z informacjami o grze
- Implementacja przycisku "Kontynuuj grę"
- Implementacja EventEmitter `continueGame`
- Stylowanie komponentu (SCSS)
- Testy jednostkowe

**3.2 Implementacja GameModeCardComponent**:
- Utworzenie komponentu standalone
- Implementacja template z kartą trybu gry
- Implementacja EventEmitter `modeSelected`
- Stylowanie komponentu (SCSS)
- Testy jednostkowe

### Krok 4: Implementacja HomeComponent

**4.1 Utworzenie komponentu**:
- Utworzenie pliku `frontend/src/app/features/home/home.component.ts`
- Utworzenie pliku `frontend/src/app/features/home/home.component.html`
- Utworzenie pliku `frontend/src/app/features/home/home.component.scss`

**4.2 Implementacja logiki komponentu**:
- Import wymaganych modułów (CommonModule, RouterModule, PrimeNG modules)
- Implementacja właściwości komponentu (`savedGame$`, `isGuest$`, `currentUser$`, `gameModes`)
- Implementacja metody `ngOnInit()` z inicjalizacją
- Implementacja metody `checkSavedGame()`
- Implementacja metody `ensureGuestSession()`
- Implementacja metody `onGameModeSelected(mode: GameMode)`
- Implementacja metody `onContinueGame(gameId: number)`
- Implementacja metod nawigacji (`navigateToLogin()`, `navigateToRegister()`)

**4.3 Implementacja template**:
- Struktura HTML z sekcjami (header, banner, game modes, auth section)
- Warunkowe wyświetlanie komponentów (`*ngIf`)
- Użycie `async` pipe dla Observable
- Integracja z komponentami współdzielonymi

**4.4 Stylowanie**:
- Implementacja stylów SCSS dla `.home-container`
- Stylowanie sekcji nagłówka
- Stylowanie sekcji trybów gry (grid layout)
- Stylowanie sekcji auth
- Responsywność dla różnych rozdzielczości ekranu

### Krok 5: Konfiguracja routingu

**5.1 Dodanie routingu**:
- Dodanie ścieżki `/` do konfiguracji routingu
- Powiązanie ścieżki z `HomeComponent`
- Ustawienie `pathMatch: 'full'`

**5.2 Testy routingu**:
- Testy jednostkowe routingu
- Testy E2E nawigacji do strony głównej

### Krok 6: Implementacja animacji

**6.1 Animacje Angular**:
- Fade-in dla banneru z grą (300ms)
- Scale animation dla kart trybów gry (hover effect)
- Smooth transitions dla przycisków

**6.2 CSS Transitions**:
- Transitions dla hover states
- Transitions dla focus states
- Transitions dla disabled states

### Krok 7: Implementacja obsługi błędów

**7.1 Obsługa błędów API**:
- Implementacja `catchError()` w Observable
- Implementacja toast notifications dla błędów
- Implementacja fallback values dla błędów

**7.2 Obsługa błędów nawigacji**:
- Implementacja error handler dla błędów routingu
- Przekierowanie do strony 404 dla nieistniejących tras

### Krok 8: Implementacja i18n

**8.1 Konfiguracja Angular i18n**:
- Konfiguracja plików tłumaczeń (en, pl)
- Dodanie kluczy tłumaczeń dla wszystkich tekstów w komponencie

**8.2 Użycie tłumaczeń**:
- Zastąpienie hardcoded tekstów pipe `translate`
- Testy dla różnych języków

### Krok 9: Testy

**9.1 Testy jednostkowe**:
- Testy dla `HomeComponent` (Jest + Angular Testing Library)
- Testy dla `GameBannerComponent`
- Testy dla `GameModeCardComponent`
- Testy dla serwisów (AuthService, GameService)

**9.2 Testy E2E (Cypress)**:
- Scenariusz: Gość → wybór trybu gry
- Scenariusz: Zarejestrowany → kontynuacja zapisanej gry
- Scenariusz: Gość → rejestracja
- Scenariusz: Gość → logowanie

### Krok 10: Dostępność (a11y)

**10.1 ARIA labels**:
- Dodanie `aria-label` dla wszystkich przycisków
- Dodanie `aria-describedby` dla sekcji z opisami

**10.2 Keyboard navigation**:
- Obsługa nawigacji klawiaturą dla kart trybów
- Obsługa Enter/Space dla aktywacji przycisków
- Focus indicators dla wszystkich interaktywnych elementów

**10.3 Screen reader support**:
- Semantyczne znaczniki HTML
- Opisy dla screen readerów

### Krok 11: Optymalizacja wydajności

**11.1 Lazy loading**:
- Upewnienie się, że komponenty są lazy loaded jeśli potrzeba
- Optymalizacja bundle size

**11.2 Change detection**:
- Użycie `OnPush` change detection strategy jeśli możliwe
- Optymalizacja subskrypcji Observable

**11.3 Caching**:
- Cache'owanie odpowiedzi API dla zapisanej gry (opcjonalne)
- Cache'owanie statusu użytkownika

### Krok 12: Code review i dokumentacja

**12.1 Code review**:
- Sprawdzenie zgodności z zasadami implementacji
- Weryfikacja zgodności z ESLint i Prettier
- Review bezpieczeństwa i wydajności

**12.2 Dokumentacja**:
- Komentarze w kodzie (tylko tam gdzie wymagane)
- Aktualizacja README z informacjami o komponencie
- Dokumentacja API endpoints używanych przez komponent

### Krok 13: Wdrożenie

**13.1 Merge do głównej gałęzi**:
- Utworzenie Pull Request
- Code review przez zespół
- Merge po akceptacji

**13.2 Weryfikacja w środowisku deweloperskim**:
- Testy manualne wszystkich scenariuszy
- Weryfikacja działania na różnych przeglądarkach
- Weryfikacja responsywności

**13.3 Wdrożenie na produkcję**:
- Wdrożenie przez CI/CD pipeline
- Monitorowanie błędów po wdrożeniu
- Zbieranie feedbacku od użytkowników

