# Docker Configuration - World at War: Turn-Based Strategy

## Przegląd

Konfiguracja Docker dla aplikacji World at War: Turn-Based Strategy składa się z:
- **Frontend**: Angular 17 aplikacja serwowana przez nginx
- **Backend**: Spring Boot 3.x aplikacja Java 21
- **PostgreSQL**: Baza danych
- **Redis**: Cache i sesje

## Struktura plików

```
.
├── docker-compose.yml          # Główna konfiguracja Docker Compose
├── frontend/
│   ├── Dockerfile              # Multi-stage build dla frontendu
│   ├── nginx.conf              # Konfiguracja nginx
│   └── .dockerignore           # Pliki wykluczone z builda frontendu
└── backend/
    ├── Dockerfile              # Multi-stage build dla backendu
    └── .dockerignore           # Pliki wykluczone z builda backendu
```

## Wymagania

- Docker 20.10+
- Docker Compose 2.0+
- BuildKit włączony (domyślnie w nowszych wersjach Docker)

## Zmienne środowiskowe

### Frontend (build-time)

- `SUPABASE_URL` - URL do Supabase (opcjonalne, domyślnie puste)
- `SUPABASE_ANON_KEY` - Klucz anonimowy Supabase (opcjonalne, domyślnie pusty)
- `API_BASE_URL` - URL do API backendu (domyślnie: `http://backend:4333/api`)

### Backend (runtime)

- `SPRING_PROFILES_ACTIVE` - Profil Spring Boot (domyślnie: `prod`)
- `SPRING_DATASOURCE_URL` - URL do bazy PostgreSQL (domyślnie: `jdbc:postgresql://postgres:5432/postgres`)
- `SPRING_DATASOURCE_USERNAME` - Użytkownik bazy danych (domyślnie: `postgres`)
- `SPRING_DATASOURCE_PASSWORD` - Hasło bazy danych (domyślnie: `postgres`)
- `SPRING_DATA_REDIS_HOST` - Host Redis (domyślnie: `redis`)
- `SPRING_DATA_REDIS_PORT` - Port Redis (domyślnie: `6379`)
- `SPRING_DATA_REDIS_PASSWORD` - Hasło Redis (opcjonalne)
- `JWT_SECRET` - Sekret JWT (domyślnie: wartość fallback, **WYMAGANE w produkcji**)
- `JWT_EXPIRATION` - Czas wygaśnięcia JWT w ms (domyślnie: `3600000`)
- `APP_CORS_ALLOWED_ORIGINS` - Dozwolone źródła CORS (domyślnie: `http://localhost:4222,https://tbswars.win,https://api.tbswars.win`)
- `APP_COOKIE_SECURE` - Secure flag dla cookies (domyślnie: `false` dla dev)
- `APP_COOKIE_SAME_SITE` - SameSite dla cookies (domyślnie: `Lax`)
- `LOGGING_LEVEL_COM_TBS` - Poziom logowania (domyślnie: `INFO`)

### PostgreSQL

- `POSTGRES_DB` - Nazwa bazy danych (domyślnie: `postgres`)
- `POSTGRES_USER` - Użytkownik (domyślnie: `postgres`)
- `POSTGRES_PASSWORD` - Hasło (domyślnie: `postgres`)
- `POSTGRES_PORT` - Port mapowany na hosta (domyślnie: `5432`)

### Redis

- `REDIS_PORT` - Port mapowany na hosta (domyślnie: `6379`)

## Budowanie obrazów

### Budowanie wszystkich serwisów

```bash
docker-compose build
```

### Budowanie pojedynczego serwisu

```bash
# Frontend
docker-compose build frontend

# Backend
docker-compose build backend
```

### Budowanie z tagowaniem (dla CI/CD)

```bash
# Z użyciem commit SHA
COMMIT_SHA=$(git rev-parse --short HEAD)
docker-compose build --build-arg BUILD_VERSION=${COMMIT_SHA} backend
docker tag tbswars/backend:latest tbswars/backend:${COMMIT_SHA}
```

## Uruchamianie aplikacji

### Uruchomienie wszystkich serwisów

```bash
docker-compose up -d
```

### Uruchomienie z logami

```bash
docker-compose up
```

### Uruchomienie pojedynczego serwisu

```bash
docker-compose up -d frontend
docker-compose up -d backend
```

### Uruchomienie z własnymi zmiennymi środowiskowymi

Utwórz plik `.env` w głównym katalogu projektu:

```env
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
API_BASE_URL=http://backend:4333/api
JWT_SECRET=your-secure-jwt-secret
SPRING_DATASOURCE_PASSWORD=secure-password
POSTGRES_PASSWORD=secure-password
```

Następnie uruchom:

```bash
docker-compose up -d
```

## Porty

- **Frontend**: `4222` (http://localhost:4222)
- **Backend**: `4333` (http://localhost:4333)
- **PostgreSQL**: `5432` (domyślnie, konfigurowalne przez `POSTGRES_PORT`)
- **Redis**: `6379` (domyślnie, konfigurowalne przez `REDIS_PORT`)

## Health Checks

Wszystkie serwisy mają skonfigurowane health checks:

- **Frontend**: `GET /health` (co 30s)
- **Backend**: `GET /actuator/health` (co 30s, start period 40s)
- **PostgreSQL**: `pg_isready` (co 10s)
- **Redis**: `redis-cli ping` (co 10s)

## Zarządzanie kontenerami

### Wyświetlanie statusu

```bash
docker-compose ps
```

### Wyświetlanie logów

```bash
# Wszystkie serwisy
docker-compose logs -f

# Pojedynczy serwis
docker-compose logs -f backend
docker-compose logs -f frontend
```

### Zatrzymanie serwisów

```bash
docker-compose stop
```

### Zatrzymanie i usunięcie kontenerów

```bash
docker-compose down
```

### Zatrzymanie i usunięcie z wolumenami

```bash
docker-compose down -v
```

## Rozwiązywanie problemów

### Problem: Backend nie może połączyć się z bazą danych

**Rozwiązanie**: Sprawdź czy PostgreSQL jest uruchomiony i zdrowy:
```bash
docker-compose ps postgres
docker-compose logs postgres
```

### Problem: Frontend nie może połączyć się z backendem

**Rozwiązanie**: Sprawdź czy backend jest uruchomiony i czy `API_BASE_URL` jest poprawnie ustawiony:
```bash
docker-compose ps backend
docker-compose logs backend
curl http://localhost:4333/actuator/health
```

### Problem: Błędy builda frontendu

**Rozwiązanie**: Sprawdź czy wszystkie zmienne środowiskowe są ustawione:
```bash
docker-compose build --no-cache frontend
```

### Problem: Błędy builda backendu

**Rozwiązanie**: Sprawdź czy Gradle wrapper jest dostępny:
```bash
docker-compose build --no-cache backend
```

### Problem: Porty są już zajęte

**Rozwiązanie**: Zmień porty w `docker-compose.yml` lub zatrzymaj procesy używające tych portów.

## Produkcja

### Bezpieczeństwo

1. **Zawsze ustaw `JWT_SECRET`** przez zmienną środowiskową w produkcji
2. **Użyj silnych haseł** dla PostgreSQL i Redis
3. **Włącz `APP_COOKIE_SECURE=true`** dla HTTPS
4. **Skonfiguruj `APP_CORS_ALLOWED_ORIGINS`** z właściwymi domenami produkcyjnymi
5. **Użyj read-only filesystem** gdzie to możliwe (można dodać do Dockerfile)

### Optymalizacja

1. **Użyj multi-stage builds** (już zaimplementowane)
2. **Cache'uj warstwy** - zależności są kopiowane przed kodem aplikacji
3. **Użyj .dockerignore** - wyklucza niepotrzebne pliki z builda
4. **Monitoruj health checks** - użyj Prometheus/Grafana do monitorowania

### Deployment na DigitalOcean

1. Zbuduj obrazy lokalnie lub w CI/CD
2. Wypchnij do Docker Registry (np. DigitalOcean Container Registry)
3. Użyj `docker-compose.yml` na serwerze lub skonfiguruj Kubernetes
4. Ustaw zmienne środowiskowe przez DigitalOcean App Platform lub Kubernetes Secrets

## Rozwój lokalny

Dla rozwoju lokalnego możesz użyć:

```bash
# Tylko baza danych i Redis
docker-compose up -d postgres redis

# Frontend i backend uruchom lokalnie przez npm/gradle
cd frontend && npm start
cd backend && ./gradlew bootRun
```

## CI/CD Integration

Przykładowa konfiguracja GitHub Actions:

```yaml
- name: Build Docker images
  run: |
    docker-compose build --build-arg BUILD_VERSION=${{ github.sha }}
    
- name: Run tests
  run: |
    docker-compose up -d
    # Czekaj na health checks
    docker-compose ps
    # Uruchom testy
```

## Dodatkowe informacje

- Wszystkie obrazy używają non-root users dla bezpieczeństwa
- Health checks są skonfigurowane dla wszystkich serwisów
- Volumeny są używane dla trwałości danych PostgreSQL i Redis
- Sieć wewnętrzna `tbswars-network` izoluje komunikację między kontenerami

