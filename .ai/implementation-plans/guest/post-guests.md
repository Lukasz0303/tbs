# API Endpoint Implementation Plan: POST /api/guests

## 1. Przegląd punktu końcowego

**POST /api/guests** to endpoint służący do utworzenia lub pobrania profilu użytkownika gościa identyfikowanego przez adres IP. Endpoint jest publiczny i nie wymaga uwierzytelnienia. Pozwala użytkownikowi rozpocząć grę bez rejestracji.

Endpoint implementuje logikę "find or create" - jeśli gość z danym IP już istnieje, zwraca istniejący profil (200 OK), jeśli nie, tworzy nowy (201 Created). Goście są identyfikowani wyłącznie przez adres IP i nie mają powiązania z Supabase Auth.

Kluczowe zastosowania:
- Natychmiastowe rozpoczęcie gry bez rejestracji
- Identyfikacja gości przez IP
- Tworzenie tymczasowych profili dla gości
- Zachowanie statystyk gier gości

## 2. Szczegóły żądania

### Metoda HTTP
- **POST** - operacja tworzenia lub odczytu zasobu (upsert)

### Struktura URL
```
POST /api/guests
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

**`GuestRequest`** DTO:
```json
{
  "ipAddress": "192.168.1.100"
}
```

**Walidacja:**
- `ipAddress`: Opcjonalne (String), jeśli nie podano, wyciągnięte z `HttpServletRequest.getRemoteAddr()` lub nagłówków `X-Forwarded-For` / `X-Real-IP`
- Format: IPv4 lub IPv6 (walidacja formatu)

### Przykład żądania
```http
POST /api/guests HTTP/1.1
Host: api.example.com
Content-Type: application/json
Accept: application/json

{
  "ipAddress": "192.168.1.100"
}
```

**Przykład bez ipAddress (wyciągnięte z żądania):**
```http
POST /api/guests HTTP/1.1
Host: api.example.com
Content-Type: application/json
Accept: application/json

{}
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
**`com.tbs.dto.guest.GuestRequest`** (istniejący)
```java
public record GuestRequest(String ipAddress) {}
```

**Uwagi implementacyjne:**
- `ipAddress` - Opcjonalne, jeśli null, wyciągnięte z żądania

#### Response DTO
**`com.tbs.dto.guest.GuestResponse`** (istniejący)
```java
public record GuestResponse(
    long userId,
    boolean isGuest,
    long totalPoints,
    int gamesPlayed,
    int gamesWon,
    Instant createdAt
) {
    public GuestResponse {
        isGuest = true;
    }
}
```

**Uwagi implementacyjne:**
- `userId` - ID użytkownika z tabeli `users.id`
- `isGuest` - Zawsze true dla gości
- `totalPoints` - Z `users.total_points`
- `gamesPlayed` - Z `users.games_played`
- `gamesWon` - Z `users.games_won`
- `createdAt` - Z `users.created_at`

### Enums
- Brak bezpośredniego użycia enumów w tym endpoincie

### Modele domenowe (do stworzenia)
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.BadRequestException`** - wyjątek dla 400 Bad Request (nieprawidłowy IP)

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.GuestService`** - serwis obsługujący gości
- **`com.tbs.service.IpAddressService`** - wyodrębnianie i walidacja adresów IP

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Profil gościa już istnieje (znaleziony po IP)

**Przykład odpowiedzi:**
```json
{
  "userId": 123,
  "isGuest": true,
  "totalPoints": 500,
  "gamesPlayed": 5,
  "gamesWon": 2,
  "createdAt": "2024-01-19T08:20:00Z"
}
```

**201 Created** - Nowy profil gościa utworzony

**Przykład odpowiedzi:**
```json
{
  "userId": 124,
  "isGuest": true,
  "totalPoints": 0,
  "gamesPlayed": 0,
  "gamesWon": 0,
  "createdAt": "2024-01-20T15:30:00Z"
}
```

### Kody statusu błędów

**400 Bad Request** - Nieprawidłowy adres IP
```json
{
  "error": {
    "code": "BAD_REQUEST",
    "message": "Invalid IP address",
    "details": {
      "ipAddress": "Invalid IP address format"
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

1. **Odebranie żądania HTTP POST /api/guests**
   - Walidacja formatu JSON
   - Parsowanie `GuestRequest` DTO

2. **Wyodrębnienie adresu IP**
   - Jeśli `ipAddress` w żądaniu: użyj z żądania
   - Jeśli `ipAddress` null: wyciągnij z `HttpServletRequest`
     - `request.getRemoteAddr()` - bezpośredni adres
     - Nagłówki `X-Forwarded-For` - za proxy/load balancer
     - Nagłówek `X-Real-IP` - za reverse proxy
   - Wybór pierwszego dostępnego źródła

3. **Walidacja adresu IP**
   - Sprawdzenie formatu IPv4 lub IPv6
   - Sprawdzenie zakresu (np. nie 0.0.0.0, nie localhost dla prod)
   - Jeśli nieprawidłowy IP → 400 Bad Request

4. **Wyszukanie istniejącego gościa**
   - Zapytanie: `SELECT * FROM users WHERE ip_address = ? AND is_guest = TRUE`
   - Jeśli znaleziono → 200 OK (zwróć istniejący profil)
   - Jeśli nie znaleziono → kontynuuj do kroku 5

5. **Utworzenie nowego profilu gościa**
   - Wstawienie rekordu do tabeli `users`:
     - `auth_user_id` = NULL (goście nie mają powiązania z Supabase Auth)
     - `username` = NULL
     - `is_guest` = TRUE
     - `ip_address` = adres IP
     - `total_points` = 0
     - `games_played` = 0
     - `games_won` = 0
     - `created_at` = NOW()
     - `updated_at` = NOW()
   - Jeśli błąd bazy danych → 500 Internal Server Error

6. **Generowanie odpowiedzi**
   - Mapowanie encji `User` → `GuestResponse` DTO
   - Ustawienie `isGuest = true`

7. **Zwrócenie odpowiedzi HTTP 200 OK lub 201 Created**
   - Jeśli istniejący profil → 200 OK
   - Jeśli nowy profil → 201 Created
   - Serializacja `GuestResponse` do JSON

### Integracja z bazą danych

**Tabela: `users`**
- SELECT dla istniejącego gościa: `WHERE ip_address = ? AND is_guest = TRUE`
- INSERT dla nowego gościa:
  - `auth_user_id` = NULL
  - `username` = NULL
  - `is_guest` = TRUE
  - `ip_address` = adres IP (INET)
  - `total_points` = 0
  - `games_played` = 0
  - `games_won` = 0
  - `created_at` = NOW()
  - `updated_at` = NOW()

**Indeksy:**
- `idx_users_ip_address` (partial, WHERE `is_guest = TRUE`) - szybkie wyszukiwanie gości po IP

### Wyodrębnianie adresu IP

**Źródła adresu IP:**
1. Z żądania (`GuestRequest.ipAddress`)
2. Z nagłówka `X-Forwarded-For` (za proxy/load balancer)
3. Z nagłówka `X-Real-IP` (za reverse proxy)
4. Z `HttpServletRequest.getRemoteAddr()` (bezpośredni adres)

**Logika wyboru:**
- Sprawdź źródła w kolejności priorytetu
- Użyj pierwszego dostępnego i prawidłowego adresu IP
- Obsługa wielu adresów w `X-Forwarded-For` (pierwszy adres)

**Walidacja IP:**
- Format IPv4: `^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$`
- Format IPv6: `^([0-9a-fA-F]{0,4}:){7}[0-9a-fA-F]{0,4}$`
- Użycie `InetAddress.getByName()` lub Apache Commons Validator

## 6. Względy bezpieczeństwa

### Uwierzytelnianie

**Publiczny endpoint:**
- Endpoint nie wymaga uwierzytelnienia (publiczny)
- Jednak powinien mieć rate limiting, aby zapobiec spamowi

### Walidacja adresu IP

**Format:**
- Walidacja formatu IPv4 lub IPv6
- Sprawdzenie zakresu (np. nie private/localhost w prod, chyba że dev)

**Sanityzacja:**
- Trim whitespace
- Normalizacja formatu IP

### Ochrona przed atakami

**SQL Injection:**
- Użycie parametrówzowanych zapytań (JPA/Hibernate automatycznie)
- Brak dynamicznego SQL na podstawie IP

**IP spoofing:**
- Zaufanie do nagłówków proxy tylko w środowiskach kontrolowanych
- Walidacja źródła IP w produkcji
- Używanie `X-Forwarded-For` tylko za zaufanym proxy

**Rate Limiting:**
- Ograniczenie szybkości dla publicznych endpointów: 100 żądań/minutę na IP (zgodnie z api-plan.md)
- Implementacja przez Redis z algorytmem przesuwającego okna
- Klucz: `rate_limit:guests:{ipAddress}`

**Spam tworzenia gości:**
- Limit: np. 10 nowych gości na godzinę z jednego IP
- Klucz: `rate_limit:guest:create:{ipAddress}`

### Bezpieczeństwo danych

**Ochrona wrażliwych danych:**
- Adresy IP mogą być wrażliwe (GDPR/RODO)
- Rozważ anonimizację IP po pewnym czasie
- Nie logować pełnych adresów IP w logach (maskowanie)

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Nieprawidłowy format IP (400 Bad Request)
**Scenariusz:** Nieprawidłowy format adresu IP w żądaniu
```java
if (!isValidIpAddress(ipAddress)) {
    throw new BadRequestException("Invalid IP address format");
}
```

**Obsługa:**
- Walidacja formatu IP przed użyciem
- Zwrócenie 400 Bad Request z komunikatem "Invalid IP address"
- Logowanie próby z nieprawidłowym IP

#### 2. Błąd bazy danych (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z bazą danych, timeout, błąd SQL
```java
@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ApiErrorResponse> handleDataAccessException(DataAccessException e) {
    log.error("Database error during guest creation", e);
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

#### 3. Nie można wyodrębnić IP (400 Bad Request)
**Scenariusz:** Nie można wyodrębnić adresu IP z żądania
```java
String ipAddress = extractIpAddress(request);
if (ipAddress == null || ipAddress.isEmpty()) {
    throw new BadRequestException("Unable to determine IP address");
}
```

**Obsługa:**
- Sprawdzenie wszystkich źródeł IP
- Jeśli żadne źródło nie jest dostępne → 400 Bad Request
- Logowanie próby bez IP

### Global Exception Handler

**Struktura:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException e) {
        // 400 handling
    }
    
    @ExceptionHandler({DataAccessException.class, Exception.class})
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
        // 500 handling
    }
}
```

### Logowanie błędów

**Poziomy logowania:**
- **INFO:** Pomyślne utworzenie/pobranie profilu gościa (bez pełnego IP dla prywatności)
- **WARN:** Próba z nieprawidłowym IP
- **ERROR:** Błędy bazy danych

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Maskowanie adresów IP w logach (np. `192.168.1.xxx`)
- Zawartość logów: timestamp, poziom, komunikat, maskedIP, stack trace (dla błędów)

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Indeksy:**
- Tabela `users` powinna mieć indeks na `ip_address` (partial, WHERE `is_guest = TRUE`)
- Zapytania powinny używać indeksów (EXPLAIN ANALYZE)

**Zapytania:**
- SELECT dla istniejącego gościa: użycie indeksu na `ip_address`
- Upsert pattern: SELECT → jeśli brak, INSERT (lub użycie PostgreSQL `ON CONFLICT`)

**Optymalizacja upsert:**
- Opcja 1: SELECT → INSERT (jeśli brak)
- Opcja 2: INSERT ... ON CONFLICT DO NOTHING → SELECT (PostgreSQL)
- Opcja 3: Użycie `@Query` z `@Lock(LockModeType.PESSIMISTIC_WRITE)` dla atomicity

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 100 żądań/minutę na IP (zgodnie z api-plan.md)
- Klucz: `rate_limit:guests:{ipAddress}`

**Limit tworzenia gości:**
- Limit: 10 nowych gości na godzinę z jednego IP
- Klucz: `rate_limit:guest:create:{ipAddress}`

**Korzyści:**
- Zapobieganie spamowi
- Sprawiedliwy podział zasobów

### Cache'owanie

**Cache profilu gościa (opcjonalne):**
- Klucz: `guest:profile:{ipAddress}`
- TTL: 5-15 minut
- Strategia: Cache-aside
- Inwalidacja: przy aktualizacji statystyk gościa

**Korzyści:**
- Redukcja obciążenia bazy danych dla często używanych gości
- Szybsze odpowiedzi dla powtarzających się żądań

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="POST",endpoint="/api/guests",status="200"}` - liczba istniejących profili
- `http_requests_total{method="POST",endpoint="/api/guests",status="201"}` - liczba nowych profili
- `http_requests_total{method="POST",endpoint="/api/guests",status="400"}` - liczba błędów walidacji IP
- `http_request_duration_seconds{method="POST",endpoint="/api/guests"}` - czas odpowiedzi
- `guests_created_total` - liczba utworzonych profili gości

**Alerty:**
- Wysoki wskaźnik błędów 400 (>10% żądań) - problem z wyodrębnianiem IP
- Długi czas odpowiedzi (>500ms) - problem z bazą danych
- Wysoki wskaźnik błędów 500 (>1% żądań) - problem z infrastrukturą

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `GuestRequest` i `GuestResponse` DTO istnieją
- Sprawdzenie konfiguracji bazy danych
- Weryfikacja struktury pakietów

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.service.GuestService` - serwis obsługujący gości
- `com.tbs.service.IpAddressService` - wyodrębnianie i walidacja IP
- `com.tbs.exception.BadRequestException` - wyjątek dla 400

**1.3 Konfiguracja zależności:**
- Apache Commons Validator (opcjonalne, dla walidacji IP)

### Krok 2: Implementacja serwisu wyodrębniania IP

**2.1 Utworzenie IpAddressService:**
```java
@Service
public class IpAddressService {
    
    public String extractIpAddress(HttpServletRequest request, String providedIp) {
        if (providedIp != null && !providedIp.isEmpty()) {
            return providedIp;
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        
        return request.getRemoteAddr();
    }
    
    public boolean isValidIpAddress(String ip) {
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
```

**2.2 Testy serwisu:**
- Test wyodrębniania IP z różnych źródeł
- Test walidacji IP

### Krok 3: Implementacja serwisu gości

**3.1 Utworzenie GuestService:**
```java
@Service
@Transactional
public class GuestService {
    private final UserRepository userRepository;
    private final IpAddressService ipAddressService;
    
    public GuestResponse findOrCreateGuest(String ipAddress) {
        Optional<User> existingGuest = userRepository.findByIpAddressAndIsGuest(ipAddress, true);
        
        if (existingGuest.isPresent()) {
            return mapToGuestResponse(existingGuest.get());
        }
        
        User newGuest = createGuestUser(ipAddress);
        User savedGuest = userRepository.save(newGuest);
        return mapToGuestResponse(savedGuest);
    }
    
    private User createGuestUser(String ipAddress) {
        User guest = new User();
        guest.setIsGuest(true);
        guest.setIpAddress(ipAddress);
        guest.setTotalPoints(0);
        guest.setGamesPlayed(0);
        guest.setGamesWon(0);
        return guest;
    }
}
```

**3.2 Testy serwisu:**
- Test jednostkowy z Mockito dla tworzenia nowego gościa (201)
- Test dla przypadku gdy gość już istnieje (200)
- Test dla przypadku nieprawidłowego IP (400)

### Krok 4: Implementacja kontrolera

**4.1 Utworzenie GuestController:**
```java
@RestController
@RequestMapping("/api/guests")
public class GuestController {
    private final GuestService guestService;
    private final IpAddressService ipAddressService;
    private final HttpServletRequest request;
    
    @PostMapping
    public ResponseEntity<GuestResponse> createOrGetGuest(@RequestBody GuestRequest guestRequest) {
        String ipAddress = ipAddressService.extractIpAddress(request, guestRequest.ipAddress());
        
        if (!ipAddressService.isValidIpAddress(ipAddress)) {
            throw new BadRequestException("Invalid IP address");
        }
        
        GuestResponse response = guestService.findOrCreateGuest(ipAddress);
        
        HttpStatus status = response.createdAt().isAfter(Instant.now().minusSeconds(1))
            ? HttpStatus.CREATED : HttpStatus.OK;
        
        return ResponseEntity.status(status).body(response);
    }
}
```

**4.2 Konfiguracja Spring Security:**
- Upewnienie się, że `/api/guests` jest publiczny (permitAll)
- Konfiguracja CORS jeśli potrzebne

**4.3 Testy kontrolera:**
- Test integracyjny z `@WebMvcTest` dla tworzenia nowego gościa (201)
- Test dla przypadku gdy gość już istnieje (200)
- Test dla przypadku nieprawidłowego IP (400)
- Test dla przypadku bez IP w żądaniu (wyciągnięte z request)

### Krok 5: Implementacja obsługi błędów

**5.1 Utworzenie global exception handler:**
- Obsługa `BadRequestException` (400)
- Obsługa `DataAccessException` (500)

**5.2 Testy exception handler:**
- Test dla każdego typu wyjątku
- Weryfikacja formatu odpowiedzi błędu

### Krok 6: Implementacja rate limiting

**6.1 Konfiguracja rate limiting:**
- Implementacja filtru Spring Security lub interceptor
- Integracja z Redis

**6.2 Dodanie rate limiting do endpointu:**
- Limit: 100 żądań/minutę na IP
- Limit tworzenia: 10 gości/godzinę na IP
- Obsługa przekroczenia limitu (429 Too Many Requests)

### Krok 7: Konfiguracja Swagger/OpenAPI

**7.1 Dodanie adnotacji Swagger:**
```java
@Operation(
    summary = "Create or get guest profile",
    description = "Creates a new guest profile or returns existing one based on IP address"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Guest profile already exists"),
    @ApiResponse(responseCode = "201", description = "New guest profile created"),
    @ApiResponse(responseCode = "400", description = "Invalid IP address")
})
@PostMapping
public ResponseEntity<GuestResponse> createOrGetGuest(...) {
    // ...
}
```

### Krok 8: Testy integracyjne i E2E

**8.1 Testy integracyjne:**
- Test pełnego przepływu z bazą danych
- Test upsert pattern (find or create)
- Test wyodrębniania IP z różnych źródeł

**8.2 Testy E2E (Cypress):**
- Test utworzenia profilu gościa
- Test pobrania istniejącego profilu gościa
- Test obsługi błędów walidacji IP

### Krok 9: Dokumentacja i code review

**9.1 Dokumentacja:**
- Aktualizacja README z informacjami o endpoincie
- Dokumentacja Swagger/OpenAPI
- Dokumentacja wyodrębniania IP za proxy

**9.2 Code review:**
- Sprawdzenie zgodności z zasadami implementacji
- Review bezpieczeństwa i prywatności IP
- Weryfikacja obsługi błędów

### Krok 10: Wdrożenie i monitoring

**10.1 Wdrożenie:**
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Test z różnymi konfiguracjami proxy

**10.2 Monitoring:**
- Konfiguracja metryk Prometheus
- Konfiguracja alertów dla wysokiego wskaźnika błędów
- Monitorowanie liczby tworzonych gości

## 10. Podsumowanie

Plan implementacji endpointu **POST /api/guests** obejmuje kompleksowe podejście do wdrożenia z logiką "find or create" dla gości. Kluczowe aspekty:

- **Bezpieczeństwo:** Walidacja IP, ochrona przed spamem, rate limiting, prywatność adresów IP
- **Wydajność:** Optymalizacja zapytań, upsert pattern, cache'owanie
- **Obsługa błędów:** Centralna obsługa z odpowiednimi kodami statusu
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Prywatność:** Maskowanie IP w logach, zgodność z RODO

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
