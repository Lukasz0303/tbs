# World at War: Turn-Based Strategy - Backend

Spring Boot 3.x backend aplikacji World at War: Turn-Based Strategy.

## 🚀 Technologie

- **Java 21** - język programowania
- **Spring Boot 3.3.5** - framework aplikacyjny
- **Spring Security** - autoryzacja i autentykacja (JWT z blacklistą w Redis)
- **BCrypt** - hashowanie haseł użytkowników
- **PostgreSQL 17** - baza danych (Supabase)
- **Redis 7.x** - cache, sesje i blacklista tokenów JWT
- **JWT (JJWT 0.12.3)** - autoryzacja tokenowa
- **Swagger/OpenAPI** - dokumentacja API

## 📋 Wymagania

- Java 21 (JDK) - [Download](https://adoptium.net/)
- PostgreSQL 17 (lokalnie przez Supabase)
- Redis 7.x
- Supabase CLI (lokalnie)
- Docker Desktop (dla Supabase i Redis)

### Instalacja Java 21

**Windows:**
```powershell
# Pobierz i zainstaluj z https://adoptium.net/temurin/releases/
# Po instalacji ustaw zmienną środowiskową JAVA_HOME

# PowerShell (jako Administrator):
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Eclipse Adoptium\jdk-21.x.x.x-hotspot', 'Machine')
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-21-jdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

**macOS:**
```bash
# Homebrew
brew install openjdk@21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

## 🔧 Instalacja projektu lokalnie

### Szybkie uruchomienie (zalecane)

Użyj skryptu `run-backend.ps1` z głównego katalogu projektu:

```powershell
# Start - uruchomienie BE, bazy danych i Redis
.\run-backend.ps1 start

# Restart - restart z przebudową i migracjami
.\run-backend.ps1 restart

# Status - sprawdzenie statusu wszystkich serwisów
.\run-backend.ps1 status

# Logs - wyświetlenie ostatnich logów
.\run-backend.ps1 logs

# Stop - zatrzymanie backendu
.\run-backend.ps1 stop
```

### Instalacja ręczna

#### 1. Klonowanie repozytorium

```bash
git clone <repository-url>
cd tbs/backend
```

#### 2. Instalacja zależności

Projekt używa Gradle Wrapper, nie wymaga instalacji Gradle:

```bash
# Windows
gradlew.bat build

# Linux/Mac
./gradlew build
```

#### 3. Uruchomienie lokalnej bazy danych

```bash
# Z głównego katalogu projektu (tbs/)
cd ..
npx supabase start
```

To uruchomi:
- PostgreSQL (port 54322)
- Redis (port 6379)
- Supabase API (port 54321)
- Supabase Studio (port 54323)

#### 4. Zastosowanie migracji bazy danych

```bash
# Z głównego katalogu projektu (tbs/)
npx supabase db reset
```

## ⚙️ Konfiguracja zmiennych środowiskowych

### Konfiguracja lokalna

Domyślna konfiguracja w `application.properties` jest ustawiona dla lokalnego rozwoju.

Jeśli potrzebujesz nadpisać ustawienia (np. dla różnych środowisk), utwórz plik `application-local.properties` i skopiuj zawartość:

```properties
# Database Configuration (Supabase PostgreSQL)
spring.datasource.url=jdbc:postgresql://127.0.0.1:54322/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Flyway Configuration (disabled - using Supabase migrations)
spring.flyway.enabled=false

# Redis Configuration (for cache/sessions)
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0

# JWT Configuration
app.jwt.secret=V2FyOiBUaGlzIGlzIGEgdG9wIHNlY3JldCBmb3IgSldUIGVuY29kaW5nLiBJbiBwcm9kdWN0aW9uIHVzZSBhIHN0cm9uZyByYW5kb20gc2VjcmV0IQ==
app.jwt.expiration=3600000
```

### Generowanie sekretu JWT (dla produkcji)

Dla produkcji wygeneruj silny sekret:

```bash
# Linux/Mac
echo -n "YourSecretKeyForJWTEncryption" | base64

# Windows PowerShell
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("YourSecretKeyForJWTEncryption"))
```

## 🎯 Uruchamianie projektu lokalnie

### Użycie skryptu run-backend.ps1 (zalecane)

#### Opcje dostępne w skrypcie:

**`.\run-backend.ps1 start`** - Uruchomienie pełnego stacku:
- Sprawdza i uruchamia Supabase (PostgreSQL + Redis)
- Zbuduje backend
- Uruchomi aplikację Spring Boot
- Wyświetli linki do dokumentacji API

**`.\run-backend.ps1 restart`** - Restart z pełnym przebudowaniem:
- Zatrzymuje istniejący backend
- Stosuje nowe migracje bazy danych
- Przebudowuje aplikację
- Uruchamia ponownie wszystkie serwisy

**`.\run-backend.ps1 status`** - Sprawdzenie statusu wszystkich serwisów:
- Supabase (PostgreSQL) - port 54322
- Redis - port 6379
- Backend (Spring Boot) - port 8080
- Java 21 - wersja i lokalizacja

**`.\run-backend.ps1 logs`** - Wyświetlenie ostatnich logów:
- Ostatnie 50 linii z `backend/application.log`
- Wyszukuje wszystkie pliki `.log` w katalogu backend, jeśli główny plik nie istnieje

**`.\run-backend.ps1 stop`** - Zatrzymanie backendu:
- Zatrzymuje wszystkie procesy Java związane z backendem

```powershell
# Z głównego katalogu projektu
.\run-backend.ps1 start
```

Skrypt automatycznie:
- Sprawdzi i uruchomi Supabase (PostgreSQL + Redis)
- Zbuduje backend
- Uruchomi aplikację Spring Boot
- Wyświetli linki do dokumentacji API

### Uruchomienie serwera deweloperskiego (ręcznie)

```bash
# Windows
cd backend
gradlew.bat bootRun

# Linux/Mac
cd backend
./gradlew bootRun
```

Aplikacja uruchomi się na: **http://localhost:8080**

### Bezpośrednie uruchomienie JAR (po build)

```bash
# Windows
cd backend
gradlew.bat build
java -jar build/libs/tbs-0.0.1-SNAPSHOT.jar

# Linux/Mac
cd backend
./gradlew build
java -jar build/libs/tbs-0.0.1-SNAPSHOT.jar
```

## 📚 Dostępne endpointy API

Po uruchomieniu aplikacji, dokumentacja Swagger dostępna jest pod:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Endpointy autoryzacyjne

```
POST /api/auth/register - Rejestracja nowego użytkownika (email, username, password)
POST /api/auth/login    - Logowanie użytkownika (email, password) → zwraca JWT token
GET  /api/auth/me       - Pobranie profilu bieżącego użytkownika (wymaga JWT w headerze)
POST /api/auth/logout   - Wylogowanie użytkownika i dodanie tokenu do blacklisty (wymaga JWT)
```

**Autoryzacja JWT:**
- Token należy przesyłać w headerze: `Authorization: Bearer <token>`
- Token ma domyślnie 1 godzinę ważności (3600000 ms)
- Po wylogowaniu token jest dodawany do blacklisty w Redis
- Token jest stateless i nie wymaga sesji serwera

### Health Check

```
GET /actuator/health - Status aplikacji
```

## 🧪 Testowanie

```bash
# Uruchom wszystkie testy
gradlew.bat test

# Uruchom testy z raportem
gradlew.bat test --tests "*" --info
```

## 🏗️ Build

```bash
# Windows
gradlew.bat build

# Linux/Mac
./gradlew build
```

Produkt zostanie wygenerowany w: `build/libs/tbs-0.0.1-SNAPSHOT.jar`

## 📁 Struktura projektu

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/tbs/
│   │   │   ├── config/          # Konfiguracja (Security, OpenAPI)
│   │   │   ├── controller/      # Kontrolery REST API
│   │   │   ├── service/         # Logika biznesowa
│   │   │   ├── repository/      # Repozytoria danych
│   │   │   ├── model/           # Encje JPA
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── exception/       # Wyjątki i ich obsługa
│   │   │   ├── security/        # Bezpieczeństwo (JWT)
│   │   │   └── TbsApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── build.gradle                 # Zależności Gradle
├── settings.gradle              # Konfiguracja projektu
├── gradlew                      # Gradle Wrapper (Unix)
├── gradlew.bat                  # Gradle Wrapper (Windows)
└── README.md                    # Ten plik
```

## 🔍 Rozwiązywanie problemów

### Port 8080 jest zajęty

Zmień port w `application.properties`:

```properties
server.port=8081
```

### Błąd połączenia z bazą danych

Sprawdź czy Supabase działa:

```bash
npx supabase status
```

Jeśli nie działa, uruchom:

```bash
npx supabase start
```

### Błąd połączenia z Redis

Uruchom Redis lokalnie (Docker):

```bash
docker run --name waw-redis -p 6379:6379 -d redis:7
```

### Problem z JWT secret

Upewnij się, że `app.jwt.secret` w `application.properties` jest ustawione i jest poprawnym base64.

## 📖 Dodatkowe dokumenty

- [Supabase Migrations](../../supabase/migrations/README.md) - dokumentacja migracji bazy danych
- [API Plans](../../.ai/implementation-plans/) - plany implementacji endpointów
- [Tech Stack](../../.ai/tech-stack.md) - szczegółowy opis technologii

## 🤝 Przyczynianie się

1. Utwórz fork projektu
2. Stwórz branch dla nowej funkcjonalności (`git checkout -b feature/amazing-feature`)
3. Commit zmian (`git commit -m 'Add amazing feature'`)
4. Push do brancha (`git push origin feature/amazing-feature`)
5. Otwórz Pull Request

## 📝 Licencja

MIT © 2025 Łukasz Zieliński

