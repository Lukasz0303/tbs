# API Endpoint Implementation Plan: POST /api/v1/matching/queue

> **Status:** ⏳ Do implementacji

## 1. Przegląd punktu końcowego

POST /api/v1/matching/queue - Dołączenie do kolejki wyszukiwania przeciwnika

Endpoint umożliwia graczowi dołączenie do kolejki matchmakingu PvP dla wybranego rozmiaru planszy. System automatycznie wyszukuje pasującego przeciwnika i tworzy grę. Endpoint wykorzystuje Redis do zarządzania kolejką w czasie rzeczywistym i PostgreSQL do trwałego przechowywania danych.

## 2. Szczegóły żądania

- **Metoda HTTP:** POST
- **URL:** `/api/v1/matching/queue`
- **Autoryzacja:** Wymagane (JWT token)
- **Content-Type:** `application/json`

**Request Body:**
```json
{
  "boardSize": "THREE" | "FOUR" | "FIVE"
}
```

**Parametry:**
- `boardSize` (wymagane): Rozmiar planszy do gry (3x3, 4x4 lub 5x5)
  - Typ: `BoardSize` enum
  - Walidacja: `@NotNull(message = "Board size is required")`

## 3. Wykorzystywane typy

**Request DTO:**
- `MatchmakingQueueRequest` - zawiera `boardSize: BoardSize`

**Response DTO:**
- `MatchmakingQueueResponse` - zawiera `message: String`, `estimatedWaitTime: Integer`

**Enums:**
- `BoardSize`: `THREE(3)`, `FOUR(4)`, `FIVE(5)`

**Modele domenowe:**
- `QueueEntry` (Redis): `userId: Long`, `boardSize: BoardSize`, `joinedAt: Instant`
- `Game Entity`: encja reprezentująca grę PvP

## 4. Szczegóły odpowiedzi

**Response (200 OK):**
```json
{
  "message": "Successfully added to queue",
  "estimatedWaitTime": 30
}
```

**Kody odpowiedzi:**
- **200 OK** - Gracz został pomyślnie dodany do kolejki
- **400 Bad Request** - Nieprawidłowy parametr boardSize
- **401 Unauthorized** - Brak lub nieprawidłowy token JWT
- **409 Conflict** - Gracz już znajduje się w kolejce lub ma aktywną grę PvP
- **500 Internal Server Error** - Błąd serwera (np. problem z Redis)

## 5. Przepływ danych

```
1. Klient wysyła żądanie POST /api/v1/matching/queue
   ↓
2. SecurityFilter: Walidacja JWT tokena
   ↓
3. MatchingController: Przekazanie do serwisu
   ↓
4. MatchmakingService.addToQueue():
   a. Pobranie userId z SecurityContext
   b. Walidacja @Valid request
   c. Sprawdzenie czy gracz już jest w kolejce (Redis)
   d. Sprawdzenie czy gracz ma aktywną grę PvP (PostgreSQL)
   e. Obliczenie szacowanego czasu oczekiwania
   f. Dodanie wpisu do kolejki Redis z TTL 5 min
   g. Jeśli TTL = 0, automatyczne usunięcie (Redis)
   h. Sprawdzenie czy można sparować z drugim graczem
   ↓
5. Jeżeli znaleziono dopasowanie:
   a. Usunięcie obu graczy z kolejki
   b. Utworzenie nowej gry PvP w PostgreSQL
   c. Wysłanie WebSocket notyfikacji obu graczom
   d. Zwrócenie informacji o utworzonej grze
   ↓
6. Jeżeli nie znaleziono dopasowania:
   a. Zwrócenie potwierdzenia dodania do kolejki
   b. Zwrócenie szacowanego czasu oczekiwania
```

**Asynchroniczne dopasowanie graczy:**

Background Job (Spring @Scheduled co 5 sekund):
```
1. MatchmakingScheduler.processMatchmaking():
   a. Pobranie wszystkich wpisów z kolejki Redis
   b. Grupowanie po boardSize
   c. Losowe sparowanie graczy (po 2)
   d. Dla każdej pary:
      - Usunięcie z kolejki
      - Utworzenie gry w PostgreSQL
      - Wysłanie WebSocket notyfikacji
```

**Klucze Redis:**
- `matchmaking:queue:{boardSize}` - Sorted Set z timestamp jako score
- `matchmaking:user:{userId}` - Hash z danymi gracza (TTL 5 min)

## 6. Względy bezpieczeństwa

### 6.1 Uwierzytelnianie
- Wymagany prawidłowy token JWT
- User ID pobierane z SecurityContext (z tokenu JWT)
- Spring Security filtruje nieautoryzowane żądania

### 6.2 Walidacja danych wejściowych
- Bean Validation (`@NotNull`, `@Valid`) dla request body
- Enum validation dla boardSize (zapewnia tylko prawidłowe wartości)

### 6.3 Zapobieganie nadużyciom
- TTL w Redis (5 minut) zapobiega zacinaniu się gracza w kolejce
- Sprawdzanie czy gracz ma aktywną grę przed dołączeniem do kolejki
- Sprawdzanie czy gracz już jest w kolejce
- Rate limiting per IP (w przyszłości)

### 6.4 Bezpieczeństwo Redis
- Izolacja danych kolejki od innych danych (prefix `matchmaking:`)
- Redis nie zawiera wrażliwych danych użytkownika

## 7. Obsługa błędów

### 7.1 Centralna obsługa błędów

**GlobalExceptionHandler z @ControllerAdvice:**
- `MethodArgumentNotValidException` → 400
- `ConflictException` → 409
- `UnauthorizedException` → 401
- `Exception` → 500

### 7.2 Konkretne błędy i odpowiedzi

- `UserAlreadyInQueueException` → 409
  - Message: "User is already in the matchmaking queue"
- `UserHasActiveGameException` → 409
  - Message: "User already has an active PvP game"
- `InvalidBoardSizeException` → 400
  - Message: "Board size is required and must be THREE, FOUR, or FIVE"
- `RedisConnectionException` → 500
  - Message: "Matchmaking service is temporarily unavailable"

### 7.3 Logowanie błędów

Używając SLF4J Logger:
- ERROR dla błędów serwera (500)
- WARN dla konfliktów (409)
- INFO dla pomyślnych operacji
- DEBUG dla szczegółów przepływu (w dev)

**Przykład:**
```java
log.error("Failed to add user {} to matchmaking queue", userId, exception);
```

## 8. Rozważania dotyczące wydajności

### 8.1 Redis jako kolejka
- Szybkość: O(1) dodawanie/usuwanie dla sorted set
- Skalowalność: Obsługa 100-500 jednoczesnych użytkowników bez problemu
- Czyszczenie: TTL automatycznie usuwa stare wpisy
- Memory usage: ~100-200 bytes na gracza

### 8.2 Optymalizacja zapytań PostgreSQL
- Indeksy:
  ```sql
  CREATE INDEX idx_games_player1_status ON games(player1_id, status);
  CREATE INDEX idx_games_player2_status ON games(player2_id, status);
  CREATE INDEX idx_games_status ON games(status);
  ```
- Projekcje: Pobieranie tylko potrzebnych kolumn
- Connection pooling: HikariCP dla zarządzania połączeniami

### 8.3 Background processing
- Scheduled task: Co 5 sekund (konfigurowalne)
- Batch processing: Sparowanie wielu graczy naraz
- Asynchroniczne: WebSocket notyfikacje przez CompletableFuture

### 8.4 Monitoring i metryki
- Spring Actuator: Metryki endpointów
- Prometheus: Czasy odpowiedzi, throughput
- Custom metrics:
  - `matchmaking.queue.size` (Gauge)
  - `matchmaking.matches.created` (Counter)
  - `matchmaking.match.time` (Histogram)

### 8.5 Potencjalne wąskie gardła
1. Redis connection pool - zwiększyć przy większym obciążeniu
2. Database connections - monitoring HikariCP
3. Scheduled task frequency - dostosować do obciążenia
4. WebSocket notifications - batch sending

## 9. Etapy wdrożenia

### 9.1 Przygotowanie infrastruktury
1. Konfiguracja Redis connection w Spring Boot
2. Stworzenie DTO: `MatchmakingQueueRequest`, `MatchmakingQueueResponse`

### 9.2 Implementacja serwisów
3. RedisService:
   - `addToQueue(userId, boardSize)` → void
   - `getQueueForBoardSize(boardSize)` → List<QueueEntry>
   - `isUserInQueue(userId)` → boolean

4. GameService:
   - `hasActivePvpGame(userId)` → boolean
   - `createPvpGame(player1Id, player2Id, boardSize)` → Game

5. MatchmakingService:
   - `addToQueue(userId, boardSize)` → MatchmakingQueueResponse
   - `calculateEstimatedWaitTime(boardSize)` → Integer (private)
   - `tryMatchPlayers()` → List<Game> (private)

### 9.3 Implementacja kontrolera
6. MatchingController:
   - `POST /api/v1/matching/queue` → addToQueue()

7. Konfiguracja Swagger:
   - @Operation, @ApiResponse dla endpointu
   - Przykłady request/response

### 9.4 Obsługa błędów i walidacja
8. Custom exceptions:
   - `UserAlreadyInQueueException`
   - `UserHasActiveGameException`

9. GlobalExceptionHandler:
   - Mapowanie custom exceptions na kody HTTP
   - Zwracanie spójnego `ErrorResponse`
   - Logowanie błędów

### 9.5 Scheduled tasks i asynchroniczność
10. MatchmakingScheduler:
    - `@Scheduled(fixedDelay = 5000)` → processMatchmaking()
    - Grupowanie graczy po boardSize
    - Sparowanie i tworzenie gier
    - WebSocket notyfikacje

11. WebSocket notifications:
    - `MatchFoundMessage`
    - Broadcast do obu graczy
    - Retry logic dla nieudanych notyfikacji

### 9.6 Testy jednostkowe
12. MatchmakingServiceTest:
    - Dodawanie do kolejki
    - Walidacje i edge cases
    - Mocking Redis i Database

13. MatchingControllerTest:
    - Endpoint z poprawnym requestem
    - Endpoint z błędami
    - Security context mocking

14. RedisServiceTest:
    - Operacje na kolejce
    - TTL behavior
    - Concurrent access

### 9.7 Testy integracyjne
15. MatchmakingIntegrationTest:
    - Pełny przepływ: dodanie → dopasowanie → utworzenie gry
    - Test z realnym Redis
    - Test z realną bazą danych

16. SecurityTest:
    - Wymaganie autoryzacji
    - Nieprawidłowy JWT token

