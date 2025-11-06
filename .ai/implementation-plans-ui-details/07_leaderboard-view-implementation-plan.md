# Plan implementacji widoku LeaderboardComponent

> **Źródło**: `.ai/implementation-plans-ui/07_leaderboard-component.md`

## 1. Przegląd

LeaderboardComponent to widok wyświetlający globalny ranking graczy z możliwością przeglądania pozycji, wyboru przeciwnika oraz wyzwania gracza do gry. Komponent obsługuje paginację, wyświetla pozycję zalogowanego użytkownika oraz umożliwia wyświetlenie graczy wokół użytkownika. Widok jest dostępny dla wszystkich użytkowników (gości i zarejestrowanych), ale niektóre funkcjonalności (wyświetlanie własnej pozycji, wyzwanie gracza) wymagają autoryzacji.

Główne funkcjonalności:
- Wyświetlanie globalnego rankingu z paginacją (lazy loading)
- Wyróżniona karta z pozycją zalogowanego użytkownika (tylko dla zarejestrowanych)
- Wyświetlanie graczy wokół użytkownika w dialogu (tylko dla zarejestrowanych)
- Wyzwanie gracza do gry PvP z rankingu
- Wyróżnienie wizualne pozycji aktualnego użytkownika w tabeli
- Obsługa błędów API z odpowiednimi komunikatami

Komponent realizuje historyjki użytkownika: US-009 (Przeglądanie rankingu graczy) i US-010 (Wybór przeciwnika z rankingu).

## 2. Routing widoku

**Ścieżka routingu**: `/leaderboard`

**Konfiguracja routingu**:
```typescript
{
  path: 'leaderboard',
  component: LeaderboardComponent,
  pathMatch: 'full'
}
```

**Lokalizacja pliku routingu**: `frontend/src/app/app.routes.ts`

**Guardy**: Brak (widok publiczny, dostępny dla wszystkich użytkowników)

## 3. Struktura komponentów

```
LeaderboardComponent (główny komponent)
├── UserRankCardComponent (warunkowy, tylko dla zarejestrowanych)
│   ├── ButtonModule (PrimeNG - przycisk "Pokaż graczy wokół mnie")
│   └── CardModule (PrimeNG - karta z pozycją użytkownika)
├── TableModule (PrimeNG - tabela rankingu)
│   ├── PaginatorModule (PrimeNG - paginacja)
│   └── ButtonModule (PrimeNG - przycisk "Wyzwij")
└── PlayersAroundDialogComponent (warunkowy dialog)
    ├── DialogModule (PrimeNG - dialog)
    ├── TableModule (PrimeNG - tabela graczy)
    └── ButtonModule (PrimeNG - przyciski wyzwania)
```

**Hierarchia komponentów**:
- LeaderboardComponent jest komponentem standalone
- UserRankCardComponent i PlayersAroundDialogComponent są komponentami współdzielonymi
- Wszystkie komponenty używają PrimeNG do elementów UI
- Komponent używa Angular Reactive Forms i RxJS do zarządzania stanem

## 4. Szczegóły komponentów

### LeaderboardComponent

**Opis komponentu**: Główny komponent widoku rankingu, zarządza stanem rankingu, pozycją użytkownika, paginacją oraz obsługuje wyzwania graczy. Komponent integruje się z RankingService, MatchmakingService i AuthService.

**Główne elementy HTML**:
- Kontener główny (`.leaderboard-container`)
- Sekcja nagłówka (`.leaderboard-header`) z tytułem i opisem
- Warunkowa karta użytkownika (`<app-user-rank-card>`) - tylko dla zarejestrowanych
- Kontener tabeli (`.leaderboard-table-container`) z PrimeNG Table
- Warunkowy dialog graczy wokół (`<app-players-around-dialog>`)

**Obsługiwane zdarzenia**:
- `ngOnInit()` - inicjalizacja komponentu, ładowanie rankingu i pozycji użytkownika
- `onLazyLoad(event: LazyLoadEvent)` - obsługa lazy loading paginacji tabeli
- `onChallengePlayer(userId: number)` - obsługa wyzwania gracza do gry
- `onShowPlayersAround()` - otwarcie dialogu z graczami wokół użytkownika
- `onClosePlayersAroundDialog()` - zamknięcie dialogu
- `isCurrentUser(userId: number)` - sprawdzenie czy użytkownik to aktualny użytkownik
- `loadUserRanking()` - ładowanie pozycji użytkownika w rankingu
- `loadRanking(page: number, size: number)` - ładowanie strony rankingu
- `handleError(error: HttpErrorResponse)` - obsługa błędów API
- `handleChallengeError(error: HttpErrorResponse)` - obsługa błędów wyzwania

**Obsługiwana walidacja**:
- Sprawdzenie czy użytkownik jest zarejestrowany (przed wyświetleniem UserRankCard)
- Sprawdzenie czy użytkownik nie jest gościem (przed wyzwaniem gracza)
- Walidacja parametrów paginacji (page ≥ 0, size 1-100)
- Walidacja dostępności gracza przed wyzwaniem (po stronie API)

**Typy**:
- `RankingItem` - interfejs reprezentujący pozycję w rankingu
- `RankingListResponse` - interfejs odpowiedzi API z paginacją
- `RankingDetailResponse` - interfejs pozycji użytkownika
- `ChallengeRequest` - interfejs żądania wyzwania
- `ChallengeResponse` - interfejs odpowiedzi wyzwania
- `User` - interfejs reprezentujący użytkownika
- `Observable<RankingItem[]>` - Observable z listą rankingów
- `Observable<RankingDetailResponse | null>` - Observable z pozycją użytkownika
- `Observable<number>` - Observable z całkowitą liczbą rekordów
- `Observable<boolean>` - Observable ze statusem ładowania
- `BehaviorSubject<T>` - RxJS Subject dla zarządzania stanem

**Propsy**: Brak (komponent główny, nie przyjmuje propsów)

### UserRankCardComponent

**Opis komponentu**: Komponent wyświetlający wyróżnioną kartę z pozycją zalogowanego użytkownika w rankingu. Wyświetla statystyki użytkownika (pozycja, punkty, rozegrane gry, wygrane gry) oraz przycisk do wyświetlenia graczy wokół użytkownika.

**Główne elementy HTML**:
- Kontener karty (`.user-rank-card`)
- Sekcja z pozycją w rankingu (`.rank-position`)
- Sekcja ze statystykami (`.stats`)
  - Punkty (`totalPoints`)
  - Rozegrane gry (`gamesPlayed`)
  - Wygrane gry (`gamesWon`)
- Przycisk "Pokaż graczy wokół mnie" (PrimeNG Button)

**Obsługiwane zdarzenia**:
- `showPlayersAround` - EventEmitter emitujący void po kliknięciu przycisku

**Obsługiwana walidacja**: Brak (komponent prezentacyjny)

**Typy**:
- `RankingDetailResponse` - interfejs pozycji użytkownika w rankingu

**Propsy**:
- `ranking: RankingDetailResponse` - pozycja użytkownika w rankingu (required)

**Outputs**:
- `showPlayersAround: EventEmitter<void>` - emisja żądania pokazania graczy wokół

### PlayersAroundDialogComponent

**Opis komponentu**: Komponent dialogu wyświetlający listę graczy znajdujących się przed i po użytkowniku w rankingu. Umożliwia wyzwanie gracza bezpośrednio z dialogu. Dialog jest modalny i wyświetla się nad głównym widokiem.

**Główne elementy HTML**:
- Dialog PrimeNG (`.p-dialog`)
- Nagłówek dialogu z tytułem
- Tabela z listą graczy (PrimeNG Table)
- Przyciski "Wyzwij" dla każdego gracza (PrimeNG Button)
- Przycisk "Zamknij" (PrimeNG Button)

**Obsługiwane zdarzenia**:
- `challenge` - EventEmitter emitujący userId gracza do wyzwania
- `close` - EventEmitter emitujący void po zamknięciu dialogu

**Obsługiwana walidacja**:
- Sprawdzenie czy użytkownik nie jest aktualnym użytkownikiem (przed wyzwaniem)

**Typy**:
- `RankingAroundItem` - interfejs gracza w rankingu wokół użytkownika
- `RankingAroundResponse` - interfejs odpowiedzi API z graczami wokół

**Propsy**:
- `userId: number` - ID użytkownika (required, do pobrania graczy wokół)

**Outputs**:
- `close: EventEmitter<void>` - zamknięcie dialogu
- `challenge: EventEmitter<number>` - wyzwanie gracza (userId)

## 5. Typy

### RankingItem (DTO dla pozycji w rankingu)

```typescript
interface RankingItem {
  rankPosition: number;
  userId: number;
  username: string;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
}
```

**Pola**:
- `rankPosition: number` - pozycja w rankingu (zaczyna się od 1)
- `userId: number` - unikalny identyfikator użytkownika
- `username: string` - nazwa użytkownika
- `totalPoints: number` - suma punktów użytkownika
- `gamesPlayed: number` - liczba rozegranych gier
- `gamesWon: number` - liczba wygranych gier
- `createdAt: string` - data utworzenia konta (ISO 8601)

**Uwagi**:
- DTO używane w odpowiedzi z endpointu GET /api/v1/rankings
- Tylko zarejestrowani użytkownicy (nie goście) są uwzględniani w rankingu
- Ranking jest sortowany według `totalPoints DESC`, następnie `createdAt ASC`

### RankingListResponse (DTO dla odpowiedzi z paginacją)

```typescript
interface RankingListResponse {
  content: RankingItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
}
```

**Pola**:
- `content: RankingItem[]` - lista pozycji w rankingu dla danej strony
- `totalElements: number` - całkowita liczba elementów w rankingu
- `totalPages: number` - całkowita liczba stron
- `size: number` - rozmiar strony (liczba elementów na stronę)
- `number: number` - numer bieżącej strony (indeks od 0)
- `first?: boolean` - flaga wskazująca czy to pierwsza strona
- `last?: boolean` - flaga wskazująca czy to ostatnia strona

**Uwagi**:
- DTO używane w odpowiedzi z endpointu GET /api/v1/rankings
- Paginacja jest oparta na stronach (page-based)
- Domyślny rozmiar strony: 50, maksymalny: 100

### RankingDetailResponse (DTO dla pozycji użytkownika)

```typescript
interface RankingDetailResponse {
  rankPosition: number;
  userId: number;
  username: string;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
}
```

**Pola**: Identyczne z `RankingItem`

**Uwagi**:
- DTO używane w odpowiedzi z endpointu GET /api/v1/rankings/{userId}
- Endpoint zwraca 404 jeśli użytkownik jest gościem lub nie istnieje w rankingu

### RankingAroundItem (DTO dla gracza wokół użytkownika)

```typescript
interface RankingAroundItem {
  rankPosition: number;
  userId: number;
  username: string;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
}
```

**Pola**: Podobne do `RankingItem`, ale bez pola `createdAt`

**Uwagi**:
- DTO używane w odpowiedzi z endpointu GET /api/v1/rankings/around/{userId}
- Lista zawiera graczy przed i po użytkowniku (określana przez parametr `range`)
- Domyślny zakres: 5 graczy przed i po, maksymalny: 10

### RankingAroundResponse (DTO dla odpowiedzi z graczami wokół)

```typescript
interface RankingAroundResponse {
  items: RankingAroundItem[];
}
```

**Pola**:
- `items: RankingAroundItem[]` - lista graczy wokół użytkownika

**Uwagi**:
- DTO używane w odpowiedzi z endpointu GET /api/v1/rankings/around/{userId}
- Lista jest posortowana według `rankPosition ASC`

### ChallengeRequest (DTO dla żądania wyzwania)

```typescript
interface ChallengeRequest {
  boardSize: 3 | 4 | 5;
}
```

**Pola**:
- `boardSize: 3 | 4 | 5` - rozmiar planszy do gry (wymagane)

**Uwagi**:
- DTO używane w żądaniu do endpointu POST /api/v1/matching/challenge/{userId}
- Walidacja: `boardSize` musi być 3, 4 lub 5

### ChallengeResponse (DTO dla odpowiedzi wyzwania)

```typescript
interface ChallengeResponse {
  gameId: number;
  gameType: 'pvp';
  boardSize: 3 | 4 | 5;
  player1Id: number;
  player2Id: number;
  status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw';
  createdAt: string;
}
```

**Pola**:
- `gameId: number` - identyfikator utworzonej gry
- `gameType: 'pvp'` - typ gry (zawsze 'pvp' dla wyzwania)
- `boardSize: 3 | 4 | 5` - rozmiar planszy
- `player1Id: number` - ID wyzywającego gracza
- `player2Id: number` - ID wyzwanego gracza
- `status: GameStatusEnum` - status gry (zazwyczaj 'waiting' lub 'in_progress')
- `createdAt: string` - data utworzenia gry (ISO 8601)

**Uwagi**:
- DTO używane w odpowiedzi z endpointu POST /api/v1/matching/challenge/{userId}
- Po pomyślnym wyzwaniu użytkownik jest przekierowywany do widoku gry (`/game/{gameId}`)

### User (interfejs użytkownika)

```typescript
interface User {
  userId: number;
  username: string | null;
  isGuest: boolean;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt?: string;
  lastSeenAt?: string | null;
}
```

**Pola**:
- `userId: number` - unikalny identyfikator użytkownika
- `username: string | null` - nazwa użytkownika (null dla gości)
- `isGuest: boolean` - flaga wskazująca czy użytkownik jest gościem
- `totalPoints: number` - suma punktów użytkownika
- `gamesPlayed: number` - liczba rozegranych gier
- `gamesWon: number` - liczba wygranych gier
- `createdAt?: string` - data utworzenia konta (ISO 8601, opcjonalne)
- `lastSeenAt?: string | null` - data ostatniej aktywności (ISO 8601, opcjonalne)

**Uwagi**:
- Używane do sprawdzenia czy użytkownik jest zalogowany i czy jest gościem
- Uzyskane z `AuthService.getCurrentUser()`

## 6. Zarządzanie stanem

### 6.1 Stan komponentu LeaderboardComponent

Komponent używa RxJS BehaviorSubject do zarządzania stanem lokalnym:

- `rankings$: BehaviorSubject<RankingItem[]>` - lista pozycji w rankingu dla bieżącej strony
- `userRanking$: BehaviorSubject<RankingDetailResponse | null>` - pozycja użytkownika (null jeśli gość lub błąd)
- `totalRecords$: BehaviorSubject<number>` - całkowita liczba rekordów w rankingu
- `isLoading$: BehaviorSubject<boolean>` - flaga wskazująca czy trwa ładowanie danych
- `showPlayersAroundDialog$: BehaviorSubject<boolean>` - flaga wskazująca czy dialog jest otwarty
- `isChallenging: boolean` - flaga wskazująca czy trwa proces wyzwania gracza
- `currentUserId: number | null` - ID aktualnego użytkownika (używane do wyróżnienia w tabeli)

### 6.2 Stan komponentu PlayersAroundDialogComponent

Komponent używa Observable do zarządzania danymi:

- `playersAround$: Observable<RankingAroundItem[]>` - lista graczy wokół użytkownika (ładowana z API)
- `isLoading$: Observable<boolean>` - flaga wskazująca czy trwa ładowanie danych

### 6.3 Integracja z serwisami

Komponent integruje się z następującymi serwisami:

- **RankingService**: 
  - `getRanking(page: number, size: number): Observable<RankingListResponse>`
  - `getUserRanking(userId: number): Observable<RankingDetailResponse>`
  - `getPlayersAround(userId: number, range?: number): Observable<RankingAroundResponse>`

- **MatchmakingService**:
  - `challengePlayer(userId: number, boardSize: 3 | 4 | 5): Observable<ChallengeResponse>`

- **AuthService**:
  - `getCurrentUser(): Observable<User | null>`
  - `isAuthenticated(): Observable<boolean>`

### 6.4 Custom hooks

Komponent nie wymaga custom hooks, ponieważ używa standardowych mechanizmów Angular (RxJS Observable, BehaviorSubject).

### 6.5 Lifecycle hooks

- `ngOnInit()`: Inicjalizacja komponentu, ładowanie rankingu i pozycji użytkownika
- `ngOnDestroy()` (opcjonalny): Cleanup subskrypcji Observable jeśli używane są manualne subskrypcje (w przeciwnym razie używa się async pipe)

## 7. Integracja API

### 7.1 GET /api/v1/rankings

**Endpoint**: `GET /api/v1/rankings`

**Query Parameters**:
- `page: number` - numer strony (domyślnie: 0, wymagane)
- `size: number` - rozmiar strony (domyślnie: 50, maks: 100, wymagane)

**Request**: Brak body

**Response (200 OK)**:
```typescript
RankingListResponse {
  content: RankingItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
}
```

**Obsługa błędów**:
- `400 Bad Request` - nieprawidłowe parametry (size > 100, page < 0)
- `500 Internal Server Error` - błąd serwera

**Użycie w komponencie**:
- Wywoływane w metodzie `loadRanking(page: number, size: number)`
- Używane w PrimeNG Table z lazy loading (`onLazyLoad` event)
- Wyniki są aktualizowane w `rankings$` BehaviorSubject

### 7.2 GET /api/v1/rankings/{userId}

**Endpoint**: `GET /api/v1/rankings/{userId}`

**Path Parameters**:
- `userId: number` - ID użytkownika (wymagane)

**Request**: Brak body, brak query parameters

**Response (200 OK)**:
```typescript
RankingDetailResponse {
  rankPosition: number;
  userId: number;
  username: string;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
}
```

**Obsługa błędów**:
- `404 Not Found` - użytkownik nie znaleziony lub jest gościem (cicho ignorowane)
- `500 Internal Server Error` - błąd serwera (cicho ignorowane)

**Użycie w komponencie**:
- Wywoływane w metodzie `loadUserRanking()`
- Używane tylko dla zarejestrowanych użytkowników (nie gości)
- Wynik jest aktualizowany w `userRanking$` BehaviorSubject
- Błędy są cicho ignorowane (nie wyświetlają toast notification)

### 7.3 GET /api/v1/rankings/around/{userId}

**Endpoint**: `GET /api/v1/rankings/around/{userId}`

**Path Parameters**:
- `userId: number` - ID użytkownika (wymagane)

**Query Parameters**:
- `range: number` - liczba graczy przed i po (domyślnie: 5, maks: 10, opcjonalne)

**Request**: Brak body

**Response (200 OK)**:
```typescript
RankingAroundResponse {
  items: RankingAroundItem[];
}
```

**Obsługa błędów**:
- `400 Bad Request` - nieprawidłowy parametr range
- `404 Not Found` - użytkownik nie znaleziony lub jest gościem
- `500 Internal Server Error` - błąd serwera

**Użycie w komponencie**:
- Wywoływane w komponencie `PlayersAroundDialogComponent` przy otwarciu dialogu
- Używane tylko dla zarejestrowanych użytkowników
- Wyniki są wyświetlane w tabeli w dialogu

### 7.4 POST /api/v1/matching/challenge/{userId}

**Endpoint**: `POST /api/v1/matching/challenge/{userId}`

**Path Parameters**:
- `userId: number` - ID wyzwanego gracza (wymagane)

**Request Body**:
```typescript
ChallengeRequest {
  boardSize: 3 | 4 | 5;
}
```

**Response (201 Created)**:
```typescript
ChallengeResponse {
  gameId: number;
  gameType: 'pvp';
  boardSize: 3 | 4 | 5;
  player1Id: number;
  player2Id: number;
  status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw';
  createdAt: string;
}
```

**Obsługa błędów**:
- `400 Bad Request` - nieprawidłowy parametr boardSize
- `401 Unauthorized` - brak autoryzacji (użytkownik nie jest zalogowany)
- `403 Forbidden` - próba wyzwania samego siebie
- `404 Not Found` - wyzwany użytkownik nie istnieje
- `409 Conflict` - wyzwany użytkownik jest niedostępny (w grze lub w kolejce)
- `500 Internal Server Error` - błąd serwera

**Użycie w komponencie**:
- Wywoływane w metodzie `onChallengePlayer(userId: number)`
- Domyślny rozmiar planszy: 3 (można zmienić w przyszłości)
- Po pomyślnym wyzwaniu użytkownik jest przekierowywany do `/game/{gameId}`
- Toast notification z komunikatem sukcesu
- Obsługa błędów z odpowiednimi komunikatami

### 7.5 Serwisy

#### RankingService

**Lokalizacja**: `frontend/src/app/services/ranking.service.ts`

**Metody**:
```typescript
class RankingService {
  getRanking(page: number, size: number): Observable<RankingListResponse>;
  getUserRanking(userId: number): Observable<RankingDetailResponse>;
  getPlayersAround(userId: number, range?: number): Observable<RankingAroundResponse>;
}
```

**Implementacja**:
- Używa `HttpClient` do wywołań API
- Obsługuje błędy HTTP i mapuje je na odpowiednie komunikaty
- Zwraca Observable z odpowiedzią API

#### MatchmakingService

**Lokalizacja**: `frontend/src/app/services/matchmaking.service.ts`

**Metody**:
```typescript
class MatchmakingService {
  challengePlayer(userId: number, boardSize: 3 | 4 | 5): Observable<ChallengeResponse>;
}
```

**Implementacja**:
- Używa `HttpClient` do wywołań API
- Wysyła żądanie POST do `/api/v1/matching/challenge/{userId}`
- Obsługuje błędy HTTP i mapuje je na odpowiednie komunikaty
- Zwraca Observable z odpowiedzią API

#### AuthService

**Lokalizacja**: `frontend/src/app/services/auth.service.ts`

**Metody**:
```typescript
class AuthService {
  getCurrentUser(): Observable<User | null>;
  isAuthenticated(): Observable<boolean>;
}
```

**Implementacja**:
- Zwraca aktualnego użytkownika z localStorage/sessionStorage lub z API
- Sprawdza czy użytkownik jest zalogowany

## 8. Interakcje użytkownika

### 8.1 Przeglądanie rankingu

**Opis**: Użytkownik przegląda globalny ranking graczy z paginacją.

**Akcje użytkownika**:
1. Użytkownik wchodzi na stronę `/leaderboard`
2. Komponent automatycznie ładuje pierwszą stronę rankingu (page=0, size=50)
3. Użytkownik widzi tabelę z rankingiem
4. Użytkownik może przejść do innej strony używając paginacji (PrimeNG Paginator)

**Obsługa**:
- Metoda `loadRanking(page: number, size: number)` jest wywoływana przy inicjalizacji i przy zmianie strony
- PrimeNG Table z lazy loading automatycznie wywołuje `onLazyLoad` przy zmianie strony
- Stan ładowania jest wyświetlany przez `isLoading$` Observable

**Walidacja**:
- Parametry paginacji są walidowane po stronie API (page ≥ 0, size 1-100)

### 8.2 Wyświetlanie własnej pozycji

**Opis**: Zarejestrowany użytkownik widzi swoją pozycję w rankingu w wyróżnionej karcie.

**Akcje użytkownika**:
1. Zarejestrowany użytkownik wchodzi na stronę `/leaderboard`
2. Komponent automatycznie ładuje pozycję użytkownika (jeśli jest zalogowany)
3. Użytkownik widzi kartę `UserRankCardComponent` z własną pozycją i statystykami

**Obsługa**:
- Metoda `loadUserRanking()` jest wywoływana w `ngOnInit()`
- Komponent sprawdza czy użytkownik jest zarejestrowany (nie gość) przed załadowaniem
- Karta jest wyświetlana tylko jeśli `userRanking$` nie jest null
- Błędy są cicho ignorowane (użytkownik może nie być w rankingu)

**Walidacja**:
- Sprawdzenie czy użytkownik jest zarejestrowany (`!user.isGuest`)
- Endpoint zwraca 404 jeśli użytkownik jest gościem (cicho ignorowane)

### 8.3 Wyświetlanie graczy wokół użytkownika

**Opis**: Zarejestrowany użytkownik może wyświetlić dialog z graczami znajdującymi się przed i po nim w rankingu.

**Akcje użytkownika**:
1. Zarejestrowany użytkownik klika przycisk "Pokaż graczy wokół mnie" w karcie `UserRankCardComponent`
2. Dialog `PlayersAroundDialogComponent` się otwiera
3. Komponent ładuje listę graczy wokół użytkownika (range=5)
4. Użytkownik widzi listę graczy przed i po sobie w rankingu
5. Użytkownik może wyzwąć gracza z dialogu lub zamknąć dialog

**Obsługa**:
- Metoda `onShowPlayersAround()` ustawia `showPlayersAroundDialog$` na `true`
- `PlayersAroundDialogComponent` automatycznie ładuje graczy wokół przy otwarciu (ngOnInit)
- Dialog może być zamknięty przez przycisk "Zamknij" lub kliknięcie poza dialogiem

**Walidacja**:
- Sprawdzenie czy użytkownik jest zarejestrowany (przed wyświetleniem przycisku)
- Endpoint zwraca 404 jeśli użytkownik jest gościem

### 8.4 Wyzwanie gracza z rankingu

**Opis**: Użytkownik wyzwywa gracza do gry PvP z tabeli rankingu.

**Akcje użytkownika**:
1. Użytkownik klika przycisk "Wyzwij" przy graczu w tabeli rankingu
2. System sprawdza czy użytkownik jest zalogowany (nie gość)
3. System sprawdza czy użytkownik nie wyzywa samego siebie
4. System wysyła żądanie wyzwania do API
5. Po pomyślnym wyzwaniu użytkownik jest przekierowywany do widoku gry

**Obsługa**:
- Metoda `onChallengePlayer(userId: number)` jest wywoływana po kliknięciu przycisku
- Flaga `isChallenging` jest ustawiana na `true` aby zapobiec wielokrotnym wyzwaniom
- Wywołanie API przez `MatchmakingService.challengePlayer(userId, 3)`
- Po sukcesie: toast notification z komunikatem sukcesu, przekierowanie do `/game/{gameId}`
- Po błędzie: toast notification z odpowiednim komunikatem błędu

**Walidacja**:
- Sprawdzenie czy użytkownik jest zarejestrowany (przed wyzwaniem)
- Sprawdzenie czy użytkownik nie wyzywa samego siebie (`isCurrentUser(userId)`)
- Sprawdzenie czy gracz jest dostępny (po stronie API - 409 Conflict)
- Sprawdzenie czy gracz istnieje (po stronie API - 404 Not Found)

**Wyjątki**:
- Użytkownik gość nie może wyzwywać graczy (przycisk jest wyłączony)
- Użytkownik nie może wyzwywać samego siebie (przycisk jest wyłączony)
- Jeśli gracz jest niedostępny, wyświetlany jest odpowiedni komunikat błędu

### 8.5 Wyzwanie gracza z dialogu

**Opis**: Użytkownik wyzwywa gracza do gry PvP z dialogu graczy wokół.

**Akcje użytkownika**:
1. Użytkownik otwiera dialog z graczami wokół
2. Użytkownik klika przycisk "Wyzwij" przy graczu w dialogu
3. Dialog emituje event `challenge` z userId gracza
4. `LeaderboardComponent` obsługuje event i wywołuje `onChallengePlayer(userId)`
5. Dialog może być zamknięty po wyzwaniu lub pozostawiony otwarty

**Obsługa**:
- Event `challenge` z `PlayersAroundDialogComponent` jest obsługiwany przez `(challenge)="onChallengePlayer($event)"`
- Reszta obsługi jest identyczna jak w przypadku wyzwania z tabeli

**Walidacja**: Identyczna jak w przypadku wyzwania z tabeli

### 8.6 Wyróżnienie pozycji użytkownika w tabeli

**Opis**: Pozycja aktualnego użytkownika w tabeli jest wyróżniona wizualnie.

**Akcje użytkownika**:
- Użytkownik nie wykonuje żadnych akcji - wyróżnienie jest automatyczne

**Obsługa**:
- Metoda `isCurrentUser(userId: number)` sprawdza czy userId odpowiada `currentUserId`
- W template, wiersz tabeli ma klasę `current-user` jeśli `isCurrentUser(ranking.userId)` zwraca true
- Klasa CSS `.current-user` stosuje wyróżnienie wizualne (np. tło, font-weight)

**Walidacja**:
- Sprawdzenie czy `currentUserId` jest ustawione (tylko dla zarejestrowanych użytkowników)

## 9. Warunki i walidacja

### 9.1 Warunki wyświetlania komponentów

**UserRankCardComponent**:
- Wyświetlany tylko jeśli `userRanking$ | async` nie jest null
- Warunek: użytkownik musi być zarejestrowany (nie gość)
- Warunek: użytkownik musi mieć pozycję w rankingu (API zwraca 200 OK)

**PlayersAroundDialogComponent**:
- Wyświetlany tylko jeśli `showPlayersAroundDialog$ | async` jest true
- Warunek: użytkownik musi być zarejestrowany (przed otwarciem dialogu)
- Warunek: użytkownik musi mieć pozycję w rankingu (API zwraca 200 OK dla graczy wokół)

**Przycisk "Wyzwij" w tabeli**:
- Wyłączony jeśli `isCurrentUser(ranking.userId)` zwraca true
- Wyłączony jeśli `isChallenging` jest true
- Wyłączony jeśli użytkownik jest gościem (sprawdzenie w template)

### 9.2 Walidacja po stronie klienta

**Paginacja**:
- Parametry paginacji są walidowane przez PrimeNG Table (page ≥ 0, size > 0)
- Maksymalny rozmiar strony jest ograniczony do 100 (walidacja po stronie API)

**Wyzwanie gracza**:
- Sprawdzenie czy użytkownik jest zalogowany (nie gość) - przed wyzwaniem
- Sprawdzenie czy użytkownik nie wyzywa samego siebie - przed wyzwaniem
- Sprawdzenie czy `isChallenging` jest false - zapobiega wielokrotnym wyzwaniom

### 9.3 Walidacja po stronie serwera (API)

**GET /api/v1/rankings**:
- `page` musi być ≥ 0 (walidacja Bean Validation `@Min(0)`)
- `size` musi być między 1 a 100 (walidacja Bean Validation `@Min(1) @Max(100)`)

**GET /api/v1/rankings/{userId}**:
- `userId` musi być dodatnią liczbą (walidacja Bean Validation `@Positive`)
- Użytkownik musi istnieć w bazie danych
- Użytkownik nie może być gościem (`is_guest = false`)

**GET /api/v1/rankings/around/{userId}**:
- `userId` musi być dodatnią liczbą (walidacja Bean Validation `@Positive`)
- `range` musi być między 1 a 10 (walidacja Bean Validation `@Min(1) @Max(10)`)
- Użytkownik musi istnieć w bazie danych
- Użytkownik nie może być gościem (`is_guest = false`)

**POST /api/v1/matching/challenge/{userId}**:
- `userId` musi być dodatnią liczbą (walidacja Bean Validation `@Positive`)
- `boardSize` musi być 3, 4 lub 5 (walidacja enum)
- Użytkownik wyzywający musi być zalogowany (JWT token)
- Użytkownik wyzywający nie może wyzywać samego siebie
- Wyzwany użytkownik musi istnieć w bazie danych
- Wyzwany użytkownik musi być dostępny (nie w grze, nie w kolejce, online)

### 9.4 Warunki wpływające na stan interfejsu

**Stan ładowania**:
- `isLoading$` jest ustawiane na `true` przed wywołaniem API
- `isLoading$` jest ustawiane na `false` po otrzymaniu odpowiedzi (sukces lub błąd)
- PrimeNG Table wyświetla loading indicator gdy `isLoading$` jest true

**Stan wyzwania**:
- `isChallenging` jest ustawiane na `true` przed wywołaniem API wyzwania
- `isChallenging` jest ustawiane na `false` po otrzymaniu odpowiedzi (sukces lub błąd)
- Przycisk "Wyzwij" jest wyłączony gdy `isChallenging` jest true

**Stan dialogu**:
- `showPlayersAroundDialog$` jest ustawiane na `true` przy otwarciu dialogu
- `showPlayersAroundDialog$` jest ustawiane na `false` przy zamknięciu dialogu
- Dialog jest wyświetlany tylko gdy `showPlayersAroundDialog$` jest true

**Pozycja użytkownika**:
- `userRanking$` jest ustawiane na wartość z API po pomyślnym załadowaniu
- `userRanking$` pozostaje null jeśli użytkownik jest gościem lub wystąpił błąd
- `currentUserId` jest ustawiane na `userRanking$.userId` po załadowaniu pozycji

## 10. Obsługa błędów

### 10.1 Błędy pobierania rankingu

**400 Bad Request - Nieprawidłowe parametry paginacji**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik jest informowany o nieprawidłowych parametrach
- **Komunikat**: "Nieprawidłowe parametry paginacji. Spróbuj ponownie."
- **Akcja naprawcza**: Automatyczne przywrócenie poprzednich parametrów paginacji

**500 Internal Server Error - Błąd serwera**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik jest informowany o błędzie serwera
- **Komunikat**: "Wystąpił błąd serwera. Spróbuj ponownie później."
- **Akcja naprawcza**: Możliwość ponowienia żądania (refresh strony)

**Brak połączenia z internetem**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik jest informowany o braku połączenia
- **Komunikat**: "Brak połączenia z internetem. Sprawdź swoje połączenie."

### 10.2 Błędy pobierania pozycji użytkownika

**404 Not Found - Użytkownik jest gościem lub nie ma pozycji**:
- **Obsługa**: Ciche ignorowanie błędu (nie wyświetlanie toast notification)
- **Efekt dla użytkownika**: Karta `UserRankCardComponent` nie jest wyświetlana
- **Logowanie**: Błąd jest logowany do konsoli w trybie development (`console.error`)

**500 Internal Server Error - Błąd serwera**:
- **Obsługa**: Ciche ignorowanie błędu (nie wyświetlanie toast notification)
- **Efekt dla użytkownika**: Karta `UserRankCardComponent` nie jest wyświetlana
- **Logowanie**: Błąd jest logowany do konsoli w trybie development (`console.error`)

### 10.3 Błędy pobierania graczy wokół użytkownika

**400 Bad Request - Nieprawidłowy parametr range**:
- **Obsługa**: Toast notification z komunikatem błędu w dialogu
- **Efekt dla użytkownika**: Użytkownik jest informowany o nieprawidłowym parametrze
- **Komunikat**: "Nieprawidłowy parametr zakresu. Używam domyślnej wartości."

**404 Not Found - Użytkownik jest gościem lub nie ma pozycji**:
- **Obsługa**: Toast notification z komunikatem błędu, zamknięcie dialogu
- **Efekt dla użytkownika**: Dialog jest zamykany, użytkownik jest informowany o błędzie
- **Komunikat**: "Nie można wyświetlić graczy wokół. Użytkownik nie jest w rankingu."

**500 Internal Server Error - Błąd serwera**:
- **Obsługa**: Toast notification z komunikatem błędu w dialogu
- **Efekt dla użytkownika**: Użytkownik jest informowany o błędzie serwera
- **Komunikat**: "Wystąpił błąd serwera. Spróbuj ponownie później."

### 10.4 Błędy wyzwania gracza

**400 Bad Request - Nieprawidłowy parametr boardSize**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik jest informowany o nieprawidłowym parametrze
- **Komunikat**: "Nieprawidłowy rozmiar planszy. Spróbuj ponownie."

**401 Unauthorized - Brak autoryzacji**:
- **Obsługa**: Toast notification z komunikatem błędu, przekierowanie do logowania
- **Efekt dla użytkownika**: Użytkownik jest przekierowywany do strony logowania
- **Komunikat**: "Musisz być zalogowany, aby wyzwywać graczy."
- **Akcja naprawcza**: Przekierowanie do `/auth/login` z parametrem `returnUrl=/leaderboard`

**403 Forbidden - Próba wyzwania samego siebie**:
- **Obsługa**: Toast notification z komunikatem błędu (nie powinno się zdarzyć, przycisk jest wyłączony)
- **Efekt dla użytkownika**: Użytkownik jest informowany o błędzie
- **Komunikat**: "Nie możesz wyzwywać samego siebie."

**404 Not Found - Wyzwany użytkownik nie istnieje**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik jest informowany o nieistniejącym graczu
- **Komunikat**: "Gracz nie został znaleziony."
- **Akcja naprawcza**: Odświeżenie rankingu (możliwość ponowienia żądania)

**409 Conflict - Wyzwany użytkownik jest niedostępny**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik jest informowany o niedostępności gracza
- **Komunikat**: "Gracz jest niedostępny lub już w grze."
- **Akcja naprawcza**: Możliwość ponowienia żądania po chwili

**500 Internal Server Error - Błąd serwera**:
- **Obsługa**: Toast notification z komunikatem błędu
- **Efekt dla użytkownika**: Użytkownik jest informowany o błędzie serwera
- **Komunikat**: "Nie udało się wyzwać gracza. Spróbuj ponownie później."

### 10.5 Globalna obsługa błędów

**Error Handler Service** (opcjonalny):
- Centralna obsługa błędów HTTP
- Przechwytywanie błędów 401 (Unauthorized) i przekierowanie do logowania
- Przechwytywanie błędów 403 (Forbidden) i wyświetlenie komunikatu
- Przechwytywanie błędów 500 (Internal Server Error) i wyświetlenie ogólnego komunikatu

**Toast Service** (PrimeNG MessageService):
- Wyświetlanie komunikatów błędów i sukcesu
- Automatyczne znikanie po określonym czasie (domyślnie 3-5 sekund)
- Różne typy komunikatów (error, warning, info, success)
- Stosowanie spójnych komunikatów błędów w całej aplikacji

**Retry logic** (opcjonalny):
- Automatyczne ponowienie żądania przy błędach sieciowych (max 3 próby)
- Exponential backoff między próbami
- Wyświetlenie komunikatu o ponawianiu żądania

## 11. Kroki implementacji

### Krok 1: Przygotowanie infrastruktury i zależności

**1.1 Sprawdzenie istniejących komponentów i serwisów**:
- Weryfikacja czy `RankingService` istnieje i ma wymagane metody
- Weryfikacja czy `MatchmakingService` istnieje i ma metodę `challengePlayer()`
- Weryfikacja czy `AuthService` istnieje i ma metodę `getCurrentUser()`
- Sprawdzenie czy komponenty współdzielone (`UserRankCardComponent`, `PlayersAroundDialogComponent`) istnieją

**1.2 Utworzenie brakujących serwisów**:
- `RankingService` w `frontend/src/app/services/ranking.service.ts`
  - Implementacja metody `getRanking(page: number, size: number)`
  - Implementacja metody `getUserRanking(userId: number)`
  - Implementacja metody `getPlayersAround(userId: number, range?: number)`

**1.3 Utworzenie brakujących komponentów współdzielonych**:
- `UserRankCardComponent` w `frontend/src/app/components/leaderboard/user-rank-card.component.ts`
- `PlayersAroundDialogComponent` w `frontend/src/app/components/leaderboard/players-around-dialog.component.ts`

**1.4 Instalacja zależności PrimeNG**:
- Sprawdzenie czy `TableModule`, `PaginatorModule`, `ButtonModule`, `CardModule`, `DialogModule` są zainstalowane
- Instalacja brakujących modułów PrimeNG jeśli potrzeba

**1.5 Utworzenie typów TypeScript**:
- Utworzenie pliku `frontend/src/app/models/ranking.model.ts` z interfejsami `RankingItem`, `RankingListResponse`, `RankingDetailResponse`, `RankingAroundItem`, `RankingAroundResponse`
- Utworzenie pliku `frontend/src/app/models/matchmaking.model.ts` z interfejsami `ChallengeRequest`, `ChallengeResponse`
- Sprawdzenie czy typy są już zdefiniowane w `.ai/types.ts` i zaimportowanie z odpowiedniej lokalizacji

### Krok 2: Implementacja serwisów

**2.1 Implementacja RankingService**:
- Utworzenie serwisu standalone z `@Injectable({ providedIn: 'root' })`
- Implementacja metody `getRanking(page: number, size: number): Observable<RankingListResponse>`
  - Wywołanie API `GET /api/v1/rankings?page=${page}&size=${size}`
  - Obsługa błędów HTTP z mapowaniem na odpowiednie komunikaty
- Implementacja metody `getUserRanking(userId: number): Observable<RankingDetailResponse>`
  - Wywołanie API `GET /api/v1/rankings/${userId}`
  - Obsługa błędów HTTP (ciche ignorowanie 404)
- Implementacja metody `getPlayersAround(userId: number, range?: number): Observable<RankingAroundResponse>`
  - Wywołanie API `GET /api/v1/rankings/around/${userId}?range=${range || 5}`
  - Obsługa błędów HTTP z mapowaniem na odpowiednie komunikaty

**2.2 Rozszerzenie MatchmakingService**:
- Dodanie metody `challengePlayer(userId: number, boardSize: 3 | 4 | 5): Observable<ChallengeResponse>`
  - Wywołanie API `POST /api/v1/matching/challenge/${userId}` z body `{ boardSize }`
  - Obsługa błędów HTTP z mapowaniem na odpowiednie komunikaty

**2.3 Testy jednostkowe serwisów**:
- Testy dla `RankingService.getRanking()`
- Testy dla `RankingService.getUserRanking()`
- Testy dla `RankingService.getPlayersAround()`
- Testy dla `MatchmakingService.challengePlayer()`

### Krok 3: Implementacja komponentów współdzielonych

**3.1 Implementacja UserRankCardComponent**:
- Utworzenie komponentu standalone
- Implementacja template z kartą PrimeNG Card
- Wyświetlenie pozycji w rankingu, statystyk (punkty, rozegrane gry, wygrane gry)
- Implementacja przycisku "Pokaż graczy wokół mnie"
- Implementacja EventEmitter `showPlayersAround`
- Stylowanie komponentu (SCSS)
- Testy jednostkowe

**3.2 Implementacja PlayersAroundDialogComponent**:
- Utworzenie komponentu standalone
- Implementacja template z PrimeNG Dialog
- Implementacja tabeli z listą graczy (PrimeNG Table)
- Implementacja przycisków "Wyzwij" dla każdego gracza
- Implementacja EventEmitter `challenge` i `close`
- Ładowanie graczy wokół przy otwarciu dialogu (ngOnInit)
- Stylowanie komponentu (SCSS)
- Testy jednostkowe

### Krok 4: Implementacja LeaderboardComponent

**4.1 Utworzenie komponentu**:
- Utworzenie pliku `frontend/src/app/features/leaderboard/leaderboard.component.ts`
- Utworzenie pliku `frontend/src/app/features/leaderboard/leaderboard.component.html`
- Utworzenie pliku `frontend/src/app/features/leaderboard/leaderboard.component.scss`

**4.2 Implementacja logiki komponentu**:
- Import wymaganych modułów (CommonModule, RouterModule, PrimeNG modules, AsyncPipe)
- Implementacja właściwości komponentu:
  - `rankings$: BehaviorSubject<RankingItem[]>`
  - `userRanking$: BehaviorSubject<RankingDetailResponse | null>`
  - `totalRecords$: BehaviorSubject<number>`
  - `isLoading$: BehaviorSubject<boolean>`
  - `showPlayersAroundDialog$: BehaviorSubject<boolean>`
  - `isChallenging: boolean`
  - `currentUserId: number | null`
  - `pageSize: number = 50`
- Implementacja metody `ngOnInit()` z inicjalizacją:
  - Wywołanie `loadUserRanking()`
  - Wywołanie `loadRanking(0, this.pageSize)`
- Implementacja metody `loadUserRanking()`
- Implementacja metody `loadRanking(page: number, size: number)`
- Implementacja metody `onLazyLoad(event: LazyLoadEvent)`
- Implementacja metody `onChallengePlayer(userId: number)`
- Implementacja metody `onShowPlayersAround()`
- Implementacja metody `onClosePlayersAroundDialog()`
- Implementacja metody `isCurrentUser(userId: number)`
- Implementacja metody `handleError(error: HttpErrorResponse)`
- Implementacja metody `handleChallengeError(error: HttpErrorResponse)`

**4.3 Implementacja template**:
- Struktura HTML z sekcjami (header, user rank card, table, dialog)
- Warunkowe wyświetlanie komponentów (`*ngIf`)
- Użycie `async` pipe dla Observable
- Integracja z PrimeNG Table (lazy loading, paginacja)
- Integracja z komponentami współdzielonymi (`UserRankCardComponent`, `PlayersAroundDialogComponent`)
- Wyróżnienie pozycji użytkownika w tabeli (klasa CSS `current-user`)

**4.4 Stylowanie**:
- Implementacja stylów SCSS dla `.leaderboard-container`
- Stylowanie sekcji nagłówka
- Stylowanie tabeli rankingu
- Stylowanie wyróżnienia pozycji użytkownika (`.current-user`)
- Responsywność dla różnych rozdzielczości ekranu

### Krok 5: Konfiguracja routingu

**5.1 Dodanie routingu**:
- Dodanie ścieżki `/leaderboard` do konfiguracji routingu
- Powiązanie ścieżki z `LeaderboardComponent`
- Ustawienie `pathMatch: 'full'`

**5.2 Testy routingu**:
- Testy jednostkowe routingu
- Testy E2E nawigacji do strony rankingu

### Krok 6: Implementacja animacji

**6.1 Animacje Angular**:
- Fade-in dla tabeli rankingu (300ms)
- Slide-in dla dialogu z graczami wokół (300ms)
- Smooth transitions dla przycisków

**6.2 CSS Transitions**:
- Transitions dla hover states przycisków
- Transitions dla focus states
- Transitions dla disabled states

### Krok 7: Implementacja obsługi błędów

**7.1 Obsługa błędów API**:
- Implementacja `catchError()` w Observable
- Implementacja toast notifications dla błędów
- Implementacja cichego ignorowania błędów dla pozycji użytkownika (404)
- Implementacja odpowiednich komunikatów błędów dla różnych kodów HTTP

**7.2 Obsługa błędów wyzwania**:
- Implementacja szczegółowej obsługi błędów wyzwania (400, 401, 403, 404, 409, 500)
- Implementacja odpowiednich komunikatów błędów
- Implementacja przekierowania do logowania przy błędzie 401

### Krok 8: Implementacja i18n

**8.1 Konfiguracja Angular i18n**:
- Konfiguracja plików tłumaczeń (en, pl)
- Dodanie kluczy tłumaczeń dla wszystkich tekstów w komponencie:
  - Nagłówki ("Ranking graczy", "Globalny ranking najlepszych graczy")
  - Nagłówki kolumn tabeli ("Pozycja", "Nazwa użytkownika", "Punkty", "Rozegrane gry", "Wygrane gry", "Akcje")
  - Przyciski ("Wyzwij", "Pokaż graczy wokół mnie", "Zamknij")
  - Komunikaty błędów i sukcesu

**8.2 Użycie tłumaczeń**:
- Zastąpienie hardcoded tekstów pipe `translate` lub serwisem `TranslateService`
- Testy dla różnych języków

### Krok 9: Testy

**9.1 Testy jednostkowe**:
- Testy dla `LeaderboardComponent` (Jest + Angular Testing Library)
  - Test inicjalizacji komponentu
  - Test ładowania rankingu
  - Test ładowania pozycji użytkownika
  - Test paginacji (lazy loading)
  - Test wyzwania gracza
  - Test wyświetlania dialogu z graczami wokół
  - Test wyróżnienia pozycji użytkownika
- Testy dla `UserRankCardComponent`
- Testy dla `PlayersAroundDialogComponent`
- Testy dla serwisów (`RankingService`, `MatchmakingService`)

**9.2 Testy E2E (Cypress)**:
- Scenariusz: Przeglądanie rankingu (gość)
- Scenariusz: Przeglądanie rankingu (zarejestrowany użytkownik)
- Scenariusz: Wyświetlanie własnej pozycji (zarejestrowany użytkownik)
- Scenariusz: Wyświetlanie graczy wokół użytkownika
- Scenariusz: Wybór przeciwnika z rankingu (wyzwanie gracza)
- Scenariusz: Wyzwanie gracza z dialogu
- Scenariusz: Obsługa błędów API (404, 409, 500)

### Krok 10: Dostępność (a11y)

**10.1 ARIA labels**:
- Dodanie `aria-label` dla wszystkich przycisków
- Dodanie `aria-describedby` dla sekcji z opisami
- Dodanie `aria-label` dla tabeli rankingu

**10.2 Keyboard navigation**:
- Obsługa nawigacji klawiaturą dla tabeli (PrimeNG Table ma wbudowaną obsługę)
- Obsługa Enter/Space dla aktywacji przycisków
- Focus indicators dla wszystkich interaktywnych elementów
- Obsługa Escape dla zamknięcia dialogu

**10.3 Screen reader support**:
- Semantyczne znaczniki HTML (`<table>`, `<thead>`, `<tbody>`, `<th>`, `<td>`)
- Opisy dla screen readerów (aria-label, aria-describedby)
- Informacje o statusie ładowania dla screen readerów

### Krok 11: Optymalizacja wydajności

**11.1 Lazy loading**:
- Upewnienie się, że komponent jest lazy loaded jeśli potrzeba
- Optymalizacja bundle size

**11.2 Change detection**:
- Użycie `OnPush` change detection strategy jeśli możliwe
- Optymalizacja subskrypcji Observable (użycie `async` pipe zamiast manualnych subskrypcji)

**11.3 Caching**:
- Cache'owanie odpowiedzi API dla rankingu (opcjonalne, Redis po stronie backendu)
- Cache'owanie pozycji użytkownika (TTL 5-10 minut)

**11.4 Virtual scrolling** (opcjonalny):
- Rozważenie użycia virtual scrolling dla bardzo dużych rankingów (1000+ pozycji)
- PrimeNG Table obsługuje virtual scrolling

### Krok 12: Code review i dokumentacja

**12.1 Code review**:
- Sprawdzenie zgodności z zasadami implementacji (brak komentarzy w kodzie aplikacyjnym)
- Weryfikacja zgodności z ESLint i Prettier
- Review bezpieczeństwa i wydajności
- Sprawdzenie obsługi błędów i edge cases

**12.2 Dokumentacja**:
- Komentarze w kodzie tylko tam gdzie wymagane (business logic, wyjątkowe przypadki)
- Aktualizacja README z informacjami o komponencie
- Dokumentacja API endpoints używanych przez komponent

### Krok 13: Wdrożenie

**13.1 Merge do głównej gałęzi**:
- Utworzenie Pull Request
- Code review przez zespół
- Merge po akceptacji

**13.2 Weryfikacja w środowisku deweloperskim**:
- Testy manualne wszystkich scenariuszy:
  - Przeglądanie rankingu (gość)
  - Przeglądanie rankingu (zarejestrowany użytkownik)
  - Wyświetlanie własnej pozycji
  - Wyświetlanie graczy wokół użytkownika
  - Wyzwanie gracza z rankingu
  - Wyzwanie gracza z dialogu
  - Obsługa błędów API
- Weryfikacja działania na różnych przeglądarkach
- Weryfikacja responsywności
- Weryfikacja dostępności (a11y)

**13.3 Wdrożenie na produkcję**:
- Wdrożenie przez CI/CD pipeline
- Monitorowanie błędów po wdrożeniu
- Zbieranie feedbacku od użytkowników

## 12. Mapowanie historyjek użytkownika

### US-009: Przeglądanie rankingu graczy

**Implementacja**:
- `LeaderboardComponent` wyświetla globalny ranking z paginacją
- Tabela PrimeNG z lazy loading umożliwia przeglądanie wielu stron rankingu
- Pozycja użytkownika jest wyróżniona wizualnie w tabeli (klasa CSS `current-user`)
- Ranking jest aktualizowany w czasie rzeczywistym (po każdym wywołaniu API)

**Kryteria akceptacji**:
- ✅ Użytkownik widzi listę graczy posortowaną według punktów
- ✅ Pozycja użytkownika jest wyróżniona wizualnie
- ✅ Ranking jest aktualizowany przy każdym załadowaniu strony
- ✅ Użytkownik może zobaczyć podstawowe statystyki innych graczy

### US-010: Wybór przeciwnika z rankingu

**Implementacja**:
- Przycisk "Wyzwij" przy każdym graczu w tabeli rankingu
- Metoda `onChallengePlayer(userId: number)` obsługuje wyzwanie gracza
- Integracja z `MatchmakingService.challengePlayer()` i endpointem POST /api/v1/matching/challenge/{userId}
- Przekierowanie do widoku gry po pomyślnym wyzwaniu

**Kryteria akceptacji**:
- ✅ Użytkownik może kliknąć na gracza w rankingu (przycisk "Wyzwij")
- ✅ System sprawdza czy gracz jest dostępny online (po stronie API)
- ✅ Jeśli gracz jest dostępny, gra rozpoczyna się natychmiast (przekierowanie do `/game/{gameId}`)
- ✅ Jeśli gracz nie jest dostępny, użytkownik otrzymuje odpowiedni komunikat (409 Conflict)

## 13. Uwagi dodatkowe

### 13.1 Wsparcie dla gości

- Goście mogą przeglądać ranking (endpoint jest publiczny)
- Goście nie mogą wyzwywać graczy (przycisk "Wyzwij" jest wyłączony)
- Goście nie widzą karty z własną pozycją (nie są w rankingu)
- Goście nie mogą wyświetlić graczy wokół (funkcjonalność wymaga autoryzacji)

### 13.2 Wydajność

- Ranking jest cache'owany po stronie backendu (Redis, TTL 5-15 minut)
- Materialized view `player_rankings` w PostgreSQL zapewnia szybkie zapytania
- Lazy loading w PrimeNG Table zmniejsza obciążenie przy dużej liczbie rekordów
- Paginacja ogranicza liczbę rekordów ładowanych jednocześnie (domyślnie 50)

### 13.3 Bezpieczeństwo

- Endpoint GET /api/v1/rankings jest publiczny (ranking jest publiczną informacją)
- Endpoint POST /api/v1/matching/challenge/{userId} wymaga autoryzacji (JWT token)
- Walidacja po stronie serwera zapobiega wyzwaniom niedostępnych graczy
- Rate limiting może być dodany w przyszłości dla wyzwań graczy

### 13.4 Przyszłe ulepszenia

- Filtrowanie rankingu (według daty, poziomu umiejętności)
- Sortowanie rankingu (według różnych kryteriów)
- Wybór rozmiaru planszy przy wyzwaniu gracza (obecnie domyślnie 3)
- Powiadomienia push dla wyzwań graczy (WebSocket)
- Statystyki gracza (historia gier, wykresy)
- Tryb obserwatora dla trwających gier PvP

