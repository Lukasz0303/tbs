param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("start", "restart", "stop", "status")]
    [string]$Action = ""
)

$ErrorActionPreference = "Stop"

$ScriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Join-Path $ScriptPath "backend"
$FrontendDir = Join-Path $ScriptPath "frontend"
$BackendScript = Join-Path $BackendDir "run-backend.ps1"
$FrontendPidFile = Join-Path $ScriptPath ".frontend-pid"
$BackendPidFile = Join-Path $ScriptPath ".backend-pid"

function Write-ColorOutput {
    param(
        [Parameter(Mandatory=$true)]
        [ConsoleColor]$ForegroundColor,
        [Parameter(ValueFromRemainingArguments=$true)]
        [object[]]$Message
    )
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($Message) {
        Write-Output ($Message -join ' ')
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

function Test-Port {
    param([int]$Port)
    
    try {
        $connection = Test-NetConnection -ComputerName localhost -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue
        return $connection
    } catch {
        return $false
    }
}

function Test-BackendHealth {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method Get -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            return $true
        }
        return $false
    } catch {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/health" -Method Get -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                return $true
            }
        } catch {
        }
        return $false
    }
}

function Start-Backend {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Uruchamianie backendu..."
    
    if (-not (Test-Path $BackendScript)) {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Skrypt backendu nie istnieje: $BackendScript"
        return $false
    }
    
    if (Test-Port -Port 8080) {
        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Port 8080 jest juz zajety. Czekam na zwolnienie portu (max 10 sekund)..."
        $portWaitTimeout = 10
        $portWaitElapsed = 0
        $portFreed = $false
        $lastPortMessage = 0
        
        while ($portWaitElapsed -lt $portWaitTimeout) {
            if (-not (Test-Port -Port 8080)) {
                $portFreed = $true
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 8080 zwolniony"
                Start-Sleep -Seconds 1
                break
            }
            
            if (($portWaitElapsed - $lastPortMessage) -ge 2) {
                $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Port 8080 nadal zajety, czekam... ($portWaitElapsed/$portWaitTimeout sekund)"
                $lastPortMessage = $portWaitElapsed
            }
            
            Start-Sleep -Seconds 1
            $portWaitElapsed += 1
        }
        
        if (-not $portFreed) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 8080 nadal jest zajety po 10 sekundach"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz procesy Java: Get-Process -Name java"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Lub zatrzymaj recznie: .\start.ps1 stop"
            return $false
        }
    }
    
    try {
        Push-Location $BackendDir
        $backendProcess = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "run-backend.ps1", "start" -PassThru -WindowStyle Hidden
        Pop-Location
        
        if ($backendProcess) {
            $processId = $backendProcess.Id
            $processId | Out-File -FilePath $BackendPidFile -Force
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces backendu uruchomiony (PID: $processId)"
            
            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Oczekiwanie na uruchomienie backendu (max 15 sekund)..."
            $timeout = 15
            $elapsed = 0
            $portOpen = $false
            $lastStatusMessage = 0
            
            while ($elapsed -lt $timeout) {
                try {
                    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
                    if (-not $process) {
                        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces backendu zakonczyl sie przedwczesnie"
                        $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Mozliwe przyczyny:"
                        $null = Write-ColorOutput -ForegroundColor Yellow "  - Blad kompilacji Java/Gradle"
                        $null = Write-ColorOutput -ForegroundColor Yellow "  - Brak wymaganych zaleznosci"
                        $null = Write-ColorOutput -ForegroundColor Yellow "  - Problem z konfiguracja (application.properties)"
                        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz logi: Get-Content backend\application.log -Tail 50"
                        break
                    } else {
                        if ($elapsed -gt 0 -and ($elapsed - $lastStatusMessage) -ge 3) {
                            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Proces backendu dziala (PID: $processId), czekam na otwarcie portu 8080... ($elapsed/$timeout sekund)"
                            $lastStatusMessage = $elapsed
                        }
                    }
                } catch {
                    $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie mozna sprawdzic statusu procesu backendu"
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy proces istnieje: Get-Process -Id $processId"
                    break
                }
                
                if (Test-Port -Port 8080) {
                    if (-not $portOpen) {
                        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 8080 otwarty, sprawdzam health check..."
                        $portOpen = $true
                    }
                    
                    Start-Sleep -Seconds 2
                    
                    if (Test-BackendHealth) {
                        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Backend uruchomiony i odpowiada poprawnie"
                        
                        try {
                            $swaggerCheck = Invoke-WebRequest -Uri "http://localhost:8080/swagger-ui/index.html" -Method Get -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
                            if ($swaggerCheck.StatusCode -eq 200) {
                                $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Swagger UI dostepny: http://localhost:8080/swagger-ui/index.html"
                            }
                        } catch {
                        }
                        
                        return $true
                    } else {
                        if (($elapsed - $lastStatusMessage) -ge 3) {
                            $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Port otwarty, ale health check nie odpowiada, czekam dalej... ($elapsed/$timeout sekund)"
                            $lastStatusMessage = $elapsed
                        }
                    }
                } else {
                    if ($elapsed -gt 0 -and ($elapsed - $lastStatusMessage) -ge 3) {
                        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Czekam na otwarcie portu 8080... ($elapsed/$timeout sekund)"
                        $lastStatusMessage = $elapsed
                    }
                }
                
                Start-Sleep -Seconds 1
                $elapsed += 1
            }
            Write-Host ""
            
            if ($portOpen) {
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 8080 otwarty, ale backend nie odpowiada na health check"
            } else {
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Backend nie uruchomil sie w czasie 15 sekund"
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Mozliwe przyczyny:"
                $null = Write-ColorOutput -ForegroundColor Yellow "  - Backend ma blad kompilacji lub uruchomienia"
                $null = Write-ColorOutput -ForegroundColor Yellow "  - Port 8080 jest zajety przez inny proces"
                $null = Write-ColorOutput -ForegroundColor Yellow "  - Brak wymaganych zaleznosci (Java, Gradle)"
                $null = Write-ColorOutput -ForegroundColor Yellow "  - Problem z konfiguracja bazy danych lub Redis"
            }
            
            $logFile = Join-Path $BackendDir "application.log"
            if (Test-Path $logFile) {
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Ostatnie 30 linii z logow backendu (szukaj ERROR, WARN, Exception):"
                $logContent = Get-Content $logFile -Tail 30 -ErrorAction SilentlyContinue
                if ($logContent) {
                    $errorLines = $logContent | Where-Object { $_ -match "ERROR|WARN|Exception|Failed|Error" }
                    if ($errorLines) {
                        $errorLines | ForEach-Object { Write-ColorOutput -ForegroundColor Red "  $_" }
                    } else {
                        $logContent | ForEach-Object { Write-Host "  $_" }
                    }
                }
                $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Pelne logi: $logFile"
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Plik logow nie istnieje: $logFile"
                $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy proces backendu dziala: Get-Process -Name java"
            }
            
            return $false
        } else {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic procesu backendu"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy skrypt run-backend.ps1 istnieje i jest wykonywalny"
            return $false
        }
    } catch {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Blad podczas uruchamiania backendu: $_"
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Szczegoly bledu: $($_.Exception.Message)"
        if ($_.Exception.InnerException) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Wewnetrzny blad: $($_.Exception.InnerException.Message)"
        }
        return $false
    }
}

function Start-Frontend {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Uruchamianie frontendu..."
    
    if (-not (Test-Path $FrontendDir)) {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Katalog frontendu nie istnieje: $FrontendDir"
        return $false
    }
    
    if (-not (Test-Path (Join-Path $FrontendDir "package.json"))) {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] package.json nie istnieje w katalogu frontendu"
        return $false
    }
    
    try {
        $logFile = Join-Path $FrontendDir "start.log"
        Push-Location $FrontendDir
        
        $frontendProcess = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", "npm start *> start.log" -PassThru -WindowStyle Hidden
        
        Pop-Location
        
        if ($frontendProcess) {
            $frontendProcess.Id | Out-File -FilePath $FrontendPidFile -Force
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces frontendu uruchomiony (PID: $($frontendProcess.Id))"
            
            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Oczekiwanie na uruchomienie frontendu (max 15 sekund)..."
            $timeout = 15
            $elapsed = 0
            $lastStatusMessage = 0
            
            while ($elapsed -lt $timeout) {
                try {
                    $process = Get-Process -Id $frontendProcess.Id -ErrorAction SilentlyContinue
                    if (-not $process) {
                        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces frontendu zakonczyl sie przedwczesnie"
                        $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Mozliwe przyczyny:"
                        $null = Write-ColorOutput -ForegroundColor Yellow "  - Blad kompilacji TypeScript"
                        $null = Write-ColorOutput -ForegroundColor Yellow "  - Brak zainstalowanych pakietow npm"
                        $null = Write-ColorOutput -ForegroundColor Yellow "  - Problem z konfiguracja Angular"
                        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz logi: Get-Content frontend\start.log -Tail 50"
                        break
                    } else {
                        if ($elapsed -gt 0 -and ($elapsed - $lastStatusMessage) -ge 3) {
                            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Proces frontendu dziala (PID: $($frontendProcess.Id)), czekam na otwarcie portu 4200... ($elapsed/$timeout sekund)"
                            $lastStatusMessage = $elapsed
                        }
                    }
                } catch {
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Nie mozna sprawdzic statusu procesu frontendu"
                }
                
                if (Test-Port -Port 4200) {
                    $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Frontend uruchomiony na porcie 4200"
                    return $true
                } else {
                    if ($elapsed -gt 0 -and ($elapsed - $lastStatusMessage) -ge 3) {
                        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Czekam na otwarcie portu 4200... ($elapsed/$timeout sekund)"
                        $lastStatusMessage = $elapsed
                    }
                }
                
                Start-Sleep -Seconds 1
                $elapsed += 1
            }
            Write-Host ""
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Frontend nie uruchomil sie w czasie 15 sekund"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Mozliwe przyczyny:"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Frontend ma blad kompilacji TypeScript/Angular"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Port 4200 jest zajety przez inny proces"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Brak wymaganych zaleznosci (Node.js, npm)"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Problem z instalacja pakietow npm (uruchom: npm install)"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Blad w konfiguracji Angular (sprawdz angular.json)"
            
            if (Test-Path $logFile) {
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Ostatnie 30 linii z logow frontendu (szukaj ERROR, WARN, failed):"
                $logContent = Get-Content $logFile -Tail 30 -ErrorAction SilentlyContinue
                if ($logContent) {
                    $errorLines = $logContent | Where-Object { $_ -match "ERROR|WARN|failed|error|Error" }
                    if ($errorLines) {
                        $errorLines | ForEach-Object { Write-ColorOutput -ForegroundColor Red "  $_" }
                    } else {
                        $logContent | ForEach-Object { Write-Host "  $_" }
                    }
                }
                $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Pelne logi: $logFile"
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Plik logow nie istnieje: $logFile"
                $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy proces Node.js dziala: Get-Process -Name node"
            }
            
            return $false
        } else {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic procesu frontendu"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy npm jest zainstalowane: npm --version"
            return $false
        }
    } catch {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Blad podczas uruchamiania frontendu: $_"
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Szczegoly bledu: $($_.Exception.Message)"
        if ($_.Exception.InnerException) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Wewnetrzny blad: $($_.Exception.InnerException.Message)"
        }
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Upewnij sie ze Node.js i npm sa zainstalowane"
        return $false
    }
}

function Stop-Backend {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Zatrzymywanie backendu..."
    
    try {
        if (Test-Path $BackendPidFile) {
            $processId = Get-Content $BackendPidFile -ErrorAction SilentlyContinue
            if ($processId) {
                try {
                    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
                    if ($process) {
                        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
                        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces backendu zatrzymany (PID: $processId)"
                    }
                } catch {
                }
            }
            Remove-Item $BackendPidFile -ErrorAction SilentlyContinue
        }
        
        Push-Location $BackendDir
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File "run-backend.ps1" "stop" | Out-Null
        Pop-Location
        
        $javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
        $stoppedCount = 0
        foreach ($proc in $javaProcesses) {
            try {
                $commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($proc.Id)").CommandLine
                if ($commandLine -and ($commandLine -like "*bootRun*" -or $commandLine -like "*tbs*" -or $commandLine -like "*$BackendDir*")) {
                    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
                    $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces Java zatrzymany (PID: $($proc.Id))"
                    $stoppedCount++
                }
            } catch {
            }
        }
        
        if ($stoppedCount -gt 0 -or (Test-Port -Port 8080)) {
            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Czekam na zwolnienie portu 8080 (max 10 sekund)..."
            $portWaitTimeout = 10
            $portWaitElapsed = 0
            $lastPortMessage = 0
            
            while ($portWaitElapsed -lt $portWaitTimeout) {
                if (-not (Test-Port -Port 8080)) {
                    $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 8080 zwolniony"
                    break
                }
                
                if (($portWaitElapsed - $lastPortMessage) -ge 2) {
                    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Port 8080 nadal zajety, czekam... ($portWaitElapsed/$portWaitTimeout sekund)"
                    $lastPortMessage = $portWaitElapsed
                }
                
                Start-Sleep -Seconds 1
                $portWaitElapsed += 1
            }
            
            if (Test-Port -Port 8080) {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Port 8080 nadal jest zajety. Proces moze potrzebowac wiecej czasu na zamkniecie."
            }
        }
        
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Backend zatrzymany"
    } catch {
        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Blad podczas zatrzymywania backendu: $_"
    }
}

function Stop-Frontend {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Zatrzymywanie frontendu..."
    
    try {
        if (Test-Path $FrontendPidFile) {
            $processId = Get-Content $FrontendPidFile -ErrorAction SilentlyContinue
            if ($processId) {
                try {
                    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
                    if ($process) {
                        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
                        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces PowerShell frontendu zatrzymany (PID: $processId)"
                    }
                } catch {
                }
            }
            Remove-Item $FrontendPidFile -ErrorAction SilentlyContinue
        }
        
        $nodeProcesses = Get-Process -Name "node" -ErrorAction SilentlyContinue
        $stoppedCount = 0
        foreach ($proc in $nodeProcesses) {
            try {
                $commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($proc.Id)").CommandLine
                if ($commandLine -and ($commandLine -like "*ng serve*" -or $commandLine -like "*$FrontendDir*" -or $commandLine -like "*angular*")) {
                    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
                    $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces Node.js zatrzymany (PID: $($proc.Id))"
                    $stoppedCount++
                }
            } catch {
            }
        }
        
        $powershellProcesses = Get-Process -Name "powershell" -ErrorAction SilentlyContinue
        foreach ($proc in $powershellProcesses) {
            try {
                $commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($proc.Id)").CommandLine
                if ($commandLine -and ($commandLine -like "*npm start*" -or $commandLine -like "*$FrontendDir*")) {
                    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
                    $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces PowerShell zatrzymany (PID: $($proc.Id))"
                    $stoppedCount++
                }
            } catch {
            }
        }
        
        if ($stoppedCount -gt 0 -or (Test-Port -Port 4200)) {
            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Czekam na zwolnienie portu 4200 (max 10 sekund)..."
            $portWaitTimeout = 10
            $portWaitElapsed = 0
            $lastPortMessage = 0
            
            while ($portWaitElapsed -lt $portWaitTimeout) {
                if (-not (Test-Port -Port 4200)) {
                    $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 4200 zwolniony"
                    break
                }
                
                if (($portWaitElapsed - $lastPortMessage) -ge 2) {
                    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Port 4200 nadal zajety, czekam... ($portWaitElapsed/$portWaitTimeout sekund)"
                    $lastPortMessage = $portWaitElapsed
                }
                
                Start-Sleep -Seconds 1
                $portWaitElapsed += 1
            }
            
            if (Test-Port -Port 4200) {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Port 4200 nadal jest zajety. Proces moze potrzebowac wiecej czasu na zamkniecie."
            }
        }
        
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Frontend zatrzymany"
    } catch {
        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Blad podczas zatrzymywania frontendu: $_"
    }
}

function Show-Status {
    Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzanie statusu serwisow..."
    Write-Host ""
    
    $allRunning = $true
    
    Write-ColorOutput -ForegroundColor Cyan "--- Backend (Spring Boot) ---"
    if (Test-Port -Port 8080) {
        Write-ColorOutput -ForegroundColor Green "  [OK] Backend dziala na porcie 8080"
        Write-ColorOutput -ForegroundColor Cyan "  [INFO] API: http://localhost:8080"
        Write-ColorOutput -ForegroundColor Cyan "  [INFO] Swagger UI: http://localhost:8080/swagger-ui/index.html"
    } else {
        Write-ColorOutput -ForegroundColor Red "  [STOP] Backend nie dziala"
        $allRunning = $false
    }
    
    Write-Host ""
    Write-ColorOutput -ForegroundColor Cyan "--- Frontend (Angular) ---"
    if (Test-Port -Port 4200) {
        Write-ColorOutput -ForegroundColor Green "  [OK] Frontend dziala na porcie 4200"
        Write-ColorOutput -ForegroundColor Cyan "  [INFO] Aplikacja: http://localhost:4200"
    } else {
        Write-ColorOutput -ForegroundColor Red "  [STOP] Frontend nie dziala"
        $allRunning = $false
    }
    
    Write-Host ""
    if ($allRunning) {
        Write-ColorOutput -ForegroundColor Green "===================================================================="
        Write-ColorOutput -ForegroundColor Green "Wszystkie serwisy dzialaja!"
        Write-ColorOutput -ForegroundColor Green "===================================================================="
    } else {
        Write-ColorOutput -ForegroundColor Yellow "===================================================================="
        Write-ColorOutput -ForegroundColor Yellow "Nie wszystkie serwisy dzialaja"
        Write-ColorOutput -ForegroundColor Yellow "===================================================================="
        Write-ColorOutput -ForegroundColor Cyan "`[HINT`] Uruchom: .\start.ps1 start"
    }
}

function Show-Menu {
    $validChoice = $false
    $selectedAction = ""
    
    while (-not $validChoice) {
        Write-Host ""
        Write-ColorOutput -ForegroundColor Cyan "===================================================================="
        Write-ColorOutput -ForegroundColor Cyan "Wybierz akcje:"
        Write-ColorOutput -ForegroundColor Cyan "===================================================================="
        Write-Host ""
        Write-ColorOutput -ForegroundColor Yellow "  1) Start    - Uruchom backend i frontend"
        Write-ColorOutput -ForegroundColor Yellow "  2) Restart  - Zrestartuj backend i frontend"
        Write-ColorOutput -ForegroundColor Yellow "  3) Stop     - Zatrzymaj backend i frontend"
        Write-ColorOutput -ForegroundColor Yellow "  4) Status   - Sprawdz status serwisow"
        Write-Host ""
        Write-Host -NoNewline "Wpisz numer (1-4) lub nacisnij Enter aby wyjsc: "
        
        $choice = Read-Host
        
        switch ($choice) {
            "1" { 
                $selectedAction = "start"
                $validChoice = $true
            }
            "2" { 
                $selectedAction = "restart"
                $validChoice = $true
            }
            "3" { 
                $selectedAction = "stop"
                $validChoice = $true
            }
            "4" { 
                $selectedAction = "status"
                $validChoice = $true
            }
            "" { 
                Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Anulowano"
                exit 0
            }
            default {
                Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nieprawidlowy wybor. Wybierz 1, 2, 3 lub 4."
                Write-Host ""
            }
        }
    }
    
    return $selectedAction
}

Write-ColorOutput -ForegroundColor Cyan "===================================================================="
Write-ColorOutput -ForegroundColor Cyan "World at War: Turn-Based Strategy - Global Runner"
Write-ColorOutput -ForegroundColor Cyan "===================================================================="
Write-Host ""

if ([string]::IsNullOrWhiteSpace($Action)) {
    $Action = Show-Menu
}

switch ($Action) {
    "start" {
        Write-ColorOutput -ForegroundColor Cyan "[INICJUJE START]"
        Write-Host ""
        
        $backendStarted = Start-Backend
        if (-not $backendStarted) {
            Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic backendu"
            exit 1
        }
        
        Write-Host ""
        Start-Sleep -Seconds 1
        
        $frontendStarted = Start-Frontend
        if (-not $frontendStarted) {
            Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Nie udalo sie uruchomic frontendu"
        }
        
        Write-Host ""
        Write-ColorOutput -ForegroundColor Green "===================================================================="
        Write-ColorOutput -ForegroundColor Green "Aplikacja uruchomiona!"
        Write-ColorOutput -ForegroundColor Green "===================================================================="
        Write-ColorOutput -ForegroundColor Cyan "  Frontend: http://localhost:4200"
        Write-ColorOutput -ForegroundColor Cyan "  Backend API: http://localhost:8080"
        Write-ColorOutput -ForegroundColor Cyan "  Swagger UI: http://localhost:8080/swagger-ui/index.html"
        Write-Host ""
        Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Aby zatrzymac aplikacje, uruchom: .\start.ps1 stop"
    }
    
    "restart" {
        Write-ColorOutput -ForegroundColor Cyan "[INICJUJE RESTART]"
        Write-Host ""
        
        Stop-Frontend
        Stop-Backend
        
        Write-Host ""
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Czekam 3 sekundy przed uruchomieniem nowych procesow..."
        for ($i = 3; $i -gt 0; $i--) {
            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Restart za $i sekund..."
            Start-Sleep -Seconds 1
        }
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Rozpoczynam restart serwisow"
        Write-Host ""
        
        if (Test-Port -Port 8080) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 8080 nadal jest zajety. Backend nie zostal poprawnie zatrzymany."
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Zatrzymaj procesy recznie: .\start.ps1 stop"
            exit 1
        }
        
        if (Test-Port -Port 4200) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 4200 nadal jest zajety. Frontend nie zostal poprawnie zatrzymany."
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Zatrzymaj procesy recznie: .\start.ps1 stop"
            exit 1
        }
        
        $backendStarted = Start-Backend
        if (-not $backendStarted) {
            Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic backendu"
            Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy port 8080 jest wolny: Test-NetConnection -ComputerName localhost -Port 8080"
            exit 1
        }
        
        Write-Host ""
        Start-Sleep -Seconds 1
        
        $frontendStarted = Start-Frontend
        if (-not $frontendStarted) {
            Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic frontendu"
            Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy port 4200 jest wolny: Test-NetConnection -ComputerName localhost -Port 4200"
            exit 1
        }
        
        Write-Host ""
        Write-ColorOutput -ForegroundColor Green "===================================================================="
        Write-ColorOutput -ForegroundColor Green "Aplikacja zrestartowana!"
        Write-ColorOutput -ForegroundColor Green "===================================================================="
        Write-ColorOutput -ForegroundColor Cyan "  Frontend: http://localhost:4200"
        Write-ColorOutput -ForegroundColor Cyan "  Backend API: http://localhost:8080"
        Write-ColorOutput -ForegroundColor Cyan "  Swagger UI: http://localhost:8080/swagger-ui/index.html"
    }
    
    "stop" {
        Write-ColorOutput -ForegroundColor Cyan "[INICJUJE STOP]"
        Write-Host ""
        
        Stop-Frontend
        Stop-Backend
        
        Write-Host ""
        Write-ColorOutput -ForegroundColor Green "`[OK`] Wszystkie serwisy zatrzymane"
    }
    
    "status" {
        Show-Status
    }
}

Write-Host ""

