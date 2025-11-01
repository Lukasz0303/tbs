# API Endpoint Implementation Plan: WS /ws/game/{gameId}

> **Status:** ⏳ Do implementacji

## 1. Przegląd punktu końcowego

**WS /ws/game/{gameId}** to połączenie WebSocket umożliwiające komunikację w czasie rzeczywistym między dwoma graczami podczas rozgrywki PvP w Tic-Tac-Toe. Endpoint zapewnia dwukierunkową komunikację dla:
- Wysyłania ruchów gracza do serwera
- Otrzymywania aktualizacji o ruchach przeciwnika
- Monitorowania stanu gry (status, timer, zakończenie)
- Mechanizmów keep-alive (PING/PONG)
- Obsługi poddania gry

Endpoint jest kluczowy dla doświadczenia użytkownika w trybie PvP, zapewniając płynną rozgrywkę bez konieczności odświeżania strony i natychmiastową synchronizację stanu między graczami.

## 2. Szczegóły żądania

### Protokół
- **WebSocket** (ws:// lub wss:// dla TLS)

### Struktura URL
```
ws://{host}/ws/game/{gameId}
```

### Parametry URL
- `gameId` (Long, wymagane) - ID gry z tabeli `games.id`

### Subprotokół
- `game-protocol` - subprotokół do identyfikacji typu połączenia

### Nagłówki żądania WebSocket

**Wymagane:**
- `Authorization: Bearer <JWT_TOKEN>` - token JWT wydany po poprawnym logowaniu/rejestracji
- `Sec-WebSocket-Protocol: game-protocol` - subprotokół

### Autoryzacja i autentykacja

- Gracz musi być uczestnikiem gry (`games.player1_id` lub `games.player2_id`)
- Weryfikacja tokena JWT podczas handshake
- Weryfikacja uczestnictwa w grze przed zaakceptowaniem połączenia
- Typ gry musi być `PVP` (nie może to być gra vs bot)

### Przykład połączenia

```javascript
const ws = new WebSocket('ws://api.example.com/ws/game/42', 'game-protocol');
ws.setRequestHeader('Authorization', 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...');
```

## 3. Typy wiadomości

### Klient → Serwer

#### MOVE
Wysłanie ruchu gracza do serwera.

**`MoveMessage`**:
```json
{
  "type": "MOVE",
  "payload": {
    "row": 0,
    "col": 0,
    "playerSymbol": "x"
  }
}
```

**Walidacja:**
- `row`: Integer >= 0, < board_size
- `col`: Integer >= 0, < board_size
- `playerSymbol`: Enum ("x" lub "o")
- Pole musi być puste
- Gracz musi być aktualnym graczem

#### SURRENDER
Poddanie gry przez gracza.

**`SurrenderMessage`**:
```json
{
  "type": "SURRENDER",
  "payload": {}
}
```

**Reguły biznesowe:**
- Przeciwnik automatycznie wygrywa
- Gra kończy się ze statusem `FINISHED`
- Zwycięzca otrzymuje +1000 pkt

#### PING
Keep-alive - utrzymanie połączenia.

**`PingMessage`**:
```json
{
  "type": "PING",
  "payload": {
    "timestamp": "2024-01-20T15:30:00Z"
  }
}
```

### Serwer → Klient

#### MOVE_ACCEPTED
Ruch został zaakceptowany i zapisany.

**`MoveAcceptedMessage`**:
```json
{
  "type": "MOVE_ACCEPTED",
  "payload": {
    "moveId": 123,
    "row": 0,
    "col": 0,
    "playerSymbol": "x",
    "boardState": {
      "state": [
        ["x", "", ""],
        ["", "", ""],
        ["", "", ""]
      ]
    },
    "currentPlayerSymbol": "o",
    "nextMoveAt": "2024-01-20T15:30:10Z"
  }
}
```

#### MOVE_REJECTED
Ruch został odrzucony z powodu błędu walidacji.

**`MoveRejectedMessage`**:
```json
{
  "type": "MOVE_REJECTED",
  "payload": {
    "reason": "Invalid move: cell is already occupied",
    "code": "MOVE_INVALID_OCCUPIED"
  }
}
```

**Kody błędów:**
- `MOVE_INVALID_OCCUPIED` - Pole jest zajęte
- `MOVE_INVALID_OUT_OF_BOUNDS` - Ruch poza planszą
- `MOVE_INVALID_NOT_YOUR_TURN` - Nie twoja tura
- `MOVE_INVALID_GAME_NOT_ACTIVE` - Gra nie jest aktywna
- `MOVE_INVALID_TIMEOUT` - Przekroczono limit czasu

#### OPPONENT_MOVE
Przeciwnik wykonał ruch.

**`OpponentMoveMessage`**:
```json
{
  "type": "OPPONENT_MOVE",
  "payload": {
    "row": 1,
    "col": 1,
    "playerSymbol": "o",
    "boardState": {
      "state": [
        ["x", "", ""],
        ["", "o", ""],
        ["", "", ""]
      ]
    },
    "currentPlayerSymbol": "x",
    "nextMoveAt": "2024-01-20T15:30:20Z"
  }
}
```

#### GAME_UPDATE
Status gry się zmienił (np. gra zakończona).

**`GameUpdateMessage`**:
```json
{
  "type": "GAME_UPDATE",
  "payload": {
    "gameId": 42,
    "status": "finished",
    "winner": {
      "userId": 10,
      "username": "player1"
    },
    "boardState": {
      "state": [
        ["x", "x", "x"],
        ["o", "o", ""],
        ["", "", ""]
      ]
    }
  }
}
```

#### TIMER_UPDATE
Aktualizacja timera - pozostały czas na ruch.

**`TimerUpdateMessage`**:
```json
{
  "type": "TIMER_UPDATE",
  "payload": {
    "remainingSeconds": 8,
    "currentPlayerSymbol": "x"
  }
}
```

#### GAME_ENDED
Gra zakończona (wywoływane przed zamknięciem połączenia).

**`GameEndedMessage`**:
```json
{
  "type": "GAME_ENDED",
  "payload": {
    "gameId": 42,
    "status": "finished",
    "winner": {
      "userId": 10,
      "username": "player1"
    },
    "finalBoardState": {
      "state": [
        ["x", "x", "x"],
        ["o", "o", ""],
        ["", "", ""]
      ]
    },
    "totalMoves": 5
  }
}
```

#### PONG
Odpowiedź keep-alive.

**`PongMessage`**:
```json
{
  "type": "PONG",
  "payload": {
    "timestamp": "2024-01-20T15:30:00Z"
  }
}
```

## 4. Wykorzystywane typy

### DTO (Data Transfer Objects)

#### Request Messages
**`com.tbs.dto.websocket.BaseWebSocketMessage`** (interfejs bazowy)
- `com.tbs.dto.websocket.MoveMessage`
- `com.tbs.dto.websocket.SurrenderMessage`
- `com.tbs.dto.websocket.PingMessage`

#### Response Messages
- `com.tbs.dto.websocket.MoveAcceptedMessage`
- `com.tbs.dto.websocket.MoveRejectedMessage`
- `com.tbs.dto.websocket.OpponentMoveMessage`
- `com.tbs.dto.websocket.GameUpdateMessage`
- `com.tbs.dto.websocket.TimerUpdateMessage`
- `com.tbs.dto.websocket.GameEndedMessage`
- `com.tbs.dto.websocket.PongMessage`

#### Typy pomocnicze
- `com.tbs.dto.common.BoardState` - stan planszy
- `com.tbs.dto.user.WinnerInfo` - informacje o zwycięzcy
- `com.tbs.enums.GameStatus` - status gry
- `com.tbs.enums.PlayerSymbol` - symbol gracza (X/O)
- `com.tbs.enums.GameType` - typ gry (PVP/VS_BOT)

## 5. Przepływ danych

### 5.1. Nawiązanie połączenia (Handshake)

```
1. Klient inicjuje WebSocket connection do /ws/game/{gameId}
2. Serwer weryfikuje JWT token z nagłówka Authorization
3. Serwer sprawdza czy gracz jest uczestnikiem gry
4. Serwer sprawdza czy typ gry to PVP
5. Serwer sprawdza czy gra jest w statusie IN_PROGRESS
6. Jeśli wszystkie warunki spełnione, połączenie zostaje zaakceptowane
7. Gracz otrzymuje początkowy stan gry (GAME_UPDATE)
8. Rozpoczyna się timer (jeśli to tura gracza)
```

### 5.2. Przepływ ruchu

```
1. Klient wysyła MOVE z koordynatami (row, col, playerSymbol)
2. Serwer waliduje ruch:
   - Czy pole jest puste
   - Czy to tura gracza
   - Czy gra jest aktywna
   - Czy nie przekroczono limitu czasu
3a. Jeśli ruch poprawny:
   - Zapis ruchu do bazy (tabela moves)
   - Aktualizacja stanu gry (current_player_symbol, last_move_at)
   - Wygenerowanie nowego boardState
   - Sprawdzenie warunków wygranej/remisu
   - Wysłanie MOVE_ACCEPTED do gracza
   - Wysłanie OPPONENT_MOVE do przeciwnika
   - Aktualizacja timera dla nowego gracza
3b. Jeśli ruch niepoprawny:
   - Wysłanie MOVE_REJECTED z powodem i kodem błędu
4. Jeśli gra zakończona (wygrana/remis):
   - Aktualizacja gry w bazie (status, winner_id, finished_at)
   - Wywołanie triggera aktualizacji statystyk
   - Wysłanie GAME_ENDED do obu graczy
   - Zamknięcie połączeń WebSocket
```

### 5.3. Przepływ poddania

```
1. Klient wysyła SURRENDER
2. Serwer weryfikuje czy gracz jest uczestnikiem aktywnej gry
3. Serwer ustawia przeciwnika jako zwycięzcę
4. Aktualizacja gry w bazie (status=FINISHED, winner_id)
5. Wywołanie triggera aktualizacji statystyk (+1000 pkt dla zwycięzcy)
6. Wysłanie GAME_ENDED do obu graczy
7. Zamknięcie połączeń WebSocket
```

### 5.4. Przepływ timera

```
1. Po zaakceptowaniu ruchu, rozpoczyna się timer dla następnego gracza (10s)
2. Co 1 sekundę serwer wysyła TIMER_UPDATE z remainingSeconds
3. Jeśli timer osiągnie 0:
   - Ruch przeciwnika uznawany za niezwykony
   - Aktualny gracz wygrywa przez timeout
   - Aktualizacja gry (status=FINISHED, winner_id)
   - Wysłanie GAME_ENDED do obu graczy
   - Zamknięcie połączeń
```

### 5.5. Keep-Alive (PING/PONG)

```
1. Klient wysyła PING co 30 sekund (wolniejsze niż 10s timer)
2. Serwer odpowiada PONG z tym samym timestamp
3. Jeśli brak PONG przez 60s, serwer zamyka połączenie (timeout)
```

### 5.6. Obsługa rozłączeń

```
1. Klient traci połączenie (close, disconnect, timeout)
2. Serwer wykrywa zamknięcie połączenia WebSocket
3. Serwer rezerwuje 20 sekund na reconnect
4. Jeśli reconnect w ciągu 20s:
   - Gracz może kontynuować grę
   - Timer jest resetowany
5. Jeśli brak reconnect:
   - Gra kończy się (przeciwnik wygrywa przez timeout)
   - Aktualizacja gry i statystyk
   - Powiadomienie przeciwnika
```

## 6. Względy bezpieczeństwa

### 6.1. Autoryzacja i autentykacja

- **JWT Token Verification**: Weryfikacja tokena podczas handshake WebSocket
- **Game Participation Check**: Tylko uczestnicy gry mogą się połączyć
- **Game Type Validation**: Tylko gry PVP obsługują WebSocket
- **Session Management**: Każde połączenie jest powiązane z sesją użytkownika

### 6.2. Walidacja danych wejściowych

- **Coordinate Validation**: `row` i `col` muszą być w zakresie 0 to board_size-1
- **Turn Validation**: Gracz może wykonać ruch tylko w swojej turze
- **Game State Validation**: Ruchy dozwolone tylko dla `IN_PROGRESS`
- **Cell Occupancy Check**: Pole musi być puste

### 6.3. Ochrona przed nadużyciami

- **Rate Limiting**: Maksymalnie 1 ruch na 0.5s na gracza (zapobiega spamowaniu)
- **Connection Limiting**: Gracz może mieć tylko 1 aktywne połączenie WebSocket na grę
- **Timeout Protection**: Automatyczne zakończenie gry po przekroczeniu limitu czasu

### 6.4. Bezpieczeństwo transportu

- **WSS over TLS**: Produkcja używa wss:// (WebSocket Secure)
- **CORS Configuration**: Ograniczenie do dozwolonych domen
- **WebSocket Subprotocol**: Weryfikacja subprotokołu `game-protocol`

## 7. Obsługa błędów

### 7.1. Błędy podczas handshake

**Nieprawidłowy token JWT**
- Status: 401 Unauthorized
- Zamknięcie połączenia bez akceptacji

**Gracz nie jest uczestnikiem**
- Status: 403 Forbidden
- Zamknięcie połączenia

**Gra nie jest PVP**
- Status: 400 Bad Request
- Zamknięcie połączenia

**Gra nie jest aktywna**
- Status: 400 Bad Request
- Zamknięcie połączenia

### 7.2. Błędy podczas komunikacji

**MOVE_REJECTED - błędy walidacji**
- `MOVE_INVALID_OCCUPIED`: Pole jest zajęte
- `MOVE_INVALID_OUT_OF_BOUNDS`: Współrzędne poza planszą
- `MOVE_INVALID_NOT_YOUR_TURN`: Nie twoja tura
- `MOVE_INVALID_GAME_NOT_ACTIVE`: Gra zakończona lub porzucona
- `MOVE_INVALID_TIMEOUT`: Przekroczono limit czasu (10s)

**Nieprawidłowy format wiadomości**
- Ignorowanie wiadomości
- Logowanie błędu (bezpieczeństwo)
- Możliwa opcja wysłania ERROR message do klienta

**Błąd bazy danych**
- Logowanie błędów
- Wysłanie GAME_UPDATE z błędem
- Zamknięcie połączenia
- W razie potrzeby odwołanie transakcji

### 7.3. Timeouty

**Timeout ruchu (10s)**
- Automatyczne zakończenie gry
- Przeciwnik wygrywa
- Aktualizacja statystyk

**Timeout połączenia (60s bez PONG)**
- Zamknięcie połączenia
- Mechanizm 20s reconnect window
- Jeśli brak reconnect: przeciwnik wygrywa

## 8. Rozważania dotyczące wydajności

### 8.1. Zarządzanie połączeniami

- **Connection Pooling**: Efektywne zarządzanie połączeniami WebSocket
- **Session Storage**: Przechowywanie sesji w Redis dla skalowalności
- **WebSocket Cleanup**: Automatyczne zamknięcie połączeń po zakończeniu gry

### 8.2. Przetwarzanie wiadomości

- **Asynchronous Processing**: Przetwarzanie wiadomości asynchronicznie
- **Batch Updates**: Grupowanie aktualizacji timera (np. co 1s zamiast natychmiast)
- **Message Queue**: Użycie Redis pub/sub lub RabbitMQ dla wiadomości między węzłami

### 8.3. Optymalizacje bazy danych

- **Indexed Queries**: Indeksy na `games.id`, `games.player1_id`, `games.player2_id`, `moves.game_id`
- **Connection Pooling**: Użycie HikariCP dla PostgreSQL
- **Transaction Management**: Krótkie transakcje dla ruchów

### 8.4. Cache i sesje

- **Redis Session Store**: Sesje WebSocket w Redis dla skalowania poziomego
- **Game State Cache**: Cache'owanie stanu gry w Redis (opcjonalnie)
- **Timer Management**: Timer w pamięci lub Redis z TTL

### 8.5. Skalowanie

- **Load Balancing**: Wsparcie dla wielu instancji aplikacji (Session Affinity wymagane)
- **Shared State**: Użycie Redis dla współdzielonego stanu między węzłami
- **Message Broadcasting**: Pub/Sub w Redis dla rozgłaszania między węzłami

### 8.6. Monitoring i metryki

- **WebSocket Metrics**: Liczba aktywnych połączeń, czas życia połączenia
- **Message Metrics**: Liczba wiadomości na sekundę, opóźnienia przetwarzania
- **Game Metrics**: Czas trwania gry, średnia liczba ruchów, wskaźnik timeoutów

## 9. Etapy wdrożenia

### Etap 1: Infrastruktura WebSocket
1. Konfiguracja Spring WebSocket (WebSocketConfig)
2. Implementacja WebSocketHandler dla `/ws/game/{gameId}`
3. Konfiguracja subprotokołu `game-protocol`
4. Konfiguracja JWT authentication dla WebSocket
5. Podstawowy handshake z walidacją uczestnictwa

### Etap 2: Podstawowa komunikacja
6. Implementacja obsługi wiadomości (message handling)
7. Deserializacja komunikatów BaseWebSocketMessage
8. Serializacja odpowiedzi
9. Implementacja PING/PONG
10. Logging i monitoring podstawowy

### Etap 3: Logika gry
11. Implementacja obsługi MOVE
12. Walidacja ruchów (współrzędne, tura, stan gry)
13. Zapis ruchów do bazy danych
14. Generowanie boardState
15. Implementacja wykrywania wygranej/remisu
16. Implementacja SURRENDER

### Etap 4: Synchronizacja i timer
17. Implementacja timer (10s per move)
18. Wysyłanie TIMER_UPDATE co 1s
19. Obsługa timeout ruchu
20. Synchronizacja między graczami (OPPONENT_MOVE)
21. GAME_UPDATE i GAME_ENDED

### Etap 5: Obsługa błędów i rozłączeń
22. Implementacja MOVE_REJECTED z kodami błędów
23. Obsługa nieprawidłowych wiadomości
24. Implementacja 20s reconnect window
25. Timeout połączenia (60s PING timeout)
26. Auto-close na zakończenie gry

### Etap 6: Integracja i optymalizacja
27. Integracja z GameService, MoveService
28. Wywołanie triggerów bazy danych
29. Aktualizacja statystyk graczy
30. Cache'owanie stanu gry w Redis (opcjonalnie)
31. Pub/Sub w Redis dla message broadcasting (opcjonalnie)

### Etap 7: Testy i dokumentacja
32. Unit testy dla WebSocketHandler
33. Unit testy dla logiki gry
34. Integration testy dla całego przepływu
35. Testy E2E z Cypress (dwa połączenia WebSocket)
36. Testy wydajnościowe (100+ jednoczesnych gier)
37. Dokumentacja Swagger/OpenAPI dla WebSocket
38. Aktualizacja planu implementacji

### Etap 8: Deployment i monitoring
39. Konfiguracja produkcji (WSS, CORS)
40. Setup monitoring (metriki WebSocket)
41. Setup alerting (timeouts, błędne połączenia)
42. Load testing na produkcji
43. Deployment i weryfikacja

## 10. Testy

### 10.1. Unit Tests

**WebSocketHandlerTest**
- Test handshake z poprawnym JWT
- Test handshake z niepoprawnym JWT
- Test handshake dla nieuczestnika
- Test deserializacji wiadomości
- Test serializacji odpowiedzi

**GameMoveLogicTest**
- Test walidacji poprawnych ruchów
- Test walidacji niepoprawnych ruchów
- Test wykrywania wygranej
- Test wykrywania remisu
- Test zmiany tury

**TimerLogicTest**
- Test inicjalizacji timera
- Test aktualizacji timera
- Test timeout ruchu
- Test timeout połączenia

### 10.2. Integration Tests

**WebSocketIntegrationTest**
- Test pełnej rozgrywki PVP (2 połączenia)
- Test poddania
- Test timeout ruchu
- Test rozłączenia i reconnect
- Test jednoczesnych gier (100+)

### 10.3. E2E Tests (Cypress)

**GameplayE2ETest**
- Test rozpoczęcia gry PvP
- Test wykonania ruchów
- Test wygranej/przegranej
- Test poddania
- Test timera i timeout

### 10.4. Performance Tests

**LoadTest**
- 100 jednoczesnych gier PvP (200 WebSocket connections)
- Test opóźnień wiadomości
- Test zużycia pamięci/CPU
- Test skalowania z Redis

## 11. Dokumentacja

### 11.1. OpenAPI/Swagger

WebSocket endpoints nie są standardowo dokumentowane w OpenAPI, ale można dodać:
- Opis protokołu WebSocket
- Schematy wiadomości (MOVE, SURRENDER, etc.)
- Przykłady wiadomości
- Dokumentacja błędów i kodów

### 11.2. README

- Przykład połączenia WebSocket w JavaScript
- Lista wszystkich typów wiadomości
- Przykłady przepływów
- Troubleshooting guide

## 12. Uwagi dodatkowe

### 12.1. Konfiguracja

**application.properties**
```properties
# WebSocket
websocket.stomp.endpoint=/ws
websocket.game.subprotocol=game-protocol
websocket.ping.interval=30000
websocket.pong.timeout=60000
websocket.reconnect.window=20000

# Game
game.move.timeout=10000
game.pvp.max.concurrent=500
game.move.rate.limit=500
```

### 12.2. Redis Configuration (jeśli używany)

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.pub-sub.enabled=true
```

### 12.3. Dependency Injection

**Services:**
- `GameService` - logika gry, pobieranie danych
- `MoveService` - zarządzanie ruchami
- `UserService` - informacje o graczach
- `WebSocketSessionManager` - zarządzanie sesjami WebSocket

**Repositories:**
- `GameRepository` - dostęp do tabeli games
- `MoveRepository` - dostęp do tabeli moves

**Security:**
- `JwtTokenProvider` - weryfikacja JWT
- `SecurityContextHolder` - kontekst użytkownika

## 13. Roadmap przyszłości (Post-MVP)

### 13.1. Rozszerzenia funkcjonalności
- Obsługa kanałów czatu (chat między graczami)
- Obsługa undo/redo (z limitem)
- Replay gry
- Spectator mode (obserwowanie gier)

### 13.2. Optymalizacje
- Binary message format zamiast JSON (gdy liczba wiadomości wzrośnie)
- Compression WebSocket messages
- Adaptive quality dla różnych warunków sieci

### 13.3. Bezpieczeństwo
- Rate limiting per user
- Wykrywanie cheatowania (anomalie w czasach ruchów)
- Audyt log wszystkich ruchów

---

**Data utworzenia:** 2024-01-20  
**Ostatnia aktualizacja:** 2024-01-20  
**Autor:** AI Architecture Team