# API Endpoint Implementation Plan: PUT /api/users/{userId}

## 1. Przegląd punktu końcowego

**PUT /api/users/{userId}** to endpoint służący do aktualizacji profilu użytkownika. Endpoint umożliwia modyfikację nazwy użytkownika (username) oraz innych publicznych danych profilowych. Endpoint jest chroniony autoryzacją - tylko właściciel profilu może dokonywać modyfikacji.

Endpoint zwraca zaktualizowane dane profilu użytkownika: statystyki gry (punkty, rozegrane gry, wygrane), metadane (data utworzenia i aktualizacji) oraz zaktualizowaną nazwę użytkownika. Email i inne wrażliwe dane nie są modyfikowane przez ten endpoint.

Kluczowe zastosowania:
- Zmiana nazwy użytkownika
- Aktualizacja publicznych danych profilowych
- Personalizacja profilu gracza

## 2. Szczegóły żądania

### Metoda HTTP
- **PUT** - operacja modyfikująca zasób, idempotentna

### Struktura URL
```
PUT /api/users/{userId}
```

### Nagłówki żądania

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT dla uwierzytelnienia
- `Content-Type: application/json` - format ciała żądania

**Opcjonalne:**
- `Accept: application/json` - preferowany format odpowiedzi

### Parametry URL

**Path Variables:**
- `userId` (Long) - ID użytkownika z tabeli `users.id`

### Query Parameters
- Brak parametrów zapytania

### Request Body
**`com.tbs.dto.user.UpdateUserRequest`** (istniejący)
```java
public record UpdateUserRequest(
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username
) {}
```

**Uwagi implementacyjne:**
- `username` - Nazwa użytkownika (3-50 znaków, opcjonalna)
- Walidacja Bean Validation na poziomie DTO

### Przykład żądania
```http
PUT /api/users/42 HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
Accept: application/json

{
  "username": "NewPlayerName"
}
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
**`com.tbs.dto.user.UpdateUserRequest`** (istniejący)
```java
public record UpdateUserRequest(
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username
) {}
```

**Uwagi implementacyjne:**
- `username` - Nazwa użytkownika (3-50 znaków)
- Walidacja Bean Validation: `@Size` z min=3, max=50
- Wymagana unikalność username w bazie danych

#### Response DTO
**`com.tbs.dto.user.UpdateUserResponse`** (istniejący)
```java
public record UpdateUserResponse(
    long userId,
    String username,
    boolean isGuest,
    long totalPoints,
    int gamesPlayed,
    int gamesWon,
    Instant updatedAt
) {}
```

**Uwagi implementacyjne:**
- `userId` - ID użytkownika z tabeli `users.id`
- `username` - Zaktualizowana nazwa użytkownika
- `isGuest` - Flaga wskazująca czy użytkownik jest gościem
- `totalPoints` - Z `users.total_points`
- `gamesPlayed` - Z `users.games_played`
- `gamesWon` - Z `users.games_won`
- `updatedAt` - Zaktualizowany timestamp z `users.updated_at`

### Enums
- Brak bezpośredniego użycia enumów w tym endpoincie

### Modele domenowe (do stworzenia)
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.UnauthorizedException`** - wyjątek dla 401 Unauthorized
- **`com.tbs.exception.ForbiddenException`** - wyjątek dla 403 Forbidden
- **`com.tbs.exception.UserNotFoundException`** - wyjątek dla 404 Not Found
- **`com.tbs.exception.ConflictException`** - wyjątek dla 409 Conflict (duplikat username)
- **`com.tbs.exception.BadRequestException`** - wyjątek dla 400 Bad Request (nieprawidłowa walidacja)

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.UserService`** - serwis zarządzający użytkownikami
- **`com.tbs.service.AuthenticationService`** - serwis obsługujący uwierzytelnienie

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Pomyślna aktualizacja profilu użytkownika

**Przykład odpowiedzi dla zarejestrowanego użytkownika:**
```json
{
  "userId": 42,
  "username": "NewPlayerName",
  "isGuest": false,
  "totalPoints": 3500,
  "gamesPlayed": 18,
  "gamesWon": 12,
  "updatedAt": "2024-01-20T15:30:00Z"
}
```

**Przykład odpowiedzi dla gościa:**
```json
{
  "userId": 123,
  "username": null,
  "isGuest": true,
  "totalPoints": 500,
  "gamesPlayed": 5,
  "gamesWon": 2,
  "updatedAt": "2024-01-20T15:30:00Z"
}
```

### Kody statusu błędów

**400 Bad Request** - Nieprawidłowe dane wejściowe
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Username must be between 3 and 50 characters",
    "details": {
      "field": "username",
      "violation": "size not in range"
    }
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**401 Unauthorized** - Brak uwierzytelnienia
```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required",
    "details": null
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**403 Forbidden** - Próba aktualizacji cudzego profilu
```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "You can only update your own profile",
    "details": null
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**404 Not Found** - Użytkownik nie znaleziony
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

**409 Conflict** - Nazwa użytkownika już istnieje
```json
{
  "error": {
    "code": "USERNAME_ALREADY_EXISTS",
    "message": "Username already exists",
    "details": null
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

1. **Odebranie żądania HTTP PUT /api/users/{userId}**
   - Parsowanie `userId` z path variable
   - Parsowanie ciała żądania (JSON)
   - Walidacja formatu `userId` (Long)
   - Weryfikacja tokenu JWT z nagłówka Authorization

2. **Walidacja `userId`**
   - Sprawdzenie czy `userId` jest poprawną liczbą
   - Sprawdzenie zakresu (np. > 0)
   - Jeśli nieprawidłowy format → 400 Bad Request

3. **Walidacja `UpdateUserRequest`**
   - Walidacja Bean Validation (`@Valid`)
   - Sprawdzenie długości username (3-50 znaków)
   - Jeśli nieprawidłowa walidacja → 400 Bad Request

4. **Uwierzytelnienie**
   - Weryfikacja tokenu JWT
   - Pobranie identyfikatora zalogowanego użytkownika
   - Jeśli brak tokenu lub nieprawidłowy → 401 Unauthorized

5. **Autoryzacja**
   - Sprawdzenie czy `userId` odpowiada zalogowanemu użytkownikowi
   - Jeśli nie → 403 Forbidden

6. **Pobranie użytkownika z bazy danych**
   - Zapytanie: `SELECT * FROM users WHERE id = ?`
   - Jeśli użytkownik nie istnieje → 404 Not Found

7. **Weryfikacja unikalności username**
   - Sprawdzenie czy nowy username nie jest już zajęty
   - Porównanie z obecnym username użytkownika
   - Jeśli duplikat → 409 Conflict

8. **Aktualizacja profilu użytkownika**
   - Ustawienie `username = {newValue}`
   - Zapytanie: `UPDATE users SET username = ?, updated_at = ? WHERE id = ?`
   - Automatyczna aktualizacja `updated_at` przez trigger

9. **Mapowanie i zwrócenie odpowiedzi**
   - Konwersja `User` entity → `UpdateUserResponse` DTO
   - Zwrócenie odpowiedzi HTTP 200 OK

### Integracja z bazą danych

**Tabela: `users`**
- Operacja UPDATE na podstawie `id`
- Kolumny aktualizowane:
  - `username` → nowa wartość z requesta
  - `updated_at` → automatyczna aktualizacja przez trigger

**Triggery:**
- `update_users_updated_at` - automatyczna aktualizacja `updated_at`

**Indeksy:**
- `idx_users_id` (PK, automatyczny) - szybki dostęp po ID
- `idx_users_username` (UNIQUE) - unikalność i szybkie wyszukiwanie

**Constraints:**
- UNIQUE constraint na `username`

### Integracja z Spring Security

**Autoryzacja:**
- Endpoint wymaga uwierzytelnienia (Bearer token)
- Weryfikacja, że `userId` odpowiada zalogowanemu użytkownikowi

**Konfiguracja Security:**
- `/api/users/{userId}` wymaga roli ROLE_USER
- Wyjątki dla nieuwierzytelnionych żądań → 401

## 6. Względy bezpieczeństwa

### Autoryzacja

**Weryfikacja własności:**
- Użytkownik może aktualizować tylko swój własny profil
- Porównanie `userId` z `currentUserId` z tokenu JWT
- Brak możliwości aktualizacji cudzego profilu

**Wrażliwe dane:**
- Email i hasło nie są modyfikowane przez ten endpoint
- Tylko publiczne dane profilowe (username) mogą być zmieniane

### Walidacja danych wejściowych

**Path Variable `userId`:**
- Walidacja formatu (Long)
- Sprawdzenie zakresu (np. > 0)
- Sanityzacja: parsowanie i walidacja przed użyciem w zapytaniu SQL

**Request Body `UpdateUserRequest`:**
- Walidacja Bean Validation (`@Valid`)
- Sprawdzenie długości username (3-50 znaków)
- Sprawdzenie formatu username (alfanumeryczny, podkreślniki, myślniki)

**Token JWT:**
- Weryfikacja sygnatury i ekspiracji tokenu
- Sprawdzenie roli użytkownika

### Ochrona przed atakami

**SQL Injection:**
- Użycie parametrówzowanych zapytań (JPA/Hibernate automatycznie)
- Walidacja `userId` i `username` przed użyciem w zapytaniu

**Path Traversal:**
- Walidacja formatu `userId` (tylko liczby)
- Sprawdzenie zakresu

**Authorization Bypass:**
- Sprawdzenie własności profilu przed aktualizacją
- Niezawodna weryfikacja tokenu JWT

**Uniqueness Violation:**
- Sprawdzenie unikalności username przed aktualizacją
- Wykorzystanie UNIQUE constraint w bazie danych

**Rate Limiting:**
- Ograniczenie szybkości dla endpointów modyfikujących dane: 10 żądań/minutę na użytkownika
- Implementacja przez Redis

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Nieprawidłowy format userId (400 Bad Request)
**Scenariusz:** `userId` w URL nie jest liczbą
```java
@PutMapping("/{userId}")
public ResponseEntity<UpdateUserResponse> updateUserProfile(
    @PathVariable String userId, 
    @Valid @RequestBody UpdateUserRequest request
) {
    try {
        Long userIdLong = Long.parseLong(userId);
    } catch (NumberFormatException e) {
        throw new BadRequestException("Invalid user ID format");
    }
}
```

#### 2. Nieprawidłowa walidacja danych (400 Bad Request)
**Scenariusz:** Username nie spełnia wymagań walidacji (za krótki, za długi)
```java
@PutMapping("/{userId}")
public ResponseEntity<UpdateUserResponse> updateUserProfile(
    @PathVariable Long userId, 
    @Valid @RequestBody UpdateUserRequest request
) {
    // Bean Validation automatycznie sprawdza @Size
    // Jeśli nieprawidłowe, zwraca 400
}
```

**Obsługa:**
- Bean Validation automatycznie sprawdza walidację
- Zwrócenie 400 Bad Request z szczegółami błędu walidacji

#### 3. Brak uwierzytelnienia (401 Unauthorized)
**Scenariusz:** Brak tokenu JWT w nagłówku Authorization
```java
String token = request.getHeader("Authorization");
if (token == null || !token.startsWith("Bearer ")) {
    throw new UnauthorizedException("Authentication required");
}
```

**Obsługa:**
- Sprawdzenie obecności tokenu przed przetwarzaniem żądania
- Zwrócenie 401 Unauthorized z komunikatem "Authentication required"

#### 4. Próba aktualizacji cudzego profilu (403 Forbidden)
**Scenariusz:** `userId` nie odpowiada zalogowanemu użytkownikowi
```java
Long currentUserId = authenticationService.getCurrentUserId();
if (!userId.equals(currentUserId)) {
    throw new ForbiddenException("You can only update your own profile");
}
```

**Obsługa:**
- Weryfikacja własności profilu
- Zwrócenie 403 Forbidden z odpowiednim komunikatem

#### 5. Użytkownik nie znaleziony (404 Not Found)
**Scenariusz:** Użytkownik z `userId` nie istnieje w bazie danych
```java
Optional<User> user = userRepository.findById(userId);
if (user.isEmpty()) {
    throw new UserNotFoundException("User not found");
}
```

**Obsługa:**
- Sprawdzenie czy użytkownik istnieje po pobraniu z bazy
- Zwrócenie 404 Not Found z komunikatem "User not found"

#### 6. Nazwa użytkownika już istnieje (409 Conflict)
**Scenariusz:** Podana nazwa użytkownika jest już zajęta przez innego gracza
```java
Optional<User> existingUser = userRepository.findByUsername(request.username());
if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
    throw new ConflictException("Username already exists");
}
```

**Obsługa:**
- Sprawdzenie unikalności username przed aktualizacją
- Zwrócenie 409 Conflict z komunikatem "Username already exists"

#### 7. Błąd bazy danych (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z bazą danych, timeout, błąd SQL
```java
@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ApiErrorResponse> handleDataAccessException(DataAccessException e) {
    log.error("Database error while updating user profile", e);
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
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        // 400 handling dla błędów walidacji
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException e) {
        // 401 handling
    }
    
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException e) {
        // 403 handling
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException e) {
        // 404 handling
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
- **INFO:** Pomyślna aktualizacja profilu użytkownika
- **WARN:** Próba dostępu do nieistniejącego użytkownika
- **WARN:** Próba aktualizacji cudzego profilu
- **WARN:** Próba użycia zajętej nazwy użytkownika
- **ERROR:** Błędy bazy danych

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, userId, stack trace (dla błędów)

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Indeksy:**
- Tabela `users` ma indeks na `id` (PK, automatyczny)
- Indeks `idx_users_username` (UNIQUE) dla szybkiego wyszukiwania i sprawdzania unikalności
- Zapytania używają indeksów

**Zapytania:**
- Pobranie użytkownika: `SELECT * FROM users WHERE id = ?`
- Sprawdzenie unikalności: `SELECT * FROM users WHERE username = ?`
- Aktualizacja: `UPDATE users SET username = ?, updated_at = ? WHERE id = ?`

### Cache'owanie

**Redis Cache (opcjonalne):**
- Klucz: `user:profile:{userId}`
- TTL: 5-15 minut
- Strategia: Cache-aside
- Inwalidacja: przy każdej aktualizacji profilu

**Korzyści:**
- Redukcja obciążenia bazy danych dla często przeglądanych profili
- Szybsze odpowiedzi dla powtarzających się żądań GET

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 10 żądań/minutę na użytkownika
- Klucz: `rate_limit:users:update:{userId}`

**Uzasadnienie:**
- Endpoint modyfikuje dane, więc powinien być ograniczony
- Zapobiega nadmiernym zmianom nazwy użytkownika
- Ochrona przed spamowaniem systemu

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="PUT",endpoint="/api/users/{userId}",status="200"}` - liczba pomyślnych aktualizacji
- `http_requests_total{method="PUT",endpoint="/api/users/{userId}",status="400"}` - liczba błędów walidacji
- `http_requests_total{method="PUT",endpoint="/api/users/{userId}",status="409"}` - liczba konfliktów username
- `http_request_duration_seconds{method="PUT",endpoint="/api/users/{userId}"}` - czas odpowiedzi

**Alerty:**
- Wysoki wskaźnik błędów 409 (>10% żądań) - problem z duplikatami username
- Długi czas odpowiedzi (>500ms)
- Wysoki wskaźnik błędów 500 (>1% żądań)

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `UpdateUserRequest` DTO istnieje ✅
- Weryfikacja czy `UpdateUserResponse` DTO istnieje ✅
- Sprawdzenie struktury pakietów
- Weryfikacja konfiguracji Spring Security

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.service.UserService` - serwis zarządzający użytkownikami
- `com.tbs.service.AuthenticationService` - serwis obsługujący uwierzytelnienie
- `com.tbs.exception.UnauthorizedException` - wyjątek dla 401
- `com.tbs.exception.ForbiddenException` - wyjątek dla 403
- `com.tbs.exception.UserNotFoundException` - wyjątek dla 404
- `com.tbs.exception.ConflictException` - wyjątek dla 409
- `com.tbs.exception.BadRequestException` - wyjątek dla 400

### Krok 2: Implementacja serwisu

**2.1 Utworzenie UserService:**
```java
@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    
    public UpdateUserResponse updateUserProfile(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        Optional<User> existingUser = userRepository.findByUsername(request.username());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            throw new ConflictException("Username already exists");
        }
        
        user.setUsername(request.username());
        userRepository.save(user);
        
        return mapToUpdateUserResponse(user);
    }
    
    private UpdateUserResponse mapToUpdateUserResponse(User user) {
        return new UpdateUserResponse(
            user.getId(),
            user.getUsername(),
            user.isGuest(),
            user.getTotalPoints(),
            user.getGamesPlayed(),
            user.getGamesWon(),
            user.getUpdatedAt()
        );
    }
}
```

**2.2 Testy serwisu:**
- Test jednostkowy z Mockito dla pomyślnego przypadku (200)
- Test dla przypadku gdy użytkownik nie istnieje (404)
- Test dla przypadku gdy username już istnieje (409)
- Test weryfikujący aktualizację profilu

### Krok 3: Implementacja kontrolera

**3.1 Utworzenie UserController:**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final AuthenticationService authenticationService;
    
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UpdateUserResponse> updateUserProfile(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        Long currentUserId = authenticationService.getCurrentUserId();
        
        if (!userId.equals(currentUserId)) {
            throw new ForbiddenException("You can only update your own profile");
        }
        
        UpdateUserResponse response = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}
```

**3.2 Konfiguracja Spring Security:**
- Konfiguracja `/api/users/{userId}` wymaga uwierzytelnienia
- Wyjątki dla nieuwierzytelnionych żądań → 401
- Weryfikacja roli ROLE_USER

**3.3 Testy kontrolera:**
- Test integracyjny z `@WebMvcTest` dla pomyślnego przypadku (200)
- Test dla przypadku gdy użytkownik nie istnieje (404)
- Test dla przypadku gdy brak uwierzytelnienia (401)
- Test dla przypadku gdy próba aktualizacji cudzego profilu (403)
- Test dla przypadku gdy username już istnieje (409)
- Test dla przypadku gdy nieprawidłowa walidacja (400)

### Krok 4: Implementacja obsługi błędów

**4.1 Utworzenie global exception handler:**
- Obsługa `MethodArgumentNotValidException` (400)
- Obsługa `UnauthorizedException` (401)
- Obsługa `ForbiddenException` (403)
- Obsługa `UserNotFoundException` (404)
- Obsługa `ConflictException` (409)
- Obsługa `DataAccessException` (500)

**4.2 Testy exception handler:**
- Test dla każdego typu wyjątku
- Weryfikacja formatu odpowiedzi błędu
- Weryfikacja kodów statusu HTTP

### Krok 5: Konfiguracja Swagger/OpenAPI

**5.1 Dodanie adnotacji Swagger:**
```java
@Operation(
    summary = "Update user profile",
    description = "Updates the profile of an authenticated user"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid input data"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "User not found"),
    @ApiResponse(responseCode = "409", description = "Username already exists")
})
@PutMapping("/{userId}")
public ResponseEntity<UpdateUserResponse> updateUserProfile(...) {
    // ...
}
```

### Krok 6: Implementacja cache'owania (opcjonalne)

**6.1 Konfiguracja Redis cache:**
- Dodanie `@EnableCaching` w klasie konfiguracyjnej
- Konfiguracja `RedisCacheManager`

**6.2 Dodanie cache do serwisu:**
```java
@CacheEvict(value = "userProfile", key = "#userId")
public UpdateUserResponse updateUserProfile(Long userId, UpdateUserRequest request) {
    // ...
}
```

### Krok 7: Testy integracyjne i E2E

**7.1 Testy integracyjne:**
- Test pełnego przepływu z bazą danych
- Test z rzeczywistym tokenem JWT
- Test weryfikujący UNIQUE constraint w bazie danych
- Test weryfikujący trigger `update_users_updated_at`

**7.2 Testy E2E (Cypress):**
- Test aktualizacji własnego profilu
- Test próby aktualizacji cudzego profilu (403)
- Test bez uwierzytelnienia (401)
- Test z zajętą nazwą użytkownika (409)
- Test z nieprawidłową walidacją (400)

### Krok 8: Dokumentacja i code review

**8.1 Dokumentacja:**
- Aktualizacja README z informacjami o endpoincie
- Dokumentacja Swagger/OpenAPI

**8.2 Code review:**
- Sprawdzenie zgodności z zasadami implementacji
- Review bezpieczeństwa
- Weryfikacja obsługi błędów

### Krok 9: Wdrożenie i monitoring

**9.1 Wdrożenie:**
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim

**9.2 Monitoring:**
- Konfiguracja metryk Prometheus
- Konfiguracja alertów dla wysokiego wskaźnika błędów
- Monitorowanie czasu odpowiedzi

## 10. Podsumowanie

Plan implementacji endpointu **PUT /api/users/{userId}** obejmuje kompleksowe podejście do wdrożenia z obsługą uwierzytelnienia, autoryzacji i walidacji. Kluczowe aspekty:

- **Bezpieczeństwo:** Uwierzytelnienie JWT, autoryzacja dostępu, ochrona przed nieautoryzowanymi modyfikacjami
- **Walidacja:** Bean Validation, sprawdzanie unikalności username
- **Wydajność:** Optymalizacja zapytań, opcjonalne cache'owanie
- **Obsługa błędów:** Centralna obsługa z odpowiednimi kodami statusu (400, 401, 403, 404, 409, 500)
- **Rate Limiting:** Ograniczenie częstotliwości wywołań (10/min)
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Dokumentacja:** Swagger/OpenAPI, dokumentacja kodu

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
