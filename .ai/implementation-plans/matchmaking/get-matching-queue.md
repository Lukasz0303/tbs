# API Endpoint Implementation Plan: GET /api/v1/matching/queue

> **Status:** ⏳ Do implementacji

## 1. Przegląd punktu końcowego

GET /api/v1/matching/queue - Pobranie listy wszystkich graczy w kolejce matchmakingu

Endpoint umożliwia pobranie listy wszystkich graczy znajdujących się w kolejce matchmakingu wraz z ich statusem (czekają, grają, zmapowani). Informacje obejmują również szczegóły o aktywnych grach dla graczy, którzy zostali już zmapowani. Endpoint wykorzystuje Redis do pobrania danych z kolejki i PostgreSQL do sprawdzenia statusu aktywnych gier.

## 2. Szczegóły żądania

- **Metoda HTTP:** GET
- **URL:** `/api/v1/matching/queue`
- **Autoryzacja:** Wymagane (JWT token)
- **Query Parameters:**
  - `boardSize` (opcjonalne): Filtrowanie po rozmiarze planszy
    - Typ: `BoardSize` enum
    - Możliwe wartości: `THREE`, `FOUR`, `FIVE`
    - Jeśli nie podano, zwracane są gracze ze wszystkich rozmiarów planszy

**Przykłady:**
- `GET /api/v1/matching/queue` - Wszyscy gracze w kolejce
- `GET /api/v1/matching/queue?boardSize=THREE` - Tylko gracze czekający na planszę 3x3

## 3. Wykorzystywane typy

**Response DTO:**
- `QueueStatusResponse` - zawiera listę graczy w kolejce
  - `players: List<PlayerQueueStatus>`
  - `totalCount: Integer`

**Nested DTO:**
- `PlayerQueueStatus` - zawiera informacje o statusie gracza w kolejce:
  - `userId: Long`
  - `username: String`
  - `boardSize: BoardSize`
  - `status: QueuePlayerStatus` (enum: `WAITING`, `MATCHED`, `PLAYING`)
  - `joinedAt: Instant`
  - `matchedWith: Long` (opcjonalne, tylko gdy status = MATCHED lub PLAYING)
  - `matchedWithUsername: String` (opcjonalne)
  - `gameId: Long` (opcjonalne, tylko gdy status = PLAYING)
  - `isMatched: Boolean`

**Enums:**
- `BoardSize`: `THREE(3)`, `FOUR(4)`, `FIVE(5)`
- `QueuePlayerStatus`: `WAITING`, `MATCHED`, `PLAYING`

**Modele domenowe:**
- `QueueEntry` (Redis): `userId: Long`, `boardSize: BoardSize`, `joinedAt: Instant`
- `Game Entity`: encja reprezentująca grę PvP z polami:
  - `id: Long`
  - `player1Id: Long`
  - `player2Id: Long`
  - `status: GameStatus`
  - `boardSize: BoardSize`

## 4. Szczegóły odpowiedzi

**Response (200 OK):**
```json
{
  "players": [
    {
      "userId": 1,
      "username": "player1",
      "boardSize": "THREE",
      "status": "WAITING",
      "joinedAt": "2024-01-15T10:30:00Z",
      "matchedWith": null,
      "matchedWithUsername": null,
      "gameId": null,
      "isMatched": false
    },
    {
      "userId": 2,
      "username": "player2",
      "boardSize": "THREE",
      "status": "MATCHED",
      "joinedAt": "2024-01-15T10:29:00Z",
      "matchedWith": 1,
      "matchedWithUsername": "player1",
      "gameId": null,
      "isMatched": true
    },
    {
      "userId": 3,
      "username": "player3",
      "boardSize": "FOUR",
      "status": "PLAYING",
      "joinedAt": "2024-01-15T10:25:00Z",
      "matchedWith": 4,
      "matchedWithUsername": "player4",
      "gameId": 123,
      "isMatched": true
    }
  ],
  "totalCount": 3
}
```

**Kody odpowiedzi:**
- **200 OK** - Lista graczy została pomyślnie pobrana
- **401 Unauthorized** - Brak lub nieprawidłowy token JWT
- **400 Bad Request** - Nieprawidłowy parametr boardSize
- **500 Internal Server Error** - Błąd serwera (np. problem z Redis lub PostgreSQL)

## 5. Przepływ danych

```
1. Klient wysyła żądanie GET /api/v1/matching/queue
   ↓
2. SecurityFilter: Walidacja JWT tokena
   ↓
3. MatchingController: Przekazanie do serwisu
   ↓
4. MatchmakingService.getQueueStatus():
   a. Pobranie userId z SecurityContext (opcjonalne, dla autoryzacji)
   b. Walidacja query parameter boardSize (jeśli podano)
   c. Pobranie wszystkich wpisów z kolejki Redis
      - Dla każdego boardSize: matchmaking:queue:{boardSize}
   d. Dla każdego gracza w kolejce:
      - Pobranie danych użytkownika z PostgreSQL (username)
      - Sprawdzenie czy gracz ma aktywną grę PvP:
        * Jeśli tak → status = PLAYING, gameId, matchedWith
        * Jeśli nie → sprawdzenie czy jest zmapowany (w Redis)
          - Jeśli zmapowany → status = MATCHED, matchedWith
          - Jeśli nie → status = WAITING
   e. Grupowanie i filtrowanie wyników (jeśli boardSize podano)
   f. Zwrócenie listy PlayerQueueStatus
```

**Klucze Redis:**
- `matchmaking:queue:{boardSize}` - Sorted Set z timestamp jako score
- `matchmaking:user:{userId}` - Hash z danymi gracza (TTL 5 min)
- `matchmaking:matched:{userId}` - Hash z informacją o zmapowaniu (opcjonalne)

**Zapytania PostgreSQL:**
- Pobranie danych użytkownika (username) po userId
- Sprawdzenie aktywnych gier PvP dla gracza:
  ```sql
  SELECT * FROM games 
  WHERE (player1_id = ? OR player2_id = ?) 
  AND status IN ('WAITING', 'IN_PROGRESS')
  AND game_type = 'PVP'
  ```

## 6. Względy bezpieczeństwa

### 6.1 Uwierzytelnianie
- Wymagany prawidłowy token JWT
- Spring Security filtruje nieautoryzowane żądania

### 6.2 Autoryzacja
- Endpoint jest dostępny dla wszystkich zalogowanych użytkowników
- Brak możliwości modyfikacji danych przez endpoint (tylko odczyt)
- W przyszłości można rozważyć ograniczenie dostępu do administratorów

### 6.3 Prywatność danych
- Zwracane są tylko podstawowe informacje o graczach (userId, username)
- Nie są zwracane wrażliwe dane (email, hasło, itp.)
- Endpoint zwraca informacje publiczne o statusie kolejki

### 6.4 Bezpieczeństwo Redis i PostgreSQL
- Izolacja danych kolejki od innych danych (prefix `matchmaking:`)
- Redis nie zawiera wrażliwych danych użytkownika
- Parametryzowane zapytania SQL (ochrona przed SQL injection)

### 6.5 Rate limiting
- Rate limiting per IP (w przyszłości)
- Ograniczenie częstotliwości zapytań (np. max 10 na sekundę)

## 7. Obsługa błędów

### 7.1 Centralna obsługa błędów

**GlobalExceptionHandler z @ControllerAdvice:**
- `IllegalArgumentException` → 400
- `UnauthorizedException` → 401
- `Exception` → 500

### 7.2 Konkretne błędy i odpowiedzi

- `InvalidBoardSizeException` → 400
  - Message: "Board size must be THREE, FOUR, or FIVE"
- `RedisConnectionException` → 500
  - Message: "Matchmaking service is temporarily unavailable"
- `DatabaseException` → 500
  - Message: "Failed to retrieve queue status"

### 7.3 Logowanie błędów

Używając SLF4J Logger:
- ERROR dla błędów serwera (500)
- WARN dla błędów walidacji (400)
- INFO dla pomyślnych operacji
- DEBUG dla szczegółów przepływu (w dev)

**Przykład:**
```java
log.error("Failed to retrieve queue status", exception);
```

## 8. Rozważania dotyczące wydajności

### 8.1 Redis jako źródło danych
- Szybkość: O(N) pobieranie wszystkich wpisów z sorted set (N = liczba graczy)
- Skalowalność: Obsługa 100-500 jednoczesnych użytkowników bez problemu
- Memory usage: ~100-200 bytes na gracza

### 8.2 Optymalizacja zapytań PostgreSQL
- Indeksy:
  ```sql
  CREATE INDEX idx_games_player1_status ON games(player1_id, status);
  CREATE INDEX idx_games_player2_status ON games(player2_id, status);
  CREATE INDEX idx_games_status_type ON games(status, game_type);
  CREATE INDEX idx_users_id ON users(id);
  ```
- Batch queries: Pobieranie danych wielu użytkowników w jednym zapytaniu
- Projekcje: Pobieranie tylko potrzebnych kolumn
- Connection pooling: HikariCP dla zarządzania połączeniami

### 8.3 Cache'owanie wyników
- Cache wyników na 2-3 sekundy (Redis cache)
- Automatyczne unieważnienie cache przy zmianach w kolejce
- Redukcja liczby zapytań do bazy danych

### 8.4 Optymalizacja dla dużych kolejek
- Paginacja wyników (w przyszłości, jeśli kolejka będzie duża)
- Limit zwracanych wyników (np. max 100 graczy)
- Lazy loading dla szczegółów graczy

### 8.5 Monitoring i metryki
- Spring Actuator: Metryki endpointów
- Prometheus: Czasy odpowiedzi, throughput
- Custom metrics:
  - `matchmaking.queue.status.requests` (Counter)
  - `matchmaking.queue.status.response.time` (Histogram)
  - `matchmaking.queue.size` (Gauge)

### 8.6 Potencjalne wąskie gardła
1. Redis connection pool - zwiększyć przy większym obciążeniu
2. Database connections - monitoring HikariCP
3. Wielokrotne zapytania do bazy - optymalizacja przez batch queries lub cache
4. Duża liczba graczy w kolejce - rozważenie paginacji

## 9. Etapy wdrożenia

### 9.1 Przygotowanie infrastruktury
1. Konfiguracja Redis connection w Spring Boot
2. Stworzenie DTO: `QueueStatusResponse`, `PlayerQueueStatus`
3. Stworzenie enum: `QueuePlayerStatus`

### 9.2 Implementacja serwisów
4. RedisService:
   - `getAllQueueEntries()` → List<QueueEntry>
   - `getQueueEntriesForBoardSize(boardSize)` → List<QueueEntry>
   - `isUserMatched(userId)` → boolean
   - `getMatchedUserId(userId)` → Long (opcjonalne)

5. UserService:
   - `getUserById(userId)` → User
   - `getUsersByIds(List<Long> userIds)` → List<User> (batch)

6. GameService:
   - `getActivePvpGameForUser(userId)` → Game (opcjonalne)
   - `getActivePvpGamesForUsers(List<Long> userIds)` → Map<Long, Game> (batch)

7. MatchmakingService:
   - `getQueueStatus(boardSize)` → QueueStatusResponse
   - `determinePlayerStatus(userId, queueEntry)` → QueuePlayerStatus (private)
   - `enrichQueueStatusWithUserData(List<QueueEntry> entries)` → List<PlayerQueueStatus> (private)

### 9.3 Implementacja kontrolera
8. MatchingController:
   - `GET /api/v1/matching/queue` → getQueueStatus(@RequestParam(required = false) BoardSize boardSize)

9. Konfiguracja Swagger:
   - @Operation, @ApiResponse dla endpointu
   - Przykłady response
   - Dokumentacja query parameters

### 9.4 Obsługa błędów i walidacja
10. Custom exceptions:
    - `InvalidBoardSizeException` (jeśli nie istnieje)

11. GlobalExceptionHandler:
    - Mapowanie custom exceptions na kody HTTP
    - Zwracanie spójnego `ErrorResponse`
    - Logowanie błędów

### 9.5 Cache'owanie i optymalizacja
12. Cache configuration:
    - Cache wyników w Redis (TTL 2-3 sekundy)
    - Automatyczne unieważnienie przy zmianach w kolejce

13. Batch queries optimization:
    - Optymalizacja zapytań do PostgreSQL
    - Batch loading danych użytkowników

### 9.6 Testy jednostkowe
14. MatchmakingServiceTest:
    - Pobieranie statusu kolejki
    - Filtrowanie po boardSize
    - Określanie statusu graczy (WAITING, MATCHED, PLAYING)
    - Mocking Redis, Database

15. MatchingControllerTest:
    - Endpoint z query parameter
    - Endpoint bez query parameter
    - Security context mocking
    - Walidacja błędnych parametrów

16. RedisServiceTest:
    - Pobieranie wpisów z kolejki
    - Sprawdzanie zmapowanych graczy

### 9.7 Testy integracyjne
17. MatchmakingIntegrationTest:
    - Pełny przepływ: dodanie do kolejki → sprawdzenie statusu
    - Test z realnym Redis
    - Test z realną bazą danych
    - Test z różnymi statusami graczy

18. SecurityTest:
    - Wymaganie autoryzacji
    - Nieprawidłowy JWT token
    - Dostęp do endpointu dla zalogowanych użytkowników

19. PerformanceTest:
    - Test wydajności z dużą liczbą graczy w kolejce
    - Test cache'owania
    - Test batch queries

