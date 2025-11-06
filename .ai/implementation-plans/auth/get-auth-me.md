# API Endpoint Implementation Plan: GET /api/auth/me

## 1. Przegląd punktu końcowego

**GET /api/auth/me** to endpoint uwierzytelniania służący do pobrania profilu bieżącego zalogowanego użytkownika. Endpoint wymaga ważnego tokenu JWT w nagłówku Authorization i zwraca szczegółowe informacje o profilu użytkownika, w tym statystyki gry (punkty, rozegrane gry, wygrane) oraz metadane (data utworzenia, ostatnia aktywność).

Endpoint obsługuje zarówno zarejestrowanych użytkowników (powiązanych z Supabase Auth przez `auth_user_id`), jak i gości (identyfikowanych przez IP). Kluczowe zastosowania:
- Weryfikacja statusu uwierzytelnienia użytkownika
- Pobranie aktualnych statystyk użytkownika do wyświetlenia w UI
- Sprawdzenie typu użytkownika (gość vs zarejestrowany)
- Aktualizacja informacji o ostatniej aktywności użytkownika

## 2. Szczegóły żądania

### Metoda HTTP
- **GET** - operacja tylko do odczytu, idempotentna

### Struktura URL
```
GET /api/auth/me
```

### Nagłówki żądania

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT wydany po poprawnym logowaniu/rejestracji

**Opcjonalne:**
- `Accept: application/json` - preferowany format odpowiedzi (domyślnie JSON)
- `Content-Type: application/json` - format treści (nie dotyczy GET)

### Parametry URL
- Brak parametrów URL

### Query Parameters
- Brak parametrów zapytania

### Request Body
- Brak ciała żądania (metoda GET)

### Przykład żądania
```http
GET /api/auth/me HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Accept: application/json
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
- Brak - metoda GET nie wymaga DTO żądania

#### Response DTO
**`com.tbs.dto.auth.UserProfileResponse`** (istniejący)
```java
public record UserProfileResponse(
    long userId,
    String username,
    boolean isGuest,
    long totalPoints,
    int gamesPlayed,
    int gamesWon,
    Instant createdAt,
    Instant lastSeenAt
) {}
```

**Uwagi implementacyjne:**
- `userId` - ID użytkownika z tabeli `users.id`
- `username` - nazwa użytkownika (null dla gości)
- `isGuest` - flaga wskazująca czy użytkownik jest gościem
- `totalPoints` - suma punktów z kolumny `users.total_points`
- `gamesPlayed` - liczba gier z `users.games_played`
- `gamesWon` - liczba wygranych z `users.games_won`
- `createdAt` - data utworzenia konta z `users.created_at`
- `lastSeenAt` - ostatnia aktywność z `users.last_seen_at` (może być null)

### Enums
- Brak bezpośredniego użycia enumów w tym endpoincie

### Modele domenowe (do stworzenia)
- **`com.tbs.model.User`** - encja reprezentująca użytkownika z tabeli `users`
  - Pola zgodne z schematem bazy danych
  - Mapowanie JPA/Hibernate do tabeli `users`
  - Relacje do `games` i `moves` (opcjonalne, lazy loading)

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.UnauthorizedException`** - wyjątek dla 401 Unauthorized
  - Używany gdy brak tokenu JWT lub token jest nieprawidłowy/wygasły
- **`com.tbs.exception.UserNotFoundException`** - wyjątek dla 404 Not Found
  - Używany gdy użytkownik z tokenu nie istnieje w bazie danych (nieprawidłowy stan)

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Pomyślne pobranie profilu użytkownika

**Przykład odpowiedzi:**
```json
{
  "userId": 42,
  "username": "player1",
  "isGuest": false,
  "totalPoints": 3500,
  "gamesPlayed": 18,
  "gamesWon": 12,
  "createdAt": "2024-01-15T10:30:00Z",
  "lastSeenAt": "2024-01-20T14:45:00Z"
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
  "createdAt": "2024-01-19T08:20:00Z",
  "lastSeenAt": "2024-01-20T15:10:00Z"
}
```

### Kody statusu błędów

**401 Unauthorized** - Brak tokenu JWT lub token nieprawidłowy/wygasły
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

**404 Not Found** - Użytkownik z tokenu nie istnieje w bazie danych
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

**500 Internal Server Error** - Nieoczekiwany błąd serwera (błąd bazy danych, wyjątek systemowy)
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

1. **Odebranie żądania HTTP GET /api/auth/me**
   - Spring Security przechwytuje żądanie
   - Weryfikacja tokenu JWT z nagłówka `Authorization: Bearer <token>`

2. **Walidacja tokenu JWT (Spring Security)**
   - Wyodrębnienie tokenu z nagłówka Authorization
   - Parsowanie i walidacja tokenu (sygnatura, wygaśnięcie)
   - Wyodrębnienie informacji o użytkowniku z claims tokenu (np. `sub` - user ID)
   - Ustawienie `Authentication` w `SecurityContext`

3. **Wyodrębnienie identyfikatora użytkownika**
   - Z `SecurityContext.getAuthentication()` lub z tokenu JWT claims
   - Możliwe źródła:
     - Dla zarejestrowanych: `auth_user_id` (UUID z Supabase Auth) lub `user_id` (BIGINT z tabeli users)
     - Dla gości: `user_id` (BIGINT) z sesji lub tokenu

4. **Pobranie użytkownika z bazy danych**
   - Zapytanie SQL do tabeli `users`
   - Dla zarejestrowanych: `WHERE auth_user_id = ?` lub `WHERE id = ?`
   - Dla gości: `WHERE id = ? AND is_guest = true`
   - Optional: Sprawdzenie czy użytkownik istnieje

5. **Mapowanie encji do DTO**
   - Konwersja `User` entity → `UserProfileResponse` DTO
   - Obsługa null dla `username` (goście) i `lastSeenAt`

6. **Zwrócenie odpowiedzi HTTP 200 OK**
   - Serializacja `UserProfileResponse` do JSON
   - Ustawienie nagłówków odpowiedzi

### Integracja z bazą danych

**Tabela: `users`**
- Zapytanie SELECT na podstawie identyfikatora użytkownika
- Kolumny pobierane:
  - `id` → `userId`
  - `username` → `username`
  - `is_guest` → `isGuest`
  - `total_points` → `totalPoints`
  - `games_played` → `gamesPlayed`
  - `games_won` → `gamesWon`
  - `created_at` → `createdAt`
  - `last_seen_at` → `lastSeenAt`

**Row Level Security (RLS):**
- Polityki RLS w PostgreSQL powinny zapewnić, że użytkownik może pobrać tylko swoje własne dane
- Dla zarejestrowanych: `auth_user_id = auth.uid()` (Supabase Auth)
- Dla gości: `id = current_setting('app.guest_user_id')::BIGINT`

### Integracja z Spring Security

**Mechanizm uwierzytelniania:**
- JWT Authentication Filter w Spring Security Chain
- Wyodrębnienie tokenu z nagłówka `Authorization: Bearer <token>`
- Walidacja tokenu (sygnatura, wygaśnięcie) przez `JwtTokenProvider` lub podobny komponent
- Ustawienie `Authentication` w `SecurityContext`

**Konfiguracja Security:**
- Endpoint wymaga roli `ROLE_USER` lub `ROLE_GUEST`
- Wyjątek dla nieuwierzytelnionych żądań → 401 Unauthorized

### Potencjalna integracja z Redis (opcjonalna, cache)

**Cache profilu użytkownika:**
- Klucz: `user:profile:{userId}`
- TTL: 5-15 minut
- Strategia cache-aside: sprawdź cache → jeśli brak, pobierz z DB → zapisz w cache
- Inwalidacja: przy aktualizacji profilu użytkownika (PUT /api/v1/users/{userId})

## 6. Względy bezpieczeństwa

### Uwierzytelnianie

**Mechanizm JWT:**
- Token JWT wymagany w nagłówku `Authorization: Bearer <token>`
- Token wydawany po poprawnym logowaniu/rejestracji
- Walidacja tokenu przez Spring Security:
  - Weryfikacja sygnatury (algorytm HS256/RS256)
  - Sprawdzenie wygaśnięcia (`exp` claim)
  - Sprawdzenie ważności (`nbf`, `iat` claims)

**Ochrona przed atakami:**
- Zapobieganie token replay attacks: sprawdzenie wygaśnięcia
- Walidacja formatu tokenu przed parsowaniem
- Sanityzacja danych wejściowych (chociaż w GET nie ma body)

### Autoryzacja

**Zasada najniższych uprawnień:**
- Użytkownik może pobrać tylko swój własny profil
- Row Level Security (RLS) w bazie danych zapewnia dodatkową warstwę ochrony
- Walidacja w warstwie aplikacji, że `userId` z tokenu odpowiada pobieranemu profilowi

**Weryfikacja uczestnictwa:**
- Endpoint powinien weryfikować, że `userId` z tokenu JWT odpowiada `userId` w żądaniu profilu
- Zapobieganie dostępowi do profilu innych użytkowników

### Bezpieczeństwo danych

**Ochrona wrażliwych danych:**
- Nie zwracanie wrażliwych danych (hasła, tokeny, klucze)
- Filtrowanie danych z bazy przed zwróceniem (tylko wymagane pola)
- Brak logowania wrażliwych danych w logach

**Sanityzacja odpowiedzi:**
- Weryfikacja, że zwracane dane nie zawierają nieprawidłowych wartości
- Obsługa null dla opcjonalnych pól (`username`, `lastSeenAt`)

### Ochrona przed typowymi atakami

**SQL Injection:**
- Użycie parametrówzowanych zapytań (JPA/Hibernate automatycznie)
- Brak dynamicznego SQL na podstawie danych wejściowych

**XSS (Cross-Site Scripting):**
- Niskie ryzyko (GET endpoint, brak danych wejściowych)
- Sanityzacja danych w odpowiedzi JSON (Spring automatycznie)

**CSRF (Cross-Site Request Forgery):**
- Token JWT w nagłówku Authorization zapewnia ochronę przed podstawowymi atakami CSRF
- Dla dodatkowej ochrony: CSRF token w sesji (opcjonalne)

**Rate Limiting:**
- Ograniczenie szybkości dla uwierzytelnionych użytkowników: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Implementacja przez Redis z algorytmem przesuwającego okna

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Brak tokenu JWT (401 Unauthorized)
**Scenariusz:** Żądanie bez nagłówka `Authorization` lub z nieprawidłowym formatem
```java
// Spring Security automatycznie zwróci 401 przed dotarciem do kontrolera
// Wymagane: konfiguracja SecurityFilterChain z wyjątkiem dla nieuwierzytelnionych
```

**Obsługa:**
- Spring Security przechwytuje żądanie przed kontrolerem
- Zwraca 401 Unauthorized z komunikatem "Authentication required"
- Logowanie próby dostępu bez tokenu (opcjonalne, dla monitoringu)

#### 2. Nieprawidłowy token JWT (401 Unauthorized)
**Scenariusz:** Token z nieprawidłową sygnaturą, wygasły lub uszkodzony
```java
// W JwtAuthenticationFilter lub podobnym komponencie
if (!tokenProvider.validateToken(token)) {
    throw new UnauthorizedException("Invalid or expired token");
}
```

**Obsługa:**
- Walidacja tokenu przez `JwtTokenProvider`
- Zwrócenie 401 Unauthorized z komunikatem "Invalid or expired token"
- Logowanie próby dostępu z nieprawidłowym tokenem

#### 3. Użytkownik nie znaleziony (404 Not Found)
**Scenariusz:** Token JWT jest ważny, ale użytkownik z `userId` nie istnieje w bazie danych
```java
Optional<User> user = userRepository.findByAuthUserId(authUserId);
if (user.isEmpty()) {
    throw new UserNotFoundException("User not found");
}
```

**Obsługa:**
- Sprawdzenie czy użytkownik istnieje po pobraniu z bazy
- Zwrócenie 404 Not Found z komunikatem "User not found"
- Logowanie nieprawidłowego stanu (token wskazuje na nieistniejącego użytkownika)

#### 4. Błąd bazy danych (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z bazą danych, timeout, błąd SQL
```java
// Global exception handler (@ControllerAdvice)
@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ApiErrorResponse> handleDataAccessException(DataAccessException e) {
    log.error("Database error while fetching user profile", e);
    return ResponseEntity.status(500)
        .body(new ApiErrorResponse(
            new ErrorDetails("INTERNAL_SERVER_ERROR", "Database error occurred", null)
        ));
}
```

**Obsługa:**
- Przechwycenie `DataAccessException` przez global exception handler
- Zwrócenie 500 Internal Server Error z ogólnym komunikatem
- Logowanie szczegółów błędu (nie w odpowiedzi dla użytkownika)

#### 5. Błąd serializacji JSON (500 Internal Server Error)
**Scenariusz:** Problem z mapowaniem encji do DTO lub serializacją do JSON
```java
@ExceptionHandler(HttpMessageNotWritableException.class)
public ResponseEntity<ApiErrorResponse> handleSerializationException(HttpMessageNotWritableException e) {
    log.error("Error serializing user profile response", e);
    return ResponseEntity.status(500)
        .body(new ApiErrorResponse(
            new ErrorDetails("INTERNAL_SERVER_ERROR", "Error processing response", null)
        ));
}
```

**Obsługa:**
- Przechwycenie wyjątków serializacji przez global exception handler
- Zwrócenie 500 z ogólnym komunikatem
- Logowanie szczegółów błędu

### Global Exception Handler

**Struktura:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
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
- **INFO:** Pomyślne pobranie profilu użytkownika (opcjonalne, dla audytu)
- **WARN:** Próba dostępu z nieprawidłowym tokenem
- **ERROR:** Błędy bazy danych, nieprawidłowy stan systemu

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, userId (jeśli dostępne), stack trace (dla błędów)

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Indeksy:**
- Tabela `users` powinna mieć indeksy na:
  - `idx_users_auth_user_id` (UNIQUE, partial) - dla zarejestrowanych użytkowników
  - `idx_users_id` (PK, automatyczny) - dla szybkiego dostępu po ID
- Upewnić się, że zapytania używają indeksów (EXPLAIN ANALYZE)

**Zapytania:**
- Pobieranie tylko wymaganych kolumn (nie SELECT *)
- Użycie `@EntityGraph` lub `JOIN FETCH` tylko jeśli potrzebne relacje (w tym endpoincie nie)

### Cache'owanie

**Redis Cache (opcjonalne):**
- **Klucz cache:** `user:profile:{userId}`
- **TTL:** 5-15 minut
- **Strategia:** Cache-aside
  - Sprawdź cache przed zapytaniem do DB
  - Jeśli cache miss: pobierz z DB, zapisz w cache
  - Inwalidacja: przy aktualizacji profilu (PUT /api/v1/users/{userId})

**Korzyści:**
- Redukcja obciążenia bazy danych dla często używanych profili
- Szybsze odpowiedzi dla powtarzających się żądań

**Kompromisy:**
- Możliwa nieaktualność danych (TTL-based expiration)
- Dodatkowa złożoność (obsługa cache miss/hit)

### Optymalizacja odpowiedzi

**Serializacja JSON:**
- Spring Boot używa Jackson do serializacji
- Upewnić się, że `UserProfileResponse` używa `@JsonInclude(JsonInclude.Include.NON_NULL)` dla opcjonalnych pól
- Rozmiar odpowiedzi: ~200-300 bytes (mały, nie wymaga kompresji)

### Ograniczenie szybkości (Rate Limiting)

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Klucz: `rate_limit:auth:me:{userId}` lub `rate_limit:auth:me:{ipAddress}`

**Korzyści:**
- Zapobieganie nadużyciom i DoS
- Sprawiedliwy podział zasobów między użytkowników

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="GET",endpoint="/api/auth/me",status="200"}` - liczba pomyślnych żądań
- `http_requests_total{method="GET",endpoint="/api/auth/me",status="401"}` - liczba błędów 401
- `http_request_duration_seconds{method="GET",endpoint="/api/auth/me"}` - czas odpowiedzi

**Alerty:**
- Wysoki wskaźnik błędów 401 (>10% żądań)
- Długi czas odpowiedzi (>500ms)
- Wysoki wskaźnik błędów 500 (>1% żądań)

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `UserProfileResponse` DTO istnieje i jest kompletny
- Sprawdzenie czy enumy są zdefiniowane
- Weryfikacja struktury pakietów (`com.tbs.controller`, `com.tbs.service`, `com.tbs.repository`)

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.model.User` - encja JPA/Hibernate dla tabeli `users`
- `com.tbs.repository.UserRepository` - interfejs Spring Data JPA
- `com.tbs.service.UserService` - serwis biznesowy (jeśli nie istnieje)
- `com.tbs.exception.UnauthorizedException` - wyjątek dla 401
- `com.tbs.exception.UserNotFoundException` - wyjątek dla 404

**1.3 Konfiguracja zależności (build.gradle):**
```groovy
dependencies {
    // Sprawdzenie czy istnieją:
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.3' // lub inna wersja JWT
    // ... inne zależności
}
```

### Krok 2: Implementacja encji i repozytorium

**2.1 Utworzenie encji User (`com.tbs.model.User`):**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "auth_user_id", unique = true)
    private UUID authUserId;
    
    @Column(name = "username", unique = true, nullable = true)
    private String username;
    
    @Column(name = "is_guest", nullable = false)
    private Boolean isGuest;
    
    // ... pozostałe pola zgodne z schematem bazy danych
    
    // getters, setters, konstruktory
}
```

**2.2 Utworzenie repozytorium (`com.tbs.repository.UserRepository`):**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAuthUserId(UUID authUserId);
    Optional<User> findByIdAndIsGuest(Long id, Boolean isGuest);
}
```

**2.3 Testy repozytorium:**
- Test jednostkowy dla `findByAuthUserId`
- Test jednostkowy dla `findByIdAndIsGuest`

### Krok 3: Implementacja serwisu biznesowego

**3.1 Utworzenie UserService (`com.tbs.service.UserService`):**
```java
@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService; // do wyodrębnienia userId z tokenu
    
    public UserProfileResponse getCurrentUserProfile() {
        Long userId = authenticationService.getCurrentUserId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        return mapToUserProfileResponse(user);
    }
    
    private UserProfileResponse mapToUserProfileResponse(User user) {
        // Mapowanie User → UserProfileResponse
    }
}
```

**3.2 Testy serwisu:**
- Test jednostkowy z Mockito dla `getCurrentUserProfile` (sukces)
- Test jednostkowy dla `getCurrentUserProfile` gdy użytkownik nie istnieje (404)
- Test jednostkowy dla `getCurrentUserProfile` gdy userId null (401)

### Krok 4: Implementacja kontrolera

**4.1 Utworzenie AuthController (`com.tbs.controller.AuthController`):**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(profile);
    }
}
```

**4.2 Konfiguracja Spring Security:**
- Upewnienie się, że `/api/auth/me` wymaga uwierzytelnienia
- Konfiguracja `SecurityFilterChain` z JWT authentication filter
- Wyjątki dla nieuwierzytelnionych żądań → 401

**4.3 Testy kontrolera:**
- Test integracyjny z `@WebMvcTest` dla pomyślnego przypadku (200)
- Test integracyjny dla przypadku bez tokenu (401)
- Test integracyjny dla przypadku z nieprawidłowym tokenem (401)
- Test integracyjny dla przypadku gdy użytkownik nie istnieje (404)

### Krok 5: Implementacja obsługi błędów

**5.1 Utworzenie global exception handler (`com.tbs.exception.GlobalExceptionHandler`):**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException e) {
        // Obsługa 401
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException e) {
        // Obsługa 404
    }
    
    @ExceptionHandler({DataAccessException.class, Exception.class})
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
        // Obsługa 500
    }
}
```

**5.2 Testy exception handler:**
- Test jednostkowy dla każdego typu wyjątku
- Weryfikacja formatu odpowiedzi błędu

### Krok 6: Konfiguracja Swagger/OpenAPI

**6.1 Dodanie adnotacji Swagger do kontrolera:**
```java
@Operation(
    summary = "Get current user profile",
    description = "Retrieves the profile of the currently authenticated user"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
    @ApiResponse(responseCode = "404", description = "User not found")
})
@GetMapping("/me")
public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
    // ...
}
```

**6.2 Konfiguracja OpenAPI:**
- Upewnienie się, że endpoint jest widoczny w Swagger UI
- Konfiguracja schematów żądań/odpowiedzi

### Krok 7: Implementacja cache'owania (opcjonalne)

**7.1 Konfiguracja Redis cache:**
- Dodanie `@EnableCaching` w klasie konfiguracyjnej
- Konfiguracja `RedisCacheManager`

**7.2 Dodanie cache do serwisu:**
```java
@Cacheable(value = "userProfile", key = "#userId")
public UserProfileResponse getCurrentUserProfile() {
    // ...
}
```

**7.3 Testy cache:**
- Test integracyjny weryfikujący działanie cache
- Test inwalidacji cache

### Krok 8: Implementacja rate limiting (opcjonalne)

**8.1 Konfiguracja rate limiting:**
- Implementacja filtru Spring Security lub interceptor
- Integracja z Redis dla przechowywania liczników

**8.2 Dodanie rate limiting do endpointu:**
- Konfiguracja limitów dla `/api/auth/me`
- Obsługa przekroczenia limitu (429 Too Many Requests)

**8.3 Testy rate limiting:**
- Test weryfikujący działanie limitu
- Test weryfikujący reset limitu po upływie czasu

### Krok 9: Testy integracyjne i E2E

**9.1 Testy integracyjne:**
- Test pełnego przepływu z bazą danych (testcontainers lub embedded DB)
- Test z rzeczywistym tokenem JWT
- Test weryfikujący RLS w bazie danych

**9.2 Testy E2E (Cypress):**
- Test pobrania profilu po logowaniu
- Test obsługi błędu 401 dla nieuwierzytelnionego żądania
- Test wyświetlania profilu w UI

### Krok 10: Dokumentacja i code review

**10.1 Dokumentacja:**
- Aktualizacja README z informacjami o endpoincie
- Komentarze w kodzie (tylko tam gdzie wymagane przez reguły)
- Dokumentacja Swagger/OpenAPI

**10.2 Code review:**
- Sprawdzenie zgodności z zasadami implementacji
- Weryfikacja zgodności z Checkstyle
- Review bezpieczeństwa i wydajności

### Krok 11: Wdrożenie i monitoring

**11.1 Wdrożenie:**
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Wdrożenie na produkcję przez CI/CD pipeline

**11.2 Monitoring:**
- Konfiguracja metryk Prometheus
- Konfiguracja alertów dla wysokiego wskaźnika błędów
- Monitorowanie czasu odpowiedzi i wykorzystania zasobów

## 10. Podsumowanie

Plan implementacji endpointu **GET /api/auth/me** obejmuje kompleksowe podejście do wdrożenia zgodnie z najlepszymi praktykami Spring Boot i wymaganiami projektu. Kluczowe aspekty:

- **Bezpieczeństwo:** Pełna walidacja JWT, autoryzacja, ochrona przed typowymi atakami
- **Wydajność:** Optymalizacja zapytań, opcjonalne cache'owanie, rate limiting
- **Obsługa błędów:** Centralna obsługa wyjątków z odpowiednimi kodami statusu
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Dokumentacja:** Swagger/OpenAPI, dokumentacja kodu

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.

