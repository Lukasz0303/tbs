# API Endpoint Implementation Plan: GET /api/rankings

> **Status:** ⏳ Do implementacji

## 1. Przegląd punktu końcowego

GET /api/rankings - Pobranie globalnego rankingu graczy z paginacją. Endpoint zwraca listę zarejestrowanych użytkowników posortowanych według liczby punktów zdobytych w grze. Goście nie są uwzględniani w rankingu. Ranking przechowywany jest w materialized view `player_rankings` dla optymalizacji wydajności.

## 2. Szczegóły żądania

- **Metoda HTTP:** GET
- **URL:** `/api/rankings`
- **Autoryzacja:** Publiczne (bez wymaganej autoryzacji)

### Query Parameters
- `page` (optional, Integer, default: 0): Numer strony w paginacji
- `size` (optional, Integer, default: 50, max: 100): Rozmiar strony
- `startRank` (optional, Integer): Alternatywa dla paginacji - początkowa pozycja w rankingu

### Request Body
Brak

## 3. Wykorzystywane typy

- Response DTO: `RankingListResponse` (implements `PaginatedResponse<RankingItem>`)
- Item DTO: `RankingItem`

### RankingListResponse
```java
public record RankingListResponse(
    List<RankingItem> content,
    long totalElements,
    int totalPages,
    int size,
    int number,
    boolean first,
    boolean last
)
```

### RankingItem
```java
public record RankingItem(
    long rankPosition,
    long userId,
    String username,
    long totalPoints,
    int gamesPlayed,
    int gamesWon,
    Instant createdAt
)
```

## 4. Szczegóły odpowiedzi

- **200 OK** - Lista rankingu z paginacją

**Response Body:**
```json
{
  "content": [
    {
      "rankPosition": 1,
      "userId": 123,
      "username": "bestPlayer",
      "totalPoints": 10000,
      "gamesPlayed": 50,
      "gamesWon": 45,
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 3,
  "size": 50,
  "number": 0,
  "first": true,
  "last": false
}
```

## 5. Przepływ danych

1. **Kontroler** przyjmuje żądanie z query parametrami `page`, `size`, `startRank`
2. **Walidacja** parametrów wejściowych (size ≤ 100, page ≥ 0, startRank ≥ 0)
3. **Service Layer** (`RankingService`):
   - Sprawdza czy ranking jest aktualny (cache/refresh check)
   - Pobiera dane z materialized view `player_rankings`:
     - Jeśli podano `startRank`: używa OFFSET/LIMIT
     - Jeśli podano `page/size`: używa standardowej paginacji
   - Przelicza `totalPages` i flagi `first`/`last`
4. **DTO Mapping** - mapuje wyniki z bazy na `RankingListResponse`
5. **Odpowiedź** - zwraca `200 OK` z danymi

**Query SQL:**
```sql
SELECT rank_position, user_id, username, total_points, games_played, games_won, created_at
FROM player_rankings
ORDER BY rank_position
OFFSET :offset LIMIT :size;
```

## 6. Względy bezpieczeństwa

1. **Autoryzacja**: Endpoint publiczny - brak wymaganej autoryzacji. Ranking jest publiczną informacją
2. **Walidacja wejścia**:
   - `page` - walidacja jako nieujemna liczba całkowita (`@Min(0)`)
   - `size` - walidacja jako liczba w zakresie 1-100 (`@Min(1) @Max(100)`)
   - `startRank` - walidacja jako dodatnia liczba całkowita (`@Positive`)
3. **Ochrona przed injection**: Użycie prepared statements przez JPA
4. **Rate limiting**: Rozważenie dodania rate limitingu dla endpointów rankingowych
5. **Filtrowanie danych**: Goście (`is_guest = true`) są automatycznie filtrowani

## 7. Obsługa błędów

### 400 Bad Request
**Nieprawidłowy parametr size:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Size must be between 1 and 100",
  "path": "/api/rankings"
}
```

### 500 Internal Server Error
**Błąd bazy danych:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An error occurred while fetching ranking data",
  "path": "/api/rankings"
}
```

## 8. Rozważania dotyczące wydajności

1. **Materialized View**: `player_rankings` jest materialized view dla optymalizacji. Odświeżanie co 5-10 minut lub event-driven po zakończeniu gry
2. **Cache (Redis)**: Cache'owanie wyników z TTL 5-10 minut. Klucze: `ranking:page:{page}:size:{size}` lub `ranking:start:{startRank}:size:{size}`
3. **Indeksy**: Indeks na `rank_position` w `player_rankings`
4. **Connection pooling**: HikariCP connection pooler
5. **Monitoring**: Metryki Prometheus - `ranking_requests_total`, `ranking_query_duration_seconds`

## 9. Etapy wdrożenia

1. Utworzenie interfejsu `RankingService` i implementacji `RankingServiceImpl`
2. Utworzenie kontrolera `RankingController` z endpointem `GET /api/v1/rankings`
3. Implementacja metody `getRankings(page, size, startRank)` w service layer
4. Implementacja cache logic (Redis) w Service Layer
5. Dodanie walidacji parametrów przez Bean Validation
6. Dodanie adnotacji Swagger dla dokumentacji API
7. Implementacja obsługi błędów (exception handling)
8. Testy jednostkowe dla Service i Controller (JUnit 5, Mockito)
9. Testy integracyjne i E2E (Cypress)
10. Deployment i monitoring metryk w produkcji

## 10. Powiązane endpointy

- **GET /api/v1/rankings/{userId}** - Pobranie szczegółowej pozycji w rankingu dla użytkownika
- **GET /api/v1/rankings/around/{userId}** - Pobranie rankingów wokół użytkownika
- **DELETE /api/v1/rankings/cache** - Czyszczenie cache rankingów (zobacz: `clear-rankings-cache.md`)

