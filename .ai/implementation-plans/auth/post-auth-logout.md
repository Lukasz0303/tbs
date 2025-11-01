# API Endpoint Implementation Plan: POST /api/auth/logout

## 1. Przegląd punktu końcowego

**POST /api/auth/logout** to endpoint uwierzytelniania służący do wylogowania bieżącego zalogowanego użytkownika. Endpoint wymaga ważnego tokenu JWT w nagłówku Authorization i kończy sesję użytkownika.

Endpoint może być używany do:
- Wylogowania użytkownika z aplikacji
- Unieważnienia tokenu JWT (jeśli implementowane token blacklist w Redis)
- Aktualizacji `last_seen_at` użytkownika (opcjonalne)

Kluczowe zastosowania:
- Wylogowanie użytkownika z aplikacji
- Zakończenie sesji
- Zabezpieczenie przed nieautoryzowanym dostępem po zamknięciu sesji

## 2. Szczegóły żądania

### Metoda HTTP
- **POST** - operacja zmiany stanu (wylogowanie)

### Struktura URL
```
POST /api/auth/logout
```

### Nagłówki żądania

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT wydany po poprawnym logowaniu/rejestracji
- `Content-Type: application/json` - format treści żądania

**Opcjonalne:**
- `Accept: application/json` - preferowany format odpowiedzi

### Parametry URL
- Brak parametrów URL

### Query Parameters
- Brak parametrów zapytania

### Request Body
- Brak ciała żądania (opcjonalnie, jeśli nie wymagane)

### Przykład żądania
```http
POST /api/auth/logout HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
Accept: application/json
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
- Brak - endpoint nie wymaga DTO żądania (lub opcjonalne puste body)

#### Response DTO
**`com.tbs.dto.auth.LogoutResponse`** (istniejący)
```java
public record LogoutResponse(String message) implements MessageResponse {}
```

**Uwagi implementacyjne:**
- `message` - Komunikat potwierdzający wylogowanie (np. "Wylogowano pomyślnie")

### Enums
- Brak bezpośredniego użycia enumów w tym endpoincie

### Modele domenowe (do stworzenia)
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users` (jeśli aktualizacja `last_seen_at`)

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.UnauthorizedException`** - wyjątek dla 401 Unauthorized

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.AuthService`** - serwis obsługujący wylogowanie
- **`com.tbs.service.TokenService`** - zarządzanie tokenami (opcjonalne, blacklist w Redis)

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Pomyślne wylogowanie użytkownika

**Przykład odpowiedzi:**
```json
{
  "message": "Wylogowano pomyślnie"
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

1. **Odebranie żądania HTTP POST /api/auth/logout**
   - Spring Security przechwytuje żądanie
   - Weryfikacja tokenu JWT z nagłówka `Authorization: Bearer <token>`

2. **Walidacja tokenu JWT (Spring Security)**
   - Wyodrębnienie tokenu z nagłówka Authorization
   - Parsowanie i walidacja tokenu (sygnatura, wygaśnięcie)
   - Wyodrębnienie informacji o użytkowniku z claims tokenu
   - Ustawienie `Authentication` w `SecurityContext`

3. **Wyodrębnienie identyfikatora użytkownika**
   - Z `SecurityContext.getAuthentication()` lub z tokenu JWT claims
   - Dla zarejestrowanych: `auth_user_id` (UUID) lub `user_id` (BIGINT)
   - Dla gości: `user_id` (BIGINT)

4. **Unieważnienie tokenu (opcjonalne)**
   - Dodanie tokenu do blacklist w Redis (jeśli implementowane)
   - Klucz: `token:blacklist:{jti}` (jti = JWT ID claim)
   - TTL: czas do wygaśnięcia tokenu

5. **Aktualizacja `last_seen_at` (opcjonalne)**
   - UPDATE `users.last_seen_at = NOW() WHERE id = ?`
   - Opcjonalne, jeśli endpoint ma aktualizować aktywność

6. **Zwrócenie odpowiedzi HTTP 200 OK**
   - Serializacja `LogoutResponse` do JSON

### Integracja z bazą danych

**Tabela: `users`** (opcjonalnie, jeśli aktualizacja `last_seen_at`)
- UPDATE `users.last_seen_at = NOW() WHERE id = ?`
- Kolumna aktualizowana:
  - `last_seen_at` → bieżący timestamp

### Integracja z Redis (opcjonalne, token blacklist)

**Token Blacklist:**
- Klucz: `token:blacklist:{jti}` (jti = JWT ID claim)
- TTL: czas do wygaśnięcia tokenu (z `exp` claim)
- Wartość: timestamp unieważnienia

**Korzyści:**
- Unieważnienie tokenu przed wygaśnięciem
- Zapobieganie użyciu skradzionych tokenów
- Wymaga walidacji blacklist w JWT Authentication Filter

### Integracja z Spring Security

**Mechanizm uwierzytelniania:**
- JWT Authentication Filter w Spring Security Chain
- Wyodrębnienie tokenu z nagłówka `Authorization: Bearer <token>`
- Walidacja tokenu (sygnatura, wygaśnięcie) przez `JwtTokenProvider`
- Ustawienie `Authentication` w `SecurityContext`

**Konfiguracja Security:**
- Endpoint wymaga roli `ROLE_USER` lub `ROLE_GUEST`
- Wyjątek dla nieuwierzytelnionych żądań → 401 Unauthorized

## 6. Względy bezpieczeństwa

### Uwierzytelnianie

**Mechanizm JWT:**
- Token JWT wymagany w nagłówku `Authorization: Bearer <token>`
- Token wydany po poprawnym logowaniu/rejestracji
- Walidacja tokenu przez Spring Security:
  - Weryfikacja sygnatury (algorytm HS256/RS256)
  - Sprawdzenie wygaśnięcia (`exp` claim)
  - Sprawdzenie ważności (`nbf`, `iat` claims)
  - Sprawdzenie blacklist w Redis (jeśli implementowane)

**Ochrona przed atakami:**
- Token blacklist zapobiega użyciu skradzionych tokenów
- Walidacja formatu tokenu przed parsowaniem
- Idempotentność: wielokrotne wywołanie endpointu nie powoduje błędu

### Autoryzacja

**Zasada najniższych uprawnień:**
- Użytkownik może wylogować tylko siebie (token wskazuje na użytkownika)
- Endpoint nie wymaga dodatkowych uprawnień poza uwierzytelnieniem

### Bezpieczeństwo tokenów

**Token Blacklist:**
- Implementacja w Redis z TTL
- Walidacja blacklist przy każdym żądaniu z tokenem
- Klucz: `token:blacklist:{jti}` gdzie `jti` = JWT ID claim

**Obsługa wygaśnięcia:**
- Tokeny wygasają automatycznie zgodnie z `exp` claim
- Blacklist TTL ustawiony na czas do wygaśnięcia tokenu

### Ochrona przed typowymi atakami

**Token Replay:**
- Token blacklist zapobiega użyciu tokenów po wylogowaniu
- Walidacja `exp` claim zapobiega użyciu wygasłych tokenów

**Logout CSRF:**
- Token JWT w nagłówku Authorization zapewnia ochronę przed podstawowymi atakami CSRF
- Opcjonalnie: CSRF token w sesji

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
- Logowanie próby dostępu bez tokenu

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

#### 3. Błąd Redis (blacklist) (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z Redis, timeout przy dodawaniu do blacklist
```java
try {
    tokenBlacklistService.addToBlacklist(tokenId, expirationTime);
} catch (RedisConnectionException e) {
    log.error("Failed to add token to blacklist", e);
    // Kontynuuj wylogowanie mimo błędu Redis (opcjonalne)
}
```

**Obsługa:**
- Przechwycenie `RedisConnectionException`
- Logowanie błędu
- Opcjonalnie: kontynuacja wylogowania mimo błędu Redis (token wygaśnie naturalnie)

#### 4. Błąd bazy danych (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z bazą danych przy aktualizacji `last_seen_at`
```java
@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ApiErrorResponse> handleDataAccessException(DataAccessException e) {
    log.error("Database error during logout", e);
    return ResponseEntity.status(500)
        .body(new ApiErrorResponse(
            new ErrorDetails("INTERNAL_SERVER_ERROR", "Database error occurred", null)
        ));
}
```

**Obsługa:**
- Przechwycenie `DataAccessException` przez global exception handler
- Zwrócenie 500 Internal Server Error z ogólnym komunikatem
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
    
    @ExceptionHandler({DataAccessException.class, Exception.class})
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
        // 500 handling
    }
}
```

### Logowanie błędów

**Poziomy logowania:**
- **INFO:** Pomyślne wylogowanie użytkownika (dla audytu)
- **WARN:** Próba dostępu z nieprawidłowym tokenem
- **ERROR:** Błędy Redis, błędy bazy danych

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, userId (jeśli dostępne), stack trace (dla błędów)

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Aktualizacja `last_seen_at` (opcjonalne):**
- UPDATE tylko kolumny `last_seen_at` (nie wszystkie kolumny)
- Użycie indeksu na `id` (PK, automatyczny)

**Strategia:**
- Jeśli endpoint jest wywoływany często, aktualizacja `last_seen_at` może być przeniesiona do asynchronicznego zadania
- Alternatywnie: aktualizacja `last_seen_at` w innych endpointach (np. `/api/auth/me`)

### Integracja z Redis (token blacklist)

**Optymalizacja:**
- Użycie `SETEX` dla jednoczesnego SET i TTL
- Sprawdzanie blacklist w JWT Authentication Filter (cache wynik)

**TTL:**
- TTL ustawiony na czas do wygaśnięcia tokenu (z `exp` claim)
- Automatyczne wygaśnięcie z Redis (nie wymaga ręcznego czyszczenia)

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Klucz: `rate_limit:auth:logout:{userId}`

**Korzyści:**
- Zapobieganie nadużyciom
- Sprawiedliwy podział zasobów

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="POST",endpoint="/api/auth/logout",status="200"}` - liczba pomyślnych wylogowań
- `http_requests_total{method="POST",endpoint="/api/auth/logout",status="401"}` - liczba błędów 401
- `http_request_duration_seconds{method="POST",endpoint="/api/auth/logout"}` - czas odpowiedzi
- `token_blacklist_operations_total{operation="add",status="success|error"}` - metryki blacklist

**Alerty:**
- Wysoki wskaźnik błędów 401 (>10% żądań)
- Długi czas odpowiedzi (>500ms)
- Wysoki wskaźnik błędów 500 (>1% żądań)

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `LogoutResponse` DTO istnieje
- Sprawdzenie konfiguracji Spring Security
- Weryfikacja struktury pakietów

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.service.AuthService` - serwis obsługujący wylogowanie
- `com.tbs.service.TokenBlacklistService` - zarządzanie token blacklist w Redis (opcjonalne)
- `com.tbs.exception.UnauthorizedException` - wyjątek dla 401

**1.3 Konfiguracja zależności:**
- Redis client (jeśli implementowane token blacklist)

### Krok 2: Implementacja token blacklist (opcjonalne)

**2.1 Utworzenie TokenBlacklistService:**
```java
@Service
public class TokenBlacklistService {
    private final RedisTemplate<String, String> redisTemplate;
    
    public void addToBlacklist(String tokenId, Instant expirationTime) {
        long ttl = Duration.between(Instant.now(), expirationTime).getSeconds();
        redisTemplate.opsForValue().set("token:blacklist:" + tokenId, "true", ttl, TimeUnit.SECONDS);
    }
    
    public boolean isBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("token:blacklist:" + tokenId));
    }
}
```

**2.2 Integracja z JWT Authentication Filter:**
- Sprawdzenie blacklist przed walidacją tokenu
- Jeśli token w blacklist → 401 Unauthorized

### Krok 3: Implementacja serwisu wylogowania

**3.1 Utworzenie AuthService:**
```java
@Service
@Transactional
public class AuthService {
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    
    public LogoutResponse logout(String token) {
        Long userId = authenticationService.getCurrentUserId();
        String tokenId = jwtTokenProvider.getTokenId(token);
        
        tokenBlacklistService.addToBlacklist(tokenId, jwtTokenProvider.getExpiration(token));
        
        // Opcjonalnie: aktualizacja last_seen_at
        userRepository.updateLastSeenAt(userId, Instant.now());
        
        return new LogoutResponse("Wylogowano pomyślnie");
    }
}
```

**3.2 Testy serwisu:**
- Test jednostkowy z Mockito dla pomyślnego wylogowania
- Test dla przypadku dodania tokenu do blacklist
- Test dla przypadku błędu Redis

### Krok 4: Implementacja kontrolera

**4.1 Utworzenie AuthController:**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final HttpServletRequest request;
    
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LogoutResponse> logout() {
        String token = extractTokenFromRequest(request);
        LogoutResponse response = authService.logout(token);
        return ResponseEntity.ok(response);
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

**4.2 Konfiguracja Spring Security:**
- Upewnienie się, że `/api/auth/logout` wymaga uwierzytelnienia
- Konfiguracja `SecurityFilterChain` z JWT authentication filter

**4.3 Testy kontrolera:**
- Test integracyjny z `@WebMvcTest` dla pomyślnego przypadku (200)
- Test dla przypadku bez tokenu (401)
- Test dla przypadku z nieprawidłowym tokenem (401)

### Krok 5: Implementacja obsługi błędów

**5.1 Utworzenie global exception handler:**
- Obsługa `UnauthorizedException` (401)
- Obsługa `DataAccessException` (500)
- Obsługa `RedisConnectionException` (500, opcjonalnie)

**5.2 Testy exception handler:**
- Test dla każdego typu wyjątku
- Weryfikacja formatu odpowiedzi błędu

### Krok 6: Konfiguracja Swagger/OpenAPI

**6.1 Dodanie adnotacji Swagger:**
```java
@Operation(
    summary = "Logout user",
    description = "Logs out the currently authenticated user"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "User logged out successfully"),
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
})
@PostMapping("/logout")
public ResponseEntity<LogoutResponse> logout() {
    // ...
}
```

### Krok 7: Testy integracyjne i E2E

**7.1 Testy integracyjne:**
- Test pełnego przepływu z tokenem JWT
- Test token blacklist w Redis
- Test aktualizacji `last_seen_at` (jeśli implementowane)

**7.2 Testy E2E (Cypress):**
- Test wylogowania po logowaniu
- Test obsługi błędu 401 dla nieuwierzytelnionego żądania
- Test że token nie działa po wylogowaniu (jeśli blacklist)

### Krok 8: Dokumentacja i code review

**8.1 Dokumentacja:**
- Aktualizacja README z informacjami o endpoincie
- Dokumentacja Swagger/OpenAPI
- Dokumentacja token blacklist (jeśli implementowane)

**8.2 Code review:**
- Sprawdzenie zgodności z zasadami implementacji
- Review bezpieczeństwa
- Weryfikacja obsługi błędów

### Krok 9: Wdrożenie i monitoring

**9.1 Wdrożenie:**
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Test token blacklist w dev

**9.2 Monitoring:**
- Konfiguracja metryk Prometheus
- Konfiguracja alertów dla wysokiego wskaźnika błędów
- Monitorowanie operacji token blacklist

## 10. Podsumowanie

Plan implementacji endpointu **POST /api/auth/logout** obejmuje kompleksowe podejście do wdrożenia z opcjonalną implementacją token blacklist. Kluczowe aspekty:

- **Bezpieczeństwo:** Token blacklist, unieważnienie tokenów, ochrona przed replay attacks
- **Wydajność:** Optymalizacja operacji Redis i bazy danych
- **Obsługa błędów:** Centralna obsługa z odpowiednimi kodami statusu
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Monitoring:** Metryki i alerty dla token blacklist i operacji logout

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
