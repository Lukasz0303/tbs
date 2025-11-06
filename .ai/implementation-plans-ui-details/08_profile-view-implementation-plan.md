# Plan implementacji widoku ProfileComponent

> **Źródło**: `.ai/implementation-plans-ui/08_profile-component.md`

## 1. Przegląd

ProfileComponent to widok umożliwiający użytkownikom przeglądanie i zarządzanie swoim profilem. Komponent obsługuje zarówno gości, jak i zarejestrowanych użytkowników, wyświetlając podstawowe informacje, statystyki, pozycję w rankingu oraz ostatnią zapisaną grę z możliwością kontynuacji.

Główne funkcjonalności:
- Wyświetlanie podstawowych informacji profilu (nazwa użytkownika, email, status)
- Wyświetlanie statystyk użytkownika (punkty, rozegrane gry, wygrane)
- Wyświetlanie pozycji w rankingu (tylko dla zarejestrowanych użytkowników)
- Wyświetlanie ostatniej zapisanej gry z możliwością kontynuacji
- Edycja nazwy użytkownika (tylko dla zarejestrowanych użytkowników)
- Zachęta do rejestracji dla gości

Komponent realizuje historyjki użytkownika: US-011 (Zarządzanie profilem gracza) i US-012 (Automatyczne zapisywanie gier - kontynuacja ostatniej gry).

## 2. Routing widoku

**Ścieżka routingu**: `/profile`

**Konfiguracja routingu**:
```typescript
{
  path: 'profile',
  component: ProfileComponent
}
```

**Lokalizacja pliku routingu**: `frontend/src/app/app.routes.ts`

**Guardy**: Brak (widok dostępny dla wszystkich, ale niektóre funkcje wymagają rejestracji)

## 3. Struktura komponentów

```
ProfileComponent (główny komponent)
├── ProfileHeaderComponent (opcjonalny, nagłówek z tytułem)
├── ProfileInfoCardComponent (karta z podstawowymi informacjami)
│   ├── InputTextModule (PrimeNG - pole edycji nazwy)
│   └── ButtonModule (PrimeNG - przycisk edycji)
├── UserStatsComponent (komponent współdzielony - statystyki)
│   └── RankingBadgeComponent (opcjonalny, badge z pozycją w rankingu)
├── LastGameCardComponent (komponent współdzielony - ostatnia gra)
│   └── ButtonModule (PrimeNG - przycisk kontynuacji)
├── RegistrationEncouragementComponent (warunkowy, tylko dla gości)
│   └── ButtonModule (PrimeNG - przycisk rejestracji)
└── EditUsernameDialogComponent (dialog edycji nazwy)
    ├── InputTextModule (PrimeNG - pole nazwy)
    ├── ButtonModule (PrimeNG - przyciski zapisz/anuluj)
    └── DialogModule (PrimeNG - dialog)
```

**Hierarchia komponentów**:
- ProfileComponent jest komponentem standalone
- UserStatsComponent, LastGameCardComponent, EditUsernameDialogComponent są komponentami współdzielonymi
- Wszystkie komponenty używają PrimeNG do elementów UI
- Dialog używa PrimeNG DialogModule

## 4. Szczegóły komponentów

### ProfileComponent

**Opis komponentu**: Główny komponent widoku profilu, zarządza stanem użytkownika, rankingiem, ostatnią grą oraz dialogiem edycji nazwy użytkownika. Komponent obsługuje logikę biznesową związaną z wyświetlaniem profilu, edycją nazwy użytkownika oraz kontynuacją zapisanej gry.

**Główne elementy HTML**:
- Kontener główny (`.profile-container`)
- Sekcja nagłówka (`.profile-header`) z tytułem
- Sekcja zawartości (`.profile-content`) z kartami:
  - Karta podstawowych informacji (`.profile-info-card`)
  - Karta statystyk (`.profile-stats-card`) z `<app-user-stats>`
  - Karta ostatniej gry (`<app-last-game-card>`) - warunkowa
  - Sekcja zachęty do rejestracji (`.registration-encouragement`) - warunkowa, tylko dla gości
- Dialog edycji nazwy (`<app-edit-username-dialog>`) - warunkowy

**Obsługiwane zdarzenia**:
- `ngOnInit()` - inicjalizacja komponentu, pobranie profilu użytkownika, rankingu i ostatniej gry
- `onEditUsername()` - otwarcie dialogu edycji nazwy użytkownika
- `onCloseEditDialog()` - zamknięcie dialogu edycji nazwy
- `onSaveUsername(newUsername: string)` - zapisanie nowej nazwy użytkownika
- `onContinueGame(gameId: number)` - nawigacja do kontynuacji zapisanej gry
- `navigateToRegister()` - nawigacja do strony rejestracji
- `loadUserRanking()` - pobranie pozycji użytkownika w rankingu
- `loadLastGame()` - pobranie ostatniej zapisanej gry
- `handleUpdateError(error: HttpErrorResponse)` - obsługa błędów aktualizacji nazwy

**Obsługiwana walidacja**:
- Sprawdzenie czy użytkownik jest gościem (przed wyświetleniem opcji edycji nazwy)
- Sprawdzenie czy użytkownik jest zarejestrowany (przed pobraniem rankingu)
- Sprawdzenie czy istnieje zapisana gra (przed wyświetleniem karty ostatniej gry)
- Walidacja nazwy użytkownika w dialogu edycji (3-50 znaków, unikalność)

**Typy**:
- `User` - interfejs reprezentujący użytkownika
- `Ranking` - interfejs reprezentujący pozycję w rankingu
- `Game` - interfejs reprezentujący grę
- `Observable<User | null>` - Observable z aktualnym użytkownikiem
- `Observable<Ranking | null>` - Observable z pozycją w rankingu
- `Observable<Game | null>` - Observable z ostatnią grą
- `Observable<boolean>` - Observable ze statusem dialogu edycji
- `UpdateUserRequest` - DTO dla żądania aktualizacji nazwy użytkownika
- `UpdateUserResponse` - DTO dla odpowiedzi aktualizacji nazwy użytkownika
- `HttpErrorResponse` - Angular HTTP error response

**Propsy**: Brak (komponent główny, nie przyjmuje propsów)

### UserStatsComponent

**Opis komponentu**: Komponent współdzielony wyświetlający statystyki użytkownika (punkty, rozegrane gry, wygrane) oraz pozycję w rankingu (jeśli użytkownik jest zarejestrowany i ma pozycję w rankingu).

**Główne elementy HTML**:
- Kontener statystyk (`.user-stats`)
- Sekcja statystyk (`.stats-grid`)
  - Statystyka punktów (`.stat-item`)
  - Statystyka rozegranych gier (`.stat-item`)
  - Statystyka wygranych (`.stat-item`)
- Sekcja rankingu (`.ranking-section`) - warunkowa, tylko jeśli ranking istnieje
  - Badge z pozycją w rankingu (`.ranking-badge`)

**Obsługiwane zdarzenia**: Brak (komponent prezentacyjny)

**Obsługiwana walidacja**: Brak (komponent prezentacyjny, walidacja w komponencie rodzica)

**Typy**:
- `User` - interfejs reprezentujący użytkownika
- `Ranking | null` - interfejs reprezentujący pozycję w rankingu (opcjonalny)

**Propsy**:
- `user: User` - użytkownik (wymagane)
- `ranking: Ranking | null` - pozycja w rankingu (opcjonalne)

### LastGameCardComponent

**Opis komponentu**: Komponent współdzielony wyświetlający informacje o ostatniej zapisanej grze z możliwością kontynuacji. Komponent wyświetla typ gry, przeciwnika, status oraz przycisk kontynuacji.

**Główne elementy HTML**:
- Kontener karty (`.last-game-card`)
- Nagłówek karty (`.card-header`) z tytułem
- Informacje o grze (`.game-info`)
  - Typ gry (vs_bot / pvp)
  - Przeciwnik (bot / gracz)
  - Status gry
  - Data ostatniego ruchu
- Przycisk kontynuacji (`.continue-button`)

**Obsługiwane zdarzenia**:
- `continueGame` - EventEmitter emitujący `gameId` po kliknięciu przycisku kontynuacji

**Obsługiwana walidacja**: Brak (komponent prezentacyjny, walidacja w komponencie rodzica)

**Typy**:
- `Game` - interfejs reprezentujący grę

**Propsy**:
- `game: Game` - ostatnia gra (wymagane)

**Outputs**:
- `continueGame: EventEmitter<number>` - emisja gameId przy kontynuacji gry

### EditUsernameDialogComponent

**Opis komponentu**: Komponent współdzielony wyświetlający dialog edycji nazwy użytkownika z walidacją po stronie klienta i serwera. Komponent obsługuje zapisanie nowej nazwy użytkownika oraz zamknięcie dialogu.

**Główne elementy HTML**:
- Dialog PrimeNG (`<p-dialog>`)
- Formularz (`<form [formGroup]="editForm" (ngSubmit)="onSave()">`)
  - Pole nazwy użytkownika (`<input pInputText formControlName="username">`)
  - Komunikaty błędów walidacji
  - Przyciski akcji (`.dialog-actions`)
    - Przycisk anuluj (`<p-button label="Anuluj" (onClick)="onClose()">`)
    - Przycisk zapisz (`<p-button type="submit" label="Zapisz">`)

**Obsługiwane zdarzenia**:
- `ngOnInit()` - inicjalizacja formularza z aktualną nazwą użytkownika
- `onSave()` - walidacja i emisja zdarzenia zapisu
- `onClose()` - emisja zdarzenia zamknięcia
- `isFieldInvalid(fieldName: string)` - sprawdzenie czy pole jest nieprawidłowe
- `getFieldError(fieldName: string)` - pobranie komunikatu błędu dla pola

**Obsługiwana walidacja**:
- **Nazwa użytkownika**:
  - Wymagane (`Validators.required`)
  - Minimalna długość 3 znaki (`Validators.minLength(3)`)
  - Maksymalna długość 50 znaków (`Validators.maxLength(50)`)
  - Format alfanumeryczny z podkreślnikami i myślnikami (regex pattern)
- Walidacja po stronie serwera (API):
  - 400 Bad Request - nieprawidłowa walidacja (długość, format)
  - 409 Conflict - nazwa użytkownika już istnieje
  - 403 Forbidden - próba aktualizacji cudzego profilu

**Typy**:
- `FormGroup` - Angular Reactive Forms
- `FormControl` - Angular Reactive Forms

**Propsy**:
- `currentUsername: string` - aktualna nazwa użytkownika (wymagane)

**Outputs**:
- `close: EventEmitter<void>` - emisja przy zamknięciu dialogu
- `save: EventEmitter<string>` - emisja nowej nazwy użytkownika przy zapisie

## 5. Typy

### User (interfejs użytkownika)

```typescript
interface User {
  userId: number;
  username: string | null;
  email?: string;
  isGuest: boolean;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
  lastSeenAt?: string | null;
}
```

**Pola**:
- `userId: number` - unikalny identyfikator użytkownika (BIGINT)
- `username: string | null` - nazwa użytkownika (null dla gości)
- `email?: string` - adres email użytkownika (opcjonalne, tylko dla zarejestrowanych)
- `isGuest: boolean` - flaga wskazująca czy użytkownik jest gościem
- `totalPoints: number` - suma punktów użytkownika
- `gamesPlayed: number` - liczba rozegranych gier
- `gamesWon: number` - liczba wygranych gier
- `createdAt: string` - data utworzenia konta (ISO 8601)
- `lastSeenAt?: string | null` - data ostatniej aktywności (ISO 8601, opcjonalne)

**Uwagi**:
- Interfejs używany do reprezentacji użytkownika w komponencie
- Mapowanie z `UserProfileResponse` z endpointu GET /api/auth/me
- Email jest opcjonalny i wyświetlany tylko dla zarejestrowanych użytkowników

### Ranking (interfejs pozycji w rankingu)

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

**Pola**:
- `rankPosition: number` - pozycja w rankingu (1-based)
- `userId: number` - identyfikator użytkownika
- `username: string` - nazwa użytkownika (zawsze string, nie null dla zarejestrowanych)
- `totalPoints: number` - suma punktów użytkownika
- `gamesPlayed: number` - liczba rozegranych gier
- `gamesWon: number` - liczba wygranych gier
- `createdAt: string` - data utworzenia konta (ISO 8601)

**Uwagi**:
- Interfejs używany do reprezentacji pozycji w rankingu
- Mapowanie z `RankingDetailResponse` z endpointu GET /api/v1/rankings/{userId}
- Ranking jest dostępny tylko dla zarejestrowanych użytkowników (goście nie mają pozycji w rankingu)
- Jeśli użytkownik nie ma pozycji w rankingu, endpoint zwraca 404 Not Found

### Game (interfejs gry)

```typescript
interface Game {
  gameId: number;
  gameType: 'vs_bot' | 'pvp';
  boardSize: 3 | 4 | 5;
  status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw';
  player1Id: number;
  player2Id: number | null;
  botDifficulty: 'easy' | 'medium' | 'hard' | null;
  currentPlayerSymbol: 'X' | 'O' | null;
  createdAt: string;
  lastMoveAt?: string | null;
  finishedAt?: string | null;
  boardState: string[][];
  totalMoves: number;
}
```

**Pola**:
- `gameId: number` - unikalny identyfikator gry
- `gameType: 'vs_bot' | 'pvp'` - typ gry (vs bot lub PvP)
- `boardSize: 3 | 4 | 5` - rozmiar planszy
- `status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw'` - status gry
- `player1Id: number` - identyfikator pierwszego gracza
- `player2Id: number | null` - identyfikator drugiego gracza (null dla vs_bot)
- `botDifficulty: 'easy' | 'medium' | 'hard' | null` - poziom trudności bota (null dla PvP)
- `currentPlayerSymbol: 'X' | 'O' | null` - symbol aktualnego gracza
- `createdAt: string` - data utworzenia gry (ISO 8601)
- `lastMoveAt?: string | null` - data ostatniego ruchu (ISO 8601, opcjonalne)
- `finishedAt?: string | null` - data zakończenia gry (ISO 8601, opcjonalne)
- `boardState: string[][]` - stan planszy (2D array)
- `totalMoves: number` - całkowita liczba ruchów

**Uwagi**:
- Interfejs używany do reprezentacji gry w komponencie
- Mapowanie z `GameListItem` lub `GameDetailResponse` z endpointu GET /api/games
- Dla ostatniej gry używany jest endpoint GET /api/games?status=in_progress&size=1
- Tylko gry ze statusem `in_progress` są wyświetlane jako ostatnia gra

### UpdateUserRequest (DTO dla żądania aktualizacji)

```typescript
interface UpdateUserRequest {
  username: string;
}
```

**Pola**:
- `username: string` - nowa nazwa użytkownika (3-50 znaków)

**Uwagi**:
- DTO używane do wysłania żądania do endpointu PUT /api/v1/users/{userId}
- Walidacja po stronie klienta: `Validators.required`, `Validators.minLength(3)`, `Validators.maxLength(50)`
- Walidacja po stronie serwera: `@Size(min = 3, max = 50)` (Bean Validation)

### UpdateUserResponse (DTO dla odpowiedzi aktualizacji)

```typescript
interface UpdateUserResponse {
  userId: number;
  username: string | null;
  isGuest: boolean;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  updatedAt: string;
}
```

**Pola**:
- `userId: number` - identyfikator użytkownika
- `username: string | null` - zaktualizowana nazwa użytkownika
- `isGuest: boolean` - flaga wskazująca czy użytkownik jest gościem
- `totalPoints: number` - suma punktów użytkownika
- `gamesPlayed: number` - liczba rozegranych gier
- `gamesWon: number` - liczba wygranych gier
- `updatedAt: string` - data aktualizacji (ISO 8601)

**Uwagi**:
- DTO używane do odbioru odpowiedzi z endpointu PUT /api/v1/users/{userId}
- Po pomyślnej aktualizacji, komponent aktualizuje stan użytkownika w `AuthService`

## 6. Zarządzanie stanem

### Stan komponentu ProfileComponent

Komponent ProfileComponent zarządza stanem za pomocą Observable i BehaviorSubject:

**Observable stanu użytkownika**:
```typescript
currentUser$ = this.authService.getCurrentUser();
```
- Observable z aktualnym użytkownikiem z `AuthService`
- Automatycznie aktualizowany przy zmianie użytkownika
- Używany w template z `async` pipe

**BehaviorSubject dla rankingu**:
```typescript
userRanking$ = new BehaviorSubject<Ranking | null>(null);
```
- BehaviorSubject z pozycją użytkownika w rankingu
- Inicjalizowany jako `null` (brak rankingu)
- Aktualizowany po pobraniu rankingu z API
- Używany w template z `async` pipe

**BehaviorSubject dla ostatniej gry**:
```typescript
lastGame$ = new BehaviorSubject<Game | null>(null);
```
- BehaviorSubject z ostatnią zapisaną grą
- Inicjalizowany jako `null` (brak gry)
- Aktualizowany po pobraniu ostatniej gry z API
- Używany w template z `async` pipe

**BehaviorSubject dla dialogu edycji**:
```typescript
showEditDialog$ = new BehaviorSubject<boolean>(false);
```
- BehaviorSubject ze statusem widoczności dialogu edycji nazwy
- Inicjalizowany jako `false` (dialog ukryty)
- Aktualizowany przy otwarciu/zamknięciu dialogu
- Używany w template z `async` pipe

### Zarządzanie stanem w serwisach

**AuthService**:
- Zarządza stanem aktualnego użytkownika
- Metoda `getCurrentUser()` zwraca Observable z użytkownikiem
- Metoda `updateCurrentUser(user: User)` aktualizuje stan użytkownika

**RankingService**:
- Metoda `getUserRanking(userId: number)` zwraca Observable z pozycją w rankingu
- Obsługuje błędy 404 (użytkownik nie ma pozycji w rankingu)

**GameService**:
- Metoda `getSavedGame()` zwraca Observable z ostatnią zapisaną grą
- Pobiera gry ze statusem `in_progress` z paginacją (size=1)
- Obsługuje brak zapisanej gry (null)

**UserService**:
- Metoda `updateUser(userId: number, data: UpdateUserRequest)` zwraca Observable z zaktualizowanym użytkownikiem
- Obsługuje błędy 400, 403, 409

### Custom hooks (brak)

Komponent nie wymaga custom hooks - wszystkie operacje są obsługiwane przez serwisy i Observable.

## 7. Integracja API

### GET /api/auth/me - Pobranie profilu użytkownika

**Endpoint**: `GET /api/auth/me`

**Autoryzacja**: Wymagana (Bearer token JWT)

**Odpowiedź sukcesu (200 OK)**:
```json
{
  "userId": 42,
  "username": "player1",
  "isGuest": false,
  "totalPoints": 3500,
  "gamesPlayed": 18,
  "gamesWon": 12,
  "createdAt": "2024-01-15T10:30:00Z",
  "lastSeenAt": "2024-01-20T14:45:00Z"
}
```

**Odpowiedzi błędów**:
- 401 Unauthorized - brak uwierzytelnienia
- 404 Not Found - użytkownik nie znaleziony
- 500 Internal Server Error - błąd serwera

**Integracja w komponencie**:
- Wywołanie przez `AuthService.getCurrentUser()`
- Observable automatycznie aktualizowany przy zmianie użytkownika
- Używany w template z `async` pipe

### GET /api/v1/rankings/{userId} - Pobranie pozycji w rankingu

**Endpoint**: `GET /api/v1/rankings/{userId}`

**Autoryzacja**: Publiczne (bez wymaganej autoryzacji)

**Path Parameters**:
- `userId: number` - identyfikator użytkownika

**Odpowiedź sukcesu (200 OK)**:
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

**Odpowiedzi błędów**:
- 404 Not Found - użytkownik nie znaleziony lub użytkownik jest gościem
- 500 Internal Server Error - błąd serwera

**Integracja w komponencie**:
- Wywołanie przez `RankingService.getUserRanking(userId)`
- Wywoływane tylko dla zarejestrowanych użytkowników (`!user.isGuest`)
- Obsługa błędu 404 (użytkownik nie ma pozycji w rankingu) - ciche logowanie, brak wyświetlania rankingu
- Aktualizacja `userRanking$` BehaviorSubject

### GET /api/games - Pobranie ostatniej zapisanej gry

**Endpoint**: `GET /api/games?status=in_progress&size=1`

**Autoryzacja**: Wymagana (Bearer token JWT)

**Query Parameters**:
- `status: string` - status gry (`in_progress`)
- `size: number` - rozmiar strony (1)

**Odpowiedź sukcesu (200 OK)**:
```json
{
  "content": [
    {
      "gameId": 42,
      "gameType": "vs_bot",
      "boardSize": 3,
      "status": "in_progress",
      "player1Username": "player1",
      "player2Username": null,
      "winnerUsername": null,
      "botDifficulty": "easy",
      "totalMoves": 5,
      "createdAt": "2024-01-20T15:30:00Z",
      "lastMoveAt": "2024-01-20T15:32:00Z",
      "finishedAt": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 1,
  "number": 0,
  "first": true,
  "last": true
}
```

**Odpowiedzi błędów**:
- 401 Unauthorized - brak uwierzytelnienia
- 400 Bad Request - nieprawidłowe parametry zapytania
- 500 Internal Server Error - błąd serwera

**Integracja w komponencie**:
- Wywołanie przez `GameService.getSavedGame()`
- Obsługa braku zapisanej gry (pusta lista `content`) - ciche logowanie, brak wyświetlania karty
- Aktualizacja `lastGame$` BehaviorSubject z pierwszym elementem z `content` lub `null`

### PUT /api/v1/users/{userId} - Aktualizacja nazwy użytkownika

**Endpoint**: `PUT /api/v1/users/{userId}`

**Autoryzacja**: Wymagana (Bearer token JWT)

**Path Parameters**:
- `userId: number` - identyfikator użytkownika

**Request Body**:
```json
{
  "username": "newusername"
}
```

**Odpowiedź sukcesu (200 OK)**:
```json
{
  "userId": 42,
  "username": "newusername",
  "isGuest": false,
  "totalPoints": 3500,
  "gamesPlayed": 18,
  "gamesWon": 12,
  "updatedAt": "2024-01-20T15:30:00Z"
}
```

**Odpowiedzi błędów**:
- 400 Bad Request - nieprawidłowa walidacja (długość, format)
- 401 Unauthorized - brak uwierzytelnienia
- 403 Forbidden - próba aktualizacji cudzego profilu
- 404 Not Found - użytkownik nie znaleziony
- 409 Conflict - nazwa użytkownika już istnieje
- 500 Internal Server Error - błąd serwera

**Integracja w komponencie**:
- Wywołanie przez `UserService.updateUser(userId, { username: newUsername })`
- Wywoływane tylko dla zarejestrowanych użytkowników (`!user.isGuest`)
- Po pomyślnej aktualizacji:
  - Aktualizacja użytkownika w `AuthService.updateCurrentUser(updatedUser)`
  - Zamknięcie dialogu edycji (`showEditDialog$.next(false)`)
  - Wyświetlenie komunikatu sukcesu (toast notification)
- Obsługa błędów:
  - 409 Conflict - komunikat "Nazwa użytkownika już istnieje"
  - 403 Forbidden - komunikat "Nie masz uprawnień do aktualizacji tego profilu"
  - Inne błędy - ogólny komunikat błędu

## 8. Interakcje użytkownika

### Przeglądanie profilu

**Scenariusz**: Użytkownik otwiera widok profilu (`/profile`)

**Kroki**:
1. Komponent inicjalizuje się (`ngOnInit()`)
2. Pobiera aktualnego użytkownika z `AuthService.getCurrentUser()`
3. Jeśli użytkownik jest zarejestrowany (`!user.isGuest`):
   - Pobiera pozycję w rankingu (`loadUserRanking()`)
4. Pobiera ostatnią zapisaną grę (`loadLastGame()`)
5. Wyświetla profil z podstawowymi informacjami, statystykami i ostatnią grą

**Oczekiwany wynik**:
- Wyświetlenie profilu użytkownika z wszystkimi informacjami
- Wyświetlenie pozycji w rankingu (jeśli użytkownik jest zarejestrowany i ma pozycję)
- Wyświetlenie karty ostatniej gry (jeśli istnieje zapisana gra)
- Wyświetlenie zachęty do rejestracji (jeśli użytkownik jest gościem)

### Edycja nazwy użytkownika

**Scenariusz**: Zarejestrowany użytkownik chce zmienić swoją nazwę użytkownika

**Kroki**:
1. Użytkownik klika przycisk "Edytuj" obok nazwy użytkownika
2. Komponent otwiera dialog edycji (`onEditUsername()`)
3. Dialog wyświetla aktualną nazwę użytkownika w polu tekstowym
4. Użytkownik wprowadza nową nazwę użytkownika
5. Walidacja po stronie klienta (3-50 znaków, format alfanumeryczny)
6. Użytkownik klika przycisk "Zapisz"
7. Komponent wysyła żądanie PUT /api/v1/users/{userId}
8. Po pomyślnej aktualizacji:
   - Aktualizacja użytkownika w `AuthService`
   - Zamknięcie dialogu
   - Wyświetlenie komunikatu sukcesu

**Oczekiwany wynik**:
- Dialog edycji otwiera się z aktualną nazwą użytkownika
- Walidacja po stronie klienta działa poprawnie
- Po zapisaniu, nazwa użytkownika jest aktualizowana w profilu
- Komunikat sukcesu jest wyświetlany

**Obsługa błędów**:
- 409 Conflict - wyświetlenie komunikatu "Nazwa użytkownika już istnieje"
- 403 Forbidden - wyświetlenie komunikatu "Nie masz uprawnień do aktualizacji tego profilu"
- 400 Bad Request - wyświetlenie komunikatu z błędami walidacji
- Inne błędy - wyświetlenie ogólnego komunikatu błędu

### Kontynuacja zapisanej gry

**Scenariusz**: Użytkownik chce kontynuować ostatnią zapisaną grę

**Kroki**:
1. Użytkownik widzi kartę ostatniej gry w profilu
2. Użytkownik klika przycisk "Kontynuuj grę"
3. Komponent emituje zdarzenie `continueGame` z `gameId`
4. Komponent nawiguje do widoku gry (`/game/{gameId}`)

**Oczekiwany wynik**:
- Nawigacja do widoku gry z kontynuacją zapisanej gry
- Stan gry jest przywrócony z bazy danych

### Zachęta do rejestracji (dla gości)

**Scenariusz**: Gość przegląda swój profil i widzi zachętę do rejestracji

**Kroki**:
1. Gość otwiera widok profilu (`/profile`)
2. Komponent wyświetla sekcję zachęty do rejestracji
3. Gość klika przycisk "Zarejestruj się"
4. Komponent nawiguje do strony rejestracji (`/auth/register`)

**Oczekiwany wynik**:
- Nawigacja do strony rejestracji
- Po rejestracji, użytkownik może wrócić do profilu i zobaczyć pełne funkcje

## 9. Warunki i walidacja

### Warunki wyświetlania elementów

**Przycisk edycji nazwy użytkownika**:
- Warunek: `!user.isGuest` (tylko dla zarejestrowanych użytkowników)
- Komponent: ProfileComponent
- Wpływ na stan: Przycisk jest ukryty dla gości

**Pole email**:
- Warunek: `!user.isGuest` (tylko dla zarejestrowanych użytkowników)
- Komponent: ProfileComponent
- Wpływ na stan: Pole email jest ukryte dla gości

**Pozycja w rankingu**:
- Warunek: `!user.isGuest && ranking !== null` (tylko dla zarejestrowanych użytkowników z pozycją w rankingu)
- Komponent: UserStatsComponent
- Wpływ na stan: Sekcja rankingu jest ukryta, jeśli użytkownik nie ma pozycji w rankingu

**Karta ostatniej gry**:
- Warunek: `lastGame !== null` (tylko jeśli istnieje zapisana gra)
- Komponent: ProfileComponent
- Wpływ na stan: Karta jest ukryta, jeśli nie ma zapisanej gry

**Sekcja zachęty do rejestracji**:
- Warunek: `user.isGuest` (tylko dla gości)
- Komponent: ProfileComponent
- Wpływ na stan: Sekcja jest ukryta dla zarejestrowanych użytkowników

### Walidacja nazwy użytkownika

**Walidacja po stronie klienta (EditUsernameDialogComponent)**:
- Wymagane: `Validators.required`
- Minimalna długość: `Validators.minLength(3)` - minimum 3 znaki
- Maksymalna długość: `Validators.maxLength(50)` - maksimum 50 znaków
- Format: regex pattern `^[a-zA-Z0-9_-]+$` - tylko alfanumeryczne znaki, podkreślniki i myślniki

**Walidacja po stronie serwera (API)**:
- Wymagane: `@NotBlank` (Bean Validation)
- Długość: `@Size(min = 3, max = 50)` (Bean Validation)
- Unikalność: sprawdzenie w bazie danych (UNIQUE constraint)
- Format: walidacja formatu (alfanumeryczne znaki, podkreślniki, myślniki)

**Obsługa błędów walidacji**:
- 400 Bad Request - błędy walidacji (długość, format) - wyświetlenie komunikatów błędów w dialogu
- 409 Conflict - nazwa użytkownika już istnieje - wyświetlenie komunikatu "Nazwa użytkownika już istnieje"
- 403 Forbidden - próba aktualizacji cudzego profilu - wyświetlenie komunikatu "Nie masz uprawnień do aktualizacji tego profilu"

### Warunki pobierania danych

**Pobieranie rankingu**:
- Warunek: `user !== null && !user.isGuest` (tylko dla zarejestrowanych użytkowników)
- Komponent: ProfileComponent
- Obsługa błędu 404: ciche logowanie, brak wyświetlania rankingu (`userRanking$` pozostaje `null`)

**Pobieranie ostatniej gry**:
- Warunek: zawsze (dla wszystkich użytkowników)
- Komponent: ProfileComponent
- Obsługa braku gry: ciche logowanie, brak wyświetlania karty (`lastGame$` pozostaje `null`)

## 10. Obsługa błędów

### Błędy pobierania profilu użytkownika

**Scenariusz**: Błąd podczas pobierania profilu użytkownika (GET /api/auth/me)

**Obsługa**:
- 401 Unauthorized - przekierowanie do strony logowania (`/auth/login`)
- 404 Not Found - wyświetlenie komunikatu błędu, możliwość ponowienia
- 500 Internal Server Error - wyświetlenie komunikatu błędu, możliwość ponowienia

**Implementacja**:
```typescript
this.authService.getCurrentUser().subscribe({
  next: (user) => {
    // Obsługa sukcesu
  },
  error: (error) => {
    if (error.status === 401) {
      this.router.navigate(['/auth/login']);
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Błąd',
        detail: 'Nie udało się pobrać profilu użytkownika'
      });
    }
  }
});
```

### Błędy pobierania rankingu

**Scenariusz**: Błąd podczas pobierania pozycji w rankingu (GET /api/v1/rankings/{userId})

**Obsługa**:
- 404 Not Found - ciche logowanie, brak wyświetlania rankingu (użytkownik nie ma pozycji w rankingu lub jest gościem)
- 500 Internal Server Error - ciche logowanie, brak wyświetlania rankingu

**Implementacja**:
```typescript
this.rankingService.getUserRanking(userId).subscribe({
  next: (ranking) => {
    this.userRanking$.next(ranking);
  },
  error: (error) => {
    console.error('Error loading user ranking:', error);
    // Ciche logowanie, brak wyświetlania rankingu
  }
});
```

### Błędy pobierania ostatniej gry

**Scenariusz**: Błąd podczas pobierania ostatniej zapisanej gry (GET /api/games?status=in_progress&size=1)

**Obsługa**:
- 401 Unauthorized - ciche logowanie, brak wyświetlania karty
- 400 Bad Request - ciche logowanie, brak wyświetlania karty
- 500 Internal Server Error - ciche logowanie, brak wyświetlania karty

**Implementacja**:
```typescript
this.gameService.getSavedGame().subscribe({
  next: (game) => {
    this.lastGame$.next(game);
  },
  error: (error) => {
    console.error('Error loading last game:', error);
    // Ciche logowanie, brak wyświetlania karty
  }
});
```

### Błędy aktualizacji nazwy użytkownika

**Scenariusz**: Błąd podczas aktualizacji nazwy użytkownika (PUT /api/v1/users/{userId})

**Obsługa**:
- 400 Bad Request - wyświetlenie komunikatów błędów walidacji w dialogu
- 401 Unauthorized - wyświetlenie komunikatu błędu, przekierowanie do logowania
- 403 Forbidden - wyświetlenie komunikatu "Nie masz uprawnień do aktualizacji tego profilu"
- 404 Not Found - wyświetlenie komunikatu "Użytkownik nie znaleziony"
- 409 Conflict - wyświetlenie komunikatu "Nazwa użytkownika już istnieje"
- 500 Internal Server Error - wyświetlenie ogólnego komunikatu błędu

**Implementacja**:
```typescript
this.userService.updateUser(userId, { username: newUsername }).subscribe({
  next: (updatedUser) => {
    this.messageService.add({
      severity: 'success',
      summary: 'Sukces',
      detail: 'Nazwa użytkownika została zaktualizowana'
    });
    this.authService.updateCurrentUser(updatedUser);
    this.showEditDialog$.next(false);
  },
  error: (error) => {
    this.handleUpdateError(error);
  }
});

private handleUpdateError(error: HttpErrorResponse): void {
  let message = 'Nie udało się zaktualizować nazwy użytkownika';
  
  if (error.status === 409) {
    message = 'Nazwa użytkownika już istnieje';
  } else if (error.status === 403) {
    message = 'Nie masz uprawnień do aktualizacji tego profilu';
  } else if (error.status === 404) {
    message = 'Użytkownik nie znaleziony';
  } else if (error.status === 401) {
    message = 'Sesja wygasła. Zaloguj się ponownie';
    this.router.navigate(['/auth/login']);
  }
  
  this.messageService.add({
    severity: 'error',
    summary: 'Błąd',
    detail: message
  });
}
```

### Obsługa przypadków brzegowych

**Brak użytkownika**:
- Jeśli `currentUser$` jest `null`, komponent wyświetla komunikat o braku użytkownika lub przekierowuje do logowania

**Brak rankingu**:
- Jeśli użytkownik nie ma pozycji w rankingu (404), komponent nie wyświetla sekcji rankingu (ciche logowanie)

**Brak zapisanej gry**:
- Jeśli nie ma zapisanej gry (pusta lista lub błąd), komponent nie wyświetla karty ostatniej gry (ciche logowanie)

**Gość próbuje edytować nazwę**:
- Przycisk edycji jest ukryty dla gości (`*ngIf="!user.isGuest"`), więc gość nie może edytować nazwy

## 11. Kroki implementacji

### Krok 1: Przygotowanie struktury komponentu

1. Utworzenie komponentu ProfileComponent:
   - Lokalizacja: `frontend/src/app/features/profile/profile.component.ts`
   - Plik HTML: `frontend/src/app/features/profile/profile.component.html`
   - Plik SCSS: `frontend/src/app/features/profile/profile.component.scss`
   - Komponent standalone z importami: `CommonModule`, `RouterModule`, `AsyncPipe`, `ButtonModule`

2. Konfiguracja routingu:
   - Dodanie routu `/profile` w `app.routes.ts`
   - Komponent: `ProfileComponent`

### Krok 2: Implementacja podstawowej struktury template

1. Utworzenie struktury HTML:
   - Kontener główny (`.profile-container`)
   - Nagłówek (`.profile-header`)
   - Sekcja zawartości (`.profile-content`)
   - Karta podstawowych informacji (`.profile-info-card`)
   - Karta statystyk (`.profile-stats-card`)

2. Dodanie warunków wyświetlania:
   - `*ngIf="currentUser$ | async as user"` dla całej zawartości
   - `*ngIf="!user.isGuest"` dla przycisku edycji i pola email
   - `*ngIf="user.isGuest"` dla sekcji zachęty do rejestracji

### Krok 3: Implementacja logiki komponentu

1. Inicjalizacja komponentu:
   - Implementacja `ngOnInit()` z pobraniem profilu, rankingu i ostatniej gry
   - Implementacja `loadUserRanking()` z obsługą błędów
   - Implementacja `loadLastGame()` z obsługą błędów

2. Implementacja metod obsługi zdarzeń:
   - `onEditUsername()` - otwarcie dialogu edycji
   - `onCloseEditDialog()` - zamknięcie dialogu
   - `onSaveUsername(newUsername: string)` - zapisanie nowej nazwy
   - `onContinueGame(gameId: number)` - nawigacja do gry
   - `navigateToRegister()` - nawigacja do rejestracji
   - `handleUpdateError(error: HttpErrorResponse)` - obsługa błędów aktualizacji

### Krok 4: Implementacja komponentu UserStatsComponent

1. Utworzenie komponentu UserStatsComponent:
   - Lokalizacja: `frontend/src/app/components/profile/user-stats.component.ts`
   - Komponent standalone z importami: `CommonModule`, `AsyncPipe`

2. Implementacja template:
   - Wyświetlanie statystyk (punkty, rozegrane gry, wygrane)
   - Wyświetlanie pozycji w rankingu (warunkowo, jeśli ranking istnieje)

3. Implementacja logiki:
   - Inputy: `user: User`, `ranking: Ranking | null`
   - Wyświetlanie statystyk z obiektu `user`
   - Wyświetlanie pozycji w rankingu z obiektu `ranking` (jeśli istnieje)

### Krok 5: Implementacja komponentu LastGameCardComponent

1. Utworzenie komponentu LastGameCardComponent:
   - Lokalizacja: `frontend/src/app/components/profile/last-game-card.component.ts`
   - Komponent standalone z importami: `CommonModule`, `ButtonModule`

2. Implementacja template:
   - Wyświetlanie informacji o grze (typ, przeciwnik, status, data ostatniego ruchu)
   - Przycisk kontynuacji gry

3. Implementacja logiki:
   - Input: `game: Game`
   - Output: `continueGame: EventEmitter<number>`
   - Metoda `onContinue()` - emisja `gameId`

### Krok 6: Implementacja komponentu EditUsernameDialogComponent

1. Utworzenie komponentu EditUsernameDialogComponent:
   - Lokalizacja: `frontend/src/app/components/profile/edit-username-dialog.component.ts`
   - Komponent standalone z importami: `CommonModule`, `ReactiveFormsModule`, `InputTextModule`, `ButtonModule`, `DialogModule`

2. Implementacja template:
   - Dialog PrimeNG (`<p-dialog>`)
   - Formularz z polem nazwy użytkownika
   - Komunikaty błędów walidacji
   - Przyciski akcji (anuluj, zapisz)

3. Implementacja logiki:
   - Input: `currentUsername: string`
   - Outputs: `close: EventEmitter<void>`, `save: EventEmitter<string>`
   - Formularz reactive z walidacją (required, minLength, maxLength, pattern)
   - Metoda `onSave()` - walidacja i emisja nowej nazwy
   - Metoda `onClose()` - emisja zamknięcia
   - Metody pomocnicze: `isFieldInvalid()`, `getFieldError()`

### Krok 7: Integracja z serwisami

1. Implementacja wywołań API:
   - `AuthService.getCurrentUser()` - pobranie profilu użytkownika
   - `RankingService.getUserRanking(userId)` - pobranie rankingu
   - `GameService.getSavedGame()` - pobranie ostatniej gry
   - `UserService.updateUser(userId, data)` - aktualizacja nazwy użytkownika

2. Obsługa odpowiedzi API:
   - Mapowanie DTO na interfejsy TypeScript
   - Aktualizacja stanu komponentu (BehaviorSubject)
   - Obsługa błędów z odpowiednimi komunikatami

### Krok 8: Implementacja stylowania

1. Utworzenie pliku SCSS:
   - Lokalizacja: `frontend/src/app/features/profile/profile.component.scss`
   - Style dla kontenera, kart, sekcji
   - Responsywność (grid layout)
   - Animacje (fade-in dla kart, slide-in dla dialogu)

2. Style dla komponentów współdzielonych:
   - `user-stats.component.scss` - style dla statystyk
   - `last-game-card.component.scss` - style dla karty ostatniej gry
   - `edit-username-dialog.component.scss` - style dla dialogu

### Krok 9: Implementacja i18n

1. Dodanie kluczy tłumaczeń:
   - `profile.title` - "Profil użytkownika"
   - `profile.username` - "Nazwa użytkownika"
   - `profile.email` - "Email"
   - `profile.status` - "Status"
   - `profile.stats` - "Statystyki"
   - `profile.edit` - "Edytuj"
   - `profile.registerEncouragement` - "Chcesz śledzić swoje postępy?"
   - `profile.registerButton` - "Zarejestruj się"
   - Komunikaty błędów i sukcesu

2. Integracja z TranslateService:
   - Użycie pipe `translate` w template
   - Użycie `TranslateService` w komponencie (dla dynamicznych komunikatów)

### Krok 10: Testy jednostkowe

1. Testy ProfileComponent:
   - Test wyświetlania profilu użytkownika
   - Test edycji nazwy użytkownika
   - Test kontynuacji gry
   - Test zachęty do rejestracji (dla gości)
   - Test obsługi błędów

2. Testy komponentów współdzielonych:
   - Test UserStatsComponent (wyświetlanie statystyk i rankingu)
   - Test LastGameCardComponent (wyświetlanie gry i kontynuacja)
   - Test EditUsernameDialogComponent (walidacja i zapis)

### Krok 11: Testy E2E (Cypress)

1. Scenariusz: Przeglądanie profilu (zarejestrowany użytkownik):
   - Logowanie użytkownika
   - Nawigacja do profilu
   - Weryfikacja wyświetlania podstawowych informacji
   - Weryfikacja wyświetlania statystyk
   - Weryfikacja wyświetlania rankingu
   - Weryfikacja wyświetlania ostatniej gry (jeśli istnieje)

2. Scenariusz: Edycja nazwy użytkownika:
   - Otwarcie dialogu edycji
   - Wprowadzenie nowej nazwy użytkownika
   - Walidacja po stronie klienta
   - Zapisanie nowej nazwy
   - Weryfikacja aktualizacji w profilu

3. Scenariusz: Kontynuacja zapisanej gry:
   - Wyświetlenie karty ostatniej gry
   - Kliknięcie przycisku kontynuacji
   - Weryfikacja nawigacji do widoku gry

4. Scenariusz: Profil gościa:
   - Otwarcie profilu jako gość
   - Weryfikacja wyświetlania podstawowych informacji
   - Weryfikacja braku opcji edycji
   - Weryfikacja wyświetlania zachęty do rejestracji
   - Kliknięcie przycisku rejestracji
   - Weryfikacja nawigacji do strony rejestracji

### Krok 12: Dostępność (a11y)

1. Dodanie ARIA labels:
   - `aria-label` dla przycisków
   - `aria-describedby` dla pól formularza
   - `aria-live` dla komunikatów błędów i sukcesu

2. Keyboard navigation:
   - Obsługa klawisza Enter w formularzu edycji
   - Obsługa klawisza Escape w dialogu
   - Focus management w dialogu

3. Screen reader support:
   - Semantyczne znaczniki HTML
   - Opisy dla statystyk i rankingu
   - Komunikaty dla zmian stanu

### Krok 13: Code review i optymalizacja

1. Code review:
   - Sprawdzenie zgodności z zasadami implementacji
   - Weryfikacja obsługi błędów
   - Weryfikacja walidacji
   - Weryfikacja dostępności

2. Optymalizacja:
   - Optymalizacja wywołań API (unikanie niepotrzebnych żądań)
   - Optymalizacja renderowania (OnPush change detection)
   - Optymalizacja stylów (unikanie niepotrzebnych selektorów)

### Krok 14: Dokumentacja i wdrożenie

1. Dokumentacja:
   - Aktualizacja README z informacjami o widoku profilu
   - Dokumentacja komponentów współdzielonych
   - Dokumentacja API integration

2. Wdrożenie:
   - Merge do głównej gałęzi przez PR
   - Weryfikacja w środowisku deweloperskim
   - Test z różnymi scenariuszami użytkownika (gość, zarejestrowany)
   - Weryfikacja integracji z innymi widokami

