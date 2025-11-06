# Plan implementacji widoku AuthLoginComponent

> **Źródło**: `.ai/implementation-plans-ui/02_auth-login-component.md`

## 1. Przegląd

AuthLoginComponent to widok umożliwiający zarejestrowanym użytkownikom zalogowanie się do systemu. Komponent obsługuje formularz logowania z walidacją po stronie klienta i serwera, integrację z API backendu oraz odpowiednią obsługę błędów i przekierowań po pomyślnym logowaniu.

Główne funkcjonalności:
- Formularz logowania z polami email i hasło
- Walidacja po stronie klienta (reactive forms)
- Integracja z endpointem POST /api/auth/login
- Obsługa błędów API (401, 404, 422, 500)
- Przekierowanie po pomyślnym logowaniu (do zapisanej gry lub strony głównej)
- Sprawdzenie czy użytkownik jest już zalogowany (przekierowanie)
- Linki nawigacyjne do rejestracji i trybu gościa

Komponent realizuje historyjkę użytkownika: US-003 (Logowanie zarejestrowanego użytkownika).

## 2. Routing widoku

**Ścieżka routingu**: `/auth/login`

**Konfiguracja routingu**:
```typescript
{
  path: 'auth',
  component: AuthComponent,
  children: [
    { path: 'login', component: AuthLoginComponent },
    { path: 'register', component: AuthRegisterComponent }
  ]
}
```

**Lokalizacja pliku routingu**: `frontend/src/app/app.routes.ts`

**Guardy**: Brak (widok publiczny, ale sprawdza czy użytkownik jest już zalogowany i przekierowuje)

## 3. Struktura komponentów

```
AuthLoginComponent (główny komponent)
├── FormGroup (reactive form)
│   ├── FormControl (email)
│   └── FormControl (password)
├── InputTextModule (PrimeNG - pole email)
├── PasswordModule (PrimeNG - pole hasła)
├── ButtonModule (PrimeNG - przycisk submit)
└── RouterModule (nawigacja)
```

**Hierarchia komponentów**:
- AuthLoginComponent jest komponentem standalone
- Komponent używa PrimeNG do elementów UI (InputText, Password, Button)
- Komponent używa Angular Reactive Forms do zarządzania formularzem
- Komponent używa Angular Router do nawigacji

## 4. Szczegóły komponentów

### AuthLoginComponent

**Opis komponentu**: Główny komponent widoku logowania, zarządza formularzem logowania, walidacją, integracją z API oraz obsługą błędów i przekierowań. Komponent sprawdza czy użytkownik jest już zalogowany i przekierowuje do strony głównej jeśli tak.

**Główne elementy HTML**:
- Kontener główny (`.auth-container`)
- Karta formularza (`.auth-card`)
- Nagłówek z tytułem (`<h2>Logowanie</h2>`)
- Formularz (`<form [formGroup]="loginForm" (ngSubmit)="onSubmit()">`)
  - Pole email (`<input pInputText formControlName="email">`)
  - Pole hasło (`<p-password formControlName="password">`)
  - Przycisk submit (`<p-button type="submit">`)
- Sekcja linków nawigacyjnych (`.auth-links`)
  - Link do rejestracji (`<a routerLink="/auth/register">`)
  - Link do trybu gościa (`<a routerLink="/">`)

**Obsługiwane zdarzenia**:
- `ngOnInit()` - inicjalizacja komponentu, sprawdzenie czy użytkownik jest już zalogowany
- `onSubmit()` - obsługa submit formularza, walidacja i wywołanie API
- `isFieldInvalid(fieldName: string)` - sprawdzenie czy pole jest nieprawidłowe
- `getFieldError(fieldName: string)` - pobranie komunikatu błędu dla pola
- `markFormGroupTouched(formGroup: FormGroup)` - oznaczenie wszystkich pól jako touched
- `redirectAfterLogin()` - przekierowanie po pomyślnym logowaniu
- `handleLoginError(error: HttpErrorResponse)` - obsługa błędów API

**Obsługiwana walidacja**:
- **Email**: 
  - Wymagane (`Validators.required`)
  - Format email (`Validators.email`)
- **Hasło**: 
  - Wymagane (`Validators.required`)
  - Minimalna długość 8 znaków (`Validators.minLength(8)`)
- Walidacja po stronie serwera (API):
  - 422 Unprocessable Entity - błędy walidacji Bean Validation
  - 401 Unauthorized - nieprawidłowe dane uwierzytelniające
  - 404 Not Found - użytkownik nie znaleziony

**Typy**:
- `LoginRequest` - DTO dla żądania logowania
- `LoginResponse` - DTO dla odpowiedzi z API
- `User` - interfejs reprezentujący użytkownika
- `FormGroup` - Angular Reactive Forms
- `FormControl` - Angular Reactive Forms
- `HttpErrorResponse` - Angular HTTP error response
- `Observable<LoginResponse>` - Observable z odpowiedzią API
- `Observable<Game | null>` - Observable z zapisaną grą (dla przekierowania)

**Propsy**: Brak (komponent główny, nie przyjmuje propsów)

## 5. Typy

### LoginRequest (DTO dla żądania)

```typescript
interface LoginRequest {
  email: string;
  password: string;
}
```

**Pola**:
- `email: string` - adres email użytkownika (wymagane, format email)
- `password: string` - hasło użytkownika (wymagane, min. 8 znaków)

**Uwagi**:
- DTO używane do wysłania żądania do endpointu POST /api/auth/login
- Walidacja po stronie klienta: `Validators.required`, `Validators.email`, `Validators.minLength(8)`
- Walidacja po stronie serwera: `@NotBlank`, `@Email` (Bean Validation)

### LoginResponse (DTO dla odpowiedzi)

```typescript
interface LoginResponse {
  userId: string;
  username: string;
  email: string;
  isGuest: boolean;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  authToken: string;
}
```

**Pola**:
- `userId: string` - unikalny identyfikator użytkownika (UUID lub BIGINT jako string)
- `username: string` - nazwa użytkownika
- `email: string` - adres email użytkownika
- `isGuest: boolean` - flaga wskazująca czy użytkownik jest gościem (zawsze false dla zarejestrowanych)
- `totalPoints: number` - suma punktów użytkownika
- `gamesPlayed: number` - liczba rozegranych gier
- `gamesWon: number` - liczba wygranych gier
- `authToken: string` - token JWT do uwierzytelniania kolejnych żądań

**Uwagi**:
- DTO zwracane przez endpoint POST /api/auth/login po pomyślnym logowaniu
- `authToken` jest zapisywany w localStorage przez AuthService
- `userId`, `username`, `email`, `totalPoints`, `gamesPlayed`, `gamesWon` są używane do aktualizacji stanu użytkownika w AuthService

### User (interfejs użytkownika)

```typescript
interface User {
  userId: string;
  username: string;
  email: string;
  isGuest: boolean;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt?: string;
  lastSeenAt?: string | null;
}
```

**Pola**:
- `userId: string` - unikalny identyfikator użytkownika
- `username: string` - nazwa użytkownika
- `email: string` - adres email użytkownika
- `isGuest: boolean` - flaga wskazująca czy użytkownik jest gościem
- `totalPoints: number` - suma punktów użytkownika
- `gamesPlayed: number` - liczba rozegranych gier
- `gamesWon: number` - liczba wygranych gier
- `createdAt?: string` - data utworzenia konta (opcjonalne)
- `lastSeenAt?: string | null` - data ostatniej aktywności (opcjonalne)

**Uwagi**:
- Interfejs używany do reprezentacji użytkownika w aplikacji frontendowej
- Mapowany z `LoginResponse` przez AuthService
- Używany do aktualizacji stanu użytkownika w AuthService

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
- `error.code: string` - kod błędu (np. "UNAUTHORIZED", "USER_NOT_FOUND", "VALIDATION_ERROR")
- `error.message: string` - komunikat błędu
- `error.details: Record<string, string> | null` - szczegóły błędów walidacji (dla 422) lub null
- `timestamp: string` - timestamp błędu (ISO 8601)
- `status: 'error'` - status odpowiedzi (zawsze "error")

**Uwagi**:
- DTO zwracane przez API w przypadku błędów (401, 404, 422, 500)
- Używane do wyświetlania komunikatów błędów użytkownikowi
- `error.details` zawiera mapę błędów walidacji dla 422 Unprocessable Entity

### Game (interfejs gry - dla przekierowania)

```typescript
interface Game {
  gameId: number;
  gameType: 'vs_bot' | 'pvp';
  boardSize: 3 | 4 | 5;
  status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw';
  player1Id: number;
  player2Id: number | null;
  botDifficulty: 'easy' | 'medium' | 'hard' | null;
  currentPlayerSymbol: 'x' | 'o' | null;
  winnerId: number | null;
  lastMoveAt: string | null;
  createdAt: string;
  updatedAt: string;
  finishedAt: string | null;
  totalMoves: number;
}
```

**Uwagi**:
- Interfejs używany do reprezentacji zapisanej gry
- Używany w `redirectAfterLogin()` do przekierowania do zapisanej gry po logowaniu
- Pobierany z endpointu GET /api/games?status=in_progress&size=1 przez GameService

## 6. Zarządzanie stanem

**Strategia zarządzania stanem**: Reactive Forms + RxJS Observables + BehaviorSubject (w AuthService)

**Stan komponentu**:
- `loginForm: FormGroup` - reactive form z polami email i password
- `isLoading: boolean` - flaga wskazująca czy trwa proces logowania
- `loginForm.get('email')` - FormControl dla pola email
- `loginForm.get('password')` - FormControl dla pola password

**Stan w AuthService** (współdzielony):
- `currentUser$: BehaviorSubject<User | null>` - aktualny użytkownik (null jeśli nie zalogowany)
- `authToken$: BehaviorSubject<string | null>` - token JWT (null jeśli nie zalogowany)
- `isAuthenticated$: Observable<boolean>` - Observable ze statusem uwierzytelnienia

**Custom hooki**: Brak (komponent używa standardowych Observable i Reactive Forms)

**Subskrypcje**:
- Subskrypcja do `authService.login()` w `onSubmit()` dla wywołania API
- Subskrypcja do `gameService.getSavedGame()` w `redirectAfterLogin()` dla sprawdzenia zapisanej gry
- Subskrypcja do `authService.isAuthenticated()` w `ngOnInit()` dla sprawdzenia czy użytkownik jest już zalogowany

**Lifecycle hooks**:
- `ngOnInit()` - inicjalizacja: sprawdzenie czy użytkownik jest już zalogowany, utworzenie formularza
- `ngOnDestroy()` - czyszczenie subskrypcji (jeśli używane bez async pipe)

**Wzorce RxJS**:
- `take(1)` - dla jednorazowych operacji (sprawdzenie statusu uwierzytelnienia)
- `catchError()` - dla obsługi błędów w Observable (błędy API)
- `switchMap()` - dla przełączania między Observable (logowanie → sprawdzenie zapisanej gry)
- `tap()` - dla efektów ubocznych (zapisanie tokenu, aktualizacja stanu użytkownika)

## 7. Integracja API

### 7.1 Endpoint: POST /api/auth/login

**Cel**: Uwierzytelnienie zarejestrowanego użytkownika i zwrócenie tokenu JWT oraz danych użytkownika

**Metoda HTTP**: POST

**URL**: `/api/auth/login`

**Nagłówki**:
- `Content-Type: application/json` - format treści żądania
- `Accept: application/json` - preferowany format odpowiedzi

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Walidacja request body**:
- `email`: Wymagane (`@NotBlank`), format email (`@Email`)
- `password`: Wymagane (`@NotBlank`)

**Odpowiedź sukcesu (200 OK)**:
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "player1",
  "email": "user@example.com",
  "isGuest": false,
  "totalPoints": 3500,
  "gamesPlayed": 18,
  "gamesWon": 12,
  "authToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Odpowiedzi błędów**:

**401 Unauthorized** - Nieprawidłowe dane uwierzytelniające:
```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid email or password",
    "details": null
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**404 Not Found** - Użytkownik nie znaleziony:
```json
{
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "User not found",
    "details": null
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**422 Unprocessable Entity** - Błędy walidacji:
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": {
      "email": "Email must be valid",
      "password": "Password is required"
    }
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**500 Internal Server Error** - Błąd serwera:
```json
{
  "error": {
    "code": "INTERNAL_SERVER_ERROR",
    "message": "An unexpected error occurred",
    "details": null
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**Obsługa w komponencie**:
```typescript
onSubmit(): void {
  if (this.loginForm.invalid) {
    this.markFormGroupTouched(this.loginForm);
    return;
  }

  this.isLoading = true;
  const { email, password } = this.loginForm.value;

  this.authService.login(email, password).subscribe({
    next: (response) => {
      this.isLoading = false;
      this.messageService.add({
        severity: 'success',
        summary: 'Sukces',
        detail: 'Zalogowano pomyślnie'
      });
      this.redirectAfterLogin();
    },
    error: (error) => {
      this.isLoading = false;
      this.handleLoginError(error);
    }
  });
}
```

**Integracja z AuthService**:
- `AuthService.login(email: string, password: string): Observable<LoginResponse>` - metoda wywołująca API i zarządzająca tokenem
- `AuthService.isAuthenticated(): boolean` - metoda sprawdzająca czy użytkownik jest zalogowany
- `AuthService.getCurrentUser(): Observable<User | null>` - metoda pobierająca aktualnego użytkownika

### 7.2 Endpoint: GET /api/games?status=in_progress&size=1

**Cel**: Sprawdzenie czy istnieje zapisana gra w statusie `in_progress` dla przekierowania po logowaniu

**Metoda HTTP**: GET

**URL**: `/api/games?status=in_progress&size=1`

**Nagłówki**:
- `Authorization: Bearer <JWT_TOKEN>` - token JWT (wymagane)
- `Accept: application/json`

**Parametry zapytania**:
- `status: 'in_progress'` - filtrowanie gier w toku
- `size: 1` - maksymalnie 1 wynik (najnowsza gra)

**Odpowiedź sukcesu (200 OK)**:
```json
{
  "content": [
    {
      "gameId": 42,
      "gameType": "vs_bot",
      "boardSize": 3,
      "status": "in_progress",
      "player1Id": 123,
      "player2Id": null,
      "botDifficulty": "easy",
      "currentPlayerSymbol": "x",
      "winnerId": null,
      "lastMoveAt": "2024-01-20T15:30:00Z",
      "createdAt": "2024-01-20T14:00:00Z",
      "updatedAt": "2024-01-20T15:30:00Z",
      "finishedAt": null,
      "totalMoves": 5
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 1,
  "number": 0
}
```

**Odpowiedź gdy brak zapisanej gry (200 OK)**:
```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "size": 1,
  "number": 0
}
```

**Obsługa w komponencie**:
```typescript
private redirectAfterLogin(): void {
  this.gameService.getSavedGame().subscribe({
    next: (response) => {
      if (response.content.length > 0) {
        const game = response.content[0];
        this.router.navigate(['/game', game.gameId]);
      } else {
        this.router.navigate(['/']);
      }
    },
    error: () => {
      this.router.navigate(['/']);
    }
  });
}
```

**Integracja z GameService**:
- `GameService.getSavedGame(): Observable<SavedGameResponse>` - metoda wywołująca API i zwracająca zapisaną grę

## 8. Interakcje użytkownika

### 8.1 Wypełnianie formularza

**Akcja użytkownika**: Użytkownik wprowadza email i hasło w odpowiednie pola formularza

**Oczekiwany wynik**:
- Pola formularza są aktualizowane w czasie rzeczywistym (two-way binding)
- Walidacja po stronie klienta jest wykonywana przy każdej zmianie wartości
- Komunikaty błędów są wyświetlane pod polami gdy pole jest nieprawidłowe i touched
- Przycisk submit jest aktywny tylko gdy formularz jest prawidłowy

**Obsługa w komponencie**:
- Reactive Forms automatycznie aktualizuje wartości pól
- `isFieldInvalid(fieldName)` sprawdza czy pole jest nieprawidłowe
- `getFieldError(fieldName)` zwraca komunikat błędu dla pola
- `[disabled]="loginForm.invalid || isLoading"` na przycisku submit

### 8.2 Submit formularza

**Akcja użytkownika**: Użytkownik klika przycisk "Zaloguj się" lub naciska Enter w polu formularza

**Oczekiwany wynik**:
- Jeśli formularz jest nieprawidłowy: wszystkie pola są oznaczone jako touched, komunikaty błędów są wyświetlane
- Jeśli formularz jest prawidłowy: przycisk submit pokazuje stan loading, żądanie API jest wysyłane
- Po pomyślnym logowaniu: toast notification z komunikatem sukcesu, przekierowanie do zapisanej gry lub strony głównej
- Po błędzie logowania: toast notification z komunikatem błędu, formularz pozostaje wypełniony

**Obsługa w komponencie**:
- `onSubmit()` sprawdza walidację formularza
- Jeśli nieprawidłowy: `markFormGroupTouched()` oznacza wszystkie pola jako touched
- Jeśli prawidłowy: `authService.login()` wywołuje API
- `isLoading` jest ustawiane na `true` podczas żądania
- `redirectAfterLogin()` przekierowuje po sukcesie
- `handleLoginError()` obsługuje błędy

### 8.3 Nawigacja do rejestracji

**Akcja użytkownika**: Użytkownik klika link "Zarejestruj się"

**Oczekiwany wynik**:
- Użytkownik jest przekierowany do `/auth/register`
- Formularz logowania pozostaje wypełniony (jeśli użytkownik wróci)

**Obsługa w komponencie**:
- `<a routerLink="/auth/register">` używa Angular Router do nawigacji

### 8.4 Nawigacja do trybu gościa

**Akcja użytkownika**: Użytkownik klika link "graj jako gość"

**Oczekiwany wynik**:
- Użytkownik jest przekierowany do `/` (strona główna)
- Użytkownik może rozpocząć grę jako gość bez logowania

**Obsługa w komponencie**:
- `<a routerLink="/">` używa Angular Router do nawigacji

### 8.5 Sprawdzenie czy użytkownik jest już zalogowany

**Akcja użytkownika**: Użytkownik wchodzi na stronę `/auth/login` gdy jest już zalogowany

**Oczekiwany wynik**:
- Użytkownik jest automatycznie przekierowany do `/` (strona główna)
- Formularz logowania nie jest wyświetlany

**Obsługa w komponencie**:
- `ngOnInit()` sprawdza `authService.isAuthenticated()`
- Jeśli zalogowany: `router.navigate(['/'])` przekierowuje do strony głównej

## 9. Warunki i walidacja

### 9.1 Walidacja po stronie klienta

**Warunki dla pola email**:
- Pole jest wymagane (`Validators.required`)
  - Komunikat błędu: "To pole jest wymagane"
  - Sprawdzane gdy pole jest puste i touched
- Format email musi być prawidłowy (`Validators.email`)
  - Komunikat błędu: "Nieprawidłowy format email"
  - Sprawdzane gdy pole zawiera wartość ale format jest nieprawidłowy

**Warunki dla pola password**:
- Pole jest wymagane (`Validators.required`)
  - Komunikat błędu: "To pole jest wymagane"
  - Sprawdzane gdy pole jest puste i touched
- Minimalna długość 8 znaków (`Validators.minLength(8)`)
  - Komunikat błędu: "Minimalna długość: 8 znaków"
  - Sprawdzane gdy pole zawiera wartość ale długość jest mniejsza niż 8

**Warunki dla formularza**:
- Formularz jest prawidłowy tylko gdy wszystkie pola są prawidłowe
- Przycisk submit jest aktywny tylko gdy formularz jest prawidłowy i nie trwa proces logowania (`loginForm.invalid || isLoading`)

**Obsługa w komponencie**:
- `isFieldInvalid(fieldName)` sprawdza czy pole jest nieprawidłowe: `field && field.invalid && (field.dirty || field.touched)`
- `getFieldError(fieldName)` zwraca komunikat błędu na podstawie typu błędu
- `markFormGroupTouched()` oznacza wszystkie pola jako touched po submit nieprawidłowego formularza

### 9.2 Walidacja po stronie serwera

**Warunki dla request body**:
- `email`: Wymagane (`@NotBlank`), format email (`@Email`)
  - Błąd 422: `"email": "Email is required"` lub `"email": "Email must be valid"`
- `password`: Wymagane (`@NotBlank`)
  - Błąd 422: `"password": "Password is required"`

**Warunki dla uwierzytelnienia**:
- Email i hasło muszą być prawidłowe (walidacja przez Supabase Auth)
  - Błąd 401: `"Invalid email or password"` (ogólny komunikat dla bezpieczeństwa)
- Użytkownik musi istnieć w tabeli `users`
  - Błąd 404: `"User not found"` (rzadki przypadek - użytkownik w Supabase Auth ale nie w `users`)

**Obsługa w komponencie**:
- `handleLoginError(error)` sprawdza `error.status` i wyświetla odpowiedni komunikat
- Dla 422: szczegóły błędów walidacji są w `error.error.details`
- Dla 401/404: ogólny komunikat błędu jest w `error.error.message`
- Dla 500: ogólny komunikat "Wystąpił błąd serwera. Spróbuj ponownie później."

### 9.3 Warunki dla przekierowania

**Warunek**: Po pomyślnym logowaniu sprawdzana jest zapisana gra

**Logika**:
- Jeśli istnieje zapisana gra (`response.content.length > 0`): przekierowanie do `/game/{gameId}`
- Jeśli nie ma zapisanej gry (`response.content.length === 0`): przekierowanie do `/` (strona główna)
- Jeśli wystąpi błąd przy pobieraniu zapisanej gry: przekierowanie do `/` (strona główna)

**Obsługa w komponencie**:
- `redirectAfterLogin()` wywołuje `gameService.getSavedGame()`
- Sprawdza `response.content.length` i przekierowuje odpowiednio
- Obsługuje błędy i przekierowuje do strony głównej w przypadku błędu

## 10. Obsługa błędów

### 10.1 Błędy walidacji po stronie klienta

**Scenariusz**: Użytkownik próbuje wysłać formularz z nieprawidłowymi danymi

**Obsługa**:
- Wszystkie pola są oznaczone jako touched (`markFormGroupTouched()`)
- Komunikaty błędów są wyświetlane pod każdym nieprawidłowym polem
- Formularz nie jest wysyłany do API
- Użytkownik może poprawić błędy i spróbować ponownie

**Implementacja**:
```typescript
onSubmit(): void {
  if (this.loginForm.invalid) {
    this.markFormGroupTouched(this.loginForm);
    return;
  }
  // ... dalsza obsługa
}
```

### 10.2 Błędy walidacji po stronie serwera (422)

**Scenariusz**: API zwraca 422 Unprocessable Entity z błędami walidacji Bean Validation

**Obsługa**:
- Toast notification z komunikatem "Błąd walidacji"
- Szczegóły błędów walidacji są wyświetlane w toast notification lub pod polami formularza
- Formularz pozostaje wypełniony
- Użytkownik może poprawić błędy i spróbować ponownie

**Implementacja**:
```typescript
private handleLoginError(error: HttpErrorResponse): void {
  if (error.status === 422) {
    const details = error.error?.error?.details;
    if (details) {
      Object.keys(details).forEach(field => {
        const control = this.loginForm.get(field);
        if (control) {
          control.setErrors({ serverError: details[field] });
        }
      });
    }
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd walidacji',
      detail: 'Sprawdź poprawność wprowadzonych danych'
    });
  }
  // ... dalsza obsługa
}
```

### 10.3 Nieprawidłowe dane uwierzytelniające (401)

**Scenariusz**: API zwraca 401 Unauthorized - nieprawidłowy email lub hasło

**Obsługa**:
- Toast notification z komunikatem "Nieprawidłowy email lub hasło"
- Formularz pozostaje wypełniony (dla wygody użytkownika)
- Pole hasło może być wyczyszczone (opcjonalnie)
- Użytkownik może spróbować ponownie

**Implementacja**:
```typescript
private handleLoginError(error: HttpErrorResponse): void {
  if (error.status === 401) {
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd logowania',
      detail: 'Nieprawidłowy email lub hasło'
    });
    this.loginForm.get('password')?.reset();
  }
  // ... dalsza obsługa
}
```

### 10.4 Użytkownik nie znaleziony (404)

**Scenariusz**: API zwraca 404 Not Found - użytkownik nie istnieje w tabeli `users`

**Obsługa**:
- Toast notification z komunikatem "Użytkownik nie został znaleziony"
- Formularz pozostaje wypełniony
- Użytkownik może spróbować ponownie lub skontaktować się z supportem

**Implementacja**:
```typescript
private handleLoginError(error: HttpErrorResponse): void {
  if (error.status === 404) {
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd logowania',
      detail: 'Użytkownik nie został znaleziony'
    });
  }
  // ... dalsza obsługa
}
```

### 10.5 Błąd serwera (500)

**Scenariusz**: API zwraca 500 Internal Server Error - błąd serwera lub Supabase Auth

**Obsługa**:
- Toast notification z komunikatem "Wystąpił błąd serwera. Spróbuj ponownie później."
- Formularz pozostaje wypełniony
- Użytkownik może spróbować ponownie po chwili

**Implementacja**:
```typescript
private handleLoginError(error: HttpErrorResponse): void {
  if (error.status >= 500) {
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd serwera',
      detail: 'Wystąpił błąd serwera. Spróbuj ponownie później.'
    });
  }
  // ... dalsza obsługa
}
```

### 10.6 Błąd sieci lub timeout

**Scenariusz**: Brak połączenia z serwerem, timeout lub błąd sieci

**Obsługa**:
- Toast notification z komunikatem "Brak połączenia z serwerem. Sprawdź połączenie internetowe."
- Formularz pozostaje wypełniony
- Użytkownik może spróbować ponownie po przywróceniu połączenia

**Implementacja**:
```typescript
private handleLoginError(error: HttpErrorResponse): void {
  if (!error.status || error.status === 0) {
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd połączenia',
      detail: 'Brak połączenia z serwerem. Sprawdź połączenie internetowe.'
    });
  }
  // ... dalsza obsługa
}
```

### 10.7 Błąd przy pobieraniu zapisanej gry

**Scenariusz**: Po pomyślnym logowaniu wystąpi błąd przy pobieraniu zapisanej gry

**Obsługa**:
- Użytkownik jest przekierowany do strony głównej (`/`)
- Błąd jest logowany w konsoli (dla debugowania)
- Toast notification nie jest wyświetlany (użytkownik już widział komunikat sukcesu)

**Implementacja**:
```typescript
private redirectAfterLogin(): void {
  this.gameService.getSavedGame().subscribe({
    next: (response) => {
      if (response.content.length > 0) {
        const game = response.content[0];
        this.router.navigate(['/game', game.gameId]);
      } else {
        this.router.navigate(['/']);
      }
    },
    error: () => {
      console.error('Error loading saved game:', error);
      this.router.navigate(['/']);
    }
  });
}
```

## 11. Kroki implementacji

### Krok 1: Przygotowanie struktury komponentu

**1.1 Utworzenie plików komponentu**:
- `frontend/src/app/features/auth/auth-login.component.ts`
- `frontend/src/app/features/auth/auth-login.component.html`
- `frontend/src/app/features/auth/auth-login.component.scss`

**1.2 Utworzenie podstawowej struktury komponentu**:
```typescript
@Component({
  selector: 'app-auth-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, InputTextModule, PasswordModule, ButtonModule, ToastModule],
  templateUrl: './auth-login.component.html',
  styleUrls: ['./auth-login.component.scss']
})
export class AuthLoginComponent implements OnInit {
  // ... implementacja
}
```

### Krok 2: Implementacja formularza i walidacji

**2.1 Utworzenie reactive form**:
- Utworzenie `FormGroup` z polami `email` i `password`
- Dodanie walidatorów: `Validators.required`, `Validators.email`, `Validators.minLength(8)`
- Inicjalizacja formularza w konstruktorze

**2.2 Implementacja metod walidacji**:
- `isFieldInvalid(fieldName: string)` - sprawdzenie czy pole jest nieprawidłowe
- `getFieldError(fieldName: string)` - pobranie komunikatu błędu
- `markFormGroupTouched(formGroup: FormGroup)` - oznaczenie wszystkich pól jako touched

### Krok 3: Implementacja template

**3.1 Utworzenie struktury HTML**:
- Kontener główny (`.auth-container`)
- Karta formularza (`.auth-card`)
- Nagłówek z tytułem
- Formularz z polami email i password
- Przycisk submit
- Sekcja linków nawigacyjnych

**3.2 Dodanie PrimeNG komponentów**:
- `<input pInputText>` dla pola email
- `<p-password>` dla pola hasła
- `<p-button>` dla przycisku submit
- `<p-toast>` dla powiadomień (opcjonalnie, jeśli używany MessageService)

**3.3 Dodanie walidacji w template**:
- `[class.ng-invalid]` dla pól formularza
- `*ngIf` dla komunikatów błędów
- `[disabled]` dla przycisku submit

### Krok 4: Implementacja integracji z API

**4.1 Utworzenie AuthService** (jeśli nie istnieje):
- `login(email: string, password: string): Observable<LoginResponse>`
- `isAuthenticated(): boolean`
- `getCurrentUser(): Observable<User | null>`
- Metody do zarządzania tokenem JWT (zapis w localStorage)

**4.2 Implementacja metody onSubmit**:
- Sprawdzenie walidacji formularza
- Wywołanie `authService.login()`
- Obsługa odpowiedzi sukcesu i błędów
- Ustawienie `isLoading` podczas żądania

**4.3 Implementacja metody redirectAfterLogin**:
- Wywołanie `gameService.getSavedGame()`
- Sprawdzenie czy istnieje zapisana gra
- Przekierowanie do zapisanej gry lub strony głównej

### Krok 5: Implementacja obsługi błędów

**5.1 Implementacja metody handleLoginError**:
- Sprawdzenie `error.status`
- Obsługa błędów 401, 404, 422, 500
- Wyświetlanie odpowiednich komunikatów błędów przez MessageService

**5.2 Dodanie obsługi błędów sieci**:
- Sprawdzenie czy `error.status` jest 0 lub undefined
- Wyświetlanie komunikatu o braku połączenia

### Krok 6: Implementacja sprawdzania statusu uwierzytelnienia

**6.1 Implementacja ngOnInit**:
- Sprawdzenie `authService.isAuthenticated()`
- Przekierowanie do strony głównej jeśli użytkownik jest już zalogowany

### Krok 7: Implementacja stylowania

**7.1 Utworzenie stylów SCSS**:
- Stylowanie `.auth-container` (centrowanie, padding)
- Stylowanie `.auth-card` (tło, cienie, zaokrąglenia)
- Stylowanie `.form-group` (marginesy, odstępy)
- Stylowanie `.error-message` (kolory, rozmiary czcionek)
- Stylowanie `.auth-links` (kolory linków, hover)

**7.2 Dodanie animacji**:
- Fade-in dla formularza (300ms)
- Smooth transitions dla przycisków
- Pulse animation dla błędów walidacji (opcjonalnie)

### Krok 8: Konfiguracja routingu

**8.1 Aktualizacja app.routes.ts**:
- Dodanie routingu dla `/auth/login`
- Konfiguracja jako child route pod `/auth` (jeśli używany AuthComponent jako layout)

**8.2 Aktualizacja nawigacji**:
- Sprawdzenie czy linki nawigacyjne w innych komponentach wskazują na `/auth/login`

### Krok 9: Implementacja i18n (opcjonalnie)

**9.1 Dodanie kluczy tłumaczeń**:
- Klucze dla etykiet pól formularza
- Klucze dla komunikatów błędów walidacji
- Klucze dla komunikatów błędów API
- Klucze dla linków nawigacyjnych

**9.2 Aktualizacja template**:
- Zastąpienie tekstów przez `{{ 'auth.login.title' | translate }}`
- Użycie `TranslateService` w komponencie dla dynamicznych komunikatów

### Krok 10: Testy jednostkowe

**10.1 Utworzenie pliku testowego**:
- `frontend/src/app/features/auth/auth-login.component.spec.ts`

**10.2 Implementacja testów**:
- Test inicjalizacji komponentu
- Test walidacji formularza
- Test logowania z poprawnymi danymi
- Test obsługi błędów (401, 404, 422, 500)
- Test przekierowania po sukcesie
- Test przekierowania gdy użytkownik jest już zalogowany

### Krok 11: Testy E2E (Cypress)

**11.1 Utworzenie pliku testowego**:
- `frontend/cypress/e2e/auth-login.cy.ts`

**11.2 Implementacja testów**:
- Scenariusz: Logowanie z poprawnymi danymi
- Scenariusz: Logowanie z nieprawidłowymi danymi (401)
- Scenariusz: Logowanie z nieprawidłowym formatem email (422)
- Scenariusz: Przekierowanie do zapisanej gry po logowaniu
- Scenariusz: Przekierowanie gdy użytkownik jest już zalogowany

### Krok 12: Dostępność (a11y)

**12.1 Dodanie ARIA labels**:
- `aria-describedby` dla pól formularza
- `aria-required="true"` dla wymaganych pól
- `aria-label` dla przycisku submit

**12.2 Implementacja keyboard navigation**:
- Obsługa Tab dla nawigacji między polami
- Obsługa Enter dla submit formularza
- Focus management po błędach walidacji

**12.3 Screen reader support**:
- Komunikaty błędów są powiązane z polami przez `aria-describedby`
- Komunikaty sukcesu są ogłaszane przez screen reader

### Krok 13: Code review i optymalizacja

**13.1 Code review**:
- Sprawdzenie zgodności z zasadami implementacji
- Sprawdzenie obsługi błędów
- Sprawdzenie dostępności

**13.2 Optymalizacja**:
- Sprawdzenie czy subskrypcje są prawidłowo czyszczone
- Sprawdzenie czy użyte są odpowiednie operatory RxJS
- Sprawdzenie czy nie ma memory leaks

### Krok 14: Dokumentacja i wdrożenie

**14.1 Dokumentacja**:
- Aktualizacja README z informacjami o komponencie
- Dokumentacja API w komentarzach (jeśli wymagane)

**14.2 Wdrożenie**:
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Test integracji z backendem

