<authentication_analysis>
1. Przepływy autentykacji z PRD i specyfikacji:
   - Logowanie istniejącego użytkownika (AuthLoginComponent, POST /api/v1/auth/login,
     rate limit 5/15min per IP i per account, JWT w httpOnly cookie, zapis w AuthService,
     redirect do /app).
   - Rejestracja + auto-login (AuthRegisterComponent, POST /api/v1/auth/register,
     automatyczne cookie i przejście do dashboard).
   - Gość przechodzi do rejestracji (GuestStartButton → RegisterDialog →
     konsolidacja stanu gry z kontem - przyszła funkcjonalność).
   - Weryfikacja tokena (AuthGuard, GET /api/v1/auth/me, automatyczne wylogowanie przy 401).
   - Wylogowanie globalne (POST /api/v1/auth/logout, blacklist w Redis, czyszczenie AuthService).
2. Główni aktorzy i interakcje:
   - Przeglądarka/Angular: renderuje komponenty (AuthLoginComponent, AuthRegisterComponent),
     wysyła formularze, przechowuje stan w AuthService (signals).
   - Angular Router + Guards: sprawdza autentykację przed trasami, przekierowuje do /auth/login.
   - Spring Boot API (AuthController): obsługuje /api/v1/auth/*, używa Spring Security JWT.
   - JwtTokenProvider + TokenBlacklistService: generuje i weryfikuje JWT, zarządza blacklistą w Redis.
   - Redis: przechowuje blacklistę tokenów (token:blacklist:{tokenId}) i rate limitery.
   - PostgreSQL (Supabase): przechowuje użytkowników (tabela users), hasła hashowane BCrypt.
3. Procesy weryfikacji i odświeżania tokenów:
   - Token JWT (domyślnie 3600s/1h) przechowywany w httpOnly cookie `authToken`.
   - JwtAuthenticationFilter (Spring Security) weryfikuje token z cookie i blacklistę przed każdym żądaniem.
   - W przypadku nieprawidłowego/wygasłego tokena zwraca 401, co powoduje automatyczne wylogowanie przez AuthInterceptor.
   - AuthInterceptor przechwytuje 401/403, wywołuje logout() i czyści stan użytkownika.
4. Kroki autentykacji (skrót):
   a) Wejście na stronę auth → AuthGuard sprawdza isAuthenticated(), przekierowuje jeśli zalogowany.
   b) Formularz logowania/rejestracji waliduje dane i wysyła POST do /api/v1/auth/login|register z withCredentials: true.
   c) Backend sprawdza rate limit, weryfikuje dane, generuje JWT przez JwtTokenProvider, ustawia httpOnly cookie.
   d) AuthService wypełnia currentUserSignal, Router przenosi do /app, AuthGuard pilnuje stanu przy kolejnych trasach.
   e) Przy każdym żądaniu JwtAuthenticationFilter weryfikuje token z cookie i blacklistę; przy 401 AuthInterceptor wylogowuje.
   f) Wylogowanie dodaje token na blacklistę w Redis i usuwa cookie, frontend czyści AuthService i localStorage.
</authentication_analysis>

<mermaid_diagram>
```mermaid
sequenceDiagram
  autonumber
  participant Browser as Przeglądarka (Angular)
  participant Router as Angular Router + Guards
  participant Api as Spring Boot API
  participant JWT as JwtTokenProvider
  participant Redis as Redis (Blacklist + Rate Limit)
  participant DB as PostgreSQL (Supabase)

  Browser->>Router: Wejście na /auth/login
  activate Router
  Router->>Browser: Sprawdź AuthService.isAuthenticated()
  alt Użytkownik zalogowany
    Router->>Browser: Redirect do /app/dashboard
  else Użytkownik niezalogowany
    Router->>Browser: Renderuj AuthLoginComponent
  end
  deactivate Router

  Browser->>Api: POST /api/v1/auth/register (withCredentials: true)
  activate Api
  Api->>Redis: Sprawdź rate limit (ip + email + username)
  alt Rate limit OK
    Api->>DB: Sprawdź unikalność email/username
    Api->>DB: Utwórz użytkownika (BCrypt hash hasła)
    Api->>JWT: Generuj token (jti, sub=userId, exp)
    JWT-->>Api: Token JWT
    Api->>Redis: Zapis rate limit (inkrementacja)
    Api-->>Browser: RegisterResponse + Set-Cookie: authToken (httpOnly)
    Browser->>Browser: AuthService.currentUserSignal.set(user)
    Browser->>Router: Navigate do /app/dashboard
  else Rate limit przekroczony
    Api-->>Browser: 429 Rate Limit Exceeded
  end
  deactivate Api

  Browser->>Api: POST /api/v1/auth/login (email, password, withCredentials: true)
  activate Api
  Api->>Redis: Sprawdź rate limit (ip + account)
  alt Rate limit OK
    Api->>DB: Znajdź użytkownika po email
    Api->>Api: Weryfikuj hasło (BCryptPasswordEncoder.matches)
    alt Hasło poprawne
      Api->>JWT: Generuj token (jti, sub=userId, exp)
      JWT-->>Api: Token JWT
      Api->>Redis: Zapis rate limit (inkrementacja)
      Api-->>Browser: LoginResponse + Set-Cookie: authToken (httpOnly)
      Browser->>Api: GET /api/v1/auth/me (withCredentials: true)
      Api->>JWT: Weryfikuj token z cookie
      JWT->>Redis: Sprawdź blacklistę (token:blacklist:{jti})
      alt Token ważny i nie na blackliście
        Api->>DB: Pobierz profil użytkownika
        Api-->>Browser: UserProfileResponse
        Browser->>Browser: AuthService.currentUserSignal.set(user)
        Browser->>Router: Navigate do /app/dashboard
      else Token nieważny
        Api-->>Browser: 401 Unauthorized
        Browser->>Browser: AuthInterceptor → logout(), redirect do /auth/login
      end
    else Hasło niepoprawne
      Api->>Redis: Zapis rate limit (inkrementacja)
      Api-->>Browser: 401 Invalid credentials
    end
  else Rate limit przekroczony
    Api-->>Browser: 429 Rate Limit Exceeded
  end
  deactivate Api

  Browser->>Router: Nawigacja do /app/*
  activate Router
  Router->>Browser: AuthGuard sprawdza isAuthenticated()
  alt Użytkownik zalogowany
    Router->>Browser: Zezwól na dostęp
  else Użytkownik niezalogowany
    Router->>Browser: Redirect do /auth/login?redirectTo=/app/...
  end
  deactivate Router

  Browser->>Api: Żądanie gry lub ranking (withCredentials: true)
  activate Api
  Api->>JWT: Weryfikuj token z cookie (JwtAuthenticationFilter)
  JWT->>Redis: Sprawdź blacklistę (token:blacklist:{jti})
  alt Token aktywny i nie na blackliście
    JWT-->>Api: Claims (userId z sub)
    Api->>DB: Pobierz dane gry/rankingu
    Api-->>Browser: Dane gry i ranking
  else Token odrzucony/wygasły
    Api-->>Browser: 401 Unauthorized
    Browser->>Browser: AuthInterceptor → logout(), redirect do /auth/login
  end
  deactivate Api

  Browser->>Api: POST /api/v1/auth/logout (withCredentials: true)
  activate Api
  Api->>JWT: Wyodrębnij jti z tokena z cookie
  Api->>Redis: Dodaj do blacklisty (token:blacklist:{jti}, TTL=pozostały czas)
  Api-->>Browser: LogoutResponse + Set-Cookie: authToken="" (Max-Age=0)
  Browser->>Browser: AuthService.currentUserSignal.set(null), localStorage.clear()
  Browser->>Router: Navigate do /auth/login
  deactivate Api
```
</mermaid_diagram>

