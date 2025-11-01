# API Endpoint Implementation Plan: GET /api/users/{userId}

## 1. Przegląd punktu końcowego

**GET /api/users/{userId}** to endpoint służący do pobrania profilu użytkownika po ID. Endpoint jest częściowo publiczny - zarejestrowani użytkownicy mogą przeglądać publiczne profile innych zarejestrowanych użytkowników (dla rankingu), natomiast własny profil wymaga uwierzytelnienia dla pełnych danych.

Endpoint zwraca publiczne informacje o profilu użytkownika: statystyki gry (punkty, rozegrane gry, wygrane) oraz metadane (data utworzenia). Email i inne wrażliwe dane nie są zwracane.

Kluczowe zastosowania:
- Przeglądanie profili innych graczy (dla rankingu)
- Sprawdzenie własnego profilu
- Wyświetlanie statystyk gracza

## 2. Szczegóły żądania

### Metoda HTTP
- **GET** - operacja tylko do odczytu, idempotentna

### Struktura URL
```
GET /api/users/{userId}
```

### Nagłówki żądania

**Wymagane:**
- Brak (endpoint publiczny dla zarejestrowanych użytkowników)
- Uwierzytelnienie wymagane dla własnego profilu

**Opcjonalne:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT (jeśli dostęp do własnego profilu)
- `Accept: application/json` - preferowany format odpowiedzi

### Parametry URL

**Path Variables:**
- `userId` (Long) - ID użytkownika z tabeli `users.id`

### Query Parameters
- Brak parametrów zapytania

### Request Body
- Brak ciała żądania (metoda GET)

### Przykład żądania
```http
GET /api/users/42 HTTP/1.1
Host: api.example.com
Accept: application/json
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
- Brak - metoda GET nie wymaga DTO żądania

#### Response DTO
**`com.tbs.dto.user.UserProfileResponse`** (istniejący)
```java
public record UserProfileResponse(
    long userId,
    String username,
    boolean isGuest,
    long totalPoints,
    int gamesPlayed,
    int gamesWon,
    Instant createdAt
) {}
```

**Uwagi implementacyjne:**
- `userId` - ID użytkownika z tabeli `users.id`
- `username` - Nazwa użytkownika (null dla gości)
- `isGuest` - Flaga wskazująca czy użytkownik jest gościem
- `totalPoints` - Z `users.total_points`
- `gamesPlayed` - Z `users.games_played`
- `gamesWon` - Z `users.games_won`
- `createdAt` - Z `users.created_at`

### Enums
- Brak bezpośredniego użycia enumów w tym endpoincie

### Modele domenowe (do stworzenia)
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.UserNotFoundException`** - wyjątek dla 404 Not Found
- **`com.tbs.exception.ForbiddenException`** - wyjątek dla 403 Forbidden (jeśli ograniczenia dostępu)

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.UserService`** - serwis zarządzający użytkownikami

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Pomyślne pobranie profilu użytkownika

**Przykład odpowiedzi dla zarejestrowanego użytkownika:**
```json
{
  "userId": 42,
  "username": "player1",
  "isGuest": false,
  "totalPoints": 3500,
  "gamesPlayed": 18,
  "gamesWon": 12,
  "createdAt": "2024-01-15T10:30:00Z"
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
  "createdAt": "2024-01-19T08:20:00Z"
}
```

### Kody statusu błędów

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

**403 Forbidden** - Brak dostępu do profilu (opcjonalne, jeśli implementowane ograniczenia)
```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "Access denied",
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

1. **Odebranie żądania HTTP GET /api/users/{userId}**
   - Parsowanie `userId` z path variable
   - Walidacja formatu `userId` (Long)

2. **Walidacja `userId`**
   - Sprawdzenie czy `userId` jest poprawną liczbą
   - Jeśli nieprawidłowy format → 400 Bad Request

3. **Weryfikacja dostępu (opcjonalne)**
   - Jeśli użytkownik jest zalogowany i `userId` odpowiada zalogowanemu użytkownikowi: pełny dostęp
   - Jeśli użytkownik nie jest zalogowany lub `userId` nie odpowiada: publiczny dostęp (tylko zarejestrowani użytkownicy)
   - Goście mogą być widoczni tylko dla właściciela profilu (opcjonalne)

4. **Pobranie użytkownika z bazy danych**
   - Zapytanie: `SELECT * FROM users WHERE id = ?`
   - Jeśli użytkownik nie istnieje → 404 Not Found

5. **Weryfikacja dostępu do profilu**
   - Zarejestrowani użytkownicy: publiczne profile innych zarejestrowanych są dostępne
   - Goście: profil gościa dostępny tylko dla właściciela (opcjonalne)
   - Jeśli brak dostępu → 403 Forbidden

6. **Mapowanie encji do DTO**
   - Konwersja `User` entity → `UserProfileResponse` DTO
   - Obsługa null dla `username` (goście)

7. **Zwrócenie odpowiedzi HTTP 200 OK**
   - Serializacja `UserProfileResponse` do JSON

### Integracja z bazą danych

**Tabela: `users`**
- Zapytanie SELECT na podstawie `id`
- Kolumny pobierane:
  - `id` → `userId`
  - `username` → `username`
  - `is_guest` → `isGuest`
  - `total_points` → `totalPoints`
  - `games_played` → `gamesPlayed`
  - `games_won` → `gamesWon`
  - `created_at` → `createdAt`

**Indeksy:**
- `idx_users_id` (PK, automatyczny) - szybki dostęp po ID

**Row Level Security (RLS):**
- Polityki RLS mogą ograniczać dostęp do profilu gości
- Dla zarejestrowanych: publiczne profile są dostępne

### Integracja z Spring Security

**Autoryzacja:**
- Endpoint publiczny dla zarejestrowanych użytkowników (przeglądanie profili)
- Opcjonalnie: wymagane uwierzytelnienie dla własnego profilu
- Goście mogą być widoczni tylko dla właściciela profilu

**Konfiguracja Security:**
- `/api/users/{userId}` może być publiczny lub wymagać uwierzytelnienia
- Weryfikacja dostępu w warstwie aplikacji (serwis)

## 6. Względy bezpieczeństwa

### Autoryzacja

**Publiczne profile:**
- Zarejestrowani użytkownicy mogą przeglądać publiczne profile innych zarejestrowanych użytkowników
- Tylko statystyki gry i publiczne dane są zwracane (bez email, bez wrażliwych danych)

**Profil gościa:**
- Profil gościa może być widoczny tylko dla właściciela (opcjonalne)
- Lub publicznie dostępny (w zależności od wymagań)

**Własny profil:**
- Jeśli `userId` odpowiada zalogowanemu użytkownikowi: pełny dostęp
- Użycie `/api/auth/me` dla własnego profilu (lepsze rozwiązanie)

### Walidacja danych wejściowych

**Path Variable `userId`:**
- Walidacja formatu (Long)
- Sprawdzenie zakresu (np. > 0)
- Sanityzacja: parsowanie i walidacja przed użyciem w zapytaniu SQL

### Ochrona przed atakami

**SQL Injection:**
- Użycie parametrówzowanych zapytań (JPA/Hibernate automatycznie)
- Walidacja `userId` przed użyciem w zapytaniu

**Path Traversal:**
- Walidacja formatu `userId` (tylko liczby)
- Sprawdzenie zakresu

**Information Disclosure:**
- Nie zwracanie wrażliwych danych (email, hasła, tokeny)
- Filtrowanie danych przed zwróceniem (tylko publiczne pola)

**Rate Limiting:**
- Ograniczenie szybkości dla publicznych endpointów: 100 żądań/minutę na IP (zgodnie z api-plan.md)
- Implementacja przez Redis

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Nieprawidłowy format userId (400 Bad Request)
**Scenariusz:** `userId` w URL nie jest liczbą
```java
@GetMapping("/{userId}")
public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable String userId) {
    try {
        Long userIdLong = Long.parseLong(userId);
    } catch (NumberFormatException e) {
        throw new BadRequestException("Invalid user ID format");
    }
}
```

#### 2. Użytkownik nie znaleziony (404 Not Found)
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

#### 3. Brak dostępu (403 Forbidden)
**Scenariusz:** Próba dostępu do profilu gościa przez innego użytkownika (opcjonalne)
```java
if (user.isGuest() && !isOwner(userId, currentUserId)) {
    throw new ForbiddenException("Access denied to guest profile");
}
```

#### 4. Błąd bazy danych (500 Internal Server Error)
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
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException e) {
        // 404 handling
    }
    
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException e) {
        // 403 handling
    }
    
    @ExceptionHandler({DataAccessException.class, Exception.class})
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
        // 500 handling
    }
}
```

### Logowanie błędów

**Poziomy logowania:**
- **INFO:** Pomyślne pobranie profilu użytkownika (dla audytu)
- **WARN:** Próba dostępu do nieistniejącego użytkownika
- **ERROR:** Błędy bazy danych

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, userId, stack trace (dla błędów)

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Indeksy:**
- Tabela `users` powinna mieć indeks na `id` (PK, automatyczny)
- Zapytania powinny używać indeksów

**Zapytania:**
- Pobieranie tylko wymaganych kolumn (nie SELECT *)
- Użycie `@EntityGraph` jeśli potrzebne relacje (w tym endpoincie nie)

### Cache'owanie

**Redis Cache (opcjonalne):**
- Klucz: `user:profile:{userId}`
- TTL: 5-15 minut
- Strategia: Cache-aside
- Inwalidacja: przy aktualizacji profilu (PUT /api/users/{userId})

**Korzyści:**
- Redukcja obciążenia bazy danych dla często przeglądanych profili
- Szybsze odpowiedzi dla powtarzających się żądań

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 100 żądań/minutę na IP (zgodnie z api-plan.md)
- Klucz: `rate_limit:users:profile:{ipAddress}`

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="GET",endpoint="/api/users/{userId}",status="200"}` - liczba pomyślnych żądań
- `http_requests_total{method="GET",endpoint="/api/users/{userId}",status="404"}` - liczba błędów 404
- `http_request_duration_seconds{method="GET",endpoint="/api/users/{userId}"}` - czas odpowiedzi

**Alerty:**
- Wysoki wskaźnik błędów 404 (>10% żądań)
- Długi czas odpowiedzi (>500ms)
- Wysoki wskaźnik błędów 500 (>1% żądań)

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `UserProfileResponse` DTO istnieje
- Sprawdzenie struktury pakietów

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.service.UserService` - serwis zarządzający użytkownikami
- `com.tbs.exception.UserNotFoundException` - wyjątek dla 404
- `com.tbs.exception.ForbiddenException` - wyjątek dla 403 (opcjonalne)

### Krok 2: Implementacja serwisu

**2.1 Utworzenie UserService:**
```java
@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    
    public UserProfileResponse getUserProfile(Long userId, Long currentUserId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        if (user.isGuest() && !userId.equals(currentUserId)) {
            throw new ForbiddenException("Access denied to guest profile");
        }
        
        return mapToUserProfileResponse(user);
    }
}
```

**2.2 Testy serwisu:**
- Test jednostkowy z Mockito dla pomyślnego przypadku (200)
- Test dla przypadku gdy użytkownik nie istnieje (404)
- Test dla przypadku gdy gość dostępny tylko dla właściciela (403)

### Krok 3: Implementacja kontrolera

**3.1 Utworzenie UserController:**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final AuthenticationService authenticationService;
    
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        Long currentUserId = authenticationService.getCurrentUserIdOrNull();
        UserProfileResponse profile = userService.getUserProfile(userId, currentUserId);
        return ResponseEntity.ok(profile);
    }
}
```

**3.2 Konfiguracja Spring Security:**
- Konfiguracja `/api/users/{userId}` jako publiczny lub z uwierzytelnieniem
- Wyjątki dla nieuwierzytelnionych żądań → 401 (jeśli wymagane uwierzytelnienie)

**3.3 Testy kontrolera:**
- Test integracyjny z `@WebMvcTest` dla pomyślnego przypadku (200)
- Test dla przypadku gdy użytkownik nie istnieje (404)
- Test dla przypadku dostępu do profilu gościa (403)

### Krok 4: Implementacja obsługi błędów

**4.1 Utworzenie global exception handler:**
- Obsługa `UserNotFoundException` (404)
- Obsługa `ForbiddenException` (403)
- Obsługa `DataAccessException` (500)

**4.2 Testy exception handler:**
- Test dla każdego typu wyjątku
- Weryfikacja formatu odpowiedzi błędu

### Krok 5: Konfiguracja Swagger/OpenAPI

**5.1 Dodanie adnotacji Swagger:**
```java
@Operation(
    summary = "Get user profile",
    description = "Retrieves the profile of a user by ID"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
    @ApiResponse(responseCode = "404", description = "User not found"),
    @ApiResponse(responseCode = "403", description = "Access denied")
})
@GetMapping("/{userId}")
public ResponseEntity<UserProfileResponse> getUserProfile(...) {
    // ...
}
```

### Krok 6: Implementacja cache'owania (opcjonalne)

**6.1 Konfiguracja Redis cache:**
- Dodanie `@EnableCaching` w klasie konfiguracyjnej
- Konfiguracja `RedisCacheManager`

**6.2 Dodanie cache do serwisu:**
```java
@Cacheable(value = "userProfile", key = "#userId")
public UserProfileResponse getUserProfile(Long userId, Long currentUserId) {
    // ...
}
```

### Krok 7: Testy integracyjne i E2E

**7.1 Testy integracyjne:**
- Test pełnego przepływu z bazą danych
- Test z rzeczywistym tokenem JWT
- Test weryfikujący RLS w bazie danych

**7.2 Testy E2E (Cypress):**
- Test pobrania profilu innego użytkownika
- Test pobrania własnego profilu
- Test obsługi błędu 404 dla nieistniejącego użytkownika

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

Plan implementacji endpointu **GET /api/users/{userId}** obejmuje kompleksowe podejście do wdrożenia z obsługą publicznych profili i autoryzacji. Kluczowe aspekty:

- **Bezpieczeństwo:** Autoryzacja dostępu, ochrona wrażliwych danych, rate limiting
- **Wydajność:** Optymalizacja zapytań, opcjonalne cache'owanie
- **Obsługa błędów:** Centralna obsługa z odpowiednimi kodami statusu
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Dokumentacja:** Swagger/OpenAPI, dokumentacja kodu

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
