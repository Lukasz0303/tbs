# API Endpoint Implementation Plan: GET /api/games

## 1. Przegląd punktu końcowego

**GET /api/games** to endpoint służący do pobrania listy gier dla bieżącego użytkownika z filtrowaniem i paginacją. Endpoint wymaga uwierzytelnienia i zwraca tylko gry, w których użytkownik jest uczestnikiem (jako player1 lub player2).

Endpoint obsługuje:
- **Filtrowanie** według statusu (`waiting`, `in_progress`, `finished`, `abandoned`, `draw`)
- **Filtrowanie** według typu gry (`vs_bot`, `pvp`)
- **Paginację** (strona, rozmiar, sortowanie)
- **Sortowanie** po dacie utworzenia (domyślnie: `createdAt,desc`)

Kluczowe zastosowania:
- Wyświetlenie historii gier użytkownika
- Lista aktywnych gier (status: `in_progress`)
- Lista zakończonych gier (status: `finished`, `draw`, `abandoned`)
- Filtrowanie gier według typu (vs_bot vs pvp)

## 2. Szczegóły żądania

### Metoda HTTP
- **GET** - operacja tylko do odczytu, idempotentna

### Struktura URL
```
GET /api/games
```

### Nagłówki żądania

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT wydany po poprawnym logowaniu/rejestracji

**Opcjonalne:**
- `Accept: application/json` - preferowany format odpowiedzi

### Parametry URL
- Brak parametrów URL

### Query Parameters

**Filtrowanie:**
- `status` (String, opcjonalne) - Filtruj według statusu gry
  - Wartości: `waiting`, `in_progress`, `finished`, `abandoned`, `draw`
- `gameType` (String, opcjonalne) - Filtruj według typu gry
  - Wartości: `vs_bot`, `pvp`

**Paginacja:**
- `page` (Integer, opcjonalne, domyślnie: 0) - Numer strony (0-indexed)
- `size` (Integer, opcjonalne, domyślnie: 20, maks: 100) - Rozmiar strony
- `sort` (String, opcjonalne, domyślnie: `createdAt,desc`) - Pole sortowania z kierunkiem

**Przykład zapytania:**
```
GET /api/games?status=in_progress&gameType=vs_bot&page=0&size=20&sort=createdAt,desc
```

### Request Body
- Brak ciała żądania (metoda GET)

### Przykład żądania
```http
GET /api/games?status=in_progress&page=0&size=20 HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Accept: application/json
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
- Brak - metoda GET używa query parameters

#### Response DTO
**`com.tbs.dto.game.GameListResponse`** (istniejący)
```java
public record GameListResponse(
    List<GameListItem> content,
    long totalElements,
    int totalPages,
    int size,
    int number,
    boolean first,
    boolean last
) implements PaginatedResponse<GameListItem> {}
```

**`com.tbs.dto.game.GameListItem`** (istniejący)
```java
public record GameListItem(
    long gameId,
    GameType gameType,
    BoardSize boardSize,
    GameStatus status,
    String player1Username,
    String player2Username,
    String winnerUsername,
    BotDifficulty botDifficulty,
    int totalMoves,
    Instant createdAt,
    Instant lastMoveAt,
    Instant finishedAt
) {}
```

**Uwagi implementacyjne:**
- `content` - Lista gier na danej stronie
- `totalElements` - Całkowita liczba gier (przed paginacją)
- `totalPages` - Całkowita liczba stron
- `size` - Rozmiar strony (z żądania lub domyślny)
- `number` - Numer strony (z żądania lub domyślny: 0)
- `first` - Czy to pierwsza strona
- `last` - Czy to ostatnia strona

### Enums

**`com.tbs.enums.GameStatus`** (istniejący)
- `WAITING`, `IN_PROGRESS`, `FINISHED`, `ABANDONED`, `DRAW`

**`com.tbs.enums.GameType`** (istniejący)
- `VS_BOT`, `PVP`

**`com.tbs.enums.BoardSize`** (istniejący)
- `THREE`, `FOUR`, `FIVE`

**`com.tbs.enums.BotDifficulty`** (istniejący)
- `EASY`, `MEDIUM`, `HARD`

### Modele domenowe (do stworzenia)
- **`com.tbs.model.Game`** - encja JPA/Hibernate dla tabeli `games`
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`
- **`com.tbs.model.Move`** - encja JPA/Hibernate dla tabeli `moves` (dla `totalMoves`)

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.UnauthorizedException`** - wyjątek dla 401 Unauthorized

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.GameService`** - serwis zarządzający grami
- **`com.tbs.service.AuthenticationService`** - wyodrębnianie bieżącego użytkownika

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Pomyślne pobranie listy gier

**Przykład odpowiedzi:**
```json
{
  "content": [
    {
      "gameId": 42,
      "gameType": "vs_bot",
      "boardSize": 3,
      "status": "in_progress",
      "player1Username": "player1",
      "player2Username": null,
      "winnerUsername": null,
      "botDifficulty": "easy",
      "totalMoves": 5,
      "createdAt": "2024-01-20T15:30:00Z",
      "lastMoveAt": "2024-01-20T15:32:00Z",
      "finishedAt": null
    },
    {
      "gameId": 41,
      "gameType": "pvp",
      "boardSize": 3,
      "status": "finished",
      "player1Username": "player1",
      "player2Username": "player2",
      "winnerUsername": "player1",
      "botDifficulty": null,
      "totalMoves": 9,
      "createdAt": "2024-01-20T14:00:00Z",
      "lastMoveAt": "2024-01-20T14:05:00Z",
      "finishedAt": "2024-01-20T14:05:00Z"
    }
  ],
  "totalElements": 25,
  "totalPages": 2,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
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

**400 Bad Request** - Nieprawidłowe parametry zapytania (np. nieprawidłowy status lub gameType)
```json
{
  "error": {
    "code": "BAD_REQUEST",
    "message": "Invalid query parameters",
    "details": {
      "status": "Invalid status value"
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

1. **Odebranie żądania HTTP GET /api/games**
   - Parsowanie query parameters
   - Weryfikacja tokenu JWT przez Spring Security

2. **Walidacja query parameters**
   - Sprawdzenie formatu `page` (Integer >= 0)
   - Sprawdzenie formatu `size` (Integer: 1-100)
   - Sprawdzenie formatu `sort` (String: "field,direction")
   - Walidacja `status` (enum: waiting, in_progress, finished, abandoned, draw)
   - Walidacja `gameType` (enum: vs_bot, pvp)
   - Jeśli nieprawidłowe parametry → 400 Bad Request

3. **Wyodrębnienie bieżącego użytkownika**
   - Z tokenu JWT przez Spring Security
   - Z `SecurityContext.getAuthentication()`
   - Jeśli brak użytkownika → 401 Unauthorized

4. **Budowanie zapytania do bazy danych**
   - Zapytanie: gry gdzie użytkownik jest `player1_id` lub `player2_id`
   - Filtrowanie po `status` (jeśli podane)
   - Filtrowanie po `game_type` (jeśli podane)
   - Sortowanie po `created_at` (domyślnie DESC)
   - Paginacja: `page` i `size`

5. **Wykonanie zapytania z paginacją**
   - Użycie Spring Data JPA `Pageable`
   - Zapytanie z JOIN do tabeli `users` dla `username`
   - Zliczenie `totalMoves` dla każdej gry (COUNT z tabeli `moves`)
   - Jeśli błąd bazy danych → 500 Internal Server Error

6. **Mapowanie do odpowiedzi**
   - Konwersja `Page<Game>` → `GameListResponse` DTO
   - Konwersja `Game` → `GameListItem` DTO
   - Dodanie `totalMoves` (zliczenie ruchów dla każdej gry)

7. **Zwrócenie odpowiedzi HTTP 200 OK**
   - Serializacja `GameListResponse` do JSON

### Integracja z bazą danych

**Tabela: `games`**
- SELECT z JOIN do `users` dla `username`
- Filtrowanie: `WHERE (player1_id = ? OR player2_id = ?)`
- Dodatkowe filtry: `AND status = ?` (jeśli podane), `AND game_type = ?` (jeśli podane)
- Sortowanie: `ORDER BY created_at DESC` (domyślnie)

**Tabela: `users`**
- JOIN do `games` dla `player1_username` i `player2_username`
- JOIN dla `winner_username` (jeśli gra zakończona)

**Tabela: `moves`**
- COUNT dla `totalMoves`: `SELECT COUNT(*) FROM moves WHERE game_id = ?`

**Zapytanie SQL (przykład):**
```sql
SELECT 
  g.id, g.game_type, g.board_size, g.status,
  p1.username as player1_username,
  p2.username as player2_username,
  w.username as winner_username,
  g.bot_difficulty,
  g.created_at, g.last_move_at, g.finished_at,
  (SELECT COUNT(*) FROM moves WHERE game_id = g.id) as total_moves
FROM games g
LEFT JOIN users p1 ON g.player1_id = p1.id
LEFT JOIN users p2 ON g.player2_id = p2.id
LEFT JOIN users w ON g.winner_id = w.id
WHERE (g.player1_id = ? OR g.player2_id = ?)
  AND (? IS NULL OR g.status = ?)
  AND (? IS NULL OR g.game_type = ?)
ORDER BY g.created_at DESC
LIMIT ? OFFSET ?
```

**Indeksy:**
- `idx_games_player1_id` - dla filtrowania po player1_id
- `idx_games_player2_id` - dla filtrowania po player2_id
- `idx_games_status` - dla filtrowania po status
- `idx_games_game_type` - dla filtrowania po game_type
- `idx_games_created_at` - dla sortowania

**Row Level Security (RLS):**
- Polityki RLS mogą automatycznie filtrować gry do uczestników
- Zapytania powinny respektować RLS

### Paginacja

**Implementacja Spring Data JPA:**
- Użycie `Pageable` interface
- Domyślne wartości:
  - `page` = 0 (pierwsza strona)
  - `size` = 20 (rozmiar strony)
  - `sort` = "createdAt,desc"
- Maksymalny rozmiar strony: 100

**Format sortowania:**
- `field,direction` gdzie `direction` = `asc` lub `desc`
- Przykład: `createdAt,desc`, `lastMoveAt,asc`

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
- Tylko zalogowani użytkownicy mogą przeglądać swoje gry
- Zwracane są tylko gry, w których użytkownik jest uczestnikiem

### Walidacja danych wejściowych

**Query Parameters:**
- `page`: Integer >= 0, domyślnie 0
- `size`: Integer: 1-100, domyślnie 20
- `sort`: String (format: "field,direction"), domyślnie "createdAt,desc"
- `status`: Enum (waiting, in_progress, finished, abandoned, draw), opcjonalne
- `gameType`: Enum (vs_bot, pvp), opcjonalne

**Sanityzacja:**
- Trim whitespace dla stringów
- Walidacja zakresu dla `page` i `size`
- Walidacja formatu dla `sort`
- Walidacja enumów dla `status` i `gameType`

### Ochrona przed atakami

**SQL Injection:**
- Użycie parametrówzowanych zapytań (JPA/Hibernate automatycznie)
- Brak dynamicznego SQL na podstawie query parameters
- Użycie bezpiecznych metod JPA Query

**Enum Injection:**
- Walidacja enumów przed użyciem w zapytaniu
- Sprawdzenie wartości przed użyciem

**DoS (Denial of Service):**
- Ograniczenie rozmiaru strony (maks: 100)
- Ograniczenie liczby zapytań przez rate limiting
- Optymalizacja zapytań (indeksy, JOIN-y)

**Rate Limiting:**
- Ograniczenie szybkości dla endpointów wymagających uwierzytelnienia: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Implementacja przez Redis
- Klucz: `rate_limit:games:list:{userId}`

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Brak uwierzytelnienia (401 Unauthorized)
**Scenariusz:** Żądanie bez tokenu JWT lub z nieprawidłowym tokenem
```java
// Spring Security automatycznie zwróci 401 przed dotarciem do kontrolera
```

#### 2. Nieprawidłowe parametry zapytania (400 Bad Request)
**Scenariusz:** Nieprawidłowy format `status` lub `gameType`
```java
@GetMapping("/games")
public ResponseEntity<GameListResponse> getGames(
    @RequestParam(required = false) String status,
    @RequestParam(required = false) String gameType,
    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
) {
    if (status != null && !isValidGameStatus(status)) {
        throw new BadRequestException("Invalid status value: " + status);
    }
    if (gameType != null && !isValidGameType(gameType)) {
        throw new BadRequestException("Invalid gameType value: " + gameType);
    }
    // ...
}
```

#### 3. Błąd bazy danych (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z bazą danych, timeout, błąd SQL
```java
@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ApiErrorResponse> handleDataAccessException(DataAccessException e) {
    log.error("Database error while fetching games", e);
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
- **INFO:** Pomyślne pobranie listy gier (userId, page, size, filters)
- **WARN:** Próba dostępu z nieprawidłowymi parametrami
- **ERROR:** Błędy bazy danych

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, userId, page, size, filters, stack trace (dla błędów)

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Indeksy:**
- `idx_games_player1_id` - dla szybkiego wyszukiwania gier gracza 1
- `idx_games_player2_id` - dla szybkiego wyszukiwania gier gracza 2
- `idx_games_status` - dla filtrowania po status
- `idx_games_game_type` - dla filtrowania po game_type
- `idx_games_created_at` - dla sortowania po dacie utworzenia

**Zapytania:**
- Użycie JOIN zamiast N+1 queries
- COUNT dla `totalMoves` jako subquery lub JOIN (unikanie osobnych zapytań dla każdej gry)
- Użycie `@EntityGraph` jeśli potrzebne lazy loading
- Projektowanie tylko wymaganych kolumn (nie SELECT *)

**Optymalizacja COUNT:**
- Użycie subquery dla `totalMoves` w głównym zapytaniu
- Alternatywnie: cache'owanie liczby ruchów w Redis (aktualizowane przy każdym ruchu)

### Paginacja

**Implementacja:**
- Użycie Spring Data JPA `Pageable` dla efektywnej paginacji
- Limit rozmiaru strony (maks: 100) zapobiega dużym odpowiedziom
- Offset-based paginacja (standardowa w Spring Data JPA)

**Alternatywa: Cursor-based paginacja (dla bardzo dużych zbiorów):**
- Użycie `created_at` jako cursora zamiast OFFSET
- Lepsze dla bardzo dużych zbiorów danych

### Cache'owanie

**Redis Cache (opcjonalne):**
- Klucz: `games:list:{userId}:{status}:{gameType}:{page}:{size}`
- TTL: 30-60 sekund (dla często zmieniających się danych)
- Strategia: Cache-aside
- Inwalidacja: przy tworzeniu nowej gry, zmianie statusu gry, wykonaniu ruchu

**Korzyści:**
- Redukcja obciążenia bazy danych dla często przeglądanych list
- Szybsze odpowiedzi dla powtarzających się żądań

**Wyzwania:**
- Inwalidacja cache przy zmianach (nowe gry, zmiany statusu)
- Zarządzanie TTL (zbyt długie: stare dane, zbyt krótkie: mało efektywne)

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Klucz: `rate_limit:games:list:{userId}`

**Korzyści:**
- Zapobieganie nadmiernemu obciążeniu serwera
- Sprawiedliwy podział zasobów

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="GET",endpoint="/api/games",status="200"}` - liczba pomyślnych żądań
- `http_requests_total{method="GET",endpoint="/api/games",status="400"}` - liczba błędów walidacji
- `http_request_duration_seconds{method="GET",endpoint="/api/games"}` - czas odpowiedzi
- `games_list_query_total{filter_status="in_progress|finished|..."}` - liczba zapytań według filtra

**Alerty:**
- Długi czas odpowiedzi (>1s) - problem z bazą danych lub paginacją
- Wysoki wskaźnik błędów 500 (>1% żądań) - problem z infrastrukturą
- Wysoki wskaźnik błędów 400 (>5% żądań) - problem z walidacją parametrów

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `GameListResponse` i `GameListItem` DTO istnieją
- Sprawdzenie enumów: `GameStatus`, `GameType`, `BoardSize`, `BotDifficulty`
- Weryfikacja struktury pakietów

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.model.Game` - encja JPA/Hibernate
- `com.tbs.repository.GameRepository` - repozytorium z custom queries
- `com.tbs.service.GameService` - serwis zarządzający grami
- `com.tbs.exception.BadRequestException` - wyjątek dla 400

### Krok 2: Implementacja repozytorium z paginacją

**2.1 Utworzenie GameRepository:**
```java
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    @Query("SELECT g FROM Game g " +
           "LEFT JOIN FETCH g.player1 p1 " +
           "LEFT JOIN FETCH g.player2 p2 " +
           "LEFT JOIN FETCH g.winner w " +
           "WHERE (g.player1.id = :userId OR g.player2.id = :userId) " +
           "AND (:status IS NULL OR g.status = :status) " +
           "AND (:gameType IS NULL OR g.gameType = :gameType)")
    Page<Game> findByUserIdAndFilters(
        @Param("userId") Long userId,
        @Param("status") GameStatus status,
        @Param("gameType") GameType gameType,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(m) FROM Move m WHERE m.game.id = :gameId")
    long countMovesByGameId(@Param("gameId") Long gameId);
}
```

**2.2 Testy repozytorium:**
- Test jednostkowy dla `findByUserIdAndFilters` z paginacją
- Test dla różnych filtrów (status, gameType)
- Test dla `countMovesByGameId`

### Krok 3: Implementacja serwisu listy gier

**3.1 Utworzenie GameService:**
```java
@Service
@Transactional(readOnly = true)
public class GameService {
    private final GameRepository gameRepository;
    
    public GameListResponse getGames(Long userId, GameStatus status, GameType gameType, Pageable pageable) {
        Page<Game> games = gameRepository.findByUserIdAndFilters(userId, status, gameType, pageable);
        
        List<GameListItem> items = games.getContent().stream()
            .map(game -> {
                long totalMoves = gameRepository.countMovesByGameId(game.getId());
                return mapToGameListItem(game, totalMoves);
            })
            .collect(Collectors.toList());
        
        return new GameListResponse(
            items,
            games.getTotalElements(),
            games.getTotalPages(),
            games.getSize(),
            games.getNumber(),
            games.isFirst(),
            games.isLast()
        );
    }
}
```

**3.2 Optymalizacja COUNT:**
- Użycie subquery w głównym zapytaniu zamiast osobnych zapytań dla każdej gry
- Alternatywnie: cache'owanie `totalMoves` w Redis

**3.3 Testy serwisu:**
- Test jednostkowy z Mockito dla pomyślnego przypadku (200)
- Test dla różnych filtrów (status, gameType)
- Test dla paginacji (page, size)

### Krok 4: Implementacja kontrolera

**4.1 Utworzenie GameController:**
```java
@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    private final AuthenticationService authenticationService;
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GameListResponse> getGames(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String gameType,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = authenticationService.getCurrentUserId();
        
        GameStatus statusEnum = status != null ? GameStatus.fromValue(status) : null;
        GameType gameTypeEnum = gameType != null ? GameType.fromValue(gameType) : null;
        
        if (status != null && statusEnum == null) {
            throw new BadRequestException("Invalid status value: " + status);
        }
        if (gameType != null && gameTypeEnum == null) {
            throw new BadRequestException("Invalid gameType value: " + gameType);
        }
        
        GameListResponse response = gameService.getGames(userId, statusEnum, gameTypeEnum, pageable);
        return ResponseEntity.ok(response);
    }
}
```

**4.2 Konfiguracja Spring Security:**
- Upewnienie się, że `/api/games` wymaga uwierzytelnienia
- Konfiguracja `SecurityFilterChain` z JWT authentication filter

**4.3 Testy kontrolera:**
- Test integracyjny z `@WebMvcTest` dla pomyślnego przypadku (200)
- Test dla różnych filtrów (status, gameType)
- Test dla paginacji (page, size, sort)
- Test dla przypadku bez tokenu (401)
- Test dla przypadku nieprawidłowych parametrów (400)

### Krok 5: Implementacja obsługi błędów

**5.1 Utworzenie global exception handler:**
- Obsługa `BadRequestException` (400)
- Obsługa `DataAccessException` (500)
- Obsługa `UnauthorizedException` (401)

**5.2 Testy exception handler:**
- Test dla każdego typu wyjątku
- Weryfikacja formatu odpowiedzi błędu

### Krok 6: Konfiguracja Swagger/OpenAPI

**6.1 Dodanie adnotacji Swagger:**
```java
@Operation(
    summary = "Get games list",
    description = "Retrieves a paginated list of games for the current user with filtering"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Games list retrieved successfully"),
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
    @ApiResponse(responseCode = "400", description = "Invalid query parameters")
})
@GetMapping
public ResponseEntity<GameListResponse> getGames(...) {
    // ...
}
```

### Krok 7: Implementacja cache'owania (opcjonalne)

**7.1 Konfiguracja Redis cache:**
- Dodanie `@EnableCaching` w klasie konfiguracyjnej
- Konfiguracja `RedisCacheManager`

**7.2 Dodanie cache do serwisu:**
```java
@Cacheable(value = "gamesList", key = "#userId + ':' + (#status != null ? #status : 'all') + ':' + (#gameType != null ? #gameType : 'all') + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
public GameListResponse getGames(Long userId, GameStatus status, GameType gameType, Pageable pageable) {
    // ...
}
```

**7.3 Inwalidacja cache:**
- Inwalidacja przy tworzeniu nowej gry
- Inwalidacja przy zmianie statusu gry
- Inwalidacja przy wykonaniu ruchu

### Krok 8: Testy integracyjne i E2E

**8.1 Testy integracyjne:**
- Test pełnego przepływu z bazą danych
- Test z rzeczywistym tokenem JWT
- Test paginacji i filtrów
- Test weryfikujący RLS w bazie danych

**8.2 Testy E2E (Cypress):**
- Test pobrania listy gier po logowaniu
- Test filtrowania gier według statusu
- Test paginacji gier

### Krok 9: Dokumentacja i code review

**9.1 Dokumentacja:**
- Aktualizacja README z informacjami o endpoincie
- Dokumentacja Swagger/OpenAPI
- Dokumentacja parametrów zapytania

**9.2 Code review:**
- Sprawdzenie zgodności z zasadami implementacji
- Review bezpieczeństwa
- Weryfikacja obsługi błędów

### Krok 10: Wdrożenie i monitoring

**10.1 Wdrożenie:**
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Test z różnymi konfiguracjami (filtry, paginacja)

**10.2 Monitoring:**
- Konfiguracja metryk Prometheus
- Konfiguracja alertów dla wysokiego wskaźnika błędów
- Monitorowanie czasu odpowiedzi i wydajności zapytań

## 10. Podsumowanie

Plan implementacji endpointu **GET /api/games** obejmuje kompleksowe podejście do wdrożenia z filtrowaniem, paginacją i optymalizacją wydajności. Kluczowe aspekty:

- **Bezpieczeństwo:** Uwierzytelnianie JWT, walidacja parametrów, rate limiting
- **Wydajność:** Optymalizacja zapytań, indeksy, opcjonalne cache'owanie
- **Obsługa błędów:** Centralna obsługa z odpowiednimi kodami statusu
- **Paginacja:** Efektywna paginacja z Spring Data JPA
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Dokumentacja:** Swagger/OpenAPI, dokumentacja parametrów

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
