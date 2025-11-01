# API Endpoint Implementation Plan: POST /api/auth/login

## 1. Przegląd punktu końcowego

**POST /api/auth/login** to endpoint uwierzytelniania służący do logowania zarejestrowanego użytkownika. Endpoint jest publiczny i nie wymaga uwierzytelnienia. Pozwala użytkownikowi zalogować się przez podanie adresu email i hasła.

Endpoint integruje się z Supabase Auth dla uwierzytelniania użytkowników i zwraca token JWT po pomyślnym zalogowaniu. Po uwierzytelnieniu użytkownik otrzymuje token JWT oraz pełne informacje o profilu (statystyki gry, punkty).

Kluczowe zastosowania:
- Logowanie zarejestrowanych użytkowników
- Integracja z Supabase Auth dla walidacji hasła
- Wydawanie tokenu JWT po pomyślnym logowaniu
- Pobranie aktualnych statystyk użytkownika

## 2. Szczegóły żądania

### Metoda HTTP
- **POST** - operacja uwierzytelniania

### Struktura URL
```
POST /api/auth/login
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

**`LoginRequest`** DTO:
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Walidacja:**
- `email`: Wymagane, format email (@NotBlank, @Email)
- `password`: Wymagane (@NotBlank)

### Przykład żądania
```http
POST /api/auth/login HTTP/1.1
Host: api.example.com
Content-Type: application/json
Accept: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
**`com.tbs.dto.auth.LoginRequest`** (istniejący)
```java
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Password is required")
    String password
) {}
```

#### Response DTO
**`com.tbs.dto.auth.LoginResponse`** (istniejący)
```java
public record LoginResponse(
    String userId,
    String username,
    String email,
    boolean isGuest,
    long totalPoints,
    int gamesPlayed,
    int gamesWon,
    String authToken
) {
    public LoginResponse {
        isGuest = false;
    }
}
```

**Uwagi implementacyjne:**
- `userId` - String reprezentujący UUID lub BIGINT z `users.id`
- `username` - Nazwa użytkownika z `users.username`
- `email` - Email z Supabase Auth (`auth.users.email`)
- `isGuest` - Zawsze false dla zarejestrowanych użytkowników
- `totalPoints` - Z `users.total_points`
- `gamesPlayed` - Z `users.games_played`
- `gamesWon` - Z `users.games_won`
- `authToken` - Token JWT wydany przez Supabase Auth lub Spring Security

### Enums
- Brak bezpośredniego użycia enumów w tym endpoincie

### Modele domenowe (do stworzenia)
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.UnauthorizedException`** - wyjątek dla 401 Unauthorized
- **`com.tbs.exception.UserNotFoundException`** - wyjątek dla 404 Not Found

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.AuthService`** - serwis obsługujący logowanie
- **`com.tbs.service.SupabaseAuthService`** - integracja z Supabase Auth API

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Pomyślne logowanie użytkownika

**Przykład odpowiedzi:**
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

### Kody statusu błędów

**401 Unauthorized** - Nieprawidłowe dane uwierzytelniające (nieprawidłowe hasło)
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

**404 Not Found** - Użytkownik nie znaleziony (email nie istnieje)
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

**422 Unprocessable Entity** - Błędy walidacji Bean Validation
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

1. **Odebranie żądania HTTP POST /api/auth/login**
   - Walidacja formatu JSON
   - Parsowanie `LoginRequest` DTO

2. **Walidacja danych wejściowych (Bean Validation)**
   - Walidacja adnotacji Bean Validation na `LoginRequest`
   - Sprawdzenie formatu email (@Email)
   - Sprawdzenie obecności hasła (@NotBlank)
   - Jeśli błędy walidacji → 422 Unprocessable Entity

3. **Uwierzytelnienie w Supabase Auth**
   - Wywołanie Supabase Auth API: `POST /auth/v1/token`
   - Przekazanie email i hasło (grant_type: password)
   - Supabase Auth waliduje hasło i zwraca:
     - `user.id` (UUID) - `auth_user_id`
     - Token JWT (access_token i refresh_token)
   - Jeśli nieprawidłowe hasło → 401 Unauthorized
   - Jeśli użytkownik nie istnieje → 404 Not Found

4. **Pobranie profilu użytkownika z tabeli `users`**
   - Zapytanie: `SELECT * FROM users WHERE auth_user_id = ?`
   - Jeśli użytkownik nie istnieje w tabeli `users` (nieprawidłowy stan) → 404 Not Found

5. **Generowanie odpowiedzi**
   - Mapowanie encji `User` → `LoginResponse` DTO
   - Dodanie tokenu JWT z Supabase Auth
   - Ustawienie `isGuest = false`

6. **Zwrócenie odpowiedzi HTTP 200 OK**
   - Serializacja `LoginResponse` do JSON

### Integracja z bazą danych

**Tabela: `users`**
- SELECT rekord na podstawie `auth_user_id`
- Kolumny pobierane:
  - `id` → `userId`
  - `username` → `username`
  - `total_points` → `totalPoints`
  - `games_played` → `gamesPlayed`
  - `games_won` → `gamesWon`

**Supabase Auth: `auth.users`**
- Użytkownik uwierzytelniany przez Supabase Auth API
- Hasło walidowane przez Supabase Auth (bcrypt/argon2)

### Integracja z Supabase Auth

**Endpoint Supabase Auth:**
- `POST https://{supabase-url}/auth/v1/token`
- Body: `{ "email": "...", "password": "...", "grant_type": "password" }`
- Response: `{ "access_token": "...", "refresh_token": "...", "user": {...} }`

**Obsługa błędów Supabase Auth:**
- Nieprawidłowe hasło → 401 Unauthorized
- Użytkownik nie istnieje → 404 Not Found
- Błąd serwera Supabase → 500 Internal Server Error

## 6. Względy bezpieczeństwa

### Uwierzytelnianie

**Publiczny endpoint:**
- Endpoint nie wymaga uwierzytelnienia (publiczny)
- Jednak powinien mieć rate limiting, aby zapobiec brute force

**Mechanizm Supabase Auth:**
- Walidacja hasła przez Supabase Auth (bcrypt/argon2)
- Hasło nigdy nie jest przesyłane ani przechowywane w niezaszyfrowanej formie
- Token JWT wydawany po pomyślnej walidacji

### Ochrona przed atakami

**Brute Force:**
- Rate limiting: np. 5 prób logowania na 15 minut z jednego IP
- Implementacja przez Redis z algorytmem przesuwającego okna
- Po przekroczeniu limitu: 429 Too Many Requests

**Account enumeration:**
- Unikanie różnicowania komunikatów błędów dla istniejących vs nieistniejących użytkowników
- Ogólny komunikat: "Invalid email or password" dla 401 (nawet jeśli email nie istnieje)

**Timing attacks:**
- Stały czas odpowiedzi niezależnie od wyniku walidacji (opcjonalne, zaawansowane)

**Password security:**
- Hasło nigdy nie jest logowane
- Hasło nigdy nie jest zwracane w odpowiedzi
- Walidacja przez Supabase Auth (nie lokalnie)

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 5 prób logowania na 15 minut z jednego IP
- Klucz: `rate_limit:login:{ipAddress}`
- Po przekroczeniu: 429 Too Many Requests

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Błędy walidacji Bean Validation (422 Unprocessable Entity)
**Scenariusz:** Naruszenie adnotacji walidacji (@Email, @NotBlank)
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

#### 2. Nieprawidłowe dane uwierzytelniające (401 Unauthorized)
**Scenariusz:** Nieprawidłowe hasło lub email nie istnieje w Supabase Auth
```java
try {
    SupabaseAuthResponse response = supabaseAuthService.signIn(email, password);
} catch (SupabaseAuthException e) {
    if (e.getStatusCode() == 401) {
        throw new UnauthorizedException("Invalid email or password");
    }
    throw e;
}
```

**Obsługa:**
- Zwrócenie 401 Unauthorized z ogólnym komunikatem "Invalid email or password"
- Logowanie próby logowania (bez szczegółów hasła)

#### 3. Użytkownik nie znaleziony (404 Not Found)
**Scenariusz:** Email istnieje w Supabase Auth, ale użytkownik nie istnieje w tabeli `users`
```java
Optional<User> user = userRepository.findByAuthUserId(authUserId);
if (user.isEmpty()) {
    throw new UserNotFoundException("User not found");
}
```

**Obsługa:**
- Sprawdzenie czy użytkownik istnieje po pobraniu z bazy
- Zwrócenie 404 Not Found z komunikatem "User not found"
- Logowanie nieprawidłowego stanu (użytkownik w Supabase Auth, ale nie w `users`)

#### 4. Błąd Supabase Auth (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z Supabase Auth API, timeout, błąd serwera
```java
try {
    SupabaseAuthResponse response = supabaseAuthService.signIn(email, password);
} catch (SupabaseAuthException e) {
    log.error("Supabase Auth login failed", e);
    throw new InternalServerException("Authentication service unavailable");
}
```

#### 5. Błąd bazy danych (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z bazą danych, timeout, błąd SQL
```java
@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ApiErrorResponse> handleDataAccessException(DataAccessException e) {
    log.error("Database error while fetching user profile", e);
    return ResponseEntity.status(500)
        .body(new ApiErrorResponse(
            new ErrorDetails("INTERNAL_SERVER_ERROR", "Database error occurred", null)
        ));
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
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException e) {
        // 401 handling
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException e) {
        // 404 handling
    }
    
    @ExceptionHandler({DataAccessException.class, Exception.class})
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
        // 500 handling
    }
}
```

### Logowanie błędów

**Poziomy logowania:**
- **INFO:** Pomyślne logowanie użytkownika (bez wrażliwych danych)
- **WARN:** Nieudane próby logowania (dla monitoringu bezpieczeństwa)
- **ERROR:** Błędy Supabase Auth, błędy bazy danych

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, email (bez hasła), stack trace (dla błędów)
- **NIE logować**: haseł, tokenów, pełnych danych użytkownika

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Indeksy:**
- Tabela `users` powinna mieć indeks na `auth_user_id` (UNIQUE, partial)
- Zapytania powinny używać indeksów (EXPLAIN ANALYZE)

**Zapytania:**
- Pobieranie tylko wymaganych kolumn (nie SELECT *)
- Użycie `EXISTS` zamiast `COUNT` jeśli możliwe

### Integracja z Supabase Auth

**Timeout i retry:**
- Timeout dla zapytań do Supabase Auth: 5 sekund
- Retry policy: 1 retry przy timeout/błędach sieciowych
- Connection pooling dla HTTP client

**Cache'owanie:**
- Nie cache'ować wyników logowania (zawsze fresh)
- Cache konfiguracji Supabase Auth w Redis

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 5 prób logowania na 15 minut z jednego IP
- Klucz: `rate_limit:login:{ipAddress}`

**Korzyści:**
- Zapobieganie brute force attacks
- Ochrona przed botami
- Sprawiedliwy podział zasobów

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="POST",endpoint="/api/auth/login",status="200"}` - liczba pomyślnych logowań
- `http_requests_total{method="POST",endpoint="/api/auth/login",status="401"}` - liczba nieudanych prób
- `http_request_duration_seconds{method="POST",endpoint="/api/auth/login"}` - czas odpowiedzi
- `supabase_auth_calls_total{operation="signin",status="success|error"}` - metryki Supabase Auth

**Alerty:**
- Wysoki wskaźnik błędów 401 (>50% żądań) - możliwe brute force
- Długi czas odpowiedzi (>2s) - problem z Supabase Auth lub bazą danych
- Wysoki wskaźnik błędów 500 (>1% żądań) - problem z infrastrukturą

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `LoginRequest` i `LoginResponse` DTO istnieją
- Sprawdzenie konfiguracji Supabase Auth
- Weryfikacja struktury pakietów

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.service.AuthService` - serwis obsługujący logowanie
- `com.tbs.service.SupabaseAuthService` - integracja z Supabase Auth API
- `com.tbs.exception.UnauthorizedException` - wyjątek dla 401
- `com.tbs.exception.UserNotFoundException` - wyjątek dla 404

**1.3 Konfiguracja zależności:**
- HTTP client dla Supabase Auth (RestTemplate/WebClient)
- Konfiguracja Supabase URL i API keys

### Krok 2: Implementacja integracji z Supabase Auth

**2.1 Utworzenie SupabaseAuthService:**
```java
@Service
public class SupabaseAuthService {
    private final WebClient webClient;
    private final String supabaseUrl;
    private final String supabaseAnonKey;
    
    public SupabaseAuthResponse signIn(String email, String password) {
        // Wywołanie POST /auth/v1/token
        // Obsługa błędów
    }
}
```

**2.2 Testy integracyjne Supabase Auth:**
- Test pomyślnego logowania
- Test z nieprawidłowym hasłem
- Test z nieistniejącym emailem

### Krok 3: Implementacja serwisu logowania

**3.1 Utworzenie AuthService:**
```java
@Service
@Transactional(readOnly = true)
public class AuthService {
    private final SupabaseAuthService supabaseAuthService;
    private final UserRepository userRepository;
    
    public LoginResponse login(LoginRequest request) {
        // 1. Uwierzytelnienie w Supabase Auth
        // 2. Pobranie profilu z users
        // 3. Mapowanie do response
    }
}
```

**3.2 Testy serwisu:**
- Test jednostkowy z Mockito dla pomyślnego logowania
- Test dla przypadku nieprawidłowego hasła (401)
- Test dla przypadku nieistniejącego użytkownika (404)
- Test dla przypadku błędu Supabase Auth

### Krok 4: Implementacja kontrolera

**4.1 Utworzenie AuthController:**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
```

**4.2 Konfiguracja Spring Security:**
- Upewnienie się, że `/api/auth/login` jest publiczny (permitAll)
- Konfiguracja CORS jeśli potrzebne

**4.3 Testy kontrolera:**
- Test integracyjny z `@WebMvcTest` dla pomyślnego przypadku (200)
- Test dla przypadku błędów walidacji (422)
- Test dla przypadku nieprawidłowego hasła (401)
- Test dla przypadku nieistniejącego użytkownika (404)

### Krok 5: Implementacja obsługi błędów

**5.1 Utworzenie global exception handler:**
- Obsługa `MethodArgumentNotValidException` (422)
- Obsługa `UnauthorizedException` (401)
- Obsługa `UserNotFoundException` (404)
- Obsługa `SupabaseAuthException` (500)

**5.2 Testy exception handler:**
- Test dla każdego typu wyjątku
- Weryfikacja formatu odpowiedzi błędu

### Krok 6: Implementacja rate limiting

**6.1 Konfiguracja rate limiting:**
- Implementacja filtru Spring Security lub interceptor
- Integracja z Redis

**6.2 Dodanie rate limiting do endpointu:**
- Limit: 5 prób na 15 minut na IP
- Obsługa przekroczenia limitu (429 Too Many Requests)

### Krok 7: Konfiguracja Swagger/OpenAPI

**7.1 Dodanie adnotacji Swagger:**
```java
@Operation(
    summary = "Login user",
    description = "Authenticates and logs in a registered user"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "User logged in successfully"),
    @ApiResponse(responseCode = "401", description = "Invalid email or password"),
    @ApiResponse(responseCode = "404", description = "User not found")
})
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(...) {
    // ...
}
```

### Krok 8: Testy integracyjne i E2E

**8.1 Testy integracyjne:**
- Test pełnego przepływu z Supabase Auth
- Test z rzeczywistą bazą danych
- Test rate limiting

**8.2 Testy E2E (Cypress):**
- Test logowania zarejestrowanego użytkownika
- Test obsługi błędów walidacji
- Test obsługi nieprawidłowego hasła

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
- Konfiguracja alertów dla wysokiego wskaźnika błędów 401
- Monitorowanie czasu odpowiedzi Supabase Auth

## 10. Podsumowanie

Plan implementacji endpointu **POST /api/auth/login** obejmuje kompleksowe podejście do wdrożenia z integracją Supabase Auth. Kluczowe aspekty:

- **Bezpieczeństwo:** Ochrona przed brute force, rate limiting, bezpieczne zarządzanie hasłami
- **Integracja:** Supabase Auth dla uwierzytelniania
- **Obsługa błędów:** Centralna obsługa z odpowiednimi kodami statusu
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Monitoring:** Metryki i alerty dla Supabase Auth i bezpieczeństwa

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
