# API Endpoint Implementation Plan: POST /api/games/{gameId}/moves

> **Status:** ⏳ Do implementacji

## 1. Przegląd punktu końcowego

**POST /api/games/{gameId}/moves** to endpoint służący do wykonywania ruchu przez gracza w aktywnej grze. Endpoint wymaga uwierzytelnienia i pozwala graczowi wykonać ruch na planszy gry (kółko i krzyżyk).

Endpoint umożliwia:
- Wykonanie ruchu przez gracza w grze vs_bot lub pvp
- Automatyczne wykrywanie wygranej, przegranej lub remisu
- Aktualizację statusu gry po zakończeniu
- Automatyczne wykonanie ruchu bota po ruchu gracza (dla vs_bot)
- Generowanie stanu planszy na podstawie historii ruchów

**Autoryzacja:** Wymagane uwierzytelnienie (token JWT). Gracz musi być uczestnikiem gry (`player1_id` lub `player2_id`) i aktualnym graczem (`game.current_player_symbol` == `request.playerSymbol`).

## 2. Szczegóły żądania

### Metoda HTTP
- **POST** - operacja tworzenia zasobu (ruch)

### Struktura URL
```
POST /api/games/{gameId}/moves
```

### Nagłówki żądania

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT wydany po poprawnym logowaniu/rejestracji
- `Content-Type: application/json` - format treści żądania

**Opcjonalne:**
- `Accept: application/json` - preferowany format odpowiedzi

### Parametry URL
- `gameId` (Long, wymagane) - ID gry, w której wykonywany jest ruch

### Query Parameters
- Brak parametrów zapytania

### Request Body

**`CreateMoveRequest`** DTO:
```json
{
  "row": 0,
  "col": 1,
  "playerSymbol": "x"
}
```

**Walidacja:**
- `row`: Wymagane, liczba całkowita >= 0 (@Min(0))
- `col`: Wymagane, liczba całkowita >= 0 (@Min(0))
- `playerSymbol`: Wymagane, enum ("x" lub "o") (@NotNull)

### Przykład żądania
```http
POST /api/games/42/moves HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
Accept: application/json

{
  "row": 0,
  "col": 1,
  "playerSymbol": "x"
}
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
**`com.tbs.dto.move.CreateMoveRequest`** (istniejący)
```java
public record CreateMoveRequest(
    @Min(value = 0, message = "Row must be non-negative")
    int row,

    @Min(value = 0, message = "Col must be non-negative")
    int col,

    @NotNull(message = "Player symbol is required")
    PlayerSymbol playerSymbol
) {}
```

#### Response DTO
**`com.tbs.dto.move.CreateMoveResponse`** (istniejący)
```java
public record CreateMoveResponse(
    long moveId,
    long gameId,
    int row,
    int col,
    PlayerSymbol playerSymbol,
    int moveOrder,
    Instant createdAt,
    BoardState boardState,
    GameStatus gameStatus,
    WinnerInfo winner
) {}
```

**Uwagi implementacyjne:**
- `boardState` - generowany przez funkcję bazy danych `generate_board_state(gameId)`
- `gameStatus` - zaktualizowany status gry po ruchu
- `winner` - informacje o zwycięzcy (jeśli gra zakończona)

### Enums

**`com.tbs.enums.PlayerSymbol`** (istniejący)
- `X("x")`, `O("o")`

**`com.tbs.enums.GameStatus`** (istniejący)
- `WAITING`, `IN_PROGRESS`, `FINISHED`, `ABANDONED`, `DRAW`

### Modele domenowe (do stworzenia)

- **`com.tbs.model.Game`** - encja JPA/Hibernate dla tabeli `games`
- **`com.tbs.model.Move`** - encja JPA/Hibernate dla tabeli `moves`
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`

### Wyjątki (do stworzenia)

- **`com.tbs.exception.GameNotFoundException`** - wyjątek dla 404 Not Found
- **`com.tbs.exception.ForbiddenException`** - wyjątek dla 403 Forbidden
- **`com.tbs.exception.InvalidMoveException`** - wyjątek dla 422 Unprocessable Entity
- **`com.tbs.exception.GameNotInProgressException`** - wyjątek dla 400 Bad Request
- **`com.tbs.exception.MoveValidationException`** - wyjątek dla 422 Unprocessable Entity

### Serwisy (do stworzenia)

- **`com.tbs.service.MoveService`** - serwis zarządzający ruchami
  - Metody: `createMove()`, `validateMove()`, `checkWinCondition()`, `checkDrawCondition()`, `generateBoardState()`
- **`com.tbs.service.GameService`** - serwis zarządzający grami
  - Metody: `getGameById()`, `isUserParticipant()`, `isCurrentPlayer()`, `updateGameAfterMove()`
- **`com.tbs.service.AuthenticationService`** - wyodrębnianie bieżącego użytkownika
- **`com.tbs.service.BotService`** - generowanie ruchów bota (dla vs_bot)

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**201 Created** - Ruch wykonany pomyślnie

**Przykład odpowiedzi:**
```json
{
  "moveId": 123,
  "gameId": 42,
  "row": 0,
  "col": 1,
  "playerSymbol": "x",
  "moveOrder": 5,
  "createdAt": "2024-01-20T15:30:00Z",
  "boardState": {
    "state": [
      ["x", "x", ""],
      ["o", "", ""],
      ["", "", ""]
    ]
  },
  "gameStatus": "in_progress",
  "winner": null
}
```

**Przykład odpowiedzi (wygrana):**
```json
{
  "moveId": 125,
  "gameId": 42,
  "row": 0,
  "col": 2,
  "playerSymbol": "x",
  "moveOrder": 7,
  "createdAt": "2024-01-20T15:32:00Z",
  "boardState": {
    "state": [
      ["x", "x", "x"],
      ["o", "o", ""],
      ["", "", ""]
    ]
  },
  "gameStatus": "finished",
  "winner": {
    "userId": 10,
    "username": "player1"
  }
}
```

### Kody statusu błędów

**400 Bad Request** - Gra nie jest w statusie in_progress
```json
{
  "error": {
    "code": "GAME_NOT_IN_PROGRESS",
    "message": "Game is not in progress",
    "details": "Game status must be 'in_progress' to make a move"
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

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

**403 Forbidden** - Nie jesteś uczestnikiem gry lub nie jesteś aktualnym graczem
```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "You are not the current player or not a participant",
    "details": "Only the current player can make a move"
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**404 Not Found** - Gra nie znaleziona
```json
{
  "error": {
    "code": "GAME_NOT_FOUND",
    "message": "Game not found",
    "details": "Game with id 42 does not exist"
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**422 Unprocessable Entity** - Nieprawidłowy ruch (pozycja zajęta lub poza granicami)
```json
{
  "error": {
    "code": "INVALID_MOVE",
    "message": "Invalid move",
    "details": "Position (0,1) is already occupied or out of board bounds"
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**422 Unprocessable Entity** - Błędy walidacji Bean Validation
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": {
      "row": "Row must be non-negative",
      "col": "Col must be non-negative",
      "playerSymbol": "Player symbol is required"
    }
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

**500 Internal Server Error** - Nieoczekiwany błąd serwera

## 5. Przepływ danych

### Sekwencja operacji

1. **Odebranie żądania HTTP POST /api/games/{gameId}/moves**
   - Walidacja formatu JSON
   - Parsowanie `CreateMoveRequest` DTO
   - Wyodrębnienie `gameId` z URL

2. **Walidacja danych wejściowych (Bean Validation)**
   - Walidacja adnotacji Bean Validation na `CreateMoveRequest`
   - Sprawdzenie `row >= 0` (@Min(0))
   - Sprawdzenie `col >= 0` (@Min(0))
   - Sprawdzenie `playerSymbol != null` (@NotNull)
   - Jeśli błędy walidacji → 422 Unprocessable Entity

3. **Uwierzytelnienie użytkownika**
   - Wyodrębnienie tokenu JWT z nagłówka `Authorization`
   - Walidacja tokenu JWT przez Spring Security
   - Pobranie bieżącego użytkownika z tokenu
   - Jeśli brak tokenu lub nieprawidłowy → 401 Unauthorized

4. **Pobranie gry z bazy danych**
   - Zapytanie: `SELECT * FROM games WHERE id = ?`
   - Jeśli gra nie istnieje → 404 Not Found
   - Sprawdzenie czy użytkownik jest uczestnikiem (player1_id lub player2_id)
   - Jeśli użytkownik nie jest uczestnikiem → 403 Forbidden

5. **Walidacja stanu gry**
   - Sprawdzenie czy `game.status == 'in_progress'`
   - Jeśli gra nie jest w statusie in_progress → 400 Bad Request
   - Sprawdzenie czy użytkownik jest aktualnym graczem (`game.current_player_symbol` == `request.playerSymbol`)
   - Jeśli użytkownik nie jest aktualnym graczem → 403 Forbidden

6. **Walidacja ruchu**
   - Sprawdzenie granic planszy: `row < boardSize` i `col < boardSize`
   - Jeśli ruch poza granicami → 422 Unprocessable Entity
   - Sprawdzenie czy pozycja nie jest zajęta: wywołanie funkcji bazy danych `is_move_valid(gameId, row, col)`
   - Jeśli pozycja zajęta → 422 Unprocessable Entity
   - Sprawdzenie czy `request.playerSymbol` odpowiada `game.current_player_symbol`
   - Jeśli nieprawidłowy symbol → 422 Unprocessable Entity

7. **Obliczenie kolejności ruchu**
   - Pobranie liczby istniejących ruchów: `SELECT COUNT(*) FROM moves WHERE game_id = ?`
   - `moveOrder = count + 1`

8. **Utworzenie rekordu ruchu w bazie danych**
   - Wstawienie do tabeli `moves`:
     ```sql
     INSERT INTO moves (game_id, player_id, row, col, player_symbol, move_order, created_at)
     VALUES (?, ?, ?, ?, ?, ?, NOW())
     ```
   - `player_id` = ID bieżącego użytkownika
   - Trigger automatycznie aktualizuje `games.last_move_at`

9. **Sprawdzenie warunków zakończenia gry**
   - Wygenerowanie stanu planszy: wywołanie funkcji `generate_board_state(gameId)`
   - Sprawdzenie warunku wygranej (linie poziome, pionowe, przekątne)
   - Jeśli znaleziono wygraną → ustawienie `game.status = 'finished'`, `game.winner_id = currentUserId`, `game.finished_at = NOW()`
   - Jeśli nie wygrana, sprawdzenie warunku remisu (pełna plansza)
   - Jeśli gra nadal trwa → aktualizacja `game.current_player_symbol` (zamiana X ↔ O)

10. **Aktualizacja gry w bazie danych**
    - Aktualizacja rekordu `games` (status, current_player_symbol, winner_id, finished_at)
    - Trigger automatycznie aktualizuje statystyki użytkowników (jeśli status = 'finished')

11. **Wykonanie ruchu bota (tylko dla vs_bot, jeśli gra nadal trwa)**
    - Jeśli `game.gameType == 'vs_bot'` i `game.status == 'in_progress'`:
      - Wywołanie wewnętrzne `POST /api/games/{gameId}/bot-move`
      - Asynchroniczne lub synchroniczne wykonanie ruchu bota

12. **Generowanie odpowiedzi**
    - Mapowanie encji `Move` → `CreateMoveResponse` DTO
    - Generowanie `BoardState` z funkcji bazy danych
    - Mapowanie `Game` → `GameStatus`, `WinnerInfo` (jeśli istnieje)

13. **Zwrócenie odpowiedzi HTTP 201 Created**
    - Serializacja `CreateMoveResponse` do JSON

### Integracja z bazą danych

**Tabela: `games`**
- SELECT rekord na podstawie `id`
- UPDATE: `status`, `current_player_symbol`, `winner_id`, `finished_at`, `last_move_at`, `updated_at`

**Tabela: `moves`**
- INSERT: tworzenie nowego rekordu ruchu
- SELECT COUNT: zliczanie istniejących ruchów
- SELECT: pobieranie historii ruchów dla generowania stanu planszy

**Funkcje bazy danych:**
- `is_move_valid(game_id, row, col)` - walidacja ruchu
- `generate_board_state(game_id)` - generowanie stanu planszy
- Trigger: `update_game_last_move_timestamp` - automatyczna aktualizacja `games.last_move_at`
- Trigger: `update_user_stats_on_game_finished` - automatyczna aktualizacja statystyk

## 6. Względy bezpieczeństwa

### Uwierzytelnianie i autoryzacja

**Wymagane uwierzytelnienie:**
- Endpoint wymaga tokenu JWT w nagłówku `Authorization: Bearer <token>`
- Token JWT jest walidowany przez Spring Security

**Autoryzacja:**
- Użytkownik musi być uczestnikiem gry (`player1_id` lub `player2_id`)
- Użytkownik musi być aktualnym graczem (`game.current_player_symbol` == `request.playerSymbol`)
- Tylko aktualny gracz może wykonać ruch

**Ochrona przed:**
- Wykonywaniem ruchów w nie swoich grach → 403 Forbidden
- Wykonywaniem ruchów poza swoją turą → 403 Forbidden
- Manipulacją `playerSymbol` w żądaniu → walidacja zgodności z `game.current_player_symbol`

### Walidacja danych wejściowych

**Bean Validation:**
- `@Min(0)` na `row` i `col` - zapobiega ujemnym wartościom
- `@NotNull` na `playerSymbol` - zapobiega null

**Walidacja biznesowa:**
- Sprawdzenie granic planszy
- Sprawdzenie czy pozycja nie jest zajęta
- Sprawdzenie czy `playerSymbol` odpowiada `game.current_player_symbol`
- Sprawdzenie czy gra jest w statusie `in_progress`

### Ochrona przed atakami

**Race Conditions:**
- Wykorzystanie transakcji bazy danych dla atomowości operacji
- Optymistyczne blokowanie (`@Version` w encji `Game`)
- Sprawdzenie warunków wewnątrz transakcji

**Replay Attacks:**
- Każdy ruch jest zapisywany z `moveOrder`, co zapobiega duplikatom
- Unique constraint na `(game_id, row, col)` zapobiega podwójnym ruchom

**Rate Limiting:**
- Opcjonalnie: rate limiting na endpoint (10 ruchów na minutę na gracza)

## 7. Obsługa błędów

### Scenariusze błędów

| Scenariusz | Kod HTTP | Wyjątek | Komunikat |
|------------|----------|---------|-----------|
| Brak tokenu JWT | 401 | `UnauthorizedException` | "Authentication required" |
| Gra nie istnieje | 404 | `GameNotFoundException` | "Game not found" |
| Użytkownik nie jest uczestnikiem | 403 | `ForbiddenException` | "You are not a participant of this game" |
| Użytkownik nie jest aktualnym graczem | 403 | `ForbiddenException` | "You are not the current player" |
| Gra nie jest w statusie in_progress | 400 | `GameNotInProgressException` | "Game is not in progress" |
| Ruch poza granicami planszy | 422 | `InvalidMoveException` | "Move is out of board bounds" |
| Pozycja zajęta | 422 | `InvalidMoveException` | "Position is already occupied" |
| Nieprawidłowy playerSymbol | 422 | `InvalidMoveException` | "Player symbol does not match current player" |
| Błędy walidacji Bean Validation | 422 | `MoveValidationException` | "Validation failed" |
| Błąd bazy danych | 500 | `InternalServerException` | "An unexpected error occurred" |

### Centralna obsługa wyjątków

**`@ControllerAdvice` dla globalnej obsługi wyjątków:**
- Mapowanie wyjątków na odpowiednie kody HTTP
- Generowanie konsystentnych odpowiedzi błędów (`ApiErrorResponse`)

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Indeksy:**
- Indeks na `moves.game_id` dla szybkiego pobierania ruchów gry
- Indeks na `moves.game_id, move_order` dla sortowania
- Indeks na `moves.game_id, row, col` dla unique constraint i walidacji
- Indeks na `games.id, status` dla szybkiego sprawdzania stanu gry

**Zapytania:**
- Użycie funkcji bazy danych `generate_board_state()` zamiast pobierania wszystkich ruchów w Javie
- Optymalizacja zapytań przez `EXPLAIN ANALYZE`

**Caching:**
- Opcjonalnie: cache stanu planszy w Redis dla aktywnych gier (TTL: 60 sekund)

### Transakcje i blokowanie

**Strategia transakcji:**
- `@Transactional` dla POST endpointów (INSERT + UPDATE)
- Izolacja transakcji: `READ_COMMITTED`
- Timeout transakcji: 5 sekund

**Blokowanie:**
- Optymistyczne blokowanie (`@Version` w `Game`) dla konfliktów równoległych ruchów

### Rate Limiting

**Rate Limiting:**
- Limit: 10 ruchów na minutę na gracza (Redis-based)
- Po przekroczeniu: 429 Too Many Requests

## 9. Etapy wdrożenia

1. **Utworzenie modeli domenowych (JPA Entities)**
   - `com.tbs.model.Game` - encja JPA dla tabeli `games`
   - `com.tbs.model.Move` - encja JPA dla tabeli `moves`
   - `com.tbs.model.User` - encja JPA dla tabeli `users` (jeśli nie istnieje)

2. **Utworzenie repozytoriów (JPA Repositories)**
   - `com.tbs.repository.GameRepository` extends `JpaRepository<Game, Long>`
   - `com.tbs.repository.MoveRepository` extends `JpaRepository<Move, Long>`
   - `com.tbs.repository.UserRepository` extends `JpaRepository<User, Long>` (jeśli nie istnieje)

3. **Utworzenie wyjątków**
   - `com.tbs.exception.GameNotFoundException`
   - `com.tbs.exception.ForbiddenException`
   - `com.tbs.exception.InvalidMoveException`
   - `com.tbs.exception.GameNotInProgressException`
   - `com.tbs.exception.MoveValidationException`

4. **Utworzenie serwisu `MoveService`**
   - Metoda: `CreateMoveResponse createMove(Long gameId, CreateMoveRequest request, Long userId)`
   - Metody pomocnicze: `validateMove()`, `checkWinCondition()`, `checkDrawCondition()`, `generateBoardState()`

5. **Utworzenie serwisu `GameService`** (jeśli nie istnieje)
   - Metody: `getGameById()`, `isUserParticipant()`, `isCurrentPlayer()`, `updateGameAfterMove()`

6. **Utworzenie kontrolera `MoveController`**
   - Endpoint: `@PostMapping` dla POST /api/games/{gameId}/moves
   - Dokumentacja Swagger: `@Operation`, `@ApiResponse`

7. **Konfiguracja Spring Security**
   - Dodanie endpointu do konfiguracji bezpieczeństwa

8. **Implementacja obsługi błędów**
   - Utworzenie `GlobalExceptionHandler` (jeśli nie istnieje)
   - Mapowanie wyjątków na kody HTTP

9. **Unit testy dla `MoveService`**
   - Test: `createMove()` - pomyślne wykonanie ruchu
   - Test: `createMove()` - różne scenariusze błędów

10. **Testy integracyjne dla `MoveController`**
    - Test: POST /api/games/{gameId}/moves - pomyślne wykonanie ruchu (201)
    - Test: POST /api/games/{gameId}/moves - różne scenariusze błędów

11. **Dokumentacja API**
    - Konfiguracja Swagger/OpenAPI
    - Aktualizacja dokumentacji

