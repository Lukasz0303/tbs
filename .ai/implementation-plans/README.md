# Plany implementacji endpointów REST API

Ten katalog zawiera szczegółowe plany implementacji dla wszystkich endpointów REST API zgodnie ze specyfikacją w `api-plan.md`.

## Struktura katalogów

Plany są zorganizowane według kategorii endpointów:

- **`auth/`** - Endpointy uwierzytelniania
- **`guest/`** - Endpointy dla użytkowników gości
- **`user/`** - Endpointy zarządzania profilem użytkownika
- **`game/`** - Endpointy zarządzania grami
- **`move/`** - Endpointy zarządzania ruchami
- **`matchmaking/`** - Endpointy matchmakingu PvP
- **`ranking/`** - Endpointy rankingów
- **`health/`** - Endpointy zdrowia i monitorowania

## Lista planów implementacji

### Endpointy uwierzytelniania (`auth/`)

- ✅ **`get-auth-me.md`** - GET /api/auth/me - Pobranie profilu bieżącego użytkownika
- ⏳ **`post-auth-register.md`** - POST /api/auth/register - Rejestracja nowego konta
- ⏳ **`post-auth-login.md`** - POST /api/auth/login - Uwierzytelnienie i logowanie
- ⏳ **`post-auth-logout.md`** - POST /api/auth/logout - Wylogowanie użytkownika

### Endpointy użytkowników gości (`guest/`)

- ⏳ **`post-guests.md`** - POST /api/guests - Utworzenie lub pobranie profilu gościa

### Endpointy profilu użytkownika (`user/`)

- ⏳ **`get-users-userId.md`** - GET /api/users/{userId} - Pobranie profilu użytkownika po ID
- ⏳ **`put-users-userId.md`** - PUT /api/users/{userId} - Aktualizacja profilu użytkownika
- ⏳ **`post-users-userId-last-seen.md`** - POST /api/users/{userId}/last-seen - Aktualizacja ostatniej aktywności

### Endpointy zarządzania grami (`game/`)

- ⏳ **`post-games.md`** - POST /api/games - Utworzenie nowej gry
- ⏳ **`get-games.md`** - GET /api/games - Lista gier dla bieżącego użytkownika z filtrowaniem i paginacją
- ⏳ **`get-games-gameId.md`** - GET /api/games/{gameId} - Pobranie szczegółów gry
- ⏳ **`put-games-gameId-status.md`** - PUT /api/games/{gameId}/status - Aktualizacja statusu gry
- ⏳ **`get-games-gameId-board.md`** - GET /api/games/{gameId}/board - Pobranie stanu planszy

### Endpointy zarządzania ruchami (`move/`)

- ⏳ **`post-games-gameId-moves.md`** - POST /api/games/{gameId}/moves - Wykonanie ruchu
- ⏳ **`get-games-gameId-moves.md`** - GET /api/games/{gameId}/moves - Pobranie wszystkich ruchów
- ⏳ **`post-games-gameId-bot-move.md`** - POST /api/games/{gameId}/bot-move - Wykonanie ruchu bota (wewnętrzne)

### Endpointy matchmakingu PvP (`matchmaking/`)

- ⏳ **`post-matching-queue.md`** - POST /api/matching/queue - Dołączenie do kolejki matchmakingu
- ⏳ **`delete-matching-queue.md`** - DELETE /api/matching/queue - Opuszczenie kolejki matchmakingu
- ⏳ **`get-matching-queue.md`** - GET /api/matching/queue - Pobranie listy wszystkich graczy w kolejce wraz ze statusem
- ⏳ **`post-matching-challenge-userId.md`** - POST /api/matching/challenge/{userId} - Wyzwanie konkretnego gracza

### Endpointy rankingów (`ranking/`)

- ⏳ **`get-rankings.md`** - GET /api/rankings - Pobranie globalnego rankingu
- ⏳ **`get-rankings-userId.md`** - GET /api/rankings/{userId} - Pobranie pozycji w rankingu dla użytkownika
- ⏳ **`get-rankings-around-userId.md`** - GET /api/rankings/around/{userId} - Pobranie rankingów wokół pozycji użytkownika

### Endpointy WebSocket (`websocket/`)

- ⏳ **`ws-game-gameId.md`** - WS /ws/game/{gameId} - Połączenie WebSocket dla rozgrywki PvP w czasie rzeczywistym

### Endpointy zdrowia i monitorowania (`health/`)

- ⏳ **`get-actuator-health.md`** - GET /actuator/health - Sprawdzenie zdrowia aplikacji
- ⏳ **`get-actuator-metrics.md`** - GET /actuator/metrics - Metryki aplikacji (format Prometheus)

## Legenda statusów

- ✅ **Gotowe** - Plan implementacji jest kompletny i gotowy do użycia (wszystkie sekcje uzupełnione)
- ⏳ **Szablon utworzony** - Plik został utworzony z podstawową strukturą, wymaga uzupełnienia szczegółów zgodnie z `api-plan.md`

## Jak używać planów

Każdy plan implementacji zawiera:

1. **Przegląd punktu końcowego** - Opis celu i funkcjonalności
2. **Szczegóły żądania** - Metoda HTTP, URL, nagłówki, parametry
3. **Wykorzystywane typy** - DTOs, modele domenowe, wyjątki
4. **Szczegóły odpowiedzi** - Kody statusu, przykłady odpowiedzi
5. **Przepływ danych** - Sekwencja operacji, integracja z bazą danych
6. **Względy bezpieczeństwa** - Uwierzytelnianie, autoryzacja, ochrona przed atakami
7. **Obsługa błędów** - Scenariusze błędów i obsługa
8. **Rozważania dotyczące wydajności** - Optymalizacja, cache, rate limiting
9. **Etapy wdrożenia** - Szczegółowe kroki implementacji

**Uwaga:** Większość plików zawiera tylko szablon z podstawową strukturą. Szczegóły implementacji należy uzupełnić zgodnie ze specyfikacją w `api-plan.md`, wzorując się na pełnym planie dla `get-auth-me.md`.

Plany są zgodne z:
- Specyfikacją API w `api-plan.md`
- Schematem bazy danych w `db-plan.md`
- Zasadami implementacji (Spring Boot, clean code, security)
- Stackiem technologicznym projektu

## Dodawanie nowych planów

Przy dodawaniu nowego planu implementacji:

1. Utwórz plik w odpowiednim katalogu kategorii
2. Nazwij plik zgodnie z konwencją: `<metoda>-<ścieżka>.md` (np. `get-auth-me.md`)
3. Użyj szablonu z istniejącego planu jako punktu wyjścia
4. Zaktualizuj ten plik README, dodając nowy wpis do listy

