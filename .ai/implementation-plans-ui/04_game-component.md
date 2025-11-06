# Plan implementacji: GameComponent

## 1. Przegląd

**Nazwa komponentu**: `GameComponent`  
**Lokalizacja**: `frontend/src/app/features/game/game.component.ts`  
**Ścieżka routingu**: `/game/:gameId`  
**Typ**: Standalone component

## 2. Główny cel

Wyświetlanie planszy gry i umożliwienie użytkownikowi wykonywania ruchów w grze vs bot lub PvP. Komponent obsługuje wszystkie stany gry i integruje się z REST API oraz WebSocket dla gier PvP.

## 3. Funkcjonalności

### 3.1 Wyświetlanie planszy gry
- Dynamiczna plansza (3x3, 4x4, 5x5)
- Wyświetlanie symboli X i O
- Wizualizacja linii wygranej (jeśli gra zakończona)
- Animacje ruchów

### 3.2 Informacje o grze
- Typ gry (vs_bot / PvP)
- Przeciwnik (bot / nazwa gracza)
- Status gry (waiting, in_progress, finished, draw, abandoned)
- Aktualny gracz (symbol X lub O)
- Liczba tur i aktualna tura

### 3.3 Timer (dla PvP)
- Wyświetlanie pozostałego czasu na ruch (10 sekund)
- Wizualne ostrzeżenia (warning, danger)
- Automatyczne zakończenie po timeout

### 3.4 Przycisk poddania (dla PvP)
- Możliwość poddania gry w dowolnym momencie
- Potwierdzenie poddania

### 3.5 Obsługa WebSocket (dla PvP)
- Nawiązanie połączenia WebSocket
- Odbieranie ruchów przeciwnika
- Wysyłanie własnych ruchów
- Obsługa reconnect (max 20 sekund)

### 3.6 Obsługa ruchów bota (dla vs_bot)
- Automatyczny ruch bota po ruchu gracza
- Wskaźnik "Bot myśli..."
- Opóźnienie 200ms przed ruchem bota

## 4. Struktura komponentu

### 4.1 Template

```html
<div class="game-container" *ngIf="game$ | async as game">
  <div class="game-header">
    <h2>{{ getGameTitle(game) }}</h2>
    <p-button
      *ngIf="game.gameType === 'pvp' && game.status === 'in_progress'"
      label="Poddaj się"
      severity="danger"
      (onClick)="onSurrender()">
    </p-button>
  </div>

  <div class="game-info">
    <app-game-info
      [game]="game"
      [currentUser]="currentUser$ | async">
    </app-game-info>

    <app-game-timer
      *ngIf="game.gameType === 'pvp' && game.status === 'in_progress'"
      [remainingSeconds]="timerSeconds$ | async"
      [currentPlayerSymbol]="game.currentPlayerSymbol">
    </app-game-timer>

    <app-game-bot-indicator
      *ngIf="game.gameType === 'vs_bot' && isBotThinking$ | async">
    </app-game-bot-indicator>
  </div>

  <div class="game-board-container">
    <app-game-board
      [boardSize]="game.boardSize"
      [boardState]="game.boardState"
      [currentPlayerSymbol]="game.currentPlayerSymbol"
      [disabled]="isMoveDisabled(game)"
      [gameStatus]="game.status"
      (move)="onMove($event)">
    </app-game-board>
  </div>

  <app-game-result-dialog
    *ngIf="showResultDialog$ | async"
    [game]="game"
    [currentUser]="currentUser$ | async"
    (close)="onResultDialogClose()">
  </app-game-result-dialog>
</div>

<div class="loading-container" *ngIf="isLoading$ | async">
  <p-progressSpinner></p-progressSpinner>
</div>
```

### 4.2 Komponent TypeScript

```typescript
@Component({
  selector: 'app-game',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    AsyncPipe,
    GameBoardComponent,
    GameInfoComponent,
    GameTimerComponent,
    GameBotIndicatorComponent,
    GameResultDialogComponent,
    ButtonModule,
    ProgressSpinnerModule
  ],
  templateUrl: './game.component.html',
  styleUrls: ['./game.component.scss']
})
export class GameComponent implements OnInit, OnDestroy {
  gameId: number | null = null;
  game$ = new BehaviorSubject<Game | null>(null);
  currentUser$ = this.authService.getCurrentUser();
  isLoading$ = new BehaviorSubject<boolean>(true);
  timerSeconds$ = new BehaviorSubject<number>(10);
  isBotThinking$ = new BehaviorSubject<boolean>(false);
  showResultDialog$ = new BehaviorSubject<boolean>(false);
  
  private gameSubscription?: Subscription;
  private timerSubscription?: Subscription;
  private websocketSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private gameService: GameService,
    private websocketService: WebSocketService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.gameId = +params['gameId'];
      if (this.gameId) {
        this.loadGame();
        this.setupGameUpdates();
      }
    });
  }

  ngOnDestroy(): void {
    this.gameSubscription?.unsubscribe();
    this.timerSubscription?.unsubscribe();
    this.websocketSubscription?.unsubscribe();
    this.websocketService.disconnect();
  }

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

  private setupGameUpdates(): void {
    if (!this.gameId) return;

    this.gameSubscription = interval(2000).subscribe(() => {
      this.gameService.getGame(this.gameId!).subscribe({
        next: (game) => {
          const currentGame = this.game$.value;
          if (currentGame && this.hasGameChanged(currentGame, game)) {
            this.game$.next(game);
            this.handleGameStatusChange(game);
          }
        },
        error: (error) => console.error('Error polling game:', error)
      });
    });
  }

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

  private startTimer(): void {
    this.timerSubscription = interval(1000).subscribe(() => {
      const game = this.game$.value;
      if (game && game.gameType === 'pvp' && game.status === 'in_progress' && game.lastMoveAt) {
        const elapsed = (Date.now() - new Date(game.lastMoveAt).getTime()) / 1000;
        const remaining = Math.max(0, 10 - elapsed);
        this.timerSeconds$.next(Math.floor(remaining));
        
        if (remaining <= 0) {
          this.checkTimeout();
        }
      }
    });
  }

  private checkTimeout(): void {
    const game = this.game$.value;
    if (game && game.gameType === 'pvp' && game.status === 'in_progress') {
      this.loadGame();
    }
  }

  onMove(move: { row: number, col: number }): void {
    const game = this.game$.value;
    if (!game || !this.gameId) return;

    if (game.gameType === 'pvp') {
      this.sendMoveViaWebSocket(move);
    } else {
      this.sendMoveViaREST(move);
    }
  }

  private sendMoveViaREST(move: { row: number, col: number }): void {
    const game = this.game$.value;
    if (!game || !this.gameId) return;

    const currentUser = this.currentUser$.pipe(take(1)).subscribe(user => {
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

  private sendMoveViaWebSocket(move: { row: number, col: number }): void {
    const game = this.game$.value;
    if (!game) return;

    const currentUser = this.currentUser$.pipe(take(1)).subscribe(user => {
      if (!user) return;

      const playerSymbol = game.player1Id === user.userId ? 'x' : 'o';
      this.websocketService.sendMove(move.row, move.col, playerSymbol);
    });
  }

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

  private handleMoveResponse(response: MoveResponse): void {
    if (response.gameStatus === 'finished' || response.gameStatus === 'draw') {
      this.showResultDialog$.next(true);
      this.messageService.add({
        severity: response.gameStatus === 'finished' ? 'success' : 'info',
        summary: response.gameStatus === 'finished' ? 'Gra zakończona' : 'Remis',
        detail: response.gameStatus === 'finished' 
          ? `Wygrał: ${response.winner?.username || 'Bot'}`
          : 'Gra zakończona remisem'
      });
    }
  }

  private handleWebSocketMessage(message: WebSocketMessage): void {
    switch (message.type) {
      case 'MOVE_ACCEPTED':
        this.updateGameFromMove(message.payload);
        break;
      case 'MOVE_REJECTED':
        this.messageService.add({
          severity: 'error',
          summary: 'Błąd',
          detail: message.payload.reason || 'Nieprawidłowy ruch'
        });
        break;
      case 'OPPONENT_MOVE':
        this.updateGameFromMove(message.payload);
        break;
      case 'GAME_UPDATE':
        this.updateGameFromPayload(message.payload);
        break;
      case 'TIMER_UPDATE':
        this.timerSeconds$.next(message.payload.remainingSeconds);
        break;
      case 'GAME_ENDED':
        this.updateGameFromPayload(message.payload);
        this.showResultDialog$.next(true);
        break;
    }
  }

  private updateGameFromMove(payload: any): void {
    const game = this.game$.value;
    if (game) {
      this.game$.next({
        ...game,
        boardState: payload.boardState,
        currentPlayerSymbol: payload.currentPlayerSymbol
      });
    }
  }

  private updateGameFromPayload(payload: any): void {
    const game = this.game$.value;
    if (game) {
      this.game$.next({
        ...game,
        status: payload.status,
        winnerId: payload.winner?.userId || null,
        boardState: payload.boardState || payload.finalBoardState
      });
    }
  }

  private handleGameStatusChange(game: Game): void {
    if (game.status === 'finished' || game.status === 'draw' || game.status === 'abandoned') {
      this.showResultDialog$.next(true);
    }
  }

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

  onResultDialogClose(): void {
    this.showResultDialog$.next(false);
    this.router.navigate(['/']);
  }

  isMoveDisabled(game: Game): boolean {
    if (game.status !== 'in_progress') return true;
    
    const currentUser = this.currentUser$.pipe(take(1));
    return currentUser.pipe(
      map(user => {
        if (!user) return true;
        const isCurrentPlayer = (game.player1Id === user.userId && game.currentPlayerSymbol === 'x') ||
                               (game.player2Id === user.userId && game.currentPlayerSymbol === 'o');
        return !isCurrentPlayer;
      })
    ).pipe(take(1)).subscribe();
    
    return false;
  }

  getGameTitle(game: Game): string {
    if (game.gameType === 'vs_bot') {
      return `Gra z botem (${game.botDifficulty})`;
    } else {
      return 'Gra PvP';
    }
  }

  private hasGameChanged(oldGame: Game, newGame: Game): boolean {
    return oldGame.status !== newGame.status ||
           oldGame.currentPlayerSymbol !== newGame.currentPlayerSymbol ||
           JSON.stringify(oldGame.boardState) !== JSON.stringify(newGame.boardState);
  }

  private handleError(error: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd',
      detail: error.error?.message || 'Wystąpił błąd'
    });
  }

  private handleMoveError(error: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd ruchu',
      detail: error.error?.message || 'Nieprawidłowy ruch'
    });
  }

  private handleWebSocketError(error: any): void {
    this.messageService.add({
      severity: 'warn',
      summary: 'Ostrzeżenie',
      detail: 'Problem z połączeniem WebSocket. Próbuję ponownie...'
    });
  }
}
```

## 5. Integracja API

### 5.1 Endpointy REST

- `GET /api/games/{gameId}` - pobranie stanu gry
- `POST /api/games/{gameId}/moves` - wykonanie ruchu
- `PUT /api/games/{gameId}/status` - poddanie gry
- `POST /api/games/{gameId}/bot-move` - automatyczny ruch bota (wewnętrzne)

### 5.2 WebSocket

- `WS /ws/game/{gameId}` - komunikacja real-time (PvP)

**Typy wiadomości**:
- `MOVE` - wysłanie ruchu
- `SURRENDER` - poddanie gry
- `MOVE_ACCEPTED` - ruch zaakceptowany
- `MOVE_REJECTED` - ruch odrzucony
- `OPPONENT_MOVE` - ruch przeciwnika
- `GAME_UPDATE` - aktualizacja stanu gry
- `TIMER_UPDATE` - aktualizacja timera
- `GAME_ENDED` - gra zakończona

### 5.3 Serwisy

- `GameService.getGame(gameId)` - pobranie stanu gry
- `GameService.makeMove(gameId, row, col, playerSymbol)` - wykonanie ruchu
- `GameService.surrenderGame(gameId)` - poddanie gry
- `GameService.makeBotMove(gameId)` - automatyczny ruch bota
- `WebSocketService.connect(gameId, token)` - nawiązanie połączenia WebSocket
- `WebSocketService.sendMove(row, col, playerSymbol)` - wysłanie ruchu przez WebSocket
- `WebSocketService.getMessages()` - odbieranie wiadomości WebSocket

## 6. Stany gry

### 6.1 Statusy

- `waiting` - oczekiwanie na przeciwnika (PvP)
- `in_progress` - gra w toku
- `finished` - gra zakończona (wygrana/przegrana)
- `draw` - remis
- `abandoned` - gra porzucona

### 6.2 Logika biznesowa

1. **Inicjalizacja**:
   - Pobranie stanu gry
   - Nawiązanie połączenia WebSocket (dla PvP)
   - Uruchomienie timera (dla PvP)

2. **Wykonywanie ruchów**:
   - Walidacja ruchu po stronie klienta
   - Wysłanie ruchu przez REST (vs_bot) lub WebSocket (PvP)
   - Aktualizacja stanu gry
   - Automatyczny ruch bota (dla vs_bot)

3. **Obsługa timeout**:
   - Sprawdzenie czasu ostatniego ruchu
   - Automatyczne zakończenie po 20 sekundach nieaktywności (PvP)

4. **Zakończenie gry**:
   - Wyświetlenie dialogu z wynikiem
   - Przekierowanie do home

## 7. Komponenty współdzielone

### 7.1 GameBoardComponent

**Lokalizacja**: `components/game/game-board.component.ts`

**Funkcjonalność**:
- Renderowanie planszy gry
- Obsługa kliknięć na komórki
- Animacje ruchów

### 7.2 GameInfoComponent

**Lokalizacja**: `components/game/game-info.component.ts`

**Funkcjonalność**:
- Wyświetlanie informacji o grze
- Przeciwnik, status, tury

### 7.3 GameTimerComponent

**Lokalizacja**: `components/game/game-timer.component.ts`

**Funkcjonalność**:
- Wyświetlanie pozostałego czasu na ruch
- Wizualne ostrzeżenia

### 7.4 GameBotIndicatorComponent

**Lokalizacja**: `components/game/game-bot-indicator.component.ts`

**Funkcjonalność**:
- Wyświetlanie wskaźnika "Bot myśli..."

### 7.5 GameResultDialogComponent

**Lokalizacja**: `components/game/game-result-dialog.component.ts`

**Funkcjonalność**:
- Wyświetlanie wyniku gry
- Informacje o zdobytych punktach

## 8. Animacje

- Pojawienie się symbolu: scale (0 → 1) + fade-in, 300ms
- Ruch bota: opóźnienie 200ms + animacja symbolu
- Linia wygranej: stroke-dasharray animation, 500ms opóźnienie

## 9. Obsługa błędów

- Błąd pobierania gry: przekierowanie do 404
- Błąd ruchu: toast notification z komunikatem
- Błąd WebSocket: automatyczna próba reconnect (max 20 sekund)
- Timeout: automatyczne zakończenie gry

## 10. Testy

### 10.1 Testy jednostkowe

- Sprawdzenie wyświetlania planszy
- Sprawdzenie wykonywania ruchów
- Sprawdzenie obsługi WebSocket
- Sprawdzenie obsługi ruchów bota

### 10.2 Testy E2E (Cypress)

- Scenariusz: Gra vs bot (łatwy poziom)
- Scenariusz: Gra vs bot (średni poziom)
- Scenariusz: Gra vs bot (trudny poziom)
- Scenariusz: Gra PvP z WebSocket
- Scenariusz: Poddanie gry
- Scenariusz: Timeout w grze PvP

## 11. Dostępność

- ARIA labels dla komórek planszy
- Keyboard navigation (opcjonalne)
- Focus indicators
- Screen reader support dla statusu gry

## 12. Wsparcie dla wielu języków (i18n)

### 12.1 Implementacja

Komponent wykorzystuje Angular i18n do obsługi wielu języków. Wszystkie teksty w komponencie są tłumaczone:
- Tytuły gier ("Gra z botem", "Gra PvP")
- Komunikaty o statusie gry ("Oczekiwanie na przeciwnika", "Gra w toku", "Gra zakończona")
- Komunikaty błędów ruchów
- Komunikaty WebSocket
- Komunikaty o wyniku gry ("Wygrana", "Przegrana", "Remis")

### 12.2 Języki wspierane

- **Angielski (en)** - język podstawowy, domyślny
- **Polski (pl)** - język dodatkowy

### 12.3 Użycie

Wszystkie teksty w template są opakowane w pipe `i18n` lub używają serwisu `TranslateService`:
```typescript
{{ 'game.title' | translate }}
{{ 'game.status.inProgress' | translate }}
{{ 'game.result.win' | translate }}
```

### 12.4 Backend

Backend pozostaje bez zmian - wszystkie odpowiedzi API są w języku angielskim. Tłumaczenie komunikatów z backendu i WebSocket na język użytkownika odbywa się po stronie frontendu.

## 13. Mapowanie historyjek użytkownika

- **US-004**: Rozgrywka z botem (łatwy poziom)
- **US-005**: Rozgrywka z botem (średni poziom)
- **US-006**: Rozgrywka z botem (trudny poziom)
- **US-007**: Dołączenie do gry PvP
- **US-008**: Rozgrywka PvP z funkcjonalnościami
- **US-013**: Obsługa rozłączeń w PvP
- **US-014**: Walidacja ruchów w grze

