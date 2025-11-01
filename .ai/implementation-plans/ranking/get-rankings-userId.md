# API Endpoint Implementation Plan: GET /api/rankings/{userId}

> **Status:** ⏳ Do implementacji

## 1. Przegląd punktu końcowego

GET /api/rankings/{userId} - Pobranie szczegółowej pozycji w rankingu dla konkretnego użytkownika. Endpoint zwraca pozycję rankingową wraz ze statystykami użytkownika (punkty, rozegrane gry, wygrane). Endpoint zwraca błąd 404 dla użytkowników-gosci, którzy nie są uwzględnieni w rankingu.

## 2. Szczegóły żądania

- **Metoda HTTP:** GET
- **URL:** `/api/rankings/{userId}`
- **Autoryzacja:** Publiczne (bez wymaganej autoryzacji)

### Path Parameters
- `userId` (required, Long): Identyfikator użytkownika

### Query Parameters
Brak

### Request Body
Brak

## 3. Wykorzystywane typy

- Response DTO: `RankingDetailResponse`

### RankingDetailResponse
```java
public record RankingDetailResponse(
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

- **200 OK** - Pozycja w rankingu

**Response Body:**
```json
{
  "rankPosition": 42,
  "userId": 456,
  "username": "player123",
  "totalPoints": 5500,
  "gamesPlayed": 30,
  "gamesWon": 20,
  "createdAt": "2024-02-01T08:15:00Z"
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
  "path": "/api/rankings/456"
}
```

## 5. Przepływ danych

1. **Kontroler** przyjmuje żądanie z `userId` w ścieżce
2. **Walidacja** - `userId` musi być dodatnią liczbą całkowitą
3. **Service Layer** (`RankingService`):
   - Sprawdza czy użytkownik istnieje w tabeli `users` i nie jest gościem
   - Jeśli `is_guest = true` → **404 Not Found**
   - Pobiera pozycję użytkownika z materialized view `player_rankings`
   - Jeśli użytkownik nie ma pozycji w rankingu → **404 Not Found**
4. **DTO Mapping** - mapuje wyniki na `RankingDetailResponse`
5. **Odpowiedź** - zwraca `200 OK` z danymi

**Query SQL:**
```sql
SELECT rank_position, user_id, username, total_points, games_played, games_won, created_at
FROM player_rankings
WHERE user_id = :userId;
```

**Query sprawdzający użytkownika:**
```sql
SELECT id, is_guest
FROM users
WHERE id = :userId;
```

## 6. Względy bezpieczeństwa

1. **Autoryzacja**: Endpoint publiczny - ranking jest publiczną informacją
2. **Walidacja wejścia**: `userId` - walidacja jako dodatnia liczba całkowita (`@Positive`)
3. **Ochrona przed injection**: Użycie prepared statements przez JPA
4. **Filtrowanie danych**: Goście (`is_guest = true`) są filtrowani - zwracany błąd 404
5. **Ochrona prywatności**: Ujawnienie tylko pozycji i statystyk rankingowych (nie wrażliwych danych)

## 7. Obsługa błędów

### 400 Bad Request
**Nieprawidłowy userId:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "User ID must be positive",
  "path": "/api/rankings/-1"
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
  "path": "/api/rankings/99999"
}
```

**Użytkownik jest gościem:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User is a guest and not in ranking",
  "path": "/api/rankings/456"
}
```

### 500 Internal Server Error
**Błąd bazy danych:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An error occurred while fetching user ranking",
  "path": "/api/rankings/456"
}
```

## 8. Rozważania dotyczące wydajności

1. **Materialized View**: `player_rankings` z indeksem na `user_id` dla szybkiego lookup
2. **Cache (Redis)**: Cache'owanie wyników z TTL 5-10 minut. Klucz: `ranking:user:{userId}`
3. **Indeksy**: Indeks na `user_id` w `player_rankings` oraz `id` w `users`
4. **Connection pooling**: HikariCP connection pooler
5. **Monitoring**: Metryki Prometheus - `ranking_user_lookups_total`, `ranking_user_lookup_duration_seconds`

## 9. Etapy wdrożenia

1. Utworzenie interfejsu `RankingService` i implementacji `RankingServiceImpl`
2. Utworzenie kontrolera `RankingController` z endpointem `GET /api/rankings/{userId}`
3. Implementacja metody `getUserRanking(userId)` w service layer
4. Implementacja sprawdzania czy użytkownik jest gościem
5. Implementacja cache logic (Redis) w Service Layer
6. Dodanie walidacji parametrów przez Bean Validation
7. Dodanie adnotacji Swagger dla dokumentacji API
8. Implementacja obsługi błędów (EntityNotFoundException, 404 dla gości)
9. Testy jednostkowe dla Service i Controller (JUnit 5, Mockito)
10. Testy integracyjne i E2E (Cypress)

