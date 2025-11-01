# API Endpoint Implementation Plan: PUT /api/games/{gameId}/status

## 1. Przegląd punktu końcowego

**PUT /api/games/{gameId}/status** to endpoint służący do aktualizacji statusu gry (poddanie, porzucenie). Endpoint wymaga uwierzytelnienia i pozwala uczestnikom gry (player1 lub player2) zaktualizować status gry, np. na `abandoned` (porzucona) lub zakończyć grę przez poddanie.

Endpoint obsługuje przejścia statusu:
- `in_progress` → `abandoned` - Porzucenie gry
- `in_progress` → `finished` - Poddanie się (gra kończy się z wygraną przeciwnika)
- Inne przejścia mogą być obsługiwane zgodnie z regułami biznesowymi

Kluczowe zastosowania:
- Poddanie się w grze (gracz uznaje się za pokonanego)
- Porzucenie gry (gracz opuszcza grę)
- Zmiana statusu gry przez uczestnika

## 2. Szczegóły żądania

### Metoda HTTP
- **PUT** - operacja aktualizacji zasobu

### Struktura URL
```
PUT /api/games/{gameId}/status
```

### Nagłówki żądania

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT wydany po poprawnym logowaniu/rejestracji
- `Content-Type: application/json` - format treści żądania

**Opcjonalne:**
- `Accept: application/json` - preferowany format odpowiedzi

### Parametry URL

**Path Variables:**
- `gameId` (Long) - ID gry z tabeli `games.id`

### Query Parameters
- Brak parametrów zapytania

### Request Body

**`UpdateGameStatusRequest`** DTO:
```json
{
  "status": "abandoned"
}
```

**Walidacja:**
- `status`: Wymagane (@NotNull), enum: `WAITING`, `IN_PROGRESS`, `FINISHED`, `ABANDONED`, `DRAW`
- Dozwolone przejścia statusu (reguły biznesowe):
  - `in_progress` → `abandoned` - Porzucenie gry
  - `in_progress` → `finished` - Poddanie się
  - Inne przejścia mogą być niedozwolone (np. `finished` → `in_progress`)

### Przykład żądania
```http
PUT /api/games/42/status HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
Accept: application/json

{
  "status": "abandoned"
}
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
**`com.tbs.dto.game.UpdateGameStatusRequest`** (istniejący)
```java
public record UpdateGameStatusRequest(
    @NotNull(message = "Status is required")
    GameStatus status
) {}
```

#### Response DTO
**`com.tbs.dto.game.UpdateGameStatusResponse`** (istniejący)
```java
public record UpdateGameStatusResponse(
    long gameId,
    GameStatus status,
    Instant updatedAt
) {}
```

**Uwagi implementacyjne:**
- `gameId` - ID gry z tabeli `games.id`
- `status` - Nowy status gry z `games.status`
- `updatedAt` - Data aktualizacji z `games.updated_at` (aktualizowana automatycznie przez trigger)

### Enums

**`com.tbs.enums.GameStatus`** (istniejący)
- `WAITING`, `IN_PROGRESS`, `FINISHED`, `ABANDONED`, `DRAW`

### Modele domenowe (do stworzenia)
- **`com.tbs.model.Game`** - encja JPA/Hibernate dla tabeli `games`

### Wyjątki (do stworzenia lub wykorzystania)
- **`com.tbs.exception.GameNotFoundException`** - wyjątek dla 404 Not Found
- **`com.tbs.exception.ForbiddenException`** - wyjątek dla 403 Forbidden
- **`com.tbs.exception.ValidationException`** - wyjątek dla 422 Unprocessable Entity
- **`com.tbs.exception.UnauthorizedException`** - wyjątek dla 401 Unauthorized

### Serwisy (do stworzenia lub wykorzystania)
- **`com.tbs.service.GameService`** - serwis zarządzający grami
- **`com.tbs.service.AuthenticationService`** - wyodrębnianie bieżącego użytkownika

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Status gry zaktualizowany pomyślnie

**Przykład odpowiedzi:**
```json
{
  "gameId": 42,
  "status": "abandoned",
  "updatedAt": "2024-01-20T15:35:00Z"
}
```

### Kody statusu błędów

**400 Bad Request** - Nieprawidłowe parametry żądania
```json
{
  "error": {
    "code": "BAD_REQUEST",
    "message": "Invalid request parameters",
    "details": null
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

**403 Forbidden** - Nie jesteś uczestnikiem lub nieprawidłowe przejście statusu
```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "You are not a participant of this game or invalid status transition",
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

**422 Unprocessable Entity** - Nieprawidłowe przejście statusu
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid status transition",
    "details": {
      "currentStatus": "finished",
      "requestedStatus": "in_progress",
      "reason": "Cannot change status from finished to in_progress"
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

1. **Odebranie żądania HTTP PUT /api/games/{gameId}/status**
   - Walidacja formatu JSON
   - Parsowanie `UpdateGameStatusRequest` DTO
   - Parsowanie `gameId` z path variable
   - Weryfikacja tokenu JWT przez Spring Security

2. **Walidacja danych wejściowych (Bean Validation)**
   - Walidacja adnotacji Bean Validation na `UpdateGameStatusRequest`
   - Sprawdzenie obecności `status` (@NotNull)
   - Jeśli błędy walidacji → 422 Unprocessable Entity

3. **Walidacja `gameId`**
   - Sprawdzenie czy `gameId` jest poprawną liczbą
   - Jeśli nieprawidłowy format → 400 Bad Request

4. **Wyodrębnienie bieżącego użytkownika**
   - Z tokenu JWT przez Spring Security
   - Z `SecurityContext.getAuthentication()`
   - Jeśli brak użytkownika → 401 Unauthorized

5. **Pobranie gry z bazy danych**
   - Zapytanie: `SELECT * FROM games WHERE id = ?`
   - Jeśli gra nie istnieje → 404 Not Found

6. **Weryfikacja uczestnictwa**
   - Sprawdzenie czy użytkownik jest `player1_id` lub `player2_id`
   - Jeśli nie jest uczestnikiem → 403 Forbidden

7. **Walidacja przejścia statusu**
   - Sprawdzenie czy przejście z aktualnego statusu na nowy jest dozwolone
   - Dozwolone przejścia:
     - `in_progress` → `abandoned` - Porzucenie gry
     - `in_progress` → `finished` - Poddanie się (ustawienie zwycięzcy)
   - Niedozwolone przejścia:
     - `finished` → `in_progress` - Gra już zakończona
     - `abandoned` → `in_progress` - Gra już porzucona
     - `waiting` → `finished` - Gra nie rozpoczęta
   - Jeśli nieprawidłowe przejście → 422 Unprocessable Entity

8. **Aktualizacja statusu gry**
   - UPDATE `games.status = ? WHERE id = ?`
   - Jeśli nowy status to `finished` lub `abandoned`:
     - Ustawienie `finished_at = NOW()` (jeśli status to `finished`, `abandoned`, lub `draw`)
     - Jeśli status to `finished` (poddanie):
       - Ustawienie `winner_id` na przeciwnika (gracz, który nie poddał się)
   - Trigger `update_games_updated_at` automatycznie aktualizuje `updated_at`
   - Jeśli błąd bazy danych → 500 Internal Server Error

9. **Automatyczne aktualizacje (triggery)**
   - Trigger `update_user_stats_on_game_finished` (jeśli status = `finished`):
     - Aktualizacja statystyk użytkownika (punkty, gry rozegrane, wygrane)
     - Obliczanie punktów przez `calculate_game_points()`

10. **Pobranie zaktualizowanej gry**
    - SELECT zaktualizowanego rekordu z bazy danych
    - Pobranie `updated_at` (zaktualizowane przez trigger)

11. **Mapowanie do odpowiedzi**
    - Konwersja encji `Game` → `UpdateGameStatusResponse` DTO
    - Dodanie `updatedAt` (z bazy danych)

12. **Zwrócenie odpowiedzi HTTP 200 OK**
    - Serializacja `UpdateGameStatusResponse` do JSON

### Integracja z bazą danych

**Tabela: `games`**
- UPDATE statusu gry: `UPDATE games SET status = ?, winner_id = ?, finished_at = ? WHERE id = ?`
- Kolumny aktualizowane:
  - `status` (game_status_enum) - nowy status z żądania
  - `winner_id` (BIGINT) - ustawiony na przeciwnika jeśli status = `finished` (poddanie)
  - `finished_at` (TIMESTAMP) - ustawiony na NOW() jeśli status to `finished`, `abandoned`, lub `draw`
  - `updated_at` (TIMESTAMP) - aktualizowany automatycznie przez trigger `update_games_updated_at`

**Ograniczenia bazy danych (CHECK constraints):**
- `games_status_check`: Walidacja poprawności statusu
- `games_finished_check`: Zakończone gry wymagają `finished_at IS NOT NULL`

**Triggery:**
- `update_games_updated_at`: Automatyczna aktualizacja `updated_at` przy UPDATE
- `update_user_stats_on_game_finished`: Automatyczna aktualizacja statystyk użytkownika gdy status = `finished`
  - Obliczanie punktów przez `calculate_game_points()`
  - Aktualizacja `users.total_points`, `games_played`, `games_won`

**Funkcje bazy danych:**
- `calculate_game_points(p_game_type, p_bot_difficulty)`: Obliczanie punktów za wygraną
- Wywoływane automatycznie przez trigger `update_user_stats_on_game_finished`

**Row Level Security (RLS):**
- Polityki RLS mogą ograniczać dostęp do aktualizacji gry tylko dla uczestników
- Zapytania powinny respektować RLS

### Walidacja przejść statusu

**Dozwolone przejścia:**
- `in_progress` → `abandoned` - Porzucenie gry (ustawienie `finished_at`, bez zwycięzcy)
- `in_progress` → `finished` - Poddanie się (ustawienie `winner_id` na przeciwnika, `finished_at`)

**Niedozwolone przejścia:**
- `finished` → `in_progress` - Gra już zakończona
- `abandoned` → `in_progress` - Gra już porzucona
- `waiting` → `finished` - Gra nie rozpoczęta
- `finished` → `abandoned` - Gra już zakończona
- `abandoned` → `finished` - Gra już porzucona

**Implementacja:**
```java
private void validateStatusTransition(GameStatus currentStatus, GameStatus newStatus) {
    if (currentStatus == GameStatus.FINISHED || currentStatus == GameStatus.ABANDONED) {
        throw new ValidationException("Cannot change status from " + currentStatus + " to " + newStatus);
    }
    if (currentStatus == GameStatus.WAITING && newStatus == GameStatus.FINISHED) {
        throw new ValidationException("Cannot finish a game that has not started");
    }
    if (currentStatus == GameStatus.IN_PROGRESS) {
        if (newStatus != GameStatus.FINISHED && newStatus != GameStatus.ABANDONED) {
            throw new ValidationException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }
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
- Tylko uczestnicy gry (player1 lub player2) mogą aktualizować status gry
- Weryfikacja uczestnictwa w warstwie aplikacji lub przez RLS

### Walidacja danych wejściowych

**Bean Validation:**
- `status`: Wymagane (@NotNull), enum (tylko dozwolone wartości)

**Reguły biznesowe:**
- Walidacja przejść statusu (dozwolone vs niedozwolone przejścia)
- Sprawdzenie aktualnego statusu przed aktualizacją

### Ochrona przed atakami

**SQL Injection:**
- Użycie parametrówzowanych zapytań (JPA/Hibernate automatycznie)
- Walidacja `gameId` i `status` przed użyciem w zapytaniu

**Enum Injection:**
- Walidacja enumów przez Bean Validation
- Sprawdzenie wartości przed użyciem

**Rate Limiting:**
- Ograniczenie szybkości dla endpointów wymagających uwierzytelnienia: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Implementacja przez Redis
- Klucz: `rate_limit:games:status:{userId}`

**Ochrona przed manipulacją:**
- Weryfikacja uczestnictwa przed aktualizacją statusu
- Walidacja przejść statusu zapobiega nieprawidłowym zmianom stanu gry
- RLS w bazie danych zapewnia dodatkową warstwę ochrony

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

#### 3. Nieprawidłowe przejście statusu (422 Unprocessable Entity)
**Scenariusz:** Próba zmiany statusu z niedozwolonym przejściem (np. `finished` → `in_progress`)
```java
if (!isValidStatusTransition(currentStatus, requestedStatus)) {
    throw new ValidationException(
        "Invalid status transition from " + currentStatus + " to " + requestedStatus,
        Map.of("currentStatus", currentStatus, "requestedStatus", requestedStatus)
    );
}
```

#### 4. Gra nie znaleziona (404 Not Found)
**Scenariusz:** Gra z `gameId` nie istnieje w bazie danych
```java
Optional<Game> game = gameRepository.findById(gameId);
if (game.isEmpty()) {
    throw new GameNotFoundException("Game not found");
}
```

#### 5. Brak dostępu (403 Forbidden)
**Scenariusz:** Użytkownik nie jest uczestnikiem gry
```java
Game game = gameRepository.findById(gameId).orElseThrow();
if (!game.getPlayer1().getId().equals(userId) && 
    (game.getPlayer2() == null || !game.getPlayer2().getId().equals(userId))) {
    throw new ForbiddenException("You are not a participant of this game");
}
```

#### 6. Błąd bazy danych (500 Internal Server Error)
**Scenariusz:** Błąd połączenia z bazą danych, constraint violation, timeout
```java
@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ApiErrorResponse> handleDataAccessException(DataAccessException e) {
    log.error("Database error during game status update", e);
    return ResponseEntity.status(500)
        .body(new ApiErrorResponse(
            new ErrorDetails("INTERNAL_SERVER_ERROR", "Database error occurred", null)
        ));
}
```

#### 7. Constraint violation (400 Bad Request lub 422)
**Scenariusz:** Naruszenie ograniczeń bazy danych (np. games_status_check)
```java
try {
    gameRepository.save(game);
} catch (DataIntegrityViolationException e) {
    if (e.getMessage().contains("games_status_check")) {
        throw new ValidationException("Invalid game status configuration");
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
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(ValidationException e) {
        // 422 handling
    }
    
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
- **INFO:** Pomyślna aktualizacja statusu gry (gameId, userId, oldStatus, newStatus)
- **WARN:** Próba nieprawidłowego przejścia statusu (gameId, userId, oldStatus, newStatus)
- **ERROR:** Błędy bazy danych, constraint violations

**Strukturazowane logowanie:**
- Format JSON dla łatwej integracji z systemami monitoringu
- Zawartość logów: timestamp, poziom, komunikat, gameId, userId, oldStatus, newStatus, stack trace (dla błędów)

## 8. Rozważania dotyczące wydajności

### Optymalizacja zapytań do bazy danych

**Indeksy:**
- Tabela `games` powinna mieć indeks na `id` (PK, automatyczny)
- Indeks na `status` może pomóc w zapytaniach filtrujących

**Zapytania:**
- UPDATE tylko wymaganych kolumn (nie wszystkie kolumny)
- Użycie `@Modifying` w Spring Data JPA dla UPDATE zapytań
- Pobranie zaktualizowanego rekordu po UPDATE (jeśli potrzebne `updated_at` z triggera)

**Optymalizacja UPDATE:**
- UPDATE z WHERE clause: `UPDATE games SET status = ?, ... WHERE id = ?`
- Użycie `@Query` dla custom UPDATE zapytań (opcjonalne)

### Transakcyjność

**Strategia:**
- Użycie `@Transactional` dla atomowej aktualizacji
- Jeśli status = `finished` (poddanie):
  - UPDATE statusu gry
  - UPDATE zwycięzcy (`winner_id`)
  - Ustawienie `finished_at`
  - Trigger automatycznie aktualizuje statystyki użytkownika
- Wszystkie operacje w jednej transakcji

### Rate Limiting

**Implementacja:**
- Redis-based rate limiting z algorytmem przesuwającego okna
- Limit: 1000 żądań/minutę na użytkownika (zgodnie z api-plan.md)
- Klucz: `rate_limit:games:status:{userId}`

**Korzyści:**
- Zapobieganie spamowi aktualizacji statusu
- Sprawiedliwy podział zasobów

### Monitoring i metryki

**Metryki Prometheus:**
- `http_requests_total{method="PUT",endpoint="/api/games/{gameId}/status",status="200"}` - liczba pomyślnych aktualizacji
- `http_requests_total{method="PUT",endpoint="/api/games/{gameId}/status",status="403"}` - liczba błędów autoryzacji
- `http_requests_total{method="PUT",endpoint="/api/games/{gameId}/status",status="422"}` - liczba błędów walidacji
- `http_request_duration_seconds{method="PUT",endpoint="/api/games/{gameId}/status"}` - czas odpowiedzi
- `game_status_transitions_total{from="in_progress",to="abandoned|finished"}` - metryki przejść statusu

**Alerty:**
- Wysoki wskaźnik błędów 422 (>10% żądań) - problem z walidacją przejść statusu
- Długi czas odpowiedzi (>500ms) - problem z bazą danych
- Wysoki wskaźnik błędów 500 (>1% żądań) - problem z infrastrukturą

## 9. Etapy wdrożenia

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów:**
- Weryfikacja czy `UpdateGameStatusRequest` i `UpdateGameStatusResponse` DTO istnieją
- Sprawdzenie enumów: `GameStatus`
- Weryfikacja struktury pakietów

**1.2 Utworzenie brakujących komponentów:**
- `com.tbs.model.Game` - encja JPA/Hibernate
- `com.tbs.service.GameService` - serwis zarządzający grami
- `com.tbs.exception.GameNotFoundException` - wyjątek dla 404
- `com.tbs.exception.ForbiddenException` - wyjątek dla 403
- `com.tbs.exception.ValidationException` - wyjątek dla 422

### Krok 2: Implementacja logiki walidacji przejść statusu

**2.1 Utworzenie StatusTransitionValidator:**
```java
@Service
public class StatusTransitionValidator {
    
    public void validateTransition(GameStatus currentStatus, GameStatus newStatus) {
        if (currentStatus == GameStatus.FINISHED || currentStatus == GameStatus.ABANDONED) {
            throw new ValidationException("Cannot change status from " + currentStatus + " to " + newStatus);
        }
        
        if (currentStatus == GameStatus.WAITING && newStatus == GameStatus.FINISHED) {
            throw new ValidationException("Cannot finish a game that has not started");
        }
        
        if (currentStatus == GameStatus.IN_PROGRESS) {
            if (newStatus != GameStatus.FINISHED && newStatus != GameStatus.ABANDONED) {
                throw new ValidationException("Invalid status transition from " + currentStatus + " to " + newStatus);
            }
        }
    }
}
```

**2.2 Testy walidatora:**
- Test dla dozwolonych przejść (`in_progress` → `abandoned`, `in_progress` → `finished`)
- Test dla niedozwolonych przejść (`finished` → `in_progress`, `waiting` → `finished`)

### Krok 3: Implementacja serwisu aktualizacji statusu

**3.1 Utworzenie GameService:**
```java
@Service
@Transactional
public class GameService {
    private final GameRepository gameRepository;
    private final StatusTransitionValidator statusTransitionValidator;
    private final AuthenticationService authenticationService;
    
    public UpdateGameStatusResponse updateGameStatus(Long gameId, GameStatus newStatus, Long userId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found"));
        
        if (!game.getPlayer1().getId().equals(userId) && 
            (game.getPlayer2() == null || !game.getPlayer2().getId().equals(userId))) {
            throw new ForbiddenException("You are not a participant of this game");
        }
        
        statusTransitionValidator.validateTransition(game.getStatus(), newStatus);
        
        GameStatus oldStatus = game.getStatus();
        game.setStatus(newStatus);
        
        if (newStatus == GameStatus.FINISHED || newStatus == GameStatus.ABANDONED) {
            game.setFinishedAt(Instant.now());
        }
        
        if (newStatus == GameStatus.FINISHED) {
            Long winnerId = game.getPlayer1().getId().equals(userId) 
                ? game.getPlayer2().getId() 
                : game.getPlayer1().getId();
            game.setWinnerId(winnerId);
        }
        
        Game updatedGame = gameRepository.save(game);
        
        return new UpdateGameStatusResponse(
            updatedGame.getId(),
            updatedGame.getStatus(),
            updatedGame.getUpdatedAt()
        );
    }
}
```

**3.2 Testy serwisu:**
- Test jednostkowy z Mockito dla pomyślnego przypadku (200)
- Test dla przypadku gdy gra nie istnieje (404)
- Test dla przypadku gdy użytkownik nie jest uczestnikiem (403)
- Test dla przypadku nieprawidłowego przejścia statusu (422)
- Test dla przypadku poddania się (ustawienie zwycięzcy)

### Krok 4: Implementacja kontrolera

**4.1 Utworzenie GameController:**
```java
@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    private final AuthenticationService authenticationService;
    
    @PutMapping("/{gameId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UpdateGameStatusResponse> updateGameStatus(
        @PathVariable Long gameId,
        @Valid @RequestBody UpdateGameStatusRequest request
    ) {
        Long userId = authenticationService.getCurrentUserId();
        UpdateGameStatusResponse response = gameService.updateGameStatus(gameId, request.status(), userId);
        return ResponseEntity.ok(response);
    }
}
```

**4.2 Konfiguracja Spring Security:**
- Upewnienie się, że `/api/games/{gameId}/status` wymaga uwierzytelnienia
- Konfiguracja `SecurityFilterChain` z JWT authentication filter

**4.3 Testy kontrolera:**
- Test integracyjny z `@WebMvcTest` dla pomyślnego przypadku (200)
- Test dla przypadku błędów walidacji (422)
- Test dla przypadku nieprawidłowego przejścia statusu (422)
- Test dla przypadku gdy gra nie istnieje (404)
- Test dla przypadku gdy użytkownik nie jest uczestnikiem (403)
- Test dla przypadku bez tokenu (401)

### Krok 5: Implementacja obsługi błędów

**5.1 Utworzenie global exception handler:**
- Obsługa `MethodArgumentNotValidException` (422)
- Obsługa `ValidationException` (422)
- Obsługa `GameNotFoundException` (404)
- Obsługa `ForbiddenException` (403)
- Obsługa `DataAccessException` (500)
- Obsługa `DataIntegrityViolationException` (400/422)

**5.2 Testy exception handler:**
- Test dla każdego typu wyjątku
- Weryfikacja formatu odpowiedzi błędu

### Krok 6: Konfiguracja Swagger/OpenAPI

**6.1 Dodanie adnotacji Swagger:**
```java
@Operation(
    summary = "Update game status",
    description = "Updates the status of a game (surrender, abandon)"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Game status updated successfully"),
    @ApiResponse(responseCode = "403", description = "You are not a participant or invalid status transition"),
    @ApiResponse(responseCode = "404", description = "Game not found"),
    @ApiResponse(responseCode = "422", description = "Invalid status transition")
})
@PutMapping("/{gameId}/status")
public ResponseEntity<UpdateGameStatusResponse> updateGameStatus(...) {
    // ...
}
```

### Krok 7: Testy integracyjne i E2E

**7.1 Testy integracyjne:**
- Test pełnego przepływu z bazą danych
- Test z rzeczywistym tokenem JWT
- Test walidacji przejść statusu
- Test automatycznych aktualizacji (triggery)
- Test ustawiania zwycięzcy przy poddaniu

**7.2 Testy E2E (Cypress):**
- Test aktualizacji statusu gry (poddanie, porzucenie)
- Test obsługi błędów walidacji przejść statusu
- Test obsługi błędu 403 dla gry bez uczestnictwa

### Krok 8: Dokumentacja i code review

**8.1 Dokumentacja:**
- Aktualizacja README z informacjami o endpoincie
- Dokumentacja Swagger/OpenAPI
- Dokumentacja reguł przejść statusu

**8.2 Code review:**
- Sprawdzenie zgodności z zasadami implementacji
- Review bezpieczeństwa
- Weryfikacja obsługi błędów

### Krok 9: Wdrożenie i monitoring

**9.1 Wdrożenie:**
- Merge do głównej gałęzi przez PR
- Weryfikacja w środowisku deweloperskim
- Test z różnymi przejściami statusu

**9.2 Monitoring:**
- Konfiguracja metryk Prometheus
- Konfiguracja alertów dla wysokiego wskaźnika błędów
- Monitorowanie czasu odpowiedzi i przejść statusu

## 10. Podsumowanie

Plan implementacji endpointu **PUT /api/games/{gameId}/status** obejmuje kompleksowe podejście do wdrożenia z walidacją przejść statusu i autoryzacją uczestników. Kluczowe aspekty:

- **Bezpieczeństwo:** Uwierzytelnianie JWT, autoryzacja uczestników, walidacja przejść statusu
- **Wydajność:** Optymalizacja zapytań, transakcyjność, automatyczne aktualizacje przez triggery
- **Obsługa błędów:** Centralna obsługa z odpowiednimi kodami statusu
- **Logika biznesowa:** Walidacja przejść statusu, automatyczne ustawianie zwycięzcy przy poddaniu
- **Testowanie:** Testy jednostkowe, integracyjne i E2E
- **Dokumentacja:** Swagger/OpenAPI, dokumentacja reguł przejść statusu

Implementacja powinna być wykonywana krok po kroku zgodnie z sekcją "Etapy wdrożenia", z weryfikacją każdego etapu przed przejściem do następnego.
