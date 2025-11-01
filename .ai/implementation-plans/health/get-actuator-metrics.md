# API Endpoint Implementation Plan: GET /actuator/metrics

## 1. Przegląd punktu końcowego

**GET /actuator/metrics** to endpoint Spring Actuator służący do pobrania metryk aplikacji w formacie Prometheus. Endpoint wymaga uwierzytelnienia i jest dostępny tylko dla administratorów, aby zapobiec wyciekowi wrażliwych informacji o wydajności aplikacji.

Endpoint zwraca:
- **Metryki aplikacji** w formacie Prometheus
- Metryki wydajności (czas odpowiedzi, liczba żądań)
- Metryki zasobów (pamięć, CPU)
- Metryki niestandardowe (gry, użytkownicy, itp.)

Kluczowe zastosowania:
- Zbieranie metryk przez Prometheus
- Monitorowanie wydajności aplikacji w Grafanie
- Analiza wydajności i optymalizacja
- Alerting w systemach monitoringu

## 2. Szczegóły żądania

### Metoda HTTP
- **GET** - operacja tylko do odczytu, idempotentna

### Struktura URL
```
GET /actuator/metrics
```

### Nagłówki żądania

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT wydany po poprawnym logowaniu/rejestracji (tylko dla adminów)

**Opcjonalne:**
- `Accept: application/vnd.spring-boot.actuator.v3+json` - format odpowiedzi Spring Actuator
- `Accept: text/plain` - format Prometheus (domyślny)

### Parametry URL
- Brak parametrów URL

### Query Parameters

**Filtrowanie metryk:**
- `tag` (String, opcjonalne) - Filtr metryk według tagów (np. `tag=uri:/api/games`)
- Format: `tag=key:value` (wielokrotne tagi: `tag=uri:/api/games&tag=method:GET`)

**Przykład zapytania:**
```
GET /actuator/metrics/http.server.requests?tag=uri:/api/games&tag=method:GET
```

### Request Body
- Brak ciała żądania (metoda GET)

### Przykład żądania
```http
GET /actuator/metrics HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Accept: text/plain
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
- Brak - metoda GET używa query parameters

#### Response DTO
- **Format Prometheus** (text/plain) - metryki w formacie Prometheus
- **Format JSON** (application/json) - metryki w formacie JSON (Spring Actuator)

**Format Prometheus (domyślny):**
```
# HELP http_server_requests_seconds Duration of HTTP server request handling
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",uri="/api/games",status="200"} 150.0
http_server_requests_seconds_sum{method="GET",uri="/api/games",status="200"} 25.5
http_server_requests_seconds_max{method="GET",uri="/api/games",status="200"} 0.5

# HELP jvm_memory_used_bytes Used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="PS Old Gen"} 1.5E8

# HELP custom_games_total Total number of games created
# TYPE custom_games_total counter
custom_games_total{game_type="vs_bot"} 100.0
custom_games_total{game_type="pvp"} 50.0
```

**Format JSON (Spring Actuator):**
```json
{
  "names": [
    "http.server.requests",
    "jvm.memory.used",
    "custom.games.total"
  ]
}
```

### Enums
- Brak bezpośredniego użycia enumów w tym endpoincie

### Modele domenowe (do stworzenia)
- Brak - endpoint używa Spring Actuator metrics

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.UnauthorizedException`** - wyjątek dla 401 Unauthorized
- **`com.tbs.exception.ForbiddenException`** - wyjątek dla 403 Forbidden (nie-admin)

### Serwisy (do stworzenia lub wykorzystania)
- **Spring Actuator Metrics:**
  - `MeterRegistry` - rejestr metryk
  - `Counter` - liczniki
  - `Timer` - czasomierze
  - `Gauge` - wartości chwilowe
- **Custom metrics** (opcjonalne):
  - Metryki gier (liczba utworzonych gier, aktywnych gier)
  - Metryki użytkowników (liczba aktywnych użytkowników)
  - Metryki WebSocket (liczba aktywnych połączeń)

### Konfiguracja Spring Actuator

**Zależności (build.gradle):**
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
}
```

**Konfiguracja (application.properties/yml):**
```properties
management.endpoints.web.exposure.include=metrics,prometheus
management.endpoint.metrics.enabled=true
management.metrics.export.prometheus.enabled=true
management.endpoints.web.security.roles=ADMIN
```

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Metryki w formacie Prometheus

**Przykład odpowiedzi (format Prometheus):**
```
# HELP http_server_requests_seconds Duration of HTTP server request handling
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",uri="/api/games",status="200"} 150.0
http_server_requests_seconds_sum{method="GET",uri="/api/games",status="200"} 25.5
http_server_requests_seconds_max{method="GET",uri="/api/games",status="200"} 0.5

# HELP jvm_memory_used_bytes Used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="PS Old Gen"} 1.5E8

# HELP custom_games_total Total number of games created
# TYPE custom_games_total counter
custom_games_total{game_type="vs_bot"} 100.0
custom_games_total{game_type="pvp"} 50.0
```

**Przykład odpowiedzi (format JSON - lista metryk):**
```json
{
  "names": [
    "http.server.requests",
    "jvm.memory.used",
    "jvm.gc.pause",
    "custom.games.total",
    "custom.active.users"
  ]
}
```

### Kody statusu błędów

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

**403 Forbidden** - Brak uprawnień administratora
```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "Access denied - admin role required",
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

1. **Odebranie żądania HTTP GET /actuator/metrics**
   - Spring Actuator przechwytuje żądanie
   - Routing do metrics endpoint

2. **Weryfikacja uwierzytelnienia**
   - Walidacja tokenu JWT przez Spring Security
   - Sprawdzenie roli użytkownika (wymagana rola ADMIN)
   - Jeśli brak uwierzytelnienia → 401 Unauthorized
   - Jeśli brak uprawnień administratora → 403 Forbidden

3. **Pobranie metryk z MeterRegistry**
   - Spring Actuator pobiera wszystkie metryki z `MeterRegistry`
   - Filtrowanie metryk według tagów (jeśli podane query parameters)
   - Formatowanie metryk do formatu Prometheus

4. **Generowanie odpowiedzi Prometheus**
   - Konwersja metryk Spring Actuator → format Prometheus
   - Dodanie HELP i TYPE dla każdej metryki
   - Formatowanie wartości metryk (liczba, czas, itp.)

5. **Zwrócenie odpowiedzi HTTP 200 OK**
   - Serializacja metryk do formatu Prometheus (text/plain)
   - Ustawienie nagłówka `Content-Type: text/plain; version=0.0.4`

### Integracja z Spring Actuator

**MeterRegistry:**
- `MeterRegistry` (Micrometer) - centralny rejestr metryk
- Automatyczne zbieranie metryk z Spring Boot:
  - HTTP request metrics (`http.server.requests`)
  - JVM metrics (`jvm.memory.used`, `jvm.gc.pause`)
  - Database metrics (`jdbc.connections.active`)
  - Custom metrics (zdefiniowane przez aplikację)

**Prometheus Registry:**
- `PrometheusMeterRegistry` - rejestr Prometheus
- Automatyczna konwersja metryk do formatu Prometheus
- Eksport metryk przez `/actuator/prometheus` endpoint (alternatywa)

### Metryki domyślne Spring Boot

**HTTP Request Metrics:**
- `http.server.requests` - metryki żądań HTTP
  - Tagi: `method`, `uri`, `status`
  - Wartości: `count`, `sum`, `max` (czas odpowiedzi)

**JVM Metrics:**
- `jvm.memory.used` - użyta pamięć JVM
- `jvm.gc.pause` - czas pauzy garbage collection
- `jvm.threads.live` - liczba żywych wątków

**Database Metrics:**
- `jdbc.connections.active` - aktywne połączenia z bazą danych
- `jdbc.connections.idle` - bezczynne połączenia z bazą danych

### Custom Metrics (opcjonalne)

**Metryki gier:**
```java
@Service
public class GameMetrics {
    private final Counter gamesCreatedCounter;
    private final Gauge activeGamesGauge;
    
    @Autowired
    public GameMetrics(MeterRegistry meterRegistry) {
        this.gamesCreatedCounter = Counter.builder("custom.games.total")
            .description("Total number of games created")
            .tag("game_type", "vs_bot|pvp")
            .register(meterRegistry);
        
        this.activeGamesGauge = Gauge.builder("custom.games.active")
            .description("Number of active games")
            .register(meterRegistry, this, GameMetrics::getActiveGamesCount);
    }
    
    public void incrementGamesCreated(GameType gameType) {
        gamesCreatedCounter.increment(Tags.of("game_type", gameType.getValue()));
    }
    
    private double getActiveGamesCount() {
        return gameRepository.countByStatus(GameStatus.IN_PROGRESS);
    }
}
```

**Metryki użytkowników:**
```java
@Service
public class UserMetrics {
    private final Gauge activeUsersGauge;
    
    @Autowired
    public UserMetrics(MeterRegistry meterRegistry) {
        this.activeUsersGauge = Gauge.builder("custom.users.active")
            .description("Number of active users")
            .register(meterRegistry, this, UserMetrics::getActiveUsersCount);
    }
    
    private double getActiveUsersCount() {
        return userRepository.countByLastSeenAtAfter(Instant.now().minusSeconds(300));
    }
}
```

## 6. Względy bezpieczeństwa

### Uwierzytelnianie

**Mechanizm JWT:**
- Token JWT wymagany w nagłówku `Authorization: Bearer <token>`
- Token wydany po poprawnym logowaniu/rejestracji
- Walidacja tokenu przez Spring Security:
  - Weryfikacja sygnatury
  - Sprawdzenie wygaśnięcia
  - Sprawdzenie ważności

**Autoryzacja:**
- Endpoint wymaga uwierzytelnienia (`@PreAuthorize("isAuthenticated()")`)
- Endpoint wymaga roli ADMIN (`@PreAuthorize("hasRole('ADMIN')")`)
- Tylko administratorzy mogą przeglądać metryki aplikacji

**Konfiguracja Spring Security:**
```java
@Configuration
public class ActuatorSecurityConfig {
    
    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .requestMatchers(EndpointRequest.to("metrics", "prometheus"))
            .authorizeHttpRequests(requests -> requests
                .requestMatchers(EndpointRequest.to("metrics", "prometheus"))
                .hasRole("ADMIN")
            );
        return http.build();
    }
}
```

### Walidacja danych wejściowych

**Query Parameters:**
- `tag` - Filtrowanie metryk według tagów
- Walidacja formatu: `tag=key:value`
- Sanityzacja: trim whitespace, walidacja wartości

### Ochrona przed atakami

**SQL Injection:**
- Brak bezpośredniego SQL (użycie MeterRegistry)
- Brak potrzeby walidacji

**Information Disclosure:**
- Metryki mogą zawierać wrażliwe informacje o wydajności aplikacji
- Dlatego endpoint wymaga roli ADMIN
- Nie powinien być publiczny

**Rate Limiting:**
- Opcjonalne: Rate limiting dla metrics endpoint
- Limit: np. 100 żądań/minutę na administratora
- Implementacja przez Redis

### Ochrona przed nadużyciami

**Ograniczenie dostępu:**
- Tylko administratorzy mogą przeglądać metryki
- Opcjonalne: Ograniczenie dostępu do zaufanych adresów IP (dla Prometheus)

**Audyt:**
- Logowanie wszystkich żądań do metrics endpoint
- Zawartość logów: timestamp, userId, admin username, IP address

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Brak uwierzytelnienia (401 Unauthorized)
**Scenariusz:** Żądanie bez tokenu JWT lub z nieprawidłowym tokenem
```java
// Spring Security automatycznie zwróci 401 przed dotarciem do kontrolera
// Wymagane: konfiguracja SecurityFilterChain z wyjątkiem dla nieuwierzytelnionych
```

**Obsługa:**
- Spring Security przechwytuje żądanie przed endpointem
- Zwraca 401 Unauthorized z komunikatem "Authentication required"
- Logowanie próby dostępu bez tokenu

#### 2. Brak uprawnień administratora (403 Forbidden)
**Scenariusz:** Użytkownik zalogowany, ale bez roli ADMIN
```java
// Spring Security automatycznie zwróci 403 przed dotarciem do kontrolera
// Wymagane: konfiguracja SecurityFilterChain z wymaganą rolą ADMIN
```

**Obsługa:**
- Spring Security przechwytuje żądanie przed endpointem
- Zwraca 403 Forbidden z komunikatem "Access denied - admin role required"
- Logowanie próby dostępu bez uprawnień administratora

#### 3. Błąd pobierania metryk (500 Internal Server Error)
**Scenariusz:** Nieoczekiwany błąd podczas pobierania metryk z MeterRegistry
```java
@ExceptionHandler({Exception.class})
public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
    log.error("Error while retrieving metrics", e);
    return ResponseEntity.status(500)
        .body(new ApiErrorResponse(
            new ErrorDetails("INTERNAL_SERVER_ERROR", "Error retrieving metrics", null)
        ));
}
```

**Obsługa:**
- Przechwycenie wyjątku przez global exception handler
- Zwrócenie 500 Internal Server Error z ogólnym komunikatem
- Logowanie szczegółów błędu

### Global Exception Handler

**Struktura:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler({Exception.class})
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
        // 500 handling
    }
}
```

**Uwaga:** Spring Actuator ma własną obsługę błędów, więc global exception handler może nie być potrzebny dla metrics endpoint.

### Logowanie błędów

**Poziomy logowania:**
- **INFO:** Pomyślne pobranie metryk (userId, admin username)
- **WARN:** Próba dostępu bez uprawnień administratora (userId, username)
- **ERROR:** Błędy podczas pobierania metryk

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, userId, admin username, IP address, stack trace (dla błędów)

## 8. Rozważania dotyczące wydajności

### Optymalizacja pobierania metryk

**Strategia:**
- Metryki są zbierane przez Micrometer/MeterRegistry (lekka operacja)
- Brak ciężkich zapytań do bazy danych dla domyślnych metryk
- Custom metrics mogą wymagać zapytań do bazy danych (opcjonalne)

**Cache'owanie metryk (niezalecane):**
- Metryki nie powinny być cache'owane (zawsze fresh)
- Prometheus oczekuje aktualnych metryk
- Cache może ukryć problemy z wydajnością

### Custom Metrics Performance

**Optymalizacja custom metrics:**
- Użycie Gauge z lazy evaluation dla kosztownych zapytań
- Cache'owanie wyników w Gauge (jeśli metryki są kosztowne)
- Alternatywnie: aktualizacja Gauge przez scheduled tasks (zamiast on-demand)

**Przykład optymalizacji:**
```java
@Service
public class GameMetrics {
    private final AtomicDouble activeGamesCount = new AtomicDouble(0);
    private final Gauge activeGamesGauge;
    
    @Scheduled(fixedRate = 10000) // Co 10 sekund
    public void updateActiveGamesCount() {
        long count = gameRepository.countByStatus(GameStatus.IN_PROGRESS);
        activeGamesCount.set(count);
    }
    
    @Autowired
    public GameMetrics(MeterRegistry meterRegistry) {
        this.activeGamesGauge = Gauge.builder("custom.games.active")
            .description("Number of active games")
            .register(meterRegistry, this, gm -> gm.activeGamesCount.get());
    }
}
```

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 100 żądań/minutę na administratora
- Klucz: `rate_limit:metrics:{userId}`

**Korzyści:**
- Zapobieganie nadmiernemu obciążeniu serwera
- Sprawiedliwy podział zasobów

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="GET",endpoint="/actuator/metrics",status="200"}` - liczba pomyślnych żądań
- `http_requests_total{method="GET",endpoint="/actuator/metrics",status="401"}` - liczba błędów uwierzytelnienia
- `http_requests_total{method="GET",endpoint="/actuator/metrics",status="403"}` - liczba błędów autoryzacji
- `http_request_duration_seconds{method="GET",endpoint="/actuator/metrics"}` - czas odpowiedzi
- `metrics_endpoint_calls_total` - liczba wywołań metrics endpoint

**Alerty:**
- Wysoki wskaźnik błędów 401/403 (>10% żądań) - możliwe próby nieautoryzowanego dostępu
- Długi czas odpowiedzi (>1s) - problem z pobieraniem metryk
- Wysoki wskaźnik błędów 500 (>1% żądań) - problem z infrastrukturą

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja zależności Spring Actuator
- Sprawdzenie zależności Prometheus (Micrometer Prometheus)
- Weryfikacja struktury pakietów

**1.2 Konfiguracja zależności (build.gradle):**
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
}
```

### Krok 2: Konfiguracja Spring Actuator

**2.1 Konfiguracja application.properties:**
```properties
management.endpoints.web.exposure.include=metrics,prometheus
management.endpoint.metrics.enabled=true
management.metrics.export.prometheus.enabled=true

# Security
management.endpoints.web.security.roles=ADMIN
management.endpoint.metrics.roles=ADMIN
```

**2.2 Konfiguracja application.yml (alternatywa):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: metrics,prometheus
      security:
        roles: ADMIN
  endpoint:
    metrics:
      enabled: true
      roles: ADMIN
  metrics:
    export:
      prometheus:
        enabled: true
```

### Krok 3: Konfiguracja Spring Security

**3.1 Utworzenie ActuatorSecurityConfig:**
```java
@Configuration
public class ActuatorSecurityConfig {
    
    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .requestMatchers(EndpointRequest.to("metrics", "prometheus"))
            .authorizeHttpRequests(requests -> requests
                .requestMatchers(EndpointRequest.to("metrics", "prometheus"))
                .hasRole("ADMIN")
            )
            .httpBasic();
        return http.build();
    }
}
```

**3.2 Konfiguracja ról użytkowników:**
- Upewnienie się, że administratorzy mają rolę `ROLE_ADMIN`
- Konfiguracja w Spring Security (UserDetailsService lub podobne)

**3.3 Testy security:**
- Test dla przypadku bez tokenu (401)
- Test dla przypadku bez roli ADMIN (403)
- Test dla przypadku z rolą ADMIN (200)

### Krok 4: Implementacja custom metrics (opcjonalne)

**4.1 Utworzenie GameMetrics:**
```java
@Service
public class GameMetrics {
    private final Counter gamesCreatedCounter;
    private final Gauge activeGamesGauge;
    private final GameRepository gameRepository;
    
    @Autowired
    public GameMetrics(MeterRegistry meterRegistry, GameRepository gameRepository) {
        this.gameRepository = gameRepository;
        
        this.gamesCreatedCounter = Counter.builder("custom.games.total")
            .description("Total number of games created")
            .tag("game_type", "vs_bot|pvp")
            .register(meterRegistry);
        
        this.activeGamesGauge = Gauge.builder("custom.games.active")
            .description("Number of active games")
            .register(meterRegistry, this, GameMetrics::getActiveGamesCount);
    }
    
    public void incrementGamesCreated(GameType gameType) {
        gamesCreatedCounter.increment(Tags.of("game_type", gameType.getValue()));
    }
    
    private double getActiveGamesCount() {
        return gameRepository.countByStatus(GameStatus.IN_PROGRESS);
    }
}
```

**4.2 Integracja z serwisami:**
```java
@Service
public class GameService {
    private final GameMetrics gameMetrics;
    
    public CreateGameResponse createGame(CreateGameRequest request) {
        // ... tworzenie gry
        
        gameMetrics.incrementGamesCreated(game.getGameType());
        
        return response;
    }
}
```

**4.3 Testy custom metrics:**
- Test jednostkowy dla GameMetrics (incrementGamesCreated)
- Test dla Gauge (getActiveGamesCount)
- Test integracyjny z MeterRegistry

### Krok 5: Konfiguracja Prometheus

**5.1 Konfiguracja Prometheus scrape config:**
```yaml
scrape_configs:
  - job_name: 'tbs-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
    basic_auth:
      username: 'admin'
      password: 'password'
```

**5.2 Konfiguracja Prometheus authentication:**
- Użycie basic auth lub bearer token dla Prometheus
- Opcjonalnie: tworzenie specjalnego konta dla Prometheus (read-only access)

### Krok 6: Testy integracyjne

**6.1 Testy metrics endpoint:**
- Test integracyjny dla pomyślnego przypadku (200 OK, metryki w formacie Prometheus)
- Test dla przypadku bez tokenu (401)
- Test dla przypadku bez roli ADMIN (403)
- Test filtrowania metryk według tagów

**6.2 Testy z rzeczywistymi metrykami:**
- Test z rzeczywistym MeterRegistry
- Test z custom metrics
- Test z Prometheus format

### Krok 7: Integracja z Prometheus/Grafana

**7.1 Konfiguracja Prometheus:**
- Konfiguracja scrape config dla `/actuator/prometheus`
- Konfiguracja authentication dla Prometheus
- Test pobierania metryk przez Prometheus

**7.2 Konfiguracja Grafana:**
- Konfiguracja Prometheus jako źródła danych w Grafanie
- Tworzenie dashboardów dla metryk aplikacji
- Konfiguracja alertów w Grafanie

### Krok 8: Dokumentacja i code review

**8.1 Dokumentacja:**
- Aktualizacja README z informacjami o metrics endpoint
- Dokumentacja konfiguracji Spring Actuator
- Dokumentacja integracji z Prometheus/Grafana
- Dokumentacja custom metrics

**8.2 Code review:**
- Sprawdzenie zgodności z zasadami implementacji
- Review bezpieczeństwa (uwierzytelnianie, autoryzacja)
- Weryfikacja custom metrics

### Krok 9: Wdrożenie i monitoring

**9.1 Wdrożenie:**
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Test z Prometheus/Grafana na dev

**9.2 Monitoring:**
- Konfiguracja Prometheus na produkcji
- Konfiguracja alertów w Grafanie
- Monitorowanie metryk aplikacji

**9.3 Integracja z CI/CD:**
- Konfiguracja Prometheus w pipeline (jeśli potrzebne)
- Test metryk w CI/CD

## 10. Podsumowanie

Plan implementacji endpointu **GET /actuator/metrics** obejmuje kompleksowe podejście do wdrożenia z użyciem Spring Actuator i Prometheus. Kluczowe aspekty:

- **Bezpieczeństwo:** Uwierzytelnianie JWT, autoryzacja administratorów, ochrona przed wyciekiem wrażliwych informacji
- **Wydajność:** Efektywne zbieranie metryk, opcjonalne custom metrics, optymalizacja kosztownych zapytań
- **Obsługa błędów:** Automatyczna obsługa przez Spring Actuator, odpowiednie kody statusu (200, 401, 403, 500)
- **Testowanie:** Testy integracyjne z rzeczywistymi metrykami, testy security
- **Monitoring:** Integracja z Prometheus/Grafana, alerty, dashboardy
- **Dokumentacja:** Konfiguracja Spring Actuator, integracja z Prometheus/Grafana, dokumentacja custom metrics

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
