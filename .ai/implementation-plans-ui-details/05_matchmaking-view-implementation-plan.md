# Plan implementacji widoku MatchmakingComponent

> **Źródło**: `.ai/implementation-plans-ui/05_matchmaking-component.md`

## 1. Przegląd

MatchmakingComponent to widok wyświetlający stan oczekiwania na przeciwnika w kolejce matchmakingu PvP. Komponent automatycznie dołącza użytkownika do kolejki przy inicjalizacji (na podstawie rozmiaru planszy przekazanego w query params) i przekierowuje do widoku gry po znalezieniu przeciwnika.

Główne funkcjonalności:
- Automatyczne dołączenie do kolejki matchmakingu przy inicjalizacji komponentu
- Wyświetlanie informacji o rozmiarze planszy i szacowanym czasie oczekiwania
- Animacja ładowania i wskaźnik postępu podczas oczekiwania
- Możliwość anulowania kolejki i powrotu do strony głównej
- Automatyczne przekierowanie do widoku gry po znalezieniu przeciwnika
- Polling statusu matchmakingu (co 2 sekundy) lub obsługa WebSocket notifications
- Obsługa błędów API (409 Conflict, 401 Unauthorized, 500 Internal Server Error)

Komponent realizuje historyjkę użytkownika: US-007 (Dołączenie do gry PvP).

## 2. Routing widoku

**Ścieżka routingu**: `/game/matchmaking`

**Konfiguracja routingu**:
```typescript
{
  path: 'game/matchmaking',
  component: MatchmakingComponent
}
```

**Lokalizacja pliku routingu**: `frontend/src/app/app.routes.ts`

**Query parameters**:
- `boardSize` (opcjonalny, domyślnie 3): rozmiar planszy (3, 4, 5)
- Przykład: `/game/matchmaking?boardSize=3`

**Guardy**: Wymagana autoryzacja (AuthGuard) - użytkownik musi być zalogowany (gość lub zarejestrowany)

## 3. Struktura komponentów

```
MatchmakingComponent (główny komponent)
├── ProgressSpinnerModule (PrimeNG - animacja ładowania)
├── ButtonModule (PrimeNG - przycisk anulowania)
├── RouterModule (nawigacja)
└── CommonModule (directives, async pipe)
```

**Hierarchia komponentów**:
- MatchmakingComponent jest komponentem standalone
- Komponent używa PrimeNG do elementów UI (ProgressSpinner, Button)
- Komponent używa RxJS Observables i BehaviorSubject do zarządzania stanem
- Komponent używa Angular Router do nawigacji

## 4. Szczegóły komponentów

### MatchmakingComponent

**Opis komponentu**: Główny komponent widoku matchmakingu, zarządza dołączeniem do kolejki, pollingiem statusu, wyświetlaniem informacji o oczekiwaniu oraz anulowaniem kolejki. Komponent automatycznie dołącza użytkownika do kolejki przy inicjalizacji i przekierowuje do widoku gry po znalezieniu przeciwnika.

**Główne elementy HTML**:
- Kontener główny (`.matchmaking-container`)
- Karta matchmakingu (`.matchmaking-card`)
- Nagłówek z tytułem (`<h2>Szukanie przeciwnika...</h2>`)
- Sekcja informacji (`.matchmaking-info`)
  - Informacja o rozmiarze planszy (`{{ boardSize }}x{{ boardSize }}`)
  - Szacowany czas oczekiwania (`{{ estimatedWaitTime$ | async }}`)
- Sekcja animacji (`.matchmaking-animation`)
  - ProgressSpinner (PrimeNG)
  - Tekst statusu (`{{ statusText$ | async }}`)
- Sekcja akcji (`.matchmaking-actions`)
  - Przycisk anulowania (`<p-button label="Anuluj" (onClick)="onCancel()">`)

**Obsługiwane zdarzenia**:
- `ngOnInit()` - inicjalizacja komponentu, pobranie boardSize z query params, dołączenie do kolejki
- `ngOnDestroy()` - czyszczenie subskrypcji (polling, timer)
- `onCancel()` - obsługa anulowania kolejki, opuszczenie kolejki i przekierowanie do home
- `joinQueue()` - dołączenie do kolejki matchmakingu (private)
- `startPolling()` - rozpoczęcie polling statusu matchmakingu (private)
- `checkMatchmakingStatus()` - sprawdzenie statusu matchmakingu przez polling (private)
- `startWaitTimeCounter()` - rozpoczęcie odliczania szacowanego czasu oczekiwania (private)
- `handleError(error: HttpErrorResponse)` - obsługa błędów API (private)
- `navigateToGame(gameId: number)` - przekierowanie do widoku gry (private)

**Obsługiwana walidacja**:
- Walidacja boardSize z query params (3, 4, 5) - domyślnie 3 jeśli nieprawidłowy
- Sprawdzenie czy użytkownik jest zalogowany (przed dołączeniem do kolejki)
- Walidacja odpowiedzi API:
  - 409 Conflict - użytkownik już jest w kolejce lub ma aktywną grę PvP
  - 401 Unauthorized - brak lub nieprawidłowy token JWT
  - 500 Internal Server Error - błąd serwera (np. problem z Redis)

**Typy**:
- `BoardSize` - typ reprezentujący rozmiar planszy (3 | 4 | 5)
- `MatchmakingQueueRequest` - DTO dla żądania dołączenia do kolejki
- `MatchmakingQueueResponse` - DTO dla odpowiedzi z API (dołączenie do kolejki)
- `LeaveQueueResponse` - DTO dla odpowiedzi z API (opuszczenie kolejki)
- `MatchmakingStatusResponse` - DTO dla odpowiedzi z API (status matchmakingu)
- `BehaviorSubject<number>` - Observable z szacowanym czasem oczekiwania
- `BehaviorSubject<string>` - Observable z tekstem statusu
- `BehaviorSubject<boolean>` - Observable ze stanem anulowania
- `Subscription` - subskrypcje dla polling i timera

**Propsy**: Brak (komponent główny, nie przyjmuje propsów)

## 5. Typy

### BoardSize

```typescript
type BoardSize = 3 | 4 | 5;
```

**Wartości**:
- `3` - plansza 3x3
- `4` - plansza 4x4
- `5` - plansza 5x5

**Uwagi**:
- Typ używany do reprezentacji rozmiaru planszy w całej aplikacji
- Walidowany w komponencie (domyślnie 3 jeśli nieprawidłowy)

### MatchmakingQueueRequest (DTO dla żądania)

```typescript
interface MatchmakingQueueRequest {
  boardSize: 'THREE' | 'FOUR' | 'FIVE';
}
```

**Pola**:
- `boardSize: 'THREE' | 'FOUR' | 'FIVE'` - rozmiar planszy w formacie enum (wymagane)

**Uwagi**:
- DTO używane do wysłania żądania do endpointu POST /api/v1/matching/queue
- Mapowanie z typu `BoardSize` (3 | 4 | 5) na enum backendowy ('THREE' | 'FOUR' | 'FIVE')
- Walidacja po stronie serwera: `@NotNull(message = "Board size is required")`

### MatchmakingQueueResponse (DTO dla odpowiedzi)

```typescript
interface MatchmakingQueueResponse {
  message: string;
  estimatedWaitTime: number | null;
}
```

**Pola**:
- `message: string` - komunikat potwierdzenia (np. "Successfully added to queue")
- `estimatedWaitTime: number | null` - szacowany czas oczekiwania w sekundach (null jeśli nie można oszacować)

**Uwagi**:
- DTO zwracane przez endpoint POST /api/v1/matching/queue po pomyślnym dołączeniu do kolejki
- `estimatedWaitTime` jest używane do inicjalizacji odliczania czasu w komponencie
- Jeśli `estimatedWaitTime` jest null, wyświetlany jest ogólny komunikat "Szukanie przeciwnika..."

### LeaveQueueResponse (DTO dla odpowiedzi)

```typescript
interface LeaveQueueResponse {
  message: string;
}
```

**Pola**:
- `message: string` - komunikat potwierdzenia (np. "Successfully removed from queue")

**Uwagi**:
- DTO zwracane przez endpoint DELETE /api/v1/matching/queue po pomyślnym opuszczeniu kolejki
- Używane do wyświetlenia komunikatu informacyjnego użytkownikowi

### MatchmakingStatusResponse (DTO dla odpowiedzi)

```typescript
interface MatchmakingStatusResponse {
  gameId: number | null;
  status: 'WAITING' | 'MATCHED' | 'PLAYING';
  matchedWith: number | null;
  matchedWithUsername: string | null;
}
```

**Pola**:
- `gameId: number | null` - ID gry (null jeśli gracz jeszcze nie został zmapowany)
- `status: 'WAITING' | 'MATCHED' | 'PLAYING'` - status matchmakingu
- `matchedWith: number | null` - ID zmapowanego przeciwnika (null jeśli brak)
- `matchedWithUsername: string | null` - nazwa użytkownika zmapowanego przeciwnika (null jeśli brak)

**Uwagi**:
- DTO zwracane przez endpoint GET /api/v1/matching/queue (dla aktualnego użytkownika)
- Używane do sprawdzenia czy przeciwnik został znaleziony
- Jeśli `gameId` nie jest null, następuje przekierowanie do widoku gry

### ApiErrorResponse (DTO dla błędów API)

```typescript
interface ApiErrorResponse {
  error: {
    code: string;
    message: string;
    details: Record<string, string> | null;
  };
  timestamp: string;
  path: string;
}
```

**Pola**:
- `error.code: string` - kod błędu (np. "USER_ALREADY_IN_QUEUE", "USER_HAS_ACTIVE_GAME")
- `error.message: string` - komunikat błędu
- `error.details: Record<string, string> | null` - szczegóły błędu (null jeśli brak)
- `timestamp: string` - znacznik czasu błędu (ISO 8601)
- `path: string` - ścieżka endpointu, który zwrócił błąd

**Uwagi**:
- DTO używane do obsługi błędów API
- Mapowane z `HttpErrorResponse` przez metodę `handleError()`
- Używane do wyświetlenia komunikatu błędu użytkownikowi (toast notification)

## 6. Zarządzanie stanem

**Strategia zarządzania stanem**: RxJS Observables + BehaviorSubject + Subscriptions

**Stan komponentu**:
- `boardSize: BoardSize` - rozmiar planszy (3 | 4 | 5), inicjalizowany z query params
- `estimatedWaitTime$: BehaviorSubject<number>` - szacowany czas oczekiwania w sekundach
- `statusText$: BehaviorSubject<string>` - tekst statusu wyświetlany użytkownikowi
- `isCancelling$: BehaviorSubject<boolean>` - flaga wskazująca czy trwa anulowanie kolejki
- `pollingSubscription?: Subscription` - subskrypcja dla polling statusu matchmakingu
- `waitTimeSubscription?: Subscription` - subskrypcja dla odliczania czasu oczekiwania
- `matchmakingSubscription?: Subscription` - subskrypcja dla operacji matchmakingu

**Custom hooki**: Brak (komponent używa standardowych Observable i BehaviorSubject)

**Subskrypcje**:
- Subskrypcja do `estimatedWaitTime$` w template przez `async` pipe
- Subskrypcja do `statusText$` w template przez `async` pipe
- Subskrypcja do `isCancelling$` w template przez `async` pipe
- Subskrypcja do `pollingSubscription` - polling statusu matchmakingu co 2 sekundy
- Subskrypcja do `waitTimeSubscription` - odliczanie czasu oczekiwania co 1 sekundę
- Wszystkie subskrypcje są czyszczone w `ngOnDestroy()`

**Przepływ stanu**:
1. `ngOnInit()` → pobranie `boardSize` z query params → `joinQueue()`
2. `joinQueue()` → wywołanie API → aktualizacja `estimatedWaitTime$` → `startPolling()` + `startWaitTimeCounter()`
3. `startPolling()` → utworzenie subskrypcji → `checkMatchmakingStatus()` co 2 sekundy
4. `checkMatchmakingStatus()` → wywołanie API → jeśli `gameId` nie null → `navigateToGame()`
5. `startWaitTimeCounter()` → odliczanie `estimatedWaitTime$` co 1 sekundę
6. `onCancel()` → aktualizacja `isCancelling$` → wywołanie API → przekierowanie do home

## 7. Integracja API

### 7.1 Endpoint: POST /api/v1/matching/queue

**Opis**: Dołączenie do kolejki matchmakingu PvP dla wybranego rozmiaru planszy.

**Request**:
```typescript
POST /api/v1/matching/queue
Headers: {
  Authorization: 'Bearer <JWT_TOKEN>',
  Content-Type: 'application/json'
}
Body: {
  boardSize: 'THREE' | 'FOUR' | 'FIVE'
}
```

**Response (200 OK)**:
```typescript
{
  message: string;
  estimatedWaitTime: number | null;
}
```

**Obsługa błędów**:
- **400 Bad Request** - Nieprawidłowy parametr boardSize
- **401 Unauthorized** - Brak lub nieprawidłowy token JWT
- **409 Conflict** - Użytkownik już jest w kolejce lub ma aktywną grę PvP
- **500 Internal Server Error** - Błąd serwera (np. problem z Redis)

**Użycie w komponencie**:
- Wywoływane w `joinQueue()` podczas inicjalizacji komponentu
- Mapowanie `boardSize` z typu `3 | 4 | 5` na enum `'THREE' | 'FOUR' | 'FIVE'`
- Po pomyślnym dołączeniu: aktualizacja `estimatedWaitTime$`, rozpoczęcie polling i odliczania czasu
- W przypadku błędu: wyświetlenie komunikatu błędu i przekierowanie do home

### 7.2 Endpoint: DELETE /api/v1/matching/queue

**Opis**: Opuszczenie kolejki matchmakingu PvP.

**Request**:
```typescript
DELETE /api/v1/matching/queue
Headers: {
  Authorization: 'Bearer <JWT_TOKEN>'
}
```

**Response (200 OK)**:
```typescript
{
  message: string;
}
```

**Obsługa błędów**:
- **401 Unauthorized** - Brak lub nieprawidłowy token JWT
- **404 Not Found** - Użytkownik nie znajduje się w kolejce
- **500 Internal Server Error** - Błąd serwera (np. problem z Redis)

**Użycie w komponencie**:
- Wywoływane w `onCancel()` po kliknięciu przycisku anulowania
- Przed wywołaniem: aktualizacja `isCancelling$` na `true`
- Po pomyślnym opuszczeniu: wyświetlenie komunikatu informacyjnego i przekierowanie do home
- W przypadku błędu: wyświetlenie komunikatu błędu i przywrócenie `isCancelling$` na `false`

### 7.3 Endpoint: GET /api/v1/matching/queue

**Opis**: Pobranie statusu matchmakingu dla aktualnego użytkownika (polling).

**Request**:
```typescript
GET /api/v1/matching/queue
Headers: {
  Authorization: 'Bearer <JWT_TOKEN>'
}
```

**Response (200 OK)**:
```typescript
{
  gameId: number | null;
  status: 'WAITING' | 'MATCHED' | 'PLAYING';
  matchedWith: number | null;
  matchedWithUsername: string | null;
}
```

**Obsługa błędów**:
- **401 Unauthorized** - Brak lub nieprawidłowy token JWT
- **500 Internal Server Error** - Błąd serwera

**Użycie w komponencie**:
- Wywoływane w `checkMatchmakingStatus()` podczas polling (co 2 sekundy)
- Sprawdzenie czy `gameId` nie jest null - jeśli nie null, przekierowanie do widoku gry
- W przypadku błędu: logowanie błędu (bez wyświetlania użytkownikowi, aby nie przeszkadzać)

### 7.4 Serwisy

**MatchmakingService**:
```typescript
class MatchmakingService {
  joinQueue(boardSize: BoardSize): Observable<MatchmakingQueueResponse>;
  leaveQueue(): Observable<LeaveQueueResponse>;
  getMatchmakingStatus(): Observable<MatchmakingStatusResponse>;
}
```

**Uwagi**:
- Serwis obsługuje wszystkie operacje matchmakingu
- Używa `HttpClient` do komunikacji z API
- Obsługuje automatyczne dodawanie nagłówka `Authorization` (przez interceptor)
- Zwraca `Observable` dla każdej operacji
- Obsługuje błędy HTTP przez `catchError` operator

## 8. Interakcje użytkownika

### 8.1 Dołączenie do kolejki (automatyczne)

**Scenariusz**: Użytkownik wchodzi na stronę `/game/matchmaking?boardSize=3`

**Kroki**:
1. Komponent inicjalizuje się (`ngOnInit()`)
2. Pobranie `boardSize` z query params (domyślnie 3 jeśli brak)
3. Automatyczne wywołanie `joinQueue()` z `boardSize`
4. Wyświetlenie animacji ładowania i komunikatu "Szukanie przeciwnika..."
5. Po pomyślnym dołączeniu: rozpoczęcie polling i odliczania czasu
6. Wyświetlenie szacowanego czasu oczekiwania (jeśli dostępny)

**Oczekiwane rezultaty**:
- Użytkownik jest dodany do kolejki matchmakingu
- Wyświetlana jest animacja ładowania i informacje o oczekiwaniu
- Rozpoczyna się polling statusu matchmakingu
- Odliczanie czasu oczekiwania (jeśli dostępny)

**Obsługa błędów**:
- Błąd 409: Wyświetlenie komunikatu "Jesteś już w kolejce" lub "Masz aktywną grę PvP"
- Błąd 401: Przekierowanie do strony logowania
- Błąd 500: Wyświetlenie komunikatu "Błąd serwera" i przekierowanie do home

### 8.2 Anulowanie kolejki

**Scenariusz**: Użytkownik klika przycisk "Anuluj" podczas oczekiwania

**Kroki**:
1. Użytkownik klika przycisk "Anuluj"
2. Wywołanie `onCancel()`
3. Aktualizacja `isCancelling$` na `true` (wyłączenie przycisku)
4. Wywołanie API `leaveQueue()`
5. Po pomyślnym opuszczeniu: wyświetlenie komunikatu "Opuszczono kolejkę"
6. Przekierowanie do strony głównej (`/`)

**Oczekiwane rezultaty**:
- Użytkownik jest usunięty z kolejki matchmakingu
- Wyświetlany jest komunikat informacyjny
- Następuje przekierowanie do strony głównej
- Wszystkie subskrypcje (polling, timer) są czyszczone

**Obsługa błędów**:
- Błąd 404: Wyświetlenie komunikatu "Nie jesteś w kolejce" i przekierowanie do home
- Błąd 401: Przekierowanie do strony logowania
- Błąd 500: Wyświetlenie komunikatu "Błąd serwera" i przywrócenie przycisku

### 8.3 Znalezienie przeciwnika (automatyczne przekierowanie)

**Scenariusz**: System znajduje przeciwnika podczas polling statusu matchmakingu

**Kroki**:
1. Polling wywołuje `checkMatchmakingStatus()` co 2 sekundy
2. API zwraca `MatchmakingStatusResponse` z `gameId` nie null
3. Wywołanie `navigateToGame(gameId)`
4. Przekierowanie do `/game/{gameId}`
5. Czyszczenie wszystkich subskrypcji (polling, timer)

**Oczekiwane rezultaty**:
- Użytkownik jest przekierowany do widoku gry
- Wszystkie subskrypcje są czyszczone
- Widok gry nawiązuje połączenie WebSocket i rozpoczyna rozgrywkę

**Obsługa błędów**:
- Błąd 401: Przekierowanie do strony logowania
- Błąd 500: Logowanie błędu (bez wyświetlania użytkownikowi, polling kontynuowany)

### 8.4 Zmiana rozmiaru planszy (query params)

**Scenariusz**: Użytkownik zmienia query param `boardSize` podczas przebywania na stronie

**Kroki**:
1. Subskrypcja do `route.queryParams` w `ngOnInit()`
2. Wykrycie zmiany `boardSize` w query params
3. Opuszczenie poprzedniej kolejki (jeśli była)
4. Dołączenie do nowej kolejki z nowym `boardSize`

**Oczekiwane rezultaty**:
- Użytkownik jest przenoszony do kolejki dla nowego rozmiaru planszy
- Wyświetlane są zaktualizowane informacje o rozmiarze planszy
- Rozpoczyna się nowy polling i odliczanie czasu

**Obsługa błędów**:
- Nieprawidłowy `boardSize`: użycie domyślnego (3) i wyświetlenie komunikatu

## 9. Warunki i walidacja

### 9.1 Walidacja boardSize

**Warunek**: `boardSize` musi być wartością 3, 4 lub 5

**Weryfikacja**:
- W komponencie: sprawdzenie wartości z query params
- Jeśli wartość nie jest 3, 4 lub 5: użycie domyślnej wartości 3
- Mapowanie na enum backendowy: `3 → 'THREE'`, `4 → 'FOUR'`, `5 → 'FIVE'`

**Wpływ na stan**:
- Nieprawidłowa wartość: użycie domyślnej (3) bez wyświetlania błędu
- Prawidłowa wartość: użycie wartości z query params

### 9.2 Walidacja autoryzacji

**Warunek**: Użytkownik musi być zalogowany (gość lub zarejestrowany)

**Weryfikacja**:
- Przed dołączeniem do kolejki: sprawdzenie przez AuthGuard
- Jeśli użytkownik nie jest zalogowany: przekierowanie do strony logowania
- Wywołanie API wymaga tokenu JWT w nagłówku `Authorization`

**Wpływ na stan**:
- Brak autoryzacji: przekierowanie do strony logowania przed wyświetleniem komponentu
- Błąd 401 z API: przekierowanie do strony logowania i wyświetlenie komunikatu błędu

### 9.3 Walidacja statusu użytkownika (409 Conflict)

**Warunek**: Użytkownik nie może być już w kolejce lub mieć aktywnej gry PvP

**Weryfikacja**:
- Po stronie serwera: sprawdzenie czy użytkownik jest w kolejce (Redis)
- Po stronie serwera: sprawdzenie czy użytkownik ma aktywną grę PvP (PostgreSQL)
- Jeśli warunek nie jest spełniony: zwrócenie błędu 409 Conflict

**Wpływ na stan**:
- Użytkownik już w kolejce: wyświetlenie komunikatu "Jesteś już w kolejce" i przekierowanie do home
- Użytkownik ma aktywną grę: wyświetlenie komunikatu "Masz aktywną grę PvP" i przekierowanie do gry

### 9.4 Walidacja odpowiedzi API

**Warunek**: Odpowiedź API musi zawierać wymagane pola

**Weryfikacja**:
- `MatchmakingQueueResponse`: sprawdzenie czy `message` i `estimatedWaitTime` są obecne
- `MatchmakingStatusResponse`: sprawdzenie czy `gameId`, `status` są obecne
- Jeśli pola są nieprawidłowe: traktowanie jako błąd serwera (500)

**Wpływ na stan**:
- Nieprawidłowa odpowiedź: wyświetlenie komunikatu "Błąd serwera" i przekierowanie do home
- Prawidłowa odpowiedź: aktualizacja stanu komponentu (estimatedWaitTime, status)

### 9.5 Walidacja timeout matchmakingu

**Warunek**: Matchmaking nie powinien trwać w nieskończoność

**Weryfikacja**:
- Szacowany czas oczekiwania: jeśli przekroczony, wyświetlenie komunikatu "Długi czas oczekiwania"
- Maksymalny czas oczekiwania: 5 minut (TTL w Redis)
- Po przekroczeniu: automatyczne usunięcie z kolejki przez backend

**Wpływ na stan**:
- Przekroczenie czasu: wyświetlenie komunikatu informacyjnego (bez automatycznego opuszczenia kolejki)
- Automatyczne usunięcie przez backend: przekierowanie do home przy następnym polling

## 10. Obsługa błędów

### 10.1 Błąd dołączenia do kolejki

**Scenariusz**: Błąd podczas wywołania `POST /api/v1/matching/queue`

**Obsługa**:
- **400 Bad Request**: Wyświetlenie komunikatu "Nieprawidłowy rozmiar planszy" i przekierowanie do home
- **401 Unauthorized**: Przekierowanie do strony logowania z komunikatem "Musisz się zalogować"
- **409 Conflict**: 
  - Jeśli użytkownik już w kolejce: wyświetlenie komunikatu "Jesteś już w kolejce" i przekierowanie do home
  - Jeśli użytkownik ma aktywną grę: wyświetlenie komunikatu "Masz aktywną grę PvP" i przekierowanie do gry
- **500 Internal Server Error**: Wyświetlenie komunikatu "Błąd serwera. Spróbuj ponownie później" i przekierowanie do home

**Implementacja**:
```typescript
private handleError(error: HttpErrorResponse): void {
  const errorMessage = error.error?.message || 'Wystąpił błąd podczas matchmakingu';
  
  if (error.status === 401) {
    this.router.navigate(['/auth/login']);
  } else if (error.status === 409) {
    if (error.error?.code === 'USER_HAS_ACTIVE_GAME') {
      const gameId = error.error?.details?.gameId;
      if (gameId) {
        this.router.navigate(['/game', gameId]);
      }
    } else {
      this.router.navigate(['/']);
    }
  } else {
    this.router.navigate(['/']);
  }
  
  this.messageService.add({
    severity: 'error',
    summary: 'Błąd',
    detail: errorMessage
  });
}
```

### 10.2 Błąd anulowania kolejki

**Scenariusz**: Błąd podczas wywołania `DELETE /api/v1/matching/queue`

**Obsługa**:
- **401 Unauthorized**: Przekierowanie do strony logowania
- **404 Not Found**: Wyświetlenie komunikatu "Nie jesteś w kolejce" i przekierowanie do home
- **500 Internal Server Error**: Wyświetlenie komunikatu "Błąd serwera" i przywrócenie przycisku (isCancelling$ = false)

**Implementacja**:
```typescript
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
```

### 10.3 Błąd polling statusu

**Scenariusz**: Błąd podczas wywołania `GET /api/v1/matching/queue` podczas polling

**Obsługa**:
- **401 Unauthorized**: Przekierowanie do strony logowania
- **500 Internal Server Error**: Logowanie błędu (bez wyświetlania użytkownikowi, polling kontynuowany)

**Implementacja**:
```typescript
private checkMatchmakingStatus(): void {
  this.matchmakingService.getMatchmakingStatus().subscribe({
    next: (status) => {
      if (status.gameId) {
        this.navigateToGame(status.gameId);
      }
    },
    error: (error) => {
      if (error.status === 401) {
        this.router.navigate(['/auth/login']);
      } else {
        console.error('Error checking matchmaking status:', error);
      }
    }
  });
}
```

### 10.4 Błąd przekierowania do gry

**Scenariusz**: Błąd podczas przekierowania do widoku gry (np. nieprawidłowy gameId)

**Obsługa**:
- Sprawdzenie czy `gameId` jest prawidłowy (nie null, nie undefined)
- Jeśli nieprawidłowy: logowanie błędu i kontynuacja polling
- Jeśli prawidłowy: przekierowanie do `/game/{gameId}`

**Implementacja**:
```typescript
private navigateToGame(gameId: number): void {
  if (!gameId) {
    console.error('Invalid gameId:', gameId);
    return;
  }
  
  this.cleanupSubscriptions();
  this.router.navigate(['/game', gameId]);
}
```

### 10.5 Timeout matchmakingu

**Scenariusz**: Użytkownik czeka dłużej niż szacowany czas oczekiwania

**Obsługa**:
- Wyświetlenie komunikatu informacyjnego "Długi czas oczekiwania. Możesz anulować i spróbować ponownie później"
- Kontynuacja polling (backend automatycznie usunie użytkownika po 5 minutach)
- Możliwość anulowania kolejki przez użytkownika

**Implementacja**:
```typescript
private startWaitTimeCounter(): void {
  this.waitTimeSubscription = interval(1000).subscribe(() => {
    const currentWaitTime = this.estimatedWaitTime$.value;
    if (currentWaitTime > 0) {
      this.estimatedWaitTime$.next(currentWaitTime - 1);
    } else if (currentWaitTime === 0) {
      this.statusText$.next('Szukanie przeciwnika...');
    }
    
    const elapsedTime = Date.now() - this.joinTime;
    if (elapsedTime > 300000) {
      this.statusText$.next('Długi czas oczekiwania. Możesz anulować i spróbować ponownie później.');
    }
  });
}
```

### 10.6 Błąd sieci/połączenia

**Scenariusz**: Brak połączenia z internetem lub timeout żądania

**Obsługa**:
- Wykrycie błędu sieci (NetworkError, timeout)
- Wyświetlenie komunikatu "Brak połączenia z internetem. Sprawdź połączenie i spróbuj ponownie"
- Możliwość ponowienia próby (odświeżenie strony)

**Implementacja**:
```typescript
private handleError(error: HttpErrorResponse): void {
  if (error.status === 0 || error.error instanceof ProgressEvent) {
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd połączenia',
      detail: 'Brak połączenia z internetem. Sprawdź połączenie i spróbuj ponownie.'
    });
    return;
  }
  
  // ... reszta obsługi błędów
}
```

## 11. Kroki implementacji

### Krok 1: Utworzenie struktury komponentu

1. Utworzenie pliku `matchmaking.component.ts` w `frontend/src/app/features/game/`
2. Utworzenie pliku `matchmaking.component.html` w `frontend/src/app/features/game/`
3. Utworzenie pliku `matchmaking.component.scss` w `frontend/src/app/features/game/`
4. Utworzenie pliku `matchmaking.component.spec.ts` w `frontend/src/app/features/game/`

### Krok 2: Definicja typów i interfejsów

1. Utworzenie pliku `matchmaking.types.ts` w `frontend/src/app/features/game/types/`
2. Zdefiniowanie typów:
   - `BoardSize`
   - `MatchmakingQueueRequest`
   - `MatchmakingQueueResponse`
   - `LeaveQueueResponse`
   - `MatchmakingStatusResponse`
3. Eksport typów z pliku `index.ts`

### Krok 3: Implementacja MatchmakingService

1. Utworzenie pliku `matchmaking.service.ts` w `frontend/src/app/services/`
2. Implementacja metod:
   - `joinQueue(boardSize: BoardSize): Observable<MatchmakingQueueResponse>`
   - `leaveQueue(): Observable<LeaveQueueResponse>`
   - `getMatchmakingStatus(): Observable<MatchmakingStatusResponse>`
3. Dodanie obsługi błędów HTTP przez `catchError`
4. Dodanie automatycznego dodawania nagłówka `Authorization` (przez interceptor)

### Krok 4: Implementacja komponentu TypeScript

1. Deklaracja komponentu jako standalone:
   ```typescript
   @Component({
     selector: 'app-matchmaking',
     standalone: true,
     imports: [CommonModule, RouterModule, AsyncPipe, ButtonModule, ProgressSpinnerModule],
     templateUrl: './matchmaking.component.html',
     styleUrls: ['./matchmaking.component.scss']
   })
   ```

2. Implementacja właściwości komponentu:
   - `boardSize: BoardSize`
   - `estimatedWaitTime$: BehaviorSubject<number>`
   - `statusText$: BehaviorSubject<string>`
   - `isCancelling$: BehaviorSubject<boolean>`
   - Subskrypcje (polling, timer)

3. Implementacja metod:
   - `ngOnInit()`
   - `ngOnDestroy()`
   - `joinQueue()`
   - `onCancel()`
   - `startPolling()`
   - `checkMatchmakingStatus()`
   - `startWaitTimeCounter()`
   - `handleError()`
   - `navigateToGame()`
   - `cleanupSubscriptions()`

### Krok 5: Implementacja template HTML

1. Utworzenie struktury HTML:
   - Kontener główny (`.matchmaking-container`)
   - Karta matchmakingu (`.matchmaking-card`)
   - Nagłówek z tytułem
   - Sekcja informacji (rozmiar planszy, szacowany czas)
   - Sekcja animacji (ProgressSpinner, tekst statusu)
   - Sekcja akcji (przycisk anulowania)

2. Dodanie dyrektyw Angular:
   - `*ngIf` dla warunkowego wyświetlania
   - `async` pipe dla Observable
   - `[disabled]` dla przycisku anulowania

3. Dodanie komponentów PrimeNG:
   - `p-progressSpinner`
   - `p-button`

### Krok 6: Implementacja stylowania SCSS

1. Utworzenie stylów dla `.matchmaking-container`:
   - Centrowanie wizualne (flexbox)
   - Responsywność (padding, max-width)

2. Utworzenie stylów dla `.matchmaking-card`:
   - Karta z cieniem (box-shadow)
   - Zaokrąglone rogi (border-radius)
   - Centrowanie tekstu

3. Utworzenie stylów dla sekcji:
   - `.matchmaking-info` - informacje o planszy i czasie
   - `.matchmaking-animation` - animacja ładowania
   - `.matchmaking-actions` - przyciski akcji

4. Dodanie animacji:
   - Rotating spinner (PrimeNG)
   - Pulse animation dla tekstu statusu
   - Smooth transitions dla przycisków

### Krok 7: Konfiguracja routingu

1. Dodanie routingu w `app.routes.ts`:
   ```typescript
   {
     path: 'game/matchmaking',
     component: MatchmakingComponent,
     canActivate: [AuthGuard]
   }
   ```

2. Konfiguracja AuthGuard (jeśli nie istnieje):
   - Sprawdzenie czy użytkownik jest zalogowany
   - Przekierowanie do `/auth/login` jeśli nie

### Krok 8: Integracja z i18n

1. Dodanie kluczy tłumaczeń w `assets/i18n/pl.json` i `assets/i18n/en.json`:
   - `matchmaking.title`
   - `matchmaking.searching`
   - `matchmaking.boardSize`
   - `matchmaking.estimatedWaitTime`
   - `matchmaking.cancel`
   - `matchmaking.errors.*`

2. Użycie pipe `translate` w template:
   ```html
   <h2>{{ 'matchmaking.title' | translate }}</h2>
   ```

3. Użycie serwisu `TranslateService` w komponencie (opcjonalnie):
   ```typescript
   this.statusText$.next(this.translateService.instant('matchmaking.searching'));
   ```

### Krok 9: Implementacja obsługi błędów

1. Implementacja metody `handleError()`:
   - Obsługa kodów błędów (400, 401, 404, 409, 500)
   - Wyświetlanie komunikatów błędów przez MessageService
   - Przekierowania w zależności od błędu

2. Dodanie obsługi błędów sieci/połączenia:
   - Wykrycie NetworkError
   - Wyświetlanie komunikatu o braku połączenia

3. Dodanie obsługi timeout matchmakingu:
   - Wyświetlanie komunikatu po przekroczeniu czasu

### Krok 10: Implementacja polling i timera

1. Implementacja metody `startPolling()`:
   - Utworzenie subskrypcji z `interval(2000)`
   - Wywołanie `checkMatchmakingStatus()` co 2 sekundy

2. Implementacja metody `checkMatchmakingStatus()`:
   - Wywołanie API `getMatchmakingStatus()`
   - Sprawdzenie czy `gameId` nie jest null
   - Przekierowanie do gry jeśli znaleziono przeciwnika

3. Implementacja metody `startWaitTimeCounter()`:
   - Utworzenie subskrypcji z `interval(1000)`
   - Odliczanie `estimatedWaitTime$` co 1 sekundę
   - Aktualizacja tekstu statusu

### Krok 11: Testy jednostkowe

1. Utworzenie testów w `matchmaking.component.spec.ts`:
   - Test inicjalizacji komponentu
   - Test dołączenia do kolejki
   - Test anulowania kolejki
   - Test przekierowania po znalezieniu przeciwnika
   - Test obsługi błędów
   - Test polling statusu
   - Test odliczania czasu

2. Mockowanie serwisów:
   - `MatchmakingService`
   - `Router`
   - `ActivatedRoute`
   - `MessageService`

### Krok 12: Testy E2E (Cypress)

1. Utworzenie testów w `cypress/e2e/matchmaking.cy.ts`:
   - Scenariusz: Dołączenie do matchmakingu
   - Scenariusz: Anulowanie matchmakingu
   - Scenariusz: Znalezienie przeciwnika i przekierowanie do gry
   - Scenariusz: Obsługa błędów API

2. Konfiguracja testów:
   - Mockowanie API responses
   - Testowanie różnych rozmiarów planszy
   - Testowanie przekierowań

### Krok 13: Dostępność (a11y)

1. Dodanie ARIA labels:
   - `aria-label` dla przycisku anulowania
   - `aria-live` dla tekstu statusu
   - `role="status"` dla sekcji statusu

2. Dodanie screen reader announcements:
   - Ogłoszenie zmiany statusu matchmakingu
   - Ogłoszenie znalezienia przeciwnika

3. Dodanie keyboard navigation:
   - Obsługa klawisza Enter dla przycisku anulowania
   - Focus indicators dla przycisków

### Krok 14: Optymalizacja i wydajność

1. Optymalizacja subskrypcji:
   - Używanie `async` pipe w template (automatyczne unsubscribe)
   - Ręczne czyszczenie subskrypcji w `ngOnDestroy()`

2. Optymalizacja polling:
   - Zatrzymanie polling po znalezieniu przeciwnika
   - Zatrzymanie polling po anulowaniu kolejki

3. Optymalizacja renderowania:
   - Używanie `OnPush` change detection strategy (opcjonalnie)
   - Memoization dla obliczeń (opcjonalnie)

### Krok 15: Dokumentacja i code review

1. Dodanie komentarzy JSDoc dla metod publicznych
2. Sprawdzenie zgodności z zasadami projektu (ESLint, Prettier)
3. Code review i refaktoryzacja
4. Aktualizacja dokumentacji projektu

