Uruchom szybką weryfikację czy backend i frontend działają poprawnie używając skryptu `start.ps1` z opcją `verify`.

Komenda wykonuje następujące kroki:
1. Uruchamia backend (Spring Boot) poprzez skrypt `backend/run-backend.ps1`
2. Uruchamia frontend (Angular) poprzez `npm start`
3. Weryfikuje czy oba serwisy działają poprawnie:
   - Sprawdza czy porty 8080 (backend) i 4200 (frontend) są otwarte
   - Weryfikuje health check backendu (endpoint `/actuator/health` lub `/api/v1/health`)
   - Testuje czy backend API odpowiada na requesty HTTP (`/v3/api-docs`)
   - Testuje czy frontend odpowiada na requesty HTTP
   - Sprawdza czy procesy Java (backend) i Node.js (frontend) działają poprawnie

Jeśli weryfikacja zakończy się sukcesem, wyświetli szczegółowy raport z:
- Potwierdzeniem działania obu serwisów
- Linkami do aplikacji (Frontend, Backend API, Swagger UI)

Jeśli wystąpią problemy, skrypt wyświetli:
- Szczegółowe komunikaty o błędach
- Ostatnie logi z backendu i frontendu (szukając ERROR, WARN, Exception)
- Wskazówki jak rozwiązać problemy

WYKONANIE:
Uruchom komendę PowerShell: `.\start.ps1 verify`

Lub jeśli używasz menu interaktywnego, wybierz opcję 5 "Verify".

Skrypt automatycznie:
- Zatrzyma istniejące procesy blokujące porty (jeśli są)
- Uruchomi backend z pełną weryfikacją (Supabase, Redis, build, start)
- Uruchomi frontend z monitorowaniem postępu
- Przeprowadzi szczegółową weryfikację działania obu serwisów
- Wyświetli czytelny raport z wynikami

WAŻNE:
- Upewnij się że masz zainstalowane wszystkie wymagane zależności (Java 21, Node.js, npm, Docker dla Supabase/Redis)
- Jeśli serwisy już działają, skrypt automatycznie zatrzyma stare procesy przed uruchomieniem nowych
- W przypadku błędów, sprawdź logi które są automatycznie wyświetlane przez skrypt

