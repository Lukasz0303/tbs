# Plan implementacji: ProfileComponent

## 1. Przegląd

**Nazwa komponentu**: `ProfileComponent`  
**Lokalizacja**: `frontend/src/app/features/profile/profile.component.ts`  
**Ścieżka routingu**: `/profile`  
**Typ**: Standalone component

## 2. Główny cel

Wyświetlanie i zarządzanie profilem użytkownika z podstawowymi informacjami, statystykami i ostatnią grą. Komponent obsługuje zarówno gości, jak i zarejestrowanych użytkowników.

## 3. Funkcjonalności

### 3.1 Wyświetlanie profilu
- Podstawowe informacje (nazwa użytkownika, email)
- Status (gość/zarejestrowany)
- Statystyki (punkty, rozegrane gry, wygrane)
- Pozycja w rankingu (jeśli zarejestrowany)

### 3.2 Ostatnia zapisana gra
- Karta z ostatnią grą (jeśli istnieje)
- Możliwość kontynuacji gry
- Informacje o grze (typ, przeciwnik, status)

### 3.3 Edycja profilu
- Możliwość edycji nazwy użytkownika (tylko zarejestrowani)
- Dialog edycji nazwy
- Walidacja nazwy użytkownika

### 3.4 Zachęta do rejestracji
- Dla gości: zachęta do rejestracji
- Link do rejestracji

## 4. Struktura komponentu

### 4.1 Template

```html
<div class="profile-container">
  <div class="profile-header">
    <h2>Profil użytkownika</h2>
  </div>

  <div class="profile-content" *ngIf="currentUser$ | async as user">
    <div class="profile-info-card">
      <h3>Podstawowe informacje</h3>
      <div class="info-item">
        <label>Nazwa użytkownika:</label>
        <span *ngIf="user.username">{{ user.username }}</span>
        <span *ngIf="!user.username">Gość</span>
        <p-button
          *ngIf="!user.isGuest"
          label="Edytuj"
          size="small"
          (onClick)="onEditUsername()">
        </p-button>
      </div>
      <div class="info-item" *ngIf="!user.isGuest">
        <label>Email:</label>
        <span>{{ user.email }}</span>
      </div>
      <div class="info-item">
        <label>Status:</label>
        <span>{{ user.isGuest ? 'Gość' : 'Zarejestrowany' }}</span>
      </div>
    </div>

    <div class="profile-stats-card">
      <h3>Statystyki</h3>
      <app-user-stats [user]="user" [ranking]="userRanking$ | async">
      </app-user-stats>
    </div>

    <app-last-game-card
      *ngIf="lastGame$ | async as game"
      [game]="game"
      (continueGame)="onContinueGame($event)">
    </app-last-game-card>

    <div class="profile-actions" *ngIf="user.isGuest">
      <div class="registration-encouragement">
        <h3>Chcesz śledzić swoje postępy?</h3>
        <p>Zarejestruj się, aby mieć stały dostęp do profilu i historii gier</p>
        <p-button
          label="Zarejestruj się"
          (onClick)="navigateToRegister()">
        </p-button>
      </div>
    </div>
  </div>

  <app-edit-username-dialog
    *ngIf="showEditDialog$ | async"
    [currentUsername]="(currentUser$ | async)?.username || ''"
    (close)="onCloseEditDialog()"
    (save)="onSaveUsername($event)">
  </app-edit-username-dialog>
</div>
```

### 4.2 Komponent TypeScript

```typescript
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    AsyncPipe,
    ButtonModule,
    UserStatsComponent,
    LastGameCardComponent,
    EditUsernameDialogComponent
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  currentUser$ = this.authService.getCurrentUser();
  userRanking$ = new BehaviorSubject<Ranking | null>(null);
  lastGame$ = new BehaviorSubject<Game | null>(null);
  showEditDialog$ = new BehaviorSubject<boolean>(false);

  constructor(
    private authService: AuthService,
    private rankingService: RankingService,
    private gameService: GameService,
    private userService: UserService,
    private router: Router,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.loadUserRanking();
    this.loadLastGame();
  }

  private loadUserRanking(): void {
    this.currentUser$.pipe(
      take(1),
      filter(user => user !== null && !user.isGuest),
      switchMap(user => this.rankingService.getUserRanking(user!.userId))
    ).subscribe({
      next: (ranking) => this.userRanking$.next(ranking),
      error: (error) => console.error('Error loading user ranking:', error)
    });
  }

  private loadLastGame(): void {
    this.gameService.getSavedGame().subscribe({
      next: (game) => this.lastGame$.next(game),
      error: (error) => console.error('Error loading last game:', error)
    });
  }

  onEditUsername(): void {
    this.showEditDialog$.next(true);
  }

  onCloseEditDialog(): void {
    this.showEditDialog$.next(false);
  }

  onSaveUsername(newUsername: string): void {
    this.currentUser$.pipe(
      take(1),
      filter(user => user !== null && !user.isGuest),
      switchMap(user => this.userService.updateUser(user!.userId, { username: newUsername }))
    ).subscribe({
      next: (updatedUser) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Sukces',
          detail: 'Nazwa użytkownika została zaktualizowana'
        });
        this.authService.updateCurrentUser(updatedUser);
        this.showEditDialog$.next(false);
      },
      error: (error) => {
        this.handleUpdateError(error);
      }
    });
  }

  onContinueGame(gameId: number): void {
    this.router.navigate(['/game', gameId]);
  }

  navigateToRegister(): void {
    this.router.navigate(['/auth/register']);
  }

  private handleUpdateError(error: any): void {
    let message = 'Nie udało się zaktualizować nazwy użytkownika';
    
    if (error.status === 409) {
      message = 'Nazwa użytkownika już istnieje';
    } else if (error.status === 403) {
      message = 'Nie masz uprawnień do aktualizacji tego profilu';
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

- `GET /api/auth/me` - profil użytkownika
- `GET /api/rankings/{userId}` - pozycja w rankingu (jeśli zarejestrowany)
- `GET /api/games?status=in_progress&size=1` - ostatnia gra
- `PUT /api/v1/users/{userId}` - aktualizacja nazwy użytkownika

**Request body** (PUT /api/v1/users/{userId}):
```json
{
  "username": "string"
}
```

**Response (200 OK)**:
```json
{
  "userId": 1,
  "username": "newusername",
  "isGuest": false,
  "totalPoints": 1000,
  "gamesPlayed": 10,
  "gamesWon": 5,
  "updatedAt": "2024-11-06T12:00:00Z"
}
```

### 5.2 Serwisy

- `AuthService.getCurrentUser()` - pobranie aktualnego użytkownika
- `RankingService.getUserRanking(userId)` - pobranie pozycji w rankingu
- `GameService.getSavedGame()` - pobranie ostatniej gry
- `UserService.updateUser(userId, data)` - aktualizacja użytkownika

## 6. Komponenty współdzielone

### 6.1 UserStatsComponent

**Lokalizacja**: `components/profile/user-stats.component.ts`

**Funkcjonalność**:
- Wyświetlanie statystyk użytkownika
- Punkty, rozegrane gry, wygrane
- Pozycja w rankingu (jeśli zarejestrowany)

**Inputs**:
- `user: User` - użytkownik
- `ranking: Ranking | null` - pozycja w rankingu

### 6.2 LastGameCardComponent

**Lokalizacja**: `components/profile/last-game-card.component.ts`

**Funkcjonalność**:
- Karta z ostatnią grą
- Informacje o grze (typ, przeciwnik, status)
- Przycisk kontynuacji gry

**Inputs**:
- `game: Game` - ostatnia gra

**Outputs**:
- `continueGame: EventEmitter<number>` - emisja gameId

### 6.3 EditUsernameDialogComponent

**Lokalizacja**: `components/profile/edit-username-dialog.component.ts`

**Funkcjonalność**:
- Dialog edycji nazwy użytkownika
- Walidacja nazwy użytkownika
- Zapisanie zmian

**Inputs**:
- `currentUsername: string` - aktualna nazwa użytkownika

**Outputs**:
- `close: EventEmitter<void>` - zamknięcie dialogu
- `save: EventEmitter<string>` - zapisanie nowej nazwy

## 7. Stylowanie

### 7.1 SCSS

```scss
.profile-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;

  .profile-header {
    text-align: center;
    margin-bottom: 2rem;

    h2 {
      font-size: 2rem;
    }
  }

  .profile-content {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 2rem;

    .profile-info-card,
    .profile-stats-card {
      padding: 2rem;
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

      h3 {
        margin-bottom: 1.5rem;
      }

      .info-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;

        label {
          font-weight: 500;
          margin-right: 1rem;
        }
      }
    }

    .registration-encouragement {
      padding: 2rem;
      background: #f5f5f5;
      border-radius: 8px;
      text-align: center;

      h3 {
        margin-bottom: 1rem;
      }

      p {
        margin-bottom: 1rem;
        color: #666;
      }
    }
  }
}
```

## 8. Animacje

- Fade-in dla kart profilu
- Smooth transitions dla przycisków
- Slide-in dla dialogu edycji

## 9. Obsługa błędów

- Błąd pobierania profilu: toast notification, możliwość ponowienia
- Błąd aktualizacji nazwy: toast notification z konkretnym komunikatem
- Błąd pobierania ostatniej gry: ciche logowanie, brak wyświetlania karty

## 10. Testy

### 10.1 Testy jednostkowe

- Sprawdzenie wyświetlania profilu
- Sprawdzenie edycji nazwy użytkownika
- Sprawdzenie kontynuacji gry
- Sprawdzenie zachęty do rejestracji (dla gości)

### 10.2 Testy E2E (Cypress)

- Scenariusz: Przeglądanie profilu (zarejestrowany)
- Scenariusz: Edycja nazwy użytkownika
- Scenariusz: Kontynuacja zapisanej gry
- Scenariusz: Profil gościa

## 11. Dostępność

- ARIA labels dla wszystkich elementów
- Keyboard navigation
- Screen reader support dla statystyk
- Focus indicators

## 12. Wsparcie dla wielu języków (i18n)

### 12.1 Implementacja

Komponent wykorzystuje Angular i18n do obsługi wielu języków. Wszystkie teksty w komponencie są tłumaczone:
- Nagłówki ("Profil użytkownika", "Podstawowe informacje", "Statystyki")
- Etykiety pól ("Nazwa użytkownika", "Email", "Status")
- Komunikaty zachęty do rejestracji ("Chcesz śledzić swoje postępy?", "Zarejestruj się")
- Komunikaty błędów

### 12.2 Języki wspierane

- **Angielski (en)** - język podstawowy, domyślny
- **Polski (pl)** - język dodatkowy

### 12.3 Użycie

Wszystkie teksty w template są opakowane w pipe `i18n` lub używają serwisu `TranslateService`:
```typescript
{{ 'profile.title' | translate }}
{{ 'profile.username' | translate }}
{{ 'profile.email' | translate }}
{{ 'profile.registerEncouragement' | translate }}
```

### 12.4 Backend

Backend pozostaje bez zmian - wszystkie odpowiedzi API są w języku angielskim. Tłumaczenie komunikatów z backendu na język użytkownika odbywa się po stronie frontendu.

## 13. Mapowanie historyjek użytkownika

- **US-011**: Zarządzanie profilem gracza
- **US-012**: Automatyczne zapisywanie gier - kontynuacja ostatniej gry

