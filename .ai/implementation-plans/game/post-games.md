# API Endpoint Implementation Plan: POST /api/games

## 1. Przegląd punktu końcowego

**POST /api/games** to endpoint służący do utworzenia nowej gry (vs_bot lub pvp). Endpoint wymaga uwierzytelnienia i pozwala zalogowanemu użytkownikowi utworzyć grę z botem AI lub dołączyć do kolejki PvP.

Endpoint obsługuje dwa typy gier:
- **vs_bot**: Gra z botem AI (wymaga `botDifficulty`, `player2_id` = NULL)
- **pvp**: Gra z innym graczem (wymaga matchmakingu lub wyzwania konkretnego gracza, `botDifficulty` = NULL)

Kluczowe zastosowania:
- Rozpoczęcie gry z botem AI (3 poziomy trudności)
- Utworzenie gry PvP (dla matchmakingu lub wyzwania)
- Inicjalizacja planszy i stanu gry
- Generowanie początkowego stanu planszy

## 2. Szczegóły żądania

### Metoda HTTP
- **POST** - operacja tworzenia zasobu

### Struktura URL
```
POST /api/games
```

### Nagłówki żądania

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT wydany po poprawnym logowaniu/rejestracji
- `Content-Type: application/json` - format treści żądania

**Opcjonalne:**
- `Accept: application/json` - preferowany format odpowiedzi

### Parametry URL
- Brak parametrów URL

### Query Parameters
- Brak parametrów zapytania

### Request Body

**`CreateGameRequest`** DTO:
```json
{
  "gameType": "vs_bot",
  "boardSize": 3,
  "botDifficulty": "easy"
}
```

**Walidacja:**
- `gameType`: Wymagane (@NotNull), enum: `VS_BOT` lub `PVP`
- `boardSize`: Wymagane (@NotNull), enum: `THREE` (3), `FOUR` (4), `FIVE` (5)
- `botDifficulty`: Opcjonalne, enum: `EASY`, `MEDIUM`, `HARD` (wymagane jeśli `gameType = VS_BOT`)

**Reguły biznesowe:**
- Dla vs_bot: `botDifficulty` jest wymagane, `player2_id` = NULL
- Dla pvp: `botDifficulty` = NULL, `player2_id` zostanie przypisane później (matchmaking/wyzwanie)

### Przykład żądania
```http
POST /api/games HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
Accept: application/json

{
  "gameType": "vs_bot",
  "boardSize": 3,
  "botDifficulty": "easy"
}
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
**`com.tbs.dto.game.CreateGameRequest`** (istniejący)
```java
public record CreateGameRequest(
    @NotNull(message = "Game type is required")
    GameType gameType,
    
    @NotNull(message = "Board size is required")
    BoardSize boardSize,
    
    BotDifficulty botDifficulty
) {}
```

#### Response DTO
**`com.tbs.dto.game.CreateGameResponse`** (istniejący)
```java
public record CreateGameResponse(
    long gameId,
    GameType gameType,
    BoardSize boardSize,
    long player1Id,
    Long player2Id,
    BotDifficulty botDifficulty,
    GameStatus status,
    PlayerSymbol currentPlayerSymbol,
    Instant createdAt,
    BoardState boardState
) {}
```

**Uwagi implementacyjne:**
- `gameId` - ID gry z tabeli `games.id`
- `gameType` - Typ gry z `games.game_type`
- `boardSize` - Rozmiar planszy z `games.board_size`
- `player1Id` - ID gracza 1 (twórca gry) z `games.player1_id`
- `player2Id` - ID gracza 2 (NULL dla vs_bot, przypisane dla pvp) z `games.player2_id`
- `botDifficulty` - Poziom trudności bota (tylko dla vs_bot) z `games.bot_difficulty`
- `status` - Status gry (domyślnie `WAITING`) z `games.status`
- `currentPlayerSymbol` - Symbol aktualnego gracza (NULL dla nowej gry) z `games.current_player_symbol`
- `createdAt` - Data utworzenia z `games.created_at`
- `boardState` - Stan planszy (generowany dynamicznie z funkcji bazy danych lub w warstwie aplikacji)

### Enums

**`com.tbs.enums.GameType`** (istniejący)
- `VS_BOT` - Gra z botem AI
- `PVP` - Gra z innym graczem

**`com.tbs.enums.BoardSize`** (istniejący)
- `THREE` - Plansza 3x3
- `FOUR` - Plansza 4x4
- `FIVE` - Plansza 5x5

**`com.tbs.enums.BotDifficulty`** (istniejący)
- `EASY` - Łatwy poziom (+100 punktów za wygraną)
- `MEDIUM` - Średni poziom (+500 punktów za wygraną)
- `HARD` - Trudny poziom (+1000 punktów za wygraną)

**`com.tbs.enums.GameStatus`** (istniejący)
- `WAITING` - Gra oczekuje na rozpoczęcie
- `IN_PROGRESS` - Gra w toku
- `FINISHED` - Gra zakończona (wygrał gracz)
- `ABANDONED` - Gra porzucona
- `DRAW` - Remis

**`com.tbs.enums.PlayerSymbol`** (istniejący)
- `X` - Symbol gracza X
- `O` - Symbol gracza O

### Modele domenowe (do stworzenia)
- **`com.tbs.model.Game`** - encja JPA/Hibernate dla tabeli `games`
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.BadRequestException`** - wyjątek dla 400 Bad Request
- **`com.tbs.exception.ValidationException`** - wyjątek dla 422 Unprocessable Entity
- **`com.tbs.exception.UnauthorizedException`** - wyjątek dla 401 Unauthorized

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.GameService`** - serwis zarządzający grami
- **`com.tbs.service.BoardStateService`** - generowanie stanu planszy
- **`com.tbs.service.AuthenticationService`** - wyodrębnianie bieżącego użytkownika

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**201 Created** - Gra utworzona pomyślnie

**Przykład odpowiedzi dla vs_bot:**
```json
{
  "gameId": 42,
  "gameType": "vs_bot",
  "boardSize": 3,
  "player1Id": 123,
  "player2Id": null,
  "botDifficulty": "easy",
  "status": "waiting",
  "currentPlayerSymbol": null,
  "createdAt": "2024-01-20T15:30:00Z",
  "boardState": {
    "cells": [
      [null, null, null],
      [null, null, null],
      [null, null, null]
    ]
  }
}
```

**Przykład odpowiedzi dla pvp:**
```json
{
  "gameId": 43,
  "gameType": "pvp",
  "boardSize": 3,
  "player1Id": 123,
  "player2Id": null,
  "botDifficulty": null,
  "status": "waiting",
  "currentPlayerSymbol": null,
  "createdAt": "2024-01-20T15:30:00Z",
  "boardState": {
    "cells": [
      [null, null, null],
      [null, null, null],
      [null, null, null]
    ]
  }
}
```

### Kody statusu błędów

**400 Bad Request** - Nieprawidłowe parametry gry
```json
{
  "error": {
    "code": "BAD_REQUEST",
    "message": "Invalid game parameters",
    "details": {
      "reason": "botDifficulty is required for vs_bot games"
    }
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

**422 Unprocessable Entity** - Błędy walidacji Bean Validation
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": {
      "gameType": "Game type is required",
      "boardSize": "Board size is required"
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

1. **Odebranie żądania HTTP POST /api/games**
   - Walidacja formatu JSON
   - Parsowanie `CreateGameRequest` DTO
   - Weryfikacja tokenu JWT przez Spring Security

2. **Walidacja danych wejściowych (Bean Validation)**
   - Walidacja adnotacji Bean Validation na `CreateGameRequest`
   - Sprawdzenie obecności `gameType` (@NotNull)
   - Sprawdzenie obecności `boardSize` (@NotNull)
   - Jeśli błędy walidacji → 422 Unprocessable Entity

3. **Walidacja reguł biznesowych**
   - Dla vs_bot: sprawdzenie czy `botDifficulty` jest podane
     - Jeśli brak → 400 Bad Request
   - Dla pvp: sprawdzenie czy `botDifficulty` jest NULL
     - Jeśli podane → 400 Bad Request
   - Jeśli nieprawidłowe parametry → 400 Bad Request

4. **Wyodrębnienie bieżącego użytkownika**
   - Z tokenu JWT przez Spring Security
   - Z `SecurityContext.getAuthentication()`
   - Jeśli brak użytkownika → 401 Unauthorized

5. **Utworzenie nowej gry w bazie danych**
   - Wstawienie rekordu do tabeli `games`:
     - `game_type` = z żądania
     - `board_size` = z żądania
     - `player1_id` = bieżący użytkownik
     - `player2_id` = NULL (dla vs_bot i nowej pvp)
     - `bot_difficulty` = z żądania (dla vs_bot) lub NULL (dla pvp)
     - `status` = 'waiting'
     - `current_player_symbol` = NULL
     - `winner_id` = NULL
     - `last_move_at` = NULL
     - `created_at` = NOW()
     - `updated_at` = NOW()
     - `finished_at` = NULL
   - Jeśli błąd bazy danych → 500 Internal Server Error

6. **Generowanie początkowego stanu planszy**
   - Inicjalizacja pustej planszy o rozmiarze `boardSize x boardSize`
   - Wszystkie komórki = null (puste)
   - Albo użycie funkcji bazy danych `generate_board_state(p_game_id)` (jeśli dostępna)
   - Albo generowanie w warstwie aplikacji

7. **Dla vs_bot: automatyczne rozpoczęcie gry (opcjonalne)**
   - Zmiana statusu z `waiting` na `in_progress`
   - Ustawienie `current_player_symbol` = 'x' (gracz zaczyna)
   - Alternatywnie: rozpoczęcie gry przy pierwszym ruchu

8. **Mapowanie do odpowiedzi**
   - Konwersja encji `Game` → `CreateGameResponse` DTO
   - Dodanie `boardState` (pusty stan początkowy)
   - Ustawienie `currentPlayerSymbol` = null (lub 'x' jeśli gra rozpoczęta)

9. **Zwrócenie odpowiedzi HTTP 201 Created**
   - Serializacja `CreateGameResponse` do JSON
   - Ustawienie nagłówka `Location: /api/games/{gameId}` (opcjonalne)

### Integracja z bazą danych

**Tabela: `games`**
- INSERT nowego rekordu
- Kolumny:
  - `game_type` (game_type_enum)
  - `board_size` (SMALLINT: 3, 4, 5)
  - `player1_id` (BIGINT) → bieżący użytkownik
  - `player2_id` (BIGINT) → NULL
  - `bot_difficulty` (bot_difficulty_enum) → z żądania (vs_bot) lub NULL (pvp)
  - `status` (game_status_enum) → 'waiting'
  - `current_player_symbol` (player_symbol_enum) → NULL
  - `winner_id` (BIGINT) → NULL
  - `last_move_at` (TIMESTAMP) → NULL
  - `created_at` (TIMESTAMP) → NOW()
  - `updated_at` (TIMESTAMP) → NOW()
  - `finished_at` (TIMESTAMP) → NULL

**Ograniczenia bazy danych (CHECK constraints):**
- `games_vs_bot_check`: vs_bot wymaga `bot_difficulty IS NOT NULL` i `player2_id IS NULL`
- `games_status_check`: status 'waiting' wymaga `current_player_symbol IS NULL` i `winner_id IS NULL`
- `board_size CHECK`: rozmiar planszy 3, 4 lub 5

**Funkcje bazy danych (opcjonalne):**
- `generate_board_state(p_game_id)`: Generowanie stanu planszy na podstawie historii ruchów
- Dla nowej gry: pusta plansza

### Generowanie stanu planszy

**Strategia:**
- Dla nowej gry: pusty stan planszy (wszystkie komórki = null)
- Generowanie w warstwie aplikacji (Java)
- Alternatywnie: użycie funkcji bazy danych (jeśli dostępna)

**Implementacja:**
```java
private BoardState generateInitialBoardState(BoardSize boardSize) {
    int size = boardSize.getValue();
    String[][] cells = new String[size][size];
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
            cells[i][j] = null;
        }
    }
    return new BoardState(cells);
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
- Tylko zalogowani użytkownicy mogą tworzyć gry
- Wyjątki dla nieuwierzytelnionych żądań → 401

### Walidacja danych wejściowych

**Bean Validation:**
- `gameType`: Wymagane (@NotNull), enum (tylko VS_BOT, PVP)
- `boardSize`: Wymagane (@NotNull), enum (tylko THREE, FOUR, FIVE)
- `botDifficulty`: Opcjonalne, enum (tylko EASY, MEDIUM, HARD)

**Reguły biznesowe:**
- Walidacja zgodności `gameType` i `botDifficulty`
- Sprawdzenie zakresu `boardSize` (3, 4, 5)

### Ochrona przed atakami

**SQL Injection:**
- Użycie parametrówzowanych zapytań (JPA/Hibernate automatycznie)
- Brak dynamicznego SQL na podstawie danych wejściowych

**Enum Injection:**
- Walidacja enumów przez Bean Validation
- Sprawdzenie wartości przed użyciem

**Rate Limiting:**
- Ograniczenie szybkości dla endpointów wymagających uwierzytelnienia: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Implementacja przez Redis
- Klucz: `rate_limit:games:create:{userId}`

**Ochrona przed spamem:**
- Limit: np. 10 nowych gier na minutę z jednego użytkownika
- Zapobieganie nadmiernemu tworzeniu gier

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Nieprawidłowy format danych (400 Bad Request)
**Scenariusz:** Brak wymaganych pól, nieprawidłowy format JSON
```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ApiErrorResponse> handleBadRequest(HttpMessageNotReadableException e) {
    return ResponseEntity.status(400)
        .body(new ApiErrorResponse(
            new ErrorDetails("BAD_REQUEST", "Invalid request format", null)
        ));
}
```

#### 2. Błędy walidacji Bean Validation (422 Unprocessable Entity)
**Scenariusz:** Naruszenie adnotacji walidacji (@NotNull)
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException e) {
    Map<String, String> errors = new HashMap<>();
    e.getBindingResult().getFieldErrors().forEach(error -> 
        errors.put(error.getField(), error.getDefaultMessage())
    );
    
    return ResponseEntity.status(422)
        .body(new ApiErrorResponse(
            new ErrorDetails("VALIDATION_ERROR", "Validation failed", errors)
        ));
}
```

#### 3. Nieprawidłowe parametry gry (400 Bad Request)
**Scenariusz:** vs_bot bez `botDifficulty` lub pvp z `botDifficulty`
```java
if (request.gameType() == GameType.VS_BOT && request.botDifficulty() == null) {
    throw new BadRequestException("botDifficulty is required for vs_bot games");
}
if (request.gameType() == GameType.PVP && request.botDifficulty() != null) {
    throw new BadRequestException("botDifficulty must be null for pvp games");
}
```

#### 4. Błąd bazy danych (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z bazą danych, constraint violation, timeout
```java
@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ApiErrorResponse> handleDataAccessException(DataAccessException e) {
    log.error("Database error during game creation", e);
    return ResponseEntity.status(500)
        .body(new ApiErrorResponse(
            new ErrorDetails("INTERNAL_SERVER_ERROR", "Database error occurred", null)
        ));
}
```

#### 5. Constraint violation (400 Bad Request)
**Scenariusz:** Naruszenie ograniczeń bazy danych (np. games_vs_bot_check)
```java
try {
    Game game = gameRepository.save(newGame);
} catch (DataIntegrityViolationException e) {
    if (e.getMessage().contains("games_vs_bot_check")) {
        throw new BadRequestException("Invalid game configuration: vs_bot requires botDifficulty");
    }
    throw e;
}
```

### Global Exception Handler

**Struktura:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        // 422 handling
    }
    
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
- **INFO:** Pomyślne utworzenie gry (gameId, gameType, boardSize, userId)
- **WARN:** Próba utworzenia gry z nieprawidłowymi parametrami
- **ERROR:** Błędy bazy danych, constraint violations

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, gameId (jeśli dostępne), userId, stack trace (dla błędów)

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**INSERT operacja:**
- Wstawienie jednego rekordu do tabeli `games`
- Użycie JPA/Hibernate `save()` z `@GeneratedValue` dla auto-increment ID

**Indeksy:**
- Tabela `games` powinna mieć indeksy na:
  - `player1_id` - dla szybkiego wyszukiwania gier gracza
  - `game_type` i `status` - dla matchmakingu
  - `created_at` - dla sortowania

### Generowanie stanu planszy

**Optymalizacja:**
- Dla nowej gry: pusty stan planszy generowany w pamięci (Java)
- Brak zapytań do bazy danych dla pustej planszy
- Funkcja bazy danych `generate_board_state()` używana tylko dla gier z ruchami

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Limit tworzenia: 10 nowych gier/minutę na użytkownika
- Klucz: `rate_limit:games:create:{userId}`

**Korzyści:**
- Zapobieganie spamowi tworzenia gier
- Sprawiedliwy podział zasobów

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="POST",endpoint="/api/games",status="201"}` - liczba utworzonych gier
- `http_requests_total{method="POST",endpoint="/api/games",status="400"}` - liczba błędów walidacji
- `http_request_duration_seconds{method="POST",endpoint="/api/games"}` - czas odpowiedzi
- `games_created_total{game_type="vs_bot|pvp"}` - liczba utworzonych gier według typu

**Alerty:**
- Wysoki wskaźnik błędów 400 (>10% żądań) - problem z walidacją
- Długi czas odpowiedzi (>500ms) - problem z bazą danych
- Wysoki wskaźnik błędów 500 (>1% żądań) - problem z infrastrukturą

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `CreateGameRequest` i `CreateGameResponse` DTO istnieją
- Sprawdzenie enumów: `GameType`, `BoardSize`, `BotDifficulty`, `GameStatus`, `PlayerSymbol`
- Weryfikacja struktury pakietów

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.model.Game` - encja JPA/Hibernate
- `com.tbs.service.GameService` - serwis zarządzający grami
- `com.tbs.service.BoardStateService` - generowanie stanu planszy
- `com.tbs.exception.BadRequestException` - wyjątek dla 400
- `com.tbs.exception.ValidationException` - wyjątek dla 422

### Krok 2: Implementacja encji i repozytorium

**2.1 Utworzenie encji Game:**
```java
@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    private GameType gameType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "board_size", nullable = false)
    private BoardSize boardSize;
    
    @ManyToOne
    @JoinColumn(name = "player1_id", nullable = false)
    private User player1;
    
    @ManyToOne
    @JoinColumn(name = "player2_id")
    private User player2;
    
    // ... pozostałe pola zgodne z schematem bazy danych
}
```

**2.2 Utworzenie repozytorium:**
```java
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    // Zapytania specyficzne dla gier
}
```

### Krok 3: Implementacja serwisu tworzenia gry

**3.1 Utworzenie GameService:**
```java
@Service
@Transactional
public class GameService {
    private final GameRepository gameRepository;
    private final BoardStateService boardStateService;
    private final AuthenticationService authenticationService;
    
    public CreateGameResponse createGame(CreateGameRequest request, Long userId) {
        validateGameRequest(request);
        
        Game game = new Game();
        game.setGameType(request.gameType());
        game.setBoardSize(request.boardSize());
        game.setPlayer1(userRepository.findById(userId).orElseThrow());
        game.setPlayer2(null);
        game.setBotDifficulty(request.botDifficulty());
        game.setStatus(GameStatus.WAITING);
        game.setCurrentPlayerSymbol(null);
        
        Game savedGame = gameRepository.save(game);
        
        BoardState boardState = boardStateService.generateInitialBoardState(request.boardSize());
        
        return mapToCreateGameResponse(savedGame, boardState);
    }
    
    private void validateGameRequest(CreateGameRequest request) {
        if (request.gameType() == GameType.VS_BOT && request.botDifficulty() == null) {
            throw new BadRequestException("botDifficulty is required for vs_bot games");
        }
        if (request.gameType() == GameType.PVP && request.botDifficulty() != null) {
            throw new BadRequestException("botDifficulty must be null for pvp games");
        }
    }
}
```

**3.2 Testy serwisu:**
- Test jednostkowy z Mockito dla tworzenia gry vs_bot (201)
- Test dla tworzenia gry pvp (201)
- Test dla przypadku nieprawidłowych parametrów (400)
- Test dla przypadku vs_bot bez botDifficulty (400)

### Krok 4: Implementacja kontrolera

**4.1 Utworzenie GameController:**
```java
@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    private final AuthenticationService authenticationService;
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreateGameResponse> createGame(@Valid @RequestBody CreateGameRequest request) {
        Long userId = authenticationService.getCurrentUserId();
        CreateGameResponse response = gameService.createGame(request, userId);
        return ResponseEntity.status(201)
            .location(URI.create("/api/games/" + response.gameId()))
            .body(response);
    }
}
```

**4.2 Konfiguracja Spring Security:**
- Upewnienie się, że `/api/games` wymaga uwierzytelnienia
- Konfiguracja `SecurityFilterChain` z JWT authentication filter

**4.3 Testy kontrolera:**
- Test integracyjny z `@WebMvcTest` dla pomyślnego przypadku (201)
- Test dla przypadku błędów walidacji (422)
- Test dla przypadku nieprawidłowych parametrów (400)
- Test dla przypadku bez tokenu (401)

### Krok 5: Implementacja obsługi błędów

**5.1 Utworzenie global exception handler:**
- Obsługa `MethodArgumentNotValidException` (422)
- Obsługa `BadRequestException` (400)
- Obsługa `DataAccessException` (500)
- Obsługa `DataIntegrityViolationException` (400)

**5.2 Testy exception handler:**
- Test dla każdego typu wyjątku
- Weryfikacja formatu odpowiedzi błędu

### Krok 6: Konfiguracja Swagger/OpenAPI

**6.1 Dodanie adnotacji Swagger:**
```java
@Operation(
    summary = "Create new game",
    description = "Creates a new game (vs_bot or pvp)"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Game created successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid game parameters"),
    @ApiResponse(responseCode = "422", description = "Validation errors")
})
@PostMapping
public ResponseEntity<CreateGameResponse> createGame(...) {
    // ...
}
```

### Krok 7: Testy integracyjne i E2E

**7.1 Testy integracyjne:**
- Test pełnego przepływu z bazą danych
- Test z rzeczywistym tokenem JWT
- Test walidacji ograniczeń bazy danych

**7.2 Testy E2E (Cypress):**
- Test utworzenia gry vs_bot
- Test utworzenia gry pvp
- Test obsługi błędów walidacji

### Krok 8: Dokumentacja i code review

**8.1 Dokumentacja:**
- Aktualizacja README z informacjami o endpoincie
- Dokumentacja Swagger/OpenAPI
- Dokumentacja reguł biznesowych (vs_bot vs pvp)

**8.2 Code review:**
- Sprawdzenie zgodności z zasadami implementacji
- Review bezpieczeństwa
- Weryfikacja obsługi błędów

### Krok 9: Wdrożenie i monitoring

**9.1 Wdrożenie:**
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Test z różnymi konfiguracjami (vs_bot, pvp)

**9.2 Monitoring:**
- Konfiguracja metryk Prometheus
- Konfiguracja alertów dla wysokiego wskaźnika błędów
- Monitorowanie czasu odpowiedzi

## 10. Podsumowanie

Plan implementacji endpointu **POST /api/games** obejmuje kompleksowe podejście do wdrożenia z obsługą obu typów gier (vs_bot i pvp). Kluczowe aspekty:

- **Bezpieczeństwo:** Uwierzytelnianie JWT, walidacja danych, ochrona przed spamem, rate limiting
- **Wydajność:** Optymalizacja zapytań, efektywne generowanie stanu planszy
- **Obsługa błędów:** Centralna obsługa z odpowiednimi kodami statusu
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Dokumentacja:** Swagger/OpenAPI, dokumentacja reguł biznesowych

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
