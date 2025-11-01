# API Endpoint Implementation Plan: GET /api/games/{gameId}/moves

> **Status:** ⏳ Do implementacji

## 1. Przegląd punktu końcowego

**GET /api/games/{gameId}/moves** to endpoint służący do pobrania wszystkich ruchów dla określonej gry. Endpoint wymaga uwierzytelnienia i zwraca tylko ruchy z gier, w których użytkownik jest uczestnikiem.

Endpoint umożliwia:
- Pobranie historii wszystkich ruchów w grze
- Wyświetlenie stanu planszy na podstawie historii ruchów
- Analizę przebiegu gry
- Odtworzenie sekwencji ruchów (replay)

**Autoryzacja:** Wymagane uwierzytelnienie (token JWT). Gracz musi być uczestnikiem gry (`player1_id` lub `player2_id`).

## 2. Szczegóły żądania

### Metoda HTTP
- **GET** - operacja tylko do odczytu, idempotentna

### Struktura URL
```
GET /api/games/{gameId}/moves
```

### Nagłówki żądania

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT wydany po poprawnym logowaniu/rejestracji

**Opcjonalne:**
- `Accept: application/json` - preferowany format odpowiedzi

### Parametry URL
- `gameId` (Long, wymagane) - ID gry, dla której pobierane są ruchy

### Query Parameters
- Brak parametrów zapytania (wszystkie ruchy są zwracane)

### Request Body
- Brak ciała żądania (metoda GET)

### Przykład żądania
```http
GET /api/games/42/moves HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Accept: application/json
```

## 3. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request DTO
- Brak - metoda GET używa parametrów URL

#### Response DTO
**`com.tbs.dto.move.MoveListItem`** (istniejący) - lista ruchów jako tablica
```java
public record MoveListItem(
    long moveId,
    int row,
    int col,
    PlayerSymbol playerSymbol,
    int moveOrder,
    Long playerId,
    String playerUsername,
    Instant createdAt
) {}
```

**Uwagi implementacyjne:**
- Ruchy są zwracane jako tablica `MoveListItem[]`, posortowane według `moveOrder` rosnąco
- `playerId` i `playerUsername` = null dla ruchów bota
- `playerId` i `playerUsername` = informacje o graczu dla ruchów graczy

**Przykład struktury odpowiedzi:**
```json
[
  {
    "moveId": 120,
    "row": 0,
    "col": 0,
    "playerSymbol": "x",
    "moveOrder": 1,
    "playerId": 10,
    "playerUsername": "player1",
    "createdAt": "2024-01-20T15:28:00Z"
  },
  {
    "moveId": 121,
    "row": 1,
    "col": 1,
    "playerSymbol": "o",
    "moveOrder": 2,
    "playerId": null,
    "playerUsername": null,
    "createdAt": "2024-01-20T15:29:00Z"
  }
]
```

### Enums

**`com.tbs.enums.PlayerSymbol`** (istniejący)
- `X("x")`, `O("o")`

### Modele domenowe (do stworzenia)

- **`com.tbs.model.Game`** - encja JPA/Hibernate dla tabeli `games`
- **`com.tbs.model.Move`** - encja JPA/Hibernate dla tabeli `moves`
- **`com.tbs.model.User`** - encja JPA/Hibernate dla tabeli `users`

### Wyjątki (do stworzenia lub wykorzystania)

- **`com.tbs.exception.GameNotFoundException`** - wyjątek dla 404 Not Found
- **`com.tbs.exception.UnauthorizedException`** - wyjątek dla 401 Unauthorized
- **`com.tbs.exception.ForbiddenException`** - wyjątek dla 403 Forbidden

### Serwisy (do stworzenia lub wykorzystania)

- **`com.tbs.service.MoveService`** - serwis zarządzający ruchami
  - Metoda: `List<MoveListItem> getMovesByGameId(Long gameId, Long userId)`
- **`com.tbs.service.GameService`** - serwis zarządzający grami
  - Metody: `getGameById()`, `isUserParticipant()`
- **`com.tbs.service.AuthenticationService`** - wyodrębnianie bieżącego użytkownika
  - Metody: `getCurrentUserId()`, `getCurrentUser()`

### Repozytoria (do stworzenia)

- **`com.tbs.repository.GameRepository`** - JPA Repository dla `Game`
  - Metody: `findById()`
- **`com.tbs.repository.MoveRepository`** - JPA Repository dla `Move`
  - Metody: `findByGameIdOrderByMoveOrderAsc()`, z JOIN do `User` dla username
- **`com.tbs.repository.UserRepository`** - JPA Repository dla `User`
  - Metody: `findById()`

## 4. Szczegóły odpowiedzi

### Kod statusu sukcesu

**200 OK** - Lista ruchów pobrana pomyślnie

**Przykład odpowiedzi:**
```json
[
  {
    "moveId": 120,
    "row": 0,
    "col": 0,
    "playerSymbol": "x",
    "moveOrder": 1,
    "playerId": 10,
    "playerUsername": "player1",
    "createdAt": "2024-01-20T15:28:00Z"
  },
  {
    "moveId": 121,
    "row": 1,
    "col": 1,
    "playerSymbol": "o",
    "moveOrder": 2,
    "playerId": null,
    "playerUsername": null,
    "createdAt": "2024-01-20T15:29:00Z"
  },
  {
    "moveId": 122,
    "row": 0,
    "col": 1,
    "playerSymbol": "x",
    "moveOrder": 3,
    "playerId": 10,
    "playerUsername": "player1",
    "createdAt": "2024-01-20T15:30:00Z"
  }
]
```

**Uwagi:**
- Ruchy są posortowane według `moveOrder` rosnąco (kolejność chronologiczna)
- Ruchy bota mają `playerId` i `playerUsername` = null
- Ruchy graczy zawierają informacje o graczu (`playerId`, `playerUsername`)

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

**403 Forbidden** - Nie jesteś uczestnikiem gry
```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "You are not a participant of this game",
    "details": "Only game participants can view moves"
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

1. **Odebranie żądania HTTP GET /api/games/{gameId}/moves**
   - Wyodrębnienie `gameId` z URL

2. **Uwierzytelnienie użytkownika**
   - Wyodrębnienie tokenu JWT z nagłówka `Authorization`
   - Walidacja tokenu JWT przez Spring Security
   - Pobranie bieżącego użytkownika z tokenu
   - Jeśli brak tokenu lub nieprawidłowy → 401 Unauthorized

3. **Pobranie gry z bazy danych**
   - Zapytanie: `SELECT * FROM games WHERE id = ?`
   - Jeśli gra nie istnieje → 404 Not Found
   - Sprawdzenie czy użytkownik jest uczestnikiem (player1_id lub player2_id)
   - Jeśli użytkownik nie jest uczestnikiem → 403 Forbidden

4. **Pobranie ruchów z bazy danych**
   - Zapytanie z JOIN do tabeli `users`:
     ```sql
     SELECT m.*, u.id as player_id, u.username as player_username
     FROM moves m
     LEFT JOIN users u ON m.player_id = u.id
     WHERE m.game_id = ?
     ORDER BY m.move_order ASC
     ```
   - Pobranie danych gracza (username) dla każdego ruchu (jeśli `player_id IS NOT NULL`)
   - Sortowanie według `move_order` rosnąco (kolejność chronologiczna)

5. **Mapowanie do DTO**
   - Mapowanie encji `Move` → `MoveListItem` DTO dla każdego rekordu
   - `playerId` = `move.playerId` (może być null dla ruchów bota)
   - `playerUsername` = `user.username` (może być null dla ruchów bota)

6. **Zwrócenie odpowiedzi HTTP 200 OK**
    - Serializacja listy `MoveListItem[]` do JSON

### Integracja z bazą danych

**Tabela: `moves`**
- SELECT z LEFT JOIN do `users` dla pobrania username gracza
- ORDER BY `move_order ASC` dla zachowania kolejności chronologicznej
- Filtrowanie według `game_id`

**Tabela: `users`**
- LEFT JOIN dla pobrania username gracza (tylko dla ruchów graczy, nie botów)

**Tabela: `games`**
- SELECT dla walidacji uczestnictwa i sprawdzenia istnienia gry

## 6. Względy bezpieczeństwa

### Uwierzytelnianie i autoryzacja

**Wymagane uwierzytelnienie:**
- Endpoint wymaga tokenu JWT w nagłówku `Authorization: Bearer <token>`
- Token JWT jest walidowany przez Spring Security
- Bieżący użytkownik jest wyodrębniany z tokenu JWT

**Autoryzacja:**
- Użytkownik musi być uczestnikiem gry (`player1_id` lub `player2_id`)
- Tylko uczestnicy gry mogą zobaczyć ruchy

**Ochrona przed:**
- Przeglądaniem ruchów z nie swoich gier → 403 Forbidden
- Wyciekiem informacji o innych grach → 403 Forbidden

### Ochrona przed atakami

**Row Level Security (RLS):**
- Polityki RLS w Supabase kontrolują dostęp do tabel `games` i `moves`
- Zarejestrowani użytkownicy: dostęp tylko do swoich gier (polityki `games_select_authenticated`, `moves_select_authenticated`)

**Injection Attacks:**
- Użycie JPA/Hibernate z parametryzowanymi zapytaniami (zapobiega SQL injection)

**Rate Limiting:**
- Opcjonalnie: rate limiting na endpoint (np. 100 żądań na minutę na użytkownika)

## 7. Obsługa błędów

### Scenariusze błędów

| Scenariusz | Kod HTTP | Wyjątek | Komunikat |
|------------|----------|---------|-----------|
| Brak tokenu JWT | 401 | `UnauthorizedException` | "Authentication required" |
| Nieprawidłowy token JWT | 401 | `UnauthorizedException` | "Invalid or expired token" |
| Gra nie istnieje | 404 | `GameNotFoundException` | "Game not found" |
| Użytkownik nie jest uczestnikiem | 403 | `ForbiddenException` | "You are not a participant of this game" |
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
- Indeks na `games.id` dla szybkiego sprawdzania istnienia gry

**Zapytania:**
- Użycie LEFT JOIN zamiast wielokrotnych zapytań (pobranie username gracza w jednym zapytaniu)
- Sortowanie w bazie danych (`ORDER BY move_order ASC`) zamiast sortowania w Javie

**Caching:**
- Opcjonalnie: cache historii ruchów w Redis dla zakończonych gier (TTL: 1 godzina)
- Cache danych gry (TTL: 30 sekund) dla aktywnych gier

### Transakcje i blokowanie

**Strategia transakcji:**
- `@Transactional(readOnly = true)` dla GET endpointów
- Izolacja transakcji: `READ_COMMITTED`

### Paginacja (opcjonalnie, dla przyszłości)

**Jeśli liczba ruchów wzrośnie:**
- Dodanie parametrów paginacji: `page`, `size`
- Zmiana odpowiedzi na `PaginatedResponse<MoveListItem>`
- Limit domyślny: 100 ruchów na stronę

## 9. Etapy wdrożenia

1. **Utworzenie modeli domenowych (JPA Entities)**
   - `com.tbs.model.Game` - encja JPA dla tabeli `games`
   - `com.tbs.model.Move` - encja JPA dla tabeli `moves`
   - `com.tbs.model.User` - encja JPA dla tabeli `users` (jeśli nie istnieje)

2. **Utworzenie repozytoriów (JPA Repositories)**
   - `com.tbs.repository.GameRepository` extends `JpaRepository<Game, Long>`
   - `com.tbs.repository.MoveRepository` extends `JpaRepository<Move, Long>`
     - Metoda: `@Query("SELECT m FROM Move m LEFT JOIN FETCH m.player WHERE m.gameId = ?1 ORDER BY m.moveOrder ASC") List<Move> findByGameIdOrderByMoveOrderAsc(Long gameId)`
   - `com.tbs.repository.UserRepository` extends `JpaRepository<User, Long>` (jeśli nie istnieje)

3. **Utworzenie wyjątków**
   - `com.tbs.exception.GameNotFoundException`
   - `com.tbs.exception.ForbiddenException`
   - `com.tbs.exception.UnauthorizedException`

4. **Utworzenie serwisu `MoveService`**
   - Metoda: `List<MoveListItem> getMovesByGameId(Long gameId, Long userId)`
     - Walidacja uczestnictwa
     - Pobranie ruchów z JOIN users
     - Mapowanie do DTO

5. **Utworzenie serwisu `GameService`** (jeśli nie istnieje)
   - Metody: `getGameById()`, `isUserParticipant()`

6. **Utworzenie kontrolera `MoveController`**
   - Endpoint: `@GetMapping` dla GET /api/games/{gameId}/moves
   - Dokumentacja Swagger: `@Operation`, `@ApiResponse`

7. **Konfiguracja Spring Security**
   - Dodanie endpointu do konfiguracji bezpieczeństwa

8. **Implementacja obsługi błędów**
   - Utworzenie `GlobalExceptionHandler` (jeśli nie istnieje)
   - Mapowanie wyjątków na kody HTTP

9. **Unit testy dla `MoveService`**
   - Test: `getMovesByGameId()` - pomyślne pobranie ruchów
   - Test: `getMovesByGameId()` - użytkownik nie jest uczestnikiem

10. **Testy integracyjne dla `MoveController`**
    - Test: GET /api/games/{gameId}/moves - pomyślne pobranie ruchów (200)
    - Test: GET /api/games/{gameId}/moves - użytkownik nie jest uczestnikiem (403)
    - Test: GET /api/games/{gameId}/moves - gra nie istnieje (404)

11. **Dokumentacja API**
    - Konfiguracja Swagger/OpenAPI
    - Aktualizacja dokumentacji

