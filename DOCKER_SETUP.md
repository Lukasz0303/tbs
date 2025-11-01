# Instrukcja naprawy Docker Desktop

## Problem
Docker Desktop nie może wystartować, ponieważ wymaga WSL2 (Windows Subsystem for Linux 2). Możliwe sytuacje:
- WSL nie jest zainstalowany w systemie
- WSL jest zainstalowany, ale wymaga aktualizacji do najnowszej wersji
- Docker Desktop wyświetla komunikat "WSL needs updating"

## Rozwiązanie

### Krok 1: Zainstaluj/aktualizuj WSL2 (wymaga uprawnień administratora)

1. **Uruchom PowerShell jako administrator:**
   - Naciśnij `Win + X` lub kliknij prawym przyciskiem myszy na przycisk Start
   - Wybierz "Windows PowerShell (Administrator)" lub "Terminal (Administrator)"

2. **Najpierw spróbuj zaktualizować WSL (jeśli jest już zainstalowany):**

```powershell
wsl --update
```

Jeśli komenda zwraca błąd, że WSL nie jest zainstalowany, przejdź do kroku 3.

3. **Jeśli WSL nie jest zainstalowany, zainstaluj go:**

```powershell
# Zainstaluj WSL2 z domyślną dystrybucją Ubuntu
wsl --install

# Jeśli powyższe polecenie nie działa, użyj alternatywnej metody:
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart

# Po wykonaniu powyższych poleceń, zrestartuj komputer
```

4. **Po restarcie (jeśli był wymagany), ustaw WSL2 jako domyślną wersję:**

```powershell
wsl --set-default-version 2
```

5. **Sprawdź instalację:**

```powershell
wsl --status
```

Powinieneś zobaczyć informację o zainstalowanym WSL2.

### Krok 2: Skonfiguruj Docker Desktop

1. **Zrestartuj Docker Desktop:**
   - Jeśli Docker Desktop jest otwarty, zamknij go całkowicie
   - Uruchom Docker Desktop ponownie
   - Kliknij przycisk **"Restart"** w Docker Desktop (jeśli jest wyświetlany)

2. **Jeśli Docker Desktop nadal nie startuje:**
   - Zamknij Docker Desktop całkowicie
   - Uruchom Docker Desktop ponownie
   - Powinien teraz wystartować bez problemów

3. **W ustawieniach Docker Desktop:**
   - Otwórz Docker Desktop
   - Przejdź do Settings (Ustawienia) → General (Ogólne)
   - Upewnij się, że opcja "Use WSL 2 based engine" jest zaznaczona

4. **W sekcji Resources (Zasoby):**
   - Możesz dostosować ilość przydzielonej pamięci RAM (zalecane minimum: 4 GB)

### Krok 3: Weryfikacja

Sprawdź, czy Docker działa poprawnie:

```bash
docker --version
docker ps
docker info
```

### Krok 4: Uruchomienie Supabase

Po naprawie Docker Desktop, możesz uruchomić lokalną instancję Supabase:

```bash
npx supabase start
```

## Alternatywne rozwiązania (jeśli powyższe nie działa)

### Opcja 1: Zainstaluj dystrybucję Linux dla WSL2 ręcznie

```powershell
# Zobacz dostępne dystrybucje
wsl --list --online

# Zainstaluj wybraną dystrybucję (np. Ubuntu)
wsl --install -d Ubuntu

# Ustaw jako domyślną wersję 2
wsl --set-default-version 2
```

### Opcja 2: Sprawdź wirtualizację

Docker Desktop wymaga wirtualizacji w BIOS/UEFI:

1. Zrestartuj komputer
2. Wejdź do BIOS/UEFI (zwykle klawisz F2, F10, Del podczas startu)
3. Znajdź i włącz:
   - Virtualization Technology (VT-x dla Intel lub AMD-V dla AMD)
   - Hyper-V (jeśli dostępne)

### Opcja 3: Zaktualizuj Docker Desktop

Jeśli masz starą wersję Docker Desktop:

1. Pobierz najnowszą wersję z: https://www.docker.com/products/docker-desktop/
2. Zainstaluj ją (nadpisze poprzednią wersję)

## Rozwiązywanie problemów

### Problem: "WSL needs updating" w Docker Desktop

1. **Uruchom PowerShell jako administrator** i wykonaj:
   ```powershell
   wsl --update
   ```

2. **W Docker Desktop kliknij przycisk "Restart"**

3. **Jeśli to nie pomaga:**
   - Zamknij Docker Desktop całkowicie
   - Uruchom PowerShell jako administrator
   - Wykonaj: `wsl --shutdown`
   - Uruchom Docker Desktop ponownie

### Problem: "Docker Desktop is unable to start" nadal występuje

1. **Zamknij wszystkie uruchomione instancje Docker Desktop**
2. **Sprawdź, czy nie ma konfliktów portów:**
   - Upewnij się, że porty używane przez Supabase (54321-54324) są wolne
3. **Zrestartuj WSL:**
   ```powershell
   wsl --shutdown
   ```
4. **Zrestartuj komputer** po instalacji WSL2 (jeśli jeszcze nie zrobiłeś)

### Problem: WSL2 nie instaluje się

- Upewnij się, że używasz Windows 10 (wersja 2004 lub nowsza) lub Windows 11
- Sprawdź aktualizacje Windows: Settings → Update & Security → Windows Update

### Problem: "Virtualization is not enabled"

- Włącz wirtualizację w BIOS/UEFI (patrz Opcja 2 powyżej)
- Wyłącz Hyper-V, jeśli jest włączony, a używasz Docker Desktop z WSL2

## Przydatne komendy

```bash
# Sprawdź status WSL
wsl --status

# Zobacz zainstalowane dystrybucje
wsl --list --verbose

# Sprawdź wersję Docker
docker --version

# Sprawdź status kontenerów
docker ps -a

# Zatrzymaj wszystkie kontenery
docker stop $(docker ps -aq)

# Usuń wszystkie kontenery
docker rm $(docker ps -aq)
```

## Dodatkowe zasoby

- [Dokumentacja WSL2](https://docs.microsoft.com/en-us/windows/wsl/)
- [Dokumentacja Docker Desktop](https://docs.docker.com/desktop/)
- [Instrukcja Supabase](SUPABASE_SETUP.md)

