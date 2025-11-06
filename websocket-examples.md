# Przykłady połączenia WebSocket

## Endpoint WebSocket
- **URL**: `ws://localhost:8080/ws/game/{gameId}`
- **Protokół**: WebSocket
- **Autoryzacja**: JWT token w nagłówku `Authorization: Bearer <token>`

## Token JWT (przykład)
```
eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiI2ZjA2NzQ3Mi1iMjQxLTRiY2ItOThhOS04OTQ5MzNmY2FiODYiLCJzdWIiOiIyIiwiaWF0IjoxNzYyMjA2Nzk4LCJleHAiOjE3NjIyMTAzOTh9.deF2Lxz41wbZqnz-26zYRk60BsQLuVJsvfkR4Ql7m1J5AuYEuyjoisvlNYJrOetPA1kdehWvFGKdgSAOxQt3tQ
```

## Przykłady połączenia

### 0. Dołączenie do kolejki matchmakingu (REST API)

**Endpoint:** `POST /api/v1/matching/queue`

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/matching/queue' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiJiMTk3ZDVhOC04NmNhLTQ5Y2ItODQ2My02NzNhZTY2MDgxYzciLCJzdWIiOiIxIiwiaWF0IjoxNzYyMjU0MTI3LCJleHAiOjE3NjIyNTc3Mjd9.tQZFj0jL5fZKFe3tXDd3SAZjKz1tuhUfLjc2yNtm7b80y0856v2iEfve6_zHThb9Qg-YXqUeLFmwj2_28VwP_g' \
  -d '{
  "boardSize": "THREE"
}'
```

**Endpoint:** `DELETE /api/v1/matching/queue`

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/matching/queue' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiJiMTk3ZDVhOC04NmNhLTQ5Y2ItODQ2My02NzNhZTY2MDgxYzciLCJzdWIiOiIxIiwiaWF0IjoxNzYyMjU0MTI3LCJleHAiOjE3NjIyNTc3Mjd9.tQZFj0jL5fZKFe3tXDd3SAZjKz1tuhUfLjc2yNtm7b80y0856v2iEfve6_zHThb9Qg-YXqUeLFmwj2_28VwP_g'
```

**Endpoint:** `POST /api/v1/matching/challenge/{userId}`

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/matching/challenge/1' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiJiMTk3ZDVhOC04NmNhLTQ5Y2ItODQ2My02NzNhZTY2MDgxYzciLCJzdWIiOiIxIiwiaWF0IjoxNzYyMjU0MTI3LCJleHAiOjE3NjIyNTc3Mjd9.tQZFj0jL5fZKFe3tXDd3SAZjKz1tuhUfLjc2yNtm7b80y0856v2iEfve6_zHThb9Qg-YXqUeLFmwj2_28VwP_g' \
  -d '{
  "boardSize": "THREE"
}'
```

### 1. Użycie `websocat` (Rust - najlepsze rozwiązanie)

**Instalacja:**
```bash
# Windows (via cargo)
cargo install websocat

# Linux/Mac
cargo install websocat
```

**Połączenie:**
```bash
websocat 'ws://localhost:8080/ws/game/1' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiI2ZjA2NzQ3Mi1iMjQxLTRiY2ItOThhOS04OTQ5MzNmY2FiODYiLCJzdWIiOiIyIiwiaWF0IjoxNzYyMjA2Nzk4LCJleHAiOjE3NjIyMTAzOTh9.deF2Lxz41wbZqnz-26zYRk60BsQLuVJsvfkR4Ql7m1J5AuYEuyjoisvlNYJrOetPA1kdehWvFGKdgSAOxQt3tQ'
```

### 2. Użycie `wscat` (Node.js)

**Instalacja:**
```bash
npm install -g wscat
```

**Połączenie:**
```bash
wscat -c 'ws://localhost:8080/ws/game/1' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiI2ZjA2NzQ3Mi1iMjQxLTRiY2ItOThhOS04OTQ5MzNmY2FiODYiLCJzdWIiOiIyIiwiaWF0IjoxNzYyMjA2Nzk4LCJleHAiOjE3NjIyMTAzOTh9.deF2Lxz41wbZqnz-26zYRk60BsQLuVJsvfkR4Ql7m1J5AuYEuyjoisvlNYJrOetPA1kdehWvFGKdgSAOxQt3tQ'
```

### 3. Użycie `curl` (tylko handshake - nie utrzymuje połączenia)

**Uwaga:** `curl` nie obsługuje natywnie WebSocket, ale może wykonać handshake. Do pełnego połączenia użyj `websocat` lub `wscat`.

```bash
curl -i -N \
  -H 'Connection: Upgrade' \
  -H 'Upgrade: websocket' \
  -H 'Sec-WebSocket-Version: 13' \
  -H 'Sec-WebSocket-Key: SGVsbG8sIHdvcmxkIQ==' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiI2ZjA2NzQ3Mi1iMjQxLTRiY2ItOThhOS04OTQ5MzNmY2FiODYiLCJzdWIiOiIyIiwiaWF0IjoxNzYyMjA2Nzk4LCJleHAiOjE3NjIyMTAzOTh9.deF2Lxz41wbZqnz-26zYRk60BsQLuVJsvfkR4Ql7m1J5AuYEuyjoisvlNYJrOetPA1kdehWvFGKdgSAOxQt3tQ' \
  http://localhost:8080/ws/game/1
```

### 4. Użycie JavaScript (przeglądarka)

```javascript
const token = 'eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiI2ZjA2NzQ3Mi1iMjQxLTRiY2ItOThhOS04OTQ5MzNmY2FiODYiLCJzdWIiOiIyIiwiaWF0IjoxNzYyMjA2Nzk4LCJleHAiOjE3NjIyMTAzOTh9.deF2Lxz41wbZqnz-26zYRk60BsQLuVJsvfkR4Ql7m1J5AuYEuyjoisvlNYJrOetPA1kdehWvFGKdgSAOxQt3tQ';
const gameId = 1;

const ws = new WebSocket(`ws://localhost:8080/ws/game/${gameId}`);

ws.onopen = () => {
  console.log('Połączono z WebSocket');
  
  // Przykład: Wysłanie ruchu
  ws.send(JSON.stringify({
    type: 'MOVE',
    payload: {
      row: 0,
      col: 0,
      playerSymbol: 'x'
    }
  }));
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('Otrzymano wiadomość:', message);
};

ws.onerror = (error) => {
  console.error('Błąd WebSocket:', error);
};

ws.onclose = () => {
  console.log('Połączenie zamknięte');
};
```

**Uwaga:** W przeglądrze nie można ustawić nagłówka `Authorization` bezpośrednio w konstruktorze `WebSocket`. Musisz użyć interceptor'a lub połączyć się przez `wscat`/`websocat` z nagłówkiem.

### 5. Użycie `websocat` z wysyłaniem wiadomości

```bash
# Wysłanie wiadomości MOVE
echo '{"type":"MOVE","payload":{"row":0,"col":0,"playerSymbol":"x"}}' | \
  websocat 'ws://localhost:8080/ws/game/1' \
    -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiI2ZjA2NzQ3Mi1iMjQxLTRiY2ItOThhOS04OTQ5MzNmY2FiODYiLCJzdWIiOiIyIiwiaWF0IjoxNzYyMjA2Nzk4LCJleHAiOjE3NjIyMTAzOTh9.deF2Lxz41wbZqnz-26zYRk60BsQLuVJsvfkR4Ql7m1J5AuYEuyjoisvlNYJrOetPA1kdehWvFGKdgSAOxQt3tQ'

# Wysłanie wiadomości PING
echo '{"type":"PING","payload":{"timestamp":"2024-01-20T15:30:00Z"}}' | \
  websocat 'ws://localhost:8080/ws/game/1' \
    -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiI2ZjA2NzQ3Mi1iMjQxLTRiY2ItOThhOS04OTQ5MzNmY2FiODYiLCJzdWIiOiIyIiwiaWF0IjoxNzYyMjA2Nzk4LCJleHAiOjE3NjIyMTAzOTh9.deF2Lxz41wbZqnz-26zYRk60BsQLuVJsvfkR4Ql7m1J5AuYEuyjoisvlNYJrOetPA1kdehWvFGKdgSAOxQt3tQ'

# Wysłanie wiadomości SURRENDER
echo '{"type":"SURRENDER","payload":{}}' | \
  websocat 'ws://localhost:8080/ws/game/1' \
    -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiI2ZjA2NzQ3Mi1iMjQxLTRiY2ItOThhOS04OTQ5MzNmY2FiODYiLCJzdWIiOiIyIiwiaWF0IjoxNzYyMjA2Nzk4LCJleHAiOjE3NjIyMTAzOTh9.deF2Lxz41wbZqnz-26zYRk60BsQLuVJsvfkR4Ql7m1J5AuYEuyjoisvlNYJrOetPA1kdehWvFGKdgSAOxQt3tQ'
```

## Przykłady wiadomości

### Wysłanie ruchu (MOVE)
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

### Wysłanie PING (keep-alive)
```json
{
  "type": "PING",
  "payload": {
    "timestamp": "2024-01-20T15:30:00Z"
  }
}
```

### Wysłanie SURRENDER (poddanie)
```json
{
  "type": "SURRENDER",
  "payload": {}
}
```

## Zamiana {gameId} na rzeczywiste ID

W przykładach używam `gameId = 1`. Zamień na rzeczywiste ID gry, np.:
- `ws://localhost:8080/ws/game/42` dla gry o ID 42
- `ws://localhost:8080/ws/game/123` dla gry o ID 123

