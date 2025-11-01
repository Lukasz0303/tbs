# API Endpoint Implementation Plan: DELETE /api/matching/queue

> **Status:** ⏳ Do implementacji

## 1. Przegląd punktu końcowego

DELETE /api/matching/queue - Opuszczenie kolejki matchmakingu

Endpoint umożliwia graczowi opuszczenie kolejki matchmakingu PvP. Gracz może opuścić kolejkę w dowolnym momencie przed znalezieniem przeciwnika. Endpoint wykorzystuje Redis do usunięcia gracza z kolejki.

## 2. Szczegóły żądania

- **Metoda HTTP:** DELETE
- **URL:** `/api/matching/queue`
- **Autoryzacja:** Wymagane (JWT token)
- **Request Body:** Brak

## 3. Wykorzystywane typy

**Response DTO:**
- `LeaveQueueResponse` - zawiera `message: String`

**Modele domenowe:**
- `QueueEntry` (Redis): `userId: Long`, `boardSize: BoardSize`, `joinedAt: Instant`

## 4. Szczegóły odpowiedzi

**Response (200 OK):**
```json
{
  "message": "Successfully removed from queue"
}
```

**Kody odpowiedzi:**
- **200 OK** - Gracz został pomyślnie usunięty z kolejki
- **401 Unauthorized** - Brak lub nieprawidłowy token JWT
- **404 Not Found** - Gracz nie znajduje się w kolejce
- **500 Internal Server Error** - Błąd serwera (np. problem z Redis)

## 5. Przepływ danych

```
1. Klient wysyła żądanie DELETE /api/matching/queue
   ↓
2. SecurityFilter: Walidacja JWT tokena
   ↓
3. MatchingController: Przekazanie do serwisu
   ↓
4. MatchmakingService.removeFromQueue():
   a. Pobranie userId z SecurityContext
   b. Sprawdzenie czy gracz jest w kolejce (Redis)
   c. Usunięcie wpisu z kolejki Redis
   d. Zwrócenie potwierdzenia usunięcia
```

**Klucze Redis:**
- `matchmaking:queue:{boardSize}` - Sorted Set z timestamp jako score
- `matchmaking:user:{userId}` - Hash z danymi gracza (TTL 5 min)

## 6. Względy bezpieczeństwa

### 6.1 Uwierzytelnianie
- Wymagany prawidłowy token JWT
- User ID pobierane z SecurityContext (z tokenu JWT)
- Spring Security filtruje nieautoryzowane żądania

### 6.2 Autoryzacja
- Gracz może opuścić tylko swoją własną kolejkę
- Brak możliwości usunięcia innego gracza z kolejki

### 6.3 Bezpieczeństwo Redis
- Izolacja danych kolejki od innych danych (prefix `matchmaking:`)
- Redis nie zawiera wrażliwych danych użytkownika

## 7. Obsługa błędów

### 7.1 Centralna obsługa błędów

**GlobalExceptionHandler z @ControllerAdvice:**
- `EntityNotFoundException` → 404
- `UnauthorizedException` → 401
- `Exception` → 500

### 7.2 Konkretne błędy i odpowiedzi

- `UserNotInQueueException` → 404
  - Message: "User is not in the matchmaking queue"
- `RedisConnectionException` → 500
  - Message: "Matchmaking service is temporarily unavailable"

### 7.3 Logowanie błędów

Używając SLF4J Logger:
- ERROR dla błędów serwera (500)
- WARN dla sytuacji gdy gracz próbuje opuścić kolejkę gdy nie jest w niej
- INFO dla pomyślnych operacji
- DEBUG dla szczegółów przepływu (w dev)

**Przykład:**
```java
log.warn("User {} attempted to leave queue but was not in queue", userId);
```

## 8. Rozważania dotyczące wydajności

### 8.1 Redis jako kolejka
- Szybkość: O(1) usuwanie dla sorted set
- Skalowalność: Obsługa 100-500 jednoczesnych użytkowników bez problemu
- Memory usage: Zautomatyzowane czyszczenie po usunięciu

### 8.2 Idempotentność
- Operacja jest idempotentna - wielokrotne wywołania dla tego samego gracza nie powodują błędu
- Sprawdzenie czy gracz jest w kolejce przed usunięciem

### 8.3 Monitoring i metryki
- Spring Actuator: Metryki endpointów
- Prometheus: Czasy odpowiedzi, throughput
- Custom metrics:
  - `matchmaking.queue.size` (Gauge)
  - `matchmaking.leaves` (Counter)

### 8.4 Potencjalne wąskie gardła
1. Redis connection pool - zwiększyć przy większym obciążeniu
2. Network latency - minimalna interakcja z Redis

## 9. Etapy wdrożenia

### 9.1 Przygotowanie infrastruktury
1. Konfiguracja Redis connection w Spring Boot
2. Stworzenie DTO: `LeaveQueueResponse`

### 9.2 Implementacja serwisów
3. RedisService:
   - `removeFromQueue(userId)` → boolean
   - `isUserInQueue(userId)` → boolean

4. MatchmakingService:
   - `removeFromQueue(userId)` → LeaveQueueResponse

### 9.3 Implementacja kontrolera
5. MatchingController:
   - `DELETE /api/matching/queue` → removeFromQueue()

6. Konfiguracja Swagger:
   - @Operation, @ApiResponse dla endpointu
   - Przykłady response

### 9.4 Obsługa błędów i walidacja
7. Custom exceptions:
   - `UserNotInQueueException`

8. GlobalExceptionHandler:
   - Mapowanie custom exceptions na kody HTTP
   - Zwracanie spójnego `ErrorResponse`
   - Logowanie błędów

### 9.5 Testy jednostkowe
9. MatchmakingServiceTest:
    - Usuwanie z kolejki
    - Walidacje i edge cases
    - Mocking Redis

10. MatchingControllerTest:
    - Endpoint z poprawnym requestem
    - Endpoint gdy gracz nie jest w kolejce
    - Security context mocking

11. RedisServiceTest:
    - Operacje na kolejce
    - Sprawdzanie czy gracz jest w kolejce

### 9.6 Testy integracyjne
12. MatchmakingIntegrationTest:
    - Pełny przepływ: dodanie → opuszczenie kolejki
    - Test z realnym Redis
    - Test idempotentności

13. SecurityTest:
    - Wymaganie autoryzacji
    - Nieprawidłowy JWT token
    - Brak możliwości opuszczenia kolejki innego gracza

