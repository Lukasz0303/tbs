# Plan implementacji widoku GameComponent

> **Źródło**: `.ai/implementation-plans-ui/04_game-component.md`

## 1. Przegląd

GameComponent to główny widok rozgrywki w aplikacji World at War: Turn-Based Strategy, służący do wyświetlania planszy gry i umożliwienia użytkownikowi wykonywania ruchów w grze vs bot lub PvP. Komponent obsługuje wszystkie stany gry, integruje się z REST API oraz WebSocket dla gier PvP, zapewniając płynną rozgrywkę w czasie rzeczywistym.

Główne funkcjonalności:
- Wyświetlanie planszy gry (3x3, 4x4, 5x5)
- Wykonywanie ruchów przez gracza (REST dla vs_bot, WebSocket dla PvP)
- Obsługa automatycznych ruchów bota (vs_bot)
- Timer dla gier PvP (10 sekund na ruch)
- Możliwość poddania gry (PvP)
- Wyświetlanie informacji o grze i przeciwniku
- Dialog z wynikiem gry po zakończeniu
- Obsługa reconnect WebSocket (max 20 sekund)

Komponent realizuje historyjki użytkownika: US-004 (Rozgrywka z botem łatwy poziom), US-005 (Rozgrywka z botem średni poziom), US-006 (Rozgrywka z botem trudny poziom), US-007 (Dołączenie do gry PvP), US-008 (Rozgrywka PvP z funkcjonalnościami), US-013 (Obsługa rozłączeń w PvP), US-014 (Walidacja ruchów w grze).

## 2. Routing widoku

**Ścieżka routingu**: `/game/:gameId`

**Konfiguracja routingu**:
```typescript
{
  path: 'game/:gameId',
  component: GameComponent,
  canActivate: [AuthGuard]
}
```

**Lokalizacja pliku routingu**: `frontend/src/app/app.routes.ts` lub odpowiedni plik konfiguracji routingu

**Guardy**: `AuthGuard` - wymagane uwierzytelnienie (użytkownik musi być zalogowany lub mieć sesję gościa)

**Parametry routingu**:
- `gameId` (number) - ID gry z tabeli `games.id`

## 3. Struktura komponentów

```
GameComponent (główny komponent)
├── GameHeaderComponent (opcjonalny, nagłówek z tytułem i przyciskiem poddania)
│   └── ButtonModule (PrimeNG - przycisk "Poddaj się")
├── GameInfoComponent (informacje o grze)
│   └── PlayerInfo, StatusInfo, TurnInfo
├── GameTimerComponent (warunkowy, tylko dla PvP)
│   └── TimerDisplay, WarningIndicators
├── GameBotIndicatorComponent (warunkowy, tylko dla vs_bot)
│   └── LoadingSpinner, "Bot myśli..." text
├── GameBoardComponent (główna plansza gry)
│   └── BoardGrid, CellComponent, WinLineAnimation
├── GameResultDialogComponent (warunkowy, dialog z wynikiem)
│   └── DialogModule (PrimeNG), WinnerInfo, PointsInfo, ButtonModule
└── ProgressSpinnerModule (PrimeNG - wskaźnik ładowania)
```

**Hierarchia komponentów**:
- GameComponent jest komponentem standalone
- GameBoardComponent, GameInfoComponent, GameTimerComponent, GameBotIndicatorComponent, GameResultDialogComponent są komponentami współdzielonymi
- Wszystkie komponenty używają PrimeNG do elementów UI
- GameComponent zarządza stanem gry i koordynuje komunikację z API/WebSocket

## 4. Szczegóły komponentów

### GameComponent

**Opis komponentu**: Główny komponent widoku, zarządza stanem gry, komunikacją z API/WebSocket, timerem i wszystkimi interakcjami użytkownika. Obsługuje logikę biznesową związaną z wykonywaniem ruchów, obsługą bota, timerem PvP i zakończeniem gry.

**Główne elementy HTML**:
- Kontener główny (`.game-container`)
- Sekcja nagłówka (`.game-header`) z tytułem i przyciskiem poddania
- Sekcja informacji (`.game-info`) z GameInfoComponent, GameTimerComponent, GameBotIndicatorComponent
- Kontener planszy (`.game-board-container`) z GameBoardComponent
- Warunkowy GameResultDialogComponent
- Warunkowy wskaźnik ładowania (`.loading-container`)

**Obsługiwane zdarzenia**:
- `ngOnInit()` - inicjalizacja komponentu, pobranie stanu gry, nawiązanie WebSocket (PvP), uruchomienie timera (PvP)
- `ngOnDestroy()` - czyszczenie subskrypcji, rozłączenie WebSocket
- `onMove(move: { row: number, col: number })` - obsługa wykonania ruchu przez gracza
- `onSurrender()` - obsługa poddania gry (PvP)
- `onResultDialogClose()` - obsługa zamknięcia dialogu z wynikiem

**Obsługiwana walidacja**:
- Sprawdzenie czy gra istnieje (przed wyświetleniem)
- Sprawdzenie czy użytkownik jest uczestnikiem gry (walidacja po stronie API)
- Sprawdzenie czy to tura gracza (przed umożliwieniem ruchu)
- Sprawdzenie czy gra jest w statusie `in_progress` (przed umożliwieniem ruchu)
- Walidacja ruchu po stronie klienta (pole nie zajęte, współrzędne w zakresie planszy)

**Typy**:
- `Game` - interfejs reprezentujący grę
- `User` - interfejs reprezentujący użytkownika
- `MoveRequest` - interfejs dla żądania ruchu
- `MoveResponse` - interfejs dla odpowiedzi po ruchu
- `WebSocketMessage` - interfejs dla wiadomości WebSocket
- `Observable<Game | null>` - Observable ze stanem gry
- `Observable<User | null>` - Observable z aktualnym użytkownikiem
- `Observable<number>` - Observable z pozostałymi sekundami timera
- `Observable<boolean>` - Observable ze statusem myślenia bota
- `Observable<boolean>` - Observable ze statusem wyświetlania dialogu wyniku

**Propsy**: Brak (komponent główny, nie przyjmuje propsów, pobiera `gameId` z routingu)

### GameBoardComponent

**Opis komponentu**: Komponent wyświetlający planszę gry (3x3, 4x4, 5x5) z komórkami, symbolami X i O, oraz animacjami ruchów i linii wygranej. Obsługuje kliknięcia na komórki i emituje zdarzenia ruchów.

**Główne elementy HTML**:
- Kontener planszy (`.game-board`)
- Siatka komórek (`.board-grid`) z dynamicznym rozmiarem
- Komórka planszy (`.board-cell`) dla każdej pozycji
- Warunkowa linia wygranej (`.win-line`) z animacją SVG
- Warunkowe symbole X i O w komórkach z animacjami

**Obsługiwane zdarzenia**:
- `move` - EventEmitter emitujący `{ row: number, col: number }` po kliknięciu na komórkę
- `cellClick(row: number, col: number)` - obsługa kliknięcia na komórkę

**Obsługiwana walidacja**:
- Sprawdzenie czy komórka nie jest zajęta (przed emisją zdarzenia)
- Sprawdzenie czy plansza nie jest wyłączona (`disabled` prop)
- Sprawdzenie czy gra jest w statusie `in_progress` (przed umożliwieniem kliknięcia)

**Typy**:
- `BoardState` - interfejs reprezentujący stan planszy (tablica 2D stringów)
- `PlayerSymbol` - typ union `'x' | 'o' | null`
- `GameStatus` - typ union statusów gry
- `WinLine` - interfejs dla linii wygranej (start, end, type: 'horizontal' | 'vertical' | 'diagonal')

**Propsy**:
- `boardSize: 3 | 4 | 5` - rozmiar planszy (wymagane)
- `boardState: BoardState` - stan planszy (wymagane)
- `currentPlayerSymbol: 'x' | 'o' | null` - symbol aktualnego gracza (wymagane)
- `disabled: boolean` - flaga wyłączenia planszy (opcjonalne, domyślnie false)
- `gameStatus: GameStatus` - status gry (wymagane)
- `winLine: WinLine | null` - informacje o linii wygranej (opcjonalne)

### GameInfoComponent

**Opis komponentu**: Komponent wyświetlający informacje o grze: typ gry, przeciwnik, status, aktualny gracz, liczba tur i aktualna tura.

**Główne elementy HTML**:
- Kontener informacji (`.game-info`)
- Sekcja typu gry (`.game-type`)
- Sekcja przeciwnika (`.opponent-info`)
- Sekcja statusu (`.game-status`)
- Sekcja aktualnego gracza (`.current-player`)
- Sekcja tur (`.turns-info`)

**Obsługiwane zdarzenia**: Brak (komponent prezentacyjny)

**Obsługiwana walidacja**: Brak (komponent prezentacyjny)

**Typy**:
- `Game` - interfejs reprezentujący grę (wymagane)
- `User` - interfejs reprezentujący użytkownika (opcjonalne)

**Propsy**:
- `game: Game` - obiekt gry (wymagane)
- `currentUser: User | null` - aktualny użytkownik (opcjonalne)

### GameTimerComponent

**Opis komponentu**: Komponent wyświetlający pozostały czas na ruch w grach PvP (10 sekund) z wizualnymi ostrzeżeniami (warning, danger) przy niskim czasie.

**Główne elementy HTML**:
- Kontener timera (`.game-timer`)
- Wyświetlacz czasu (`.timer-display`)
- Wskaźniki wizualne (`.timer-warning`, `.timer-danger`)
- Pasek postępu (`.timer-progress-bar`)

**Obsługiwane zdarzenia**: Brak (komponent prezentacyjny)

**Obsługiwana walidacja**: Brak (komponent prezentacyjny)

**Typy**:
- `number` - pozostałe sekundy (0-10)

**Propsy**:
- `remainingSeconds: number` - pozostałe sekundy na ruch (wymagane, zakres 0-10)
- `currentPlayerSymbol: 'x' | 'o'` - symbol aktualnego gracza (wymagane)

### GameBotIndicatorComponent

**Opis komponentu**: Komponent wyświetlający wskaźnik "Bot myśli..." podczas generowania ruchu bota w grach vs_bot.

**Główne elementy HTML**:
- Kontener wskaźnika (`.bot-indicator`)
- Animowany spinner (`.bot-spinner`)
- Tekst "Bot myśli..." (`.bot-text`)

**Obsługiwane zdarzenia**: Brak (komponent prezentacyjny)

**Obsługiwana walidacja**: Brak (komponent prezentacyjny)

**Typy**: Brak (komponent bez propsów)

**Propsy**: Brak (komponent bez propsów)

### GameResultDialogComponent

**Opis komponentu**: Komponent wyświetlający dialog z wynikiem gry po zakończeniu (wygrana, przegrana, remis), informacje o zdobytych punktach i przycisk do powrotu do strony głównej.

**Główne elementy HTML**:
- Dialog PrimeNG (`.result-dialog`)
- Sekcja wyniku (`.result-section`)
- Sekcja punktów (`.points-section`)
- Przycisk zamknięcia (`.close-button`)

**Obsługiwane zdarzenia**:
- `close` - EventEmitter emitujący po kliknięciu przycisku zamknięcia

**Obsługiwana walidacja**: Brak (komponent prezentacyjny)

**Typy**:
- `Game` - interfejs reprezentujący grę (wymagane)
- `User` - interfejs reprezentujący użytkownika (opcjonalne)

**Propsy**:
- `game: Game` - obiekt gry (wymagane)
- `currentUser: User | null` - aktualny użytkownik (opcjonalne)

## 5. Typy

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
  boardState: BoardState;
  player1?: PlayerInfo;
  player2?: PlayerInfo | null;
  winner?: WinnerInfo | null;
  moves?: MoveListItem[];
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
- `boardState: BoardState` - stan planszy
- `player1?: PlayerInfo` - informacje o pierwszym graczu (opcjonalne)
- `player2?: PlayerInfo | null` - informacje o drugim graczu (opcjonalne)
- `winner?: WinnerInfo | null` - informacje o zwycięzcy (opcjonalne)
- `moves?: MoveListItem[]` - historia ruchów (opcjonalne)

### BoardState

```typescript
interface BoardState {
  cells: (string | null)[][];
}
```

**Pola**:
- `cells: (string | null)[][]` - tablica 2D reprezentująca planszę, gdzie każda komórka może zawierać `'x'`, `'o'` lub `null`

### MoveRequest

```typescript
interface MoveRequest {
  row: number;
  col: number;
  playerSymbol: 'x' | 'o';
}
```

**Pola**:
- `row: number` - indeks wiersza (0-based)
- `col: number` - indeks kolumny (0-based)
- `playerSymbol: 'x' | 'o'` - symbol gracza wykonującego ruch

### MoveResponse

```typescript
interface MoveResponse {
  moveId: number;
  gameId: number;
  row: number;
  col: number;
  playerSymbol: 'x' | 'o';
  moveOrder: number;
  createdAt: string;
  boardState: BoardState;
  gameStatus: 'in_progress' | 'finished' | 'draw';
  winner: WinnerInfo | null;
}
```

**Pola**:
- `moveId: number` - unikalny identyfikator ruchu
- `gameId: number` - ID gry
- `row: number` - indeks wiersza
- `col: number` - indeks kolumny
- `playerSymbol: 'x' | 'o'` - symbol gracza
- `moveOrder: number` - kolejność ruchu
- `createdAt: string` - data utworzenia ruchu (ISO 8601)
- `boardState: BoardState` - zaktualizowany stan planszy
- `gameStatus: 'in_progress' | 'finished' | 'draw'` - status gry po ruchu
- `winner: WinnerInfo | null` - informacje o zwycięzcy (jeśli gra zakończona)

### WebSocketMessage

```typescript
interface WebSocketMessage {
  type: 'MOVE_ACCEPTED' | 'MOVE_REJECTED' | 'OPPONENT_MOVE' | 'GAME_UPDATE' | 'TIMER_UPDATE' | 'GAME_ENDED' | 'PONG';
  payload: any;
}
```

**Pola**:
- `type: string` - typ wiadomości WebSocket
- `payload: any` - payload wiadomości (struktura zależy od typu)

### MoveAcceptedPayload

```typescript
interface MoveAcceptedPayload {
  moveId: number;
  row: number;
  col: number;
  playerSymbol: 'x' | 'o';
  boardState: BoardState;
  currentPlayerSymbol: 'x' | 'o';
  nextMoveAt: string;
}
```

### MoveRejectedPayload

```typescript
interface MoveRejectedPayload {
  reason: string;
  code: 'MOVE_INVALID_OCCUPIED' | 'MOVE_INVALID_OUT_OF_BOUNDS' | 'MOVE_INVALID_NOT_YOUR_TURN' | 'MOVE_INVALID_GAME_NOT_ACTIVE' | 'MOVE_INVALID_TIMEOUT';
}
```

### OpponentMovePayload

```typescript
interface OpponentMovePayload {
  row: number;
  col: number;
  playerSymbol: 'x' | 'o';
  boardState: BoardState;
  currentPlayerSymbol: 'x' | 'o';
  nextMoveAt: string;
}
```

### GameUpdatePayload

```typescript
interface GameUpdatePayload {
  gameId: number;
  status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw';
  winner: WinnerInfo | null;
  boardState: BoardState;
}
```

### TimerUpdatePayload

```typescript
interface TimerUpdatePayload {
  remainingSeconds: number;
  currentPlayerSymbol: 'x' | 'o';
}
```

### GameEndedPayload

```typescript
interface GameEndedPayload {
  gameId: number;
  status: 'finished' | 'draw' | 'abandoned';
  winner: WinnerInfo | null;
  finalBoardState: BoardState;
  totalMoves: number;
}
```

### PlayerInfo

```typescript
interface PlayerInfo {
  userId: number;
  username: string;
  isGuest: boolean;
}
```

**Pola**:
- `userId: number` - ID użytkownika
- `username: string` - nazwa użytkownika
- `isGuest: boolean` - flaga wskazująca czy użytkownik jest gościem

### WinnerInfo

```typescript
interface WinnerInfo {
  userId: number;
  username: string;
}
```

**Pola**:
- `userId: number` - ID zwycięzcy
- `username: string` - nazwa zwycięzcy

### MoveListItem

```typescript
interface MoveListItem {
  moveId: number;
  row: number;
  col: number;
  playerSymbol: 'x' | 'o';
  moveOrder: number;
  playerId: number | null;
  createdAt: string;
}
```

**Pola**:
- `moveId: number` - unikalny identyfikator ruchu
- `row: number` - indeks wiersza
- `col: number` - indeks kolumny
- `playerSymbol: 'x' | 'o'` - symbol gracza
- `moveOrder: number` - kolejność ruchu
- `playerId: number | null` - ID gracza (null dla ruchów bota)
- `createdAt: string` - data utworzenia ruchu (ISO 8601)

### WinLine

```typescript
interface WinLine {
  start: { row: number; col: number };
  end: { row: number; col: number };
  type: 'horizontal' | 'vertical' | 'diagonal';
}
```

**Pola**:
- `start: { row: number; col: number }` - punkt startowy linii
- `end: { row: number; col: number }` - punkt końcowy linii
- `type: 'horizontal' | 'vertical' | 'diagonal'` - typ linii wygranej

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

## 6. Zarządzanie stanem

**Strategia zarządzania stanem**: RxJS Observables + BehaviorSubject + Reactive Forms

**Stan komponentu**:
- `gameId: number | null` - ID gry z routingu
- `game$: BehaviorSubject<Game | null>` - stan gry (null podczas ładowania)
- `currentUser$: Observable<User | null>` - Observable z aktualnym użytkownikiem (z AuthService)
- `isLoading$: BehaviorSubject<boolean>` - flaga ładowania (true podczas pobierania gry)
- `timerSeconds$: BehaviorSubject<number>` - pozostałe sekundy na ruch (dla PvP)
- `isBotThinking$: BehaviorSubject<boolean>` - flaga myślenia bota (dla vs_bot)
- `showResultDialog$: BehaviorSubject<boolean>` - flaga wyświetlania dialogu z wynikiem

**Subskrypcje**:
- Subskrypcja do `route.params` w `ngOnInit` dla pobrania `gameId`
- Subskrypcja do `gameService.getGame(gameId)` dla pobrania stanu gry
- Subskrypcja do `interval(2000)` dla polling stanu gry (fallback jeśli brak WebSocket)
- Subskrypcja do `interval(1000)` dla aktualizacji timera (PvP)
- Subskrypcja do `websocketService.getMessages()` dla odbierania wiadomości WebSocket (PvP)
- Subskrypcja do `game$` w template przez `async` pipe
- Subskrypcja do `currentUser$` w template przez `async` pipe
- Subskrypcja do `isLoading$` w template przez `async` pipe
- Subskrypcja do `timerSeconds$` w template przez `async` pipe
- Subskrypcja do `isBotThinking$` w template przez `async` pipe
- Subskrypcja do `showResultDialog$` w template przez `async` pipe

**Lifecycle hooks**:
- `ngOnInit()` - inicjalizacja: pobranie `gameId` z routingu, załadowanie stanu gry, nawiązanie WebSocket (PvP), uruchomienie timera (PvP), uruchomienie polling (fallback)
- `ngOnDestroy()` - czyszczenie subskrypcji, rozłączenie WebSocket, zatrzymanie timera, zatrzymanie polling

**Wzorce RxJS**:
- `BehaviorSubject` - dla stanu gry, flag ładowania, timera, myślenia bota, dialogu wyniku
- `interval()` - dla polling stanu gry (co 2 sekundy) i aktualizacji timera (co 1 sekundę)
- `take(1)` - dla jednorazowych operacji (pobranie użytkownika)
- `switchMap()` - dla przełączania między Observable (route params → load game)
- `catchError()` - dla obsługi błędów w Observable
- `filter()` - dla filtrowania wartości (tylko zmiany w grze)
- `map()` - dla transformacji danych

**Custom hooki**: Brak (komponent używa standardowych Observable i BehaviorSubject)

## 7. Integracja API

### 7.1 Endpoint: GET /api/games/{gameId}

**Cel**: Pobranie szczegółowych informacji o grze, w tym stanu planszy i historii ruchów

**Metoda HTTP**: GET

**Parametry URL**:
- `gameId` (number) - ID gry

**Nagłówki**:
- `Authorization: Bearer <JWT_TOKEN>` - token JWT (wymagane)
- `Accept: application/json`

**Odpowiedź sukcesu (200 OK)**:
```json
{
  "gameId": 42,
  "gameType": "vs_bot",
  "boardSize": 3,
  "player1": {
    "userId": 123,
    "username": "player1",
    "isGuest": false
  },
  "player2": null,
  "winner": null,
  "botDifficulty": "easy",
  "status": "in_progress",
  "currentPlayerSymbol": "x",
  "lastMoveAt": "2024-01-20T15:32:00Z",
  "createdAt": "2024-01-20T15:30:00Z",
  "updatedAt": "2024-01-20T15:32:00Z",
  "finishedAt": null,
  "boardState": {
    "cells": [
      ["x", null, null],
      [null, "o", null],
      [null, null, "x"]
    ]
  },
  "totalMoves": 3,
  "moves": [...]
}
```

**Obsługa w komponencie**:
```typescript
private loadGame(): void {
  if (!this.gameId) return;

  this.isLoading$.next(true);
  this.gameService.getGame(this.gameId).subscribe({
    next: (game) => {
      this.game$.next(game);
      this.isLoading$.next(false);
      
      if (game.gameType === 'pvp' && game.status === 'in_progress') {
        this.connectWebSocket();
        this.startTimer();
      }
      
      if (game.status === 'finished' || game.status === 'draw' || game.status === 'abandoned') {
        this.showResultDialog$.next(true);
      }
    },
    error: (error) => {
      this.isLoading$.next(false);
      this.handleError(error);
    }
  });
}
```

### 7.2 Endpoint: POST /api/games/{gameId}/moves

**Cel**: Wykonanie ruchu przez gracza w grze vs_bot

**Metoda HTTP**: POST

**Parametry URL**:
- `gameId` (number) - ID gry

**Nagłówki**:
- `Authorization: Bearer <JWT_TOKEN>` - token JWT (wymagane)
- `Content-Type: application/json`
- `Accept: application/json`

**Ciało żądania**:
```json
{
  "row": 0,
  "col": 1,
  "playerSymbol": "x"
}
```

**Odpowiedź sukcesu (201 Created)**:
```json
{
  "moveId": 123,
  "gameId": 42,
  "row": 0,
  "col": 1,
  "playerSymbol": "x",
  "moveOrder": 5,
  "createdAt": "2024-01-20T15:30:00Z",
  "boardState": {
    "cells": [
      ["x", "x", null],
      [null, "o", null],
      [null, null, null]
    ]
  },
  "gameStatus": "in_progress",
  "winner": null
}
```

**Obsługa w komponencie**:
```typescript
private sendMoveViaREST(move: { row: number, col: number }): void {
  const game = this.game$.value;
  if (!game || !this.gameId) return;

  this.currentUser$.pipe(take(1)).subscribe(user => {
    if (!user) return;

    const playerSymbol = game.player1Id === user.userId ? 'x' : 'o';
    
    this.gameService.makeMove(this.gameId!, move.row, move.col, playerSymbol).subscribe({
      next: (response) => {
        this.game$.next({ ...game, ...response });
        this.handleMoveResponse(response);
        
        if (game.gameType === 'vs_bot' && response.gameStatus === 'in_progress') {
          this.makeBotMove();
        }
      },
      error: (error) => this.handleMoveError(error)
    });
  });
}
```

### 7.3 Endpoint: POST /api/games/{gameId}/bot-move

**Cel**: Automatyczne wykonanie ruchu bota w grze vs_bot (wywoływane wewnętrznie)

**Metoda HTTP**: POST

**Parametry URL**:
- `gameId` (number) - ID gry

**Nagłówki**:
- `Authorization: Bearer <JWT_TOKEN>` - token JWT (wymagane)
- `Accept: application/json`

**Odpowiedź sukcesu (200 OK)**:
```json
{
  "moveId": 124,
  "gameId": 42,
  "row": 1,
  "col": 2,
  "playerSymbol": "o",
  "moveOrder": 6,
  "createdAt": "2024-01-20T15:31:00Z",
  "boardState": {
    "cells": [
      ["x", "x", null],
      [null, "o", "o"],
      [null, null, null]
    ]
  },
  "gameStatus": "in_progress",
  "winner": null
}
```

**Obsługa w komponencie**:
```typescript
private makeBotMove(): void {
  this.isBotThinking$.next(true);
  
  setTimeout(() => {
    if (!this.gameId) return;

    this.gameService.makeBotMove(this.gameId).subscribe({
      next: (response) => {
        const game = this.game$.value;
        if (game) {
          this.game$.next({ ...game, ...response });
          this.handleMoveResponse(response);
        }
        this.isBotThinking$.next(false);
      },
      error: (error) => {
        this.isBotThinking$.next(false);
        this.handleError(error);
      }
    });
  }, 200);
}
```

### 7.4 Endpoint: PUT /api/games/{gameId}/status

**Cel**: Poddanie gry przez gracza (PvP)

**Metoda HTTP**: PUT

**Parametry URL**:
- `gameId` (number) - ID gry

**Nagłówki**:
- `Authorization: Bearer <JWT_TOKEN>` - token JWT (wymagane)
- `Content-Type: application/json`
- `Accept: application/json`

**Ciało żądania**:
```json
{
  "status": "finished"
}
```

**Odpowiedź sukcesu (200 OK)**:
```json
{
  "gameId": 42,
  "status": "finished",
  "updatedAt": "2024-01-20T15:35:00Z"
}
```

**Obsługa w komponencie**:
```typescript
onSurrender(): void {
  if (!this.gameId) return;

  this.gameService.surrenderGame(this.gameId).subscribe({
    next: () => {
      this.messageService.add({
        severity: 'info',
        summary: 'Gra zakończona',
        detail: 'Poddano grę'
      });
      this.loadGame();
    },
    error: (error) => this.handleError(error)
  });
}
```

### 7.5 WebSocket: WS /ws/game/{gameId}

**Cel**: Komunikacja w czasie rzeczywistym dla gier PvP

**Protokół**: WebSocket (ws:// lub wss://)

**Subprotokół**: `game-protocol`

**Parametry URL**:
- `gameId` (number) - ID gry

**Nagłówki handshake**:
- `Authorization: Bearer <JWT_TOKEN>` - token JWT (wymagane)
- `Sec-WebSocket-Protocol: game-protocol`

**Typy wiadomości (Klient → Serwer)**:
- `MOVE` - wysłanie ruchu gracza
- `SURRENDER` - poddanie gry
- `PING` - keep-alive

**Typy wiadomości (Serwer → Klient)**:
- `MOVE_ACCEPTED` - ruch zaakceptowany
- `MOVE_REJECTED` - ruch odrzucony
- `OPPONENT_MOVE` - ruch przeciwnika
- `GAME_UPDATE` - aktualizacja stanu gry
- `TIMER_UPDATE` - aktualizacja timera
- `GAME_ENDED` - gra zakończona
- `PONG` - odpowiedź keep-alive

**Obsługa w komponencie**:
```typescript
private connectWebSocket(): void {
  if (!this.gameId) return;

  const token = this.authService.getToken();
  if (!token) {
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd',
      detail: 'Brak tokenu uwierzytelniającego'
    });
    return;
  }

  this.websocketService.connect(this.gameId, token).subscribe({
    next: () => {
      this.websocketSubscription = this.websocketService.getMessages().subscribe({
        next: (message) => this.handleWebSocketMessage(message),
        error: (error) => this.handleWebSocketError(error)
      });
    },
    error: (error) => {
      this.messageService.add({
        severity: 'warn',
        summary: 'Ostrzeżenie',
        detail: 'Nie udało się nawiązać połączenia WebSocket. Używam polling.'
      });
    }
  });
}
```

### 7.6 Serwisy Angular

**GameService**:
- `getGame(gameId: number): Observable<Game>` - pobranie stanu gry
- `makeMove(gameId: number, row: number, col: number, playerSymbol: 'x' | 'o'): Observable<MoveResponse>` - wykonanie ruchu
- `makeBotMove(gameId: number): Observable<MoveResponse>` - automatyczny ruch bota
- `surrenderGame(gameId: number): Observable<UpdateGameStatusResponse>` - poddanie gry

**WebSocketService**:
- `connect(gameId: number, token: string): Observable<void>` - nawiązanie połączenia WebSocket
- `disconnect(): void` - rozłączenie WebSocket
- `sendMove(row: number, col: number, playerSymbol: 'x' | 'o'): void` - wysłanie ruchu przez WebSocket
- `sendSurrender(): void` - wysłanie poddania przez WebSocket
- `getMessages(): Observable<WebSocketMessage>` - odbieranie wiadomości WebSocket

**AuthService**:
- `getCurrentUser(): Observable<User | null>` - pobranie aktualnego użytkownika
- `getToken(): string | null` - pobranie tokenu JWT

**Router**:
- `navigate(commands: any[]): Promise<boolean>` - nawigacja do innych widoków

**MessageService** (PrimeNG):
- `add(message: Message): void` - wyświetlenie toast notification

## 8. Interakcje użytkownika

### 8.1 Wykonanie ruchu (vs_bot)

**Scenariusz**: Użytkownik klika na komórkę planszy w grze vs_bot

**Kroki**:
1. Użytkownik klika na komórkę w `GameBoardComponent`
2. `GameBoardComponent` emituje zdarzenie `move` z `{ row, col }`
3. `GameComponent` odbiera zdarzenie w metodzie `onMove(move)`
4. Sprawdzenie czy gra jest typu `vs_bot`
5. Wywołanie `sendMoveViaREST(move)`
6. Pobranie symbolu gracza z `currentUser$`
7. Wywołanie `gameService.makeMove()` z parametrami ruchu
8. Po sukcesie: aktualizacja `game$` z odpowiedzią
9. Jeśli gra nadal trwa: wywołanie `makeBotMove()` po 200ms
10. Obsługa odpowiedzi bota: aktualizacja `game$`, wyłączenie `isBotThinking$`
11. Jeśli gra zakończona: wyświetlenie dialogu z wynikiem

**Obsługa błędów**:
- Błąd ruchu: toast notification z komunikatem błędu
- Błąd ruchu bota: toast notification, wyłączenie `isBotThinking$`

### 8.2 Wykonanie ruchu (PvP)

**Scenariusz**: Użytkownik klika na komórkę planszy w grze PvP

**Kroki**:
1. Użytkownik klika na komórkę w `GameBoardComponent`
2. `GameBoardComponent` emituje zdarzenie `move` z `{ row, col }`
3. `GameComponent` odbiera zdarzenie w metodzie `onMove(move)`
4. Sprawdzenie czy gra jest typu `pvp`
5. Wywołanie `sendMoveViaWebSocket(move)`
6. Pobranie symbolu gracza z `currentUser$`
7. Wywołanie `websocketService.sendMove()` z parametrami ruchu
8. Oczekiwanie na odpowiedź WebSocket (`MOVE_ACCEPTED` lub `MOVE_REJECTED`)
9. Jeśli `MOVE_ACCEPTED`: aktualizacja `game$` z payload
10. Jeśli `MOVE_REJECTED`: toast notification z komunikatem błędu
11. Odbieranie `OPPONENT_MOVE`: aktualizacja `game$` z ruchem przeciwnika
12. Odbieranie `GAME_ENDED`: wyświetlenie dialogu z wynikiem

**Obsługa błędów**:
- Błąd WebSocket: toast notification, fallback do polling
- Ruch odrzucony: toast notification z powodem odrzucenia

### 8.3 Poddanie gry (PvP)

**Scenariusz**: Użytkownik klika przycisk "Poddaj się" w grze PvP

**Kroki**:
1. Użytkownik klika przycisk "Poddaj się" w nagłówku
2. `GameComponent` wywołuje metodę `onSurrender()`
3. Wywołanie `gameService.surrenderGame(gameId)`
4. Po sukcesie: toast notification, odświeżenie stanu gry przez `loadGame()`
5. Aktualizacja `game$` z nowym statusem
6. Wyświetlenie dialogu z wynikiem (przeciwnik wygrywa)

**Obsługa błędów**:
- Błąd poddania: toast notification z komunikatem błędu

### 8.4 Automatyczny ruch bota (vs_bot)

**Scenariusz**: Bot wykonuje ruch automatycznie po ruchu gracza

**Kroki**:
1. Po wykonaniu ruchu gracza w grze vs_bot, wywołanie `makeBotMove()`
2. Ustawienie `isBotThinking$.next(true)`
3. Opóźnienie 200ms (`setTimeout`)
4. Wywołanie `gameService.makeBotMove(gameId)`
5. Po sukcesie: aktualizacja `game$` z odpowiedzią
6. Wyłączenie `isBotThinking$.next(false)`
7. Jeśli gra zakończona: wyświetlenie dialogu z wynikiem

**Obsługa błędów**:
- Błąd ruchu bota: toast notification, wyłączenie `isBotThinking$`

### 8.5 Aktualizacja timera (PvP)

**Scenariusz**: Timer odlicza czas pozostały na ruch przeciwnika

**Kroki**:
1. Po załadowaniu gry PvP, wywołanie `startTimer()`
2. Subskrypcja do `interval(1000)` dla aktualizacji co sekundę
3. Obliczenie pozostałego czasu: `10 - elapsed` (gdzie `elapsed` to czas od `lastMoveAt`)
4. Aktualizacja `timerSeconds$.next(remaining)`
5. Jeśli `remaining <= 0`: wywołanie `checkTimeout()`
6. Odświeżenie stanu gry przez `loadGame()`
7. Odbieranie `TIMER_UPDATE` z WebSocket: aktualizacja `timerSeconds$`

**Obsługa błędów**:
- Timeout: automatyczne zakończenie gry, przeciwnik wygrywa

### 8.6 Zamknięcie dialogu z wynikiem

**Scenariusz**: Użytkownik zamyka dialog z wynikiem gry

**Kroki**:
1. Użytkownik klika przycisk zamknięcia w `GameResultDialogComponent`
2. `GameResultDialogComponent` emituje zdarzenie `close`
3. `GameComponent` odbiera zdarzenie w metodzie `onResultDialogClose()`
4. Wyłączenie `showResultDialog$.next(false)`
5. Nawigacja do `/` (strona główna)

**Obsługa błędów**: Brak (tylko nawigacja)

### 8.7 Reconnect WebSocket (PvP)

**Scenariusz**: Połączenie WebSocket zostaje utracone i użytkownik próbuje ponownie połączyć się

**Kroki**:
1. Wykrycie utraty połączenia WebSocket
2. Automatyczna próba reconnect (max 20 sekund)
3. Jeśli reconnect w ciągu 20s: kontynuacja gry, reset timera
4. Jeśli brak reconnect: zakończenie gry, przeciwnik wygrywa

**Obsługa błędów**:
- Błąd reconnect: fallback do polling, toast notification

## 9. Warunki i walidacja

### 9.1 Warunki wyświetlania komponentów

**GameBoardComponent**:
- Warunek: `game$ | async as game` - wyświetlany tylko gdy `game !== null`
- Warunek: `game.status === 'in_progress'` - plansza aktywna tylko dla gier w toku
- Warunek: `isMoveDisabled(game) === false` - komórki aktywne tylko dla aktualnego gracza

**GameTimerComponent**:
- Warunek: `game.gameType === 'pvp'` - wyświetlany tylko dla gier PvP
- Warunek: `game.status === 'in_progress'` - wyświetlany tylko dla gier w toku

**GameBotIndicatorComponent**:
- Warunek: `game.gameType === 'vs_bot'` - wyświetlany tylko dla gier vs_bot
- Warunek: `isBotThinking$ | async` - wyświetlany tylko gdy bot myśli

**GameResultDialogComponent**:
- Warunek: `showResultDialog$ | async` - wyświetlany tylko gdy `true`
- Warunek: `game.status === 'finished' || game.status === 'draw' || game.status === 'abandoned'` - wyświetlany tylko dla zakończonych gier

**Przycisk "Poddaj się"**:
- Warunek: `game.gameType === 'pvp'` - wyświetlany tylko dla gier PvP
- Warunek: `game.status === 'in_progress'` - wyświetlany tylko dla gier w toku

### 9.2 Walidacja ruchów po stronie klienta

**Sprawdzenie przed wykonaniem ruchu**:
- Pole nie jest zajęte: `boardState.cells[row][col] === null`
- Współrzędne w zakresie planszy: `row >= 0 && row < boardSize && col >= 0 && col < boardSize`
- Gra jest w statusie `in_progress`: `game.status === 'in_progress'`
- To tura gracza: `isMoveDisabled(game) === false`
- Użytkownik jest uczestnikiem gry: walidacja po stronie serwera (403 Forbidden jeśli nie)

**Walidacja po stronie serwera (API)**:
- Pole nie jest zajęte (422 Unprocessable Entity)
- Współrzędne w zakresie planszy (422 Unprocessable Entity)
- Gra jest w statusie `in_progress` (400 Bad Request)
- To tura gracza (403 Forbidden)
- Użytkownik jest uczestnikiem gry (403 Forbidden)
- Nie przekroczono limitu czasu (422 Unprocessable Entity dla timeout)

### 9.3 Walidacja stanu gry

**Sprawdzenie przed wyświetleniem**:
- Gra istnieje: `game !== null` (404 Not Found jeśli nie)
- Użytkownik jest uczestnikiem: `game.player1Id === userId || game.player2Id === userId` (403 Forbidden jeśli nie)

**Sprawdzenie przed wykonaniem ruchu**:
- Gra jest w statusie `in_progress`: `game.status === 'in_progress'` (400 Bad Request jeśli nie)
- To tura gracza: `game.currentPlayerSymbol === playerSymbol` (403 Forbidden jeśli nie)

### 9.4 Walidacja WebSocket

**Sprawdzenie przed nawiązaniem połączenia**:
- Token JWT istnieje: `token !== null` (401 Unauthorized jeśli nie)
- Gra jest typu PvP: `game.gameType === 'pvp'` (400 Bad Request jeśli nie)
- Gra jest w statusie `in_progress`: `game.status === 'in_progress'` (400 Bad Request jeśli nie)
- Użytkownik jest uczestnikiem gry: walidacja po stronie serwera (403 Forbidden jeśli nie)

### 9.5 Wpływ warunków na stan interfejsu

**Gra nie istnieje (404)**:
- Wyświetlenie komunikatu błędu
- Przekierowanie do strony głównej

**Użytkownik nie jest uczestnikiem (403)**:
- Wyświetlenie komunikatu błędu
- Przekierowanie do strony głównej

**Gra zakończona**:
- Plansza wyłączona (`disabled: true`)
- Wyświetlenie dialogu z wynikiem
- Ukrycie timera i przycisku poddania

**Nie tura gracza**:
- Plansza wyłączona (`disabled: true`)
- Wyświetlenie komunikatu "Oczekiwanie na przeciwnika" (PvP)
- Wyświetlenie wskaźnika "Bot myśli..." (vs_bot)

**Timeout w grze PvP**:
- Automatyczne zakończenie gry
- Przeciwnik wygrywa
- Wyświetlenie dialogu z wynikiem

## 10. Obsługa błędów

### 10.1 Błędy API

**Błąd pobierania gry (404 Not Found)**:
- **Obsługa**: Przekierowanie do strony głównej z komunikatem błędu
- **Efekt dla użytkownika**: Wyświetlenie toast notification, nawigacja do `/`
- **Komunikat**: "Gra nie została znaleziona"

**Błąd pobierania gry (403 Forbidden)**:
- **Obsługa**: Przekierowanie do strony głównej z komunikatem błędu
- **Efekt dla użytkownika**: Wyświetlenie toast notification, nawigacja do `/`
- **Komunikat**: "Nie masz uprawnień do tej gry"

**Błąd pobierania gry (401 Unauthorized)**:
- **Obsługa**: Przekierowanie do strony logowania
- **Efekt dla użytkownika**: Wyświetlenie toast notification, nawigacja do `/auth/login`
- **Komunikat**: "Sesja wygasła. Zaloguj się ponownie."

**Błąd wykonania ruchu (422 Unprocessable Entity)**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik może spróbować ponownie
- **Komunikat**: Komunikat z API (`error.error.message`)

**Błąd wykonania ruchu (403 Forbidden)**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik jest informowany, że nie może wykonać ruchu
- **Komunikat**: "Nie możesz wykonać ruchu w tej turze"

**Błąd wykonania ruchu (400 Bad Request)**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik jest informowany, że gra nie jest aktywna
- **Komunikat**: "Gra nie jest aktywna"

**Błąd ruchu bota (500 Internal Server Error)**:
- **Obsługa**: Toast notification z komunikatem błędu, wyłączenie `isBotThinking$`
- **Efekt dla użytkownika**: Użytkownik może kontynuować grę, ale bot nie wykona ruchu
- **Komunikat**: "Błąd wykonania ruchu bota. Spróbuj odświeżyć stronę."

**Błąd poddania gry (500 Internal Server Error)**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik może spróbować ponownie
- **Komunikat**: "Nie udało się poddać gry. Spróbuj ponownie."

### 10.2 Błędy WebSocket

**Błąd nawiązania połączenia WebSocket**:
- **Obsługa**: Toast notification z ostrzeżeniem, fallback do polling
- **Efekt dla użytkownika**: Gra działa w trybie polling (co 2 sekundy), użytkownik może kontynuować grę
- **Komunikat**: "Nie udało się nawiązać połączenia WebSocket. Używam polling."

**Błąd odbierania wiadomości WebSocket**:
- **Obsługa**: Toast notification z ostrzeżeniem, próba reconnect
- **Efekt dla użytkownika**: Automatyczna próba reconnect, fallback do polling
- **Komunikat**: "Problem z połączeniem WebSocket. Próbuję ponownie..."

**Ruch odrzucony (MOVE_REJECTED)**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik jest informowany o przyczynie odrzucenia ruchu
- **Komunikat**: Komunikat z payload WebSocket (`payload.reason`)

**Timeout połączenia WebSocket (60s bez PONG)**:
- **Obsługa**: Zamknięcie połączenia, mechanizm 20s reconnect window
- **Efekt dla użytkownika**: Jeśli brak reconnect: przeciwnik wygrywa
- **Komunikat**: "Połączenie utracone. Próbuję ponownie..."

### 10.3 Błędy walidacji

**Ruch na zajęte pole**:
- **Obsługa**: Toast notification z komunikatem błędu (po stronie klienta lub serwera)
- **Efekt dla użytkownika**: Użytkownik nie może wykonać ruchu na zajęte pole
- **Komunikat**: "To pole jest już zajęte"

**Ruch poza planszę**:
- **Obsługa**: Toast notification z komunikatem błędu (po stronie klienta lub serwera)
- **Efekt dla użytkownika**: Użytkownik nie może wykonać ruchu poza planszę
- **Komunikat**: "Ruch poza planszę"

**Ruch poza turę**:
- **Obsługa**: Toast notification z komunikatem błędu (po stronie serwera)
- **Efekt dla użytkownika**: Użytkownik nie może wykonać ruchu poza swoją turę
- **Komunikat**: "Nie twoja tura"

**Timeout ruchu (10s)**:
- **Obsługa**: Automatyczne zakończenie gry, przeciwnik wygrywa
- **Efekt dla użytkownika**: Gra kończy się, wyświetlenie dialogu z wynikiem
- **Komunikat**: "Przekroczono limit czasu. Przeciwnik wygrywa."

### 10.4 Błędy sieci

**Timeout żądania**:
- **Obsługa**: Toast notification z komunikatem błędu, możliwość ponowienia
- **Efekt dla użytkownika**: Użytkownik może spróbować ponownie
- **Komunikat**: "Przekroczono czas oczekiwania. Sprawdź połączenie internetowe."

**Brak połączenia z internetem**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik jest informowany o braku połączenia
- **Komunikat**: "Brak połączenia z internetem. Sprawdź swoje połączenie."

### 10.5 Globalna obsługa błędów

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
- Weryfikacja czy `GameService` istnieje i ma wymagane metody (`getGame`, `makeMove`, `makeBotMove`, `surrenderGame`)
- Weryfikacja czy `WebSocketService` istnieje i ma wymagane metody (`connect`, `disconnect`, `sendMove`, `sendSurrender`, `getMessages`)
- Weryfikacja czy `AuthService` istnieje i ma metody (`getCurrentUser`, `getToken`)
- Sprawdzenie czy komponenty współdzielone (`GameBoardComponent`, `GameInfoComponent`, `GameTimerComponent`, `GameBotIndicatorComponent`, `GameResultDialogComponent`) istnieją

**1.2 Utworzenie brakujących komponentów współdzielonych**:
- `GameBoardComponent` w `frontend/src/app/components/game/game-board.component.ts`
- `GameInfoComponent` w `frontend/src/app/components/game/game-info.component.ts`
- `GameTimerComponent` w `frontend/src/app/components/game/game-timer.component.ts`
- `GameBotIndicatorComponent` w `frontend/src/app/components/game/game-bot-indicator.component.ts`
- `GameResultDialogComponent` w `frontend/src/app/components/game/game-result-dialog.component.ts`

**1.3 Instalacja zależności PrimeNG**:
- Sprawdzenie czy `ButtonModule`, `DialogModule`, `ProgressSpinnerModule` są zainstalowane
- Instalacja brakujących modułów PrimeNG jeśli potrzeba

**1.4 Utworzenie typów TypeScript**:
- Utworzenie pliku `frontend/src/app/models/game.model.ts` z interfejsami `Game`, `BoardState`, `MoveRequest`, `MoveResponse`, `WinLine`
- Utworzenie pliku `frontend/src/app/models/websocket.model.ts` z interfejsami `WebSocketMessage`, `MoveAcceptedPayload`, `MoveRejectedPayload`, `OpponentMovePayload`, `GameUpdatePayload`, `TimerUpdatePayload`, `GameEndedPayload`
- Utworzenie pliku `frontend/src/app/models/user.model.ts` z interfejsami `User`, `PlayerInfo`, `WinnerInfo`
- Utworzenie pliku `frontend/src/app/models/api.model.ts` z DTO z API

### Krok 2: Implementacja serwisów (jeśli brakuje)

**2.1 Rozszerzenie GameService**:
- Dodanie metody `getGame(gameId: number): Observable<Game>`
- Dodanie metody `makeMove(gameId: number, row: number, col: number, playerSymbol: 'x' | 'o'): Observable<MoveResponse>`
- Dodanie metody `makeBotMove(gameId: number): Observable<MoveResponse>`
- Dodanie metody `surrenderGame(gameId: number): Observable<UpdateGameStatusResponse>`
- Implementacja wywołań API

**2.2 Implementacja WebSocketService**:
- Utworzenie serwisu `WebSocketService` w `frontend/src/app/services/websocket.service.ts`
- Implementacja metody `connect(gameId: number, token: string): Observable<void>`
- Implementacja metody `disconnect(): void`
- Implementacja metody `sendMove(row: number, col: number, playerSymbol: 'x' | 'o'): void`
- Implementacja metody `sendSurrender(): void`
- Implementacja metody `getMessages(): Observable<WebSocketMessage>`
- Implementacja obsługi reconnect (max 20 sekund)

**2.3 Rozszerzenie AuthService**:
- Weryfikacja czy metody `getCurrentUser()` i `getToken()` istnieją
- Dodanie brakujących metod jeśli potrzeba

**2.4 Testy jednostkowe serwisów**:
- Testy dla `GameService.getGame()`
- Testy dla `GameService.makeMove()`
- Testy dla `GameService.makeBotMove()`
- Testy dla `GameService.surrenderGame()`
- Testy dla `WebSocketService.connect()`
- Testy dla `WebSocketService.getMessages()`
- Testy dla `WebSocketService.sendMove()`

### Krok 3: Implementacja komponentów współdzielonych

**3.1 Implementacja GameBoardComponent**:
- Utworzenie komponentu standalone
- Implementacja template z siatką planszy (dynamiczny rozmiar)
- Implementacja komórek z symbolami X i O
- Implementacja animacji ruchów (scale + fade-in, 300ms)
- Implementacja animacji linii wygranej (stroke-dasharray, 500ms opóźnienie)
- Implementacja obsługi kliknięć na komórki
- Implementacja EventEmitter `move`
- Implementacja propsów (`boardSize`, `boardState`, `currentPlayerSymbol`, `disabled`, `gameStatus`, `winLine`)
- Stylowanie komponentu (SCSS)
- Testy jednostkowe

**3.2 Implementacja GameInfoComponent**:
- Utworzenie komponentu standalone
- Implementacja template z informacjami o grze
- Implementacja wyświetlania typu gry, przeciwnika, statusu, aktualnego gracza, liczby tur
- Implementacja propsów (`game`, `currentUser`)
- Stylowanie komponentu (SCSS)
- Testy jednostkowe

**3.3 Implementacja GameTimerComponent**:
- Utworzenie komponentu standalone
- Implementacja template z wyświetlaczem timera
- Implementacja wizualnych ostrzeżeń (warning, danger) przy niskim czasie
- Implementacja paska postępu
- Implementacja propsów (`remainingSeconds`, `currentPlayerSymbol`)
- Stylowanie komponentu (SCSS)
- Testy jednostkowe

**3.4 Implementacja GameBotIndicatorComponent**:
- Utworzenie komponentu standalone
- Implementacja template z animowanym spinnerem i tekstem "Bot myśli..."
- Stylowanie komponentu (SCSS)
- Testy jednostkowe

**3.5 Implementacja GameResultDialogComponent**:
- Utworzenie komponentu standalone
- Implementacja template z dialogiem PrimeNG
- Implementacja wyświetlania wyniku gry (wygrana, przegrana, remis)
- Implementacja wyświetlania zdobytych punktów
- Implementacja przycisku zamknięcia
- Implementacja EventEmitter `close`
- Implementacja propsów (`game`, `currentUser`)
- Stylowanie komponentu (SCSS)
- Testy jednostkowe

### Krok 4: Implementacja GameComponent

**4.1 Utworzenie komponentu**:
- Utworzenie pliku `frontend/src/app/features/game/game.component.ts`
- Utworzenie pliku `frontend/src/app/features/game/game.component.html`
- Utworzenie pliku `frontend/src/app/features/game/game.component.scss`

**4.2 Implementacja logiki komponentu**:
- Import wymaganych modułów (CommonModule, RouterModule, PrimeNG modules)
- Implementacja właściwości komponentu (`gameId`, `game$`, `currentUser$`, `isLoading$`, `timerSeconds$`, `isBotThinking$`, `showResultDialog$`)
- Implementacja subskrypcji (`gameSubscription`, `timerSubscription`, `websocketSubscription`)
- Implementacja metody `ngOnInit()` z inicjalizacją
- Implementacja metody `ngOnDestroy()` z czyszczeniem
- Implementacja metody `loadGame()`
- Implementacja metody `setupGameUpdates()` (polling fallback)
- Implementacja metody `connectWebSocket()`
- Implementacja metody `startTimer()`
- Implementacja metody `checkTimeout()`
- Implementacja metody `onMove(move)`
- Implementacja metody `sendMoveViaREST(move)`
- Implementacja metody `sendMoveViaWebSocket(move)`
- Implementacja metody `makeBotMove()`
- Implementacja metody `handleMoveResponse(response)`
- Implementacja metody `handleWebSocketMessage(message)`
- Implementacja metody `updateGameFromMove(payload)`
- Implementacja metody `updateGameFromPayload(payload)`
- Implementacja metody `handleGameStatusChange(game)`
- Implementacja metody `onSurrender()`
- Implementacja metody `onResultDialogClose()`
- Implementacja metody `isMoveDisabled(game)`
- Implementacja metody `getGameTitle(game)`
- Implementacja metody `hasGameChanged(oldGame, newGame)`
- Implementacja metod obsługi błędów (`handleError`, `handleMoveError`, `handleWebSocketError`)

**4.3 Implementacja template**:
- Struktura HTML z sekcjami (header, info, board, dialog, loading)
- Warunkowe wyświetlanie komponentów (`*ngIf`)
- Użycie `async` pipe dla Observable
- Integracja z komponentami współdzielonymi
- Obsługa zdarzeń (`(move)`, `(close)`, `(onClick)`)

**4.4 Stylowanie**:
- Implementacja stylów SCSS dla `.game-container`
- Stylowanie sekcji nagłówka
- Stylowanie sekcji informacji
- Stylowanie kontenera planszy
- Responsywność dla różnych rozdzielczości ekranu

### Krok 5: Konfiguracja routingu

**5.1 Dodanie routingu**:
- Dodanie ścieżki `/game/:gameId` do konfiguracji routingu
- Powiązanie ścieżki z `GameComponent`
- Dodanie `AuthGuard` do ochrony routingu

**5.2 Testy routingu**:
- Testy jednostkowe routingu
- Testy E2E nawigacji do widoku gry

### Krok 6: Implementacja animacji

**6.1 Animacje Angular**:
- Fade-in dla planszy (300ms)
- Scale animation dla symboli X i O (0 → 1, 300ms)
- Stroke-dasharray animation dla linii wygranej (500ms opóźnienie)
- Smooth transitions dla przycisków

**6.2 CSS Transitions**:
- Transitions dla hover states komórek planszy
- Transitions dla focus states
- Transitions dla disabled states

### Krok 7: Implementacja obsługi błędów

**7.1 Obsługa błędów API**:
- Implementacja `catchError()` w Observable
- Implementacja toast notifications dla błędów
- Implementacja przekierowań dla błędów 404, 403, 401
- Implementacja fallback values dla błędów

**7.2 Obsługa błędów WebSocket**:
- Implementacja obsługi `MOVE_REJECTED`
- Implementacja obsługi rozłączeń
- Implementacja reconnect (max 20 sekund)
- Implementacja fallback do polling

**7.3 Obsługa błędów walidacji**:
- Implementacja walidacji po stronie klienta
- Implementacja komunikatów błędów dla nieprawidłowych ruchów

### Krok 8: Implementacja i18n

**8.1 Konfiguracja Angular i18n**:
- Konfiguracja plików tłumaczeń (en, pl)
- Dodanie kluczy tłumaczeń dla wszystkich tekstów w komponencie

**8.2 Użycie tłumaczeń**:
- Zastąpienie hardcoded tekstów pipe `translate`
- Testy dla różnych języków

### Krok 9: Testy

**9.1 Testy jednostkowe**:
- Testy dla `GameComponent` (Jest + Angular Testing Library)
- Testy dla `GameBoardComponent`
- Testy dla `GameInfoComponent`
- Testy dla `GameTimerComponent`
- Testy dla `GameBotIndicatorComponent`
- Testy dla `GameResultDialogComponent`
- Testy dla serwisów (`GameService`, `WebSocketService`)

**9.2 Testy E2E (Cypress)**:
- Scenariusz: Gra vs bot (łatwy poziom)
- Scenariusz: Gra vs bot (średni poziom)
- Scenariusz: Gra vs bot (trudny poziom)
- Scenariusz: Gra PvP z WebSocket
- Scenariusz: Poddanie gry
- Scenariusz: Timeout w grze PvP
- Scenariusz: Reconnect WebSocket

### Krok 10: Dostępność (a11y)

**10.1 ARIA labels**:
- Dodanie `aria-label` dla wszystkich przycisków
- Dodanie `aria-label` dla komórek planszy
- Dodanie `aria-describedby` dla sekcji z opisami

**10.2 Keyboard navigation**:
- Obsługa nawigacji klawiaturą dla komórek planszy (opcjonalne)
- Obsługa Enter/Space dla aktywacji przycisków
- Focus indicators dla wszystkich interaktywnych elementów

**10.3 Screen reader support**:
- Semantyczne znaczniki HTML
- Opisy dla screen readerów dla statusu gry

### Krok 11: Optymalizacja wydajności

**11.1 Change detection**:
- Użycie `OnPush` change detection strategy jeśli możliwe
- Optymalizacja subskrypcji Observable
- Unikanie niepotrzebnych aktualizacji

**11.2 Caching**:
- Cache'owanie stanu gry w BehaviorSubject
- Unikanie niepotrzebnych wywołań API

**11.3 WebSocket**:
- Optymalizacja reconnect
- Fallback do polling jeśli WebSocket nie działa

### Krok 12: Code review i dokumentacja

**12.1 Code review**:
- Sprawdzenie zgodności z zasadami implementacji
- Weryfikacja zgodności z ESLint i Prettier
- Review bezpieczeństwa i wydajności

**12.2 Dokumentacja**:
- Aktualizacja README z informacjami o komponencie
- Dokumentacja API endpoints używanych przez komponent
- Dokumentacja WebSocket protocol

### Krok 13: Wdrożenie

**13.1 Merge do głównej gałęzi**:
- Utworzenie Pull Request
- Code review przez zespół
- Merge po akceptacji

**13.2 Weryfikacja w środowisku deweloperskim**:
- Testy manualne wszystkich scenariuszy
- Weryfikacja działania na różnych przeglądarkach
- Weryfikacja responsywności
- Weryfikacja działania WebSocket

**13.3 Wdrożenie na produkcję**:
- Wdrożenie przez CI/CD pipeline
- Monitorowanie błędów po wdrożeniu
- Zbieranie feedbacku od użytkowników

