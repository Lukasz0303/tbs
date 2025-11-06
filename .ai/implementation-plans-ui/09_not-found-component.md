# Plan implementacji: NotFoundComponent

## 1. Przegląd

**Nazwa komponentu**: `NotFoundComponent`  
**Lokalizacja**: `frontend/src/app/features/not-found/not-found.component.ts`  
**Ścieżka routingu**: `/404` lub `**` (catch-all)  
**Typ**: Standalone component

## 2. Główny cel

Wyświetlanie komunikatu o błędzie, gdy użytkownik próbuje uzyskać dostęp do nieistniejącej strony. Komponent zapewnia przyjazny komunikat i łatwą nawigację z powrotem do głównych sekcji aplikacji.

## 3. Funkcjonalności

### 3.1 Wyświetlanie komunikatu błędu
- Komunikat o błędzie 404
- Informacja o nieistniejącej stronie
- Przyjazny komunikat dla użytkownika

### 3.2 Nawigacja
- Link powrotu do strony głównej
- Link do rankingu
- Link do innych głównych sekcji

## 4. Struktura komponentu

### 4.1 Template

```html
<div class="not-found-container">
  <div class="not-found-content">
    <div class="not-found-icon">
      <i class="pi pi-exclamation-triangle"></i>
    </div>
    
    <h1>404</h1>
    <h2>Strona nie została znaleziona</h2>
    <p>Przepraszamy, strona której szukasz nie istnieje lub została przeniesiona.</p>

    <div class="not-found-actions">
      <p-button
        label="Powrót do strony głównej"
        (onClick)="navigateToHome()">
      </p-button>
      
      <p-button
        label="Ranking"
        severity="secondary"
        (onClick)="navigateToLeaderboard()">
      </p-button>
    </div>

    <div class="not-found-links">
      <a routerLink="/">Strona główna</a>
      <a routerLink="/leaderboard">Ranking</a>
      <a routerLink="/profile">Profil</a>
    </div>
  </div>
</div>
```

### 4.2 Komponent TypeScript

```typescript
@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule
  ],
  templateUrl: './not-found.component.html',
  styleUrls: ['./not-found.component.scss']
})
export class NotFoundComponent {
  constructor(private router: Router) {}

  navigateToHome(): void {
    this.router.navigate(['/']);
  }

  navigateToLeaderboard(): void {
    this.router.navigate(['/leaderboard']);
  }
}
```

## 5. Integracja API

Brak integracji API - komponent jest czysto prezentacyjny.

## 6. Stylowanie

### 6.1 SCSS

```scss
.not-found-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: 2rem;

  .not-found-content {
    text-align: center;
    max-width: 600px;

    .not-found-icon {
      font-size: 5rem;
      color: #ff9800;
      margin-bottom: 2rem;
    }

    h1 {
      font-size: 6rem;
      font-weight: bold;
      margin: 0;
      color: #333;
    }

    h2 {
      font-size: 2rem;
      margin: 1rem 0;
      color: #666;
    }

    p {
      font-size: 1.1rem;
      color: #666;
      margin-bottom: 2rem;
    }

    .not-found-actions {
      display: flex;
      gap: 1rem;
      justify-content: center;
      margin-bottom: 2rem;
    }

    .not-found-links {
      display: flex;
      gap: 2rem;
      justify-content: center;

      a {
        color: #007bff;
        text-decoration: none;
        font-size: 1rem;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }
}
```

## 7. Animacje

- Fade-in dla całego komponentu (300ms)
- Pulse animation dla ikony błędu
- Smooth transitions dla przycisków

## 8. Obsługa błędów

Brak obsługi błędów - komponent jest czysto prezentacyjny.

## 9. Testy

### 9.1 Testy jednostkowe

- Sprawdzenie wyświetlania komunikatu błędu
- Sprawdzenie nawigacji do strony głównej
- Sprawdzenie nawigacji do rankingu

### 9.2 Testy E2E (Cypress)

- Scenariusz: Próba dostępu do nieistniejącej strony
- Scenariusz: Nawigacja z strony 404 do głównych sekcji

## 10. Dostępność

- ARIA labels dla wszystkich elementów
- Keyboard navigation
- Screen reader support dla komunikatu błędu
- Focus indicators

## 11. Wsparcie dla wielu języków (i18n)

### 11.1 Implementacja

Komponent wykorzystuje Angular i18n do obsługi wielu języków. Wszystkie teksty w komponencie są tłumaczone:
- Komunikaty błędu ("404", "Strona nie została znaleziona", "Przepraszamy, strona której szukasz nie istnieje lub została przeniesiona.")
- Przyciski ("Powrót do strony głównej", "Ranking")
- Linki nawigacyjne

### 11.2 Języki wspierane

- **Angielski (en)** - język podstawowy, domyślny
- **Polski (pl)** - język dodatkowy

### 11.3 Użycie

Wszystkie teksty w template są opakowane w pipe `i18n` lub używają serwisu `TranslateService`:
```typescript
{{ 'notFound.title' | translate }}
{{ 'notFound.message' | translate }}
{{ 'notFound.backToHome' | translate }}
```

### 11.4 Backend

Backend pozostaje bez zmian - komponent jest czysto prezentacyjny i nie wymaga integracji z backendem.

## 12. Mapowanie historyjek użytkownika

Brak bezpośredniego mapowania - komponent obsługuje przypadki brzegowe.

