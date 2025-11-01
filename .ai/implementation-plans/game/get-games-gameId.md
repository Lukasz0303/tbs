# API Endpoint Implementation Plan: GET /api/games/{gameId}

## 1. Przegląd punktu końcowego

**GET /api/games/{gameId}** to endpoint służący do pobrania szczegółowych informacji o grze. Endpoint wymaga uwierzytelnienia i zwraca pełne dane gry tylko dla uczestników (player1 lub player2).

Endpoint zwraca:
- Szczegółowe informacje o grze (typ, rozmiar planszy, status, gracze, zwycięzca)
- Pełny stan planszy (generowany dynamicznie z historii ruchów)
- Historię wszystkich ruchów w grze
- Statystyki gry (liczba ruchów, daty)

Kluczowe zastosowania:
- Wyświetlenie szczegółów gry podczas rozgrywki
- Odświeżenie stanu gry po przerwaniu połączenia
- Przeglądanie historii zakończonych gier
- Odtworzenie stanu planszy na podstawie historii ruchów

## 2. Szczegóły żądania

### Metoda HTTP
- **GET** - operacja tylko do odczytu, idempotentna

### Struktura URL
```
GET /api/games/{gameId}
```

### Nagłówki żądania

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT wydany po poprawnym logowaniu/rejestracji

**Opcjonalne:**
- `Accept: application/json` - preferowany format odpowiedzi

### Parametry URL

**Path Variables:**
- `gameId` (Long) - ID gry z tabeli `games.id`

### Query Parameters
- Brak parametrów zapytania

### Request Body
- Brak ciała żądania (metoda GET)

### Przykład żądania
```http
GET /api/games/42 HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Accept: application/json
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
- Brak - metoda GET nie wymaga DTO żądania

#### Response DTO
**`com.tbs.dto.game.GameDetailResponse`** (istniejący)
```java
public record GameDetailResponse(
    long gameId,
    GameType gameType,
    BoardSize boardSize,
    PlayerInfo player1,
    PlayerInfo player2,
    WinnerInfo winner,
    BotDifficulty botDifficulty,
    GameStatus status,
    PlayerSymbol currentPlayerSymbol,
    Instant lastMoveAt,
    Instant createdAt,
    Instant updatedAt,
    Instant finishedAt,
    BoardState boardState,
    int totalMoves,
    List<MoveListItem> moves
) {}
```

**`com.tbs.dto.user.PlayerInfo`** (istniejący)
```java
public record PlayerInfo(
    long userId,
    String username,
    boolean isGuest
) {}
```

**`com.tbs.dto.user.WinnerInfo`** (istniejący)
```java
public record WinnerInfo(
    long userId,
    String username
) {}
```

**`com.tbs.dto.move.MoveListItem`** (istniejący)
```java
public record MoveListItem(
    long moveId,
    int row,
    int col,
    PlayerSymbol playerSymbol,
    int moveOrder,
    Long playerId,
    Instant createdAt
) {}
```

**Uwagi implementacyjne:**
- `gameId` - ID gry z tabeli `games.id`
- `gameType` - Typ gry z `games.game_type`
- `boardSize` - Rozmiar planszy z `games.board_size`
- `player1`, `player2` - Informacje o graczach z JOIN do `users`
- `winner` - Informacje o zwycięzcy (NULL jeśli gra nie zakończona) z `games.winner_id`
- `botDifficulty` - Poziom trudności bota (tylko dla vs_bot) z `games.bot_difficulty`
- `status` - Status gry z `games.status`
- `currentPlayerSymbol` - Symbol aktualnego gracza z `games.current_player_symbol`
- `boardState` - Stan planszy (generowany dynamicznie z funkcji bazy danych)
- `totalMoves` - Liczba ruchów (COUNT z tabeli `moves`)
- `moves` - Lista wszystkich ruchów (SELECT z tabeli `moves` ORDER BY `move_order`)

### Enums

**`com.tbs.enums.GameType`**, **`com.tbs.enums.BoardSize`**, **`com.tbs.enums.BotDifficulty`**, **`com.tbs.enums.GameStatus`**, **`com.tbs.enums.PlayerSymbol`** (istniejące)

### Modele domenowe (do stworzenia)
- **`com.tbs.model.Game`** - encja JPA/Hibernate dla tabeli `games`
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`
- **`com.tbs.model.Move`** - encja JPA/Hibernate dla tabeli `moves`

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.GameNotFoundException`** - wyjątek dla 404 Not Found
- **`com.tbs.exception.ForbiddenException`** - wyjątek dla 403 Forbidden
- **`com.tbs.exception.UnauthorizedException`** - wyjątek dla 401 Unauthorized

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.GameService`** - serwis zarządzający grami
- **`com.tbs.service.BoardStateService`** - generowanie stanu planszy
- **`com.tbs.service.AuthenticationService`** - wyodrębnianie bieżącego użytkownika

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Pomyślne pobranie szczegółów gry

**Przykład odpowiedzi:**
```json
{
  "gameId": 42,
  "gameType": "vs_bot",
  "boardSize": 3,
  "player1": {
    "userId": 123,
    "username": "player1",
    "isGuest": false
  },
  "player2": null,
  "winner": null,
  "botDifficulty": "easy",
  "status": "in_progress",
  "currentPlayerSymbol": "x",
  "lastMoveAt": "2024-01-20T15:32:00Z",
  "createdAt": "2024-01-20T15:30:00Z",
  "updatedAt": "2024-01-20T15:32:00Z",
  "finishedAt": null,
  "boardState": {
    "cells": [
      ["x", null, null],
      [null, "o", null],
      [null, null, "x"]
    ]
  },
  "totalMoves": 3,
  "moves": [
    {
      "moveId": 1,
      "row": 0,
      "col": 0,
      "playerSymbol": "x",
      "moveOrder": 1,
      "playerId": 123,
      "createdAt": "2024-01-20T15:30:00Z"
    },
    {
      "moveId": 2,
      "row": 1,
      "col": 1,
      "playerSymbol": "o",
      "moveOrder": 2,
      "playerId": null,
      "createdAt": "2024-01-20T15:31:00Z"
    },
    {
      "moveId": 3,
      "row": 2,
      "col": 2,
      "playerSymbol": "x",
      "moveOrder": 3,
      "playerId": 123,
      "createdAt": "2024-01-20T15:32:00Z"
    }
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

**403 Forbidden** - Nie jesteś uczestnikiem tej gry
```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "You are not a participant of this game",
    "details": null
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

1. **Odebranie żądania HTTP GET /api/games/{gameId}**
   - Parsowanie `gameId` z path variable
   - Walidacja formatu `gameId` (Long)
   - Weryfikacja tokenu JWT przez Spring Security

2. **Walidacja `gameId`**
   - Sprawdzenie czy `gameId` jest poprawną liczbą
   - Jeśli nieprawidłowy format → 400 Bad Request

3. **Wyodrębnienie bieżącego użytkownika**
   - Z tokenu JWT przez Spring Security
   - Z `SecurityContext.getAuthentication()`
   - Jeśli brak użytkownika → 401 Unauthorized

4. **Pobranie gry z bazy danych**
   - Zapytanie: `SELECT * FROM games WHERE id = ?`
   - Jeśli gra nie istnieje → 404 Not Found

5. **Weryfikacja uczestnictwa**
   - Sprawdzenie czy użytkownik jest `player1_id` lub `player2_id`
   - Jeśli nie jest uczestnikiem → 403 Forbidden

6. **Pobranie informacji o graczach**
   - JOIN do tabeli `users` dla `player1` i `player2`
   - JOIN do tabeli `users` dla `winner` (jeśli gra zakończona)

7. **Pobranie historii ruchów**
   - Zapytanie: `SELECT * FROM moves WHERE game_id = ? ORDER BY move_order ASC`
   - Lista wszystkich ruchów uporządkowana według `move_order`

8. **Generowanie stanu planszy**
   - Użycie funkcji bazy danych `generate_board_state(p_game_id)` (jeśli dostępna)
   - Alternatywnie: generowanie w warstwie aplikacji na podstawie historii ruchów
   - Stan planszy to tablica 2D o rozmiarze `boardSize x boardSize`

9. **Zliczenie liczby ruchów**
   - COUNT z tabeli `moves`: `SELECT COUNT(*) FROM moves WHERE game_id = ?`
   - Alternatywnie: `moves.size()` z pobranych ruchów

10. **Mapowanie do odpowiedzi**
    - Konwersja encji `Game` → `GameDetailResponse` DTO
    - Konwersja encji `User` → `PlayerInfo` DTO
    - Konwersja encji `Move` → `MoveListItem` DTO
    - Dodanie `boardState` (wygenerowany stan planszy)
    - Dodanie `totalMoves` (liczba ruchów)

11. **Zwrócenie odpowiedzi HTTP 200 OK**
    - Serializacja `GameDetailResponse` do JSON

### Integracja z bazą danych

**Tabela: `games`**
- SELECT z JOIN do `users` dla `player1`, `player2`, `winner`
- Zapytanie: `SELECT g.*, p1.*, p2.*, w.* FROM games g LEFT JOIN users p1 ON g.player1_id = p1.id LEFT JOIN users p2 ON g.player2_id = p2.id LEFT JOIN users w ON g.winner_id = w.id WHERE g.id = ?`

**Tabela: `moves`**
- SELECT wszystkich ruchów dla gry: `SELECT * FROM moves WHERE game_id = ? ORDER BY move_order ASC`
- JOIN do `users` dla `player_id` (jeśli ruch gracza, nie bota)

**Funkcja bazy danych:**
- `generate_board_state(p_game_id)` - Generowanie stanu planszy na podstawie historii ruchów
- Zwraca: `TEXT[][]` - tablica 2D reprezentująca planszę

**Indeksy:**
- `idx_games_id` (PK, automatyczny) - szybki dostęp po ID
- `idx_moves_game_id` - dla szybkiego wyszukiwania ruchów w grze
- `idx_moves_game_id_move_order` - dla sortowania ruchów według `move_order`

**Row Level Security (RLS):**
- Polityki RLS mogą automatycznie filtrować gry do uczestników
- Zapytania powinny respektować RLS

### Generowanie stanu planszy

**Strategia:**
- Użycie funkcji bazy danych `generate_board_state(p_game_id)` (jeśli dostępna)
- Alternatywnie: generowanie w warstwie aplikacji na podstawie historii ruchów

**Implementacja w warstwie aplikacji:**
```java
private BoardState generateBoardState(Game game, List<Move> moves) {
    int size = game.getBoardSize().getValue();
    String[][] cells = new String[size][size];
    
    for (Move move : moves) {
        cells[move.getRow()][move.getCol()] = move.getPlayerSymbol().getValue();
    }
    
    return new BoardState(cells);
}
```

**Funkcja bazy danych (opcjonalnie):**
- `generate_board_state(p_game_id)` - generowanie w PostgreSQL
- Zwraca: `TEXT[][]` - tablica 2D

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
- Tylko uczestnicy gry (player1 lub player2) mogą przeglądać szczegóły gry
- Weryfikacja uczestnictwa w warstwie aplikacji lub przez RLS

### Walidacja danych wejściowych

**Path Variable `gameId`:**
- Walidacja formatu (Long)
- Sprawdzenie zakresu (np. > 0)
- Sanityzacja: parsowanie i walidacja przed użyciem w zapytaniu SQL

### Ochrona przed atakami

**SQL Injection:**
- Użycie parametrówzowanych zapytań (JPA/Hibernate automatycznie)
- Walidacja `gameId` przed użyciem w zapytaniu

**Path Traversal:**
- Walidacja formatu `gameId` (tylko liczby)
- Sprawdzenie zakresu

**Information Disclosure:**
- Weryfikacja uczestnictwa przed zwróceniem danych gry
- Tylko uczestnicy mogą zobaczyć szczegóły gry
- RLS w bazie danych zapewnia dodatkową warstwę ochrony

**Rate Limiting:**
- Ograniczenie szybkości dla endpointów wymagających uwierzytelnienia: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Implementacja przez Redis
- Klucz: `rate_limit:games:detail:{userId}`

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Nieprawidłowy format gameId (400 Bad Request)
**Scenariusz:** `gameId` w URL nie jest liczbą
```java
@GetMapping("/{gameId}")
public ResponseEntity<GameDetailResponse> getGameDetail(@PathVariable String gameId) {
    try {
        Long gameIdLong = Long.parseLong(gameId);
    } catch (NumberFormatException e) {
        throw new BadRequestException("Invalid game ID format");
    }
}
```

#### 2. Gra nie znaleziona (404 Not Found)
**Scenariusz:** Gra z `gameId` nie istnieje w bazie danych
```java
Optional<Game> game = gameRepository.findById(gameId);
if (game.isEmpty()) {
    throw new GameNotFoundException("Game not found");
}
```

**Obsługa:**
- Sprawdzenie czy gra istnieje po pobraniu z bazy
- Zwrócenie 404 Not Found z komunikatem "Game not found"

#### 3. Brak dostępu (403 Forbidden)
**Scenariusz:** Użytkownik nie jest uczestnikiem gry
```java
Game game = gameRepository.findById(gameId).orElseThrow();
if (!game.getPlayer1().getId().equals(userId) && 
    (game.getPlayer2() == null || !game.getPlayer2().getId().equals(userId))) {
    throw new ForbiddenException("You are not a participant of this game");
}
```

#### 4. Błąd bazy danych (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z bazą danych, timeout, błąd SQL
```java
@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ApiErrorResponse> handleDataAccessException(DataAccessException e) {
    log.error("Database error while fetching game details", e);
    return ResponseEntity.status(500)
        .body(new ApiErrorResponse(
            new ErrorDetails("INTERNAL_SERVER_ERROR", "Database error occurred", null)
        ));
}
```

### Global Exception Handler

**Struktura:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleGameNotFound(GameNotFoundException e) {
        // 404 handling
    }
    
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException e) {
        // 403 handling
    }
    
    @ExceptionHandler({DataAccessException.class, Exception.class})
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
        // 500 handling
    }
}
```

### Logowanie błędów

**Poziomy logowania:**
- **INFO:** Pomyślne pobranie szczegółów gry (gameId, userId)
- **WARN:** Próba dostępu do nieistniejącej gry lub gry bez uczestnictwa
- **ERROR:** Błędy bazy danych

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, gameId, userId, stack trace (dla błędów)

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Indeksy:**
- Tabela `games` powinna mieć indeks na `id` (PK, automatyczny)
- Tabela `moves` powinna mieć indeks na `game_id` i `move_order` dla szybkiego wyszukiwania i sortowania

**Zapytania:**
- Użycie JOIN zamiast N+1 queries
- Pobranie wszystkich danych w jednym zapytaniu (Game + Users + Moves)
- Użycie `@EntityGraph` jeśli potrzebne lazy loading
- Projektowanie tylko wymaganych kolumn (nie SELECT *)

**Optymalizacja JOIN:**
- Użycie LEFT JOIN dla opcjonalnych danych (player2, winner)
- Użycie JOIN tylko dla wymaganych danych (player1)

**Optymalizacja COUNT:**
- `totalMoves` można pobrać jako `moves.size()` zamiast osobnego zapytania COUNT
- Alternatywnie: użycie subquery w głównym zapytaniu

### Generowanie stanu planszy

**Optymalizacja:**
- Użycie funkcji bazy danych `generate_board_state()` (jeśli dostępna) - wydajniejsze
- Alternatywnie: generowanie w warstwie aplikacji (mniej obciążenia bazy danych dla wielu jednoczesnych żądań)
- Cache'owanie stanu planszy w Redis (opcjonalne, z inwalidacją przy każdym ruchu)

### Cache'owanie

**Redis Cache (opcjonalne):**
- Klucz: `game:detail:{gameId}`
- TTL: 30-60 sekund (dla często zmieniających się danych)
- Strategia: Cache-aside
- Inwalidacja: przy każdym ruchu, zmianie statusu gry

**Korzyści:**
- Redukcja obciążenia bazy danych dla często przeglądanych gier
- Szybsze odpowiedzi dla powtarzających się żądań

**Wyzwania:**
- Inwalidacja cache przy zmianach (nowe ruchy, zmiany statusu)
- Zarządzanie TTL (zbyt długie: stare dane, zbyt krótkie: mało efektywne)

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Klucz: `rate_limit:games:detail:{userId}`

**Korzyści:**
- Zapobieganie nadmiernemu obciążeniu serwera
- Sprawiedliwy podział zasobów

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="GET",endpoint="/api/games/{gameId}",status="200"}` - liczba pomyślnych żądań
- `http_requests_total{method="GET",endpoint="/api/games/{gameId}",status="403"}` - liczba błędów autoryzacji
- `http_requests_total{method="GET",endpoint="/api/games/{gameId}",status="404"}` - liczba błędów 404
- `http_request_duration_seconds{method="GET",endpoint="/api/games/{gameId}"}` - czas odpowiedzi
- `game_detail_queries_total` - liczba zapytań o szczegóły gry

**Alerty:**
- Wysoki wskaźnik błędów 403 (>10% żądań) - możliwe problemy z autoryzacją
- Długi czas odpowiedzi (>1s) - problem z bazą danych lub generowaniem stanu planszy
- Wysoki wskaźnik błędów 500 (>1% żądań) - problem z infrastrukturą

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `GameDetailResponse`, `PlayerInfo`, `WinnerInfo`, `MoveListItem` DTO istnieją
- Sprawdzenie enumów: `GameType`, `BoardSize`, `BotDifficulty`, `GameStatus`, `PlayerSymbol`
- Weryfikacja struktury pakietów

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.model.Game` - encja JPA/Hibernate
- `com.tbs.model.User` - encja JPA/Hibernate
- `com.tbs.model.Move` - encja JPA/Hibernate
- `com.tbs.service.GameService` - serwis zarządzający grami
- `com.tbs.service.BoardStateService` - generowanie stanu planszy
- `com.tbs.exception.GameNotFoundException` - wyjątek dla 404
- `com.tbs.exception.ForbiddenException` - wyjątek dla 403

### Krok 2: Implementacja repozytorium z JOIN

**2.1 Utworzenie GameRepository:**
```java
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    @Query("SELECT g FROM Game g " +
           "LEFT JOIN FETCH g.player1 p1 " +
           "LEFT JOIN FETCH g.player2 p2 " +
           "LEFT JOIN FETCH g.winner w " +
           "WHERE g.id = :gameId")
    Optional<Game> findByIdWithPlayers(@Param("gameId") Long gameId);
}
```

**2.2 Utworzenie MoveRepository:**
```java
@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {
    List<Move> findByGameIdOrderByMoveOrderAsc(Long gameId);
    
    long countByGameId(Long gameId);
}
```

**2.3 Testy repozytorium:**
- Test jednostkowy dla `findByIdWithPlayers`
- Test dla `findByGameIdOrderByMoveOrderAsc`
- Test dla `countByGameId`

### Krok 3: Implementacja serwisu szczegółów gry

**3.1 Utworzenie GameService:**
```java
@Service
@Transactional(readOnly = true)
public class GameService {
    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final BoardStateService boardStateService;
    
    public GameDetailResponse getGameDetail(Long gameId, Long userId) {
        Game game = gameRepository.findByIdWithPlayers(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found"));
        
        if (!game.getPlayer1().getId().equals(userId) && 
            (game.getPlayer2() == null || !game.getPlayer2().getId().equals(userId))) {
            throw new ForbiddenException("You are not a participant of this game");
        }
        
        List<Move> moves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        long totalMoves = moveRepository.countByGameId(gameId);
        BoardState boardState = boardStateService.generateBoardState(game, moves);
        
        return mapToGameDetailResponse(game, moves, boardState, totalMoves);
    }
}
```

**3.2 Utworzenie BoardStateService:**
```java
@Service
public class BoardStateService {
    public BoardState generateBoardState(Game game, List<Move> moves) {
        int size = game.getBoardSize().getValue();
        String[][] cells = new String[size][size];
        
        for (Move move : moves) {
            cells[move.getRow()][move.getCol()] = move.getPlayerSymbol().getValue();
        }
        
        return new BoardState(cells);
    }
}
```

**3.3 Testy serwisu:**
- Test jednostkowy z Mockito dla pomyślnego przypadku (200)
- Test dla przypadku gdy gra nie istnieje (404)
- Test dla przypadku gdy użytkownik nie jest uczestnikiem (403)
- Test dla generowania stanu planszy

### Krok 4: Implementacja kontrolera

**4.1 Utworzenie GameController:**
```java
@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    private final AuthenticationService authenticationService;
    
    @GetMapping("/{gameId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GameDetailResponse> getGameDetail(@PathVariable Long gameId) {
        Long userId = authenticationService.getCurrentUserId();
        GameDetailResponse response = gameService.getGameDetail(gameId, userId);
        return ResponseEntity.ok(response);
    }
}
```

**4.2 Konfiguracja Spring Security:**
- Upewnienie się, że `/api/games/{gameId}` wymaga uwierzytelnienia
- Konfiguracja `SecurityFilterChain` z JWT authentication filter

**4.3 Testy kontrolera:**
- Test integracyjny z `@WebMvcTest` dla pomyślnego przypadku (200)
- Test dla przypadku gdy gra nie istnieje (404)
- Test dla przypadku gdy użytkownik nie jest uczestnikiem (403)
- Test dla przypadku bez tokenu (401)

### Krok 5: Implementacja obsługi błędów

**5.1 Utworzenie global exception handler:**
- Obsługa `GameNotFoundException` (404)
- Obsługa `ForbiddenException` (403)
- Obsługa `DataAccessException` (500)

**5.2 Testy exception handler:**
- Test dla każdego typu wyjątku
- Weryfikacja formatu odpowiedzi błędu

### Krok 6: Konfiguracja Swagger/OpenAPI

**6.1 Dodanie adnotacji Swagger:**
```java
@Operation(
    summary = "Get game details",
    description = "Retrieves detailed information about a game including board state and move history"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Game details retrieved successfully"),
    @ApiResponse(responseCode = "403", description = "You are not a participant of this game"),
    @ApiResponse(responseCode = "404", description = "Game not found")
})
@GetMapping("/{gameId}")
public ResponseEntity<GameDetailResponse> getGameDetail(...) {
    // ...
}
```

### Krok 7: Implementacja cache'owania (opcjonalne)

**7.1 Konfiguracja Redis cache:**
- Dodanie `@EnableCaching` w klasie konfiguracyjnej
- Konfiguracja `RedisCacheManager`

**7.2 Dodanie cache do serwisu:**
```java
@Cacheable(value = "gameDetail", key = "#gameId")
public GameDetailResponse getGameDetail(Long gameId, Long userId) {
    // ...
}
```

**7.3 Inwalidacja cache:**
- Inwalidacja przy każdym ruchu
- Inwalidacja przy zmianie statusu gry

### Krok 8: Testy integracyjne i E2E

**8.1 Testy integracyjne:**
- Test pełnego przepływu z bazą danych
- Test z rzeczywistym tokenem JWT
- Test weryfikujący RLS w bazie danych
- Test generowania stanu planszy

**8.2 Testy E2E (Cypress):**
- Test pobrania szczegółów gry po logowaniu
- Test obsługi błędu 403 dla gry bez uczestnictwa
- Test wyświetlania stanu planszy

### Krok 9: Dokumentacja i code review

**9.1 Dokumentacja:**
- Aktualizacja README z informacjami o endpoincie
- Dokumentacja Swagger/OpenAPI
- Dokumentacja generowania stanu planszy

**9.2 Code review:**
- Sprawdzenie zgodności z zasadami implementacji
- Review bezpieczeństwa
- Weryfikacja obsługi błędów

### Krok 10: Wdrożenie i monitoring

**10.1 Wdrożenie:**
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Test z różnymi stanami gry (in_progress, finished)

**10.2 Monitoring:**
- Konfiguracja metryk Prometheus
- Konfiguracja alertów dla wysokiego wskaźnika błędów
- Monitorowanie czasu odpowiedzi i wydajności zapytań

## 10. Podsumowanie

Plan implementacji endpointu **GET /api/games/{gameId}** obejmuje kompleksowe podejście do wdrożenia z generowaniem stanu planszy i autoryzacją uczestników. Kluczowe aspekty:

- **Bezpieczeństwo:** Uwierzytelnianie JWT, autoryzacja uczestników, ochrona przed nieautoryzowanym dostępem
- **Wydajność:** Optymalizacja zapytań, generowanie stanu planszy, opcjonalne cache'owanie
- **Obsługa błędów:** Centralna obsługa z odpowiednimi kodami statusu
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Dokumentacja:** Swagger/OpenAPI, dokumentacja generowania stanu planszy

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
