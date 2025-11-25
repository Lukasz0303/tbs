# Specyfikacja modułu autoryzacji (rejestracja, logowanie, odzyskiwanie hasła)

## Kontekst i cel

Moduł auth musi spełnić wymagania z `PRD` dla World at War: Turn‑Based Strategy: szybkie wejście gościa, pełna rejestracja/login, bezpieczne tokeny JWT w httpOnly+SameSite cookie, natychmiastowe unieważnianie sesji przez Redis oraz odzyskiwanie hasła. Specyfikacja poniżej scala wymagania UX (PrimeNG Verona, Angular 17) z warstwą backendową Spring Boot 3 z własną implementacją JWT (bez Supabase Auth) tak, aby nie naruszyć istniejącej rozgrywki, rankingu ani wydajności. W miejscach, gdzie proszono o rozdzielenie „stron Astro” i „formularzy React”, przyjmujemy analogiczne odwzorowanie: strony/layouty SSR odpowiadają komponentom routingu Angular/Angular Universal, a interaktywne formularze to samodzielne komponenty klienta (standalone Angular z Reactive Forms) osadzane w tych stronach.

## 1. Architektura interfejsu użytkownika

### 1.1 Tryby layoutu i nawigacja

- `PublicShellLayout` (analog strony Astro, SSR): lekki layout dla gości (landing, ranking, logowanie, rejestracja, reset hasła). Odpowiada za meta tagi, top-nav, komunikaty globalne i inicjalny fetch `GET /auth/me` (jeśli istnieje cookie) poprzez resolver serwerowy.
- `AuthShellLayout`: używany po udanym logowaniu (dashboard, profile, plansze gry). Zapewnia dostęp do guardów (`AuthGuard`, `GuestGuard`) i przekazuje `AuthState` do komponentów gry.
- `DialogShell` (modal PrimeNG) dla mikro-flow (np. reset hasła w trakcie gry) – formularz klienta jest ładowany lazy w modalu, ale korzysta z tych samych usług co strony główne.

Nawigacja:
- Angular Router konfiguruje dwie główne sekcje (`/auth/...`, `/app/...`). `AuthGuard` przed wejściem na `/app` sprawdza ważność tokena przez `GET /api/v1/auth/me`; w razie 401 `AuthInterceptor` automatycznie wylogowuje użytkownika i przekierowuje do `/auth/login`.
- Guard `GuestOnlyGuard` blokuje dostęp użytkownika zalogowanego do `/auth/login` i `/auth/register`, kierując go do `/app/dashboard`.

### 1.2 Strony i layouty (warstwa SSR / analog Astro)

| Strona (ścieżka) | Rola i zawartość | Wejścia/wyjścia | Integracje |
| --- | --- | --- | --- |
| `AuthLandingPage` (`/auth`) | CTA do trybu gościa lub logowania; sekcja o poziomach trudności i rankingu | Pobiera `AuthState` aby ukryć CTA logowania dla zalogowanych | `PublicShellLayout`, `CallToActionPanel`, `GuestStartButton` |
| `LoginPage` (`/auth/login`) | Renderuje hero + `LoginFormComponent` | Odbiera query `redirectTo` i przekazuje go do formularza; SSR sprawdza black-listę tokenów | `AuthFormShellComponent`, `AuthApiService` |
| `RegisterPage` (`/auth/register`) | Formularz rejestracji + informacje o rankingach | Po sukcesie przekierowuje do `/app/onboarding` | `RegisterFormComponent`, `PasswordRulesPanel` |
| `ForgotPasswordPage` (`/auth/forgot-password`) | Formularz podania emaila do resetu, weryfikacja Captcha | Wyświetla stan `requestId` i cooldown | `PasswordRecoveryRequestComponent`, `CaptchaWidget` |
| `ResetPasswordPage` (`/auth/reset-password/:token`) | Formularz ustawienia nowego hasła | Resolver SSR waliduje token (HEAD `/auth/password/token/{id}`) i przekazuje status do klienta, by uniknąć mrugnięcia UI | `PasswordResetFormComponent`, `TokenStatusBanner` |
| `LogoutPage` (`/auth/logout`) | Informuje o wylogowaniu na wszystkich urządzeniach, pokazuje przyciski przejścia | SSR usuwa `authUser` z TransferState, wymusza czyszczenie store | `SessionStatusBannerComponent` |

### 1.3 Komponenty formularzy (warstwa klienta / analog React)

- `AuthFormShellComponent`: wspólna rama formularzy (nagłówek, krokowanie, panel błędów). Propaguje zdarzenia `submitted`, `cancelled`, `routeChanged`.
- `LoginFormComponent`: Reactive Form z polami `email`, `password`. Emituje `loginSuccess(AuthUser)` oraz `loginError(AuthError)`. Deleguje zapytanie do `AuthApiService.login`.
- `RegisterFormComponent`: pola `username`, `email`, `password`, `passwordConfirm`, `termsAccepted`. Weryfikuje hasło wg polityk i automatycznie wywołuje `AuthApiService.register`, a po sukcesie strzela `autoLoginRequested`.
- `PasswordRecoveryRequestComponent`: pole `email`, slot na Captcha/Proof-of-Work. Pokazuje stan rate limitu (np. 5 prób/min/IP). Wysyła `AuthApiService.requestPasswordReset`.
- `PasswordResetFormComponent`: `password`, `passwordConfirm`; otrzymuje `resetToken` i `requestId` przez `@Input`. Po sukcesie pokazuje `SuccessState` i CTA do logowania.
- `AuthMessagesComponent`: centralny komponent do renderowania błędów walidacji i błędów API (mapa kodów -> teksty i ikony PrimeNG).
- `GuestStartButton`: inicjuje flow gościa (`/app/guest-session`) i komunikuje ograniczenia (brak możliwości resetu hasła).

Każdy komponent korzysta z `AuthAnalyticsService` do logowania zdarzeń (success, invalid, rate-limit) do warstwy telemetrycznej.

### 1.4 Walidacja i komunikaty

| Pole | Reguły UI | Komunikat PL/EN | Notatki bezpieczeństwa |
| --- | --- | --- | --- |
| `email` | regex RFC 5322, max 254, lowercase trim | „Podaj poprawny adres email” | Maskowanie wartości w logach |
| `password` | min 12 znaków, co najmniej 1 litera, 1 cyfra, 1 znak specjalny, brak 3 identycznych znaków z rzędu | „Hasło musi mieć ≥12 znaków...” | Strength meter PrimeNG |
| `passwordConfirm` | musi równać się `password` | „Hasła są różne” | Walidator cross-field |
| `username` | 3-24 znaki, alfanum + `_` lub `-`, unikalny | „Nazwa zajęta” (mapowanie status 409) | UI wykorzystuje debounce walidację serwerową |
| `resetToken` | sprawdzany tylko serwerowo, UI pokazuje status (ważny/zużyty/wygasły) | „Link do resetu wygasł” | Resolwer SSR odpytuje backend |

Komunikaty błędów globalnych:
- 401 -> „Sesja wygasła. Zaloguj się ponownie.”
- 429 -> „Za dużo prób. Spróbuj za chwilę (czas do odblokowania).”
- 422 -> „Nieprawidłowe dane. Popraw pola oznaczone na czerwono.”

### 1.5 Scenariusze biznesowe

1. **Logowanie istniejącego użytkownika**: użytkownik wchodzi na `/auth/login`, SSR wykrywa token -> redirect do `/app` lub pozostaje gdy brak. Formularz waliduje dane, w razie 2FA (jeśli zostanie dodane) potrafi rozszerzyć sekwencję. Po sukcesie `AuthStore` i `GameStateService` pobierają zaległe stany gier.
2. **Rejestracja + auto-login**: po poprawnym `POST /auth/register` backend tworzy konto, zapisuje hashed password (BCrypt), emituje event `USER_REGISTERED` (np. dla rankingu). Backend zwraca `Set-Cookie` dla refresh tokena i access token w nagłówku; UI przenosi do `/app/onboarding`.
3. **Reset hasła**: użytkownik wysyła email -> backend generuje `PasswordResetToken` (UUID, TTL 30 min) i wysyła link. Po kliknięciu, strona z resolverem SSR weryfikuje token i renderuje formularz zmiany. Po sukcesie aktywne sesje są unieważniane w Redisie.
4. **Gość przechodzi do rejestracji**: w trakcie gry gość wybiera „Zapisz progres” -> otwiera się `RegisterDialog`. Po sukcesie backend konsoliduje zapisany stan gry z kontem użytkownika.
5. **Rate limit**: Po 5 błędnych logowaniach/min/IP UI pokazuje countdown, a backend ustawia `Retry-After`. Formularz blokuje przycisk i pozwala tylko na wklejenie kodu odzyskiwania (jeśli dostępny).

### 1.6 Stan klienta, tokeny i nawigacja

- `AuthService` (Angular signals) przechowuje `currentUserSignal`, `isGuestSignal`. Inicjalizowany z localStorage przy starcie aplikacji, odświeżany przez `loadCurrentUser()` wywołujące `GET /api/v1/auth/me`.
- `AuthService` kapsułkuje wywołania HTTP (`/api/v1/auth/...`). Wszystkie żądania używają `withCredentials: true` aby przesyłać httpOnly cookie z tokenem.
- `AuthInterceptor` przechwytuje odpowiedzi HTTP. W przypadku 401/403 automatycznie wywołuje `logout()` i czyści stan użytkownika.
- `AuthGuard` sprawdza `AuthService.isAuthenticated()`. Jeśli brak danych, wykonuje `loadCurrentUser()`. W przypadku 401 redirect do `/auth/login?redirectTo=<current>`.
- `GuestGuard` chroni ścieżki tylko dla gości (np. rejestracja). W razie aktywnej sesji redirectuje do `redirectTo` lub `/app`.
- Lokalny cache: `wow-current-user` w localStorage (tylko nietajne dane użytkownika). Tokeny dostępne wyłącznie w httpOnly cookie `authToken`; UI bazuje na `User` z API.

### 1.7 i18n, dostępność, responsywność

- Wszystkie teksty korzystają z `ngx-translate` (en jako domyślny, pl jako dodatkowy), klucze `auth.login.error.invalidCredentials`, itp.
- Formularze wykorzystują semantyczne `label` + `input` i komunikaty ARIA (`aria-live="polite"` dla błędów).
- PrimeNG Verona Theme zapewnia spójność wizualną; formularze mają dwa breakpoints (≥992px grid 2 kolumny, <992px stack). Przycisk CTA jest zawsze dostępny w viewport.

## 2. Logika backendowa

### 2.1 Moduły i komponenty Spring Boot

- `AuthController` (`/api/v1/auth`) – REST API dla autentykacji.
- `AuthService` – logika rejestracji/logowania, deleguje do `UserRepository` i `JwtTokenProvider`.
- `JwtTokenProvider` – tworzenie i walidacja JWT (HMAC-SHA256, domyślnie 3600s/1h, konfigurowalne przez `app.jwt.expiration`), zawiera cache claims w pamięci.
- `TokenBlacklistService` – zarządza unieważnianiem tokenów w Redis (klucze `token:blacklist:{tokenId}` z TTL = pozostały czas życia tokena).
- `JwtAuthenticationFilter` – filtr Spring Security sprawdzający token z cookie `authToken` i blacklistę przed każdym żądaniem.
- `RateLimitingService` – używa Redis (klucze `auth:login:ip:{ip}`, `auth:login:account:{email}`, `auth:register:ip:{ip}`, itp.) do limitów 5 prób/15min (login) i 3/h (register).
- `UserRepository` – dostęp do tabeli `users` w PostgreSQL (Supabase).
- `PasswordEncoder` (BCrypt) – hashowanie haseł przy rejestracji i weryfikacja przy logowaniu.
- `SecurityConfig` – konfiguracja Spring Security z `SecurityFilterChain`, CORS, wyłączeniem CSRF (stateless JWT).

### 2.2 Modele danych i kontrakty

| Model | Pola kluczowe | Uwagi |
| --- | --- | --- |
| `User` (Entity) | `id`, `email`, `username`, `passwordHash`, `isGuest`, `avatar`, `totalPoints`, `gamesPlayed`, `gamesWon`, `createdAt`, `lastSeenAt` | Hash BCrypt (domyślny cost), email unikalny dla zarejestrowanych |
| `UserProfileResponse` | `userId`, `username`, `email`, `isGuest`, `avatar`, `totalPoints`, `gamesPlayed`, `gamesWon`, `createdAt`, `lastSeenAt` | Zwracany przez `GET /api/v1/auth/me` |
| `RegisterRequest` | `username`, `email`, `password`, `avatar?` | Walidacja @Email, @Pattern, @Size |
| `LoginRequest` | `email`, `password` | Walidacja @Email, @NotBlank |
| `LoginResponse` | `userId`, `username`, `email`, `isGuest`, `avatar`, `totalPoints`, `gamesPlayed`, `gamesWon` | Token ustawiany w httpOnly cookie `authToken` |
| `RegisterResponse` | `userId`, `username`, `email`, `isGuest`, `avatar`, `totalPoints`, `gamesPlayed`, `gamesWon` | Token ustawiany w httpOnly cookie `authToken` |
| `LogoutResponse` | `message` | Potwierdzenie wylogowania |

### 2.3 Endpointy API

| Endpoint | Opis | Wejście | Wyjście | Statusy |
| --- | --- | --- | --- | --- |
| `POST /api/v1/auth/register` | Tworzy konto, automatycznie loguje | `RegisterRequest` | `RegisterResponse`, `Set-Cookie: authToken` (httpOnly, SameSite=Lax, Secure opcjonalne) | 201, 400, 409, 422, 429 |
| `POST /api/v1/auth/login` | Logowanie użytkownika | `LoginRequest` | `LoginResponse` + `Set-Cookie: authToken` | 200, 400, 401, 422, 429 |
| `POST /api/v1/auth/logout` | Unieważnia token (blacklist w Redis) | Cookie `authToken` | `LogoutResponse`, czyści cookie | 200, 401 |
| `GET /api/v1/auth/me` | Zwraca aktualnego użytkownika | Cookie `authToken` | `UserProfileResponse` | 200, 401, 404 |

Uwagi:
- Wszystkie endpointy wymagają `withCredentials: true` w żądaniach HTTP (Angular `HttpClient`).
- Token JWT jest przechowywany wyłącznie w httpOnly cookie `authToken` (maxAge=3600s, path=/).
- `JwtAuthenticationFilter` automatycznie wyodrębnia token z cookie i weryfikuje go przed każdym żądaniem do chronionych endpointów.
- Rate limiting: login 5 prób/15min per IP i per account, register 3 próby/h per IP i per email/username.

### 2.4 Walidacja i reguły biznesowe

- Rejestracja: weryfikacja emaila (unikalność w bazie), username (unikalność, 3-24 znaki, alfanum + `_` lub `-`), hasło (min 12 znaków, walidacja po stronie backendu), rate limit 3 próby/h per IP i per email/username.
- Logowanie: przed porównaniem hasła sprawdzany jest rate limit (5 prób/15min per IP i per account). Używana jest `BCryptPasswordEncoder` (domyślny cost). Weryfikacja hasła przez `passwordEncoder.matches()`.
- Tokeny JWT: generowane przez `JwtTokenProvider` z UUID jako `jti` (token ID), zawierają `sub` (userId), `iat`, `exp`. Domyślny czas życia: 3600s (konfigurowalne przez `app.jwt.expiration`).
- Wylogowanie: token jest dodawany do blacklisty w Redis (`token:blacklist:{tokenId}`) z TTL równym pozostałemu czasowi życia tokena. `JwtAuthenticationFilter` sprawdza blacklistę przed każdym żądaniem.
- Gość -> rejestracja: użytkownik gość może przejść do rejestracji, zachowując swoje gry i punkty (przyszła funkcjonalność).

### 2.5 Obsługa tokenów, sesji i Redis

- Redis przechowuje:
  - `token:blacklist:{tokenId}` – TTL = pozostały czas życia tokena (automatyczne wygasanie).
  - `auth:login:ip:{ip}` – licznik prób logowania per IP (okno 15min).
  - `auth:login:account:{email}` – licznik prób logowania per konto (okno 15min).
  - `auth:register:ip:{ip}` – licznik prób rejestracji per IP (okno 1h).
  - `auth:register:email:{email}` – licznik prób rejestracji per email (okno 1h).
  - `auth:register:username:{username}` – licznik prób rejestracji per username (okno 1h).
- Token JWT zawiera: `jti` (UUID), `sub` (userId jako String), `iat` (issued at), `exp` (expiration). Podpis: HMAC-SHA256.
- `JwtAuthenticationFilter` (Spring Security) sprawdza token z cookie `authToken` i blacklistę przed każdym żądaniem. W przypadku nieprawidłowego/wygasłego tokena zwraca 401, co powoduje automatyczne wylogowanie przez `AuthInterceptor` w Angular.

### 2.6 Obsługa wyjątków i błędów

| Wyjątek | HTTP | Kod domenowy | Reakcja UI |
| --- | --- | --- | --- |
| `InvalidCredentialsException` | 401 | `AUTH_INVALID_CREDENTIALS` | Pokazanie komunikatu + inkrementacja licznika |
| `AccountLockedException` | 423 | `AUTH_ACCOUNT_LOCKED` | CTA do resetu hasła, link do wsparcia |
| `RateLimitExceededException` | 429 | `AUTH_RATE_LIMIT` + `Retry-After` | Formularz blokuje przycisk i pokazuje timer |
| `DuplicateResourceException` | 409 | `AUTH_DUPLICATE_EMAIL/USERNAME` | Podświetlone pole i link do logowania |
| `PasswordTokenExpiredException` | 410 | `AUTH_RESET_TOKEN_EXPIRED` | UI proponuje ponowne żądanie tokenu |

Globalny `RestExceptionHandler` mapuje wyjątki na strukturę `{timestamp, path, code, message, details}`. Szczegóły pól trafiają do `details`.

### 2.7 Renderowanie server-side (SSR) i integracja layoutów

- Angular Universal (jeśli wdrożone) może wykorzystywać resolver do weryfikacji tokena przed renderowaniem. W obecnej implementacji frontend działa jako SPA:
  1. Przy starcie aplikacji `AuthService` sprawdza localStorage dla `wow-current-user`.
  2. Jeśli brak danych lub użytkownik zalogowany, wywołuje `loadCurrentUser()` → `GET /api/v1/auth/me`.
  3. W przypadku 401 `AuthInterceptor` automatycznie czyści stan i przekierowuje do `/auth/login`.
- `AuthGuard` chroni trasy `/app/*` wymagając `isAuthenticated() === true`.
- Wylogowanie: `POST /api/v1/auth/logout` dodaje token do blacklisty i czyści cookie po stronie serwera. Frontend czyści localStorage i przekierowuje do `/auth/login`.

### 2.8 Sekwencje end-to-end

1. **Rejestracja + auto-login**
   1. UI waliduje dane (email, username, password).
   2. `POST /api/v1/auth/register` z `withCredentials: true`.
   3. Backend sprawdza rate limit, weryfikuje unikalność email/username, hashuje hasło (BCrypt), tworzy użytkownika w PostgreSQL.
   4. `AuthService` generuje JWT przez `JwtTokenProvider`, ustawia httpOnly cookie `authToken`.
   5. UI otrzymuje `RegisterResponse`, zapisuje użytkownika w `AuthService.currentUserSignal`, przekierowuje do `/app/dashboard`.
2. **Logowanie**
   1. UI wysyła `POST /api/v1/auth/login` z `withCredentials: true`.
   2. Backend sprawdza rate limit (per IP i per account), weryfikuje hasło przez `BCryptPasswordEncoder.matches()`.
   3. `AuthService` generuje JWT, ustawia httpOnly cookie `authToken`.
   4. UI otrzymuje `LoginResponse`, wywołuje `loadCurrentUser()` aby odświeżyć pełny profil, zapisuje w `AuthService.currentUserSignal`.
3. **Wylogowanie**
   1. UI wysyła `POST /api/v1/auth/logout` z `withCredentials: true`.
   2. Backend wyodrębnia token z cookie, dodaje `jti` do blacklisty w Redis (`token:blacklist:{tokenId}`), czyści cookie.
   3. UI czyści `AuthService.currentUserSignal` i localStorage, przekierowuje do `/auth/login`.

### 2.9 Monitorowanie, testy i migracje

- **Logowanie i metryki**: Logi SLF4J w `AuthService`, `JwtTokenProvider`, `TokenBlacklistService`. Spring Actuator dla metryk (jeśli włączone). Logi audytowe zawierają IP, userId, wynik operacji.
- **Testy**: Jednostkowe (`AuthService`, `JwtTokenProvider`, `TokenBlacklistService`), integracyjne (MockMvc + PostgreSQL testcontainers), kontraktowe (OpenAPI/Swagger). Frontend: Jest/ATL dla komponentów formularzy, e2e (Cypress) dla scenariuszy rejestracji, logowania.
- **Migracje DB**: Flyway migracje w `backend/src/main/resources/db/migration/`. Tabela `users` zawiera pola: `id`, `email`, `username`, `password_hash`, `is_guest`, `avatar`, `total_points`, `games_played`, `games_won`, `created_at`, `last_seen_at`. Indeksy na `email`, `username`.
- **Konfiguracja**: Properties w `application.properties`: `app.jwt.secret` (Base64, min 256 bitów), `app.jwt.expiration` (ms, domyślnie 3600000), `app.cookie.secure` (true/false), `app.rate-limit.login-per-ip`, `app.rate-limit.register-per-ip`, `app.cors.*`.

---

Specyfikacja powyżej zachowuje spójność z istniejącym działaniem gry, rankingu oraz wymaganiami bezpieczeństwa, jednocześnie dodając kompletne flow rejestracji, logowania i odzyskiwania hasła zarówno w UI, jak i w backendzie.

