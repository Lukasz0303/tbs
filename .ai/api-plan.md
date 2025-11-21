# Plan REST API

> **Uwaga dot. UI:** Całe środowisko frontendowe korzysta z motywu PrimeNG Verona (`https://verona.primeng.org/`). Każdy endpoint, przykład payloadu i scenariusz integracyjny zakłada spójność wizualną i interakcji z tym motywem w każdym widoku.

## 1. Zasoby

### 1.1 Zasób Users
**Tabela bazy danych**: `users`

Reprezentuje profile graczy, obsługując zarówno zarejestrowanych użytkowników, jak i graczy gości identyfikowanych przez adres IP.

### 1.2 Zasób Games
**Tabela bazy danych**: `games`

Reprezentuje sesje gier, ujednoliconym model dla obu typów gier: vs_bot i pvp.

### 1.3 Zasób Moves
**Tabela bazy danych**: `moves`

Reprezentuje indywidualne ruchy/akcje w grach. Stan planszy jest generowany dynamicznie z historii ruchów.

### 1.4 Zasób Rankings (Widok Materialized)
**Tabela bazy danych**: `player_rankings` (materialized view)

Reprezentuje globalny ranking z pre-obliczonymi pozycjami tylko dla zarejestrowanych użytkowników.

### 1.5 Zasób Auth (Spring Security JWT)
**Baza danych**: `users`

Uwierzytelnianie i autoryzacja zarządzane są wewnętrznie przez Spring Security. Hasła są przechowywane w tabeli `users` w postaci hashy (BCrypt), a tokeny dostępu/odświeżania JWT są generowane przez backend i przechowywane w httpOnly cookie.

---

## 2. Punkty końcowe

### 2.1 Punkty końcowe uwierzytelniania

#### POST /api/auth/register
**Opis**: Rejestracja nowego konta użytkownika
**Autoryzacja**: Publiczne

**Ciało żądania**:
```json
{
  "email": "string",
  "password": "string",
  "username": "string"
}
```

**Walidacja**:
- Email musi być w poprawnym formacie
- Hasło musi spełniać wymagania bezpieczeństwa (minimalna długość)
- Nazwa użytkownika musi być unikalna i spełniać wymagania formatu

**Odpowiedź sukcesu** (201 Created):
```json
{
  "userId": "string (UUID)",
  "username": "string",
  "email": "string",
  "isGuest": false,
  "totalPoints": 0,
  "gamesPlayed": 0,
  "gamesWon": 0,
  "authToken": "string (JWT)"
}
```

**Odpowiedzi błędów**:
- 400 Bad Request: Nieprawidłowe dane wejściowe
- 409 Conflict: Nazwa użytkownika lub email już istnieje
- 422 Unprocessable Entity: Błędy walidacji

---

#### POST /api/auth/login
**Opis**: Uwierzytelnienie i logowanie użytkownika
**Autoryzacja**: Publiczne

**Ciało żądania**:
```json
{
  "email": "string",
  "password": "string"
}
```

**Odpowiedź sukcesu** (200 OK):
```json
{
  "userId": "string (UUID)",
  "username": "string",
  "email": "string",
  "isGuest": false,
  "totalPoints": 0,
  "gamesPlayed": 0,
  "gamesWon": 0,
  "authToken": "string (JWT)"
}
```

**Odpowiedzi błędów**:
- 401 Unauthorized: Nieprawidłowe dane uwierzytelniające
- 404 Not Found: Użytkownik nie znaleziony

---

#### POST /api/auth/logout
**Opis**: Wylogowanie bieżącego użytkownika
**Autoryzacja**: Wymagane uwierzytelnienie

**Odpowiedź sukcesu** (200 OK):
```json
{
  "message": "Wylogowano pomyślnie"
}
```

---

#### GET /api/auth/me
**Opis**: Pobranie profilu bieżącego użytkownika
**Autoryzacja**: Wymagane uwierzytelnienie

**Odpowiedź sukcesu** (200 OK):
```json
{
  "userId": "number",
  "username": "string",
  "isGuest": false,
  "totalPoints": 0,
  "gamesPlayed": 0,
  "gamesWon": 0,
  "createdAt": "string (ISO 8601)",
  "lastSeenAt": "string (ISO 8601)"
}
```

**Odpowiedzi błędów**:
- 401 Unauthorized: Brak uwierzytelnienia

---

### 2.2 Punkty końcowe użytkowników gości

#### POST /api/guests
**Opis**: Utworzenie lub pobranie profilu użytkownika gościa identyfikowanego przez IP
**Autoryzacja**: Publiczne

**Ciało żądania**:
```json
{
  "ipAddress": "string (opcjonalne, wyciągnięte z żądania jeśli nie podano)"
}
```

**Odpowiedź sukcesu** (200 OK lub 201 Created):
```json
{
  "userId": "number",
  "isGuest": true,
  "totalPoints": 0,
  "gamesPlayed": 0,
  "gamesWon": 0,
  "createdAt": "string (ISO 8601)"
}
```

**Odpowiedzi błędów**:
- 400 Bad Request: Nieprawidłowy adres IP

---

### 2.3 Punkty końcowe profilu użytkownika

#### GET /api/v1/users/{userId}
**Opis**: Pobranie profilu użytkownika po ID
**Autoryzacja**: Publiczne (zarejestrowani użytkownicy), Wymagane uwierzytelnienie (własny profil)

**Parametry zapytania**: Brak

**Odpowiedź sukcesu** (200 OK):
```json
{
  "userId": "number",
  "username": "string (null dla gości)",
  "isGuest": false,
  "totalPoints": 0,
  "gamesPlayed": 0,
  "gamesWon": 0,
  "createdAt": "string (ISO 8601)"
}
```

**Odpowiedzi błędów**:
- 404 Not Found: Użytkownik nie znaleziony

---

#### PUT /api/v1/users/{userId}
**Opis**: Aktualizacja profilu użytkownika
**Autoryzacja**: Wymagane uwierzytelnienie (tylko własny profil)

**Ciało żądania**:
```json
{
  "username": "string (opcjonalne, tylko dla zarejestrowanych użytkowników)"
}
```

**Odpowiedź sukcesu** (200 OK):
```json
{
  "userId": "number",
  "username": "string",
  "isGuest": false,
  "totalPoints": 0,
  "gamesPlayed": 0,
  "gamesWon": 0,
  "updatedAt": "string (ISO 8601)"
}
```

**Odpowiedzi błędów**:
- 403 Forbidden: Nie można aktualizować profilu innego użytkownika
- 404 Not Found: Użytkownik nie znaleziony
- 409 Conflict: Nazwa użytkownika już istnieje

---

#### POST /api/v1/users/{userId}/last-seen
**Opis**: Aktualizacja znacznika czasu ostatniej aktywności użytkownika (dla matchmakingu)
**Autoryzacja**: Wymagane uwierzytelnienie (tylko własny profil)

**Odpowiedź sukcesu** (200 OK):
```json
{
  "message": "Ostatnia aktywność zaktualizowana pomyślnie",
  "lastSeenAt": "string (ISO 8601)"
}
```

---

### 2.4 Punkty końcowe zarządzania grami

#### POST /api/games
**Opis**: Utworzenie nowej gry (vs_bot lub pvp)
**Autoryzacja**: Wymagane uwierzytelnienie

**Ciało żądania**:
```json
{
  "gameType": "vs_bot | pvp",
  "boardSize": 3 | 4 | 5,
  "botDifficulty": "easy | medium | hard (wymagane jeśli gameType to vs_bot)"
}
```

**Walidacja**:
- `boardSize` musi być 3, 4 lub 5
- Dla vs_bot: `botDifficulty` jest wymagane, `player2_id` jest null
- Dla pvp: `botDifficulty` jest null, `player2_id` zostanie przypisane

**Odpowiedź sukcesu** (201 Created):
```json
{
  "gameId": "number",
  "gameType": "vs_bot",
  "boardSize": 3,
  "player1Id": "number",
  "player2Id": "number | null",
  "botDifficulty": "easy",
  "status": "waiting",
  "currentPlayerSymbol": "x | o | null",
  "createdAt": "string (ISO 8601)",
  "boardState": "string[][]"
}
```

**Odpowiedzi błędów**:
- 400 Bad Request: Nieprawidłowe parametry gry
- 422 Unprocessable Entity: Błędy walidacji

---

#### GET /api/games
**Opis**: Lista gier dla bieżącego użytkownika z filtrowaniem i paginacją
**Autoryzacja**: Wymagane uwierzytelnienie

**Parametry zapytania**:
- `status`: Filtruj według statusu (waiting, in_progress, finished, abandoned, draw)
- `gameType`: Filtruj według typu (vs_bot, pvp)
- `page`: Numer strony (domyślnie: 0)
- `size`: Rozmiar strony (domyślnie: 20, maks: 100)
- `sort`: Pole sortowania (domyślnie: createdAt,desc)

**Odpowiedź sukcesu** (200 OK):
```json
{
  "content": [
    {
      "gameId": "number",
      "gameType": "vs_bot",
      "boardSize": 3,
      "status": "in_progress",
      "player1Username": "string",
      "player2Username": "string | null",
      "winnerUsername": "string | null",
      "botDifficulty": "easy",
      "totalMoves": 5,
      "createdAt": "string (ISO 8601)",
      "lastMoveAt": "string (ISO 8601)",
      "finishedAt": "string (ISO 8601) | null"
    }
  ],
  "totalElements": 0,
  "totalPages": 0,
  "size": 20,
  "number": 0
}
```

---

#### GET /api/games/{gameId}
**Opis**: Pobranie szczegółowych informacji o grze
**Autoryzacja**: Wymagane uwierzytelnienie (musi być uczestnikiem)

**Odpowiedź sukcesu** (200 OK):
```json
{
  "gameId": "number",
  "gameType": "vs_bot",
  "boardSize": 3,
  "player1": {
    "userId": "number",
    "username": "string",
    "isGuest": false
  },
  "player2": {
    "userId": "number",
    "username": "string",
    "isGuest": false
  } | null,
  "winner": {
    "userId": "number",
    "username": "string"
  } | null,
  "botDifficulty": "easy",
  "status": "in_progress",
  "currentPlayerSymbol": "x",
  "lastMoveAt": "string (ISO 8601)",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)",
  "finishedAt": "string (ISO 8601) | null",
  "boardState": "string[][]",
  "totalMoves": 5,
  "moves": [
    {
      "moveId": "number",
      "row": 0,
      "col": 0,
      "playerSymbol": "x",
      "moveOrder": 1,
      "playerId": "number",
      "createdAt": "string (ISO 8601)"
    }
  ]
}
```

**Odpowiedzi błędów**:
- 403 Forbidden: Nie jesteś uczestnikiem tej gry
- 404 Not Found: Gra nie znaleziona

---

#### PUT /api/games/{gameId}/status
**Opis**: Aktualizacja statusu gry (poddanie, porzucenie)
**Autoryzacja**: Wymagane uwierzytelnienie (musi być uczestnikiem)

**Ciało żądania**:
```json
{
  "status": "abandoned"
}
```

**Odpowiedź sukcesu** (200 OK):
```json
{
  "gameId": "number",
  "status": "abandoned",
  "updatedAt": "string (ISO 8601)"
}
```

**Odpowiedzi błędów**:
- 403 Forbidden: Nie jesteś uczestnikiem lub nieprawidłowe przejście statusu
- 404 Not Found: Gra nie znaleziona
- 422 Unprocessable Entity: Nieprawidłowe przejście statusu

---

### 2.5 Punkty końcowe ruchów

#### POST /api/games/{gameId}/moves
**Opis**: Wykonanie ruchu w grze
**Autoryzacja**: Wymagane uwierzytelnienie (musi być uczestnikiem i aktualnym graczem)

**Ciało żądania**:
```json
{
  "row": 0,
  "col": 0,
  "playerSymbol": "x | o"
}
```

**Walidacja**:
- Ruch musi być poprawny (pozycja nie zajęta, w granicach planszy)
- Gracz musi być aktualnym graczem (zgodny z `current_player_symbol`)
- Gra musi być w statusie `in_progress`
- Wiersze i kolumny są indeksowane od 0

**Odpowiedź sukcesu** (201 Created):
```json
{
  "moveId": "number",
  "gameId": "number",
  "row": 0,
  "col": 0,
  "playerSymbol": "x",
  "moveOrder": 3,
  "createdAt": "string (ISO 8601)",
  "boardState": "string[][]",
  "gameStatus": "in_progress | finished | draw",
  "winner": {
    "userId": "number",
    "username": "string"
  } | null
}
```

**Odpowiedzi błędów**:
- 400 Bad Request: Nieprawidłowy ruch
- 403 Forbidden: Nie jesteś aktualnym graczem lub uczestnikiem
- 404 Not Found: Gra nie znaleziona
- 422 Unprocessable Entity: Nieprawidłowy ruch (pozycja zajęta lub poza granicami)

---

#### GET /api/games/{gameId}/moves
**Opis**: Pobranie wszystkich ruchów dla gry
**Autoryzacja**: Wymagane uwierzytelnienie (musi być uczestnikiem)

**Parametry zapytania**: Brak

**Odpowiedź sukcesu** (200 OK):
```json
[
  {
    "moveId": "number",
    "row": 0,
    "col": 0,
    "playerSymbol": "x",
    "moveOrder": 1,
    "playerId": "number",
    "playerUsername": "string | null (null dla ruchów bota)",
    "createdAt": "string (ISO 8601)"
  }
]
```

**Odpowiedzi błędów**:
- 403 Forbidden: Nie jesteś uczestnikiem
- 404 Not Found: Gra nie znaleziona

---

### 2.6 Punkty końcowe bota AI

#### POST /api/games/{gameId}/bot-move
**Opis**: Wykonanie ruchu bota (wywoływane automatycznie dla gier vs_bot)
**Autoryzacja**: Wewnętrzne (nie eksponowane do klientów, wywoływane po ruchu gracza)

**Ciało żądania**: Brak

**Odpowiedź sukcesu** (200 OK):
```json
{
  "moveId": "number",
  "gameId": "number",
  "row": 1,
  "col": 1,
  "playerSymbol": "o",
  "moveOrder": 4,
  "createdAt": "string (ISO 8601)",
  "boardState": "string[][]",
  "gameStatus": "in_progress | finished | draw",
  "winner": {
    "userId": "number",
    "username": "string"
  } | null
}
```

---

### 2.7 Punkty końcowe matchmakingu PvP

#### POST /api/v1/matching/queue
**Opis**: Dołączenie do kolejki matchmakingu dla PvP
**Autoryzacja**: Wymagane uwierzytelnienie

**Ciało żądania**:
```json
{
  "boardSize": 3 | 4 | 5
}
```

**Odpowiedź sukcesu** (200 OK):
```json
{
  "message": "Zakolejkowano do matchmakingu",
  "estimatedWaitTime": "number (sekundy)"
}
```

---

#### DELETE /api/v1/matching/queue
**Opis**: Opuszczenie kolejki matchmakingu
**Autoryzacja**: Wymagane uwierzytelnienie

**Odpowiedź sukcesu** (200 OK):
```json
{
  "message": "Opuszczono kolejkę matchmakingu"
}
```

---

#### POST /api/v1/matching/challenge/{userId}
**Opis**: Wyzwanie konkretnego gracza do gry
**Autoryzacja**: Wymagane uwierzytelnienie

**Ciało żądania**:
```json
{
  "boardSize": 3 | 4 | 5
}
```

**Odpowiedź sukcesu** (201 Created):
```json
{
  "gameId": "number",
  "gameType": "pvp",
  "boardSize": 3,
  "player1Id": "number",
  "player2Id": "number",
  "status": "waiting",
  "createdAt": "string (ISO 8601)"
}
```

**Odpowiedzi błędów**:
- 404 Not Found: Wyzwany użytkownik nie znaleziony
- 409 Conflict: Użytkownik jest niedostępny lub już w grze

---

### 2.8 Punkty końcowe rankingów

#### GET /api/rankings
**Opis**: Pobranie globalnego rankingu
**Autoryzacja**: Publiczne

**Parametry zapytania**:
- `page`: Numer strony (domyślnie: 0)
- `size`: Rozmiar strony (domyślnie: 50, maks: 100)
- `startRank`: Początkowa pozycja w rankingu (alternatywa dla strony)

**Odpowiedź sukcesu** (200 OK):
```json
{
  "content": [
    {
      "rankPosition": 1,
      "userId": "number",
      "username": "string",
      "totalPoints": 5000,
      "gamesPlayed": 25,
      "gamesWon": 15,
      "createdAt": "string (ISO 8601)"
    }
  ],
  "totalElements": 0,
  "totalPages": 0,
  "size": 50,
  "number": 0
}
```

---

#### GET /api/rankings/{userId}
**Opis**: Pobranie pozycji w rankingu dla konkretnego użytkownika
**Autoryzacja**: Publiczne

**Odpowiedź sukcesu** (200 OK):
```json
{
  "rankPosition": 42,
  "userId": "number",
  "username": "string",
  "totalPoints": 3500,
  "gamesPlayed": 18,
  "gamesWon": 12,
  "createdAt": "string (ISO 8601)"
}
```

**Odpowiedzi błędów**:
- 404 Not Found: Użytkownik nie znaleziony lub użytkownik jest gościem (nie w rankingu)

---

#### GET /api/rankings/around/{userId}
**Opis**: Pobranie rankingów wokół konkretnej pozycji użytkownika
**Autoryzacja**: Publiczne

**Parametry zapytania**:
- `range`: Liczba graczy przed i po (domyślnie: 5, maks: 10)

**Odpowiedź sukcesu** (200 OK):
```json
[
  {
    "rankPosition": 37,
    "userId": "number",
    "username": "string",
    "totalPoints": 3700,
    "gamesPlayed": 20,
    "gamesWon": 14
  }
]
```

---

### 2.9 Punkty końcowe stanu gry

#### GET /api/games/{gameId}/board
**Opis**: Pobranie aktualnego stanu planszy dla gry
**Autoryzacja**: Wymagane uwierzytelnienie (musi być uczestnikiem)

**Odpowiedź sukcesu** (200 OK):
```json
{
  "boardState": "string[][]",
  "boardSize": 3,
  "totalMoves": 5,
  "lastMove": {
    "row": 1,
    "col": 1,
    "playerSymbol": "x",
    "moveOrder": 5
  }
}
```

**Odpowiedzi błędów**:
- 403 Forbidden: Nie jesteś uczestnikiem
- 404 Not Found: Gra nie znaleziona

---

### 2.10 Punkty końcowe WebSocket

#### WS /ws/game/{gameId}
**Opis**: Połączenie WebSocket dla rozgrywki PvP w czasie rzeczywistym
**Autoryzacja**: Wymagane uwierzytelnienie (musi być uczestnikiem)

**Subprotokół**: `game-protocol`

**Typy wiadomości**:
- Klient → Serwer:
  - `MOVE`: Wyślij ruch do serwera
  - `SURRENDER`: Poddaj grę
  - `PING`: Keep-alive

- Serwer → Klient:
  - `MOVE_ACCEPTED`: Ruch został zaakceptowany
  - `MOVE_REJECTED`: Ruch został odrzucony (błąd walidacji)
  - `OPPONENT_MOVE`: Przeciwnik wykonał ruch
  - `GAME_UPDATE`: Status gry się zmienił
  - `TIMER_UPDATE`: Pozostały czas na aktualny ruch
  - `GAME_ENDED`: Gra zakończona
  - `PONG`: Odpowiedź keep-alive

---

### 2.11 Punkty końcowe zdrowia i monitorowania

#### GET /actuator/health
**Opis**: Sprawdzenie zdrowia aplikacji
**Autoryzacja**: Publiczne

**Odpowiedź sukcesu** (200 OK):
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    },
    "websocket": {
      "status": "UP"
    }
  }
}
```

---

#### GET /actuator/metrics
**Opis**: Metryki aplikacji (format Prometheus)
**Autoryzacja**: Wymagane uwierzytelnienie (tylko admin)

**Odpowiedź sukcesu** (200 OK):
Metryki w formacie Prometheus

---

## 3. Uwierzytelnianie i autoryzacja

### 3.1 Mechanizm uwierzytelniania

**Główne**: Tokeny JWT (access + refresh) wydawane bezpośrednio przez Spring Security

**Przepływ**:
1. Użytkownik rejestruje się/loguje przez endpoint backendu (`/api/v1/auth/register|login`)
2. Spring Security waliduje dane uwierzytelniające przeciwko hashom (BCrypt) przechowywanym w tabeli `users`
3. Backend generuje nowy access token (krótko żyjący) oraz refresh token (dłuższy), zapisuje ich identyfikatory (JTI) do Redis i wysyła w httpOnly cookie
4. Klient przy kolejnych żądaniach korzysta z cookie (lub dodaje nagłówek `Authorization: Bearer <token>`)

**Uwierzytelnianie gości**:
- Goście identyfikowani przez adres IP
- Sesje gości śledzone przez tymczasowe tokeny lub ciasteczka sesji
- Stosowane ograniczenie szybkości na podstawie IP

### 3.2 Reguły autoryzacji

**Publiczne punkty końcowe**:
- Rejestracja i logowanie
- Rankingi (tylko do odczytu)
- Sprawdzanie zdrowia aplikacji

**Punkty końcowe wymagające uwierzytelnienia**:
- Wszystkie operacje związane z grami
- Zarządzanie profilem użytkownika
- Matchmaking

**Poziomy autoryzacji**:
- **Gość**: Może grać, przeglądać ograniczone dane
- **Zarejestrowany**: Pełny dostęp do wszystkich funkcji
- **Admin**: Dostęp do metryk i punktów końcowych administracyjnych

### 3.3 Integracja RLS

Polityki Row Level Security w PostgreSQL wymuszają:
- Użytkownicy mogą tylko przeglądać swoje własne profile
- Gry są dostępne tylko dla uczestników
- Ruchy są widoczne tylko dla uczestników gry
- Rankingi są publiczne, ale ograniczone do zarejestrowanych użytkowników

**Implementacja**:
- Backend waliduje JWT i wyodrębnia kontekst użytkownika
- Ustawia zmienną sesji `app.guest_user_id` dla użytkowników gości
- Dla zarejestrowanych użytkowników ustawia zmienną `app.current_user_id`, którą wymuszają polityki RLS w PostgreSQL

---

## 4. Walidacja i logika biznesowa

### 4.1 Walidacja użytkownika

**Rejestracja**:
- Email: Poprawny format email, unikalny
- Hasło: Minimalna długość, wymagania dotyczące złożoności
- Nazwa użytkownika: 3-50 znaków, alfanumeryczne i podkreślniki, unikalna

**Aktualizacja**:
- Nazwa użytkownika nie może zostać zmieniona na istniejącą
- Goście nie mogą aktualizować nazwy użytkownika

**Ograniczenia bazy danych**:
- `users_registered_check`: Gwarantuje poprawną strukturę danych
- Ograniczenia unikalności na `auth_user_id` i `username`

---

### 4.2 Walidacja gry

**Tworzenie**:
- `boardSize`: Musi być 3, 4 lub 5
- `gameType`: Musi być "vs_bot" lub "pvp"
- Dla vs_bot: `botDifficulty` musi być "easy", "medium" lub "hard", `player2_id` musi być NULL
- Dla pvp: `botDifficulty` musi być NULL, `player2_id` musi zostać przypisane

**Przejścia statusu**:
- `waiting` → `in_progress`: Gdy drugi gracz dołącza (pvp) lub gra się rozpoczyna (vs_bot)
- `in_progress` → `finished`: Gra wygrana przez gracza
- `in_progress` → `draw`: Plansza wypełniona bez zwycięzcy
- `in_progress` → `abandoned`: Gracz poddaje się

**Ograniczenia bazy danych**:
- `games_vs_bot_check`: Zapewnia poprawną konfigurację typu gry
- `games_status_check`: Zapewnia prawidłowe wartości statusu
- `games_finished_check`: Zapewnia ustawienie `finished_at` dla zakończonych gier

---

### 4.3 Walidacja ruchu

**Reguły biznesowe**:
- Ruch musi być w granicach planszy (0 <= row, col < boardSize)
- Pozycja nie może być zajęta
- Gracz musi być aktualnym graczem (zgodny z `current_player_symbol`)
- Gra musi być w statusie `in_progress`

**Ograniczenia bazy danych**:
- `row >= 0`, `col >= 0`
- `move_order > 0`
- Ograniczenie unikalności na `(game_id, row, col)` zapobiega duplikatom pozycji
- Klucze obce zapewniają integralność referencyjną

**Funkcje bazy danych**:
- `is_move_valid(p_game_id, p_row, p_col)`: Waliduje ruch przed wstawieniem

---

### 4.4 Generowanie stanu planszy

**Logika**:
- Stan planszy jest generowany dynamicznie z historii ruchów
- Używa funkcji bazy danych `generate_board_state(p_game_id)`
- Ruchy uporządkowane według `move_order` ASC
- Puste komórki reprezentowane jako null lub pusty string

**Implementacja**:
- Stan planszy obliczany na żądanie dla żądań GET
- Zwracany w odpowiedziach API do wyświetlenia
- Wiadomości WebSocket zawierają pełne lub przyrostowe aktualizacje

---

### 4.5 Wykrywanie wygranej

**Logika**:
- Zaimplementowane w warstwie aplikacji
- Sprawdza wiersze, kolumny i przekątne pod kątem pasujących symboli
- Zwraca natychmiast po warunku wygranej
- Kontynuuje do momentu wypełnienia planszy w celu wykrycia remisu

**Automatyczne aktualizacje**:
- Trigger `update_user_stats_on_game_finished` aktualizuje statystyki
- Oblicza i przyznaje punkty przez `calculate_game_points()`
- Ustawia znacznik czasu `finished_at`

---

### 4.6 System punktowy

**Przyznawane automatycznie**:
- Wygrana PvP: +1000 punktów
- vs_bot (łatwy): +100 punktów
- vs_bot (średni): +500 punktów
- vs_bot (trudny): +1000 punktów

**Implementacja**:
- Obliczane przez `calculate_game_points(p_game_type, p_bot_difficulty)`
- Stosowane automatycznie przez trigger bazy danych
- Nie wymaga ręcznego zarządzania punktami

---

### 4.7 Obsługa timeoutów PvP

**Reguła biznesowa**:
- 20-sekundowy timeout nieaktywności dla gier PvP
- Sprawdzane przez funkcję bazy danych `check_pvp_timeout()`
- Wywoływane przez zadanie Spring Scheduled (co 5-10 sekund)

**Proces**:
1. Zapytanie gier z `status = 'in_progress'` i `game_type = 'pvp'`
2. Sprawdzenie czy `last_move_at < NOW() - INTERVAL '20 seconds'`
3. Ustawienie `status = 'finished'`
4. Ustawienie `winner_id` na gracza, który nie timeout'ował
5. Ustawienie znacznika czasu `finished_at`

---

### 4.8 Odświeżanie rankingu

**Reguła biznesowa**:
- Widok materialized `player_rankings` pre-oblicza pozycje
- Odświeżany przez `refresh_player_rankings()` używając CONCURRENTLY
- Wywoływane przez zadanie Spring Scheduled (co 5-15 minut)

**Implementacja**:
- Tylko zarejestrowani użytkownicy uwzględnieni (`is_guest = FALSE`)
- Sortowane według `total_points DESC`, `created_at ASC`
- Pre-obliczona `rank_position` dla szybkich zapytań

---

### 4.9 Logika matchmakingu

**Algorytm**:
- Prosta kolejka FIFO dla losowego matchmakingu
- Sprawdza dostępność gracza (`last_seen_at` w granicach progu)
- Uwzględnia preferencje rozmiaru planszy
- Tworzy grę natychmiast po znalezieniu dopasowania

**Wyzwania**:
- Wyzwania konkretnego gracza przez `/api/v1/matching/challenge/{userId}`
- Sprawdza czy docelowy użytkownik jest dostępny online
- Zwraca 409 jeśli użytkownik niedostępny lub w aktywnej grze

---

### 4.10 Logika bota AI

**Tryb łatwy**:
- Losowy wybór poprawnego ruchu
- Brak strategicznego myślenia

**Tryb średni**:
- Blokuje wygrywające ruchy przeciwnika
- Wykonuje wygrywające ruchy gdy dostępne
- Losowy w przeciwnym razie

**Tryb trudny**:
- Algorytm minimax dla optymalnej gry
- Doskonała ocena stanu gry
- Niepokonana strategia

**Implementacja**:
- Ruchy bota wykonywane po stronie serwera po ruchach gracza
- Wywoływane przez zautomatyzowany proces, nie przez żądanie klienta
- Powiadomienia WebSocket wysyłane do gracza

---

## 5. Formaty odpowiedzi i obsługa błędów

### 5.1 Standardowy format odpowiedzi

**Odpowiedź sukcesu**:
```json
{
  "data": {},
  "timestamp": "string (ISO 8601)",
  "status": "success"
}
```

**Odpowiedź błędu**:
```json
{
  "error": {
    "code": "string",
    "message": "string",
    "details": {}
  },
  "timestamp": "string (ISO 8601)",
  "status": "error"
}
```

---

### 5.2 Kody statusu HTTP

**Sukces**:
- 200 OK: Udane GET, PUT, DELETE
- 201 Created: Udane POST tworzące zasób
- 204 No Content: Udane DELETE bez treści

**Błąd klienta**:
- 400 Bad Request: Nieprawidłowe parametry żądania
- 401 Unauthorized: Brak lub nieprawidłowe uwierzytelnienie
- 403 Forbidden: Niewystarczające uprawnienia
- 404 Not Found: Zasób nie znaleziony
- 409 Conflict: Konflikt zasobów (duplikat, naruszenie ograniczeń)
- 422 Unprocessable Entity: Błędy walidacji

**Błąd serwera**:
- 500 Internal Server Error: Nieoczekiwany błąd
- 503 Service Unavailable: Usługa tymczasowo niedostępna

---

### 5.3 Ograniczenie szybkości

**Publiczne punkty końcowe**:
- 100 żądań na minutę na IP

**Punkty końcowe wymagające uwierzytelnienia**:
- 1000 żądań na minutę na użytkownika

**Akcje w grze**:
- 10 ruchów na minutę na grę (zapobiega spamowi)

**WebSocket**:
- 60 wiadomości na minutę na połączenie

---

## 6. Paginacja

**Format**:
- Paginacja oparta na stronach
- Domyślny rozmiar strony: 20
- Maksymalny rozmiar strony: 100

**Parametry zapytania**:
- `page`: Numer strony indeksowany od 0
- `size`: Liczba elementów na stronę
- `sort`: Pola oddzielone przecinkami z kierunkiem (np. `createdAt,desc`)

**Odpowiedź**:
```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```

---

## 7. Protokół WebSocket

### 7.1 Połączenie

**Punkt końcowy**: `WS /ws/game/{gameId}`

**Uwierzytelnianie**: Token JWT w parametrze zapytania lub subprotokole

**Cykl życia połączenia**:
1. Klient nawiązuje połączenie WebSocket
2. Serwer waliduje uwierzytelnienie
3. Serwer wysyła początkowy stan gry
4. Klient/serwer wymieniają wiadomości do zakończenia gry
5. Klient może ponownie nawiązać połączenie jeśli zostało utracone

---

### 7.2 Typy wiadomości

**MOVE (Client → Server)**:
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

**MOVE_ACCEPTED (Server → Client)**:
```json
{
  "type": "MOVE_ACCEPTED",
  "payload": {
    "moveId": 1,
    "row": 0,
    "col": 0,
    "playerSymbol": "x",
    "boardState": "string[][]",
    "currentPlayerSymbol": "o",
    "nextMoveAt": "string (ISO 8601)"
  }
}
```

**MOVE_REJECTED (Server → Client)**:
```json
{
  "type": "MOVE_REJECTED",
  "payload": {
    "reason": "Invalid move: position occupied",
    "code": "INVALID_MOVE"
  }
}
```

**OPPONENT_MOVE (Server → Client)**:
```json
{
  "type": "OPPONENT_MOVE",
  "payload": {
    "row": 1,
    "col": 1,
    "playerSymbol": "o",
    "boardState": "string[][]",
    "currentPlayerSymbol": "x",
    "nextMoveAt": "string (ISO 8601)"
  }
}
```

**GAME_UPDATE (Server → Client)**:
```json
{
  "type": "GAME_UPDATE",
  "payload": {
    "gameId": 1,
    "status": "finished",
    "winner": {
      "userId": 42,
      "username": "player1"
    },
    "boardState": "string[][]"
  }
}
```

**TIMER_UPDATE (Server → Client)**:
```json
{
  "type": "TIMER_UPDATE",
  "payload": {
    "remainingSeconds": 8,
    "currentPlayerSymbol": "x"
  }
}
```

**GAME_ENDED (Server → Client)**:
```json
{
  "type": "GAME_ENDED",
  "payload": {
    "gameId": 1,
    "status": "finished",
    "winner": {
      "userId": 42,
      "username": "player1"
    },
    "finalBoardState": "string[][]",
    "totalMoves": 9
  }
}
```

**SURRENDER (Client → Server)**:
```json
{
  "type": "SURRENDER",
  "payload": {}
}
```

**PING/PONG (Both directions)**:
```json
{
  "type": "PING",
  "payload": {
    "timestamp": "string (ISO 8601)"
  }
}
```

---

## 8. Modele danych

### 8.1 Model użytkownika

```typescript
interface User {
  userId: number;
  authUserId: string | null;
  username: string | null;
  isGuest: boolean;
  ipAddress: string | null;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  lastSeenAt: string | null;
  createdAt: string;
  updatedAt: string;
}
```

---

### 8.2 Model gry

```typescript
interface Game {
  gameId: number;
  gameType: 'vs_bot' | 'pvp';
  boardSize: 3 | 4 | 5;
  player1Id: number;
  player2Id: number | null;
  botDifficulty: 'easy' | 'medium' | 'hard' | null;
  status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw';
  currentPlayerSymbol: 'x' | 'o' | null;
  winnerId: number | null;
  lastMoveAt: string | null;
  createdAt: string;
  updatedAt: string;
  finishedAt: string | null;
}
```

---

### 8.3 Model ruchu

```typescript
interface Move {
  moveId: number;
  gameId: number;
  playerId: number | null;
  row: number;
  col: number;
  playerSymbol: 'x' | 'o';
  moveOrder: number;
  createdAt: string;
}
```

---

### 8.4 Model rankingu

```typescript
interface Ranking {
  rankPosition: number;
  userId: number;
  username: string;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
}
```

---

## 9. Zagadnienia bezpieczeństwa

### 9.1 Bezpieczeństwo uwierzytelniania

- Tokeny JWT z wygaśnięciem (15 minut)
- Tokeny odświeżające dla przedłużonych sesji
- Bezpieczne przechowywanie ciasteczek (HttpOnly, Secure, SameSite)
- Hashowanie haseł po stronie aplikacji (BCrypt, cost ≥ 12)

---

### 9.2 Bezpieczeństwo API

- HTTPS wymagane dla wszystkich punktów końcowych
- CORS skonfigurowane tylko dla dozwolonych źródeł
- Limity rozmiaru żądań zapobiegające DoS
- Zapobieganie SQL injection przez parametryzowane zapytania
- Zapobieganie XSS przez sanityzację wejścia

---

### 9.3 Ograniczenie szybkości

- Zaimplementowane przez Redis
- Algorytm przesuwającego okna
- Na podstawie IP dla gości
- Na podstawie użytkownika dla uwierzytelnionych

---

### 9.4 Ochrona danych

- Row Level Security na poziomie bazy danych
- Zarządzanie użytkownikami przez Spring Security oraz tabelę `users`
- Anonimizacja IP gości po sesji
- Brak wrażliwych danych w logach

---

## 10. Zagadnienia wydajności

### 10.1 Optymalizacja bazy danych

- Indeksy na często zapytujących kolumnach
- Widok materialized dla rankingów
- Pula połączeń
- Cache'owanie wyników zapytań dla rankingów

---

### 10.2 Strategia cache'owania

**Cache Redis**:
- Dane rankingowe (TTL 5-15 minut)
- Aktywne stany gier
- Sesje użytkowników
- Kolejki matchmakingu

**Unieważnianie cache**:
- Rankingi: Odświeżanie oparte na czasie
- Stany gier: Unieważnianie oparte na zdarzeniach
- Sesje: Wygaśnięcie oparte na TTL

---

### 10.3 Optymalizacja odpowiedzi

- Paginacja dla dużych zbiorów danych
- Selektywna projekcja pól
- Stan planszy obliczany na żądanie
- WebSocket dla aktualizacji w czasie rzeczywistym (zmniejsza polling)

---

## 11. Strategia testowania

### 11.1 Testy jednostkowe

- Logika walidacji kontrolera
- Reguły biznesowe warstwy serwisowej
- Zapytania repozytoriów
- Funkcje pomocnicze

---

### 11.2 Testy integracyjne

- Przepływy API end-to-end
- Transakcje bazodanowe
- Połączenia WebSocket
- Przepływy uwierzytelniania

---

### 11.3 Testy E2E (Cypress)

- Pełne podróże użytkownika
- Scenariusze gier (vs_bot, pvp)
- Przepływy uwierzytelniania
- Aktualizacje rankingu

---

## 12. Monitorowanie i obserwowalność

### 12.1 Logowanie

- Strukturyzowane logowanie (format JSON)
- Logowanie żądań/odpowiedzi
- Śledzenie błędów ze śladami stosu
- Logowanie zdarzeń gier

---

### 12.2 Metryki (Prometheus)

- Szybkość i opóźnienie żądań
- Wskaźniki błędów według punktów końcowych
- Liczba aktywnych gier
- Liczba połączeń WebSocket
- Wydajność zapytań do bazy danych

---

### 12.3 Kontrole zdrowia

- Łączność z bazą danych
- Łączność z Redis
- Status serwera WebSocket
- Zależności zewnętrznych usług

---

## 13. Wersjonowanie API

**Strategia**: Wersjonowanie oparte na URL

**Format**: `/api/v1/...`

**Obecna wersja**: v1

**Migracja**: Nowe wersje dodawane jako `/api/v2/...` z powiadomieniami o deprecacji

---

## 14. Dokumentacja Swagger

**Punkt końcowy**: `/swagger-ui.html`

**Funkcje**:
- Interaktywny eksplorator API
- Schematy żądań/odpowiedzi
- Konfiguracja uwierzytelniania
- Funkcjonalność try-it-out

**Eksport**: Specyfikacja OpenAPI 3.0 dostępna pod `/v3/api-docs`

---

## 15. Założenia i uwagi

### 15.1 Założenia

1. Spring Security obsługuje rejestrację użytkowników, uwierzytelnianie i zarządzanie hashami haseł (BCrypt)
2. Frontend korzysta z endpointów `/api/v1/auth/*`, a tokeny JWT są utrzymywane w httpOnly cookie (opcjonalnie w nagłówku Authorization)
3. Połączenia WebSocket zarządzane przez Spring WebSocket z subprotokołem STOMP
4. Ruchy bota wykonywane po stronie serwera jako zautomatyzowane procesy w tle
5. Redis używany do zarządzania sesjami, cache'owania i ograniczania szybkości
6. Triggery PostgreSQL obsługują automatyczne aktualizacje statystyk
7. Matchmaking używa prostej kolejki FIFO początkowo (może być ulepszony dopasowaniem opartym na elo później)

---

### 15.2 Przyszłe ulepszenia

1. Matchmaking oparty na elo dla gier dopasowanych pod względem umiejętności
2. Tryb obserwatora dla trwających gier PvP
3. Funkcjonalność powtarzania gier
4. Tryb turniejowy
5. Powiadomienia push dla zaproszeń do gry
6. Funkcje społecznościowe (znajomi, czat)

---

## 16. Dodatek: Integracja funkcji bazy danych

### 16.1 Funkcje używane przez API

- `generate_board_state(p_game_id)`: Generowanie planszy dla odpowiedzi
- `is_move_valid(p_game_id, p_row, p_col)`: Walidacja przed wstawieniem
- `calculate_game_points(p_game_type, p_bot_difficulty)`: Obliczanie punktów
- `get_user_ranking_position(p_user_id)`: Wyszukiwanie pozycji
- `refresh_player_rankings()`: Zaplanowane zadanie odświeżania
- `check_pvp_timeout()`: Zaplanowane zadanie sprawdzania timeoutu

---

### 16.2 Triggery

Wszystkie triggery są zarządzane przez bazę danych i nie wymagają interwencji API:
- `update_users_updated_at`: Automatyczne aktualizacje znaczników czasu
- `update_games_updated_at`: Automatyczne aktualizacje znaczników czasu
- `update_game_last_move_timestamp`: Automatyczne śledzenie ostatniego ruchu
- `update_user_stats_on_game_finished`: Automatyczne aktualizacje statystyk

---

## 17. Konfiguracja specyficzna dla środowiska

### 17.1 Środowisko deweloperskie

- Wygaśnięcie JWT: 24 godziny
- Ograniczenia szybkości: Rozluźnione
- Szczegółowe komunikaty błędów
- CORS: Zezwalaj na wszystkie źródła

---

### 17.2 Produkcja

- Wygaśnięcie JWT: 15 minut
- Ograniczenia szybkości: Ścisłe egzekwowanie
- Sanityzowane komunikaty błędów
- CORS: Określone dozwolone źródła
- Tylko HTTPS
- Logowanie żądań włączone
- Zbieranie metryk włączone

---

## 18. Zgodność i prywatność

### 18.1 Prywatność danych

- Adresy IP gości przechowywane tymczasowo
- Brak trwałego śledzenia dla gości
- RLS zapewnia izolację danych
- Zgodne z RODO przetwarzanie danych

---

### 18.2 Ograniczenie szybkości i uczciwe korzystanie

- Zapobiega nadużyciom i zapewnia uczciwą rozgrywkę
- Użytkownicy goście ograniczeni do podstawowych funkcji
- Zarejestrowani użytkownicy mają priorytet w matchmakingu
- Środki anty-cheat poprzez walidację po stronie serwera

---

