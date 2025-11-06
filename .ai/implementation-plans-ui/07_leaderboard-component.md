# Plan implementacji: LeaderboardComponent

## 1. Przegląd

**Nazwa komponentu**: `LeaderboardComponent`  
**Lokalizacja**: `frontend/src/app/features/leaderboard/leaderboard.component.ts`  
**Ścieżka routingu**: `/leaderboard`  
**Typ**: Standalone component

## 2. Główny cel

Wyświetlanie globalnego rankingu graczy z możliwością przeglądania pozycji i wyboru przeciwnika. Komponent obsługuje paginację i umożliwia wyzwanie gracza do gry.

## 3. Funkcjonalności

### 3.1 Wyświetlanie rankingu
- Tabela z rankingiem (paginacja)
- Pozycja w rankingu
- Nazwa użytkownika
- Punkty
- Rozegrane gry
- Wygrane gry

### 3.2 Pozycja użytkownika
- Wyróżniona karta z pozycją użytkownika (jeśli zarejestrowany)
- Wyświetlanie tylko dla zarejestrowanych użytkowników

### 3.3 Gracze wokół użytkownika
- Przycisk "Pokaż graczy wokół mnie" (dla zarejestrowanych)
- Dialog z graczami wokół użytkownika
- Możliwość wyzwania gracza z dialogu

### 3.4 Wyzwanie gracza
- Kliknięcie na gracza w rankingu
- Sprawdzenie dostępności gracza
- Wyzwanie do gry
- Przekierowanie do GameComponent

## 4. Struktura komponentu

### 4.1 Template

```html
<div class="leaderboard-container">
  <div class="leaderboard-header">
    <h2>Ranking graczy</h2>
    <p>Globalny ranking najlepszych graczy</p>
  </div>

  <app-user-rank-card
    *ngIf="userRanking$ | async as userRanking"
    [ranking]="userRanking"
    (showPlayersAround)="onShowPlayersAround()">
  </app-user-rank-card>

  <div class="leaderboard-table-container">
    <p-table
      [value]="rankings$ | async"
      [paginator]="true"
      [rows]="pageSize"
      [totalRecords]="totalRecords$ | async"
      [lazy]="true"
      (onLazyLoad)="onLazyLoad($event)"
      [loading]="isLoading$ | async">
      
      <ng-template pTemplate="header">
        <tr>
          <th>Pozycja</th>
          <th>Nazwa użytkownika</th>
          <th>Punkty</th>
          <th>Rozegrane gry</th>
          <th>Wygrane gry</th>
          <th>Akcje</th>
        </tr>
      </ng-template>

      <ng-template pTemplate="body" let-ranking>
        <tr [class.current-user]="isCurrentUser(ranking.userId)">
          <td>{{ ranking.rankPosition }}</td>
          <td>{{ ranking.username }}</td>
          <td>{{ ranking.totalPoints }}</td>
          <td>{{ ranking.gamesPlayed }}</td>
          <td>{{ ranking.gamesWon }}</td>
          <td>
            <p-button
              label="Wyzwij"
              size="small"
              (onClick)="onChallengePlayer(ranking.userId)"
              [disabled]="isCurrentUser(ranking.userId) || isChallenging">
            </p-button>
          </td>
        </tr>
      </ng-template>
    </p-table>
  </div>

  <app-players-around-dialog
    *ngIf="showPlayersAroundDialog$ | async"
    [userId]="currentUserId"
    (close)="onClosePlayersAroundDialog()"
    (challenge)="onChallengePlayer($event)">
  </app-players-around-dialog>
</div>
```

### 4.2 Komponent TypeScript

```typescript
@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    AsyncPipe,
    TableModule,
    ButtonModule,
    PaginatorModule,
    UserRankCardComponent,
    PlayersAroundDialogComponent
  ],
  templateUrl: './leaderboard.component.html',
  styleUrls: ['./leaderboard.component.scss']
})
export class LeaderboardComponent implements OnInit {
  rankings$ = new BehaviorSubject<Ranking[]>([]);
  userRanking$ = new BehaviorSubject<Ranking | null>(null);
  totalRecords$ = new BehaviorSubject<number>(0);
  isLoading$ = new BehaviorSubject<boolean>(true);
  showPlayersAroundDialog$ = new BehaviorSubject<boolean>(false);
  isChallenging = false;
  currentUserId: number | null = null;
  pageSize = 50;

  constructor(
    private rankingService: RankingService,
    private matchmakingService: MatchmakingService,
    private authService: AuthService,
    private router: Router,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.loadUserRanking();
    this.loadRanking(0, this.pageSize);
  }

  private loadUserRanking(): void {
    this.authService.getCurrentUser().pipe(
      take(1),
      filter(user => user !== null && !user.isGuest),
      switchMap(user => this.rankingService.getUserRanking(user!.userId))
    ).subscribe({
      next: (ranking) => {
        this.userRanking$.next(ranking);
        this.currentUserId = ranking.userId;
      },
      error: (error) => console.error('Error loading user ranking:', error)
    });
  }

  private loadRanking(page: number, size: number): void {
    this.isLoading$.next(true);
    
    this.rankingService.getRanking(page, size).subscribe({
      next: (response) => {
        this.rankings$.next(response.content);
        this.totalRecords$.next(response.totalElements);
        this.isLoading$.next(false);
      },
      error: (error) => {
        this.isLoading$.next(false);
        this.handleError(error);
      }
    });
  }

  onLazyLoad(event: any): void {
    const page = event.first / event.rows;
    this.loadRanking(page, event.rows);
  }

  onChallengePlayer(userId: number): void {
    if (this.isChallenging) return;

    this.isChallenging = true;
    
    this.matchmakingService.challengePlayer(userId, 3).subscribe({
      next: (game) => {
        this.isChallenging = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Sukces',
          detail: 'Wyzwanie zostało wysłane'
        });
        this.router.navigate(['/game', game.gameId]);
      },
      error: (error) => {
        this.isChallenging = false;
        this.handleChallengeError(error);
      }
    });
  }

  onShowPlayersAround(): void {
    this.showPlayersAroundDialog$.next(true);
  }

  onClosePlayersAroundDialog(): void {
    this.showPlayersAroundDialog$.next(false);
  }

  isCurrentUser(userId: number): boolean {
    return this.currentUserId === userId;
  }

  private handleError(error: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd',
      detail: error.error?.message || 'Nie udało się załadować rankingu'
    });
  }

  private handleChallengeError(error: any): void {
    let message = 'Nie udało się wyzwać gracza';
    
    if (error.status === 404) {
      message = 'Gracz nie został znaleziony';
    } else if (error.status === 409) {
      message = 'Gracz jest niedostępny lub już w grze';
    }

    this.messageService.add({
      severity: 'error',
      summary: 'Błąd',
      detail: message
    });
  }
}
```

## 5. Integracja API

### 5.1 Endpointy

- `GET /api/rankings` - lista graczy (paginacja)
- `GET /api/rankings/{userId}` - pozycja użytkownika
- `GET /api/rankings/around/{userId}` - gracze wokół użytkownika
- `POST /api/v1/matching/challenge/{userId}` - wyzwanie gracza do gry

**Request params** (GET /api/rankings):
- `page`: Numer strony (domyślnie: 0)
- `size`: Rozmiar strony (domyślnie: 50, maks: 100)

**Response (200 OK)**:
```json
{
  "content": [
    {
      "rankPosition": 1,
      "userId": 1,
      "username": "player1",
      "totalPoints": 5000,
      "gamesPlayed": 25,
      "gamesWon": 15,
      "createdAt": "2024-11-06T12:00:00Z"
    }
  ],
  "totalElements": 100,
  "totalPages": 2,
  "size": 50,
  "number": 0
}
```

### 5.2 Serwisy

- `RankingService.getRanking(page, size)` - pobranie rankingu
- `RankingService.getUserRanking(userId)` - pobranie pozycji użytkownika
- `RankingService.getPlayersAround(userId, range)` - pobranie graczy wokół użytkownika
- `MatchmakingService.challengePlayer(userId, boardSize)` - wyzwanie gracza do gry

## 6. Komponenty współdzielone

### 6.1 UserRankCardComponent

**Lokalizacja**: `components/leaderboard/user-rank-card.component.ts`

**Funkcjonalność**:
- Karta z pozycją użytkownika
- Statystyki użytkownika
- Przycisk "Pokaż graczy wokół mnie"

**Inputs**:
- `ranking: Ranking` - pozycja użytkownika

**Outputs**:
- `showPlayersAround: EventEmitter<void>` - emisja żądania pokazania graczy wokół

### 6.2 PlayersAroundDialogComponent

**Lokalizacja**: `components/leaderboard/players-around-dialog.component.ts`

**Funkcjonalność**:
- Dialog z graczami wokół użytkownika
- Lista graczy przed i po użytkowniku
- Możliwość wyzwania gracza

**Inputs**:
- `userId: number` - ID użytkownika

**Outputs**:
- `close: EventEmitter<void>` - zamknięcie dialogu
- `challenge: EventEmitter<number>` - wyzwanie gracza

## 7. Stylowanie

### 7.1 SCSS

```scss
.leaderboard-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;

  .leaderboard-header {
    text-align: center;
    margin-bottom: 2rem;

    h2 {
      font-size: 2rem;
      margin-bottom: 0.5rem;
    }

    p {
      font-size: 1.1rem;
      color: #666;
    }
  }

  .leaderboard-table-container {
    margin-top: 2rem;

    ::ng-deep .p-datatable {
      .current-user {
        background-color: #e3f2fd;
        font-weight: 500;
      }
    }
  }
}
```

## 8. Animacje

- Fade-in dla tabeli rankingu
- Smooth transitions dla przycisków
- Slide-in dla dialogu z graczami wokół

## 9. Obsługa błędów

- Błąd pobierania rankingu: toast notification, możliwość ponowienia
- Błąd wyzwania gracza: toast notification z konkretnym komunikatem
- Błąd pobierania pozycji użytkownika: ciche logowanie, brak wyświetlania karty

## 10. Testy

### 10.1 Testy jednostkowe

- Sprawdzenie wyświetlania rankingu
- Sprawdzenie paginacji
- Sprawdzenie wyzwania gracza
- Sprawdzenie wyświetlania graczy wokół użytkownika

### 10.2 Testy E2E (Cypress)

- Scenariusz: Przeglądanie rankingu
- Scenariusz: Wybór przeciwnika z rankingu
- Scenariusz: Wyświetlanie graczy wokół użytkownika

## 11. Dostępność

- ARIA labels dla tabeli
- Keyboard navigation
- Screen reader support dla rankingu
- Focus indicators

## 12. Wsparcie dla wielu języków (i18n)

### 12.1 Implementacja

Komponent wykorzystuje Angular i18n do obsługi wielu języków. Wszystkie teksty w komponencie są tłumaczone:
- Nagłówki ("Ranking graczy", "Globalny ranking najlepszych graczy")
- Nagłówki kolumn tabeli ("Pozycja", "Nazwa użytkownika", "Punkty", "Rozegrane gry", "Wygrane gry", "Akcje")
- Przyciski ("Wyzwij", "Pokaż graczy wokół mnie")
- Komunikaty błędów

### 12.2 Języki wspierane

- **Angielski (en)** - język podstawowy, domyślny
- **Polski (pl)** - język dodatkowy

### 12.3 Użycie

Wszystkie teksty w template są opakowane w pipe `i18n` lub używają serwisu `TranslateService`:
```typescript
{{ 'leaderboard.title' | translate }}
{{ 'leaderboard.position' | translate }}
{{ 'leaderboard.challenge' | translate }}
```

### 12.4 Backend

Backend pozostaje bez zmian - wszystkie odpowiedzi API są w języku angielskim. Tłumaczenie komunikatów z backendu na język użytkownika odbywa się po stronie frontendu.

## 13. Mapowanie historyjek użytkownika

- **US-009**: Przeglądanie rankingu graczy
- **US-010**: Wybór przeciwnika z rankingu

