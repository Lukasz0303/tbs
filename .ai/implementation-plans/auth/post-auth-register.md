# API Endpoint Implementation Plan: POST /api/auth/register

## 1. Przegląd punktu końcowego

**POST /api/auth/register** to endpoint uwierzytelniania służący do rejestracji nowego konta użytkownika. Endpoint jest publiczny i nie wymaga uwierzytelnienia. Pozwala użytkownikowi utworzyć konto przez podanie adresu email, hasła i nazwy użytkownika.

Endpoint korzysta wyłącznie z wewnętrznego serwisu Spring Security: waliduje dane wejściowe, sprawdza unikalność w tabeli `users`, zapisuje hash hasła (BCrypt) i tworzy rekord profilu. Po pomyślnej rejestracji backend generuje parę tokenów JWT (access + refresh), zapisuje refresh token w Redis i automatycznie loguje użytkownika (httpOnly cookie).

Kluczowe zastosowania:
- Rejestracja nowych użytkowników w bazie PostgreSQL (`users`)
- Hashowanie haseł przy użyciu `PasswordEncoder` (BCrypt, cost ≥ 12)
- Generowanie tokenów access/refresh przez `JwtTokenProvider`
- Konfiguracja domyślnych statystyk/avatara i natychmiastowy login

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
- `userId` - String reprezentujący `users.id` (BIGINT) zwrócony po zapisie
- `username` - Nazwa użytkownika z `users.username`
- `email` - Email z `users.email`
- `isGuest` - Zawsze false dla zarejestrowanych użytkowników
- `totalPoints`/`gamesPlayed`/`gamesWon` - Ustawione na 0 przy tworzeniu
- `authToken` - Access token JWT wygenerowany przez Spring Security (również wystawiany w httpOnly cookie)

### Enums
- Brak bezpośredniego użycia enumów w tym endpoincie

### Modele domenowe (do stworzenia)
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`
  - Pola: `id`, `email`, `username`, `passwordHash`, `isGuest`, `avatar`, `stats`, `createdAt`
  - `authUserId` może pozostać `NULL` jako rezerwacja pod przyszłe integracje, ale nie bierze udziału w procesie

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.BadRequestException`** - wyjątek dla 400 Bad Request
- **`com.tbs.exception.ConflictException`** - wyjątek dla 409 Conflict (duplikat email/username)
- **`com.tbs.exception.ValidationException`** - wyjątek dla 422 Unprocessable Entity (błędy walidacji)

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.AuthService`** - rejestracja (walidacja, tworzenie użytkownika, tokeny)
- **`org.springframework.security.crypto.password.PasswordEncoder`** - hashowanie haseł (BCrypt)
- **`com.tbs.security.JwtTokenProvider`** / **TokenService** - generowanie access/refresh tokenów
- **`com.tbs.security.TokenBlacklistService`** / Redis store – przechowywanie refresh tokenów, czarna lista

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
   - `userRepository.existsByEmail(request.email())`
   - `userRepository.existsByUsername(request.username())`
   - Jeśli duplikat → 409 Conflict z odpowiednim komunikatem

4. **Hashowanie hasła**
   - `passwordEncoder.encode(request.password())`
   - Opcjonalnie: walidacja siły hasła (np. biblioteka zxcvbn)

5. **Utworzenie profilu użytkownika w tabeli `users`**
   - Utworzenie encji `User`:
     - `email` = request.email().trim().toLowerCase()
     - `username` = request.username().trim()
     - `passwordHash` = wynik BCrypt
     - `isGuest` = FALSE
     - `avatar` = request.avatar() lub domyślny (np. 1)
     - Statystyki = 0
   - Zapis przez `userRepository.save(user)`

6. **Generowanie tokenów i odpowiedzi**
   - `jwtTokenProvider.generateAccessToken(userId)`
   - `jwtTokenProvider.generateRefreshToken(userId)`
   - Zapis refresh tokena w Redis (klucz `refresh:{jti}`)
   - Mapowanie `User` → `RegisterResponse`, ustawienie `isGuest=false`
   - Ustawienie nagłówków `Set-Cookie` (httpOnly) dla obu tokenów

7. **Zwrócenie odpowiedzi HTTP 201 Created**
   - Serializacja `RegisterResponse` do JSON
   - Opcjonalny nagłówek `Location: /api/v1/users/{userId}`

### Integracja z bazą danych

**Tabela: `users`**
- INSERT nowego rekordu
- Kluczowe kolumny ustawiane podczas rejestracji:
  - `email` (VARCHAR, UNIQUE, lower-case)
  - `username` (VARCHAR(50), UNIQUE)
  - `password_hash` (VARCHAR, BCrypt)
  - `is_guest` = FALSE
  - `avatar` = wartość z zakresu 1-6 (domyślnie 1)
  - `total_points`, `games_played`, `games_won` = 0
  - `created_at`, `updated_at` = automatyczne znaczniki

**Sprawdzanie unikalności:**
- `SELECT 1 FROM users WHERE email = ?`
- `SELECT 1 FROM users WHERE username = ?`

### Transakcyjność

**Krytyczne operacje:**
1. Walidacja i zapis użytkownika w tabeli `users`
2. Generowanie tokenów + zapis refresh tokena w Redis

**Strategia:**
- Operacje bazodanowe wykonujemy w `@Transactional` – w razie błędu zapis zostanie wycofany
- Jeśli zapis do Redis się nie powiedzie po udanym INSERT, rzucamy wyjątek, aby transakcja została wycofana (użytkownik nie zostanie utworzony bez poprawnego tokena)
- Po sukcesie transakcji wysyłamy tokeny w cookie oraz odświeżamy cache profilu (jeśli istnieje)

## 6. Względy bezpieczeństwa

### Uwierzytelnianie

**Publiczny endpoint:**
- Endpoint nie wymaga uwierzytelnienia (publiczny)
- Jednak powinien mieć rate limiting, aby zapobiec spamowi rejestracji

### Walidacja danych wejściowych

**Email:**
- Walidacja formatu przez Bean Validation (@Email)
- Sanityzacja: `trim()` i konwersja do lower-case
- Sprawdzenie unikalności w tabeli `users` (SQL index)

**Hasło:**
- Minimalna długość: 8 znaków (@Size(min = 8)); można wymusić większą
- Hashowane lokalnie przy użyciu `PasswordEncoder` (BCrypt)
- Hasło nigdy nie jest przechowywane ani logowane w postaci jawnej

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

- Access token: ważność ~15 minut, podpisany `app.jwt.secret`
- Refresh token: ważność np. 7 dni, przechowywany w Redis (JTI → userId, exp)
- Tokeny wysyłane w httpOnly + Secure cookies (`wow-access-token`, `wow-refresh-token`) z nagłówkiem `Set-Cookie`
- Wymagane HTTPS/SameSite, aby zminimalizować ryzyko XSS/CSRF

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

#### 4. Błąd generowania tokenów / zapisu w Redis (500 Internal Server Error)
**Scenariusz:** Generator JWT zgłasza wyjątek albo Redis nie zapisuje refresh tokena
```java
try {
    String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
    tokenStore.saveRefreshToken(refreshJti, user.getId(), refreshExpiry);
} catch (JwtException | RedisConnectionFailureException e) {
    log.error("Failed to generate or persist auth tokens", e);
    throw new InternalServerException("Authentication service unavailable");
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
- **WARN:** Próba rejestracji z istniejącym email/username lub przekroczony limit
- **ERROR:** Błędy bazy danych, generowania tokenów lub zapisu w Redis

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, userId (jeśli dostępne), stack trace (dla błędów)
- **NIE logować**: haseł, tokenów, pełnych danych użytkownika

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Sprawdzanie unikalności:**
- Użycie indeksów UNIQUE na `email` i `username`
- Zapytania powinny korzystać z `EXISTS`, aby zatrzymać się przy pierwszym dopasowaniu

**Zapytania:**
- Sprawdzenie unikalności username: `SELECT 1 FROM users WHERE username = ?`
- Sprawdzenie unikalności email: `SELECT 1 FROM users WHERE email = ?`

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 5 rejestracji na godzinę z jednego IP (konfigurowalne)
- Klucz: `rate_limit:register:{ipAddress}`

**Korzyści:**
- Zapobieganie spamowi rejestracji
- Ochrona przed botami
- Sprawiedliwy podział zasobów

### Operacje Redis / tokeny

- Wszystkie operacje Redis powinny mieć timeout 2s i pojedynczy retry
- Refresh token przechowywany jako `refresh:{jti}` z TTL równym dacie wygaśnięcia
- Czarna lista access tokenów nie jest tworzona podczas rejestracji, ale refresh tokeny muszą być gotowe do unieważnienia (logout)

### Monitoring i metryki

**Metryki Prometheus:**
- `auth_register_attempts_total{status="success|conflict|error"}`
- `auth_register_duration_seconds`
- `rate_limit_registrations_blocked_total`
- `redis_commands_total{entity="refresh_token",command="set"}` – obserwacja stabilności Redis

**Alerty:**
- Wysoki wskaźnik błędów 409 (>50%) – możliwy bot lub problem UX
- Brak możliwości zapisu do Redis (spadek `redis_commands_total`, wzrost błędów)
- P95 czasu odpowiedzi > 1s

## 9. Etapy wdrożenia

1. **Infrastruktura**
   - Upewnij się, że tabela `users` posiada kolumnę `password_hash` oraz indeksy UNIQUE (`email`, `username`)
   - Skonfiguruj `PasswordEncoder`, `JwtTokenProvider`, `TokenBlacklistService`, `RedisTemplate` oraz `RateLimitingService`

2. **Logika serwisowa (`AuthService.register`)**
   - Walidacja biznesowa: `existsByEmail`, `existsByUsername`
   - Hashowanie hasła (BCrypt) i utworzenie encji `User`
   - Zapis użytkownika (`@Transactional`)
   - Generacja tokenów access/refresh + zapis refresh tokena w Redis
   - Mapowanie `User` → `RegisterResponse`

3. **Warstwa HTTP (`AuthController`)**
   - Endpoint `POST /api/v1/auth/register`
   - Integracja z `RateLimitingService` (IP/email)
   - Ustawienie `Set-Cookie` (HttpOnly, Secure, SameSite) dla obu tokenów oraz `Location`

4. **Obsługa błędów i metryki**
   - Rozszerzenie `GlobalExceptionHandler` (400/409/422/500)
   - Dodanie liczników Prometheus (`auth_register_attempts_total`, `auth_register_duration_seconds`, `rate_limit_registrations_blocked_total`)

5. **Testy**
   - Jednostkowe: sukces, duplikaty, błędy DB, błędy Redis/JWT
   - Integracyjne: MockMvc + Testcontainers (PostgreSQL + Redis)
   - E2E: Cypress – formularz rejestracji, błędy walidacji, komunikaty konfliktów

6. **Dokumentacja i monitoring**
   - Aktualizacja Swagger (`@Operation`, `@ApiResponses`), README i runbooków (np. procedura rotacji sekretów JWT)
   - Konfiguracja alertów (wzrost 409/500, awarie Redis)
   - Przygotowanie dashboardu z metrykami `auth_register_*`

## 10. Podsumowanie

Proces rejestracji jest teraz w pełni obsługiwany wewnętrznie przez Spring Security z wykorzystaniem PostgreSQL (przechowywanie profilu i hashy BCrypt) oraz Redis (refresh tokeny, rate limiting). Kluczowe filary:

- **Bezpieczeństwo:** Walidacja danych, hashowanie BCrypt, httpOnly cookies, rate limiting i audyt
- **Spójność:** Brak zależności od zewnętrznych dostawców – jeden przepływ w dev/test/prod
- **Operacyjność:** Metryki i alerty dla prób rejestracji, awarii DB/Redis oraz tokenów

Wdrożenie należy prowadzić krok po kroku zgodnie z sekcją 9, pilnując testów i monitoringu na każdym etapie, aby spełnić wymagania PRD dotyczące produkcyjnej gotowości systemu uwierzytelniania.
