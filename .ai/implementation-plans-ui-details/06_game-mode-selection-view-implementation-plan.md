# Plan implementacji widoku GameModeSelectionComponent

## 1. Przegląd

GameModeSelectionComponent to widok umożliwiający użytkownikowi wybór rozmiaru planszy i poziomu trudności bota przed rozpoczęciem gry vs bot. Po wyborze komponent automatycznie tworzy grę i przekierowuje do GameComponent.

Główne funkcjonalności:
- Wybór rozmiaru planszy (3x3, 4x4, 5x5) z wizualizacją planszy
- Wybór poziomu trudności bota (łatwy, średni, trudny) z opisem i informacją o punktacji
- Walidacja wyboru przed utworzeniem gry
- Integracja z endpointem POST /api/games
- Przekierowanie do GameComponent po utworzeniu gry
- Obsługa błędów API z wyświetlaniem komunikatów użytkownikowi

Komponent realizuje historyjki użytkownika: US-004 (Rozgrywka z botem - łatwy poziom), US-005 (Rozgrywka z botem - średni poziom), US-006 (Rozgrywka z botem - trudny poziom).

## 2. Routing widoku

**Ścieżka routingu**: `/game/mode-selection`

**Konfiguracja routingu**:
```typescript
{
  path: 'game',
  children: [
    {
      path: 'mode-selection',
      component: GameModeSelectionComponent
    },
    {
      path: ':gameId',
      component: GameComponent
    }
  ]
}
```

**Lokalizacja pliku routingu**: `frontend/src/app/app.routes.ts`

**Guardy**: Wymagane uwierzytelnienie (użytkownik musi być zalogowany lub gościem)

## 3. Struktura komponentów

```
GameModeSelectionComponent (główny komponent)
├── GameModeSelectionHeaderComponent (opcjonalny, nagłówek z tytułem)
├── BoardSizeSelectionSectionComponent (sekcja wyboru rozmiaru planszy)
│   └── BoardSizeCardComponent (powtarzalny, karty rozmiaru planszy)
│       └── BoardPreviewComponent (opcjonalny, miniaturka planszy)
├── DifficultySelectionSectionComponent (sekcja wyboru poziomu trudności)
│   └── DifficultyCardComponent (powtarzalny, karty poziomu trudności)
│       └── IconComponent (opcjonalny, ikona poziomu)
└── ButtonModule (PrimeNG - przycisk rozpoczęcia gry)
```

**Hierarchia komponentów**:
- GameModeSelectionComponent jest komponentem standalone
- BoardSizeCardComponent, DifficultyCardComponent są komponentami współdzielonymi
- Wszystkie komponenty używają PrimeNG do elementów UI
- Komponenty używają Angular Animations do płynnych animacji

## 4. Szczegóły komponentów

### GameModeSelectionComponent

**Opis komponentu**: Główny komponent widoku, zarządza stanem wyboru rozmiaru planszy i poziomu trudności, integracją z API oraz obsługą błędów i przekierowań. Komponent inicjalizuje domyślne wartości (3x3, łatwy) i umożliwia użytkownikowi zmianę wyboru przed rozpoczęciem gry.

**Główne elementy HTML**:
- Kontener główny (`.mode-selection-container`)
- Sekcja nagłówka (`.mode-selection-header`) z tytułem i podtytułem
- Sekcja wyboru rozmiaru planszy (`.selection-section.board-size-section`)
  - Nagłówek sekcji (`<h3>Rozmiar planszy</h3>`)
  - Kontener kart (`.board-size-cards`)
  - Karty rozmiaru planszy (`<app-board-size-card>`)
- Sekcja wyboru poziomu trudności (`.selection-section.difficulty-section`)
  - Nagłówek sekcji (`<h3>Poziom trudności</h3>`)
  - Kontener kart (`.difficulty-cards`)
  - Karty poziomu trudności (`<app-difficulty-card>`)
- Sekcja akcji (`.selection-actions`)
  - Przycisk rozpoczęcia gry (`<p-button>`)

**Obsługiwane zdarzenia**:
- `ngOnInit()` - inicjalizacja komponentu, ustawienie domyślnych wartości
- `onBoardSizeSelect(size: 3 | 4 | 5)` - obsługa wyboru rozmiaru planszy
- `onDifficultySelect(difficultyId: 'easy' | 'medium' | 'hard')` - obsługa wyboru poziomu trudności
- `onStartGame()` - obsługa rozpoczęcia gry, walidacja i wywołanie API
- `handleError(error: HttpErrorResponse)` - obsługa błędów API

**Obsługiwana walidacja**:
- Sprawdzenie czy wybrano rozmiar planszy (przed rozpoczęciem gry)
- Sprawdzenie czy wybrano poziom trudności (przed rozpoczęciem gry)
- Walidacja po stronie serwera (API):
  - 400 Bad Request - nieprawidłowe parametry gry
  - 401 Unauthorized - brak uwierzytelnienia
  - 422 Unprocessable Entity - błędy walidacji Bean Validation
  - 500 Internal Server Error - błąd serwera

**Typy**:
- `BoardSize` - typ reprezentujący rozmiar planszy (3 | 4 | 5)
- `BotDifficulty` - typ reprezentujący poziom trudności ('easy' | 'medium' | 'hard')
- `CreateGameRequest` - DTO dla żądania utworzenia gry
- `CreateGameResponse` - DTO dla odpowiedzi API
- `Game` - interfejs reprezentujący grę
- `Observable<CreateGameResponse>` - Observable z odpowiedzią API
- `HttpErrorResponse` - Angular HTTP error response

**Propsy**: Brak (komponent główny, nie przyjmuje propsów)

### BoardSizeCardComponent

**Opis komponentu**: Komponent wyświetlający kartę z rozmiarem planszy, wizualizacją planszy i możliwością wyboru. Karta jest zaznaczona wizualnie gdy rozmiar jest wybrany.

**Główne elementy HTML**:
- Kontener karty (`.board-size-card`)
- Wizualizacja planszy (`.board-preview`)
- Etykieta rozmiaru (`.size-label`)
- Wskaźnik wyboru (`.selected-indicator`)

**Obsługiwane zdarzenia**:
- `select` - EventEmitter emitujący rozmiar planszy po kliknięciu

**Obsługiwana walidacja**: Brak (komponent prezentacyjny)

**Typy**:
- `BoardSize` - typ reprezentujący rozmiar planszy (3 | 4 | 5)
- `boolean` - flaga wskazująca czy rozmiar jest wybrany

**Propsy**:
- `size: 3 | 4 | 5` - rozmiar planszy (wymagane)
- `selected: boolean` - czy rozmiar jest wybrany (wymagane)

**Outputs**:
- `select: EventEmitter<3 | 4 | 5>` - emisja wybranego rozmiaru

### DifficultyCardComponent

**Opis komponentu**: Komponent wyświetlający kartę z poziomem trudności, opisem, informacją o punktacji i ikoną. Karta jest zaznaczona wizualnie gdy poziom jest wybrany.

**Główne elementy HTML**:
- Kontener karty (`.difficulty-card`)
- Ikona poziomu (`.difficulty-icon`)
- Etykieta poziomu (`.difficulty-label`)
- Opis poziomu (`.difficulty-description`)
- Informacja o punktacji (`.difficulty-points`)
- Wskaźnik wyboru (`.selected-indicator`)

**Obsługiwane zdarzenia**:
- `select` - EventEmitter emitujący poziom trudności po kliknięciu

**Obsługiwana walidacja**: Brak (komponent prezentacyjny)

**Typy**:
- `Difficulty` - interfejs reprezentujący poziom trudności
- `boolean` - flaga wskazująca czy poziom jest wybrany

**Propsy**:
- `difficulty: Difficulty` - poziom trudności (wymagane)
- `selected: boolean` - czy poziom jest wybrany (wymagane)

**Outputs**:
- `select: EventEmitter<'easy' | 'medium' | 'hard'>` - emisja wybranego poziomu

## 5. Typy

### BoardSize (typ rozmiaru planszy)

```typescript
type BoardSize = 3 | 4 | 5;
```

**Uwagi**:
- Typ reprezentujący rozmiar planszy w grze kółko i krzyżyk
- Używany do wyboru rozmiaru planszy przed rozpoczęciem gry
- Mapowany na enum `BoardSize` w backendzie (THREE, FOUR, FIVE)

### BotDifficulty (typ poziomu trudności)

```typescript
type BotDifficulty = 'easy' | 'medium' | 'hard';
```

**Uwagi**:
- Typ reprezentujący poziom trudności bota AI
- Używany do wyboru poziomu trudności przed rozpoczęciem gry
- Mapowany na enum `BotDifficulty` w backendzie (EASY, MEDIUM, HARD)

### Difficulty (interfejs poziomu trudności)

```typescript
interface Difficulty {
  id: 'easy' | 'medium' | 'hard';
  label: string;
  description: string;
  points: number;
  icon: string;
}
```

**Pola**:
- `id: 'easy' | 'medium' | 'hard'` - unikalny identyfikator poziomu trudności
- `label: string` - etykieta poziomu (np. "Łatwy", "Średni", "Trudny")
- `description: string` - opis poziomu (np. "Bot wykonuje losowe, poprawne ruchy")
- `points: number` - liczba punktów za wygraną (100, 500, 1000)
- `icon: string` - nazwa ikony (np. "smile", "meh", "frown")

**Uwagi**:
- Interfejs używany do reprezentacji poziomu trudności w komponencie
- Używany w DifficultyCardComponent do wyświetlania informacji o poziomie

### CreateGameRequest (DTO dla żądania)

```typescript
interface CreateGameRequest {
  gameType: 'vs_bot';
  boardSize: 3 | 4 | 5;
  botDifficulty: 'easy' | 'medium' | 'hard';
}
```

**Pola**:
- `gameType: 'vs_bot'` - typ gry (zawsze 'vs_bot' dla tego widoku)
- `boardSize: 3 | 4 | 5` - rozmiar planszy (wymagane)
- `botDifficulty: 'easy' | 'medium' | 'hard'` - poziom trudności bota (wymagane)

**Uwagi**:
- DTO używane do wysłania żądania do endpointu POST /api/games
- Walidacja po stronie klienta: sprawdzenie czy wybrano rozmiar i poziom
- Walidacja po stronie serwera: `@NotNull`, `@Valid` (Bean Validation)

### CreateGameResponse (DTO dla odpowiedzi)

```typescript
interface CreateGameResponse {
  gameId: number;
  gameType: 'vs_bot' | 'pvp';
  boardSize: 3 | 4 | 5;
  player1Id: number;
  player2Id: number | null;
  botDifficulty: 'easy' | 'medium' | 'hard' | null;
  status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw';
  currentPlayerSymbol: 'x' | 'o' | null;
  createdAt: string;
  boardState: string[][];
}
```

**Pola**:
- `gameId: number` - unikalny identyfikator gry
- `gameType: 'vs_bot' | 'pvp'` - typ gry (zawsze 'vs_bot' dla tego widoku)
- `boardSize: 3 | 4 | 5` - rozmiar planszy
- `player1Id: number` - ID gracza 1 (twórca gry)
- `player2Id: number | null` - ID gracza 2 (NULL dla vs_bot)
- `botDifficulty: 'easy' | 'medium' | 'hard' | null` - poziom trudności bota
- `status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw'` - status gry
- `currentPlayerSymbol: 'x' | 'o' | null` - symbol aktualnego gracza
- `createdAt: string` - data utworzenia (ISO 8601)
- `boardState: string[][]` - stan planszy (pusta plansza dla nowej gry)

**Uwagi**:
- DTO zwracane przez endpoint POST /api/games po pomyślnym utworzeniu gry
- `gameId` jest używany do przekierowania do GameComponent
- `boardState` jest pustą planszą dla nowej gry

### ApiErrorResponse (DTO dla błędów API)

```typescript
interface ApiErrorResponse {
  error: {
    code: string;
    message: string;
    details: Record<string, string> | null;
  };
  timestamp: string;
  status: 'error';
}
```

**Pola**:
- `error.code: string` - kod błędu (np. "BAD_REQUEST", "VALIDATION_ERROR")
- `error.message: string` - komunikat błędu
- `error.details: Record<string, string> | null` - szczegóły błędu (opcjonalne)
- `timestamp: string` - znacznik czasu błędu (ISO 8601)
- `status: 'error'` - status odpowiedzi

**Uwagi**:
- DTO używane do obsługi błędów API
- Używane w handleError do wyświetlania komunikatów użytkownikowi

## 6. Zarządzanie stanem

**Stan komponentu**:
- `selectedBoardSize: 3 | 4 | 5 | null` - wybrany rozmiar planszy
- `selectedDifficulty: 'easy' | 'medium' | 'hard' | null` - wybrany poziom trudności
- `isLoading: boolean` - flaga wskazująca czy trwa tworzenie gry

**Inicjalizacja stanu**:
- W `ngOnInit()`: ustawienie domyślnych wartości (3, 'easy')
- `selectedBoardSize = 3`
- `selectedDifficulty = 'easy'`
- `isLoading = false`

**Aktualizacja stanu**:
- `onBoardSizeSelect(size)`: aktualizacja `selectedBoardSize`
- `onDifficultySelect(difficultyId)`: aktualizacja `selectedDifficulty`
- `onStartGame()`: ustawienie `isLoading = true` przed wywołaniem API, `isLoading = false` po zakończeniu

**Brak customowych hooków**: Komponent używa standardowych mechanizmów Angular (Reactive Forms, Services, Observables)

## 7. Integracja API

### Endpoint: POST /api/games

**Opis**: Endpoint służący do utworzenia nowej gry vs_bot z wybranymi parametrami (rozmiar planszy, poziom trudności).

**Request**:
```typescript
POST /api/games
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "gameType": "vs_bot",
  "boardSize": 3,
  "botDifficulty": "easy"
}
```

**Response (201 Created)**:
```typescript
{
  "gameId": 42,
  "gameType": "vs_bot",
  "boardSize": 3,
  "player1Id": 123,
  "player2Id": null,
  "botDifficulty": "easy",
  "status": "waiting",
  "currentPlayerSymbol": null,
  "createdAt": "2024-01-20T15:30:00Z",
  "boardState": [
    [null, null, null],
    [null, null, null],
    [null, null, null]
  ]
}
```

**Obsługa błędów**:
- 400 Bad Request: Nieprawidłowe parametry gry
- 401 Unauthorized: Brak uwierzytelnienia
- 422 Unprocessable Entity: Błędy walidacji Bean Validation
- 500 Internal Server Error: Błąd serwera

**Implementacja w GameService**:
```typescript
createGame(request: CreateGameRequest): Observable<CreateGameResponse> {
  return this.http.post<CreateGameResponse>(
    `${this.apiUrl}/games`,
    request,
    { headers: this.getAuthHeaders() }
  );
}
```

## 8. Interakcje użytkownika

### Wybór rozmiaru planszy

**Akcja użytkownika**: Kliknięcie na kartę rozmiaru planszy (3x3, 4x4, 5x5)

**Oczekiwany wynik**:
- Karta jest zaznaczona wizualnie (zmiana stylu, wskaźnik wyboru)
- Poprzednio wybrana karta jest odznaczona
- Stan `selectedBoardSize` jest aktualizowany
- Animacja przejścia dla zaznaczenia

**Obsługa**: `onBoardSizeSelect(size: 3 | 4 | 5)`

### Wybór poziomu trudności

**Akcja użytkownika**: Kliknięcie na kartę poziomu trudności (łatwy, średni, trudny)

**Oczekiwany wynik**:
- Karta jest zaznaczona wizualnie (zmiana stylu, wskaźnik wyboru)
- Poprzednio wybrana karta jest odznaczona
- Stan `selectedDifficulty` jest aktualizowany
- Animacja przejścia dla zaznaczenia

**Obsługa**: `onDifficultySelect(difficultyId: 'easy' | 'medium' | 'hard')`

### Rozpoczęcie gry

**Akcja użytkownika**: Kliknięcie przycisku "Rozpocznij grę"

**Oczekiwany wynik**:
- Walidacja wyboru (sprawdzenie czy wybrano rozmiar i poziom)
- Jeśli walidacja nie powiodła się: wyświetlenie komunikatu błędu
- Jeśli walidacja powiodła się:
  - Przycisk jest wyłączony (`isLoading = true`)
  - Wyświetlenie wskaźnika ładowania
  - Wywołanie API POST /api/games
  - Po pomyślnym utworzeniu gry: przekierowanie do `/game/{gameId}`
  - Po błędzie: wyświetlenie komunikatu błędu, przywrócenie przycisku

**Obsługa**: `onStartGame()`

## 9. Warunki i walidacja

### Walidacja po stronie klienta

**Warunki przed rozpoczęciem gry**:
- `selectedBoardSize !== null` - musi być wybrany rozmiar planszy
- `selectedDifficulty !== null` - musi być wybrany poziom trudności
- `!isLoading` - nie może trwać już tworzenie gry

**Komponenty odpowiedzialne**:
- GameModeSelectionComponent: sprawdzenie warunków w `onStartGame()`
- Przycisk "Rozpocznij grę": wyłączony gdy `!selectedBoardSize || !selectedDifficulty || isLoading`

**Wpływ na stan interfejsu**:
- Przycisk jest wyłączony gdy warunki nie są spełnione
- Komunikat błędu jest wyświetlany gdy użytkownik próbuje rozpocząć grę bez wyboru

### Walidacja po stronie serwera (API)

**Warunki weryfikowane przez API**:
- `gameType` musi być "vs_bot"
- `boardSize` musi być 3, 4 lub 5
- `botDifficulty` musi być "easy", "medium" lub "hard" (wymagane dla vs_bot)
- Użytkownik musi być uwierzytelniony (token JWT)

**Komponenty odpowiedzialne**:
- Backend: walidacja w kontrolerze i serwisie
- Frontend: obsługa błędów w `handleError()`

**Wpływ na stan interfejsu**:
- Błąd 400: wyświetlenie komunikatu "Nieprawidłowe parametry gry"
- Błąd 401: przekierowanie do strony logowania
- Błąd 422: wyświetlenie szczegółów błędów walidacji
- Błąd 500: wyświetlenie komunikatu "Nie udało się utworzyć gry"

## 10. Obsługa błędów

### Scenariusz 1: Brak wyboru rozmiaru lub poziomu

**Sytuacja**: Użytkownik próbuje rozpocząć grę bez wyboru rozmiaru planszy lub poziomu trudności

**Obsługa**:
- Walidacja w `onStartGame()` przed wywołaniem API
- Wyświetlenie komunikatu: "Wybierz rozmiar planszy i poziom trudności"
- Nie wywoływanie API

**Komponent**: GameModeSelectionComponent

### Scenariusz 2: Błąd 400 Bad Request

**Sytuacja**: API zwraca błąd 400 (nieprawidłowe parametry gry)

**Obsługa**:
- Obsługa w `handleError()`
- Wyświetlenie komunikatu: "Nieprawidłowe parametry gry"
- Przywrócenie przycisku (`isLoading = false`)

**Komponent**: GameModeSelectionComponent

### Scenariusz 3: Błąd 401 Unauthorized

**Sytuacja**: Użytkownik nie jest uwierzytelniony lub token wygasł

**Obsługa**:
- Obsługa w `handleError()`
- Przekierowanie do strony logowania (`/auth/login`)
- Wyświetlenie komunikatu: "Musisz się zalogować, aby rozpocząć grę"

**Komponent**: GameModeSelectionComponent

### Scenariusz 4: Błąd 422 Unprocessable Entity

**Sytuacja**: API zwraca błędy walidacji Bean Validation

**Obsługa**:
- Obsługa w `handleError()`
- Wyświetlenie szczegółów błędów walidacji z `error.details`
- Przywrócenie przycisku (`isLoading = false`)

**Komponent**: GameModeSelectionComponent

### Scenariusz 5: Błąd 500 Internal Server Error

**Sytuacja**: Błąd serwera podczas tworzenia gry

**Obsługa**:
- Obsługa w `handleError()`
- Wyświetlenie komunikatu: "Nie udało się utworzyć gry. Spróbuj ponownie."
- Przywrócenie przycisku (`isLoading = false`)
- Możliwość ponowienia próby

**Komponent**: GameModeSelectionComponent

### Scenariusz 6: Błąd sieciowy

**Sytuacja**: Brak połączenia z serwerem lub timeout

**Obsługa**:
- Obsługa w `handleError()`
- Wyświetlenie komunikatu: "Brak połączenia z serwerem. Sprawdź połączenie internetowe."
- Przywrócenie przycisku (`isLoading = false`)
- Możliwość ponowienia próby

**Komponent**: GameModeSelectionComponent

## 11. Kroki implementacji

### Krok 1: Przygotowanie struktury komponentu

**1.1 Utworzenie komponentu głównego**:
- Utworzenie pliku `game-mode-selection.component.ts` w `frontend/src/app/features/game/`
- Utworzenie pliku `game-mode-selection.component.html` (template)
- Utworzenie pliku `game-mode-selection.component.scss` (style)

**1.2 Konfiguracja komponentu**:
- Deklaracja jako standalone component
- Importy: CommonModule, RouterModule, ButtonModule (PrimeNG)
- Konfiguracja routingu w `app.routes.ts`

### Krok 2: Implementacja komponentów pomocniczych

**2.1 Utworzenie BoardSizeCardComponent**:
- Utworzenie pliku `board-size-card.component.ts` w `frontend/src/app/components/game/`
- Implementacja propsów: `size`, `selected`
- Implementacja outputu: `select`
- Stylowanie karty z wizualizacją planszy

**2.2 Utworzenie DifficultyCardComponent**:
- Utworzenie pliku `difficulty-card.component.ts` w `frontend/src/app/components/game/`
- Implementacja propsów: `difficulty`, `selected`
- Implementacja outputu: `select`
- Stylowanie karty z ikoną, opisem i punktacją

### Krok 3: Implementacja logiki komponentu głównego

**3.1 Implementacja stanu komponentu**:
- Definicja zmiennych: `selectedBoardSize`, `selectedDifficulty`, `isLoading`
- Definicja tablic: `boardSizes`, `difficulties`
- Inicjalizacja w `ngOnInit()`: ustawienie domyślnych wartości

**3.2 Implementacja metod obsługi zdarzeń**:
- `onBoardSizeSelect(size)`: aktualizacja `selectedBoardSize`
- `onDifficultySelect(difficultyId)`: aktualizacja `selectedDifficulty`
- `onStartGame()`: walidacja, wywołanie API, obsługa odpowiedzi

**3.3 Implementacja obsługi błędów**:
- `handleError(error)`: obsługa różnych kodów błędów
- Wyświetlanie komunikatów użytkownikowi (MessageService)

### Krok 4: Integracja z GameService

**4.1 Sprawdzenie istniejącego GameService**:
- Weryfikacja czy `GameService` istnieje
- Sprawdzenie metody `createGame()`

**4.2 Implementacja metody createGame w GameService** (jeśli nie istnieje):
```typescript
createGame(request: CreateGameRequest): Observable<CreateGameResponse> {
  return this.http.post<CreateGameResponse>(
    `${this.apiUrl}/games`,
    request,
    { headers: this.getAuthHeaders() }
  );
}
```

**4.3 Integracja w komponencie**:
- Wstrzyknięcie `GameService` w konstruktorze
- Wywołanie `createGame()` w `onStartGame()`
- Obsługa odpowiedzi i błędów

### Krok 5: Implementacja template

**5.1 Struktura HTML**:
- Kontener główny z klasą `.mode-selection-container`
- Sekcja nagłówka z tytułem i podtytułem
- Sekcja wyboru rozmiaru planszy z kartami
- Sekcja wyboru poziomu trudności z kartami
- Sekcja akcji z przyciskiem

**5.2 Integracja komponentów pomocniczych**:
- Użycie `*ngFor` do iteracji po `boardSizes` i `difficulties`
- Przekazanie propsów do `BoardSizeCardComponent` i `DifficultyCardComponent`
- Obsługa eventów `select`

**5.3 Integracja PrimeNG**:
- Użycie `p-button` dla przycisku rozpoczęcia gry
- Konfiguracja `[disabled]` i `[loading]`

### Krok 6: Implementacja stylów

**6.1 Stylowanie kontenera głównego**:
- Maksymalna szerokość, wyśrodkowanie, padding
- Responsywność dla różnych rozdzielczości

**6.2 Stylowanie sekcji wyboru**:
- Grid layout dla kart (responsive)
- Odstępy między kartami
- Stylowanie nagłówków sekcji

**6.3 Stylowanie kart**:
- Stylowanie BoardSizeCardComponent (hover, selected)
- Stylowanie DifficultyCardComponent (hover, selected)
- Animacje przejść (CSS transitions)

**6.4 Stylowanie przycisku**:
- Wyśrodkowanie, marginesy
- Stylowanie stanu disabled i loading

### Krok 7: Implementacja animacji

**7.1 Animacje Angular**:
- Fade-in dla sekcji wyboru
- Scale animation dla kart przy hover
- Smooth transitions dla zaznaczenia

**7.2 CSS Transitions**:
- Transition dla zmiany stanu selected
- Transition dla hover na kartach

### Krok 8: Implementacja obsługi błędów

**8.1 Obsługa błędów API**:
- Implementacja `handleError()` z obsługą różnych kodów błędów
- Wyświetlanie komunikatów przez MessageService (PrimeNG Toast)

**8.2 Walidacja po stronie klienta**:
- Sprawdzenie warunków przed wywołaniem API
- Wyświetlanie komunikatów walidacji

### Krok 9: Testy jednostkowe

**9.1 Testy komponentu głównego**:
- Test inicjalizacji z domyślnymi wartościami
- Test wyboru rozmiaru planszy
- Test wyboru poziomu trudności
- Test rozpoczęcia gry (pomyślny przypadek)
- Test obsługi błędów API

**9.2 Testy komponentów pomocniczych**:
- Test BoardSizeCardComponent (props, events)
- Test DifficultyCardComponent (props, events)

### Krok 10: Testy E2E (Cypress)

**10.1 Scenariusz: Wybór trybu vs bot (łatwy poziom)**:
- Wejście na `/game/mode-selection`
- Wybór rozmiaru 3x3
- Wybór poziomu łatwy
- Kliknięcie "Rozpocznij grę"
- Weryfikacja przekierowania do `/game/{gameId}`

**10.2 Scenariusz: Wybór trybu vs bot (średni poziom)**:
- Analogicznie dla poziomu średni

**10.3 Scenariusz: Wybór trybu vs bot (trudny poziom)**:
- Analogicznie dla poziomu trudny

**10.4 Scenariusz: Walidacja wyboru**:
- Próba rozpoczęcia gry bez wyboru
- Weryfikacja komunikatu błędu

### Krok 11: Dokumentacja i code review

**11.1 Dokumentacja**:
- Aktualizacja README z informacjami o widoku
- Komentarze w kodzie (jeśli wymagane)

**11.2 Code review**:
- Sprawdzenie zgodności z zasadami implementacji
- Weryfikacja obsługi błędów
- Sprawdzenie responsywności i animacji

### Krok 12: Wdrożenie i weryfikacja

**12.1 Wdrożenie**:
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Test integracji z API

**12.2 Weryfikacja**:
- Test wszystkich scenariuszy użytkownika (US-004, US-005, US-006)
- Weryfikacja responsywności
- Weryfikacja animacji i przejść

