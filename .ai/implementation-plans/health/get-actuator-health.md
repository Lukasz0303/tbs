# API Endpoint Implementation Plan: GET /actuator/health

## 1. Przegląd punktu końcowego

**GET /actuator/health** to endpoint Spring Actuator służący do sprawdzania zdrowia aplikacji i jej komponentów. Endpoint jest publiczny i nie wymaga uwierzytelnienia, co pozwala systemom zewnętrznym (load balancers, monitoring) na sprawdzanie dostępności aplikacji.

Endpoint zwraca:
- **Ogólny status** aplikacji (`UP`, `DOWN`)
- **Status komponentów** (baza danych, Redis, WebSocket)
- Informacje o stanie każdego komponentu

Kluczowe zastosowania:
- Monitorowanie zdrowia aplikacji przez systemy zewnętrzne
- Load balancer health checks
- Systemy monitoringu (Prometheus, Grafana)
- Automatyczne restartowanie kontenerów w Docker/Kubernetes

## 2. Szczegóły żądania

### Metoda HTTP
- **GET** - operacja tylko do odczytu, idempotentna

### Struktura URL
```
GET /actuator/health
```

### Nagłówki żądania

**Wymagane:**
- Brak (endpoint publiczny)

**Opcjonalne:**
- `Accept: application/json` - preferowany format odpowiedzi
- `Accept: */*` - domyślny format

### Parametry URL
- Brak parametrów URL

### Query Parameters
- Brak parametrów zapytania

### Request Body
- Brak ciała żądania (metoda GET)

### Przykład żądania
```http
GET /actuator/health HTTP/1.1
Host: api.example.com
Accept: application/json
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
- Brak - metoda GET nie wymaga DTO żądania

#### Response DTO
**`com.tbs.dto.health.HealthResponse`** (istniejący)
```java
public record HealthResponse(
    HealthStatus status,
    HealthComponents components
) {
    public record HealthComponents(
        HealthComponent db,
        HealthComponent redis,
        HealthComponent websocket
    ) {}
}
```

**`com.tbs.dto.health.HealthComponent`** (istniejący)
```java
public record HealthComponent(HealthStatus status) {}
```

**`com.tbs.dto.health.HealthStatus`** (istniejący)
```java
public enum HealthStatus {
    UP("UP"),
    DOWN("DOWN");
    
    private final String value;
    
    HealthStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
```

**Uwagi implementacyjne:**
- `status` - Ogólny status aplikacji (UP jeśli wszystkie komponenty UP, DOWN w przeciwnym razie)
- `components` - Status poszczególnych komponentów:
  - `db` - Status bazy danych PostgreSQL
  - `redis` - Status Redis
  - `websocket` - Status WebSocket

### Enums

**`com.tbs.dto.health.HealthStatus`** (istniejący)
- `UP` - Komponent działa poprawnie
- `DOWN` - Komponent nie działa lub występują problemy

### Modele domenowe (do stworzenia)
- Brak - endpoint używa Spring Actuator health indicators

### Wyjątki (do stworzenia lub wykorzystania)
- Brak - Spring Actuator obsługuje błędy automatycznie

### Serwisy (do stworzenia lub wykorzystania)
- **Spring Actuator Health Indicators:**
  - `DataSourceHealthIndicator` - dla bazy danych PostgreSQL
  - `RedisHealthIndicator` - dla Redis
  - Custom health indicator dla WebSocket (opcjonalne)

### Konfiguracja Spring Actuator

**Zależności (build.gradle):**
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

**Konfiguracja (application.properties/yml):**
```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=always
management.health.db.enabled=true
management.health.redis.enabled=true
```

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Status zdrowia aplikacji i komponentów

**Przykład odpowiedzi (wszystkie komponenty UP):**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    },
    "websocket": {
      "status": "UP"
    }
  }
}
```

**Przykład odpowiedzi (jeden komponent DOWN):**
```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "UP"
    },
    "redis": {
      "status": "DOWN"
    },
    "websocket": {
      "status": "UP"
    }
  }
}
```

### Kody statusu błędów

**503 Service Unavailable** - Aplikacja nie jest dostępna (gdy status = DOWN)
```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN"
    },
    "redis": {
      "status": "UP"
    },
    "websocket": {
      "status": "UP"
    }
  }
}
```

**500 Internal Server Error** - Nieoczekiwany błąd podczas sprawdzania zdrowia
```json
{
  "error": {
    "code": "INTERNAL_SERVER_ERROR",
    "message": "An unexpected error occurred while checking health",
    "details": null
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

## 5. Przepływ danych

### Sekwencja operacji

1. **Odebranie żądania HTTP GET /actuator/health**
   - Spring Actuator przechwytuje żądanie
   - Routing do health endpoint

2. **Wywołanie Health Indicators**
   - Spring Actuator wywołuje wszystkie zarejestrowane Health Indicators
   - Dla każdego komponentu:
     - **Database (PostgreSQL):** `DataSourceHealthIndicator`
       - Sprawdzenie połączenia z bazą danych
       - Proste zapytanie: `SELECT 1` lub podobne
       - Jeśli błąd połączenia → status = DOWN
     - **Redis:** `RedisHealthIndicator`
       - Sprawdzenie połączenia z Redis
       - Komenda PING
       - Jeśli błąd połączenia → status = DOWN
     - **WebSocket:** Custom health indicator (opcjonalne)
       - Sprawdzenie dostępności WebSocket
       - Jeśli błąd → status = DOWN

3. **Agregacja statusów**
   - Sprawdzenie statusu wszystkich komponentów
   - Określenie ogólnego statusu:
     - Jeśli wszystkie komponenty UP → status = UP
     - Jeśli jakikolwiek komponent DOWN → status = DOWN

4. **Generowanie odpowiedzi**
   - Mapowanie Health Indicators → `HealthResponse` DTO
   - Ustawienie ogólnego statusu
   - Dodanie statusów komponentów

5. **Zwrócenie odpowiedzi HTTP**
   - Jeśli status = UP → 200 OK
   - Jeśli status = DOWN → 503 Service Unavailable
   - Serializacja `HealthResponse` do JSON

### Integracja z bazą danych

**Database Health Check:**
- `DataSourceHealthIndicator` (Spring Actuator)
- Sprawdzenie połączenia z PostgreSQL
- Proste zapytanie: `SELECT 1` lub użycie `SELECT 1 FROM dual` (PostgreSQL)
- Jeśli błąd połączenia lub timeout → status = DOWN

**Strategia:**
- Użycie connection pool do sprawdzenia dostępności
- Timeout dla health check: np. 1 sekunda
- Nie używanie transakcji dla health check

### Integracja z Redis

**Redis Health Check:**
- `RedisHealthIndicator` (Spring Actuator)
- Sprawdzenie połączenia z Redis
- Komenda PING do Redis
- Jeśli błąd połączenia lub timeout → status = DOWN

**Strategia:**
- Użycie `RedisTemplate.ping()` lub `RedisConnection.ping()`
- Timeout dla health check: np. 1 sekunda
- Nie wykonywanie operacji na danych dla health check

### Integracja z WebSocket

**WebSocket Health Check (opcjonalne):**
- Custom health indicator
- Sprawdzenie dostępności WebSocket server
- Jeśli WebSocket nie jest dostępny → status = DOWN

**Implementacja:**
```java
@Component
public class WebSocketHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Sprawdzenie dostępności WebSocket server
            // np. sprawdzenie czy WebSocket handler jest zarejestrowany
            return Health.up()
                .withDetail("status", "UP")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("status", "DOWN")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## 6. Względy bezpieczeństwa

### Uwierzytelnianie

**Publiczny endpoint:**
- Endpoint nie wymaga uwierzytelnienia (publiczny)
- Pozwala systemom zewnętrznym na sprawdzanie dostępności aplikacji
- **WAŻNE:** Endpoint nie powinien zwracać wrażliwych informacji o aplikacji

**Ograniczenia dostępu:**
- Możliwe ograniczenie dostępu do health endpoint tylko dla zaufanych adresów IP (opcjonalne)
- Lub pozostawienie publicznego dla load balancerów

### Walidacja danych wejściowych

**Brak danych wejściowych:**
- Endpoint nie przyjmuje żadnych danych wejściowych (metoda GET, brak parametrów)
- Brak potrzeby walidacji

### Ochrona przed atakami

**DoS (Denial of Service):**
- Health checks są szybkie (timeout 1 sekunda)
- Brak ciężkich operacji w health check
- Rate limiting może być stosowany (ale ostrożnie, żeby nie blokować load balancerów)

**Information Disclosure:**
- Health endpoint nie zwraca wrażliwych informacji (hasła, tokeny, klucze API)
- Tylko status komponentów (UP/DOWN)
- Szczegóły błędów mogą być ograniczone (tylko dla adminów)

**Rate Limiting:**
- Opcjonalne: Limit dla health endpoint (np. 10 żądań/sekundę z jednego IP)
- Uwaga: Zbyt restrykcyjne limity mogą blokować load balancery

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Błąd połączenia z bazą danych (503 Service Unavailable)
**Scenariusz:** Baza danych PostgreSQL nie jest dostępna lub timeout
```java
// Spring Actuator automatycznie obsługuje błędy DataSourceHealthIndicator
// Status komponentu db = DOWN
// Ogólny status = DOWN
// HTTP 503 Service Unavailable
```

**Obsługa:**
- Spring Actuator automatycznie zwraca status DOWN dla komponentu db
- Ogólny status = DOWN
- Zwrócenie 503 Service Unavailable

#### 2. Błąd połączenia z Redis (503 Service Unavailable)
**Scenariusz:** Redis nie jest dostępny lub timeout
```java
// Spring Actuator automatycznie obsługuje błędy RedisHealthIndicator
// Status komponentu redis = DOWN
// Ogólny status = DOWN (jeśli inny komponent też DOWN)
// HTTP 503 Service Unavailable
```

**Obsługa:**
- Spring Actuator automatycznie zwraca status DOWN dla komponentu redis
- Ogólny status = DOWN (jeśli db też DOWN)
- Zwrócenie 503 Service Unavailable

#### 3. Częściowa niedostępność (503 Service Unavailable)
**Scenariusz:** Jeden komponent DOWN, inne UP
```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "UP"
    },
    "redis": {
      "status": "DOWN"
    },
    "websocket": {
      "status": "UP"
    }
  }
}
```

**Obsługa:**
- Ogólny status = DOWN (jeśli jakikolwiek komponent DOWN)
- Zwrócenie 503 Service Unavailable
- Szczegóły o statusie każdego komponentu

#### 4. Błąd health check (500 Internal Server Error)
**Scenariusz:** Nieoczekiwany błąd podczas sprawdzania zdrowia
```java
// Spring Actuator automatycznie obsługuje błędy
// Zwrócenie 500 Internal Server Error
```

**Obsługa:**
- Spring Actuator automatycznie obsługuje błędy
- Zwrócenie 500 Internal Server Error z komunikatem błędu
- Logowanie błędu dla administratorów

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

**Uwaga:** Spring Actuator ma własną obsługę błędów, więc global exception handler może nie być potrzebny dla health endpoint.

### Logowanie błędów

**Poziomy logowania:**
- **INFO:** Pomyślne sprawdzenie zdrowia (wszystkie komponenty UP)
- **WARN:** Częściowa niedostępność (jeden komponent DOWN)
- **ERROR:** Pełna niedostępność (wszystkie komponenty DOWN) lub błąd podczas health check

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, status komponentów, stack trace (dla błędów)

## 8. Rozważania dotyczące wydajności

### Optymalizacja health checks

**Strategia health checks:**
- Health checks powinny być **szybkie** (<1 sekunda)
- Użycie prostych zapytań (SELECT 1, PING)
- Brak ciężkich operacji w health checks
- Timeout dla health checks: 1 sekunda (konfigurowalne)

**Database Health Check:**
- Proste zapytanie: `SELECT 1`
- Użycie connection pool bez transakcji
- Timeout: 1 sekunda

**Redis Health Check:**
- Komenda PING
- Timeout: 1 sekunda

**WebSocket Health Check:**
- Sprawdzenie dostępności handlera (brak rzeczywistego połączenia)
- Timeout: 1 sekunda

### Cache'owanie

**Opcjonalne cache'owanie (niezalecane):**
- Health checks nie powinny być cache'owane (zawsze fresh)
- Load balancery oczekują aktualnego statusu
- Cache może ukryć problemy z dostępnością

### Rate Limiting

**Implementacja:**
- Opcjonalne: Rate limiting dla health endpoint
- Limit: np. 10 żądań/sekundę z jednego IP
- **UWAGA:** Zbyt restrykcyjne limity mogą blokować load balancery
- Lepiej: Bez rate limiting dla health endpoint lub bardzo wysoki limit

### Monitoring i metryki

**Metryki Prometheus:**
- `health_status{component="db|redis|websocket|overall"}` - status zdrowia (1=UP, 0=DOWN)
- `health_check_duration_seconds{component="db|redis|websocket"}` - czas sprawdzania zdrowia
- `health_check_errors_total{component="db|redis|websocket"}` - liczba błędów podczas health check

**Alerty:**
- Status DOWN dla jakiegokolwiek komponentu (>30s) - alert dla administratorów
- Długi czas health check (>1s) - możliwy problem z wydajnością
- Wysoki wskaźnik błędów (>1% health checks) - problem z infrastrukturą

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `HealthResponse`, `HealthComponent`, `HealthStatus` DTO/enum istnieją
- Sprawdzenie zależności Spring Actuator
- Weryfikacja struktury pakietów

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.dto.health.HealthResponse` - jeśli nie istnieje
- `com.tbs.dto.health.HealthComponent` - jeśli nie istnieje
- `com.tbs.dto.health.HealthStatus` - jeśli nie istnieje

**1.3 Konfiguracja zależności (build.gradle):**
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
}
```

### Krok 2: Konfiguracja Spring Actuator

**2.1 Konfiguracja application.properties:**
```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=always
management.endpoint.health.show-components=always
management.health.db.enabled=true
management.health.redis.enabled=true
management.health.defaults.enabled=true

# Timeout dla health checks
management.health.db.timeout=1000
management.health.redis.timeout=1000
```

**2.2 Konfiguracja application.yml (alternatywa):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: always
      show-components: always
  health:
    db:
      enabled: true
      timeout: 1000
    redis:
      enabled: true
      timeout: 1000
    defaults:
      enabled: true
```

### Krok 3: Implementacja custom health indicator dla WebSocket (opcjonalne)

**3.1 Utworzenie WebSocketHealthIndicator:**
```java
@Component
public class WebSocketHealthIndicator implements HealthIndicator {
    
    private final WebSocketHandler webSocketHandler;
    
    @Override
    public Health health() {
        try {
            // Sprawdzenie dostępności WebSocket server
            // np. sprawdzenie czy WebSocket handler jest zarejestrowany
            if (webSocketHandler != null && webSocketHandler.isRunning()) {
                return Health.up()
                    .withDetail("status", "UP")
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("reason", "WebSocket handler not available")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("status", "DOWN")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

**3.2 Testy health indicator:**
- Test jednostkowy dla WebSocketHealthIndicator (UP)
- Test dla przypadku DOWN (WebSocket nie dostępny)

### Krok 4: Konfiguracja DataSource Health Indicator

**4.1 Konfiguracja bazy danych:**
- Spring Actuator automatycznie wykrywa `DataSource` i tworzy `DataSourceHealthIndicator`
- Brak dodatkowej konfiguracji potrzebnej

**4.2 Testy health indicator:**
- Test jednostkowy dla DataSourceHealthIndicator (UP)
- Test dla przypadku DOWN (baza danych nie dostępna)

### Krok 5: Konfiguracja Redis Health Indicator

**5.1 Konfiguracja Redis:**
- Spring Actuator automatycznie wykrywa `RedisConnectionFactory` i tworzy `RedisHealthIndicator`
- Brak dodatkowej konfiguracji potrzebnej

**5.2 Testy health indicator:**
- Test jednostkowy dla RedisHealthIndicator (UP)
- Test dla przypadku DOWN (Redis nie dostępny)

### Krok 6: Konfiguracja health endpoint response

**6.1 Konfiguracja formatu odpowiedzi:**
- Spring Actuator domyślnie zwraca format JSON
- Możliwe dostosowanie formatu przez `HealthEndpointResponseMapper` (opcjonalne)

**6.2 Mapowanie do DTO (opcjonalne):**
```java
@Configuration
public class ActuatorConfig {
    
    @Bean
    public HealthEndpointResponseMapper healthEndpointResponseMapper() {
        return new HealthEndpointResponseMapper() {
            @Override
            public Object mapResponse(Health health) {
                // Mapowanie Health → HealthResponse DTO
                return mapToHealthResponse(health);
            }
        };
    }
}
```

### Krok 7: Testy integracyjne

**7.1 Testy health endpoint:**
- Test integracyjny dla pomyślnego przypadku (200 OK, wszystkie komponenty UP)
- Test dla przypadku gdy baza danych nie dostępna (503, db DOWN)
- Test dla przypadku gdy Redis nie dostępny (503, redis DOWN)
- Test dla przypadku gdy wszystkie komponenty DOWN (503, DOWN)

**7.2 Testy z rzeczywistymi komponentami:**
- Test z rzeczywistą bazą danych PostgreSQL
- Test z rzeczywistym Redis
- Test z rzeczywistym WebSocket (jeśli implementowane)

### Krok 8: Konfiguracja Swagger/OpenAPI

**8.1 Dodanie health endpoint do Swagger:**
```java
@Configuration
@OpenAPIDefinition(...)
public class SwaggerConfig {
    // Health endpoint może być dokumentowany w Swagger
    // lub pozostawiony jako endpoint Spring Actuator (bez dokumentacji)
}
```

**Uwaga:** Health endpoint może nie być widoczny w Swagger (jest to endpoint Spring Actuator, nie REST API).

### Krok 9: Dokumentacja i code review

**9.1 Dokumentacja:**
- Aktualizacja README z informacjami o health endpoint
- Dokumentacja konfiguracji Spring Actuator
- Dokumentacja integracji z load balancerami

**9.2 Code review:**
- Sprawdzenie zgodności z zasadami implementacji
- Review bezpieczeństwa (publiczny endpoint)
- Weryfikacja konfiguracji timeouts

### Krok 10: Wdrożenie i monitoring

**10.1 Wdrożenie:**
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Test z różnymi konfiguracjami komponentów (UP, DOWN)

**10.2 Monitoring:**
- Konfiguracja alertów dla statusu DOWN
- Integracja z Prometheus/Grafana
- Monitorowanie czasu health checks

**10.3 Integracja z load balancerami:**
- Konfiguracja load balancera do sprawdzania `/actuator/health`
- Konfiguracja health check interval (np. co 10 sekund)
- Konfiguracja timeout dla health check (np. 5 sekund)

## 10. Podsumowanie

Plan implementacji endpointu **GET /actuator/health** obejmuje kompleksowe podejście do wdrożenia z użyciem Spring Actuator. Kluczowe aspekty:

- **Bezpieczeństwo:** Publiczny endpoint (bez uwierzytelnienia), brak wrażliwych informacji, opcjonalne ograniczenie dostępu do zaufanych IP
- **Wydajność:** Szybkie health checks (<1s), proste zapytania, timeout dla health checks
- **Obsługa błędów:** Automatyczna obsługa przez Spring Actuator, odpowiednie kody statusu (200, 503)
- **Testowanie:** Testy integracyjne z rzeczywistymi komponentami
- **Monitoring:** Integracja z Prometheus/Grafana, alerty dla statusu DOWN
- **Dokumentacja:** Konfiguracja Spring Actuator, integracja z load balancerami

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
