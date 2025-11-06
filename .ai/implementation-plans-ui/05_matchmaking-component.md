# Plan implementacji: MatchmakingComponent

## 1. Przegląd

**Nazwa komponentu**: `MatchmakingComponent`  
**Lokalizacja**: `frontend/src/app/features/game/matchmaking.component.ts`  
**Ścieżka routingu**: `/game/matchmaking`  
**Typ**: Standalone component

## 2. Główny cel

Wyświetlanie stanu oczekiwania na przeciwnika w kolejce matchmakingu i umożliwienie anulowania kolejki. Komponent automatycznie dołącza do kolejki przy inicjalizacji i przekierowuje do gry po znalezieniu przeciwnika.

## 3. Funkcjonalności

### 3.1 Dołączenie do kolejki
- Automatyczne dołączenie do kolejki przy inicjalizacji
- Wybór rozmiaru planszy (3x3, 4x4, 5x5)
- Szacowany czas oczekiwania

### 3.2 Wyświetlanie statusu
- Animacja ładowania
- Wskaźnik postępu
- Informacja o rozmiarze planszy
- Szacowany czas oczekiwania

### 3.3 Anulowanie kolejki
- Przycisk anulowania kolejki
- Opuszczenie kolejki i przekierowanie do home

### 3.4 Znalezienie przeciwnika
- Automatyczne przekierowanie do GameComponent z gameId
- Nawiązanie połączenia WebSocket

## 4. Struktura komponentu

### 4.1 Template

```html
<div class="matchmaking-container">
  <div class="matchmaking-card">
    <h2>Szukanie przeciwnika...</h2>
    
    <div class="matchmaking-info">
      <p>Rozmiar planszy: {{ boardSize }}x{{ boardSize }}</p>
      <p *ngIf="estimatedWaitTime$ | async as waitTime">
        Szacowany czas oczekiwania: {{ waitTime }} sekund
      </p>
    </div>

    <div class="matchmaking-animation">
      <p-progressSpinner [style]="{ width: '100px', height: '100px' }"></p-progressSpinner>
      <p class="status-text">{{ statusText$ | async }}</p>
    </div>

    <div class="matchmaking-actions">
      <p-button
        label="Anuluj"
        severity="secondary"
        (onClick)="onCancel()"
        [disabled]="isCancelling$ | async">
      </p-button>
    </div>
  </div>
</div>
```

### 4.2 Komponent TypeScript

```typescript
@Component({
  selector: 'app-matchmaking',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    AsyncPipe,
    ButtonModule,
    ProgressSpinnerModule
  ],
  templateUrl: './matchmaking.component.html',
  styleUrls: ['./matchmaking.component.scss']
})
export class MatchmakingComponent implements OnInit, OnDestroy {
  boardSize: 3 | 4 | 5 = 3;
  estimatedWaitTime$ = new BehaviorSubject<number>(0);
  statusText$ = new BehaviorSubject<string>('Szukanie przeciwnika...');
  isCancelling$ = new BehaviorSubject<boolean>(false);
  
  private matchmakingSubscription?: Subscription;
  private pollingSubscription?: Subscription;
  private waitTimeSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private matchmakingService: MatchmakingService,
    private gameService: GameService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.boardSize = +(params['boardSize'] || 3) as 3 | 4 | 5;
      this.joinQueue();
    });
  }

  ngOnDestroy(): void {
    this.matchmakingSubscription?.unsubscribe();
    this.pollingSubscription?.unsubscribe();
    this.waitTimeSubscription?.unsubscribe();
  }

  private joinQueue(): void {
    this.matchmakingService.joinQueue(this.boardSize).subscribe({
      next: (response) => {
        this.estimatedWaitTime$.next(response.estimatedWaitTime || 0);
        this.startPolling();
        this.startWaitTimeCounter();
      },
      error: (error) => {
        this.handleError(error);
        this.router.navigate(['/']);
      }
    });
  }

  private startPolling(): void {
    this.pollingSubscription = interval(2000).subscribe(() => {
      this.checkMatchmakingStatus();
    });
  }

  private checkMatchmakingStatus(): void {
    this.matchmakingService.getMatchmakingStatus().subscribe({
      next: (status) => {
        if (status.gameId) {
          this.router.navigate(['/game', status.gameId]);
        }
      },
      error: (error) => {
        console.error('Error checking matchmaking status:', error);
      }
    });
  }

  private startWaitTimeCounter(): void {
    this.waitTimeSubscription = interval(1000).subscribe(() => {
      const currentWaitTime = this.estimatedWaitTime$.value;
      if (currentWaitTime > 0) {
        this.estimatedWaitTime$.next(currentWaitTime - 1);
      } else {
        this.statusText$.next('Szukanie przeciwnika...');
      }
    });
  }

  onCancel(): void {
    this.isCancelling$.next(true);
    
    this.matchmakingService.leaveQueue().subscribe({
      next: () => {
        this.messageService.add({
          severity: 'info',
          summary: 'Anulowano',
          detail: 'Opuszczono kolejkę matchmakingu'
        });
        this.router.navigate(['/']);
      },
      error: (error) => {
        this.isCancelling$.next(false);
        this.handleError(error);
      }
    });
  }

  private handleError(error: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd',
      detail: error.error?.message || 'Wystąpił błąd podczas matchmakingu'
    });
  }
}
```

## 5. Integracja API

### 5.1 Endpointy

- `POST /api/v1/matching/queue` - dołączenie do kolejki matchmakingu
- `DELETE /api/v1/matching/queue` - opuszczenie kolejki matchmakingu
- `GET /api/games/{gameId}` - sprawdzenie statusu matchmakingu przez polling (alternatywa dla WebSocket notification)

**Request body** (POST /api/v1/matching/queue):
```json
{
  "boardSize": 3 | 4 | 5
}
```

**Response (200 OK)**:
```json
{
  "message": "Zakolejkowano do matchmakingu",
  "estimatedWaitTime": 30
}
```

### 5.2 Serwisy

- `MatchmakingService.joinQueue(boardSize)` - dołączenie do kolejki
- `MatchmakingService.leaveQueue()` - opuszczenie kolejki
- `GameService.getGame(gameId)` - sprawdzenie statusu matchmakingu przez polling (alternatywa dla WebSocket notification)

## 6. Stany i logika biznesowa

### 6.1 Stany komponentu

- `boardSize` - rozmiar planszy (3, 4, 5)
- `estimatedWaitTime$` - szacowany czas oczekiwania (sekundy)
- `statusText$` - tekst statusu
- `isCancelling$` - stan anulowania

### 6.2 Logika biznesowa

1. **Inicjalizacja**:
   - Pobranie rozmiaru planszy z query params
   - Automatyczne dołączenie do kolejki

2. **Polling**:
   - Sprawdzanie statusu matchmakingu co 2 sekundy
   - Przekierowanie do gry po znalezieniu przeciwnika

3. **Anulowanie**:
   - Opuszczenie kolejki
   - Przekierowanie do home

## 7. Stylowanie

### 7.1 SCSS

```scss
.matchmaking-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: 2rem;

  .matchmaking-card {
    width: 100%;
    max-width: 500px;
    padding: 3rem;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    text-align: center;

    h2 {
      margin-bottom: 2rem;
    }

    .matchmaking-info {
      margin-bottom: 2rem;

      p {
        margin: 0.5rem 0;
        font-size: 1.1rem;
      }
    }

    .matchmaking-animation {
      margin: 2rem 0;

      .status-text {
        margin-top: 1rem;
        font-size: 1rem;
        color: #666;
      }
    }

    .matchmaking-actions {
      margin-top: 2rem;
    }
  }
}
```

## 8. Animacje

- Rotating spinner dla animacji ładowania
- Pulse animation dla tekstu statusu
- Smooth transitions dla przycisków

## 9. Obsługa błędów

- Błąd dołączenia do kolejki: toast notification, przekierowanie do home
- Błąd anulowania: toast notification, możliwość ponowienia
- Timeout matchmakingu: komunikat o braku przeciwników, możliwość anulowania

## 10. Testy

### 10.1 Testy jednostkowe

- Sprawdzenie dołączenia do kolejki
- Sprawdzenie anulowania kolejki
- Sprawdzenie przekierowania po znalezieniu przeciwnika

### 10.2 Testy E2E (Cypress)

- Scenariusz: Dołączenie do matchmakingu
- Scenariusz: Anulowanie matchmakingu
- Scenariusz: Znalezienie przeciwnika i przekierowanie do gry

## 11. Dostępność

- ARIA labels dla wszystkich elementów
- Screen reader announcements dla zmian statusu
- Keyboard navigation
- Focus indicators

## 12. Wsparcie dla wielu języków (i18n)

### 12.1 Implementacja

Komponent wykorzystuje Angular i18n do obsługi wielu języków. Wszystkie teksty w komponencie są tłumaczone:
- Nagłówki ("Szukanie przeciwnika...")
- Komunikaty statusu ("Szukanie przeciwnika...", "Znaleziono przeciwnika")
- Informacje o rozmiarze planszy
- Komunikaty błędów

### 12.2 Języki wspierane

- **Angielski (en)** - język podstawowy, domyślny
- **Polski (pl)** - język dodatkowy

### 12.3 Użycie

Wszystkie teksty w template są opakowane w pipe `i18n` lub używają serwisu `TranslateService`:
```typescript
{{ 'matchmaking.title' | translate }}
{{ 'matchmaking.searching' | translate }}
{{ 'matchmaking.boardSize' | translate }}
```

### 12.4 Backend

Backend pozostaje bez zmian - wszystkie odpowiedzi API są w języku angielskim. Tłumaczenie komunikatów z backendu na język użytkownika odbywa się po stronie frontendu.

## 13. Mapowanie historyjek użytkownika

- **US-007**: Dołączenie do gry PvP

