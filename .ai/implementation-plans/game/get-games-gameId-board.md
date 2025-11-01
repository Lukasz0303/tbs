# API Endpoint Implementation Plan: GET /api/games/{gameId}/board

## 1. Przegląd punktu końcowego

**GET /api/games/{gameId}/board** to endpoint służący do pobrania aktualnego stanu planszy dla gry. Endpoint wymaga uwierzytelnienia i zwraca tylko stan planszy (bez pełnych szczegółów gry).

Endpoint zwraca:
- Aktualny stan planszy (generowany dynamicznie z historii ruchów)
- Rozmiar planszy
- Liczbę wykonanych ruchów
- Informacje o ostatnim ruchu (jeśli dostępne)

Kluczowe zastosowania:
- Odświeżenie stanu planszy podczas rozgrywki
- Szybkie sprawdzenie aktualnego stanu gry (lżejsze niż pełne szczegóły gry)
- Wyświetlenie planszy bez historii wszystkich ruchów

## 2. Szczegóły żądania

### Metoda HTTP
- **GET** - operacja tylko do odczytu, idempotentna

### Struktura URL
```
GET /api/games/{gameId}/board
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
GET /api/games/42/board HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Accept: application/json
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
- Brak - metoda GET nie wymaga DTO żądania

#### Response DTO
**`com.tbs.dto.game.BoardStateResponse`** (istniejący)
```java
public record BoardStateResponse(
    BoardState boardState,
    BoardSize boardSize,
    int totalMoves,
    LastMove lastMove
) {
    public record LastMove(
        int row,
        int col,
        PlayerSymbol playerSymbol,
        int moveOrder
    ) {}
}
```

**`com.tbs.dto.common.BoardState`** (istniejący)
```java
public record BoardState(String[][] cells) {}
```

**Uwagi implementacyjne:**
- `boardState` - Stan planszy jako tablica 2D (generowany dynamicznie z historii ruchów)
- `boardSize` - Rozmiar planszy z `games.board_size`
- `totalMoves` - Liczba wykonanych ruchów (COUNT z tabeli `moves`)
- `lastMove` - Informacje o ostatnim ruchu (NULL jeśli brak ruchów)

### Enums

**`com.tbs.enums.BoardSize`** (istniejący)
- `THREE` (3x3), `FOUR` (4x4), `FIVE` (5x5)

**`com.tbs.enums.PlayerSymbol`** (istniejący)
- `X`, `O`

### Modele domenowe (do stworzenia)
- **`com.tbs.model.Game`** - encja JPA/Hibernate dla tabeli `games`
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

**200 OK** - Pomyślne pobranie stanu planszy

**Przykład odpowiedzi:**
```json
{
  "boardState": {
    "cells": [
      ["x", null, "o"],
      [null, "x", null],
      [null, null, "o"]
    ]
  },
  "boardSize": 3,
  "totalMoves": 5,
  "lastMove": {
    "row": 2,
    "col": 2,
    "playerSymbol": "o",
    "moveOrder": 5
  }
}
```

**Przykład odpowiedzi dla nowej gry (bez ruchów):**
```json
{
  "boardState": {
    "cells": [
      [null, null, null],
      [null, null, null],
      [null, null, null]
    ]
  },
  "boardSize": 3,
  "totalMoves": 0,
  "lastMove": null
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

1. **Odebranie żądania HTTP GET /api/games/{gameId}/board**
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

6. **Pobranie historii ruchów**
   - Zapytanie: `SELECT * FROM moves WHERE game_id = ? ORDER BY move_order ASC`
   - Lista wszystkich ruchów uporządkowana według `move_order`

7. **Generowanie stanu planszy**
   - Użycie funkcji bazy danych `generate_board_state(p_game_id)` (jeśli dostępna)
   - Alternatywnie: generowanie w warstwie aplikacji na podstawie historii ruchów
   - Inicjalizacja pustej planszy o rozmiarze `boardSize x boardSize`
   - Wypełnienie planszy symbolami z ruchów

8. **Pobranie ostatniego ruchu**
   - Ostatni ruch z listy ruchów (najwyższy `move_order`)
   - Jeśli brak ruchów → `lastMove = null`

9. **Zliczenie liczby ruchów**
   - `totalMoves = moves.size()` z pobranych ruchów
   - Alternatywnie: COUNT z tabeli `moves`: `SELECT COUNT(*) FROM moves WHERE game_id = ?`

10. **Mapowanie do odpowiedzi**
    - Konwersja stanu planszy → `BoardState` DTO
    - Konwersja rozmiaru planszy → `BoardSize` enum
    - Konwersja ostatniego ruchu → `LastMove` DTO (jeśli dostępny)
    - Utworzenie `BoardStateResponse` DTO

11. **Zwrócenie odpowiedzi HTTP 200 OK**
    - Serializacja `BoardStateResponse` do JSON

### Integracja z bazą danych

**Tabela: `games`**
- SELECT podstawowych informacji o grze: `SELECT id, board_size, player1_id, player2_id FROM games WHERE id = ?`

**Tabela: `moves`**
- SELECT wszystkich ruchów dla gry: `SELECT * FROM moves WHERE game_id = ? ORDER BY move_order ASC`
- Użycie indeksu `idx_moves_game_id_move_order` dla szybkiego sortowania

**Funkcja bazy danych (opcjonalnie):**
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
- Użycie funkcji bazy danych `generate_board_state(p_game_id)` (jeśli dostępna) - wydajniejsze
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
- Użycie funkcji bazy danych redukuje obciążenie warstwy aplikacji

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
- Tylko uczestnicy gry (player1 lub player2) mogą przeglądać stan planszy
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
- Weryfikacja uczestnictwa przed zwróceniem stanu planszy
- Tylko uczestnicy mogą zobaczyć stan planszy
- RLS w bazie danych zapewnia dodatkową warstwę ochrony

**Rate Limiting:**
- Ograniczenie szybkości dla endpointów wymagających uwierzytelnienia: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Implementacja przez Redis
- Klucz: `rate_limit:games:board:{userId}`

## 7. Obsługa błędów

### Scenariusze błędów i obsługa

#### 1. Nieprawidłowy format gameId (400 Bad Request)
**Scenariusz:** `gameId` w URL nie jest liczbą
```java
@GetMapping("/{gameId}/board")
public ResponseEntity<BoardStateResponse> getBoardState(@PathVariable String gameId) {
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
    log.error("Database error while fetching board state", e);
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
- **INFO:** Pomyślne pobranie stanu planszy (gameId, userId)
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
- Pobranie tylko wymaganych danych (nie SELECT *)
- Użycie `ORDER BY move_order ASC` dla prawidłowej kolejności ruchów
- Użycie indeksów dla efektywnych zapytań

**Optymalizacja COUNT:**
- `totalMoves` można pobrać jako `moves.size()` zamiast osobnego zapytania COUNT
- Alternatywnie: użycie subquery w głównym zapytaniu

### Generowanie stanu planszy

**Optymalizacja:**
- Użycie funkcji bazy danych `generate_board_state()` (jeśli dostępna) - wydajniejsze
- Alternatywnie: generowanie w warstwie aplikacji (mniej obciążenia bazy danych dla wielu jednoczesnych żądań)
- Cache'owanie stanu planszy w Redis (opcjonalne, z inwalidacją przy każdym ruchu)

**Strategia cache'owania:**
- Klucz: `game:board:{gameId}`
- TTL: 10-30 sekund (dla często zmieniających się danych)
- Inwalidacja: przy każdym ruchu

### Cache'owanie

**Redis Cache (opcjonalne):**
- Klucz: `game:board:{gameId}`
- TTL: 10-30 sekund (dla często zmieniających się danych)
- Strategia: Cache-aside
- Inwalidacja: przy każdym ruchu

**Korzyści:**
- Redukcja obciążenia bazy danych dla często przeglądanych stanów planszy
- Szybsze odpowiedzi dla powtarzających się żądań

**Wyzwania:**
- Inwalidacja cache przy zmianach (nowe ruchy)
- Zarządzanie TTL (zbyt długie: stare dane, zbyt krótkie: mało efektywne)

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Klucz: `rate_limit:games:board:{userId}`

**Korzyści:**
- Zapobieganie nadmiernemu obciążeniu serwera
- Sprawiedliwy podział zasobów

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="GET",endpoint="/api/games/{gameId}/board",status="200"}` - liczba pomyślnych żądań
- `http_requests_total{method="GET",endpoint="/api/games/{gameId}/board",status="403"}` - liczba błędów autoryzacji
- `http_requests_total{method="GET",endpoint="/api/games/{gameId}/board",status="404"}` - liczba błędów 404
- `http_request_duration_seconds{method="GET",endpoint="/api/games/{gameId}/board"}` - czas odpowiedzi
- `board_state_generation_duration_seconds` - czas generowania stanu planszy

**Alerty:**
- Wysoki wskaźnik błędów 403 (>10% żądań) - możliwe problemy z autoryzacją
- Długi czas odpowiedzi (>500ms) - problem z bazą danych lub generowaniem stanu planszy
- Wysoki wskaźnik błędów 500 (>1% żądań) - problem z infrastrukturą

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `BoardStateResponse`, `BoardState`, `LastMove` DTO istnieją
- Sprawdzenie enumów: `BoardSize`, `PlayerSymbol`
- Weryfikacja struktury pakietów

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.model.Game` - encja JPA/Hibernate
- `com.tbs.model.Move` - encja JPA/Hibernate
- `com.tbs.service.GameService` - serwis zarządzający grami
- `com.tbs.service.BoardStateService` - generowanie stanu planszy
- `com.tbs.exception.GameNotFoundException` - wyjątek dla 404
- `com.tbs.exception.ForbiddenException` - wyjątek dla 403

### Krok 2: Implementacja repozytorium

**2.1 Utworzenie GameRepository:**
```java
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findById(Long id);
}
```

**2.2 Utworzenie MoveRepository:**
```java
@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {
    List<Move> findByGameIdOrderByMoveOrderAsc(Long gameId);
    
    Optional<Move> findFirstByGameIdOrderByMoveOrderDesc(Long gameId);
}
```

**2.3 Testy repozytorium:**
- Test jednostkowy dla `findById`
- Test dla `findByGameIdOrderByMoveOrderAsc`
- Test dla `findFirstByGameIdOrderByMoveOrderDesc`

### Krok 3: Implementacja serwisu stanu planszy

**3.1 Utworzenie GameService:**
```java
@Service
@Transactional(readOnly = true)
public class GameService {
    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final BoardStateService boardStateService;
    
    public BoardStateResponse getBoardState(Long gameId, Long userId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found"));
        
        if (!game.getPlayer1().getId().equals(userId) && 
            (game.getPlayer2() == null || !game.getPlayer2().getId().equals(userId))) {
            throw new ForbiddenException("You are not a participant of this game");
        }
        
        List<Move> moves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        BoardState boardState = boardStateService.generateBoardState(game, moves);
        
        Move lastMove = moveRepository.findFirstByGameIdOrderByMoveOrderDesc(gameId).orElse(null);
        BoardStateResponse.LastMove lastMoveDto = lastMove != null 
            ? new BoardStateResponse.LastMove(lastMove.getRow(), lastMove.getCol(), 
                                             lastMove.getPlayerSymbol(), lastMove.getMoveOrder())
            : null;
        
        return new BoardStateResponse(
            boardState,
            game.getBoardSize(),
            moves.size(),
            lastMoveDto
        );
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
- Test dla generowania stanu planszy (pusta plansza, plansza z ruchami)
- Test dla ostatniego ruchu (z ruchem, bez ruchu)

### Krok 4: Implementacja kontrolera

**4.1 Utworzenie GameController:**
```java
@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    private final AuthenticationService authenticationService;
    
    @GetMapping("/{gameId}/board")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BoardStateResponse> getBoardState(@PathVariable Long gameId) {
        Long userId = authenticationService.getCurrentUserId();
        BoardStateResponse response = gameService.getBoardState(gameId, userId);
        return ResponseEntity.ok(response);
    }
}
```

**4.2 Konfiguracja Spring Security:**
- Upewnienie się, że `/api/games/{gameId}/board` wymaga uwierzytelnienia
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
    summary = "Get board state",
    description = "Retrieves the current board state for a game"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Board state retrieved successfully"),
    @ApiResponse(responseCode = "403", description = "You are not a participant of this game"),
    @ApiResponse(responseCode = "404", description = "Game not found")
})
@GetMapping("/{gameId}/board")
public ResponseEntity<BoardStateResponse> getBoardState(...) {
    // ...
}
```

### Krok 7: Implementacja cache'owania (opcjonalne)

**7.1 Konfiguracja Redis cache:**
- Dodanie `@EnableCaching` w klasie konfiguracyjnej
- Konfiguracja `RedisCacheManager`

**7.2 Dodanie cache do serwisu:**
```java
@Cacheable(value = "boardState", key = "#gameId")
public BoardStateResponse getBoardState(Long gameId, Long userId) {
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
- Test generowania stanu planszy (pusta plansza, plansza z ruchami)

**8.2 Testy E2E (Cypress):**
- Test pobrania stanu planszy po logowaniu
- Test obsługi błędu 403 dla gry bez uczestnictwa
- Test wyświetlania stanu planszy w UI

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
- Test z różnymi stanami gry (nowa gra, gra z ruchami)

**10.2 Monitoring:**
- Konfiguracja metryk Prometheus
- Konfiguracja alertów dla wysokiego wskaźnika błędów
- Monitorowanie czasu odpowiedzi i wydajności generowania stanu planszy

## 10. Podsumowanie

Plan implementacji endpointu **GET /api/games/{gameId}/board** obejmuje kompleksowe podejście do wdrożenia z generowaniem stanu planszy i autoryzacją uczestników. Kluczowe aspekty:

- **Bezpieczeństwo:** Uwierzytelnianie JWT, autoryzacja uczestników, ochrona przed nieautoryzowanym dostępem
- **Wydajność:** Optymalizacja zapytań, generowanie stanu planszy, opcjonalne cache'owanie
- **Obsługa błędów:** Centralna obsługa z odpowiednimi kodami statusu
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Dokumentacja:** Swagger/OpenAPI, dokumentacja generowania stanu planszy

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
