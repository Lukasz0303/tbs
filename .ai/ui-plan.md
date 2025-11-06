# Plan Architektury UI - World at War: Turn-Based Strategy

## 1. Przegląd architektury

Aplikacja wykorzystuje Angular 17 z architekturą opartą na feature modules i shared components. Interfejs użytkownika jest zaprojektowany z myślą o wysokiej jakości wizualnej, responsywności i płynnych animacjach używając Angular Animations, PrimeNG i CSS transitions.

### 1.1 Stack technologiczny UI

- **Framework**: Angular 17 (standalone components)
- **UI Library**: PrimeNG
- **Animacje**: Angular Animations + CSS Transitions
- **Styling**: SCSS + Tailwind CSS
- **State Management**: Angular Services z BehaviorSubject/ReplaySubject
- **Forms**: Reactive Forms
- **Routing**: Angular Router

### 1.2 Zasady projektowe

- **Komponenty standalone**: Wszystkie komponenty używają standalone API
- **Lazy loading**: Feature modules ładowane na żądanie
- **Reaktywne formularze**: Wszystkie formularze używają Reactive Forms
- **Type safety**: Pełne typowanie TypeScript dla wszystkich modeli danych
- **Accessibility**: Podstawowa dostępność (ARIA labels, keyboard navigation)
- **Responsywność**: Optymalizacja dla ekranów PC (min. 1280px szerokości)

---

## 2. Struktura widoków i routing

### 2.1 Hierarchia widoków

```
/ (HomeComponent) - Ekran startowy
├── /auth/login - Logowanie
├── /auth/register - Rejestracja
├── /game - Widok gry (vs_bot lub pvp)
│   ├── /game/:gameId - Szczegóły gry
│   └── /game/matchmaking - Oczekiwanie na przeciwnika
├── /leaderboard - Ranking graczy
├── /profile - Profil użytkownika
└── /404 - Strona błędu
```

### 2.2 Konfiguracja routingu

```typescript
export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', component: HomeComponent },
      { path: 'auth/login', component: AuthLoginComponent },
      { path: 'auth/register', component: AuthRegisterComponent },
      { path: 'game', component: GameComponent },
      { path: 'game/:gameId', component: GameComponent },
      { path: 'game/matchmaking', component: MatchmakingComponent },
      { path: 'leaderboard', component: LeaderboardComponent },
      { path: 'profile', component: ProfileComponent },
      { path: '404', component: NotFoundComponent },
      { path: '**', redirectTo: '404' }
    ]
  }
];
```

### 2.3 Layout główny

**MainLayoutComponent** zawiera:
- **Header** (NavbarComponent) - zawsze widoczny
  - Logo aplikacji
  - Menu nawigacyjne (Graj, Ranking, Profil)
  - Wskaźnik statusu użytkownika (gość/zalogowany)
  - Przycisk logowania/wylogowania
- **Router outlet** - miejsce na widoki
- **Toast container** - PrimeNG Toast dla powiadomień

---

## 3. Szczegółowa architektura widoków

### 3.1 HomeComponent - Ekran startowy

**Lokalizacja**: `features/home/home.component.ts`

**Funkcjonalność**:
- Wyświetlanie podstawowych informacji o grze
- Banner z ostatnią zapisaną grą (jeśli istnieje)
- Przyciski akcji:
  - "Graj jako gość" - natychmiastowe utworzenie sesji gościa
  - "Graj z botem" - przekierowanie do wyboru trybu vs_bot
  - "Graj PvP" - dołączenie do matchmakingu
  - "Zaloguj się" / "Zarejestruj się"

**Komponenty**:
- `GameBannerComponent` - banner z ostatnią grą (warunkowo)
- `GameModeCardComponent` - karty z trybami gry

**Integracja API**:
- `GET /api/games?status=in_progress&size=1` - sprawdzenie zapisanej gry
- `POST /api/guests` - utworzenie sesji gościa (jeśli gość)

**Stan**:
- Sprawdzenie statusu użytkownika (AuthService)
- Sprawdzenie zapisanej gry przy inicjalizacji

---

### 3.2 AuthLoginComponent - Logowanie

**Lokalizacja**: `features/auth/auth-login.component.ts`

**Funkcjonalność**:
- Formularz logowania (email, hasło)
- Walidacja pól (reactive forms)
- Link do rejestracji
- Obsługa błędów (401, 404)

**Komponenty**:
- PrimeNG InputText (email)
- PrimeNG Password (hasło)
- PrimeNG Button (submit)

**Integracja API**:
- `POST /api/auth/login`

**Walidacja**:
- Email: wymagany, format email
- Hasło: wymagane, min. długość

**Po sukcesie**:
- Zapisanie tokenu JWT
- Aktualizacja stanu użytkownika (AuthService)
- Przekierowanie do HomeComponent lub ostatniej gry

---

### 3.3 AuthRegisterComponent - Rejestracja

**Lokalizacja**: `features/auth/auth-register.component.ts`

**Funkcjonalność**:
- Formularz rejestracji (nazwa użytkownika, email, hasło, potwierdzenie hasła)
- Walidacja pól (reactive forms)
- Link do logowania
- Obsługa błędów (409, 422)

**Komponenty**:
- PrimeNG InputText (username, email)
- PrimeNG Password (hasło, potwierdzenie)
- PrimeNG Button (submit)

**Integracja API**:
- `POST /api/auth/register`

**Walidacja**:
- Nazwa użytkownika: wymagana, 3-50 znaków, alfanumeryczne + podkreślniki
- Email: wymagany, format email, unikalny
- Hasło: wymagane, min. długość, wymagania bezpieczeństwa
- Potwierdzenie hasła: musi być zgodne z hasłem

**Po sukcesie**:
- Zapisanie tokenu JWT
- Aktualizacja stanu użytkownika (AuthService)
- Przekierowanie do HomeComponent

---

### 3.4 GameComponent - Widok gry

**Lokalizacja**: `features/game/game.component.ts`

**Funkcjonalność**:
- Wyświetlanie planszy gry (dynamiczny rozmiar)
- Obsługa ruchów gracza
- Wyświetlanie informacji o grze (przeciwnik, status, timer)
- Przycisk poddania (dla PvP)
- Obsługa WebSocket (dla PvP)
- Animacje ruchów i wygranej

**Komponenty**:
- `GameBoardComponent` - plansza gry
- `GameInfoComponent` - informacje o grze
- `GameTimerComponent` - timer (dla PvP)
- `GameBotIndicatorComponent` - wskaźnik "Bot myśli..." (dla vs_bot)

**Integracja API**:
- `GET /api/games/{gameId}` - pobranie stanu gry
- `POST /api/games/{gameId}/moves` - wykonanie ruchu
- `PUT /api/games/{gameId}/status` - poddanie gry
- WebSocket `/ws/game/{gameId}` - komunikacja real-time (PvP)

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

---

### 3.5 GameBoardComponent - Plansza gry

**Lokalizacja**: `components/game/game-board.component.ts`

**Funkcjonalność**:
- Dynamiczne renderowanie planszy (3x3, 4x4, 5x5)
- Obsługa kliknięć na komórki
- Walidacja ruchów po stronie klienta
- Wyświetlanie symboli (X, O)
- Animacje komórek
- Wizualizacja linii wygranej

**Implementacja**:
- CSS Grid: `grid-template-columns: repeat(boardSize, 1fr)`
- Komórki jako osobne komponenty `GameCellComponent`
- Stan planszy z `boardState` z API
- Blokada kliknięć na zajęte pola (disabled state)

**Komponenty**:
- `GameCellComponent` - pojedyncza komórka planszy

---

### 3.6 MatchmakingComponent - Oczekiwanie na przeciwnika

**Lokalizacja**: `features/game/matchmaking.component.ts`

**Funkcjonalność**:
- Wyświetlanie animacji ładowania
- Wskaźnik postępu
- Szacowany czas oczekiwania
- Przycisk anulowania kolejki

**Integracja API**:
- `POST /api/v1/matching/queue` - dołączenie do kolejki
- `DELETE /api/v1/matching/queue` - anulowanie kolejki
- Polling `GET /api/games/{gameId}` co 2 sekundy (alternatywa dla WebSocket)

**Po znalezieniu przeciwnika**:
- Automatyczne przekierowanie do `GameComponent` z `gameId`
- Nawiązanie połączenia WebSocket

---

### 3.7 GameModeSelectionComponent - Wybór trybu vs_bot

**Lokalizacja**: `features/game/game-mode-selection.component.ts`

**Funkcjonalność**:
- Wybór rozmiaru planszy (3x3, 4x4, 5x5)
- Wybór poziomu trudności (łatwy, średni, trudny)
- Natychmiastowe rozpoczęcie gry po wyborze

**Komponenty**:
- `BoardSizeCardComponent` - karty z rozmiarami planszy
- `DifficultyCardComponent` - karty z poziomami trudności

**Integracja API**:
- `POST /api/games` - utworzenie gry vs_bot

**Po utworzeniu gry**:
- Przekierowanie do `GameComponent` z `gameId`

---

### 3.8 LeaderboardComponent - Ranking graczy

**Lokalizacja**: `features/leaderboard/leaderboard.component.ts`

**Funkcjonalność**:
- Wyświetlanie pozycji użytkownika (jeśli zarejestrowany)
- Tabela z rankingiem (paginacja)
- Przycisk "Pokaż graczy wokół mnie" (dla zarejestrowanych)
- Wizualne wyróżnienie pozycji użytkownika

**Komponenty**:
- PrimeNG Table - tabela z rankingiem
- `UserRankCardComponent` - karta z pozycją użytkownika
- `PlayersAroundDialogComponent` - dialog z graczami wokół użytkownika

**Integracja API**:
- `GET /api/rankings/{userId}` - pozycja użytkownika
- `GET /api/rankings` - lista graczy (paginacja)
- `GET /api/rankings/around/{userId}` - gracze wokół użytkownika

**Kolumny tabeli**:
- Pozycja
- Nazwa użytkownika
- Punkty
- Rozegrane gry
- Wygrane gry

---

### 3.9 ProfileComponent - Profil użytkownika

**Lokalizacja**: `features/profile/profile.component.ts`

**Funkcjonalność**:
- Wyświetlanie podstawowych informacji (nazwa, email)
- Statystyki (punkty, rozegrane gry, wygrane)
- Pozycja w rankingu
- Ostatnia zapisana gra (jeśli istnieje)
- Możliwość edycji nazwy użytkownika (tylko zarejestrowani)

**Komponenty**:
- PrimeNG Cards - karty ze statystykami
- `LastGameCardComponent` - karta z ostatnią grą
- `EditUsernameDialogComponent` - dialog edycji nazwy

**Integracja API**:
- `GET /api/auth/me` - profil użytkownika
- `GET /api/rankings/{userId}` - pozycja w rankingu
- `GET /api/games?status=in_progress&size=1` - ostatnia gra
- `PUT /api/v1/users/{userId}` - aktualizacja nazwy

**Dla gości**:
- Ograniczone informacje
- Zachęta do rejestracji

---

## 4. Komponenty współdzielone

### 4.1 NavbarComponent - Nawigacja główna

**Lokalizacja**: `components/navigation/navbar/navbar.component.ts`

**Funkcjonalność**:
- Logo aplikacji (link do home)
- Menu nawigacyjne:
  - Graj (dropdown: vs bot, PvP)
  - Ranking
  - Profil
- Wskaźnik statusu użytkownika:
  - "Gość" (dla gości)
  - Nazwa użytkownika (dla zarejestrowanych)
- Przycisk logowania/wylogowania

**Komponenty**:
- PrimeNG Menu - menu nawigacyjne
- PrimeNG Avatar - avatar użytkownika

**Integracja**:
- AuthService - status użytkownika
- Router - nawigacja

---

### 4.2 GameBannerComponent - Banner z ostatnią grą

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

---

### 4.3 LoaderComponent - Wskaźnik ładowania

**Lokalizacja**: `components/ui/loader/loader.component.ts`

**Funkcjonalność**:
- Globalny wskaźnik ładowania
- Używany podczas żądań API
- Animacja spinner (PrimeNG ProgressSpinner)

---

### 4.4 ButtonComponent - Przycisk

**Lokalizacja**: `components/ui/button/button.component.ts`

**Funkcjonalność**:
- Standaryzowany przycisk
- Warianty (primary, secondary, danger)
- Rozmiary (small, medium, large)
- Stany (disabled, loading)

---

## 5. Serwisy i zarządzanie stanem

### 5.1 AuthService - Uwierzytelnianie

**Lokalizacja**: `services/auth.service.ts`

**Funkcjonalność**:
- Zarządzanie sesją użytkownika
- Przechowywanie tokenu JWT
- Obsługa logowania/rejestracji/wylogowania
- Sprawdzanie statusu użytkownika (gość/zarejestrowany)

**State**:
```typescript
private currentUser$ = new BehaviorSubject<User | null>(null);
private isGuest$ = new BehaviorSubject<boolean>(false);
```

**Metody**:
- `login(email, password): Observable<User>`
- `register(username, email, password): Observable<User>`
- `logout(): void`
- `getCurrentUser(): Observable<User | null>`
- `isAuthenticated(): Observable<boolean>`
- `isGuest(): Observable<boolean>`
- `createGuestSession(): Observable<GuestUser>`

---

### 5.2 GameService - Zarządzanie grami

**Lokalizacja**: `services/game.service.ts`

**Funkcjonalność**:
- Tworzenie gier (vs_bot, pvp)
- Pobieranie stanu gry
- Wykonywanie ruchów
- Pobieranie zapisanych gier
- Zarządzanie stanem aktualnej gry

**State**:
```typescript
private currentGame$ = new BehaviorSubject<Game | null>(null);
private savedGame$ = new BehaviorSubject<Game | null>(null);
```

**Metody**:
- `createGame(gameType, boardSize, botDifficulty?): Observable<Game>`
- `getGame(gameId): Observable<Game>`
- `makeMove(gameId, row, col, playerSymbol): Observable<MoveResponse>`
- `surrenderGame(gameId): Observable<void>`
- `getSavedGame(): Observable<Game | null>`
- `getCurrentGame(): Observable<Game | null>`

---

### 5.3 WebSocketService - Komunikacja WebSocket

**Lokalizacja**: `services/websocket.service.ts`

**Funkcjonalność**:
- Nawiązywanie połączenia WebSocket
- Obsługa reconnect (max 20 sekund)
- Wysyłanie wiadomości (MOVE, SURRENDER, PING)
- Odbieranie wiadomości (MOVE_ACCEPTED, OPPONENT_MOVE, TIMER_UPDATE, etc.)
- Zarządzanie stanem połączenia

**State**:
```typescript
private connectionStatus$ = new BehaviorSubject<'connected' | 'disconnected' | 'connecting'>('disconnected');
private messages$ = new Subject<WebSocketMessage>();
```

**Metody**:
- `connect(gameId, token): Observable<void>`
- `disconnect(): void`
- `sendMove(row, col, playerSymbol): void`
- `surrender(): void`
- `getMessages(): Observable<WebSocketMessage>`
- `getConnectionStatus(): Observable<string>`
- `reconnect(): Observable<void>`

---

### 5.4 RankingService - Ranking

**Lokalizacja**: `services/ranking.service.ts`

**Funkcjonalność**:
- Pobieranie rankingu (paginacja)
- Pobieranie pozycji użytkownika
- Pobieranie graczy wokół użytkownika

**Metody**:
- `getRanking(page, size): Observable<RankingPage>`
- `getUserRanking(userId): Observable<Ranking>`
- `getPlayersAround(userId, range): Observable<Ranking[]>`

---

### 5.5 MatchmakingService - Matchmaking

**Lokalizacja**: `services/matchmaking.service.ts`

**Funkcjonalność**:
- Dołączanie do kolejki matchmakingu
- Opuszczanie kolejki
- Wyzwanie konkretnego gracza
- Sprawdzanie statusu matchmakingu

**Metody**:
- `joinQueue(boardSize): Observable<MatchmakingResponse>`
- `leaveQueue(): Observable<void>`
- `challengePlayer(userId, boardSize): Observable<Game>`
- `getEstimatedWaitTime(): Observable<number>`

---

### 5.6 ErrorService - Obsługa błędów

**Lokalizacja**: `services/error.service.ts`

**Funkcjonalność**:
- Centralna obsługa błędów API
- Wyświetlanie toast notifications
- Przekierowania dla błędów autoryzacji
- Logowanie błędów (tryb deweloperski)

**Metody**:
- `handleError(error: HttpErrorResponse): void`
- `showErrorToast(message: string): void`
- `handleAuthError(): void`

---

## 6. Modele danych

### 6.1 User

```typescript
interface User {
  userId: number;
  username: string | null;
  email: string;
  isGuest: boolean;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
  lastSeenAt: string | null;
}
```

### 6.2 Game

```typescript
interface Game {
  gameId: number;
  gameType: 'vs_bot' | 'pvp';
  boardSize: 3 | 4 | 5;
  player1Id: number;
  player2Id: number | null;
  botDifficulty: 'easy' | 'medium' | 'hard' | null;
  status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw';
  currentPlayerSymbol: 'x' | 'o' | null;
  winnerId: number | null;
  lastMoveAt: string | null;
  createdAt: string;
  updatedAt: string;
  finishedAt: string | null;
  boardState: string[][];
  totalMoves: number;
}
```

### 6.3 Move

```typescript
interface Move {
  moveId: number;
  gameId: number;
  row: number;
  col: number;
  playerSymbol: 'x' | 'o';
  moveOrder: number;
  playerId: number | null;
  createdAt: string;
}
```

### 6.4 MoveResponse

```typescript
interface MoveResponse {
  moveId: number;
  gameId: number;
  row: number;
  col: number;
  playerSymbol: 'x' | 'o';
  moveOrder: number;
  createdAt: string;
  boardState: string[][];
  gameStatus: 'in_progress' | 'finished' | 'draw';
  winner: {
    userId: number;
    username: string;
  } | null;
}
```

### 6.5 Ranking

```typescript
interface Ranking {
  rankPosition: number;
  userId: number;
  username: string;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
}
```

### 6.6 WebSocketMessage

```typescript
interface WebSocketMessage {
  type: 'MOVE_ACCEPTED' | 'MOVE_REJECTED' | 'OPPONENT_MOVE' | 'GAME_UPDATE' | 'TIMER_UPDATE' | 'GAME_ENDED' | 'PONG';
  payload: any;
}
```

---

## 7. Mapy podróży użytkownika

### 7.1 Scenariusz I: Gracz gość → PvP

1. **Ekran startowy** (`/`)
   - Użytkownik widzi opcje gry
   - Kliknięcie "Graj jako gość" → `POST /api/guests`
   - Kliknięcie "Graj PvP" → przekierowanie do `/game/matchmaking`

2. **Oczekiwanie na przeciwnika** (`/game/matchmaking`)
   - Wyświetlenie animacji ładowania
   - `POST /api/v1/matching/queue` z boardSize
   - Polling lub WebSocket czeka na przeciwnika
   - Po znalezieniu: przekierowanie do `/game/:gameId`

3. **Widok gry** (`/game/:gameId`)
   - Nawiązanie połączenia WebSocket
   - Wyświetlenie planszy i informacji o grze
   - Wykonywanie ruchów przez WebSocket
   - Timer dla każdego gracza (10 sekund)
   - Po zakończeniu: wyświetlenie wyniku i przekierowanie do home

### 7.2 Scenariusz II: Rejestracja → Logowanie

1. **Ekran startowy** (`/`)
   - Kliknięcie "Zarejestruj się" → `/auth/register`

2. **Rejestracja** (`/auth/register`)
   - Wypełnienie formularza (username, email, hasło)
   - Walidacja pól
   - `POST /api/auth/register`
   - Po sukcesie: automatyczne logowanie i przekierowanie do `/`

3. **Logowanie** (`/auth/login`)
   - Wypełnienie formularza (email, hasło)
   - `POST /api/auth/login`
   - Po sukcesie: przekierowanie do `/` lub ostatniej gry

### 7.3 Scenariusz III: Gracz gość → vs bot

1. **Ekran startowy** (`/`)
   - Kliknięcie "Graj jako gość" → `POST /api/guests`
   - Kliknięcie "Graj z botem" → `/game/mode-selection`

2. **Wybór trybu** (`/game/mode-selection`)
   - Wybór rozmiaru planszy (3x3, 4x4, 5x5)
   - Wybór poziomu trudności (łatwy, średni, trudny)
   - `POST /api/games` z parametrami
   - Przekierowanie do `/game/:gameId`

3. **Widok gry** (`/game/:gameId`)
   - Wyświetlenie planszy
   - Wykonywanie ruchów przez REST API
   - Po ruchu gracza: automatyczny ruch bota
   - Po zakończeniu: wyświetlenie wyniku i przekierowanie do home

### 7.4 Scenariusz IV: Przegląd rankingu → Wybór przeciwnika

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

---

## 8. Szczegóły implementacji

### 8.1 Plansza gry - implementacja

**GameBoardComponent**:

```typescript
@Component({
  selector: 'app-game-board',
  standalone: true,
  template: `
    <div class="game-board" [style.grid-template-columns]="'repeat(' + boardSize + ', 1fr)'">
      <app-game-cell
        *ngFor="let cell of cells; let i = index"
        [row]="getRow(i)"
        [col]="getCol(i)"
        [symbol]="getSymbol(i)"
        [disabled]="isCellDisabled(i)"
        (cellClick)="onCellClick($event)"
        [@cellAnimation]="getAnimationState(i)">
      </app-game-cell>
    </div>
  `,
  styles: [`
    .game-board {
      display: grid;
      gap: 8px;
      max-width: 600px;
      margin: 0 auto;
    }
  `],
  animations: [
    trigger('cellAnimation', [
      transition(':enter', [
        style({ transform: 'scale(0)', opacity: 0 }),
        animate('300ms', style({ transform: 'scale(1)', opacity: 1 }))
      ])
    ])
  ]
})
export class GameBoardComponent {
  @Input() boardSize: 3 | 4 | 5 = 3;
  @Input() boardState: string[][] = [];
  @Input() currentPlayerSymbol: 'x' | 'o' | null = null;
  @Input() disabled: boolean = false;
  
  @Output() move = new EventEmitter<{row: number, col: number}>();
  
  get cells(): number[] {
    return Array(this.boardSize * this.boardSize).fill(0);
  }
  
  getRow(index: number): number {
    return Math.floor(index / this.boardSize);
  }
  
  getCol(index: number): number {
    return index % this.boardSize;
  }
  
  getSymbol(index: number): string | null {
    const row = this.getRow(index);
    const col = this.getCol(index);
    return this.boardState[row]?.[col] || null;
  }
  
  isCellDisabled(index: number): boolean {
    return this.disabled || this.getSymbol(index) !== null;
  }
  
  onCellClick(event: {row: number, col: number}): void {
    if (!this.isCellDisabled(this.getRowIndex(event.row, event.col))) {
      this.move.emit(event);
    }
  }
}
```

### 8.2 Timer w grze PvP

**GameTimerComponent**:

```typescript
@Component({
  selector: 'app-game-timer',
  standalone: true,
  template: `
    <div class="timer" [class.warning]="remainingSeconds <= 3" [class.danger]="remainingSeconds <= 1">
      <span class="timer-text">{{ remainingSeconds }}s</span>
      <div class="timer-progress" [style.width.%]="(remainingSeconds / 10) * 100"></div>
    </div>
  `,
  styles: [`
    .timer {
      position: relative;
      padding: 8px 16px;
      border-radius: 8px;
      background: #f0f0f0;
      transition: background-color 0.3s;
    }
    .timer.warning {
      background: #ffa500;
      animation: pulse 1s infinite;
    }
    .timer.danger {
      background: #ff0000;
      animation: pulse 0.5s infinite;
    }
    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.7; }
    }
  `]
})
export class GameTimerComponent {
  @Input() remainingSeconds: number = 10;
}
```

### 8.3 Obsługa WebSocket - reconnect

**WebSocketService** (fragment):

```typescript
reconnect(maxAttempts: number = 20, interval: number = 1000): Observable<void> {
  return new Observable(observer => {
    let attempts = 0;
    const reconnectInterval = setInterval(() => {
      attempts++;
      if (attempts > maxAttempts) {
        clearInterval(reconnectInterval);
        observer.error(new Error('Max reconnection attempts reached'));
        return;
      }
      
      this.connect(this.currentGameId!, this.currentToken!).subscribe({
        next: () => {
          clearInterval(reconnectInterval);
          observer.next();
          observer.complete();
        },
        error: () => {
          // Kontynuuj próby
        }
      });
    }, interval);
  });
}
```

### 8.4 Walidacja formularzy

**AuthRegisterComponent** (fragment):

```typescript
this.registerForm = this.fb.group({
  username: ['', [
    Validators.required,
    Validators.minLength(3),
    Validators.maxLength(50),
    Validators.pattern(/^[a-zA-Z0-9_]+$/)
  ]],
  email: ['', [Validators.required, Validators.email]],
  password: ['', [Validators.required, Validators.minLength(8)]],
  confirmPassword: ['', [Validators.required]]
}, {
  validators: this.passwordMatchValidator
});

passwordMatchValidator(form: AbstractControl): ValidationErrors | null {
  const password = form.get('password');
  const confirmPassword = form.get('confirmPassword');
  return password && confirmPassword && password.value !== confirmPassword.value
    ? { passwordMismatch: true }
    : null;
}
```

---

## 9. Obsługa błędów

### 9.1 Strategia obsługi błędów

**ErrorService**:

```typescript
handleError(error: HttpErrorResponse): void {
  switch (error.status) {
    case 400:
    case 422:
      this.showErrorToast(error.error?.message || 'Nieprawidłowe dane');
      break;
    case 401:
    case 403:
      this.handleAuthError();
      break;
    case 404:
      this.showErrorToast('Zasób nie został znaleziony');
      break;
    case 409:
      this.showErrorToast(error.error?.message || 'Konflikt danych');
      break;
    case 500:
    case 503:
      this.showErrorToast('Błąd serwera. Spróbuj ponownie później.');
      break;
    default:
      this.showErrorToast('Wystąpił nieoczekiwany błąd');
  }
  
  if (environment.production === false) {
    console.error('Error details:', error);
  }
}
```

### 9.2 Toast notifications

Użycie PrimeNG Toast dla wszystkich błędów walidacji i konfliktów:

```typescript
this.messageService.add({
  severity: 'error',
  summary: 'Błąd',
  detail: message,
  life: 5000
});
```

---

## 10. Animacje i przejścia

### 10.1 Animacje komórek planszy

- **Pojawienie się symbolu**: scale (0 → 1) + fade-in, 300ms
- **Ruch bota**: opóźnienie 200ms + animacja symbolu
- **Linia wygranej**: stroke-dasharray animation, 500ms opóźnienie

### 10.2 Przejścia między widokami

- Użycie Angular Router transitions
- Fade-in/fade-out dla głównych widoków
- Slide-in dla modali i dialogów

### 10.3 Animacje ładowania

- PrimeNG ProgressSpinner dla globalnego ładowania
- Skeleton loaders dla danych asynchronicznych
- Pulse animation dla timerów

---

## 11. Responsywność

### 11.1 Breakpoints

- **Desktop**: min. 1280px szerokości
- **Tablet**: 768px - 1279px (opcjonalne dla MVP)
- **Mobile**: < 768px (poza zakresem MVP)

### 11.2 Adaptacja layoutu

- Plansza gry: maksymalna szerokość 600px, wyśrodkowana
- Tabela rankingu: scroll poziomy dla mniejszych ekranów
- Menu nawigacyjne: collapse do hamburger menu dla tabletów

---

## 12. Dostępność

### 12.1 Podstawowe wymagania

- ARIA labels dla wszystkich interaktywnych elementów
- Keyboard navigation dla formularzy
- Focus indicators dla wszystkich przycisków
- Alt text dla ikon i obrazów
- Semantic HTML (header, nav, main, footer)

### 12.2 Kontrast i czytelność

- Minimalny kontrast 4.5:1 dla tekstu
- Wizualne wskaźniki focus
- Czytelne czcionki (min. 14px)

---

## 13. Optymalizacja wydajności

### 13.1 Lazy loading

- Feature modules ładowane na żądanie
- Komponenty PrimeNG importowane selektywnie

### 13.2 Change detection

- OnPush change detection strategy dla komponentów
- TrackBy functions dla *ngFor

### 13.3 Cache'owanie

- Cache'owanie odpowiedzi API dla rankingów (5-15 minut)
- LocalStorage dla tokenu JWT
- SessionStorage dla stanu tymczasowego

---

## 14. Testowanie

### 14.1 Testy jednostkowe

- Komponenty: Angular Testing Library
- Serwisy: Mockowanie HTTP requests
- Walidacja formularzy

### 14.2 Testy E2E (Cypress)

- Scenariusze użytkownika (US-001 do US-015)
- Przepływy uwierzytelniania
- Rozgrywki (vs_bot, PvP)
- Aktualizacje rankingu

---

## 15. Checklist implementacji

### 15.1 Faza 1: Podstawowa struktura

- [ ] Konfiguracja routingu
- [ ] MainLayoutComponent z NavbarComponent
- [ ] HomeComponent z bannerem ostatniej gry
- [ ] Podstawowe serwisy (AuthService, GameService)

### 15.2 Faza 2: Uwierzytelnianie

- [ ] AuthLoginComponent
- [ ] AuthRegisterComponent
- [ ] Integracja z API auth
- [ ] Obsługa sesji gościa

### 15.3 Faza 3: Gry vs bot

- [ ] GameModeSelectionComponent
- [ ] GameComponent
- [ ] GameBoardComponent
- [ ] GameCellComponent
- [ ] Integracja z API gier
- [ ] Animacje ruchów

### 15.4 Faza 4: PvP i WebSocket

- [ ] MatchmakingComponent
- [ ] WebSocketService
- [ ] GameTimerComponent
- [ ] Obsługa reconnect
- [ ] Integracja z WebSocket API

### 15.5 Faza 5: Ranking i profil

- [ ] LeaderboardComponent
- [ ] ProfileComponent
- [ ] RankingService
- [ ] Integracja z API rankingów

### 15.6 Faza 6: Polishing

- [ ] Obsługa błędów
- [ ] Toast notifications
- [ ] Animacje i przejścia
- [ ] Responsywność
- [ ] Dostępność
- [ ] Testy E2E

---

## 16. Uwagi końcowe

### 16.1 Priorytety MVP

1. **Krytyczne**: Uwierzytelnianie, gry vs_bot, podstawowy ranking
2. **Ważne**: PvP z WebSocket, matchmaking, profil użytkownika
3. **Nice to have**: Zaawansowane animacje, optymalizacje wydajności

### 16.2 Rozszerzenia po MVP

- Tryb obserwatora dla gier PvP
- Powiadomienia push
- Funkcje społecznościowe
- Zaawansowane animacje
- Wsparcie mobilne

---

**Dokument utworzony**: 2024-11-XX
**Wersja**: 1.0
**Status**: Planowanie MVP

