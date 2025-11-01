# API Endpoint Implementation Plan: POST /api/auth/register

## 1. Przegląd punktu końcowego

**POST /api/auth/register** to endpoint uwierzytelniania służący do rejestracji nowego konta użytkownika. Endpoint jest publiczny i nie wymaga uwierzytelnienia. Pozwala użytkownikowi utworzyć konto przez podanie adresu email, hasła i nazwy użytkownika.

Endpoint integruje się z Supabase Auth dla zarządzania uwierzytelnianiem użytkowników i tworzy odpowiedni profil w tabeli `users` z powiązaniem do `auth.users`. Po pomyślnej rejestracji użytkownik otrzymuje token JWT i jest automatycznie zalogowany.

Kluczowe zastosowania:
- Rejestracja nowych użytkowników
- Integracja z Supabase Auth dla zarządzania hasłami i uwierzytelnianiem
- Tworzenie profilu użytkownika z domyślnymi wartościami (punkty, statystyki)
- Automatyczne logowanie po rejestracji

## 2. Szczegóły żądania

### Metoda HTTP
- **POST** - operacja tworzenia zasobu

### Struktura URL
```
POST /api/auth/register
```

### Nagłówki żądania

**Wymagane:**
- `Content-Type: application/json` - format treści żądania

**Opcjonalne:**
- `Accept: application/json` - preferowany format odpowiedzi

### Parametry URL
- Brak parametrów URL

### Query Parameters
- Brak parametrów zapytania

### Request Body

**`RegisterRequest`** DTO:
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "username": "player1"
}
```

**Walidacja:**
- `email`: Wymagane, format email (@NotBlank, @Email)
- `password`: Wymagane, minimum 8 znaków (@NotBlank, @Size(min = 8))
- `username`: Wymagane, 3-50 znaków, unikalna (@NotBlank, @Size(min = 3, max = 50))

### Przykład żądania
```http
POST /api/auth/register HTTP/1.1
Host: api.example.com
Content-Type: application/json
Accept: application/json

{
  "email": "user@example.com",
  "password": "securePassword123",
  "username": "player1"
}
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
**`com.tbs.dto.auth.RegisterRequest`** (istniejący)
```java
public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username
) {}
```

#### Response DTO
**`com.tbs.dto.auth.RegisterResponse`** (istniejący)
```java
public record RegisterResponse(
    String userId,
    String username,
    String email,
    boolean isGuest,
    long totalPoints,
    int gamesPlayed,
    int gamesWon,
    String authToken
) {
    public RegisterResponse {
        isGuest = false;
    }
}
```

**Uwagi implementacyjne:**
- `userId` - String reprezentujący UUID z `users.id` lub `auth_user_id`
- `username` - Nazwa użytkownika z `users.username`
- `email` - Email z Supabase Auth (`auth.users.email`)
- `isGuest` - Zawsze false dla zarejestrowanych użytkowników
- `totalPoints` - Domyślnie 0 dla nowych użytkowników
- `gamesPlayed` - Domyślnie 0
- `gamesWon` - Domyślnie 0
- `authToken` - Token JWT wydany przez Supabase Auth lub Spring Security

### Enums
- Brak bezpośredniego użycia enumów w tym endpoincie

### Modele domenowe (do stworzenia)
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`
  - Pola zgodne z schematem bazy danych
  - Mapowanie do tabeli `users`
  - Powiązanie z `auth.users` przez `auth_user_id` (UUID)

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.BadRequestException`** - wyjątek dla 400 Bad Request
- **`com.tbs.exception.ConflictException`** - wyjątek dla 409 Conflict (duplikat email/username)
- **`com.tbs.exception.ValidationException`** - wyjątek dla 422 Unprocessable Entity (błędy walidacji)

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.AuthService`** - serwis obsługujący rejestrację i logowanie
- **`com.tbs.service.SupabaseAuthService`** - integracja z Supabase Auth API
- **`com.tbs.service.UserService`** - serwis zarządzający użytkownikami w tabeli `users`

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**201 Created** - Pomyślna rejestracja użytkownika

**Przykład odpowiedzi:**
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

### Kody statusu błędów

**400 Bad Request** - Nieprawidłowe dane wejściowe
```json
{
  "error": {
    "code": "BAD_REQUEST",
    "message": "Invalid input data",
    "details": {
      "email": "Email is required",
      "password": "Password must be at least 8 characters"
    }
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**409 Conflict** - Nazwa użytkownika lub email już istnieje
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

**422 Unprocessable Entity** - Błędy walidacji Bean Validation
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": {
      "username": "Username must be between 3 and 50 characters",
      "email": "Email must be valid"
    }
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**500 Internal Server Error** - Nieoczekiwany błąd serwera
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

## 5. Przepływ danych

### Sekwencja operacji

1. **Odebranie żądania HTTP POST /api/auth/register**
   - Walidacja formatu JSON
   - Parsowanie `RegisterRequest` DTO

2. **Walidacja danych wejściowych (Bean Validation)**
   - Walidacja adnotacji Bean Validation na `RegisterRequest`
   - Sprawdzenie formatu email (@Email)
   - Sprawdzenie długości hasła (@Size(min = 8))
   - Sprawdzenie długości username (@Size(min = 3, max = 50))
   - Jeśli błędy walidacji → 422 Unprocessable Entity

3. **Sprawdzenie unikalności email i username**
   - Zapytanie do bazy danych: czy email już istnieje w Supabase Auth lub w tabeli `users`
   - Zapytanie do bazy danych: czy username już istnieje w tabeli `users`
   - Jeśli duplikat → 409 Conflict

4. **Rejestracja w Supabase Auth**
   - Wywołanie Supabase Auth API: `POST /auth/v1/signup`
   - Przekazanie email i hasło
   - Supabase Auth tworzy użytkownika w `auth.users` i zwraca:
     - `user.id` (UUID) - `auth_user_id`
     - Token JWT (access_token i refresh_token)
   - Jeśli błąd Supabase Auth → 400/409/500 w zależności od błędu

5. **Utworzenie profilu użytkownika w tabeli `users`**
   - Wstawienie rekordu do tabeli `users`:
     - `auth_user_id` = UUID z Supabase Auth
     - `username` = z żądania
     - `is_guest` = FALSE
     - `total_points` = 0
     - `games_played` = 0
     - `games_won` = 0
     - `created_at` = NOW()
     - `updated_at` = NOW()
   - Jeśli błąd bazy danych → 500 Internal Server Error

6. **Generowanie odpowiedzi**
   - Mapowanie encji `User` → `RegisterResponse` DTO
   - Dodanie tokenu JWT z Supabase Auth
   - Ustawienie `isGuest = false`

7. **Zwrócenie odpowiedzi HTTP 201 Created**
   - Serializacja `RegisterResponse` do JSON
   - Ustawienie nagłówka `Location: /api/users/{userId}` (opcjonalne)

### Integracja z bazą danych

**Tabela: `users`**
- INSERT nowego rekordu
- Kolumny:
  - `auth_user_id` (UUID) - z Supabase Auth
  - `username` (VARCHAR(50), UNIQUE)
  - `is_guest` = FALSE
  - `total_points` = 0
  - `games_played` = 0
  - `games_won` = 0
  - `created_at` = NOW()
  - `updated_at` = NOW()

**Supabase Auth: `auth.users`**
- Użytkownik tworzony automatycznie przez Supabase Auth API
- Zawiera: email, encrypted password, metadata
- Foreign key: `users.auth_user_id` → `auth.users.id` (CASCADE DELETE)

**Sprawdzanie unikalności:**
- `SELECT COUNT(*) FROM users WHERE username = ?` - sprawdzenie username
- `SELECT COUNT(*) FROM users WHERE auth_user_id = ?` - sprawdzenie email przez Supabase Auth API

### Integracja z Supabase Auth

**Endpoint Supabase Auth:**
- `POST https://{supabase-url}/auth/v1/signup`
- Body: `{ "email": "...", "password": "..." }`
- Response: `{ "user": { "id": "uuid", ... }, "access_token": "...", "refresh_token": "..." }`

**Obsługa błędów Supabase Auth:**
- Email już istnieje → 409 Conflict
- Nieprawidłowy format → 400 Bad Request
- Błąd serwera Supabase → 500 Internal Server Error

### Transakcyjność

**Krytyczne operacje:**
1. Rejestracja w Supabase Auth
2. Utworzenie rekordu w tabeli `users`

**Strategia:**
- Jeśli Supabase Auth się powiedzie, ale INSERT do `users` się nie powiedzie:
  - Należy usunąć użytkownika z Supabase Auth (rollback)
  - Alternatywnie: zaimplementować zadanie czyszczące dla niekompletnych rejestracji

## 6. Względy bezpieczeństwa

### Uwierzytelnianie

**Publiczny endpoint:**
- Endpoint nie wymaga uwierzytelnienia (publiczny)
- Jednak powinien mieć rate limiting, aby zapobiec spamowi rejestracji

### Walidacja danych wejściowych

**Email:**
- Walidacja formatu przez Bean Validation (@Email)
- Sanityzacja: trim whitespace, lowercase conversion (opcjonalne)
- Sprawdzenie unikalności w Supabase Auth

**Hasło:**
- Minimalna długość: 8 znaków (@Size(min = 8))
- Hashowanie przez Supabase Auth (bcrypt/argon2)
- Hasło nigdy nie jest przechowywane w niezaszyfrowanej formie

**Username:**
- Długość: 3-50 znaków
- Sprawdzenie unikalności w tabeli `users`
- Sanityzacja: trim whitespace
- Walidacja formatu (alfanumeryczne + podkreślniki, opcjonalne)

### Ochrona przed atakami

**SQL Injection:**
- Użycie parametrówzowanych zapytań (JPA/Hibernate automatycznie)
- Brak dynamicznego SQL

**XSS (Cross-Site Scripting):**
- Sanityzacja danych wejściowych (username, email)
- Encoding danych w odpowiedzi JSON

**Brute Force / Spam rejestracji:**
- Rate limiting: np. 5 rejestracji na godzinę z jednego IP
- Implementacja przez Redis z algorytmem przesuwającego okna

**Email enumeration:**
- Unikanie różnicowania komunikatów błędów dla istniejących vs nieistniejących emaili
- Ogólny komunikat: "Email or username already exists" dla 409

**Password strength:**
- Minimalna długość: 8 znaków (konfigurowalna)
- Opcjonalnie: wymaganie złożoności (wielkie/małe litery, cyfry, znaki specjalne)

### Bezpieczeństwo tokenów

**JWT Token:**
- Token wydawany przez Supabase Auth lub Spring Security
- Wygaśnięcie: 15 minut (konfigurowalne)
- Refresh token: dla przedłużenia sesji
- Token w odpowiedzi powinien być bezpiecznie przekazywany (HTTPS)

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Nieprawidłowy format danych (400 Bad Request)
**Scenariusz:** Brak wymaganych pól, nieprawidłowy format JSON
```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ApiErrorResponse> handleBadRequest(HttpMessageNotReadableException e) {
    return ResponseEntity.status(400)
        .body(new ApiErrorResponse(
            new ErrorDetails("BAD_REQUEST", "Invalid request format", null)
        ));
}
```

#### 2. Błędy walidacji Bean Validation (422 Unprocessable Entity)
**Scenariusz:** Naruszenie adnotacji walidacji (@Email, @Size, @NotBlank)
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException e) {
    Map<String, String> errors = new HashMap<>();
    e.getBindingResult().getFieldErrors().forEach(error -> 
        errors.put(error.getField(), error.getDefaultMessage())
    );
    
    return ResponseEntity.status(422)
        .body(new ApiErrorResponse(
            new ErrorDetails("VALIDATION_ERROR", "Validation failed", errors)
        ));
}
```

#### 3. Email lub username już istnieje (409 Conflict)
**Scenariusz:** Próba rejestracji z istniejącym email lub username
```java
Optional<User> existingUser = userRepository.findByUsernameOrEmail(request.username(), request.email());
if (existingUser.isPresent()) {
    throw new ConflictException("Username or email already exists");
}
```

#### 4. Błąd Supabase Auth (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z Supabase Auth API, timeout, błąd serwera
```java
try {
    SupabaseAuthResponse response = supabaseAuthService.signUp(email, password);
} catch (SupabaseAuthException e) {
    log.error("Supabase Auth registration failed", e);
    throw new InternalServerException("Registration service unavailable");
}
```

#### 5. Błąd bazy danych (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z bazą danych, constraint violation, timeout
```java
try {
    User user = userRepository.save(newUser);
} catch (DataIntegrityViolationException e) {
    log.error("Database error during user creation", e);
    throw new InternalServerException("Database error occurred");
}
```

#### 6. Niekompletna transakcja (rollback)
**Scenariusz:** Supabase Auth się powiodło, ale INSERT do `users` się nie powiódł
```java
@Transactional
public RegisterResponse register(RegisterRequest request) {
    try {
        SupabaseAuthResponse authResponse = supabaseAuthService.signUp(...);
        User user = userRepository.save(newUser);
        return mapToResponse(user, authResponse.accessToken());
    } catch (Exception e) {
        // Rollback Supabase Auth user
        supabaseAuthService.deleteUser(authResponse.userId());
        throw e;
    }
}
```

### Global Exception Handler

**Struktura:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        // 422 handling
    }
    
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException e) {
        // 409 handling
    }
    
    @ExceptionHandler({DataAccessException.class, Exception.class})
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
        // 500 handling
    }
}
```

### Logowanie błędów

**Poziomy logowania:**
- **INFO:** Pomyślna rejestracja użytkownika (bez wrażliwych danych)
- **WARN:** Próba rejestracji z istniejącym email/username
- **ERROR:** Błędy Supabase Auth, błędy bazy danych, niekompletne transakcje

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, userId (jeśli dostępne), stack trace (dla błędów)
- **NIE logować**: haseł, tokenów, pełnych danych użytkownika

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Sprawdzanie unikalności:**
- Użycie indeksów UNIQUE na `username` i `auth_user_id`
- Zapytania sprawdzające unikalność powinny być szybkie dzięki indeksom

**Zapytania:**
- Sprawdzenie unikalności username: `SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)`
- Użycie EXISTS zamiast COUNT dla lepszej wydajności

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 5 rejestracji na godzinę z jednego IP (konfigurowalne)
- Klucz: `rate_limit:register:{ipAddress}`

**Korzyści:**
- Zapobieganie spamowi rejestracji
- Ochrona przed botami
- Sprawiedliwy podział zasobów

### Integracja z Supabase Auth

**Timeout i retry:**
- Timeout dla zapytań do Supabase Auth: 5 sekund
- Retry policy: 1 retry przy timeout/błędach sieciowych
- Connection pooling dla HTTP client

**Cache'owanie:**
- Nie cache'ować wyników rejestracji (zawsze fresh)
- Cache konfiguracji Supabase Auth (URL, klucze API) w Redis

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="POST",endpoint="/api/auth/register",status="201"}` - liczba pomyślnych rejestracji
- `http_requests_total{method="POST",endpoint="/api/auth/register",status="409"}` - liczba prób duplikacji
- `http_request_duration_seconds{method="POST",endpoint="/api/auth/register"}` - czas odpowiedzi
- `supabase_auth_calls_total{operation="signup",status="success|error"}` - metryki Supabase Auth

**Alerty:**
- Wysoki wskaźnik błędów 409 (>50% żądań) - możliwe problemy z duplikacją
- Długi czas odpowiedzi (>2s) - problem z Supabase Auth lub bazą danych
- Wysoki wskaźnik błędów 500 (>1% żądań) - problem z infrastrukturą

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `RegisterRequest` i `RegisterResponse` DTO istnieją
- Sprawdzenie konfiguracji Supabase Auth
- Weryfikacja struktury pakietów

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.service.AuthService` - serwis obsługujący rejestrację
- `com.tbs.service.SupabaseAuthService` - integracja z Supabase Auth API
- `com.tbs.exception.ConflictException` - wyjątek dla 409
- `com.tbs.exception.ValidationException` - wyjątek dla 422

**1.3 Konfiguracja zależności:**
- HTTP client dla Supabase Auth (RestTemplate/WebClient)
- Konfiguracja Supabase URL i API keys (application.properties)

### Krok 2: Implementacja integracji z Supabase Auth

**2.1 Utworzenie SupabaseAuthService:**
```java
@Service
public class SupabaseAuthService {
    private final WebClient webClient;
    private final String supabaseUrl;
    private final String supabaseAnonKey;
    
    public SupabaseAuthResponse signUp(String email, String password) {
        // Wywołanie POST /auth/v1/signup
        // Obsługa błędów
    }
    
    public void deleteUser(UUID userId) {
        // Usunięcie użytkownika z Supabase Auth (rollback)
    }
}
```

**2.2 Testy integracyjne Supabase Auth:**
- Test pomyślnej rejestracji
- Test z istniejącym emailem
- Test z nieprawidłowym formatem

### Krok 3: Implementacja serwisu rejestracji

**3.1 Utworzenie AuthService:**
```java
@Service
@Transactional
public class AuthService {
    private final SupabaseAuthService supabaseAuthService;
    private final UserRepository userRepository;
    
    public RegisterResponse register(RegisterRequest request) {
        // 1. Walidacja unikalności
        // 2. Rejestracja w Supabase Auth
        // 3. Utworzenie profilu w users
        // 4. Mapowanie do response
    }
}
```

**3.2 Testy serwisu:**
- Test jednostkowy z Mockito dla pomyślnej rejestracji
- Test dla przypadku duplikatu email/username
- Test dla przypadku błędu Supabase Auth
- Test dla przypadku błędu bazy danych

### Krok 4: Implementacja kontrolera

**4.1 Utworzenie AuthController:**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(201).body(response);
    }
}
```

**4.2 Konfiguracja Spring Security:**
- Upewnienie się, że `/api/auth/register` jest publiczny (permitAll)
- Konfiguracja CORS jeśli potrzebne

**4.3 Testy kontrolera:**
- Test integracyjny z `@WebMvcTest` dla pomyślnego przypadku (201)
- Test dla przypadku błędów walidacji (422)
- Test dla przypadku duplikatu (409)
- Test dla przypadku błędu serwera (500)

### Krok 5: Implementacja obsługi błędów

**5.1 Utworzenie global exception handler:**
- Obsługa `MethodArgumentNotValidException` (422)
- Obsługa `ConflictException` (409)
- Obsługa `DataAccessException` (500)
- Obsługa `SupabaseAuthException` (500)

**5.2 Testy exception handler:**
- Test dla każdego typu wyjątku
- Weryfikacja formatu odpowiedzi błędu

### Krok 6: Implementacja rate limiting

**6.1 Konfiguracja rate limiting:**
- Implementacja filtru Spring Security lub interceptor
- Integracja z Redis

**6.2 Dodanie rate limiting do endpointu:**
- Limit: 5 rejestracji/godzinę na IP
- Obsługa przekroczenia limitu (429 Too Many Requests)

### Krok 7: Konfiguracja Swagger/OpenAPI

**7.1 Dodanie adnotacji Swagger:**
```java
@Operation(
    summary = "Register new user",
    description = "Creates a new user account with email, password, and username"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "User registered successfully"),
    @ApiResponse(responseCode = "409", description = "Username or email already exists"),
    @ApiResponse(responseCode = "422", description = "Validation errors")
})
@PostMapping("/register")
public ResponseEntity<RegisterResponse> register(...) {
    // ...
}
```

### Krok 8: Testy integracyjne i E2E

**8.1 Testy integracyjne:**
- Test pełnego przepływu z Supabase Auth (testcontainers lub mock)
- Test z rzeczywistą bazą danych
- Test transakcyjności (rollback)

**8.2 Testy E2E (Cypress):**
- Test rejestracji nowego użytkownika
- Test obsługi błędów walidacji
- Test obsługi duplikatu email/username

### Krok 9: Dokumentacja i code review

**9.1 Dokumentacja:**
- Aktualizacja README z informacjami o endpoincie
- Dokumentacja Swagger/OpenAPI
- Dokumentacja integracji z Supabase Auth

**9.2 Code review:**
- Sprawdzenie zgodności z zasadami implementacji
- Review bezpieczeństwa
- Weryfikacja obsługi błędów

### Krok 10: Wdrożenie i monitoring

**10.1 Wdrożenie:**
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Test integracji z Supabase Auth na dev

**10.2 Monitoring:**
- Konfiguracja metryk Prometheus
- Konfiguracja alertów dla wysokiego wskaźnika błędów
- Monitorowanie czasu odpowiedzi Supabase Auth

## 10. Podsumowanie

Plan implementacji endpointu **POST /api/auth/register** obejmuje kompleksowe podejście do wdrożenia z integracją Supabase Auth. Kluczowe aspekty:

- **Bezpieczeństwo:** Walidacja danych, ochrona przed spamem, rate limiting
- **Integracja:** Supabase Auth dla zarządzania uwierzytelnianiem
- **Transakcyjność:** Obsługa rollback dla niekompletnych rejestracji
- **Obsługa błędów:** Centralna obsługa z odpowiednimi kodami statusu
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Monitoring:** Metryki i alerty dla Supabase Auth i bazy danych

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
