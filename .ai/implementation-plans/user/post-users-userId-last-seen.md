# API Endpoint Implementation Plan: POST /api/v1/users/{userId}/last-seen

## 1. Przegląd punktu końcowego

**POST /api/v1/users/{userId}/last-seen** to endpoint służący do aktualizacji znacznika czasu ostatniej aktywności użytkownika. Endpoint jest kluczowy dla systemu matchmakingu, aby identyfikować aktywnych graczy i dopasowywać ich do gier w czasie rzeczywistym.

Endpoint wymaga uwierzytelnienia i autoryzacji - użytkownik może aktualizować tylko swój własny `last_seen_at`. Automatyczna aktualizacja znacznika czasu pozwala systemowi matchmakingu na efektywne łączenie graczy, którzy są obecnie aktywni w aplikacji.

Kluczowe zastosowania:
- Oznaczenie aktywności użytkownika dla systemu matchmakingu
- Identyfikacja aktywnych graczy w kolejce matchmaking
- Kontrola timeoutów w grach PvP
- Optymalizacja procesu łączenia graczy

## 2. Szczegóły żądania

### Metoda HTTP
- **POST** - operacja modyfikująca stan, nie-idempotentna (aktualizacja czasu)

### Struktura URL
```
POST /api/v1/users/{userId}/last-seen
```

### Nagłówki żądania

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT dla uwierzytelnienia

**Opcjonalne:**
- `Content-Type: application/json` - format żądania
- `Accept: application/json` - preferowany format odpowiedzi

### Parametry URL

**Path Variables:**
- `userId` (Long) - ID użytkownika z tabeli `users.id`

### Query Parameters
- Brak parametrów zapytania

### Request Body
- Brak ciała żądania - endpoint nie wymaga dodatkowych danych

### Przykład żądania
```http
POST /api/v1/users/42/last-seen HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
Accept: application/json
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
- Brak - metoda POST nie wymaga DTO żądania dla tego endpointu

#### Response DTO
**`com.tbs.dto.user.LastSeenResponse`** (istniejący)
```java
public record LastSeenResponse(
    String message,
    Instant lastSeenAt
) implements MessageResponse {}
```

**Uwagi implementacyjne:**
- `message` - Komunikat potwierdzający aktualizację (np. "Last seen updated successfully")
- `lastSeenAt` - Zaktualizowany znacznik czasu z `users.last_seen_at`

### Enums
- Brak bezpośredniego użycia enumów w tym endpoincie

### Modele domenowe (do stworzenia)
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.UnauthorizedException`** - wyjątek dla 401 Unauthorized
- **`com.tbs.exception.ForbiddenException`** - wyjątek dla 403 Forbidden
- **`com.tbs.exception.UserNotFoundException`** - wyjątek dla 404 Not Found

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.UserService`** - serwis zarządzający użytkownikami
- **`com.tbs.service.AuthenticationService`** - serwis obsługujący uwierzytelnienie

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Pomyślna aktualizacja znacznika czasu ostatniej aktywności

**Przykład odpowiedzi:**
```json
{
  "message": "Last seen updated successfully",
  "lastSeenAt": "2024-01-20T15:30:00Z"
}
```

### Kody statusu błędów

**400 Bad Request** - Nieprawidłowy format userId
```json
{
  "error": {
    "code": "INVALID_USER_ID",
    "message": "Invalid user ID format",
    "details": null
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
    "message": "You can only update your own last seen timestamp",
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

1. **Odebranie żądania HTTP POST /api/v1/users/{userId}/last-seen**
   - Parsowanie `userId` z path variable
   - Walidacja formatu `userId` (Long)
   - Weryfikacja tokenu JWT z nagłówka Authorization

2. **Walidacja `userId`**
   - Sprawdzenie czy `userId` jest poprawną liczbą
   - Sprawdzenie zakresu (np. > 0)
   - Jeśli nieprawidłowy format → 400 Bad Request

3. **Uwierzytelnienie**
   - Weryfikacja tokenu JWT
   - Pobranie identyfikatora zalogowanego użytkownika
   - Jeśli brak tokenu lub nieprawidłowy → 401 Unauthorized

4. **Autoryzacja**
   - Sprawdzenie czy `userId` odpowiada zalogowanemu użytkownikowi
   - Jeśli nie → 403 Forbidden

5. **Pobranie użytkownika z bazy danych**
   - Zapytanie: `SELECT * FROM users WHERE id = ?`
   - Jeśli użytkownik nie istnieje → 404 Not Found

6. **Aktualizacja `last_seen_at`**
   - Ustawienie `last_seen_at = NOW()`
   - Zapytanie: `UPDATE users SET last_seen_at = ? WHERE id = ?`
   - Automatyczna aktualizacja `updated_at` przez trigger

7. **Mapowanie i zwrócenie odpowiedzi**
   - Konwersja `User` entity → `LastSeenResponse` DTO
   - Zwrócenie odpowiedzi HTTP 200 OK

### Integracja z bazą danych

**Tabela: `users`**
- Operacja UPDATE na podstawie `id`
- Kolumna aktualizowana:
  - `last_seen_at` → ustawienie na `NOW()`

**Triggery:**
- `update_users_updated_at` - automatyczna aktualizacja `updated_at`

**Indeksy:**
- `idx_users_last_seen_at` - szybki dostęp do ostatnio aktywnych użytkowników

### Integracja z Spring Security

**Autoryzacja:**
- Endpoint wymaga uwierzytelnienia (Bearer token)
- Weryfikacja, że `userId` odpowiada zalogowanemu użytkownikowi

**Konfiguracja Security:**
- `/api/v1/users/{userId}/last-seen` wymaga roli ROLE_USER
- Wyjątki dla nieuwierzytelnionych żądań → 401

## 6. Względy bezpieczeństwa

### Autoryzacja

**Weryfikacja własności:**
- Użytkownik może aktualizować tylko swój własny `last_seen_at`
- Porównanie `userId` z `currentUserId` z tokenu JWT
- Brak możliwości aktualizacji cudzego profilu

**Wrażliwe dane:**
- Endpoint nie zwraca wrażliwych danych
- Tylko komunikat i znacznik czasu

### Walidacja danych wejściowych

**Path Variable `userId`:**
- Walidacja formatu (Long)
- Sprawdzenie zakresu (np. > 0)
- Sanityzacja: parsowanie i walidacja przed użyciem w zapytaniu SQL

**Token JWT:**
- Weryfikacja sygnatury i ekspiracji tokenu
- Sprawdzenie roli użytkownika

### Ochrona przed atakami

**SQL Injection:**
- Użycie parametrówzowanych zapytań (JPA/Hibernate automatycznie)
- Walidacja `userId` przed użyciem w zapytaniu

**Path Traversal:**
- Walidacja formatu `userId` (tylko liczby)
- Sprawdzenie zakresu

**Authorization Bypass:**
- Sprawdzenie własności profilu przed aktualizacją
- Niezawodna weryfikacja tokenu JWT

**Rate Limiting:**
- Endpoint może być często wywoływany (heartbeat dla matchmakingu)
- Limit: 30 żądań/minutę na użytkownika
- Implementacja przez Redis

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Nieprawidłowy format userId (400 Bad Request)
**Scenariusz:** `userId` w URL nie jest liczbą
```java
@PostMapping("/{userId}/last-seen")
public ResponseEntity<LastSeenResponse> updateLastSeen(@PathVariable String userId) {
    try {
        Long userIdLong = Long.parseLong(userId);
    } catch (NumberFormatException e) {
        throw new BadRequestException("Invalid user ID format");
    }
}
```

#### 2. Brak uwierzytelnienia (401 Unauthorized)
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

#### 3. Próba aktualizacji cudzego profilu (403 Forbidden)
**Scenariusz:** `userId` nie odpowiada zalogowanemu użytkownikowi
```java
Long currentUserId = authenticationService.getCurrentUserId();
if (!userId.equals(currentUserId)) {
    throw new ForbiddenException("You can only update your own last seen timestamp");
}
```

**Obsługa:**
- Weryfikacja własności profilu
- Zwrócenie 403 Forbidden z odpowiednim komunikatem

#### 4. Użytkownik nie znaleziony (404 Not Found)
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

#### 5. Błąd bazy danych (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z bazą danych, timeout, błąd SQL
```java
@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ApiErrorResponse> handleDataAccessException(DataAccessException e) {
    log.error("Database error while updating last seen", e);
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
    
    @ExceptionHandler({DataAccessException.class, Exception.class})
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
        // 500 handling
    }
}
```

### Logowanie błędów

**Poziomy logowania:**
- **INFO:** Pomyślna aktualizacja znacznika czasu
- **WARN:** Próba dostępu do nieistniejącego użytkownika
- **WARN:** Próba aktualizacji cudzego profilu
- **ERROR:** Błędy bazy danych

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, userId, stack trace (dla błędów)

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Indeksy:**
- Tabela `users` ma indeks na `id` (PK, automatyczny)
- Indeks `idx_users_last_seen_at` dla szybkich zapytań sortujących
- Zapytania używają indeksów

**Zapytania:**
- Prosta operacja UPDATE na pojedynczym wierszu
- Brak złożonych joinów lub agregacji

### Cache'owanie

**Redis Cache:**
- Klucz: `user:last_seen:{userId}`
- TTL: 1-5 minut
- Strategia: Write-through
- Inwalidacja: przy każdej aktualizacji

**Korzyści:**
- Szybsze odpowiedzi dla powtarzających się żądań
- Redukcja obciążenia bazy danych dla aktywnych użytkowników

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 30 żądań/minutę na użytkownika (dla heartbeat matchmakingu)
- Klucz: `rate_limit:users:last_seen:{userId}`

**Uzasadnienie:**
- Endpoint jest często wywoływany przez frontend (heartbeat)
- Zbyt częste wywołania mogą obciążać bazę danych
- Limit 30/min pozwala na aktualizację co 2 sekundy

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="POST",endpoint="/api/v1/users/{userId}/last-seen",status="200"}` - liczba pomyślnych żądań
- `http_requests_total{method="POST",endpoint="/api/v1/users/{userId}/last-seen",status="403"}` - liczba błędów autoryzacji
- `http_request_duration_seconds{method="POST",endpoint="/api/v1/users/{userId}/last-seen"}` - czas odpowiedzi

**Alerty:**
- Długi czas odpowiedzi (>100ms)
- Wysoki wskaźnik błędów 403 (>5% żądań)
- Wysoki wskaźnik błędów 500 (>1% żądań)

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `LastSeenResponse` DTO istnieje ✅
- Sprawdzenie struktury pakietów
- Weryfikacja konfiguracji Spring Security

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.service.UserService` - serwis zarządzający użytkownikami
- `com.tbs.service.AuthenticationService` - serwis obsługujący uwierzytelnienie
- `com.tbs.exception.UnauthorizedException` - wyjątek dla 401
- `com.tbs.exception.ForbiddenException` - wyjątek dla 403
- `com.tbs.exception.UserNotFoundException` - wyjątek dla 404

### Krok 2: Implementacja serwisu

**2.1 Utworzenie UserService:**
```java
@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    
    public LastSeenResponse updateLastSeen(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        user.setLastSeenAt(Instant.now());
        userRepository.save(user);
        
        return new LastSeenResponse("Last seen updated successfully", user.getLastSeenAt());
    }
}
```

**2.2 Testy serwisu:**
- Test jednostkowy z Mockito dla pomyślnego przypadku (200)
- Test dla przypadku gdy użytkownik nie istnieje (404)
- Test dla przypadku gdy użytkownik jest null
- Test weryfikujący aktualizację timestamoutu

### Krok 3: Implementacja kontrolera

**3.1 Utworzenie UserController:**
```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final AuthenticationService authenticationService;
    
    @PostMapping("/{userId}/last-seen")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LastSeenResponse> updateLastSeen(@PathVariable Long userId) {
        Long currentUserId = authenticationService.getCurrentUserId();
        
        if (!userId.equals(currentUserId)) {
            throw new ForbiddenException("You can only update your own last seen timestamp");
        }
        
        LastSeenResponse response = userService.updateLastSeen(userId);
        return ResponseEntity.ok(response);
    }
}
```

**3.2 Konfiguracja Spring Security:**
- Konfiguracja `/api/v1/users/{userId}/last-seen` wymaga uwierzytelnienia
- Wyjątki dla nieuwierzytelnionych żądań → 401
- Weryfikacja roli ROLE_USER

**3.3 Testy kontrolera:**
- Test integracyjny z `@WebMvcTest` dla pomyślnego przypadku (200)
- Test dla przypadku gdy użytkownik nie istnieje (404)
- Test dla przypadku gdy brak uwierzytelnienia (401)
- Test dla przypadku gdy próba aktualizacji cudzego profilu (403)

### Krok 4: Implementacja obsługi błędów

**4.1 Utworzenie global exception handler:**
- Obsługa `UnauthorizedException` (401)
- Obsługa `ForbiddenException` (403)
- Obsługa `UserNotFoundException` (404)
- Obsługa `DataAccessException` (500)

**4.2 Testy exception handler:**
- Test dla każdego typu wyjątku
- Weryfikacja formatu odpowiedzi błędu
- Weryfikacja kodów statusu HTTP

### Krok 5: Konfiguracja Swagger/OpenAPI

**5.1 Dodanie adnotacji Swagger:**
```java
@Operation(
    summary = "Update last seen timestamp",
    description = "Updates the last seen timestamp for the authenticated user"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Last seen updated successfully"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "User not found")
})
@PostMapping("/{userId}/last-seen")
public ResponseEntity<LastSeenResponse> updateLastSeen(...) {
    // ...
}
```

### Krok 6: Implementacja cache'owania (opcjonalne)

**6.1 Konfiguracja Redis cache:**
- Dodanie `@EnableCaching` w klasie konfiguracyjnej
- Konfiguracja `RedisCacheManager`

**6.2 Dodanie cache do serwisu:**
```java
@CachePut(value = "userLastSeen", key = "#userId")
public LastSeenResponse updateLastSeen(Long userId) {
    // ...
}
```

### Krok 7: Testy integracyjne i E2E

**7.1 Testy integracyjne:**
- Test pełnego przepływu z bazą danych
- Test z rzeczywistym tokenem JWT
- Test weryfikujący trigger `update_users_updated_at`

**7.2 Testy E2E (Cypress):**
- Test aktualizacji własnego last seen
- Test próby aktualizacji cudzego profilu (403)
- Test bez uwierzytelnienia (401)

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

Plan implementacji endpointu **POST /api/v1/users/{userId}/last-seen** obejmuje kompleksowe podejście do wdrożenia z obsługą uwierzytelnienia i autoryzacji. Kluczowe aspekty:

- **Bezpieczeństwo:** Uwierzytelnienie JWT, autoryzacja dostępu, ochrona przed nieautoryzowanymi aktualizacjami
- **Wydajność:** Proste operacje UPDATE, opcjonalne cache'owanie
- **Obsługa błędów:** Centralna obsługa z odpowiednimi kodami statusu
- **Rate Limiting:** Ograniczenie częstotliwości wywołań (30/min)
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Dokumentacja:** Swagger/OpenAPI, dokumentacja kodu

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
