# Plan implementacji widoku AuthRegisterComponent

> **Źródło**: `.ai/implementation-plans-ui/03_auth-register-component.md`

## 1. Przegląd

AuthRegisterComponent to widok umożliwiający nowym użytkownikom utworzenie konta w systemie. Komponent obsługuje formularz rejestracji z walidacją po stronie klienta i serwera, integrację z API backendu oraz odpowiednią obsługę błędów i przekierowań po pomyślnej rejestracji.

Główne funkcjonalności:
- Formularz rejestracji z polami: nazwa użytkownika, email, hasło, potwierdzenie hasła
- Walidacja po stronie klienta (reactive forms) i serwera (API)
- Wskaźnik siły hasła (PasswordStrengthIndicatorComponent)
- Integracja z endpointem POST /api/auth/register
- Obsługa błędów API (409 Conflict, 422 Unprocessable Entity, 500 Internal Server Error)
- Przekierowanie po pomyślnej rejestracji do strony głównej
- Sprawdzenie czy użytkownik jest już zalogowany (przekierowanie)
- Linki nawigacyjne do logowania i trybu gościa

Komponent realizuje historyjkę użytkownika: US-002 (Rejestracja nowego użytkownika).

## 2. Routing widoku

**Ścieżka routingu**: `/auth/register`

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
AuthRegisterComponent (główny komponent)
├── FormGroup (reactive form)
│   ├── FormControl (username)
│   ├── FormControl (email)
│   ├── FormControl (password)
│   └── FormControl (confirmPassword)
├── InputTextModule (PrimeNG - pole username i email)
├── PasswordModule (PrimeNG - pole hasła i potwierdzenia hasła)
├── ButtonModule (PrimeNG - przycisk submit)
├── PasswordStrengthIndicatorComponent (współdzielony komponent)
└── RouterModule (nawigacja)
```

**Hierarchia komponentów**:
- AuthRegisterComponent jest komponentem standalone
- Komponent używa PrimeNG do elementów UI (InputText, Password, Button)
- Komponent używa Angular Reactive Forms do zarządzania formularzem
- Komponent używa Angular Router do nawigacji
- Komponent używa PasswordStrengthIndicatorComponent do wyświetlania wskaźnika siły hasła

## 4. Szczegóły komponentów

### AuthRegisterComponent

**Opis komponentu**: Główny komponent widoku rejestracji, zarządza formularzem rejestracji, walidacją, integracją z API oraz obsługą błędów i przekierowań. Komponent sprawdza czy użytkownik jest już zalogowany i przekierowuje do strony głównej jeśli tak.

**Główne elementy HTML**:
- Kontener główny (`.auth-container`)
- Karta formularza (`.auth-card`)
- Nagłówek z tytułem (`<h2>Rejestracja</h2>`)
- Formularz (`<form [formGroup]="registerForm" (ngSubmit)="onSubmit()">`)
  - Pole nazwa użytkownika (`<input pInputText formControlName="username">`)
  - Pole email (`<input pInputText formControlName="email">`)
  - Pole hasło (`<p-password formControlName="password">`)
  - Komponent wskaźnika siły hasła (`<app-password-strength-indicator>`)
  - Pole potwierdzenie hasła (`<p-password formControlName="confirmPassword">`)
  - Przycisk submit (`<p-button type="submit">`)
- Sekcja linków nawigacyjnych (`.auth-links`)
  - Link do logowania (`<a routerLink="/auth/login">`)
  - Link do trybu gościa (`<a routerLink="/">`)

**Obsługiwane zdarzenia**:
- `ngOnInit()` - inicjalizacja komponentu, sprawdzenie czy użytkownik jest już zalogowany
- `onSubmit()` - obsługa submit formularza, walidacja i wywołanie API
- `passwordMatchValidator(form: AbstractControl)` - custom validator sprawdzający zgodność haseł
- `isFieldInvalid(fieldName: string)` - sprawdzenie czy pole jest nieprawidłowe
- `getFieldError(fieldName: string)` - pobranie komunikatu błędu dla pola
- `markFormGroupTouched(formGroup: FormGroup)` - oznaczenie wszystkich pól jako touched
- `handleRegisterError(error: HttpErrorResponse)` - obsługa błędów API
- `handleValidationErrors(errors: any)` - obsługa błędów walidacji z serwera

**Obsługiwana walidacja**:
- **Nazwa użytkownika**: 
  - Wymagane (`Validators.required`)
  - 3-50 znaków (`Validators.minLength(3)`, `Validators.maxLength(50)`)
  - Pattern: `/^[a-zA-Z0-9_]+$/` (alfanumeryczne + podkreślniki)
- **Email**: 
  - Wymagane (`Validators.required`)
  - Format email (`Validators.email`)
- **Hasło**: 
  - Wymagane (`Validators.required`)
  - Minimalna długość 8 znaków (`Validators.minLength(8)`)
- **Potwierdzenie hasła**: 
  - Wymagane (`Validators.required`)
  - Custom validator sprawdzający zgodność z hasłem (`passwordMatchValidator`)
- Walidacja po stronie serwera (API):
  - 422 Unprocessable Entity - błędy walidacji Bean Validation
  - 409 Conflict - nazwa użytkownika lub email już istnieje

**Typy**:
- `RegisterRequest` - DTO dla żądania rejestracji
- `RegisterResponse` - DTO dla odpowiedzi z API
- `User` - interfejs reprezentujący użytkownika
- `FormGroup` - Angular Reactive Forms
- `FormControl` - Angular Reactive Forms
- `AbstractControl` - Angular Reactive Forms (dla custom validatora)
- `ValidationErrors` - Angular Reactive Forms (dla błędów walidacji)
- `HttpErrorResponse` - Angular HTTP error response
- `Observable<RegisterResponse>` - Observable z odpowiedzią API

**Propsy**: Brak (komponent główny, nie przyjmuje propsów)

### PasswordStrengthIndicatorComponent

**Opis komponentu**: Współdzielony komponent wyświetlający wizualny wskaźnik siły hasła. Komponent analizuje wprowadzone hasło i wyświetla wizualne wskaźniki (słabe, średnie, silne) oraz opcjonalnie listę wymagań hasła.

**Główne elementy HTML**:
- Kontener wskaźnika siły hasła
- Pasek postępu lub wizualne wskaźniki (słabe, średnie, silne)
- Opcjonalnie: lista wymagań hasła (min. 8 znaków, wielkie/małe litery, cyfry, znaki specjalne)

**Obsługiwane zdarzenia**: Brak (komponent prezentacyjny)

**Obsługiwana walidacja**: Brak (komponent tylko wyświetla wskaźnik, nie waliduje)

**Typy**:
- `password: string` - hasło do analizy (input property)

**Propsy**:
- `password: string` - hasło do analizy (wymagane)

## 5. Typy

### RegisterRequest (DTO dla żądania)

```typescript
interface RegisterRequest {
  email: string;
  password: string;
  username: string;
}
```

**Pola**:
- `email: string` - adres email użytkownika (wymagane, format email)
- `password: string` - hasło użytkownika (wymagane, min. 8 znaków)
- `username: string` - nazwa użytkownika (wymagane, 3-50 znaków, alfanumeryczne + podkreślniki)

**Uwagi**:
- DTO używane do wysłania żądania do endpointu POST /api/auth/register
- Walidacja po stronie klienta: `Validators.required`, `Validators.email`, `Validators.minLength(8)`, `Validators.minLength(3)`, `Validators.maxLength(50)`, `Validators.pattern(/^[a-zA-Z0-9_]+$/)`
- Walidacja po stronie serwera: `@NotBlank`, `@Email`, `@Size(min = 8)`, `@Size(min = 3, max = 50)`

### RegisterResponse (DTO dla odpowiedzi)

```typescript
interface RegisterResponse {
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
- `userId: string` - unikalny identyfikator użytkownika (UUID jako string)
- `username: string` - nazwa użytkownika
- `email: string` - adres email użytkownika
- `isGuest: boolean` - flaga wskazująca czy użytkownik jest gościem (zawsze false dla zarejestrowanych)
- `totalPoints: number` - suma punktów użytkownika (domyślnie 0 dla nowych użytkowników)
- `gamesPlayed: number` - liczba rozegranych gier (domyślnie 0)
- `gamesWon: number` - liczba wygranych gier (domyślnie 0)
- `authToken: string` - token JWT do uwierzytelniania kolejnych żądań

**Uwagi**:
- DTO zwracane przez endpoint POST /api/auth/register po pomyślnej rejestracji (201 Created)
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
- Mapowany z `RegisterResponse` przez AuthService
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
- `error.code: string` - kod błędu (np. "CONFLICT", "VALIDATION_ERROR", "INTERNAL_SERVER_ERROR")
- `error.message: string` - komunikat błędu
- `error.details: Record<string, string> | null` - szczegóły błędów walidacji (dla 422) lub null
- `timestamp: string` - timestamp błędu (ISO 8601)
- `status: 'error'` - status odpowiedzi (zawsze "error")

**Uwagi**:
- DTO zwracane przez API w przypadku błędów (409, 422, 500)
- Używane do wyświetlania komunikatów błędów użytkownikowi
- `error.details` zawiera mapę błędów walidacji dla 422 Unprocessable Entity

## 6. Zarządzanie stanem

**Strategia zarządzania stanem**: Reactive Forms + RxJS Observables + BehaviorSubject (w AuthService)

**Stan komponentu**:
- `registerForm: FormGroup` - reactive form z polami username, email, password, confirmPassword
- `isLoading: boolean` - flaga wskazująca czy trwa proces rejestracji
- `registerForm.get('username')` - FormControl dla pola username
- `registerForm.get('email')` - FormControl dla pola email
- `registerForm.get('password')` - FormControl dla pola password
- `registerForm.get('confirmPassword')` - FormControl dla pola confirmPassword

**Stan w AuthService** (współdzielony):
- `currentUser$: BehaviorSubject<User | null>` - aktualny użytkownik (null jeśli nie zalogowany)
- `authToken$: BehaviorSubject<string | null>` - token JWT (null jeśli nie zalogowany)
- `isAuthenticated$: Observable<boolean>` - Observable ze statusem uwierzytelnienia

**Custom hooki**: Brak (komponent używa standardowych Observable i Reactive Forms)

**Subskrypcje**:
- Subskrypcja do `authService.register()` w `onSubmit()` dla wywołania API
- Subskrypcja do `authService.isAuthenticated()` w `ngOnInit()` dla sprawdzenia czy użytkownik jest już zalogowany

**Lifecycle hooks**:
- `ngOnInit()` - inicjalizacja: sprawdzenie czy użytkownik jest już zalogowany, utworzenie formularza
- `ngOnDestroy()` - czyszczenie subskrypcji (jeśli używane bez async pipe)

**Wzorce RxJS**:
- `take(1)` - dla jednorazowych operacji (sprawdzenie statusu uwierzytelnienia)
- `catchError()` - dla obsługi błędów w Observable (błędy API)
- `tap()` - dla efektów ubocznych (zapisanie tokenu, aktualizacja stanu użytkownika)

## 7. Integracja API

### 7.1 Endpoint: POST /api/auth/register

**Cel**: Rejestracja nowego użytkownika i zwrócenie tokenu JWT oraz danych użytkownika

**Metoda HTTP**: POST

**URL**: `/api/auth/register`

**Nagłówki**:
- `Content-Type: application/json` - format treści żądania
- `Accept: application/json` - preferowany format odpowiedzi

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "username": "player1"
}
```

**Walidacja request body**:
- `email`: Wymagane (`@NotBlank`), format email (`@Email`)
- `password`: Wymagane (`@NotBlank`), minimalna długość 8 znaków (`@Size(min = 8)`)
- `username`: Wymagane (`@NotBlank`), 3-50 znaków (`@Size(min = 3, max = 50)`)

**Odpowiedź sukcesu (201 Created)**:
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "player1",
  "email": "user@example.com",
  "isGuest": false,
  "totalPoints": 0,
  "gamesPlayed": 0,
  "gamesWon": 0,
  "authToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Odpowiedzi błędów**:

**409 Conflict** - Nazwa użytkownika lub email już istnieje:
```json
{
  "error": {
    "code": "CONFLICT",
    "message": "Username or email already exists",
    "details": {
      "field": "email",
      "value": "user@example.com"
    }
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
      "username": "Username must be between 3 and 50 characters",
      "email": "Email must be valid",
      "password": "Password must be at least 8 characters"
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
  if (this.registerForm.invalid) {
    this.markFormGroupTouched(this.registerForm);
    return;
  }

  this.isLoading = true;
  const { username, email, password } = this.registerForm.value;

  this.authService.register(username, email, password).subscribe({
    next: (response) => {
      this.isLoading = false;
      this.messageService.add({
        severity: 'success',
        summary: 'Sukces',
        detail: 'Konto zostało utworzone pomyślnie'
      });
      this.router.navigate(['/']);
    },
    error: (error) => {
      this.isLoading = false;
      this.handleRegisterError(error);
    }
  });
}
```

**Integracja z AuthService**:
- `AuthService.register(username: string, email: string, password: string): Observable<RegisterResponse>` - metoda wywołująca API i zarządzająca tokenem
- `AuthService.isAuthenticated(): boolean` - metoda sprawdzająca czy użytkownik jest zalogowany
- `AuthService.getCurrentUser(): Observable<User | null>` - metoda pobierająca aktualnego użytkownika

## 8. Interakcje użytkownika

### 8.1 Wypełnianie formularza

**Akcja użytkownika**: Użytkownik wprowadza nazwę użytkownika, email, hasło i potwierdzenie hasła w odpowiednie pola formularza

**Oczekiwany wynik**:
- Pola formularza są aktualizowane w czasie rzeczywistym (two-way binding)
- Walidacja po stronie klienta jest wykonywana przy każdej zmianie wartości
- Komunikaty błędów są wyświetlane pod polami gdy pole jest nieprawidłowe i touched
- Wskaźnik siły hasła jest aktualizowany w czasie rzeczywistym
- Przycisk submit jest aktywny tylko gdy formularz jest prawidłowy

**Obsługa w komponencie**:
- Reactive Forms automatycznie aktualizuje wartości pól
- `isFieldInvalid(fieldName)` sprawdza czy pole jest nieprawidłowe
- `getFieldError(fieldName)` zwraca komunikat błędu dla pola
- `PasswordStrengthIndicatorComponent` wyświetla wskaźnik siły hasła
- `[disabled]="registerForm.invalid || isLoading"` na przycisku submit

### 8.2 Submit formularza

**Akcja użytkownika**: Użytkownik klika przycisk "Zarejestruj się" lub naciska Enter w polu formularza

**Oczekiwany wynik**:
- Jeśli formularz jest nieprawidłowy: wszystkie pola są oznaczone jako touched, komunikaty błędów są wyświetlane
- Jeśli formularz jest prawidłowy: przycisk submit pokazuje stan loading, żądanie API jest wysyłane
- Po pomyślnej rejestracji: toast notification z komunikatem sukcesu, przekierowanie do strony głównej
- Po błędzie rejestracji: toast notification z komunikatem błędu, formularz pozostaje wypełniony (z wyjątkiem hasła w przypadku 409)

**Obsługa w komponencie**:
- `onSubmit()` sprawdza walidację formularza
- Jeśli nieprawidłowy: `markFormGroupTouched()` oznacza wszystkie pola jako touched
- Jeśli prawidłowy: `authService.register()` wywołuje API
- `isLoading` jest ustawiane na `true` podczas żądania
- `router.navigate(['/'])` przekierowuje po sukcesie
- `handleRegisterError()` obsługuje błędy

### 8.3 Nawigacja do logowania

**Akcja użytkownika**: Użytkownik klika link "Zaloguj się"

**Oczekiwany wynik**:
- Użytkownik jest przekierowany do `/auth/login`
- Formularz rejestracji pozostaje wypełniony (jeśli użytkownik wróci)

**Obsługa w komponencie**:
- `<a routerLink="/auth/login">` używa Angular Router do nawigacji

### 8.4 Nawigacja do trybu gościa

**Akcja użytkownika**: Użytkownik klika link "graj jako gość"

**Oczekiwany wynik**:
- Użytkownik jest przekierowany do `/` (strona główna)
- Użytkownik może rozpocząć grę jako gość bez logowania

**Obsługa w komponencie**:
- `<a routerLink="/">` używa Angular Router do nawigacji

### 8.5 Sprawdzenie czy użytkownik jest już zalogowany

**Akcja użytkownika**: Użytkownik wchodzi na stronę `/auth/register` gdy jest już zalogowany

**Oczekiwany wynik**:
- Użytkownik jest automatycznie przekierowany do `/` (strona główna)
- Formularz rejestracji nie jest wyświetlany

**Obsługa w komponencie**:
- `ngOnInit()` sprawdza `authService.isAuthenticated()`
- Jeśli zalogowany: `router.navigate(['/'])` przekierowuje do strony głównej

## 9. Warunki i walidacja

### 9.1 Walidacja po stronie klienta

**Warunki dla pola username**:
- Pole jest wymagane (`Validators.required`)
  - Komunikat błędu: "To pole jest wymagane"
  - Sprawdzane gdy pole jest puste i touched
- Długość: 3-50 znaków (`Validators.minLength(3)`, `Validators.maxLength(50)`)
  - Komunikat błędu: "Minimalna długość: 3 znaki" lub "Maksymalna długość: 50 znaków"
  - Sprawdzane gdy pole zawiera wartość ale długość jest nieprawidłowa
- Pattern: `/^[a-zA-Z0-9_]+$/` (alfanumeryczne + podkreślniki)
  - Komunikat błędu: "Nazwa użytkownika może zawierać tylko litery, cyfry i podkreślniki"
  - Sprawdzane gdy pole zawiera wartość ale format jest nieprawidłowy

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

**Warunki dla pola confirmPassword**:
- Pole jest wymagane (`Validators.required`)
  - Komunikat błędu: "To pole jest wymagane"
  - Sprawdzane gdy pole jest puste i touched
- Zgodność z hasłem (custom validator `passwordMatchValidator`)
  - Komunikat błędu: "Hasła nie są zgodne"
  - Sprawdzane gdy pole zawiera wartość ale nie jest zgodne z hasłem

**Warunki dla formularza**:
- Formularz jest prawidłowy tylko gdy wszystkie pola są prawidłowe
- Przycisk submit jest aktywny tylko gdy formularz jest prawidłowy i nie trwa proces rejestracji (`registerForm.invalid || isLoading`)

**Obsługa w komponencie**:
- `isFieldInvalid(fieldName)` sprawdza czy pole jest nieprawidłowe: `field && field.invalid && (field.dirty || field.touched)`
- `getFieldError(fieldName)` zwraca komunikat błędu na podstawie typu błędu
- `passwordMatchValidator()` sprawdza zgodność haseł
- `markFormGroupTouched()` oznacza wszystkie pola jako touched po submit nieprawidłowego formularza

### 9.2 Walidacja po stronie serwera

**Warunki dla request body**:
- `email`: Wymagane (`@NotBlank`), format email (`@Email`)
  - Błąd 422: `"email": "Email is required"` lub `"email": "Email must be valid"`
- `password`: Wymagane (`@NotBlank`), minimalna długość 8 znaków (`@Size(min = 8)`)
  - Błąd 422: `"password": "Password is required"` lub `"password": "Password must be at least 8 characters"`
- `username`: Wymagane (`@NotBlank`), 3-50 znaków (`@Size(min = 3, max = 50)`)
  - Błąd 422: `"username": "Username is required"` lub `"username": "Username must be between 3 and 50 characters"`

**Warunki dla unikalności**:
- Email musi być unikalny (sprawdzenie w Supabase Auth i tabeli `users`)
  - Błąd 409: `"Username or email already exists"` (z `details.field = "email"`)
- Nazwa użytkownika musi być unikalna (sprawdzenie w tabeli `users`)
  - Błąd 409: `"Username or email already exists"` (z `details.field = "username"`)

**Obsługa w komponencie**:
- `handleRegisterError(error)` sprawdza `error.status` i wyświetla odpowiedni komunikat
- Dla 422: szczegóły błędów walidacji są w `error.error.error.details`
- Dla 409: komunikat błędu jest w `error.error.error.message`, sprawdzane czy zawiera "username" lub "email"
- Dla 500: ogólny komunikat "Wystąpił błąd serwera. Spróbuj ponownie później."

### 9.3 Warunki dla przekierowania

**Warunek**: Po pomyślnej rejestracji użytkownik jest przekierowany do strony głównej

**Logika**:
- Po pomyślnej rejestracji: przekierowanie do `/` (strona główna)
- Token JWT jest zapisywany w localStorage przez AuthService
- Stan użytkownika jest aktualizowany w AuthService

**Obsługa w komponencie**:
- `onSubmit()` po sukcesie wywołuje `router.navigate(['/'])`
- AuthService automatycznie zapisuje token i aktualizuje stan użytkownika

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
  if (this.registerForm.invalid) {
    this.markFormGroupTouched(this.registerForm);
    return;
  }
  // ... dalsza obsługa
}
```

### 10.2 Błędy walidacji po stronie serwera (422)

**Scenariusz**: API zwraca 422 Unprocessable Entity z błędami walidacji Bean Validation

**Obsługa**:
- Toast notification z komunikatem "Nieprawidłowe dane. Sprawdź formularz."
- Szczegóły błędów walidacji są wyświetlane pod odpowiednimi polami formularza
- Formularz pozostaje wypełniony
- Użytkownik może poprawić błędy i spróbować ponownie

**Implementacja**:
```typescript
private handleRegisterError(error: HttpErrorResponse): void {
  if (error.status === 422) {
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd rejestracji',
      detail: 'Nieprawidłowe dane. Sprawdź formularz.'
    });
    this.handleValidationErrors(error.error);
  }
  // ... dalsza obsługa
}

private handleValidationErrors(errors: any): void {
  if (errors?.error?.details) {
    Object.keys(errors.error.details).forEach(key => {
      const control = this.registerForm.get(key);
      if (control) {
        control.setErrors({ serverError: errors.error.details[key] });
        control.markAsTouched();
      }
    });
  }
}
```

### 10.3 Nazwa użytkownika lub email już istnieje (409)

**Scenariusz**: API zwraca 409 Conflict - nazwa użytkownika lub email już istnieje

**Obsługa**:
- Toast notification z komunikatem "Nazwa użytkownika już istnieje" lub "Email już istnieje"
- Formularz pozostaje wypełniony (dla wygody użytkownika)
- Pole z duplikatem może być wyróżnione wizualnie (opcjonalnie)
- Użytkownik może spróbować ponownie z innymi danymi

**Implementacja**:
```typescript
private handleRegisterError(error: HttpErrorResponse): void {
  if (error.status === 409) {
    const errorMessage = error.error?.error?.message || '';
    let message = 'Wystąpił błąd podczas rejestracji';
    
    if (errorMessage.includes('username')) {
      message = 'Nazwa użytkownika już istnieje';
    } else if (errorMessage.includes('email')) {
      message = 'Email już istnieje';
    } else {
      message = errorMessage;
    }
    
    this.messageService.add({
      severity: 'error',
      summary: 'Błąd rejestracji',
      detail: message
    });
  }
  // ... dalsza obsługa
}
```

### 10.4 Błąd serwera (500)

**Scenariusz**: API zwraca 500 Internal Server Error - błąd serwera lub Supabase Auth

**Obsługa**:
- Toast notification z komunikatem "Wystąpił błąd serwera. Spróbuj ponownie później."
- Formularz pozostaje wypełniony
- Użytkownik może spróbować ponownie po chwili

**Implementacja**:
```typescript
private handleRegisterError(error: HttpErrorResponse): void {
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

### 10.5 Błąd sieci lub timeout

**Scenariusz**: Brak połączenia z serwerem, timeout lub błąd sieci

**Obsługa**:
- Toast notification z komunikatem "Brak połączenia z serwerem. Sprawdź połączenie internetowe."
- Formularz pozostaje wypełniony
- Użytkownik może spróbować ponownie po przywróceniu połączenia

**Implementacja**:
```typescript
private handleRegisterError(error: HttpErrorResponse): void {
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

## 11. Kroki implementacji

### Krok 1: Przygotowanie struktury komponentu

**1.1 Utworzenie plików komponentu**:
- `frontend/src/app/features/auth/auth-register.component.ts`
- `frontend/src/app/features/auth/auth-register.component.html`
- `frontend/src/app/features/auth/auth-register.component.scss`

**1.2 Utworzenie podstawowej struktury komponentu**:
```typescript
@Component({
  selector: 'app-auth-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    ToastModule,
    PasswordStrengthIndicatorComponent
  ],
  templateUrl: './auth-register.component.html',
  styleUrls: ['./auth-register.component.scss']
})
export class AuthRegisterComponent implements OnInit {
  // ... implementacja
}
```

### Krok 2: Implementacja formularza i walidacji

**2.1 Utworzenie reactive form**:
- Utworzenie `FormGroup` z polami `username`, `email`, `password`, `confirmPassword`
- Dodanie walidatorów: `Validators.required`, `Validators.email`, `Validators.minLength(8)`, `Validators.minLength(3)`, `Validators.maxLength(50)`, `Validators.pattern(/^[a-zA-Z0-9_]+$/)`
- Dodanie custom validatora `passwordMatchValidator` dla zgodności haseł
- Inicjalizacja formularza w konstruktorze

**2.2 Implementacja metod walidacji**:
- `isFieldInvalid(fieldName: string)` - sprawdzenie czy pole jest nieprawidłowe
- `getFieldError(fieldName: string)` - pobranie komunikatu błędu
- `passwordMatchValidator(form: AbstractControl)` - custom validator dla zgodności haseł
- `markFormGroupTouched(formGroup: FormGroup)` - oznaczenie wszystkich pól jako touched

### Krok 3: Implementacja template

**3.1 Utworzenie struktury HTML**:
- Kontener główny (`.auth-container`)
- Karta formularza (`.auth-card`)
- Nagłówek z tytułem
- Formularz z polami username, email, password, confirmPassword
- Przycisk submit
- Sekcja linków nawigacyjnych

**3.2 Dodanie PrimeNG komponentów**:
- `<input pInputText>` dla pola username i email
- `<p-password>` dla pola hasła i potwierdzenia hasła
- `<p-button>` dla przycisku submit
- `<p-toast>` dla powiadomień (opcjonalnie, jeśli używany MessageService)

**3.3 Dodanie komponentu PasswordStrengthIndicatorComponent**:
- Import komponentu
- Dodanie `<app-password-strength-indicator>` w template
- Przekazanie wartości hasła przez `[password]` input property

**3.4 Dodanie walidacji w template**:
- `[class.ng-invalid]` dla pól formularza
- `*ngIf` dla komunikatów błędów
- `[disabled]` dla przycisku submit
- `aria-describedby` i `aria-required` dla dostępności

### Krok 4: Implementacja integracji z API

**4.1 Utworzenie AuthService** (jeśli nie istnieje):
- `register(username: string, email: string, password: string): Observable<RegisterResponse>`
- `isAuthenticated(): boolean`
- `getCurrentUser(): Observable<User | null>`
- Metody do zarządzania tokenem JWT (zapis w localStorage)

**4.2 Implementacja metody onSubmit**:
- Sprawdzenie walidacji formularza
- Wywołanie `authService.register()`
- Obsługa odpowiedzi sukcesu i błędów
- Ustawienie `isLoading` podczas żądania

**4.3 Implementacja przekierowania po sukcesie**:
- Przekierowanie do `/` (strona główna) po pomyślnej rejestracji
- Toast notification z komunikatem sukcesu

### Krok 5: Implementacja obsługi błędów

**5.1 Implementacja metody handleRegisterError**:
- Sprawdzenie `error.status`
- Obsługa błędów 409, 422, 500
- Wyświetlanie odpowiednich komunikatów błędów przez MessageService

**5.2 Implementacja metody handleValidationErrors**:
- Parsowanie błędów walidacji z odpowiedzi API
- Ustawienie błędów walidacji w formularzu
- Oznaczenie pól jako touched

**5.3 Dodanie obsługi błędów sieci**:
- Sprawdzenie czy `error.status` jest 0 lub undefined
- Wyświetlanie komunikatu o braku połączenia

### Krok 6: Implementacja sprawdzania statusu uwierzytelnienia

**6.1 Implementacja ngOnInit**:
- Sprawdzenie `authService.isAuthenticated()`
- Przekierowanie do strony głównej jeśli użytkownik jest już zalogowany

### Krok 7: Implementacja komponentu PasswordStrengthIndicatorComponent

**7.1 Utworzenie plików komponentu** (jeśli nie istnieje):
- `frontend/src/app/components/ui/password-strength-indicator/password-strength-indicator.component.ts`
- `frontend/src/app/components/ui/password-strength-indicator/password-strength-indicator.component.html`
- `frontend/src/app/components/ui/password-strength-indicator/password-strength-indicator.component.scss`

**7.2 Implementacja logiki wskaźnika siły hasła**:
- Analiza hasła (długość, wielkie/małe litery, cyfry, znaki specjalne)
- Określenie poziomu siły (słabe, średnie, silne)
- Wyświetlanie wizualnych wskaźników

### Krok 8: Implementacja stylowania

**8.1 Utworzenie stylów SCSS**:
- Stylowanie `.auth-container` (centrowanie, padding)
- Stylowanie `.auth-card` (tło, cienie, zaokrąglenia)
- Stylowanie `.form-group` (marginesy, odstępy)
- Stylowanie `.error-message` (kolory, rozmiary czcionek)
- Stylowanie `.auth-links` (kolory linków, hover)

**8.2 Dodanie animacji**:
- Fade-in dla formularza (300ms)
- Smooth transitions dla przycisków
- Pulse animation dla błędów walidacji (opcjonalnie)
- Progress animation dla wskaźnika siły hasła

### Krok 9: Konfiguracja routingu

**9.1 Aktualizacja app.routes.ts**:
- Dodanie routingu dla `/auth/register`
- Konfiguracja jako child route pod `/auth` (jeśli używany AuthComponent jako layout)

**9.2 Aktualizacja nawigacji**:
- Sprawdzenie czy linki nawigacyjne w innych komponentach wskazują na `/auth/register`

### Krok 10: Implementacja i18n (opcjonalnie)

**10.1 Dodanie kluczy tłumaczeń**:
- Klucze dla etykiet pól formularza
- Klucze dla komunikatów błędów walidacji
- Klucze dla komunikatów błędów API (409, 422)
- Klucze dla linków nawigacyjnych

**10.2 Aktualizacja template**:
- Zastąpienie tekstów przez `{{ 'auth.register.title' | translate }}`
- Użycie `TranslateService` w komponencie dla dynamicznych komunikatów

### Krok 11: Testy jednostkowe

**11.1 Utworzenie pliku testowego**:
- `frontend/src/app/features/auth/auth-register.component.spec.ts`

**11.2 Implementacja testów**:
- Test inicjalizacji komponentu
- Test walidacji formularza
- Test walidacji zgodności haseł
- Test rejestracji z poprawnymi danymi
- Test obsługi błędów (409, 422, 500)
- Test przekierowania po sukcesie
- Test przekierowania gdy użytkownik jest już zalogowany

### Krok 12: Testy E2E (Cypress)

**12.1 Utworzenie pliku testowego**:
- `frontend/cypress/e2e/auth-register.cy.ts`

**12.2 Implementacja testów**:
- Scenariusz: Rejestracja z poprawnymi danymi
- Scenariusz: Rejestracja z nieprawidłowymi danymi (422)
- Scenariusz: Rejestracja z duplikatem nazwy użytkownika (409)
- Scenariusz: Rejestracja z duplikatem email (409)
- Scenariusz: Przekierowanie gdy użytkownik jest już zalogowany

### Krok 13: Dostępność (a11y)

**13.1 Dodanie ARIA labels**:
- `aria-describedby` dla pól formularza
- `aria-required="true"` dla wymaganych pól
- `aria-label` dla przycisku submit

**13.2 Implementacja keyboard navigation**:
- Obsługa Tab dla nawigacji między polami
- Obsługa Enter dla submit formularza
- Focus management po błędach walidacji

**13.3 Screen reader support**:
- Komunikaty błędów są powiązane z polami przez `aria-describedby`
- Komunikaty sukcesu są ogłaszane przez screen reader
- Wskaźnik siły hasła jest dostępny dla screen readera

### Krok 14: Code review i optymalizacja

**14.1 Code review**:
- Sprawdzenie zgodności z zasadami implementacji
- Sprawdzenie obsługi błędów
- Sprawdzenie dostępności

**14.2 Optymalizacja**:
- Sprawdzenie czy subskrypcje są prawidłowo czyszczone
- Sprawdzenie czy użyte są odpowiednie operatory RxJS
- Sprawdzenie czy nie ma memory leaks

### Krok 15: Dokumentacja i wdrożenie

**15.1 Dokumentacja**:
- Aktualizacja README z informacjami o komponencie
- Dokumentacja API w komentarzach (jeśli wymagane)

**15.2 Wdrożenie**:
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Test integracji z backendem

