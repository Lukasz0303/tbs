# API Endpoint Implementation Plan: POST /api/auth/login

## 1. Przegląd punktu końcowego

**POST /api/auth/login** to endpoint uwierzytelniania służący do logowania zarejestrowanego użytkownika. Endpoint jest publiczny i nie wymaga uwierzytelnienia. Pozwala użytkownikowi zalogować się przez podanie adresu email i hasła.

Endpoint wykorzystuje wewnętrzny serwis Spring Security do zweryfikowania poświadczeń użytkownika (hashowane hasła w tabeli `users`) i wydaje krótkotrwały token dostępu JWT (plus refresh token). Po pomyślnym zalogowaniu użytkownik otrzymuje również aktualne statystyki profilu (punkty, gry, avatar), a token jest dostarczany w nagłówku oraz w httpOnly cookie.

Kluczowe zastosowania:
- Logowanie zarejestrowanych użytkowników przeciwko lokalnej bazie (`users`)
- Hashowanie i walidacja haseł przy użyciu BCrypt (`PasswordEncoder`)
- Wydawanie pary tokenów (access + refresh) przez `JwtTokenProvider`/`TokenService`
- Opcjonalna rejestracja tokenów w Redis (blacklista / refresh store)
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
- `userId` - String reprezentujący BIGINT z `users.id`
- `username` - Nazwa użytkownika z `users.username`
- `email` - Email z `users.email`
- `isGuest` - Zawsze false dla zarejestrowanych użytkowników
- `totalPoints` - Z `users.total_points`
- `gamesPlayed` - Z `users.games_played`
- `gamesWon` - Z `users.games_won`
- `authToken` - Token JWT wydany przez Spring Security (może być również przesłany jako httpOnly cookie)

### Enums
- Brak bezpośredniego użycia enumów w tym endpoincie

### Modele domenowe (do stworzenia)
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.UnauthorizedException`** - wyjątek dla 401 Unauthorized
- **`com.tbs.exception.UserNotFoundException`** - wyjątek dla 404 Not Found

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.AuthService`** - serwis obsługujący logowanie (znajdowanie użytkownika, walidacja hasła)
- **`com.tbs.security.JwtTokenProvider`** / **`com.tbs.service.TokenService`** - generowanie tokenów access/refresh
- **`com.tbs.security.TokenBlacklistService`** - czarna lista tokenów (Redis)

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

3. **Wyszukanie użytkownika i walidacja poświadczeń**
   - `userRepository.findByEmail(request.email())`
   - Jeśli wynik pusty → rzucić `UnauthorizedException` (ogólny komunikat)
   - `passwordEncoder.matches(request.password(), user.getPasswordHash())`
   - Brak/niepoprawny hash → `UnauthorizedException`

4. **Generowanie tokenów i zapis w Redis**
   - `jwtTokenProvider.generateAccessToken(userId, roles, jti)`
   - `jwtTokenProvider.generateRefreshToken(userId, refreshJti)`
   - Persist refresh token metadata w Redis (klucz `refresh:{refreshJti}`)
   - Ewentualnie zaktualizować `lastSeenAt`

5. **Budowa odpowiedzi**
   - Mapowanie `User` → `LoginResponse`
   - Ustawienie `isGuest = false`
   - Dodanie nagłówków `Set-Cookie` dla `wow-access-token` i `wow-refresh-token` (HttpOnly, Secure, SameSite=Strict)
   - Opcjonalne dodanie `Authorization: Bearer <accessToken>` w ciele/nagłówku

6. **Zwrócenie odpowiedzi HTTP 200 OK**
   - Serializacja `LoginResponse` do JSON
   - Ustawienie nagłówków bezpieczeństwa (np. `Cache-Control: no-store`)

### Integracja z bazą danych

**Tabela: `users`**
- SELECT rekord na podstawie `email`
- Kolumny pobierane:
  - `id` → `userId`
  - `username` → `username`
  - `total_points` → `totalPoints`
  - `games_played` → `gamesPlayed`
  - `games_won` → `gamesWon`

## 6. Względy bezpieczeństwa

### Uwierzytelnianie

**Publiczny endpoint:**
- Endpoint nie wymaga uwierzytelnienia (publiczny)
- Jednak powinien mieć rate limiting, aby zapobiec brute force

**Mechanizm uwierzytelniania aplikacji:**
- Hasła przechowywane jako BCrypt (`users.password_hash`, cost ≥ 12)
- Walidacja lokalna przez `PasswordEncoder.matches`
- Tokeny access + refresh generowane przez `JwtTokenProvider`, wysyłane w httpOnly cookie + nagłówku
- Refresh token ma własny identyfikator (JTI) i jest przechowywany w Redis w celu możliwości unieważnienia

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
- Przechowywany jest wyłącznie hash BCrypt
- Można dodać zewnętrzny moduł analizy siły hasła (np. zxcvbn) po stronie klienta/serwera

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
**Scenariusz:** Nieprawidłowe hasło lub email nie istnieje w bazie
```java
User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
    throw new UnauthorizedException("Invalid email or password");
}
```

**Obsługa:**
- Zwrócenie 401 Unauthorized z ogólnym komunikatem "Invalid email or password"
- Logowanie próby logowania (bez szczegółów hasła)

#### 3. Użytkownik nie znaleziony (404 Not Found)
**Scenariusz:** (opcjonalne) włączone logowanie błędu gdy konto zostało dezaktywowane / soft-delete
```java
Optional<User> user = userRepository.findByEmail(request.email());
if (user.isEmpty()) {
    throw new UserNotFoundException("User not found");
}
```

**Obsługa:**
- Sprawdzenie czy użytkownik istnieje po pobraniu z bazy
- Zwrócenie 404 Not Found z komunikatem "User not found"
- Logowanie nieprawidłowego stanu (np. konto usunięte)

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
- **ERROR:** Błędy generowania tokenów, zapisu w Redis lub bazy danych

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, email (bez hasła), stack trace (dla błędów)
- **NIE logować**: haseł, tokenów, pełnych danych użytkownika

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Indeksy:**
- Kluczowy indeks: `idx_users_email` (UNIQUE)
- Dodatkowy indeks częściowy na `users.is_guest = false` (przyspiesza wyszukiwanie zarejestrowanych użytkowników)

**Zapytania:**
- Pobierać tylko wymagane kolumny (`id, username, email, stats`)
- Unikać dodatkowych JOIN-ów – wszystkie dane logowania znajdują się w tabeli `users`

### Zarządzanie tokenami i Redis

**Timeout i retry:**
- Operacje Redis (Lettuce) z timeoutem 2s, retry 1 raz
- W przypadku braku Redis login nadal może się udać, ale refresh token nie zostanie zapisany – należy zwrócić 500 (fail fast)

**Modele danych w Redis:**
- `refresh:{jti}` → JSON `{ "userId": 123, "exp": 1700000000 }` (TTL = exp)
- `auth:blacklist:{jti}` → `1` (TTL = exp access tokena)
- `rate_limit:login:{scope}` → licznik prób z TTL = okno czasowe

### Rate Limiting

- Limit IP: 5 prób / 15 minut
- Limit per email: 5 prób / 1 minuta (po normalizacji emaila do lower-case i hasha)
- Po przekroczeniu: 429 + `Retry-After` i nagłówki `X-RateLimit-*`

### Monitoring i metryki

- `auth_login_attempts_total{status="success|failure"}` – licznik prób logowania
- `auth_login_duration_seconds` – histogram czasu obsługi żądania
- `auth_refresh_tokens_active` – liczba aktywnych refresh tokenów (size of Redis set)
- `redis_commands_total{command="set",entity="refresh_token"}` – licznik operacji na Redisie
- Alert, gdy `failure/success > 0.5` w ciągu 15 minut lub brak możliwości zapisu do Redis

## 9. Etapy wdrożenia

1. **Przygotowanie infrastruktury**
   - Zapewnienie kolumny `password_hash` oraz indeksu na `users.email`
   - Weryfikacja konfiguracji `PasswordEncoder`, `JwtTokenProvider`, `TokenBlacklistService`, `RateLimitingService`

2. **Rozszerzenie `AuthService`**
   - Implementacja logiki: znajdź użytkownika → sprawdź hasło → wygeneruj tokeny → zapisz refresh token w Redis
   - Dodanie logowania audytowego i metryk

3. **Aktualizacja `AuthController`**
   - Obsługa nagłówków `Set-Cookie` (HttpOnly, Secure, SameSite=Strict/Lax)
   - Dodanie nagłówków `Cache-Control: no-store` i `Pragma: no-cache`

4. **Rate limiting i bezpieczeństwo**
   - Integracja `RateLimitingService` (klucze IP + email)
   - Dodanie audytu prób logowania

5. **Testy**
   - Jednostkowe (AuthService, JwtTokenProvider)
   - Integracyjne (MockMvc + Testcontainers dla PostgreSQL/Redis)
   - E2E (Cypress) – pozytywny scenariusz, błędne hasło, blokada rate limiting

6. **Monitoring i dokumentacja**
   - Eksport metryk do Prometheus/Grafana
   - Aktualizacja Swagger (`@Operation`), README, runbooków bezpieczeństwa

## 10. Podsumowanie

Zmieniony plan dla **POST /api/auth/login** zakłada w pełni samodzielne uwierzytelnianie oparte na Spring Security: lokalne przechowywanie hashy BCrypt, generowanie tokenów JWT, przechowywanie refresh tokenów i czarnej listy w Redis, a także wymuszenie bezpiecznych ciasteczek httpOnly. Priorytety:

- **Bezpieczeństwo:** hash BCrypt, httpOnly cookies, rate limiting, blacklista tokenów
- **Spójność:** brak zewnętrznych zależności (Supabase Auth), jeden przepływ dla wszystkich środowisk
- **Nadzór:** metryki Prometheus, logowanie audytowe i alerty

Implementację należy prowadzić krok po kroku (sekcja 9), testując każdy element zanim przejdziemy do kolejnego, aby zachować spójność z wymaganiami PRD i standardami architektonicznymi projektu.
