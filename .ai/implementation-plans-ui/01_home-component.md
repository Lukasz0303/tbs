# Plan implementacji: HomeComponent

## 1. Przegląd

**Nazwa komponentu**: `HomeComponent`  
**Lokalizacja**: `frontend/src/app/features/home/home.component.ts`  
**Ścieżka routingu**: `/`  
**Typ**: Standalone component

## 2. Główny cel

Ekran startowy służy jako punkt wejścia do aplikacji, prezentując użytkownikowi wszystkie dostępne opcje gry i umożliwiając natychmiastowe rozpoczęcie rozgrywki. Komponent obsługuje zarówno gości, jak i zarejestrowanych użytkowników.

## 3. Funkcjonalności

### 3.1 Wyświetlanie statusu użytkownika
- Sprawdzenie czy użytkownik jest gościem czy zarejestrowany
- Wyświetlenie odpowiednich opcji w zależności od statusu

### 3.2 Sprawdzenie zapisanej gry
- Automatyczne sprawdzenie czy istnieje zapisana gra przy inicjalizacji
- Wyświetlenie bannera z ostatnią grą (jeśli istnieje)

### 3.3 Opcje gry
- Graj jako gość - natychmiastowe utworzenie sesji gościa
- Graj z botem - przekierowanie do wyboru trybu
- Graj PvP - dołączenie do matchmakingu
- Zaloguj się / Zarejestruj się (dla gości)

### 3.4 Kontynuacja zapisanej gry
- Przycisk "Kontynuuj grę" w bannerze
- Przekierowanie do GameComponent z gameId

## 4. Struktura komponentu

### 4.1 Template

```html
<div class="home-container">
  <div class="home-header">
    <h1>World at War: Turn-Based Strategy</h1>
    <p class="subtitle">Rywalizuj z botem AI lub innymi graczami</p>
  </div>

  <app-game-banner 
    *ngIf="savedGame$ | async as game"
    [game]="game"
    (continueGame)="onContinueGame($event)">
  </app-game-banner>

  <div class="game-modes">
    <app-game-mode-card
      *ngFor="let mode of gameModes"
      [mode]="mode"
      [isGuest]="isGuest$ | async"
      (modeSelected)="onGameModeSelected($event)">
    </app-game-mode-card>
  </div>

  <div class="auth-section" *ngIf="isGuest$ | async">
    <p>Chcesz śledzić swoje postępy?</p>
    <div class="auth-buttons">
      <p-button label="Zaloguj się" (onClick)="navigateToLogin()"></p-button>
      <p-button label="Zarejestruj się" (onClick)="navigateToRegister()"></p-button>
    </div>
  </div>
</div>
```

### 4.2 Komponent TypeScript

```typescript
@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    GameBannerComponent,
    GameModeCardComponent,
    ButtonModule,
    AsyncPipe
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  savedGame$ = new BehaviorSubject<Game | null>(null);
  isGuest$ = this.authService.isGuest();
  currentUser$ = this.authService.getCurrentUser();
  
  gameModes = [
    { id: 'guest', label: 'Graj jako gość', icon: 'user', route: null },
    { id: 'bot', label: 'Graj z botem', icon: 'robot', route: '/game/mode-selection' },
    { id: 'pvp', label: 'Graj PvP', icon: 'users', route: '/game/matchmaking' }
  ];

  constructor(
    private authService: AuthService,
    private gameService: GameService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.checkSavedGame();
    this.ensureGuestSession();
  }

  private checkSavedGame(): void {
    this.gameService.getSavedGame().subscribe({
      next: (game) => this.savedGame$.next(game),
      error: (error) => console.error('Error loading saved game:', error)
    });
  }

  private ensureGuestSession(): void {
    this.isGuest$.pipe(
      take(1),
      filter(isGuest => isGuest),
      switchMap(() => this.authService.createGuestSession())
    ).subscribe({
      next: () => console.log('Guest session created'),
      error: (error) => console.error('Error creating guest session:', error)
    });
  }

  onGameModeSelected(mode: GameMode): void {
    if (mode.id === 'guest') {
      this.authService.createGuestSession().subscribe({
        next: () => this.router.navigate(['/game/mode-selection']),
        error: (error) => this.handleError(error)
      });
    } else if (mode.route) {
      this.router.navigate([mode.route]);
    }
  }

  onContinueGame(gameId: number): void {
    this.router.navigate(['/game', gameId]);
  }

  navigateToLogin(): void {
    this.router.navigate(['/auth/login']);
  }

  navigateToRegister(): void {
    this.router.navigate(['/auth/register']);
  }

  private handleError(error: any): void {
    console.error('Error:', error);
  }
}
```

## 5. Integracja API

### 5.1 Endpointy

- `GET /api/games?status=in_progress&size=1` - sprawdzenie zapisanej gry
- `POST /api/guests` - utworzenie sesji gościa (jeśli gość)

### 5.2 Serwisy

- `AuthService.createGuestSession()` - utworzenie sesji gościa
- `AuthService.isGuest()` - sprawdzenie statusu gościa
- `GameService.getSavedGame()` - pobranie zapisanej gry

## 6. Stany i logika biznesowa

### 6.1 Stany komponentu

- `savedGame$` - BehaviorSubject z zapisaną grą (null jeśli brak)
- `isGuest$` - Observable z statusem gościa
- `currentUser$` - Observable z aktualnym użytkownikiem

### 6.2 Logika biznesowa

1. **Inicjalizacja**:
   - Sprawdzenie zapisanej gry
   - Utworzenie sesji gościa (jeśli gość)

2. **Wybór trybu gry**:
   - Gość: utworzenie sesji gościa → przekierowanie
   - Zarejestrowany: bezpośrednie przekierowanie

3. **Kontynuacja gry**:
   - Przekierowanie do GameComponent z gameId

## 7. Komponenty współdzielone

### 7.1 GameBannerComponent

**Lokalizacja**: `components/game/game-banner.component.ts`

**Funkcjonalność**:
- Wyświetlanie informacji o ostatniej zapisanej grze
- Przycisk "Kontynuuj grę"
- Warunkowe wyświetlanie (tylko jeśli gra istnieje)

**Inputs**:
- `game: Game` - zapisana gra

**Outputs**:
- `continueGame: EventEmitter<number>` - emisja gameId

### 7.2 GameModeCardComponent

**Lokalizacja**: `components/game/game-mode-card.component.ts`

**Funkcjonalność**:
- Karta z trybem gry
- Ikona, etykieta, opis
- Obsługa kliknięcia

**Inputs**:
- `mode: GameMode` - tryb gry
- `isGuest: boolean` - status gościa

**Outputs**:
- `modeSelected: EventEmitter<GameMode>` - emisja wybranego trybu

## 8. Stylowanie

### 8.1 SCSS

```scss
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;

  .home-header {
    text-align: center;
    margin-bottom: 3rem;

    h1 {
      font-size: 2.5rem;
      margin-bottom: 0.5rem;
    }

    .subtitle {
      font-size: 1.2rem;
      color: #666;
    }
  }

  .game-modes {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 2rem;
    margin-bottom: 3rem;
  }

  .auth-section {
    text-align: center;
    padding: 2rem;
    background: #f5f5f5;
    border-radius: 8px;

    .auth-buttons {
      display: flex;
      gap: 1rem;
      justify-content: center;
      margin-top: 1rem;
    }
  }
}
```

## 9. Animacje

- Fade-in dla banneru z grą (300ms)
- Scale animation dla kart trybów gry (hover effect)
- Smooth transitions dla przycisków

## 10. Obsługa błędów

- Błąd pobierania zapisanej gry: ciche logowanie, brak wyświetlania bannera
- Błąd utworzenia sesji gościa: toast notification, możliwość ponowienia
- Błąd nawigacji: przekierowanie do 404

## 11. Testy

### 11.1 Testy jednostkowe

- Sprawdzenie wyświetlania bannera z grą
- Sprawdzenie utworzenia sesji gościa
- Sprawdzenie nawigacji do trybów gry
- Sprawdzenie kontynuacji gry

### 11.2 Testy E2E (Cypress)

- Scenariusz: Gość → wybór trybu gry
- Scenariusz: Zarejestrowany → kontynuacja zapisanej gry
- Scenariusz: Gość → rejestracja

## 12. Dostępność

- ARIA labels dla wszystkich przycisków
- Keyboard navigation dla kart trybów
- Focus indicators
- Screen reader support dla statusu użytkownika

## 13. Wsparcie dla wielu języków (i18n)

### 13.1 Implementacja

Komponent wykorzystuje Angular i18n do obsługi wielu języków. Wszystkie teksty w komponencie są tłumaczone:
- Etykiety przycisków ("Graj jako gość", "Graj z botem", "Graj PvP", "Zaloguj się", "Zarejestruj się")
- Nagłówki i opisy
- Komunikaty błędów
- Teksty w bannerze z ostatnią grą

### 13.2 Języki wspierane

- **Angielski (en)** - język podstawowy, domyślny
- **Polski (pl)** - język dodatkowy

### 13.3 Użycie

Wszystkie teksty w template są opakowane w pipe `i18n` lub używają serwisu `TranslateService`:
```typescript
{{ 'home.title' | translate }}
{{ 'home.playAsGuest' | translate }}
```

### 13.4 Backend

Backend pozostaje bez zmian - wszystkie odpowiedzi API są w języku angielskim. Tłumaczenie komunikatów z backendu na język użytkownika odbywa się po stronie frontendu.

## 14. Mapowanie historyjek użytkownika

- **US-001**: Rozpoczęcie gry jako gość - przycisk "Graj jako gość"
- **US-012**: Automatyczne zapisywanie gier - banner z ostatnią grą

