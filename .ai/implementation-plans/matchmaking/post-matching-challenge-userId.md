# API Endpoint Implementation Plan: POST /api/matching/challenge/{userId}

> **Status:** ⏳ Do implementacji

## 1. Przegląd punktu końcowego

POST /api/matching/challenge/{userId} - Bezpośrednie wyzwanie konkretnego gracza do gry

Endpoint umożliwia graczowi wyzwanie konkretnego gracza do gry PvP. Wyzywający gracz (challenger) musi podać rozmiar planszy, a system sprawdza dostępność wyzwanego gracza i tworzy nową grę. Endpoint wykorzystuje PostgreSQL do sprawdzania danych gracza i tworzenia gry oraz WebSocket do powiadomień.

## 2. Szczegóły żądania

- **Metoda HTTP:** POST
- **URL:** `/api/matching/challenge/{userId}`
- **Autoryzacja:** Wymagane (JWT token)
- **Content-Type:** `application/json`

**Path Parameters:**
- `userId` (wymagane): ID gracza wyzywanego
  - Typ: `Long`
  - Walidacja: musi być dodatnią liczbą, musi istnieć w bazie

**Request Body:**
```json
{
  "boardSize": "THREE" | "FOUR" | "FIVE"
}
```

**Parametry:**
- `boardSize` (wymagane): Rozmiar planszy do gry
  - Typ: `BoardSize` enum
  - Walidacja: `@NotNull(message = "Board size is required")`

## 3. Wykorzystywane typy

**Request DTO:**
- `ChallengeRequest` - zawiera `boardSize: BoardSize`

**Response DTO:**
- `ChallengeResponse` - zawiera pełne informacje o utworzonej grze

**Enums:**
- `BoardSize`: `THREE(3)`, `FOUR(4)`, `FIVE(5)`
- `GameType`: `VS_BOT`, `PVP`
- `GameStatus`: `WAITING`, `IN_PROGRESS`, `FINISHED`, `ABANDONED`, `DRAW`

**Modele domenowe:**
- `Game Entity`: encja reprezentująca grę PvP z polami:
  - `id: Long`
  - `gameType: GameType.PVP`
  - `boardSize: BoardSize`
  - `player1Id: Long`
  - `player2Id: Long`
  - `status: GameStatus`
  - `createdAt: Instant`

## 4. Szczegóły odpowiedzi

**Response (201 Created):**
```json
{
  "gameId": 123,
  "gameType": "PVP",
  "boardSize": "THREE",
  "player1Id": 1,
  "player2Id": 2,
  "status": "WAITING",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Kody odpowiedzi:**
- **201 Created** - Gra została pomyślnie utworzona
- **400 Bad Request** - Nieprawidłowy parametr boardSize lub userId
- **401 Unauthorized** - Brak lub nieprawidłowy token JWT
- **403 Forbidden** - Próba wyzwania samego siebie
- **404 Not Found** - Wyzwany użytkownik nie istnieje
- **409 Conflict** - Wyzwany użytkownik jest niedostępny (w grze lub w kolejce)
- **500 Internal Server Error** - Błąd serwera

## 5. Przepływ danych

```
1. Klient wysyła żądanie POST /api/matching/challenge/{userId}
   ↓
2. SecurityFilter: Walidacja JWT tokena
   ↓
3. MatchingController: Przekazanie do serwisu
   ↓
4. MatchmakingService.createDirectChallenge():
   a. Pobranie challengerId z SecurityContext
   b. Walidacja @Valid request
   c. Sprawdzenie czy challengerId != userId (nie można wyzywać siebie)
   d. Sprawdzenie czy wyzwany gracz istnieje (PostgreSQL)
   e. Sprawdzenie czy wyzwany gracz jest dostępny:
      - Czy ma aktywną grę PvP?
      - Czy jest w kolejce matchmakingu?
      - Czy jest w trybie offline (last_seen_at > 5 min)?
   f. Utworzenie nowej gry PvP w PostgreSQL:
      - status = WAITING
      - gameType = PVP
      - player1Id = challengerId
      - player2Id = userId
      - boardSize = z request
   g. Wysłanie WebSocket notyfikacji do wyzwanego gracza
   h. Zwrócenie informacji o utworzonej grze
```

**Notyfikacje WebSocket:**
- Wysłanie `MatchFoundMessage` do wyzwanego gracza
- Broadcast zawiera informacje o nowej grze
- Retry logic dla nieudanych notyfikacji

## 6. Względy bezpieczeństwa

### 6.1 Uwierzytelnianie
- Wymagany prawidłowy token JWT
- Challenger ID pobierane z SecurityContext (z tokenu JWT)
- Spring Security filtruje nieautoryzowane żądania

### 6.2 Autoryzacja
- Gracz może wyzwywać tylko innych graczy (nie samego siebie)
- Własne ID z SecurityContext vs path parameter `userId` - walidacja różności
- Możliwość dodania w przyszłości rate limiting per użytkownik

### 6.3 Walidacja danych wejściowych
- Bean Validation (`@NotNull`, `@Valid`) dla request body
- Custom validation dla userId (dodatnia liczba, istnieje w bazie)
- Enum validation dla boardSize (zapewnia tylko prawidłowe wartości)

### 6.4 Zapobieganie nadużyciom
- Sprawdzanie czy wyzwany gracz jest dostępny (nie w grze, nie w kolejce)
- Sprawdzanie czy gracz istnieje
- Rate limiting per IP (future consideration)

### 6.5 SQL Injection Protection
- Używanie Spring Data JPA / Hibernate
- Parametryzowane zapytania
- Walidacja ID przed zapytaniami

## 7. Obsługa błędów

### 7.1 Centralna obsługa błędów

**GlobalExceptionHandler z @ControllerAdvice:**
- `MethodArgumentNotValidException` → 400
- `EntityNotFoundException` → 404
- `ConflictException` → 409
- `UnauthorizedException` → 401
- `ForbiddenException` → 403
- `Exception` → 500

### 7.2 Konkretne błędy i odpowiedzi

- `CannotChallengeSelfException` → 403
  - Message: "Users cannot challenge themselves"
- `UserNotFoundException` → 404
  - Message: "Challenged user does not exist"
- `UserUnavailableException` → 409
  - Message: "Challenged user is currently unavailable"
- `InvalidBoardSizeException` → 400
  - Message: "Board size is required and must be THREE, FOUR, or FIVE"
- `DatabaseException` → 500
  - Message: "Failed to create game"

### 7.3 Logowanie błędów

Używając SLF4J Logger:
- ERROR dla błędów serwera (500)
- WARN dla konfliktów (409), zakazanych akcji (403)
- INFO dla pomyślnych operacji
- DEBUG dla szczegółów przepływu (w dev)

**Przykład:**
```java
log.warn("User {} attempted to challenge unavailable user {}", challengerId, userId, exception);
```

## 8. Rozważania dotyczące wydajności

### 8.1 Optymalizacja zapytań PostgreSQL
- Indeksy:
  ```sql
  CREATE INDEX idx_users_id ON users(id);
  CREATE INDEX idx_games_player1_status ON games(player1_id, status);
  CREATE INDEX idx_games_player2_status ON games(player2_id, status);
  CREATE INDEX idx_games_status ON games(status);
  ```
- Projekcje: Pobieranie tylko potrzebnych kolumn
- Connection pooling: HikariCP dla zarządzania połączeniami

### 8.2 Zapytania do bazy danych
- Minimum 3 zapytania:
  1. Sprawdzenie czy wyzwany gracz istnieje
  2. Sprawdzenie czy gracz ma aktywną grę PvP
  3. Sprawdzenie czy gracz jest w kolejce
- Rozważenie optymalizacji przez cache lub join queries

### 8.3 WebSocket notyfikacje
- Asynchroniczne wysyłanie przez CompletableFuture
- Batch sending dla wielu notyfikacji
- Retry logic dla nieudanych notyfikacji
- Timeout handling

### 8.4 Monitoring i metryki
- Spring Actuator: Metryki endpointów
- Prometheus: Czasy odpowiedzi, throughput
- Custom metrics:
  - `matchmaking.challenges.created` (Counter)
  - `matchmaking.challenge.time` (Histogram)

### 8.5 Potencjalne wąskie gardła
1. Database connections - monitoring HikariCP
2. WebSocket notifications - batch sending
3. Wiele zapytań do bazy danych - optymalizacja przez cache

## 9. Etapy wdrożenia

### 9.1 Przygotowanie infrastruktury
1. Konfiguracja PostgreSQL connection pooling
2. Stworzenie DTO: `ChallengeRequest`, `ChallengeResponse`

### 9.2 Implementacja serwisów
3. UserService:
   - `userExists(userId)` → boolean
   - `isUserOnline(userId)` → boolean

4. GameService:
   - `hasActivePvpGame(userId)` → boolean
   - `createPvpGame(player1Id, player2Id, boardSize)` → Game

5. MatchmakingService:
   - `isUserAvailable(userId)` → boolean
   - `createDirectChallenge(challengerId, challengedId, boardSize)` → ChallengeResponse

6. RedisService:
   - `isUserInQueue(userId)` → boolean

### 9.3 Implementacja kontrolera
7. MatchingController:
   - `POST /api/matching/challenge/{userId}` → createDirectChallenge()

8. Konfiguracja Swagger:
   - @Operation, @ApiResponse dla endpointu
   - Przykłady request/response

### 9.4 Obsługa błędów i walidacja
9. Custom exceptions:
   - `CannotChallengeSelfException`
   - `UserNotFoundException`
   - `UserUnavailableException`

10. GlobalExceptionHandler:
    - Mapowanie custom exceptions na kody HTTP
    - Zwracanie spójnego `ErrorResponse`
    - Logowanie błędów

### 9.5 WebSocket notifications
11. WebSocket notifications:
    - `MatchFoundMessage`
    - Wysyłanie do wyzwanego gracza
    - Retry logic dla nieudanych notyfikacji

### 9.6 Testy jednostkowe
12. MatchmakingServiceTest:
    - Bezpośrednie wyzwania
    - Walidacje i edge cases
    - Mocking Database, Redis, WebSocket

13. MatchingControllerTest:
    - Endpoint z poprawnym requestem
    - Endpoint z błędami
    - Security context mocking

### 9.7 Testy integracyjne
14. MatchmakingIntegrationTest:
    - Pełny przepływ: wyzwanie → utworzenie gry
    - Test z realną bazą danych
    - Test WebSocket notyfikacji

15. SecurityTest:
    - Wymaganie autoryzacji
    - Nieprawidłowy JWT token
    - Brak możliwości wyzwania samego siebie
    - Brak możliwości wyzwania niedostępnego gracza

