# Plan implementacji: GameModeSelectionComponent

## 1. Przegląd

**Nazwa komponentu**: `GameModeSelectionComponent`  
**Lokalizacja**: `frontend/src/app/features/game/game-mode-selection.component.ts`  
**Ścieżka routingu**: `/game/mode-selection`  
**Typ**: Standalone component

## 2. Główny cel

Umożliwienie użytkownikowi wyboru rozmiaru planszy i poziomu trudności bota przed rozpoczęciem gry vs bot. Po wyborze komponent automatycznie tworzy grę i przekierowuje do GameComponent.

## 3. Funkcjonalności

### 3.1 Wybór rozmiaru planszy
- Karty z rozmiarami planszy (3x3, 4x4, 5x5)
- Wizualizacja planszy dla każdego rozmiaru
- Zaznaczenie wybranego rozmiaru

### 3.2 Wybór poziomu trudności
- Karty z poziomami trudności (łatwy, średni, trudny)
- Opis każdego poziomu
- Informacja o punktacji:
  - Łatwy: +100 pkt za wygraną
  - Średni: +500 pkt za wygraną
  - Trudny: +1000 pkt za wygraną

### 3.3 Rozpoczęcie gry
- Przycisk rozpoczęcia gry
- Walidacja wyboru przed utworzeniem gry
- Przekierowanie do GameComponent po utworzeniu gry

## 4. Struktura komponentu

### 4.1 Template

```html
<div class="mode-selection-container">
  <div class="mode-selection-header">
    <h2>Wybierz tryb gry</h2>
    <p>Wybierz rozmiar planszy i poziom trudności bota</p>
  </div>

  <div class="selection-section">
    <h3>Rozmiar planszy</h3>
    <div class="board-size-cards">
      <app-board-size-card
        *ngFor="let size of boardSizes"
        [size]="size"
        [selected]="selectedBoardSize === size"
        (select)="onBoardSizeSelect($event)">
      </app-board-size-card>
    </div>
  </div>

  <div class="selection-section">
    <h3>Poziom trudności</h3>
    <div class="difficulty-cards">
      <app-difficulty-card
        *ngFor="let difficulty of difficulties"
        [difficulty]="difficulty"
        [selected]="selectedDifficulty === difficulty.id"
        (select)="onDifficultySelect($event)">
      </app-difficulty-card>
    </div>
  </div>

  <div class="selection-actions">
    <p-button
      label="Rozpocznij grę"
      [disabled]="!selectedBoardSize || !selectedDifficulty || isLoading"
      [loading]="isLoading"
      (onClick)="onStartGame()">
    </p-button>
  </div>
</div>
```

### 4.2 Komponent TypeScript

```typescript
@Component({
  selector: 'app-game-mode-selection',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
    BoardSizeCardComponent,
    DifficultyCardComponent
  ],
  templateUrl: './game-mode-selection.component.html',
  styleUrls: ['./game-mode-selection.component.scss']
})
export class GameModeSelectionComponent implements OnInit {
  selectedBoardSize: 3 | 4 | 5 | null = null;
  selectedDifficulty: 'easy' | 'medium' | 'hard' | null = null;
  isLoading = false;

  boardSizes: (3 | 4 | 5)[] = [3, 4, 5];
  
  difficulties = [
    {
      id: 'easy',
      label: 'Łatwy',
      description: 'Bot wykonuje losowe, poprawne ruchy',
      points: 100,
      icon: 'smile'
    },
    {
      id: 'medium',
      label: 'Średni',
      description: 'Bot stosuje podstawową strategię',
      points: 500,
      icon: 'meh'
    },
    {
      id: 'hard',
      label: 'Trudny',
      description: 'Bot stosuje optymalną strategię (minimax)',
      points: 1000,
      icon: 'frown'
    }
  ];

  constructor(
    private gameService: GameService,
    private router: Router,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.selectedBoardSize = 3;
    this.selectedDifficulty = 'easy';
  }

  onBoardSizeSelect(size: 3 | 4 | 5): void {
    this.selectedBoardSize = size;
  }

  onDifficultySelect(difficultyId: 'easy' | 'medium' | 'hard'): void {
    this.selectedDifficulty = difficultyId;
  }

  onStartGame(): void {
    if (!this.selectedBoardSize || !this.selectedDifficulty) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Uwaga',
        detail: 'Wybierz rozmiar planszy i poziom trudności'
      });
      return;
    }

    this.isLoading = true;

    this.gameService.createGame({
      gameType: 'vs_bot',
      boardSize: this.selectedBoardSize,
      botDifficulty: this.selectedDifficulty
    }).subscribe({
      next: (game) => {
        this.isLoading = false;
        this.router.navigate(['/game', game.gameId]);
      },
      error: (error) => {
        this.isLoading = false;
        this.handleError(error);
      }
    });
  }

  private handleError(error: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd',
      detail: error.error?.message || 'Nie udało się utworzyć gry'
    });
  }
}
```

## 5. Integracja API

### 5.1 Endpointy

- `POST /api/games` - utworzenie gry vs_bot

**Request body**:
```json
{
  "gameType": "vs_bot",
  "boardSize": 3 | 4 | 5,
  "botDifficulty": "easy | medium | hard"
}
```

**Response (201 Created)**:
```json
{
  "gameId": 1,
  "gameType": "vs_bot",
  "boardSize": 3,
  "player1Id": 1,
  "player2Id": null,
  "botDifficulty": "easy",
  "status": "waiting",
  "currentPlayerSymbol": "x",
  "createdAt": "2024-11-06T12:00:00Z",
  "boardState": []
}
```

### 5.2 Serwisy

- `GameService.createGame(gameData)` - utworzenie gry vs_bot

## 6. Komponenty współdzielone

### 6.1 BoardSizeCardComponent

**Lokalizacja**: `components/game/board-size-card.component.ts`

**Funkcjonalność**:
- Karta z rozmiarem planszy
- Wizualizacja planszy (miniaturka)
- Zaznaczenie wybranego rozmiaru

**Inputs**:
- `size: 3 | 4 | 5` - rozmiar planszy
- `selected: boolean` - czy rozmiar jest wybrany

**Outputs**:
- `select: EventEmitter<3 | 4 | 5>` - emisja wybranego rozmiaru

### 6.2 DifficultyCardComponent

**Lokalizacja**: `components/game/difficulty-card.component.ts`

**Funkcjonalność**:
- Karta z poziomem trudności
- Opis poziomu
- Informacja o punktacji
- Ikona poziomu

**Inputs**:
- `difficulty: Difficulty` - poziom trudności
- `selected: boolean` - czy poziom jest wybrany

**Outputs**:
- `select: EventEmitter<string>` - emisja wybranego poziomu

## 7. Stylowanie

### 7.1 SCSS

```scss
.mode-selection-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;

  .mode-selection-header {
    text-align: center;
    margin-bottom: 3rem;

    h2 {
      font-size: 2rem;
      margin-bottom: 0.5rem;
    }

    p {
      font-size: 1.1rem;
      color: #666;
    }
  }

  .selection-section {
    margin-bottom: 3rem;

    h3 {
      font-size: 1.5rem;
      margin-bottom: 1.5rem;
    }

    .board-size-cards,
    .difficulty-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1.5rem;
    }
  }

  .selection-actions {
    text-align: center;
    margin-top: 3rem;
  }
}
```

## 8. Animacje

- Scale animation dla kart przy hover
- Smooth transitions dla zaznaczenia
- Fade-in dla sekcji wyboru

## 9. Obsługa błędów

- Błąd utworzenia gry: toast notification, możliwość ponowienia
- Błąd walidacji: wyświetlenie błędów w formularzu

## 10. Testy

### 10.1 Testy jednostkowe

- Sprawdzenie wyboru rozmiaru planszy
- Sprawdzenie wyboru poziomu trudności
- Sprawdzenie utworzenia gry
- Sprawdzenie przekierowania do gry

### 10.2 Testy E2E (Cypress)

- Scenariusz: Wybór trybu vs bot (łatwy poziom)
- Scenariusz: Wybór trybu vs bot (średni poziom)
- Scenariusz: Wybór trybu vs bot (trudny poziom)

## 11. Dostępność

- ARIA labels dla wszystkich kart
- Keyboard navigation dla kart
- Focus indicators
- Screen reader support dla wyboru

## 12. Wsparcie dla wielu języków (i18n)

### 12.1 Implementacja

Komponent wykorzystuje Angular i18n do obsługi wielu języków. Wszystkie teksty w komponencie są tłumaczone:
- Nagłówki ("Wybierz tryb gry", "Rozmiar planszy", "Poziom trudności")
- Opisy poziomów trudności ("Łatwy", "Średni", "Trudny")
- Opisy poziomów ("Bot wykonuje losowe, poprawne ruchy", "Bot stosuje podstawową strategię", "Bot stosuje optymalną strategię")
- Informacje o punktacji ("+100 pkt za wygraną", "+500 pkt za wygraną", "+1000 pkt za wygraną")
- Komunikaty błędów

### 12.2 Języki wspierane

- **Angielski (en)** - język podstawowy, domyślny
- **Polski (pl)** - język dodatkowy

### 12.3 Użycie

Wszystkie teksty w template są opakowane w pipe `i18n` lub używają serwisu `TranslateService`:
```typescript
{{ 'gameMode.title' | translate }}
{{ 'gameMode.difficulty.easy' | translate }}
{{ 'gameMode.difficulty.medium' | translate }}
{{ 'gameMode.difficulty.hard' | translate }}
```

### 12.4 Backend

Backend pozostaje bez zmian - wszystkie odpowiedzi API są w języku angielskim. Tłumaczenie komunikatów z backendu na język użytkownika odbywa się po stronie frontendu.

## 13. Mapowanie historyjek użytkownika

- **US-004**: Rozgrywka z botem (łatwy poziom)
- **US-005**: Rozgrywka z botem (średni poziom)
- **US-006**: Rozgrywka z botem (trudny poziom)

