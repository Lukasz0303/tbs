param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("start", "restart", "stop", "status", "verify")]
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

function Test-RedisHealth {
    if (Test-Port -Port 6379) {
        try {
            $redisCli = Get-Command redis-cli -ErrorAction SilentlyContinue
            if ($redisCli) {
                $result = & redis-cli ping 2>&1
                if ($result -match "PONG") {
                    return $true
                }
            }
        } catch {
        }
        return $true
    }
    return $false
}

function Start-Redis {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzanie Redis..."
    
    if (Test-Port -Port 6379) {
        if (Test-RedisHealth) {
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Redis juz dziala na porcie 6379"
            return $true
        }
    }
    
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Uruchamianie Redis..."
    
    try {
        $dockerCheck = Get-Command docker -ErrorAction SilentlyContinue
        if (-not $dockerCheck) {
            $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Docker nie jest dostepny, sprawdzam czy Redis jest uruchomiony przez Supabase..."
            
            Push-Location $ScriptPath
            try {
                $npxCheck = Get-Command npx -ErrorAction SilentlyContinue
                if ($npxCheck) {
                    $originalErrorAction = $ErrorActionPreference
                    $ErrorActionPreference = "Continue"
                    
                    $statusOutput = & npx supabase status 2>&1 | Where-Object { 
                        $_ -notmatch "npm warn" -and 
                        $_ -notmatch "Unknown user config"
                    }
                    $ErrorActionPreference = $originalErrorAction
                    
                    $status = ($statusOutput | Out-String).Trim()
                    if ($status -match "running" -or $status -match "Redis") {
                        Start-Sleep -Seconds 3
                        if (Test-Port -Port 6379) {
                            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Redis uruchomiony przez Supabase"
                            Pop-Location
                            return $true
                        }
                    }
                }
            } catch {
            } finally {
                Pop-Location
            }
            
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic Redis"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Zainstaluj Docker Desktop lub uruchom Supabase: npx supabase start"
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam czy kontener Redis istnieje..."
        $container = docker ps -a --filter "name=waw-redis" --format "{{.Names}}" 2>&1
        
        if ($container -and $container -eq "waw-redis") {
            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Kontener istnieje, uruchamiam..."
            docker start waw-redis 2>&1 | Out-Null
            Start-Sleep -Seconds 3
            
            if (Test-Port -Port 6379) {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Redis uruchomiony (z istniejącego kontenera)"
                return $true
            }
        } else {
            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Tworzenie nowego kontenera Redis..."
            docker run --name waw-redis -p 6379:6379 -d redis:7 2>&1 | Out-Null
            Start-Sleep -Seconds 3
            
            if (Test-Port -Port 6379) {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Redis uruchomiony (nowy kontener)"
                return $true
            }
        }
        
        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Redis nie odpowiada, sprawdzam kontener..."
        $containerStatus = docker ps --filter "name=waw-redis" --format "{{.Status}}" 2>&1
        if ($containerStatus) {
            $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Kontener Redis: $containerStatus"
        }
        
        return $false
    } catch {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Blad podczas uruchamiania Redis: $_"
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Upewnij sie ze Docker Desktop jest uruchomiony"
        return $false
    }
}

function Stop-Redis {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Zatrzymywanie Redis..."
    
    try {
        $dockerCheck = Get-Command docker -ErrorAction SilentlyContinue
        if ($dockerCheck) {
            $container = docker ps --filter "name=waw-redis" --format "{{.Names}}" 2>&1
            if ($container -and $container -eq "waw-redis") {
                docker stop waw-redis 2>&1 | Out-Null
                Start-Sleep -Seconds 2
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Kontener Redis zatrzymany"
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Kontener Redis nie jest uruchomiony"
            }
        } else {
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Docker nie jest dostepny, Redis moze byc zarzadzany przez Supabase"
        }
        
        if (Test-Port -Port 6379) {
            $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Port 6379 nadal jest zajety. Redis moze byc uruchomiony przez Supabase."
        } else {
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 6379 zwolniony"
        }
    } catch {
        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Blad podczas zatrzymywania Redis: $_"
    }
}

function Start-Backend {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] ===================================="
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Uruchamianie backendu..."
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] ===================================="
    
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam i uruchamiam Redis..."
    $redisStarted = Start-Redis
    if (-not $redisStarted) {
        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Redis nie dziala, ale kontynuuje uruchamianie backendu..."
    }
    
    if (-not (Test-Path $BackendScript)) {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Skrypt backendu nie istnieje: $BackendScript"
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy katalog backend istnieje i zawiera plik run-backend.ps1"
        return $false
    }
    
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam czy port 8080 jest wolny..."
    if (Test-Port -Port 8080) {
        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Port 8080 jest juz zajety"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Próbuję znaleźć i zatrzymać istniejący proces..."
        
        try {
            $connection = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
            if ($connection) {
                $pid = $connection.OwningProcess
                $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
                if ($proc -and $proc.ProcessName -eq "java") {
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Znaleziono proces Java na porcie 8080 (PID: $pid), zatrzymuję..."
                    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                    Start-Sleep -Seconds 2
                }
            }
        } catch {
        }
        
        $portWaitTimeout = 5
        $portWaitElapsed = 0
        $portFreed = $false
        
        while ($portWaitElapsed -lt $portWaitTimeout) {
            if (-not (Test-Port -Port 8080)) {
                $portFreed = $true
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 8080 zwolniony"
                break
            }
            Start-Sleep -Seconds 1
            $portWaitElapsed += 1
        }
        
        if (-not $portFreed) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 8080 nadal jest zajety po 5 sekundach"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz procesy Java: Get-Process -Name java"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Lub zatrzymaj recznie: .\start.ps1 stop"
            return $false
        }
    } else {
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 8080 jest wolny"
    }
    
    try {
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Uruchamiam skrypt backendu: $BackendScript"
        Push-Location $BackendDir
        $backendProcess = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "run-backend.ps1", "start" -PassThru -WindowStyle Hidden
        Pop-Location
        
        if (-not $backendProcess) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic procesu backendu"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy skrypt run-backend.ps1 istnieje i jest wykonywalny"
            return $false
        }
        
        $processId = $backendProcess.Id
        $processId | Out-File -FilePath $BackendPidFile -Force
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces PowerShell backendu uruchomiony (PID: $processId)"
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Oczekiwanie na uruchomienie backendu (max 90 sekund)..."
        $timeout = 90
        $elapsed = 0
        $portOpen = $false
        $lastStatusMessage = 0
        $healthCheckAttempts = 0
        $maxHealthCheckAttempts = 5
        
        while ($elapsed -lt $timeout) {
            try {
                $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
                if (-not $process) {
                    $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces PowerShell backendu zakonczyl sie przedwczesnie"
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Mozliwe przyczyny:"
                    $null = Write-ColorOutput -ForegroundColor Yellow "  - Blad w skrypcie run-backend.ps1"
                    $null = Write-ColorOutput -ForegroundColor Yellow "  - Brak wymaganych zaleznosci (Java, Gradle, Supabase)"
                    $null = Write-ColorOutput -ForegroundColor Yellow "  - Problem z konfiguracja (application.properties)"
                    Show-BackendErrorLogs
                    break
                }
            } catch {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Nie mozna sprawdzic statusu procesu PowerShell"
            }
            
            if (Test-Port -Port 8080) {
                if (-not $portOpen) {
                    $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 8080 otwarty (po $elapsed sekundach)"
                    $portOpen = $true
                    $healthCheckAttempts = 0
                }
                
                if ($portOpen) {
                    $healthCheckAttempts++
                    Start-Sleep -Seconds 2
                    
                    if (Test-BackendHealth) {
                        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Backend odpowiada na health check (po $healthCheckAttempts probach)"
                        
                        try {
                            $swaggerCheck = Invoke-WebRequest -Uri "http://localhost:8080/swagger-ui/index.html" -Method Get -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
                            if ($swaggerCheck.StatusCode -eq 200) {
                                $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Swagger UI dostepny: http://localhost:8080/swagger-ui/index.html"
                            }
                        } catch {
                        }
                        
                        return $true
                    } elseif ($healthCheckAttempts -ge $maxHealthCheckAttempts) {
                        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 8080 otwarty, ale backend nie odpowiada na health check po $maxHealthCheckAttempts probach"
                        $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Sprawdzam logi backendu..."
                        Show-BackendErrorLogs
                        break
                    } elseif ($healthCheckAttempts -le 2) {
                        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Port otwarty, ale health check nie odpowiada (proba $healthCheckAttempts/$maxHealthCheckAttempts), czekam..."
                    }
                }
            } else {
                if ($elapsed -gt 0 -and ($elapsed - $lastStatusMessage) -ge 5) {
                    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Czekam na otwarcie portu 8080... ($elapsed/$timeout sekund)"
                    $lastStatusMessage = $elapsed
                }
            }
            
            Start-Sleep -Seconds 1
            $elapsed += 1
        }
        
        Write-Host ""
        if ($portOpen) {
            if (-not (Test-BackendHealth)) {
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 8080 otwarty, ale backend nie odpowiada na health check"
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Sprawdzam logi backendu..."
                Show-BackendErrorLogs
            }
        } else {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Backend nie uruchomil sie w czasie $timeout sekund"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Mozliwe przyczyny:"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Backend ma blad kompilacji lub uruchomienia"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Brak wymaganych zaleznosci (Java, Gradle, Supabase, Redis)"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Problem z konfiguracja bazy danych lub Redis"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Sprawdzam logi backendu..."
            Show-BackendErrorLogs
        }
        
        return $false
    } catch {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Blad podczas uruchamiania backendu: $_"
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Szczegoly bledu: $($_.Exception.Message)"
        if ($_.Exception.InnerException) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Wewnetrzny blad: $($_.Exception.InnerException.Message)"
        }
        Show-BackendErrorLogs
        return $false
    }
}

function Show-BackendErrorLogs {
    $logFile = Join-Path $BackendDir "application.log"
    if (Test-Path $logFile) {
        $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
        $null = Write-ColorOutput -ForegroundColor Yellow "OSTATNIE LOGI BACKENDU:"
        $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
        $logContent = Get-Content $logFile -Tail 50 -ErrorAction SilentlyContinue
        if ($logContent) {
            $errorLines = $logContent | Where-Object { $_ -match "ERROR|WARN|Exception|Failed|Error|APPLICATION FAILED" }
            if ($errorLines) {
                $errorLines | ForEach-Object { Write-ColorOutput -ForegroundColor Red "  $_" }
            } else {
                $logContent | Select-Object -Last 20 | ForEach-Object { Write-Host "  $_" }
            }
        }
        $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Pelne logi: $logFile"
    } else {
        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Plik logow nie istnieje: $logFile"
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy proces backendu dziala: Get-Process -Name java"
    }
}

function Start-Frontend {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] ===================================="
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Uruchamianie frontendu..."
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] ===================================="
    
    if (-not (Test-Path $FrontendDir)) {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Katalog frontendu nie istnieje: $FrontendDir"
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy katalog frontend istnieje w katalogu projektu"
        return $false
    }
    
    $packageJsonPath = Join-Path $FrontendDir "package.json"
    if (-not (Test-Path $packageJsonPath)) {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] package.json nie istnieje w katalogu frontendu"
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy jestes w prawidlowym katalogu projektu"
        return $false
    }
    
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam czy port 4200 jest wolny..."
    if (Test-Port -Port 4200) {
        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Port 4200 jest juz zajety"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Próbuję znaleźć i zatrzymać istniejący proces..."
        
        try {
            $connection = Get-NetTCPConnection -LocalPort 4200 -State Listen -ErrorAction SilentlyContinue
            if ($connection) {
                $pid = $connection.OwningProcess
                $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
                if ($proc -and $proc.ProcessName -eq "node") {
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Znaleziono proces Node.js na porcie 4200 (PID: $pid), zatrzymuję..."
                    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                    Start-Sleep -Seconds 2
                }
            }
        } catch {
        }
        
        $portWaitTimeout = 5
        $portWaitElapsed = 0
        $portFreed = $false
        
        while ($portWaitElapsed -lt $portWaitTimeout) {
            if (-not (Test-Port -Port 4200)) {
                $portFreed = $true
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 4200 zwolniony"
                break
            }
            Start-Sleep -Seconds 1
            $portWaitElapsed += 1
        }
        
        if (-not $portFreed) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 4200 nadal jest zajety po 5 sekundach"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz procesy Node.js: Get-Process -Name node"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Lub zatrzymaj recznie: .\start.ps1 stop"
            return $false
        }
    } else {
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 4200 jest wolny"
    }
    
    try {
        $logFile = Join-Path $FrontendDir "start.log"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Uruchamiam npm start w katalogu: $FrontendDir"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Logi beda zapisywane do: $logFile"
        
        Push-Location $FrontendDir
        
        if (Test-Path $logFile) {
            Clear-Content $logFile -ErrorAction SilentlyContinue
        }
        
        $frontendProcess = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", "npm start *> start.log" -PassThru -WindowStyle Hidden
        
        Pop-Location
        
        if (-not $frontendProcess) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic procesu frontendu"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy npm jest zainstalowane: npm --version"
            return $false
        }
        
        $frontendProcess.Id | Out-File -FilePath $FrontendPidFile -Force
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces PowerShell frontendu uruchomiony (PID: $($frontendProcess.Id))"
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Oczekiwanie na uruchomienie frontendu (max 45 sekund)..."
        $timeout = 45
        $elapsed = 0
        $lastStatusMessage = 0
        $nodeProcessFound = $false
        $portOpen = $false
        $httpCheckAttempts = 0
        $maxHttpCheckAttempts = 5
        
        while ($elapsed -lt $timeout) {
            try {
                $process = Get-Process -Id $frontendProcess.Id -ErrorAction SilentlyContinue
                if (-not $process) {
                    $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces PowerShell frontendu zakonczyl sie przedwczesnie"
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Mozliwe przyczyny:"
                    $null = Write-ColorOutput -ForegroundColor Yellow "  - Blad kompilacji TypeScript/Angular"
                    $null = Write-ColorOutput -ForegroundColor Yellow "  - Brak zainstalowanych pakietow npm"
                    $null = Write-ColorOutput -ForegroundColor Yellow "  - Problem z konfiguracja Angular"
                    Show-FrontendErrorLogs
                    break
                }
            } catch {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Nie mozna sprawdzic statusu procesu PowerShell"
            }
            
            if (-not $nodeProcessFound) {
                $nodeProcesses = Get-Process -Name "node" -ErrorAction SilentlyContinue
                foreach ($nodeProc in $nodeProcesses) {
                    try {
                        $commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($nodeProc.Id)").CommandLine
                        if ($commandLine -and ($commandLine -like "*ng serve*" -or $commandLine -like "*$FrontendDir*")) {
                            $nodeProcessFound = $true
                            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Znaleziono proces Node.js Angular (PID: $($nodeProc.Id))"
                            break
                        }
                    } catch {
                    }
                }
            }
            
            if (Test-Port -Port 4200) {
                if (-not $portOpen) {
                    $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 4200 otwarty (po $elapsed sekundach)"
                    $portOpen = $true
                    $httpCheckAttempts = 0
                }
                
                if ($portOpen) {
                    $httpCheckAttempts++
                    Start-Sleep -Seconds 2
                    
                    try {
                        $response = Invoke-WebRequest -Uri "http://localhost:4200" -Method Get -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
                        if ($response.StatusCode -eq 200) {
                            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Frontend odpowiada na requesty HTTP (po $httpCheckAttempts probach)"
                            return $true
                        }
                    } catch {
                        if ($httpCheckAttempts -ge $maxHttpCheckAttempts) {
                            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 4200 otwarty, ale frontend nie odpowiada na requesty HTTP po $maxHttpCheckAttempts probach"
                            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Sprawdzam logi frontendu..."
                            Show-FrontendErrorLogs
                            break
                        } elseif ($httpCheckAttempts -le 2) {
                            $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Port otwarty, ale frontend jeszcze nie odpowiada na requesty (proba $httpCheckAttempts/$maxHttpCheckAttempts), czekam..."
                        }
                    }
                }
            } else {
                if ($elapsed -gt 0 -and ($elapsed - $lastStatusMessage) -ge 5) {
                    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Czekam na otwarcie portu 4200... ($elapsed/$timeout sekund)"
                    $lastStatusMessage = $elapsed
                }
            }
            
            Start-Sleep -Seconds 1
            $elapsed += 1
        }
        
        Write-Host ""
        if ($portOpen) {
            try {
                $finalResponse = Invoke-WebRequest -Uri "http://localhost:4200" -Method Get -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
                if ($finalResponse.StatusCode -ne 200) {
                    $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 4200 otwarty, ale frontend nie odpowiada poprawnie (status: $($finalResponse.StatusCode))"
                    Show-FrontendErrorLogs
                }
            } catch {
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 4200 otwarty, ale frontend nie odpowiada na requesty HTTP"
                Show-FrontendErrorLogs
            }
        } else {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Frontend nie uruchomil sie w czasie $timeout sekund"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Mozliwe przyczyny:"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Frontend ma blad kompilacji TypeScript/Angular"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Port 4200 jest zajety przez inny proces"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Brak wymaganych zaleznosci (Node.js, npm)"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Problem z instalacja pakietow npm (uruchom: npm install w katalogu frontend)"
            $null = Write-ColorOutput -ForegroundColor Yellow "  - Blad w konfiguracji Angular (sprawdz angular.json)"
            Show-FrontendErrorLogs
        }
        
        return $false
    } catch {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Blad podczas uruchamiania frontendu: $_"
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Szczegoly bledu: $($_.Exception.Message)"
        if ($_.Exception.InnerException) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Wewnetrzny blad: $($_.Exception.InnerException.Message)"
        }
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Upewnij sie ze Node.js i npm sa zainstalowane"
        Show-FrontendErrorLogs
        return $false
    }
}

function Show-FrontendErrorLogs {
    $logFile = Join-Path $FrontendDir "start.log"
    if (Test-Path $logFile) {
        $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
        $null = Write-ColorOutput -ForegroundColor Yellow "OSTATNIE LOGI FRONTENDU:"
        $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
        $logContent = Get-Content $logFile -Tail 50 -ErrorAction SilentlyContinue
        if ($logContent) {
            $errorLines = $logContent | Where-Object { $_ -match "ERROR|WARN|failed|error|Error|Cannot find|Module not found|compilation failed" }
            if ($errorLines) {
                $errorLines | ForEach-Object { Write-ColorOutput -ForegroundColor Red "  $_" }
            } else {
                $logContent | Select-Object -Last 20 | ForEach-Object { Write-Host "  $_" }
            }
        }
        $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Pelne logi: $logFile"
    } else {
        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Plik logow nie istnieje: $logFile"
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy proces Node.js dziala: Get-Process -Name node"
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
    
    Write-ColorOutput -ForegroundColor Cyan "--- Redis ---"
    if (Test-Port -Port 6379) {
        if (Test-RedisHealth) {
            Write-ColorOutput -ForegroundColor Green "  [OK] Redis dziala na porcie 6379"
        } else {
            Write-ColorOutput -ForegroundColor Yellow "  [WARN] Port 6379 otwarty, ale Redis nie odpowiada poprawnie"
        }
    } else {
        Write-ColorOutput -ForegroundColor Red "  [STOP] Redis nie dziala"
        $allRunning = $false
    }
    
    Write-Host ""
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

function Verify-BothServices {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] ===================================="
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Weryfikacja dzialania serwisow..."
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] ===================================="
    Write-Host ""
    
    $allOk = $true
    
    Write-ColorOutput -ForegroundColor Cyan "--- Backend (Spring Boot) ---"
    $backendOk = $true
    
    if (-not (Test-Port -Port 8080)) {
        $null = Write-ColorOutput -ForegroundColor Red "  [ERROR] Port 8080 nie jest otwarty"
        $backendOk = $false
        $allOk = $false
    } else {
        $null = Write-ColorOutput -ForegroundColor Green "  [OK] Port 8080 jest otwarty"
        
        if (-not (Test-BackendHealth)) {
            $null = Write-ColorOutput -ForegroundColor Red "  [ERROR] Backend nie odpowiada na health check"
            $backendOk = $false
            $allOk = $false
        } else {
            $null = Write-ColorOutput -ForegroundColor Green "  [OK] Backend odpowiada na health check"
            
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs" -Method Get -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
                if ($response.StatusCode -eq 200) {
                    $null = Write-ColorOutput -ForegroundColor Green "  [OK] Backend API odpowiada"
                } else {
                    $null = Write-ColorOutput -ForegroundColor Yellow "  [WARN] Backend API zwrocil status: $($response.StatusCode)"
                }
            } catch {
                $null = Write-ColorOutput -ForegroundColor Red "  [ERROR] Backend API nie odpowiada: $($_.Exception.Message)"
                $backendOk = $false
                $allOk = $false
            }
        }
        
        $javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
        $foundJavaProcess = $false
        foreach ($proc in $javaProcesses) {
            try {
                $commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($proc.Id)").CommandLine
                if ($commandLine -and ($commandLine -like "*bootRun*" -or $commandLine -like "*tbs*" -or $commandLine -like "*$BackendDir*")) {
                    $null = Write-ColorOutput -ForegroundColor Green "  [OK] Proces Java backendu dziala (PID: $($proc.Id))"
                    $foundJavaProcess = $true
                    break
                }
            } catch {
            }
        }
        
        if (-not $foundJavaProcess) {
            $null = Write-ColorOutput -ForegroundColor Yellow "  [WARN] Nie znaleziono procesu Java backendu"
        }
    }
    
    if (-not $backendOk) {
        $null = Write-ColorOutput -ForegroundColor Yellow "  [HINT] Sprawdz logi backendu: Get-Content backend\application.log -Tail 50"
    }
    
    Write-Host ""
    Write-ColorOutput -ForegroundColor Cyan "--- Frontend (Angular) ---"
    $frontendOk = $true
    
    if (-not (Test-Port -Port 4200)) {
        $null = Write-ColorOutput -ForegroundColor Red "  [ERROR] Port 4200 nie jest otwarty"
        $frontendOk = $false
        $allOk = $false
    } else {
        $null = Write-ColorOutput -ForegroundColor Green "  [OK] Port 4200 jest otwarty"
        
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:4200" -Method Get -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                $null = Write-ColorOutput -ForegroundColor Green "  [OK] Frontend odpowiada na requesty HTTP"
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "  [WARN] Frontend zwrocil status: $($response.StatusCode)"
            }
        } catch {
            $null = Write-ColorOutput -ForegroundColor Red "  [ERROR] Frontend nie odpowiada na requesty HTTP: $($_.Exception.Message)"
            $frontendOk = $false
            $allOk = $false
        }
        
        $nodeProcesses = Get-Process -Name "node" -ErrorAction SilentlyContinue
        $foundNodeProcess = $false
        foreach ($proc in $nodeProcesses) {
            try {
                $commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($proc.Id)").CommandLine
                if ($commandLine -and ($commandLine -like "*ng serve*" -or $commandLine -like "*$FrontendDir*")) {
                    $null = Write-ColorOutput -ForegroundColor Green "  [OK] Proces Node.js frontendu dziala (PID: $($proc.Id))"
                    $foundNodeProcess = $true
                    break
                }
            } catch {
            }
        }
        
        if (-not $foundNodeProcess) {
            $null = Write-ColorOutput -ForegroundColor Yellow "  [WARN] Nie znaleziono procesu Node.js frontendu"
        }
    }
    
    if (-not $frontendOk) {
        $logFile = Join-Path $FrontendDir "start.log"
        $null = Write-ColorOutput -ForegroundColor Yellow "  [HINT] Sprawdz logi frontendu: Get-Content frontend\start.log -Tail 50"
    }
    
    Write-Host ""
    Write-ColorOutput -ForegroundColor Cyan "--- Redis ---"
    $redisOk = $true
    
    if (-not (Test-Port -Port 6379)) {
        $null = Write-ColorOutput -ForegroundColor Red "  [ERROR] Port 6379 nie jest otwarty"
        $redisOk = $false
        $allOk = $false
    } else {
        $null = Write-ColorOutput -ForegroundColor Green "  [OK] Port 6379 jest otwarty"
        
        if (Test-RedisHealth) {
            $null = Write-ColorOutput -ForegroundColor Green "  [OK] Redis odpowiada poprawnie"
        } else {
            $null = Write-ColorOutput -ForegroundColor Yellow "  [WARN] Redis moze nie odpowiadac poprawnie (port otwarty, ale health check nie powiodl sie)"
        }
    }
    
    Write-Host ""
    Write-ColorOutput -ForegroundColor Cyan "--- Podsumowanie ---"
    if ($allOk) {
        Write-ColorOutput -ForegroundColor Green "===================================================================="
        Write-ColorOutput -ForegroundColor Green "Wszystkie serwisy dzialaja poprawnie!"
        Write-ColorOutput -ForegroundColor Green "===================================================================="
        Write-ColorOutput -ForegroundColor Cyan "  Frontend: http://localhost:4200"
        Write-ColorOutput -ForegroundColor Cyan "  Backend API: http://localhost:8080"
        Write-ColorOutput -ForegroundColor Cyan "  Swagger UI: http://localhost:8080/swagger-ui/index.html"
        return $true
    } else {
        Write-ColorOutput -ForegroundColor Red "===================================================================="
        Write-ColorOutput -ForegroundColor Red "Nie wszystkie serwisy dzialaja poprawnie!"
        Write-ColorOutput -ForegroundColor Red "===================================================================="
        if (-not $backendOk) {
            Write-ColorOutput -ForegroundColor Yellow "  - Backend ma problemy"
        }
        if (-not $frontendOk) {
            Write-ColorOutput -ForegroundColor Yellow "  - Frontend ma problemy"
        }
        if (-not $redisOk) {
            Write-ColorOutput -ForegroundColor Yellow "  - Redis ma problemy"
        }
        return $false
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
        Write-ColorOutput -ForegroundColor Yellow "  5) Verify   - Uruchom FE i BE i zweryfikuj czy dzialaja poprawnie"
        Write-Host ""
        Write-Host -NoNewline "Wpisz numer (1-5) lub nacisnij Enter aby wyjsc: "
        
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
            "5" {
                $selectedAction = "verify"
                $validChoice = $true
            }
            "" { 
                Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Anulowano"
                exit 0
            }
            default {
                Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nieprawidlowy wybor. Wybierz 1, 2, 3, 4 lub 5."
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
        Stop-Redis
        
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
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Uruchamiam Redis..."
        $redisStarted = Start-Redis
        if (-not $redisStarted) {
            $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Redis nie dziala, ale kontynuuje restart..."
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
        Stop-Redis
        
        Write-Host ""
        Write-ColorOutput -ForegroundColor Green "`[OK`] Wszystkie serwisy zatrzymane"
    }
    
    "status" {
        Show-Status
    }
    
    "verify" {
        Write-ColorOutput -ForegroundColor Cyan "[INICJUJE START Z WERYFIKACJA]"
        Write-Host ""
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Krok 1/3: Uruchamianie backendu..."
        $backendStarted = Start-Backend
        if (-not $backendStarted) {
            Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic backendu"
            Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz logi backendu powyzej aby znalezc przyczyne problemu"
            exit 1
        }
        
        Write-Host ""
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Krok 2/3: Uruchamianie frontendu..."
        Start-Sleep -Seconds 2
        
        $frontendStarted = Start-Frontend
        if (-not $frontendStarted) {
            Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic frontendu"
            Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz logi frontendu powyzej aby znalezc przyczyne problemu"
            exit 1
        }
        
        Write-Host ""
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Krok 3/3: Weryfikacja dzialania serwisow..."
        Start-Sleep -Seconds 3
        
        $verificationOk = Verify-BothServices
        
        Write-Host ""
        if ($verificationOk) {
            Write-ColorOutput -ForegroundColor Green "===================================================================="
            Write-ColorOutput -ForegroundColor Green "Uruchomienie i weryfikacja zakonczone pomyslnie!"
            Write-ColorOutput -ForegroundColor Green "===================================================================="
            Write-ColorOutput -ForegroundColor Cyan "  Frontend: http://localhost:4200"
            Write-ColorOutput -ForegroundColor Cyan "  Backend API: http://localhost:8080"
            Write-ColorOutput -ForegroundColor Cyan "  Swagger UI: http://localhost:8080/swagger-ui/index.html"
            Write-Host ""
            Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Aby zatrzymac aplikacje, uruchom: .\start.ps1 stop"
        } else {
            Write-ColorOutput -ForegroundColor Red "===================================================================="
            Write-ColorOutput -ForegroundColor Red "Uruchomienie zakonczone, ale weryfikacja wykazala problemy!"
            Write-ColorOutput -ForegroundColor Red "===================================================================="
            Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz szczegoly powyzej aby znalezc przyczyne problemu"
            exit 1
        }
    }
}

Write-Host ""

