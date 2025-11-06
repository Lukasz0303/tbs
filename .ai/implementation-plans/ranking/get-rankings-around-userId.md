# API Endpoint Implementation Plan: GET /api/rankings/around/{userId}

> **Status:** ⏳ Do implementacji

## 1. Przegląd punktu końcowego

GET /api/rankings/around/{userId} - Pobranie rankingów wokół konkretnej pozycji użytkownika. Endpoint zwraca listę graczy znajdujących się przed i po użytkowniku w rankingu (określaną przez parametr `range`). Funkcjonalność przydatna dla wyświetlania kontekstu pozycji użytkownika w rankingu. Endpoint zwraca błąd 404 dla użytkowników-gosci.

## 2. Szczegóły żądania

- **Metoda HTTP:** GET
- **URL:** `/api/rankings/around/{userId}`
- **Autoryzacja:** Publiczne (bez wymaganej autoryzacji)

### Path Parameters
- `userId` (required, Long): Identyfikator użytkownika

### Query Parameters
- `range` (optional, Integer, default: 5, max: 10): Liczba graczy przed i po użytkowniku w rankingu

### Request Body
Brak

## 3. Wykorzystywane typy

- Response DTO: `RankingAroundResponse`
- Item DTO: `RankingAroundItem`

### RankingAroundResponse
```java
public record RankingAroundResponse(
    List<RankingAroundItem> items
)
```

### RankingAroundItem
```java
public record RankingAroundItem(
    long rankPosition,
    long userId,
    String username,
    long totalPoints,
    int gamesPlayed,
    int gamesWon
)
```

## 4. Szczegóły odpowiedzi

- **200 OK** - Lista rankingów wokół użytkownika

**Response Body:**
```json
{
  "items": [
    {
      "rankPosition": 40,
      "userId": 111,
      "username": "playerA",
      "totalPoints": 6000,
      "gamesPlayed": 35,
      "gamesWon": 25
    },
    {
      "rankPosition": 41,
      "userId": 222,
      "username": "playerB",
      "totalPoints": 5800,
      "gamesPlayed": 40,
      "gamesWon": 30
    },
    {
      "rankPosition": 42,
      "userId": 456,
      "username": "player123",
      "totalPoints": 5500,
      "gamesPlayed": 30,
      "gamesWon": 20
    },
    {
      "rankPosition": 43,
      "userId": 333,
      "username": "playerC",
      "totalPoints": 5200,
      "gamesPlayed": 25,
      "gamesWon": 15
    }
  ]
}
```

- **404 Not Found** - Użytkownik nie znaleziony lub użytkownik jest gościem

**Response Body:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User not found or user is a guest",
  "path": "/api/rankings/around/456"
}
```

## 5. Przepływ danych

1. **Kontroler** przyjmuje żądanie z `userId` i parametrem `range`
2. **Walidacja** - `userId` dodatnia, `range` między 1-10 (domyślnie 5)
3. **Service Layer** (`RankingService`):
   - Sprawdza czy użytkownik istnieje i nie jest gościem
   - Pobiera pozycję użytkownika z `player_rankings`
   - Jeśli użytkownik nie w rankingu → **404 Not Found**
   - Wyznacza zakres: `[rankPosition - range, rankPosition + range]`
   - Pobiera graczy w tym zakresie (sortowanie po `rank_position`)
4. **DTO Mapping** - mapuje wyniki na `RankingAroundResponse`
5. **Odpowiedź** - zwraca `200 OK` z danymi

**Query SQL:**
```sql
WITH user_rank AS (
    SELECT rank_position
    FROM player_rankings
    WHERE user_id = :userId
)
SELECT pr.rank_position, pr.user_id, pr.username, pr.total_points, pr.games_played, pr.games_won
FROM player_rankings pr, user_rank ur
WHERE pr.rank_position BETWEEN (ur.rank_position - :range) AND (ur.rank_position + :range)
ORDER BY pr.rank_position;
```

## 6. Względy bezpieczeństwa

1. **Autoryzacja**: Endpoint publiczny - ranking jest publiczną informacją
2. **Walidacja wejścia**:
   - `userId` - walidacja jako dodatnia liczba całkowita (`@Positive`)
   - `range` - walidacja jako liczba w zakresie 1-10 (`@Min(1) @Max(10)`)
3. **Ochrona przed injection**: Użycie prepared statements przez JPA
4. **Filtrowanie danych**: Goście (`is_guest = true`) są filtrowani - zwracany błąd 404
5. **Limitacja wyników**: Maksymalnie 21 graczy (range * 2 + 1) zwracanych w wyniku

## 7. Obsługa błędów

### 400 Bad Request
**Nieprawidłowy userId:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "User ID must be positive",
  "path": "/api/rankings/around/-1"
}
```

**Nieprawidłowy range:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Range must be between 1 and 10",
  "path": "/api/rankings/around/456"
}
```

### 404 Not Found
**Użytkownik nie znaleziony:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User not found",
  "path": "/api/rankings/around/99999"
}
```

**Użytkownik jest gościem:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User is a guest and not in ranking",
  "path": "/api/rankings/around/456"
}
```

### 500 Internal Server Error
**Błąd bazy danych:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An error occurred while fetching ranking around user",
  "path": "/api/rankings/around/456"
}
```

## 8. Rozważania dotyczące wydajności

1. **Materialized View**: `player_rankings` z indeksem na `rank_position` dla szybkiego zakresowego lookup
2. **Cache (Redis)**: Cache'owanie wyników z TTL 5-10 minut. Klucz: `ranking:around:{userId}:range:{range}`
3. **Indeksy**: Indeks na `rank_position` w `player_rankings`
4. **CTE Query**: Użycie CTE (Common Table Expression) dla efektywnego pobierania zakresu pozycji
5. **Connection pooling**: HikariCP connection pooler
6. **Monitoring**: Metryki Prometheus - `ranking_around_requests_total`, `ranking_around_query_duration_seconds`

## 9. Etapy wdrożenia

1. Utworzenie interfejsu `RankingService` i implementacji `RankingServiceImpl`
2. Utworzenie kontrolera `RankingController` z endpointem `GET /api/v1/rankings/around/{userId}`
3. Implementacja metody `getRankingsAround(userId, range)` w service layer
4. Implementacja sprawdzania czy użytkownik jest gościem
5. Implementacja CTE query dla pobierania zakresu graczy
6. Implementacja cache logic (Redis) w Service Layer
7. Dodanie walidacji parametrów przez Bean Validation
8. Dodanie adnotacji Swagger dla dokumentacji API
9. Implementacja obsługi błędów (EntityNotFoundException, 404 dla gości)
10. Testy jednostkowe dla Service i Controller (JUnit 5, Mockito)
11. Testy integracyjne i E2E (Cypress)

## 10. Powiązane endpointy

- **GET /api/v1/rankings** - Pobranie globalnego rankingu z paginacją (zobacz: `get-rankings.md`)
- **GET /api/v1/rankings/{userId}** - Pobranie szczegółowej pozycji w rankingu dla użytkownika (zobacz: `get-rankings-userId.md`)
- **DELETE /api/v1/rankings/cache** - Czyszczenie cache rankingów (zobacz: `clear-rankings-cache.md`)

