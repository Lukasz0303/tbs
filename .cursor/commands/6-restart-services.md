Zrestartuj backend i frontend używając skryptu `start.ps1` z opcją `restart`.

Komenda wykonuje następujące kroki:
1. Zatrzymuje wszystkie działające instancje frontendu (procesy Node.js, PowerShell z npm start)
2. Zatrzymuje wszystkie działające instancje backendu (procesy Java, PowerShell z run-backend.ps1)
3. Sprawdza czy porty 8080 (backend) i 4200 (frontend) zostały zwolnione
4. Czeka 3 sekundy na pełne zamknięcie procesów
5. Weryfikuje czy porty są wolne przed uruchomieniem nowych instancji
6. Uruchamia backend (Spring Boot) poprzez skrypt `backend/run-backend.ps1`
7. Uruchamia frontend (Angular) poprzez `npm start`
8. Wyświetla raport z wynikami restartu

Skrypt automatycznie:
- Znajduje i zatrzymuje wszystkie procesy powiązane z backendem (Java z bootRun, tbs, itp.)
- Znajduje i zatrzymuje wszystkie procesy powiązane z frontendem (Node.js z ng serve, Angular)
- Sprawdza czy porty zostały zwolnione przed uruchomieniem nowych procesów
- W przypadku problemów wyświetla szczegółowe komunikaty błędów i wskazówki

WYKONANIE:
Uruchom komendę PowerShell: `.\start.ps1 restart`

Lub jeśli używasz menu interaktywnego, wybierz opcję 2 "Restart".

WAŻNE:
- Restart wymaga zatrzymania wszystkich działających instancji - upewnij się że nie ma innych procesów używających portów 8080 i 4200
- Jeśli porty nie zostaną zwolnione po 3 sekundach, skrypt wyświetli błąd i zakończy działanie
- W takim przypadku możesz ręcznie zatrzymać procesy używając: `.\start.ps1 stop`
- Po zatrzymaniu wszystkich procesów, możesz ponownie uruchomić restart

Użycie:
- Gdy chcesz odświeżyć działające serwisy po zmianach w kodzie
- Gdy serwisy zachowują się niestabilnie i potrzebujesz czystego restartu
- Gdy chcesz zastosować zmiany w konfiguracji wymagające pełnego restartu

Różnica między `start` a `restart`:
- `start` - uruchamia serwisy tylko jeśli nie działają (nie zatrzymuje istniejących)
- `restart` - zawsze zatrzymuje istniejące serwisy przed uruchomieniem nowych (czysty restart)

