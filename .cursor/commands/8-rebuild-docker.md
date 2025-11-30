Przebuduj obrazy Docker dla frontendu i backendu używając `docker-compose build`.

Komenda wykonuje następujące kroki:
1. Zatrzymuje działające kontenery (opcjonalnie, jeśli są uruchomione)
2. Przebudowuje obrazy Docker dla wszystkich serwisów zdefiniowanych w `docker-compose.yml`
3. Używa cache Docker tylko dla warstw, które nie uległy zmianie
4. Wyświetla postęp budowania każdego serwisu

### Pliki Docker w projekcie:
- `docker-compose.yml` - główny plik konfiguracyjny Docker Compose (katalog główny projektu)
- `frontend/Dockerfile` - Dockerfile dla aplikacji Angular (frontend)
- `backend/Dockerfile` - Dockerfile dla aplikacji Spring Boot (backend)

### Struktura buildów:
- **Frontend**: Multi-stage build (Node.js builder + Nginx runtime)
  - Build stage: kompiluje aplikację Angular
  - Runtime stage: serwuje statyczne pliki przez Nginx
- **Backend**: Multi-stage build (Gradle builder + JRE runtime)
  - Build stage: kompiluje aplikację Spring Boot używając Gradle
  - Runtime stage: uruchamia JAR w środowisku JRE

WYKONANIE:
Uruchom komendę w katalogu głównym projektu:
```bash
docker-compose build
```

Aby przebudować bez użycia cache (czysty build):
```bash
docker-compose build --no-cache
```

Aby przebudować tylko konkretny serwis:
```bash
docker-compose build frontend
docker-compose build backend
```

Aby przebudować i od razu uruchomić kontenery:
```bash
docker-compose up --build
```

Aby przebudować, zatrzymać stare kontenery i uruchomić nowe:
```bash
docker-compose down
docker-compose build
docker-compose up -d
```

### Opcje build:
- `--no-cache` - przebudowuje wszystkie warstwy od zera, ignorując cache
- `--pull` - pobiera najnowsze wersje obrazów bazowych przed buildem
- `--parallel` - buduje wiele serwisów równolegle (domyślnie włączone)
- `--progress=plain` - wyświetla szczegółowe logi builda

WAŻNE:
- Build może zająć kilka minut, szczególnie przy pierwszym uruchomieniu
- Upewnij się że masz wystarczająco miejsca na dysku (obrazy mogą zajmować kilka GB)
- Jeśli zmieniasz zmienne środowiskowe w `docker-compose.yml`, może być potrzebny rebuild
- Po zmianach w kodzie źródłowym (frontend/backend) zawsze wykonaj rebuild przed uruchomieniem kontenerów
- Obrazy są budowane lokalnie i oznaczane jako `tbswars/frontend:latest` i `tbswars/backend:latest`

### Zmienne środowiskowe dla builda:
Frontend wymaga następujących argumentów build (można je ustawić w `.env` lub `docker-compose.yml`):
- `SUPABASE_URL` - URL do Supabase
- `SUPABASE_ANON_KEY` - klucz anonimowy Supabase
- `API_BASE_URL` - URL do API backendu (domyślnie: `http://backend:4333/api`)

Backend wymaga:
- `BUILD_VERSION` - wersja builda (domyślnie: `latest`)

Użycie:
- Gdy wprowadzasz zmiany w kodzie źródłowym i chcesz zaktualizować obrazy Docker
- Gdy modyfikujesz pliki Dockerfile i chcesz zastosować zmiany
- Gdy chcesz odświeżyć obrazy przed wdrożeniem na produkcję
- Gdy masz problemy z działaniem kontenerów i chcesz przebudować od zera

