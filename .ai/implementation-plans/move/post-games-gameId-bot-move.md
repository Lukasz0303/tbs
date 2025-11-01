# API Endpoint Implementation Plan: POST /api/games/{gameId}/bot-move

> **Status:** ⏳ Do implementacji

## 1. Przegląd punktu końcowego

**POST /api/games/{gameId}/bot-move** to endpoint wewnętrzny służący do wykonywania ruchu bota w grze vs_bot. Endpoint jest wywoływany automatycznie po wykonaniu ruchu gracza w trybie vs_bot, jeśli gra nadal trwa.

Endpoint umożliwia:
- Automatyczne wykonanie ruchu bota po ruchu gracza (dla vs_bot)
- Generowanie ruchu bota na podstawie poziomu trudności (EASY, MEDIUM, HARD)
- Automatyczne wykrywanie wygranej, przegranej lub remisu
- Aktualizację statusu gry po zakończeniu

**Autoryzacja:** Wewnętrzny endpoint (może wymagać tokenu JWT lub specjalnej roli `ROLE_INTERNAL_SERVICE`). Endpoint nie jest eksponowany do klientów zewnętrznych.

## 2. Szczegóły żądania

### Metoda HTTP
- **POST** - operacja tworzenia zasobu (ruch bota)

### Struktura URL
```
POST /api/games/{gameId}/bot-move
```

### Nagłówki żądania

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT dla wewnętrznych wywołań (opcjonalnie)
- `Content-Type: application/json` - format treści żądania

**Uwaga:** Ten endpoint jest wywoływany wewnętrznie po wykonaniu ruchu gracza w trybie vs_bot.

### Parametry URL
- `gameId` (Long, wymagane) - ID gry, w której wykonywany jest ruch bota

### Query Parameters
- Brak parametrów zapytania

### Request Body
- Brak ciała żądania (ruch bota jest generowany automatycznie)

### Przykład żądania
```http
POST /api/games/42/bot-move HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
- Brak - ruch bota jest generowany automatycznie

#### Response DTO
**`com.tbs.dto.move.BotMoveResponse`** (istniejący)
```java
public record BotMoveResponse(
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
- Struktura identyczna z `CreateMoveResponse`
- `player_id` w bazie danych = NULL (ruch bota)
- `playerSymbol` = symbol bota (przeciwny do gracza)

### Enums

**`com.tbs.enums.PlayerSymbol`** (istniejący)
- `X("x")`, `O("o")`

**`com.tbs.enums.GameStatus`** (istniejący)
- `WAITING`, `IN_PROGRESS`, `FINISHED`, `ABANDONED`, `DRAW`

**`com.tbs.enums.BotDifficulty`** (istniejący)
- `EASY("easy")`, `MEDIUM("medium")`, `HARD("hard")`

**`com.tbs.enums.GameType`** (istniejący)
- `VS_BOT("vs_bot")`, `PVP("pvp")`

### Modele domenowe (do stworzenia)

- **`com.tbs.model.Game`** - encja JPA/Hibernate dla tabeli `games`
- **`com.tbs.model.Move`** - encja JPA/Hibernate dla tabeli `moves`
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`

### Wyjątki (do stworzenia)

- **`com.tbs.exception.GameNotFoundException`** - wyjątek dla 404 Not Found
- **`com.tbs.exception.InvalidGameTypeException`** - wyjątek dla 400 Bad Request (gra nie jest vs_bot)
- **`com.tbs.exception.GameNotInProgressException`** - wyjątek dla 400 Bad Request

### Serwisy (do stworzenia)

- **`com.tbs.service.MoveService`** - serwis zarządzający ruchami
  - Metody: `createBotMove()`, `checkWinCondition()`, `checkDrawCondition()`, `generateBoardState()`
- **`com.tbs.service.GameService`** - serwis zarządzający grami
  - Metody: `getGameById()`, `updateGameAfterMove()`
- **`com.tbs.service.BotService`** - serwis generujący ruchy bota
  - Metody: `generateBotMove()`, `generateEasyMove()`, `generateMediumMove()`, `generateHardMove()`

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Ruch bota wykonany pomyślnie

**Przykład odpowiedzi:**
```json
{
  "moveId": 124,
  "gameId": 42,
  "row": 1,
  "col": 2,
  "playerSymbol": "o",
  "moveOrder": 6,
  "createdAt": "2024-01-20T15:31:00Z",
  "boardState": {
    "state": [
      ["x", "x", ""],
      ["o", "", "o"],
      ["", "", ""]
    ]
  },
  "gameStatus": "in_progress",
  "winner": null
}
```

**Przykład odpowiedzi (wygrana bota):**
```json
{
  "moveId": 126,
  "gameId": 42,
  "row": 2,
  "col": 0,
  "playerSymbol": "o",
  "moveOrder": 8,
  "createdAt": "2024-01-20T15:33:00Z",
  "boardState": {
    "state": [
      ["x", "x", ""],
      ["o", "x", "o"],
      ["o", "", ""]
    ]
  },
  "gameStatus": "finished",
  "winner": {
    "userId": null,
    "username": "Bot"
  }
}
```

### Kody statusu błędów

**400 Bad Request** - Gra nie jest w statusie in_progress lub nie jest grą vs_bot
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

**500 Internal Server Error** - Błąd generowania ruchu bota lub błąd serwera
```json
{
  "error": {
    "code": "INTERNAL_SERVER_ERROR",
    "message": "Failed to generate bot move",
    "details": null
  },
  "timestamp": "2024-01-20T15:30:00Z",
  "status": "error"
}
```

## 5. Przepływ danych

### Sekwencja operacji

1. **Odebranie żądania HTTP POST /api/games/{gameId}/bot-move**
   - Wyodrębnienie `gameId` z URL
   - Uwierzytelnienie (wewnętrzne wywołanie lub token JWT)

2. **Pobranie gry z bazy danych**
   - Zapytanie: `SELECT * FROM games WHERE id = ?`
   - Jeśli gra nie istnieje → 404 Not Found
   - Sprawdzenie czy `game.gameType == 'vs_bot'`
   - Jeśli nie vs_bot → 400 Bad Request

3. **Walidacja stanu gry**
   - Sprawdzenie czy `game.status == 'in_progress'`
   - Jeśli gra nie jest w statusie in_progress → 400 Bad Request
   - Sprawdzenie czy aktualnym graczem jest bot (`game.current_player_symbol` != symbol gracza)

4. **Generowanie ruchu bota**
   - Pobranie `game.bot_difficulty` (EASY, MEDIUM, HARD)
   - Pobranie stanu planszy: wywołanie `generate_board_state(gameId)`
   - Wygenerowanie ruchu przez `BotService`:
     - **EASY**: losowy ruch z dostępnych pozycji
     - **MEDIUM**: podstawowa strategia (blokowanie, atak)
     - **HARD**: optymalna strategia (minimax lub podobna)
   - Walidacja wygenerowanego ruchu: `is_move_valid(gameId, row, col)`

5. **Obliczenie kolejności ruchu**
   - Pobranie liczby istniejących ruchów: `SELECT COUNT(*) FROM moves WHERE game_id = ?`
   - `moveOrder = count + 1`

6. **Utworzenie rekordu ruchu w bazie danych**
   - Wstawienie do tabeli `moves`:
     ```sql
     INSERT INTO moves (game_id, player_id, row, col, player_symbol, move_order, created_at)
     VALUES (?, NULL, ?, ?, ?, ?, NOW())
     ```
   - `player_id` = NULL (ruch bota)
   - Trigger automatycznie aktualizuje `games.last_move_at`

7. **Sprawdzenie warunków zakończenia gry**
   - Wygenerowanie stanu planszy: wywołanie funkcji `generate_board_state(gameId)`
   - Sprawdzenie warunku wygranej (linie poziome, pionowe, przekątne)
   - Jeśli znaleziono wygraną → ustawienie `game.status = 'finished'`, `game.winner_id = player1_id` (lub player2_id, w zależności od symbolu bota), `game.finished_at = NOW()`
   - Jeśli nie wygrana, sprawdzenie warunku remisu
   - Jeśli gra nadal trwa → aktualizacja `game.current_player_symbol`

8. **Aktualizacja gry w bazie danych**
   - Aktualizacja rekordu `games` (status, current_player_symbol, winner_id, finished_at)
   - Trigger automatycznie aktualizuje statystyki użytkowników

9. **Generowanie odpowiedzi**
    - Mapowanie encji `Move` → `BotMoveResponse` DTO
    - Generowanie `BoardState` z funkcji bazy danych
    - Mapowanie `Game` → `GameStatus`, `WinnerInfo` (jeśli istnieje)

10. **Zwrócenie odpowiedzi HTTP 200 OK**
    - Serializacja `BotMoveResponse` do JSON

### Integracja z bazą danych

**Tabela: `games`**
- SELECT rekord na podstawie `id`
- UPDATE: `status`, `current_player_symbol`, `winner_id`, `finished_at`, `last_move_at`, `updated_at`

**Tabela: `moves`**
- INSERT: tworzenie nowego rekordu ruchu (z `player_id = NULL`)
- SELECT COUNT: zliczanie istniejących ruchów
- SELECT: pobieranie historii ruchów dla generowania stanu planszy

**Funkcje bazy danych:**
- `is_move_valid(game_id, row, col)` - walidacja ruchu
- `generate_board_state(game_id)` - generowanie stanu planszy
- Trigger: `update_game_last_move_timestamp` - automatyczna aktualizacja `games.last_move_at`
- Trigger: `update_user_stats_on_game_finished` - automatyczna aktualizacja statystyk

## 6. Względy bezpieczeństwa

### Uwierzytelnianie i autoryzacja

**Wewnętrzny endpoint:**
- Endpoint jest wywoływany wewnętrznie po wykonaniu ruchu gracza w trybie vs_bot
- Wymaga tokenu JWT dla bezpieczeństwa (może być wewnętrzny token lub token użytkownika)
- Alternatywnie: endpoint może być chroniony przez Spring Security z określoną rolą (np. `ROLE_INTERNAL_SERVICE`)

**Autoryzacja:**
- Sprawdzenie czy gra jest typu vs_bot
- Sprawdzenie czy gra jest w statusie in_progress

**Ochrona przed:**
- Wywołaniem dla nie vs_bot gry → 400 Bad Request
- Wywołaniem dla zakończonej gry → 400 Bad Request

### Walidacja danych wejściowych

**Walidacja biznesowa:**
- Sprawdzenie czy gra jest typu vs_bot
- Sprawdzenie czy gra jest w statusie in_progress
- Walidacja wygenerowanego ruchu bota (granice planszy, zajęta pozycja)

### Ochrona przed atakami

**Race Conditions:**
- Wykorzystanie transakcji bazy danych dla atomowości operacji
- Optymistyczne blokowanie (`@Version` w encji `Game`)

**Rate Limiting:**
- Endpoint jest wywoływany wewnętrznie, więc rate limiting nie jest konieczny
- Jeśli endpoint jest eksponowany, rate limiting może być rozważony

## 7. Obsługa błędów

### Scenariusze błędów

| Scenariusz | Kod HTTP | Wyjątek | Komunikat |
|------------|----------|---------|-----------|
| Gra nie istnieje | 404 | `GameNotFoundException` | "Game not found" |
| Gra nie jest typu vs_bot | 400 | `InvalidGameTypeException` | "Game is not a vs_bot game" |
| Gra nie jest w statusie in_progress | 400 | `GameNotInProgressException` | "Game is not in progress" |
| Błąd generowania ruchu bota | 500 | `InternalServerException` | "Failed to generate bot move" |
| Błąd bazy danych | 500 | `InternalServerException` | "An unexpected error occurred" |

### Centralna obsługa wyjątków

**`@ControllerAdvice` dla globalnej obsługi wyjątków:**
- Mapowanie wyjątków na odpowiednie kody HTTP
- Generowanie konsystentnych odpowiedzi błędów (`ApiErrorResponse`)

## 8. Rozważania dotyczące wydajności

### Optymalizacja generowania ruchu bota

**Strategie bota:**
- **EASY**: losowy ruch - O(1) czas, O(1) pamięć
- **MEDIUM**: podstawowa strategia - O(n²) czas (sprawdzenie wszystkich pozycji), O(n²) pamięć
- **HARD**: optymalna strategia (minimax dla 3x3) - O(b^d) czas, gdzie b to rozgałęzienie, d to głębokość
  - Dla 3x3: minimax jest wykonalny
  - Dla 4x4 i 5x5: heurystyka z ograniczeniem głębokości

**Caching:**
- Opcjonalnie: cache stanu planszy w Redis dla szybkiego dostępu

**Timeouty:**
- Timeout generowania ruchu bota: 3 sekundy (dla HARD difficulty)
- Po przekroczeniu timeoutu: wygenerowanie ruchu przez strategię MEDIUM jako fallback

### Optymalizacja zapytań do bazy danych

**Indeksy:**
- Indeks na `moves.game_id` dla szybkiego pobierania ruchów gry
- Indeks na `games.id, game_type, status` dla szybkiego sprawdzania warunków

**Zapytania:**
- Użycie funkcji bazy danych `generate_board_state()` zamiast pobierania wszystkich ruchów w Javie

### Transakcje i blokowanie

**Strategia transakcji:**
- `@Transactional` dla POST endpointów (INSERT + UPDATE)
- Izolacja transakcji: `READ_COMMITTED`
- Timeout transakcji: 5 sekund

## 9. Etapy wdrożenia

1. **Utworzenie modeli domenowych (JPA Entities)**
   - `com.tbs.model.Game` - encja JPA dla tabeli `games`
   - `com.tbs.model.Move` - encja JPA dla tabeli `moves`

2. **Utworzenie repozytoriów (JPA Repositories)**
   - `com.tbs.repository.GameRepository` extends `JpaRepository<Game, Long>`
   - `com.tbs.repository.MoveRepository` extends `JpaRepository<Move, Long>`

3. **Utworzenie wyjątków**
   - `com.tbs.exception.GameNotFoundException`
   - `com.tbs.exception.InvalidGameTypeException`
   - `com.tbs.exception.GameNotInProgressException`

4. **Utworzenie serwisu `BotService`**
   - Metoda: `MovePosition generateBotMove(BoardState boardState, BotDifficulty difficulty, PlayerSymbol botSymbol, int boardSize)`
   - Metody pomocnicze: `generateEasyMove()`, `generateMediumMove()`, `generateHardMove()`

5. **Utworzenie serwisu `MoveService`**
   - Metoda: `BotMoveResponse createBotMove(Long gameId)`
   - Metody pomocnicze: `checkWinCondition()`, `checkDrawCondition()`, `generateBoardState()`

6. **Utworzenie kontrolera `MoveController`**
   - Endpoint: `@PostMapping("/bot-move")` dla POST /api/games/{gameId}/bot-move
   - Dokumentacja Swagger: `@Operation`, `@ApiResponse`

7. **Konfiguracja Spring Security**
   - Dodanie endpointu do konfiguracji bezpieczeństwa (wewnętrzny endpoint)

8. **Implementacja obsługi błędów**
   - Utworzenie `GlobalExceptionHandler` (jeśli nie istnieje)
   - Mapowanie wyjątków na kody HTTP

9. **Unit testy dla `BotService`**
   - Test: `generateEasyMove()` - losowy ruch z dostępnych pozycji
   - Test: `generateMediumMove()` - blokowanie przeciwnika
   - Test: `generateMediumMove()` - atak jeśli możliwe
   - Test: `generateHardMove()` - optymalna strategia (dla 3x3)

10. **Unit testy dla `MoveService`**
    - Test: `createBotMove()` - pomyślne wykonanie ruchu bota
    - Test: `createBotMove()` - różne scenariusze błędów

11. **Testy integracyjne dla `MoveController`**
    - Test: POST /api/games/{gameId}/bot-move - pomyślne wykonanie ruchu bota (200)
    - Test: POST /api/games/{gameId}/bot-move - różne scenariusze błędów

12. **Dokumentacja API**
    - Konfiguracja Swagger/OpenAPI
    - Aktualizacja dokumentacji

### Uwagi implementacyjne

**Strategie bota AI:**

**EASY (łatwy):**
- Losowy ruch z dostępnych pozycji
- Implementacja: `Collections.shuffle(availablePositions)`, wybór pierwszego

**MEDIUM (średni):**
- Priorytety: 1) atak (wygrana), 2) blokowanie przeciwnika, 3) losowy ruch
- Implementacja:
  1. Sprawdzenie wszystkich dostępnych pozycji, czy któryś da wygraną
  2. Jeśli nie, sprawdzenie wszystkich dostępnych pozycji, czy któryś blokuje wygraną przeciwnika
  3. Jeśli nie, losowy ruch

**HARD (trudny):**
- Dla 3x3: algorytm minimax (opcjonalnie)
- Dla 4x4 i 5x5: zaawansowana heurystyka:
  - Priorytet centrum (dla plansz nieparzystych)
  - Priorytet rogów
  - Priorytet blokowania przeciwnika
  - Sprawdzenie wszystkich możliwych wygranych i blokowanie

