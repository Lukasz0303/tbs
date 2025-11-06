param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("start", "restart", "logs", "stop", "status")]
    [string]$Action = "start"
)

$ErrorActionPreference = "Stop"

$ScriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = $ScriptPath
$ProjectRoot = Split-Path -Parent $ScriptPath
$LogFile = Join-Path $BackendDir "application.log"
$JAVA_HOME_PATHS = @(
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot",
    "C:\Program Files\Eclipse Adoptium",
    "C:\Program Files\Java"
)

function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    $host.UI.RawUI.ForegroundColor = $fc
}


function Find-JavaHome {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Szukam Java 21..."
    
    $originalErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    
    $result = $null
    
    try {
        if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
            $javaVersionOutput = & "$env:JAVA_HOME\bin\java.exe" -version 2>&1
            $javaVersionLine = $javaVersionOutput | Where-Object { $_ -match "version" } | Select-Object -First 1
            if ($javaVersionLine -and $javaVersionLine -match "21") {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] JAVA_HOME juz ustawiony: $env:JAVA_HOME"
                $result = $env:JAVA_HOME
            }
        }
        
        if (-not $result) {
            foreach ($path in $JAVA_HOME_PATHS) {
                if (Test-Path $path) {
                    $jdkDirs = Get-ChildItem -Path $path -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -like "jdk-21*" } | Sort-Object Name -Descending
                    if ($jdkDirs) {
                        $javaPath = $jdkDirs[0].FullName
                        $javaVersionOutput = & "$javaPath\bin\java.exe" -version 2>&1
                        $javaVersionLine = $javaVersionOutput | Where-Object { $_ -match "version" } | Select-Object -First 1
                        if ($javaVersionLine -and $javaVersionLine -match "21") {
                            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Znaleziono Java 21: $javaPath"
                            $result = $javaPath
                            break
                        }
                    }
                }
            }
        }
    } catch {
        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Blad podczas sprawdzania Java: $_"
    } finally {
        $ErrorActionPreference = $originalErrorAction
    }
    
    if ($result) {
        return $result
    }
    
    $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie znaleziono Java 21!"
    $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Zainstaluj Java 21 z: https://adoptium.net/temurin/releases/"
    exit 1
}

function Set-JavaEnvironment {
    $javaHome = Find-JavaHome
    if ($javaHome) {
        $env:JAVA_HOME = $javaHome.Trim()
        $env:PATH = "$($javaHome.Trim())\bin;$env:PATH"
        Write-Host "[DEBUG] ====== JAVA_HOME ustawiony na: $env:JAVA_HOME =====" -ForegroundColor Magenta
    } else {
        Write-Host "[DEBUG] ====== Find-JavaHome zwrocilo null lub pusty string =====" -ForegroundColor Red
    }
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

function Wait-ForPort {
    param(
        [int]$Port,
        [int]$Timeout = 30,
        [string]$ServiceName
    )
    
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Oczekiwanie na $ServiceName na porcie $Port..."
    $elapsed = 0
    while ($elapsed -lt $Timeout) {
        if (Test-Port -Port $Port) {
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] $ServiceName uruchomiony na porcie $Port"
            return $true
        }
        Start-Sleep -Seconds 2
        $elapsed += 2
        Write-Host -NoNewline "."
    }
    Write-Host ""
    $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Timeout oczekiwania na $ServiceName"
    return $false
}

function Start-Supabase {
    Write-Host "`[DEBUG`] ====== Start-Supabase WYWOŁANA =====" -ForegroundColor Magenta
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzanie statusu Supabase..."
    $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] ProjectRoot: $ProjectRoot"
    $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] BackendDir: $BackendDir"
    
    if (-not (Test-Path $ProjectRoot)) {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Katalog projektu nie istnieje: $ProjectRoot"
        return $false
    }
    
    Write-Host "`[DEBUG`] ====== PRZED Push-Location =====" -ForegroundColor Magenta
    Push-Location $ProjectRoot
    $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Obecny katalog po Push-Location: $(Get-Location)"
    Write-Host "`[DEBUG`] ====== W BLOKU TRY =====" -ForegroundColor Magenta
    try {
        Write-Host "`[DEBUG`] ====== PRZED sprawdzeniem npx =====" -ForegroundColor Magenta
        $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Sprawdzam czy npx jest dostepne..."
        Write-Host "`[DEBUG`] ====== WYKONUJE Get-Command npx =====" -ForegroundColor Magenta
        $npxCheck = Get-Command npx -ErrorAction SilentlyContinue
        Write-Host "`[DEBUG`] ====== PO Get-Command npx, npxCheck: $($npxCheck -ne $null) =====" -ForegroundColor Magenta
        if (-not $npxCheck) {
            Write-Host "`[DEBUG`] ====== npx NIE ZNALEZIONY, zwracam false =====" -ForegroundColor Magenta
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] npx nie jest dostepne w PATH"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Zainstaluj Node.js: https://nodejs.org/"
            Pop-Location
            return $false
        }
        Write-Host "`[DEBUG`] ====== npx ZNALEZIONY =====" -ForegroundColor Magenta
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] npx jest dostepne"
        
        Write-Host "`[DEBUG`] ====== PRZED npx supabase status =====" -ForegroundColor Magenta
        $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Wykonuje: npx supabase status"
        
        $originalErrorAction = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        
        try {
            $statusOutput = & npx supabase status 2>&1 | Where-Object { 
                $_ -notmatch "npm warn" -and 
                $_ -notmatch "Unknown user config" -and
                $_ -notmatch "System\.Management\.Automation" -and
                $_ -notmatch "^$"
            } | Where-Object { $_.ToString().Trim().Length -gt 0 }
            
            $statusExitCode = $LASTEXITCODE
            if ($statusExitCode -eq $null) {
                $statusExitCode = 0
            }
            Write-Host "`[DEBUG`] ====== PO npx supabase status, exit code: $statusExitCode =====" -ForegroundColor Magenta
            
            if ($statusOutput) {
                $status = ($statusOutput | Out-String).Trim()
            } else {
                $status = ""
            }
            Write-Host "`[DEBUG`] ====== Output length: $($status.Length) =====" -ForegroundColor Magenta
        } catch {
            Write-Host "`[DEBUG`] ====== BŁĄD podczas npx supabase status: $_ =====" -ForegroundColor Red
            $statusOutput = @()
            $statusExitCode = -1
            $status = ""
        } finally {
            $ErrorActionPreference = $originalErrorAction
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Exit code: $statusExitCode"
        Write-Host "[DEBUG] ====== ROZPOCZYNAM SPRAWDZANIE WZORCOW =====" -ForegroundColor Magenta
        
        if ($status -and $status.Length -gt 0) {
            $previewLength = [Math]::Min(500, $status.Length)
            $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Output (pierwsze $previewLength znakow): $($status.Substring(0, $previewLength))"
            $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Pełny output statusu Supabase:"
            $statusOutput | ForEach-Object { Write-Host "  $_" }
        } else {
            $null = Write-ColorOutput -ForegroundColor Yellow '[DEBUG] Output jest pusty'
        }
        
        Write-Host "[DEBUG] ====== PRZED SPRAWDZANIEM WZORCOW =====" -ForegroundColor Magenta
        Write-Host "[DEBUG] status jest null: $($null -eq $status)" -ForegroundColor Magenta
        Write-Host "[DEBUG] status length: $($status.Length)" -ForegroundColor Magenta
        
        $isRunning = $false
        $patternMatch = $false
        $match1 = $false
        $match2 = $false
        $match3 = $false
        $match4 = $false
        $match5 = $false
        $match6 = $false
        $match7 = $false
        $match8 = $false
        
        if ($status) {
            Write-Host "[DEBUG] ====== WEWNATRZ IF STATUS =====" -ForegroundColor Magenta
            try {
                $statusLower = $status.ToLower()
                Write-Host "[DEBUG] ====== PRZED MATCH1 =====" -ForegroundColor Magenta
                $match1 = $statusLower -match "supabase.*running"
                Write-Host "[DEBUG] ====== PO MATCH1, wynik: $match1 =====" -ForegroundColor Magenta
                $match2 = $statusLower -match "is running"
                Write-Host "[DEBUG] ====== PO MATCH2, wynik: $match2 =====" -ForegroundColor Magenta
                $match3 = $statusLower -match "already running"
                $match4 = $statusLower -match "api url"
                $match5 = $statusLower -match "database url"
                $match6 = $statusLower -match "local development setup is running"
                $match7 = $statusLower -match "graphql url"
                $match8 = $statusLower -match "studio url"
                Write-Host "[DEBUG] ====== WSZYSTKIE MATCHY ZAKONCZONE =====" -ForegroundColor Magenta
            } catch {
                Write-Host "[DEBUG] ====== BLAD W SPRAWDZANIU WZORCOW: $_ =====" -ForegroundColor Red
                Write-Host "[DEBUG] ====== StackTrace: $($_.ScriptStackTrace) =====" -ForegroundColor Red
            }
            
            Write-Host "[DEBUG] ====== PO BLOKU TRY-CATCH =====" -ForegroundColor Magenta
            Write-Host "[DEBUG] match1: $match1, match2: $match2, match6: $match6 =====" -ForegroundColor Magenta
            Write-Host "[DEBUG] ====== PRZED WYŚWIETLENIEM WYNIKÓW =====" -ForegroundColor Magenta
            Write-Host "[DEBUG] Wyniki sprawdzania wzorcow:" -ForegroundColor Cyan
            Write-Host "[DEBUG]   - supabase.*running: $match1" -ForegroundColor Cyan
            Write-Host "[DEBUG]   - is running: $match2" -ForegroundColor Cyan
            Write-Host "[DEBUG]   - already running: $match3" -ForegroundColor Cyan
            Write-Host "[DEBUG]   - api url: $match4" -ForegroundColor Cyan
            Write-Host "[DEBUG]   - database url: $match5" -ForegroundColor Cyan
            Write-Host "[DEBUG]   - local development setup is running: $match6" -ForegroundColor Cyan
            Write-Host "[DEBUG]   - graphql url: $match7" -ForegroundColor Cyan
            Write-Host "[DEBUG]   - studio url: $match8" -ForegroundColor Cyan
            Write-Host "[DEBUG] Exit code jest 0: $($statusExitCode -eq 0)" -ForegroundColor Cyan
            
            $patternMatch = $match1 -or $match2 -or $match3 -or $match4 -or $match5 -or $match6 -or $match7 -or $match8
            Write-Host "[DEBUG] Pattern match: $patternMatch" -ForegroundColor Cyan
            Write-Host "[DEBUG] ====== PRZED SPRAWDZENIEM WARUNKÓW =====" -ForegroundColor Magenta
            
            if ($statusExitCode -eq 0 -and $patternMatch) {
                Write-Host "[DEBUG] ====== WARUNKI PASUJĄ, USTAWIANIE isRunning =====" -ForegroundColor Magenta
                $isRunning = $true
                Write-Host "[OK] Wzorce pasuja, Supabase dziala" -ForegroundColor Green
            } elseif ($statusExitCode -eq 0) {
                Write-Host "[WARN] Exit code jest 0, ale wzorce nie pasuja" -ForegroundColor Yellow
            }
            Write-Host "[DEBUG] ====== PO SPRAWDZENIU WARUNKÓW, isRunning: $isRunning =====" -ForegroundColor Magenta
        } else {
            $null = Write-ColorOutput -ForegroundColor Yellow "`[DEBUG`] Status output jest pusty, sprawdzam porty jako backup"
        }
        
        if (-not $isRunning -and $statusExitCode -eq 0) {
            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Nie znaleziono wzorcow w outputcie, sprawdzam porty jako backup..."
            $postgresPortCheck = Test-Port -Port 54322
            $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] PostgreSQL port 54322: $postgresPortCheck"
            
            if ($postgresPortCheck) {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] PostgreSQL dziala na porcie 54322 (sprawdzenie portu)"
                $isRunning = $true
            }
        }
        
        Write-Host "[DEBUG] ====== PRZED SPRAWDZENIEM isRunning =====" -ForegroundColor Magenta
        Write-Host "[DEBUG] isRunning: $isRunning" -ForegroundColor Magenta
        $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] isRunning: $isRunning"
        
        if ($isRunning) {
            Write-Host "[DEBUG] ====== WEWNATRZ IF isRunning =====" -ForegroundColor Magenta
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Supabase juz dziala"
            
            Write-Host "[DEBUG] ====== SPRAWDZANIE PORTOW =====" -ForegroundColor Magenta
            $postgresRunning = Test-Port -Port 54322
            Write-Host "[DEBUG] PostgreSQL port 54322: $postgresRunning" -ForegroundColor Magenta
            if ($postgresRunning) {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] PostgreSQL dziala na porcie 54322"
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] PostgreSQL nie odpowiada na porcie 54322"
            }
            
            $redisRunning = Test-Port -Port 6379
            Write-Host "[DEBUG] Redis port 6379: $redisRunning" -ForegroundColor Magenta
            if ($redisRunning) {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Redis dziala na porcie 6379"
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Redis nie odpowiada na porcie 6379"
            }
            
            Write-Host "[DEBUG] ====== PRZED Pop-Location =====" -ForegroundColor Magenta
            Pop-Location
            Write-Host "[DEBUG] ====== PO Pop-Location, ZWRACAM TRUE =====" -ForegroundColor Magenta
            return $true
        } else {
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Supabase nie dziala lub status nieznany (isRunning: $isRunning)"
            
            if ($statusExitCode -eq 0) {
                $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Exit code jest 0, ale nie znaleziono wzorcow, sprawdzam porty..."
                $postgresPortCheck = Test-Port -Port 54322
                $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] PostgreSQL port 54322: $postgresPortCheck"
                
                if ($postgresPortCheck) {
                    $null = Write-ColorOutput -ForegroundColor Green "`[OK`] PostgreSQL dziala na porcie 54322, Supabase prawdopodobnie dziala"
                    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Zwracam true na podstawie sprawdzenia portu"
                    
                    $postgresRunning = Test-Port -Port 54322
                    if ($postgresRunning) {
                        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] PostgreSQL dziala na porcie 54322"
                    }
                    
                    $redisRunning = Test-Port -Port 6379
                    if ($redisRunning) {
                        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Redis dziala na porcie 6379"
                    } else {
                        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Redis nie odpowiada na porcie 6379"
                    }
                    
                    Pop-Location
                    return $true
                }
            }
            
            if ($status) {
                $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Status output:"
                $statusOutput | ForEach-Object { Write-Host "  $_" }
            }
        }
    } catch {
        $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Blad podczas sprawdzania statusu Supabase: $_"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam porty przed uruchomieniem..."
        
        $postgresPortCheck = Test-Port -Port 54322
        if ($postgresPortCheck) {
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] PostgreSQL dziala na porcie 54322 mimo bledu sprawdzania statusu"
            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Zwracam true na podstawie sprawdzenia portu"
            
            $postgresRunning = Test-Port -Port 54322
            if ($postgresRunning) {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] PostgreSQL dziala na porcie 54322"
            }
            
            $redisRunning = Test-Port -Port 6379
            if ($redisRunning) {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Redis dziala na porcie 6379"
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Redis nie odpowiada na porcie 6379"
            }
            
            Pop-Location
            return $true
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Przechodze do uruchamiania..."
    }
    
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Uruchamianie Supabase..."
    
    try {
        $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Sprawdzam czy jestem w katalogu projektu: $(Get-Location)"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Wykonuje: npx supabase start"
        
        $originalErrorActionStart = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        
        $output = & npx supabase start 2>&1 | Where-Object { $_ -notmatch "npm warn" -and $_ -notmatch "Unknown user config" }
        $exitCode = $LASTEXITCODE
        if ($exitCode -eq $null) {
            $exitCode = 0
        }
        $outputString = $output | Out-String
        
        $ErrorActionPreference = $originalErrorActionStart
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Exit code: $exitCode"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Output length: $($outputString.Length) znakow"
        
        if ($output) {
            $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Pełny output komendy supabase start:"
            $output | ForEach-Object { Write-Host "  $_" }
        }
        
        $isStarted = $false
        $patternMatch = $false
        
        if ($outputString) {
            $outputLower = $outputString.ToLower()
            $patternMatch = (
                $outputLower -match "supabase.*running" -or
                $outputLower -match "is running" -or
                $outputLower -match "already running" -or
                $outputLower -match "supabase local development setup is running" -or
                $outputLower -match "started supabase local development setup" -or
                $outputLower -match "api url" -or
                $outputLower -match "database url" -or
                $outputLower -match "studio url"
            )
            $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Pattern match w outputcie start: $patternMatch"
            $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] Exit code start: $exitCode"
            
            if ($exitCode -eq 0 -and $patternMatch) {
                $isStarted = $true
            }
        }
        
        if (-not $isStarted -and $exitCode -eq 0) {
            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Nie znaleziono wzorcow w outputcie start, sprawdzam porty jako backup..."
            Start-Sleep -Seconds 3
            $postgresPortCheck = Test-Port -Port 54322
            $null = Write-ColorOutput -ForegroundColor Cyan "`[DEBUG`] PostgreSQL port 54322 po starcie: $postgresPortCheck"
            
            if ($postgresPortCheck) {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] PostgreSQL dziala na porcie 54322 (sprawdzenie portu po starcie)"
                $isStarted = $true
            }
        }
        
        if ($isStarted) {
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Supabase uruchomiony lub juz dziala (exit code: $exitCode)"
            
            $postgresReady = Wait-ForPort -Port 54322 -ServiceName "PostgreSQL" -Timeout 45
            if ($postgresReady) {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] PostgreSQL gotowy"
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] PostgreSQL nie odpowiada w oczekiwanym czasie"
            }
            
            $redisReady = Wait-ForPort -Port 6379 -ServiceName "Redis" -Timeout 30
            if ($redisReady) {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Redis gotowy"
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Redis nie odpowiada w oczekiwanym czasie"
            }
            
            Pop-Location
            return $true
        } else {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic Supabase"
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Exit code: $exitCode"
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Pattern match: $patternMatch"
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] isStarted: $isStarted"
            
            $postgresPortCheck = Test-Port -Port 54322
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Ostatnie sprawdzenie portu PostgreSQL: $postgresPortCheck"
            
            if ($outputString) {
                $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
                $null = Write-ColorOutput -ForegroundColor Yellow "PEŁNY OUTPUT KOMENDY SUPABASE START:"
                $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
                $output | ForEach-Object { Write-Host $_ }
                $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Output komendy jest pusty"
            }
            
            $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam logi Supabase..."
            try {
                $originalErrorActionLogs = $ErrorActionPreference
                $ErrorActionPreference = "Continue"
                $logsOutput = & npx supabase logs 2>&1 | Where-Object { $_ -notmatch "npm warn" -and $_ -notmatch "Unknown user config" } | Select-Object -Last 20
                $ErrorActionPreference = $originalErrorActionLogs
                if ($logsOutput) {
                    $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
                    $null = Write-ColorOutput -ForegroundColor Yellow "OSTATNIE LOGI SUPABASE:"
                    $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
                    $logsOutput | ForEach-Object { Write-Host $_ }
                    $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
                }
            } catch {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Nie udalo sie pobrac logow Supabase: $_"
            }
            
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz logi: npx supabase logs"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy Docker Desktop jest uruchomiony"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy porty 54322 i 6379 sa wolne"
            
            Pop-Location
            return $false
        }
    } catch {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Blad podczas uruchamiania Supabase"
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Typ bledu: $($_.Exception.GetType().FullName)"
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Komunikat: $($_.Exception.Message)"
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] StackTrace: $($_.ScriptStackTrace)"
        
        if ($_.Exception.InnerException) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] InnerException: $($_.Exception.InnerException.Message)"
        }
        
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy npx jest dostepne: npx --version"
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy Supabase CLI jest zainstalowane"
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy Docker Desktop jest uruchomiony"
        
        Pop-Location
        return $false
    }
}

function Start-Redis {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzanie Redis..."
    
    if (Test-Port -Port 6379) {
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Redis juz dziala na porcie 6379"
        return $true
    }
    
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Uruchamianie Redis (Docker)..."
    
    try {
        docker run --name waw-redis -p 6379:6379 -d redis:7 2>&1 | Out-Null
        Start-Sleep -Seconds 3
        
        if (Test-Port -Port 6379) {
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Redis uruchomiony"
            return $true
        } else {
            $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Redis nie odpowiada, sprawdzam kontener..."
            $container = docker ps -a --filter "name=waw-redis" --format "{{.Names}}"
            if ($container -eq "waw-redis") {
                $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Kontener istnieje, uruchamiam..."
                docker start waw-redis | Out-Null
                Start-Sleep -Seconds 3
                if (Test-Port -Port 6379) {
                    $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Redis uruchomiony"
                    return $true
                }
            }
        }
    } catch {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Blad podczas uruchamiania Redis: $_"
        $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Upewnij sie ze Docker Desktop jest uruchomiony"
    }
    
    return $false
}

function Stop-Backend {
    Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Zatrzymywanie backendu..."
    
    $stopped = $false
    
    try {
        $connection = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
        if ($connection) {
            $pid = $connection.OwningProcess
            $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
            if ($proc -and $proc.ProcessName -eq "java") {
                Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Znaleziono proces Java na porcie 8080 (PID: $pid)"
                Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                Start-Sleep -Seconds 2
                $stopped = $true
                Write-ColorOutput -ForegroundColor Green "`[OK`] Backend zatrzymany"
            }
        }
    } catch {
        Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Nie udalo sie znalezc procesu na porcie 8080: $_"
    }
    
    if (-not $stopped) {
        Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Szukam procesow Java powiazanych z projektem..."
        $allJavaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
        
        foreach ($proc in $allJavaProcesses) {
            try {
                $commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($proc.Id)").CommandLine
                if ($commandLine -and ($commandLine -like "*bootRun*" -or $commandLine -like "*tbs*" -or $commandLine -like "*$BackendDir*")) {
                    Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Zatrzymywanie procesu Java (PID: $($proc.Id))"
                    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
                    $stopped = $true
                    Start-Sleep -Seconds 1
                }
            } catch {
            }
        }
        
        if ($stopped) {
            Start-Sleep -Seconds 1
            Write-ColorOutput -ForegroundColor Green "`[OK`] Backend zatrzymany"
        } else {
            Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Brak uruchomionych procesow backendu"
        }
    }
}

function Build-Backend {
    Write-Host "[DEBUG] ====== Build-Backend ROZPOCZYNA =====" -ForegroundColor Magenta
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Budowanie backendu..."
    
    $originalErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    
    Set-JavaEnvironment
    
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Przechodzenie do katalogu: $BackendDir"
    Push-Location $BackendDir
    try {
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam czy plik gradlew.bat istnieje..."
        
        if (-not (Test-Path ".\gradlew.bat")) {
            Write-Host "[DEBUG] ====== gradlew.bat NIE ISTNIEJE =====" -ForegroundColor Red
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Plik gradlew.bat nie istnieje w katalogu: $(Get-Location)"
            Pop-Location
            $ErrorActionPreference = $originalErrorAction
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Plik gradlew.bat istnieje"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Czyszczenie poprzednich buildow..."
        
        Write-Host "[DEBUG] ====== WYKONYWANIE gradlew clean =====" -ForegroundColor Magenta
        $cleanOutput = & .\gradlew.bat clean 2>&1 | Out-String
        Write-Host "[DEBUG] ====== PO gradlew clean, exit code: $LASTEXITCODE =====" -ForegroundColor Magenta
        
        if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne $null) {
            Write-Host "[DEBUG] ====== gradlew clean NIE POWIODŁO SIĘ =====" -ForegroundColor Red
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Gradle clean nie powiodl sie (Exit code: $LASTEXITCODE)"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Output:"
            $cleanOutput | Write-Host
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Clean pomyslny"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Kompilowanie backendu..."
        
        Write-Host "[DEBUG] ====== WYKONYWANIE gradlew build =====" -ForegroundColor Magenta
        $buildOutput = & .\gradlew.bat build -x test 2>&1 | Out-String
        Write-Host "[DEBUG] ====== PO gradlew build, exit code: $LASTEXITCODE =====" -ForegroundColor Magenta
        
        if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne $null) {
            Write-Host "[DEBUG] ====== gradlew build NIE POWIODŁO SIĘ =====" -ForegroundColor Red
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Gradle build nie powiodl sie (Exit code: $LASTEXITCODE)"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Output:"
            $buildOutput | Write-Host
            Pop-Location
            return $false
        }
        
        Write-Host "[DEBUG] ====== SPRAWDZANIE BUILD SUCCESSFUL =====" -ForegroundColor Magenta
        $hasBuildSuccessful = $buildOutput -match "BUILD SUCCESSFUL"
        Write-Host "[DEBUG] ====== BUILD SUCCESSFUL znaleziony: $hasBuildSuccessful =====" -ForegroundColor Magenta
        
        if ($hasBuildSuccessful) {
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Backend zbudowany pomyslnie"
            Pop-Location
            return $true
        } else {
            Write-Host "[DEBUG] ====== BUILD SUCCESSFUL NIE ZNALEZIONY =====" -ForegroundColor Red
            Write-Host "[DEBUG] ====== Długość outputu: $($buildOutput.Length) =====" -ForegroundColor Yellow
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Blad podczas budowania backendu - nie znaleziono 'BUILD SUCCESSFUL'"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Output build:"
            $buildOutput | Write-Host
            Pop-Location
            return $false
        }
    } catch {
        Write-Host "[DEBUG] ====== BLAD W Build-Backend =====" -ForegroundColor Red
        Write-Host "[DEBUG] Error: $_" -ForegroundColor Red
        Write-Host "[DEBUG] Exception Type: $($_.Exception.GetType().FullName)" -ForegroundColor Red
        Write-Host "[DEBUG] Exception Message: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "[DEBUG] StackTrace: $($_.ScriptStackTrace)" -ForegroundColor Red
        if ($_.Exception.InnerException) {
            Write-Host "[DEBUG] InnerException: $($_.Exception.InnerException.Message)" -ForegroundColor Red
        }
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Blad podczas budowania: $_"
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Szczegoly: $($_.Exception.Message)"
        Pop-Location
        $ErrorActionPreference = $originalErrorAction
        return $false
    } finally {
        $ErrorActionPreference = $originalErrorAction
    }
}

function Wait-ForApplicationStart {
    param(
        [int]$Timeout = 90,
        [string]$LogFile
    )
    
    $elapsed = 0
    $startedPattern = "Started TbsApplication"
    $errorPatterns = @(
        "APPLICATION FAILED TO START",
        "Error starting ApplicationContext",
        "Failed to start bean",
        "Unable to start embedded Tomcat",
        "Cannot assign requested address",
        "Address already in use",
        "Connection refused",
        "HikariPool.*Failed",
        "Unable to obtain connection",
        "Failed to validate connection"
    )
    
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Monitorowanie logow aplikacji..."
    
    $maxWaitForLogFile = 30
    $logFileWaitElapsed = 0
    
    while ($logFileWaitElapsed -lt $maxWaitForLogFile) {
        if (Test-Path $LogFile) {
            break
        }
        Start-Sleep -Seconds 1
        $logFileWaitElapsed++
        Write-Host -NoNewline "."
    }
    
    if (-not (Test-Path $LogFile)) {
        Write-Host ""
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Plik logow nie zostal utworzony: $LogFile"
        return $false
    }
    
    Write-Host ""
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Monitorowanie logow (timeout: $Timeout sekund)..."
    
    while ($elapsed -lt $Timeout) {
        try {
            if (-not (Test-Path $LogFile)) {
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Plik logow zostal usuniety: $LogFile"
                return $false
            }
            
            $logContent = Get-Content $LogFile -Tail 200 -ErrorAction SilentlyContinue | Out-String
            
            if ($logContent) {
                foreach ($pattern in $errorPatterns) {
                    if ($logContent -match $pattern) {
                        Write-Host ""
                        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Wykryto blad krytyczny w logach!"
                        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Wzorzec: $pattern"
                        
                        $errorLines = Get-Content $LogFile -Tail 50 -ErrorAction SilentlyContinue | 
                            Select-String -Pattern $pattern -Context 5,5
                        
                        if ($errorLines) {
                            $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
                            $null = Write-ColorOutput -ForegroundColor Yellow "WYSELEKCJONOWANE LOGI Z BLEDEM:"
                            $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
                            $errorLines | ForEach-Object { Write-Host $_ }
                        }
                        
                        return $false
                    }
                }
                
                if ($logContent -match $startedPattern) {
                    Write-Host ""
                    $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Aplikacja uruchomiona (z logow)"
                    return $true
                }
            }
        } catch {
            $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Blad podczas czytania logow: $_"
        }
        
        Start-Sleep -Seconds 2
        $elapsed += 2
        Write-Host -NoNewline "."
    }
    
    Write-Host ""
    $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Timeout oczekiwania na potwierdzenie startu z logow ($Timeout sekund)"
    
    if (Test-Path $LogFile) {
        $lastLogs = Get-Content $LogFile -Tail 100 -ErrorAction SilentlyContinue | Out-String
        
        foreach ($pattern in $errorPatterns) {
            if ($lastLogs -match $pattern) {
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Wykryto blad krytyczny w ostatnich logach: $pattern"
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Ostatnie logi:"
                Get-Content $LogFile -Tail 30 | Write-Host
                return $false
            }
        }
        
        if ($lastLogs -match $startedPattern) {
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Aplikacja uruchomiona (z logow - opoznienie wykrycia)"
            return $true
        } else {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie znaleziono potwierdzenia startu aplikacji w logach"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Ostatnie logi:"
            Get-Content $LogFile -Tail 30 | Write-Host
            return $false
        }
    }
    
    $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Plik logow nie istnieje"
    return $false
}

function Test-ApplicationHealth {
    param([int]$MaxRetries = 5)
    
    $retries = 0
    while ($retries -lt $MaxRetries) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs" -Method Get -TimeoutSec 5 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Aplikacja odpowiada na requesty HTTP"
                return $true
            }
        } catch {
            $retries++
            $errorMsg = $_.Exception.Message
            
            if ($retries -lt $MaxRetries) {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Próba $retries/$MaxRetries - Błąd: $errorMsg"
                Start-Sleep -Seconds 2
            } else {
                $probyInfo = "$MaxRetries probow"
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Wszystkie proby nieudane ($probyInfo)"
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Ostatni blad: $errorMsg"
                
                if ($errorMsg -match "Connection refused" -or $errorMsg -match "Unable to connect") {
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Aplikacja może nie być w pełni uruchomiona lub port 8080 jest zajęty przez inny proces"
                } elseif ($errorMsg -match "Timeout") {
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Aplikacja odpowiada zbyt wolno lub nie odpowiada w ogóle"
                } elseif ($errorMsg -match "404") {
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Aplikacja działa, ale endpoint /v3/api-docs nie istnieje"
                } elseif ($errorMsg -match "500") {
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Aplikacja zwraca blad serwera - sprawdz logi"
                }
            }
        }
    }
    return $false
}

function Get-JavaProcessForBackend {
    $allJavaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
    
    foreach ($proc in $allJavaProcesses) {
        try {
            $commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($proc.Id)").CommandLine
            if ($commandLine -and (
                $commandLine -like "*bootRun*" -or 
                $commandLine -like "*tbs*" -or 
                $commandLine -like "*$BackendDir*" -or
                $commandLine -like "*TbsApplication*"
            )) {
                return $proc
            }
        } catch {
        }
    }
    return $null
}

function Start-Backend {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Uruchamianie backendu..."
    
    if (Test-Port -Port 8080) {
        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Port 8080 jest zajety"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Zatrzymuje istniejacy proces..."
        Stop-Backend
        Start-Sleep -Seconds 3
        
        if (Test-Port -Port 8080) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 8080 nadal jest zajety po zatrzymaniu procesu"
            return $false
        }
    }
    
    Set-JavaEnvironment
    
    Push-Location $BackendDir
    try {
        if (Test-Path $LogFile) {
            Clear-Content $LogFile -ErrorAction SilentlyContinue
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Uruchamianie Spring Boot..."
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam czy plik gradlew.bat istnieje..."
        
        if (-not (Test-Path ".\gradlew.bat")) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Plik gradlew.bat nie istnieje w katalogu: $BackendDir"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Upewnij sie ze jestes w prawidlowym katalogu"
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Plik gradlew.bat istnieje"
        
        try {
            $gradleProcess = Start-Process -FilePath ".\gradlew.bat" -ArgumentList "bootRun" -PassThru -NoNewWindow -ErrorAction Stop
        } catch {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic procesu Gradle"
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Szczegoly: $($_.Exception.Message)"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy Gradle jest poprawnie skonfigurowany"
            Pop-Location
            return $false
        }
        
        if (-not $gradleProcess) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Start-Process zwrocil null - proces nie zostal uruchomiony"
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Proces Gradle uruchomiony (PID: $($gradleProcess.Id))"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Czekam 2 sekundy i sprawdzam czy proces faktycznie dziala..."
        Start-Sleep -Seconds 2
        
        try {
            $verifyProcess = Get-Process -Id $gradleProcess.Id -ErrorAction Stop
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces Gradle istnieje (PID: $($verifyProcess.Id))"
        } catch {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces Gradle nie istnieje! (PID: $($gradleProcess.Id))"
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces mogl sie zakonczyc zaraz po uruchomieniu"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[HINT`] Sprawdz czy gradlew.bat jest wykonywalny i czy jest w PATH"
            Pop-Location
            return $false
        }
        
        if ($gradleProcess.HasExited) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces Gradle zakonczyl sie zaraz po uruchomieniu!"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Exit code: $($gradleProcess.ExitCode)"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Sprawdzam logi..."
            
            if (Test-Path $LogFile) {
                $lastLogs = Get-Content $LogFile -Tail 50 -ErrorAction SilentlyContinue
                if ($lastLogs) {
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Ostatnie logi:"
                    $lastLogs | Write-Host
                }
            }
            
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces Gradle dziala (PID: $($gradleProcess.Id))"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Oczekiwanie na start procesu Java (max 30 sekund)..."
        
        $javaProcess = $null
        $maxWaitForJava = 30
        $javaWaitElapsed = 0
        
        while ($javaWaitElapsed -lt $maxWaitForJava) {
            if ($gradleProcess.HasExited) {
                Write-Host ""
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces Gradle zakonczyl sie podczas oczekiwania na proces Java!"
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Exit code: $($gradleProcess.ExitCode)"
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Sprawdzam logi..."
                
                if (Test-Path $LogFile) {
                    $lastLogs = Get-Content $LogFile -Tail 50 -ErrorAction SilentlyContinue
                    if ($lastLogs) {
                        $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Ostatnie logi:"
                        $lastLogs | Write-Host
                    }
                }
                
                Pop-Location
                return $false
            }
            
            $javaProcess = Get-JavaProcessForBackend
            if ($javaProcess) {
                Write-Host ""
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Znaleziono proces Java (PID: $($javaProcess.Id))"
                
                try {
                    $javaCommandLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($javaProcess.Id)").CommandLine
                    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Command line procesu Java:"
                    $null = Write-ColorOutput -ForegroundColor Cyan "  $javaCommandLine"
                } catch {
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Nie mozna pobrac command line procesu Java"
                }
                
                break
            }
            Start-Sleep -Seconds 2
            $javaWaitElapsed += 2
            Write-Host -NoNewline "."
        }
        
        Write-Host ""
        
        if (-not $javaProcess) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie znaleziono procesu Java uruchomionego przez aplikacje (timeout: $maxWaitForJava sekund)"
            
            if ($gradleProcess.HasExited) {
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces Gradle zakonczyl sie (Exit code: $($gradleProcess.ExitCode))"
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Proces Gradle nadal dziala (PID: $($gradleProcess.Id))"
            }
            
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Sprawdzam logi..."
            
            if (Test-Path $LogFile) {
                $lastLogs = Get-Content $LogFile -Tail 50 -ErrorAction SilentlyContinue
                if ($lastLogs) {
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Ostatnie logi:"
                    $lastLogs | Write-Host
                }
            }
            
            if (-not $gradleProcess.HasExited) {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Zatrzymywanie procesu Gradle..."
                Stop-Process -Id $gradleProcess.Id -Force -ErrorAction SilentlyContinue
            }
            
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam czy proces Gradle nadal dziala (powinien dzialac z procesem Java)..."
        if ($gradleProcess.HasExited) {
            $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Proces Gradle zakonczyl sie, ale proces Java dziala (to moze byc OK dla bootRun)"
        } else {
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces Gradle nadal dziala (PID: $($gradleProcess.Id))"
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Oczekiwanie na otwarcie portu 8080..."
        $portReady = Wait-ForPort -Port 8080 -Timeout 60 -ServiceName "Backend"
        
        if (-not $portReady) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 8080 nie zostal otwarty w oczekiwanym czasie (60 sekund)"
            
            if (-not $javaProcess.HasExited) {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Proces Java nadal dziala (PID: $($javaProcess.Id))"
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Sprawdzam logi aby znalezc przyczyne..."
            } else {
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces Java zakonczyl sie nieoczekiwanie (PID: $($javaProcess.Id))"
            }
            
            Get-Logs
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Port 8080 otwarty, sprawdzam czy aplikacja faktycznie dziala..."
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam czy proces Java nadal dziala..."
        
        if ($javaProcess.HasExited) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces Java zakonczyl sie zaraz po otwarciu portu! (PID: $($javaProcess.Id))"
            Get-Logs
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces Java nadal dziala (PID: $($javaProcess.Id))"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Czekam 5 sekund na pelna inicjalizacja aplikacji..."
        Start-Sleep -Seconds 5
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Po 5 sekundach - sprawdzam czy proces Java nadal dziala..."
        if ($javaProcess.HasExited) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces Java zakonczyl sie podczas oczekiwania! (PID: $($javaProcess.Id))"
            Get-Logs
            Pop-Location
            return $false
        }
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces Java nadal dziala (PID: $($javaProcess.Id))"
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam logi pod katem bledow..."
        $hasCriticalErrors = $false
        
        if (Test-Path $LogFile) {
            $logContent = Get-Content $LogFile -Tail 100 -ErrorAction SilentlyContinue | Out-String
            $criticalErrorPatterns = @(
                "APPLICATION FAILED TO START",
                "Error starting ApplicationContext",
                "Unable to start embedded Tomcat",
                "Cannot assign requested address"
            )
            
            foreach ($pattern in $criticalErrorPatterns) {
                if ($logContent -match $pattern) {
                    $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Wykryto blad krytyczny w logach: $pattern"
                    $hasCriticalErrors = $true
                    
                    $errorLines = Get-Content $LogFile -Tail 50 -ErrorAction SilentlyContinue | 
                        Select-String -Pattern $pattern -Context 3,3
                    
                    if ($errorLines) {
                        $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
                        $null = Write-ColorOutput -ForegroundColor Yellow "WYSELEKCJONOWANE LOGI Z BLEDEM:"
                        $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
                        $errorLines | ForEach-Object { Write-Host $_ }
                        $null = Write-ColorOutput -ForegroundColor Yellow "================================================================"
                    }
                    break
                }
            }
        }
        
        if ($hasCriticalErrors) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Aplikacja nie uruchomila sie z powodu bledow krytycznych"
            Get-Logs
            Pop-Location
            return $false
        }
        
        if ($javaProcess.HasExited) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces Java zakonczyl sie nieoczekiwanie (PID: $($javaProcess.Id))"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Sprawdzam logi..."
            Get-Logs
            Pop-Location
            return $false
        }
        
        if (-not (Test-Port -Port 8080)) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 8080 zostal zamkniety"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Sprawdzam logi..."
            Get-Logs
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam czy aplikacja odpowiada na requesty HTTP..."
        $healthOk = Test-ApplicationHealth -MaxRetries 10
        
        if (-not $healthOk) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Aplikacja nie odpowiada na requesty HTTP"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Port 8080 jest otwarty, ale aplikacja nie odpowiada"
            
            if (-not $javaProcess.HasExited) {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Proces Java nadal dziala (PID: $($javaProcess.Id))"
            } else {
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces Java zakonczyl sie (PID: $($javaProcess.Id))"
            }
            
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Sprawdzam logi..."
            Get-Logs
            Pop-Location
            return $false
        }
        
        if ($javaProcess.HasExited) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces Java zakonczyl sie nieoczekiwanie podczas testu health"
            Get-Logs
            Pop-Location
            return $false
        }
        
        if (-not (Test-Port -Port 8080)) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 8080 zostal zamkniety podczas testu health"
            Get-Logs
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Finalne sprawdzenie (dokladnie jak w opcji status)..."
        Start-Sleep -Seconds 3
        
        $finalPortCheck = Test-Port -Port 8080
        $finalProcessCheck = $false
        
        try {
            $finalCheck = Get-Process -Id $javaProcess.Id -ErrorAction Stop
            $finalProcessCheck = $true
        } catch {
            $finalProcessCheck = $false
        }
        
        if (-not $finalPortCheck) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Finalne sprawdzenie nie przeszlo:"
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`]   - Port 8080 nie jest otwarty (jak w opcji status)"
            Get-Logs
            Pop-Location
            return $false
        }
        
        if (-not $finalProcessCheck) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Finalne sprawdzenie nie przeszlo:"
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`]   - Proces Java nie istnieje (PID: $($javaProcess.Id))"
            Get-Logs
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Dodatkowe sprawdzenie HTTP (czy aplikacja nadal odpowiada)..."
        Start-Sleep -Seconds 2
        
        $finalHttpCheck = $false
        try {
            $finalResponse = Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs" -Method Get -TimeoutSec 5 -ErrorAction Stop
            if ($finalResponse.StatusCode -eq 200) {
                $finalHttpCheck = $true
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Aplikacja odpowiada na requesty HTTP"
            }
        } catch {
            $finalHttpCheck = $false
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Aplikacja nie odpowiada na requesty HTTP: $($_.Exception.Message)"
        }
        
        if (-not $finalHttpCheck) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Finalne sprawdzenie HTTP nie przeszlo"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Port 8080 jest otwarty, ale aplikacja nie odpowiada na requesty"
            Get-Logs
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam ponownie port (dokladnie jak w opcji status)..."
        Start-Sleep -Seconds 2
        
        $veryFinalPortCheck = Test-Port -Port 8080
        if (-not $veryFinalPortCheck) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 8080 zostal zamkniety podczas ostatniego sprawdzenia"
            Get-Logs
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Ostatnie sprawdzenie procesu Java (przed zwroceniem true)..."
        Start-Sleep -Seconds 2
        
        $finalJavaCheck = $false
        $finalJavaPid = $null
        try {
            $finalJavaCheckProcess = Get-Process -Id $javaProcess.Id -ErrorAction Stop
            $finalJavaCheck = $true
            $finalJavaPid = $finalJavaCheckProcess.Id
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces Java nadal istnieje (PID: $finalJavaPid)"
        } catch {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Proces Java nie istnieje! (PID: $($javaProcess.Id))"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Sprawdzam czy inny proces Java dziala na porcie 8080..."
            
            try {
                $connection = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
                if ($connection) {
                    $pidFromPort = $connection.OwningProcess
                    $procFromPort = Get-Process -Id $pidFromPort -ErrorAction SilentlyContinue
                    if ($procFromPort -and $procFromPort.ProcessName -eq "java") {
                        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Znaleziono inny proces Java na porcie 8080 (PID: $pidFromPort)"
                        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Oryginalny proces Java (PID: $($javaProcess.Id)) nie istnieje"
                    }
                }
            } catch {
            }
            
            Get-Logs
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzam czy port 8080 jest nadal powiazany z procesem Java..."
        Start-Sleep -Seconds 1
        
        try {
            $finalConnection = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
            if ($finalConnection) {
                $finalPidFromPort = $finalConnection.OwningProcess
                $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Port 8080 powiazany z procesem PID: $finalPidFromPort"
                
                if ($finalPidFromPort -eq $finalJavaPid) {
                    $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 8080 powiazany z prawidlowym procesem Java (PID: $finalJavaPid)"
                } else {
                    $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Port 8080 powiazany z innym procesem (PID: $finalPidFromPort, oczekiwany: $finalJavaPid)"
                    $procFromPortFinal = Get-Process -Id $finalPidFromPort -ErrorAction SilentlyContinue
                    if ($procFromPortFinal -and $procFromPortFinal.ProcessName -eq "java") {
                        $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] To tez jest proces Java, ale inny PID"
                    } else {
                        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Port 8080 powiazany z procesem innego typu: $($procFromPortFinal.ProcessName)"
                        Get-Logs
                        Pop-Location
                        return $false
                    }
                }
            } else {
                $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie mozna znalezc polaczenia na porcie 8080"
                Get-Logs
                Pop-Location
                return $false
            }
        } catch {
            $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Nie mozna sprawdzic polaczenia na porcie 8080: $_"
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Wykonuje ostatnie sprawdzenie HTTP przed zwroceniem true..."
        Start-Sleep -Seconds 2
        
        $veryFinalHttpCheck = $false
        try {
            $veryFinalResponse = Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs" -Method Get -TimeoutSec 5 -ErrorAction Stop
            if ($veryFinalResponse.StatusCode -eq 200) {
                $veryFinalHttpCheck = $true
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Ostatnie sprawdzenie HTTP: OK"
            }
        } catch {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Ostatnie sprawdzenie HTTP nieudane: $($_.Exception.Message)"
        }
        
        if (-not $veryFinalHttpCheck) {
            $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Aplikacja nie odpowiada na ostatnie sprawdzenie HTTP"
            $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Port i proces sa OK, ale aplikacja nie odpowiada"
            Get-Logs
            Pop-Location
            return $false
        }
        
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Wszystkie sprawdzenia przeszly pomyslnie!"
        $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Podsumowanie:"
        $null = Write-ColorOutput -ForegroundColor Cyan "  - Port 8080: otwarty"
        $null = Write-ColorOutput -ForegroundColor Cyan "  - Proces Java: dziala (PID: $finalJavaPid)"
        $null = Write-ColorOutput -ForegroundColor Cyan "  - HTTP response: OK"
        $null = Write-ColorOutput -ForegroundColor Cyan "  - Polaczenie port-proces: OK"
        
        Write-Host ""
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Backend uruchomiony poprawnie!"
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] API: http://localhost:8080"
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Swagger UI: http://localhost:8080/swagger-ui/index.html"
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Proces Java dziala (PID: $finalJavaPid)"
        $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Port 8080 otwarty i aplikacja odpowiada (potwierdzone przez wielokrotne testy)"
        Pop-Location
        return $true
    } catch {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nieoczekiwany blad podczas uruchamiania: $_"
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Szczegoly: $($_.Exception.Message)"
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] StackTrace: $($_.ScriptStackTrace)"
        Get-Logs
        Pop-Location
        return $false
    }
}

function Get-Logs {
    Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Pobieranie ostatnich logow..."
    
    if (Test-Path $LogFile) {
        Write-ColorOutput -ForegroundColor Yellow "================================================================"
        Write-ColorOutput -ForegroundColor Yellow "OSTATNIE LOGI BACKENDU:"
        Write-ColorOutput -ForegroundColor Yellow "================================================================"
        Get-Content $LogFile -Tail 50 | Write-Host
        Write-ColorOutput -ForegroundColor Yellow "================================================================"
    } else {
        Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Plik logow nie istnieje: $LogFile"
        
        Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Szukam innych plikow logow..."
        $allLogs = Get-ChildItem -Path $BackendDir -Recurse -Filter "*.log" -ErrorAction SilentlyContinue
        if ($allLogs) {
            foreach ($log in $allLogs) {
                Write-ColorOutput -ForegroundColor Cyan "`nLog: $($log.FullName)"
                Get-Content $log.FullName -Tail 30 | Write-Host
            }
        } else {
            Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Brak plikow logow"
        }
    }
}

function Apply-Migrations {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Stosowanie migracji bazy danych..."
    
    Push-Location $ProjectRoot
    try {
        $originalErrorActionMigrations = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $output = npx supabase db reset 2>&1 | Where-Object { $_ -notmatch "npm warn" -and $_ -notmatch "Unknown user config" } | Out-String
        $ErrorActionPreference = $originalErrorActionMigrations
        
        if ($output -match "Applying migration") {
            $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Migracje zastosowane pomyslnie"
            Pop-Location
            return $true
        } else {
            $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Nie znaleziono nowych migracji lub migracje juz zastosowane"
            Pop-Location
            return $true
        }
    } catch {
        $null = Write-ColorOutput -ForegroundColor Red "`[ERROR`] Blad podczas stosowania migracji: $_"
        Pop-Location
        return $false
    }
}

function Clear-RedisCache {
    $null = Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Czyszczenie cache Redis..."
    
    $maxRetries = 5
    $retryDelay = 2
    $retries = 0
    $success = $false
    
    while ($retries -lt $maxRetries -and -not $success) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/rankings/cache" -Method Delete -TimeoutSec 5 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                $null = Write-ColorOutput -ForegroundColor Green "`[OK`] Cache Redis wyczyszczony pomyslnie"
                $success = $true
                return $true
            }
        } catch {
            $retries++
            if ($retries -lt $maxRetries) {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Próba $retries/$maxRetries - Aplikacja może nie być jeszcze gotowa, czekam $retryDelay sekund..."
                Start-Sleep -Seconds $retryDelay
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Nie udalo sie wyczyscic cache Redis (aplikacja może nie być jeszcze uruchomiona)"
                $null = Write-ColorOutput -ForegroundColor Yellow "`[INFO`] Cache zostanie automatycznie odswiezony przy pierwszym zapytaniu"
                return $false
            }
        }
    }
    
    return $false
}

function Show-Status {
    Write-ColorOutput -ForegroundColor Cyan "`[INFO`] Sprawdzanie statusu serwisow..."
    Write-Host ""
    
    $allRunning = $true
    
    Write-ColorOutput -ForegroundColor Cyan "--- Supabase (PostgreSQL) ---"
    try {
        Push-Location $ProjectRoot
        $originalErrorActionStatus = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $statusOutput = npx supabase status 2>&1 | Where-Object { $_ -notmatch "npm warn" -and $_ -notmatch "Unknown user config" }
        $ErrorActionPreference = $originalErrorActionStatus
        Pop-Location
        
        $status = $statusOutput | Out-String
        
        if ($status -match "started" -or $status -match "running") {
            if (Test-Port -Port 54322) {
                Write-ColorOutput -ForegroundColor Green "  [OK] Supabase dziala"
                Write-ColorOutput -ForegroundColor Green "  [OK] PostgreSQL dziala na porcie 54322"
            } else {
                Write-ColorOutput -ForegroundColor Yellow "  [WARN] Supabase uruchomiony, ale PostgreSQL nie odpowiada na porcie 54322"
                $allRunning = $false
            }
        } else {
            if (Test-Port -Port 54322) {
                Write-ColorOutput -ForegroundColor Green "  [OK] PostgreSQL dziala na porcie 54322 (status nieznany)"
            } else {
                Write-ColorOutput -ForegroundColor Red "  [STOP] Supabase nie dziala"
                $allRunning = $false
            }
        }
    } catch {
        if (Test-Port -Port 54322) {
            Write-ColorOutput -ForegroundColor Green "  [OK] PostgreSQL dziala na porcie 54322 (nie udalo sie sprawdzic statusu Supabase)"
        } else {
            Write-ColorOutput -ForegroundColor Red "  [STOP] Nie udalo sie sprawdzic statusu Supabase"
            $allRunning = $false
        }
    }
    
    Write-Host ""
    Write-ColorOutput -ForegroundColor Cyan "--- Redis ---"
    if (Test-Port -Port 6379) {
        Write-ColorOutput -ForegroundColor Green "  [OK] Redis dziala na porcie 6379"
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
    Write-ColorOutput -ForegroundColor Cyan "--- Java ---"
    $originalErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    
    try {
            if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
                $javaVersionOutput = & "$env:JAVA_HOME\bin\java.exe" -version 2>&1
                $javaVersionLine = $javaVersionOutput | Where-Object { $_ -match "version" } | Select-Object -First 1
                if ($javaVersionLine -and $javaVersionLine -match "21") {
                    Write-ColorOutput -ForegroundColor Green "  [OK] Java 21 znaleziona: $env:JAVA_HOME"
                    Write-ColorOutput -ForegroundColor Cyan "  [INFO] Wersja: $javaVersionLine"
                } else {
                    Write-ColorOutput -ForegroundColor Yellow "  [WARN] Java znaleziona, ale nie wersja 21: $env:JAVA_HOME"
                    $allRunning = $false
                }
            } else {
                $javaHome = $null
                foreach ($path in $JAVA_HOME_PATHS) {
                    if (Test-Path $path) {
                        $jdkDirs = Get-ChildItem -Path $path -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -like "jdk-21*" } | Sort-Object Name -Descending
                        if ($jdkDirs) {
                            $javaHome = $jdkDirs[0].FullName
                            break
                        }
                    }
                }
                
                if ($javaHome) {
                    $javaVersionOutput = & "$javaHome\bin\java.exe" -version 2>&1
                    $javaVersionLine = $javaVersionOutput | Where-Object { $_ -match "version" } | Select-Object -First 1
                    if ($javaVersionLine -and $javaVersionLine -match "21") {
                        Write-ColorOutput -ForegroundColor Green "  [OK] Java 21 znaleziona: $javaHome"
                        Write-ColorOutput -ForegroundColor Cyan "  [INFO] Wersja: $javaVersionLine"
                    } else {
                        Write-ColorOutput -ForegroundColor Yellow "  [WARN] Java znaleziona, ale nie wersja 21"
                        $allRunning = $false
                    }
                } else {
                    Write-ColorOutput -ForegroundColor Red "  [STOP] Java 21 nie znaleziona"
                    $allRunning = $false
                }
            }
    } catch {
        Write-ColorOutput -ForegroundColor Yellow "  [WARN] Nie udalo sie sprawdzic Java: $_"
    } finally {
        $ErrorActionPreference = $originalErrorAction
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
        Write-ColorOutput -ForegroundColor Cyan "`[HINT`] Uruchom: .\run-backend.ps1 start"
    }
}

Write-ColorOutput -ForegroundColor Cyan "===================================================================="
Write-ColorOutput -ForegroundColor Cyan "World at War: Turn-Based Strategy - Backend Runner"
Write-ColorOutput -ForegroundColor Cyan "===================================================================="
Write-Host ""

switch ($Action) {
    "start" {
        Write-ColorOutput -ForegroundColor Cyan "[INICJUJE START]"
        Write-Host ""
        
        Write-Host "[DEBUG] ====== WYWOŁYWANIE Start-Supabase =====" -ForegroundColor Magenta
        $supabaseStarted = Start-Supabase
        Write-Host "[DEBUG] ====== PO Start-Supabase, wynik: $supabaseStarted =====" -ForegroundColor Magenta
        if (-not $supabaseStarted) {
            Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic Supabase"
            exit 1
        }
        
        Write-Host "[DEBUG] ====== WYWOŁYWANIE Start-Redis =====" -ForegroundColor Magenta
        $redisStarted = Start-Redis
        Write-Host "[DEBUG] ====== PO Start-Redis, wynik: $redisStarted =====" -ForegroundColor Magenta
        if (-not $redisStarted) {
            Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Redis nie dziala, kontynuuje..."
        }
        
        Write-Host "[DEBUG] ====== WYWOŁYWANIE Build-Backend =====" -ForegroundColor Magenta
        $buildSuccess = Build-Backend
        Write-Host "[DEBUG] ====== PO Build-Backend, wynik: $buildSuccess =====" -ForegroundColor Magenta
        if (-not $buildSuccess) {
            exit 1
        }
        
        Write-Host "[DEBUG] ====== WYWOŁYWANIE Start-Backend =====" -ForegroundColor Magenta
        $backendStarted = Start-Backend
        Write-Host "[DEBUG] ====== PO Start-Backend, wynik: $backendStarted =====" -ForegroundColor Magenta
        if (-not $backendStarted) {
            exit 1
        }
        
        Write-Host "[DEBUG] ====== WYWOŁYWANIE Clear-RedisCache =====" -ForegroundColor Magenta
        Clear-RedisCache
        Write-Host "[DEBUG] ====== PO Clear-RedisCache =====" -ForegroundColor Magenta
        
        Write-Host ""
        Write-ColorOutput -ForegroundColor Green "===================================================================="
        Write-ColorOutput -ForegroundColor Green "Backend uruchomiony pomyslnie!"
        Write-ColorOutput -ForegroundColor Green "===================================================================="
        Write-ColorOutput -ForegroundColor Cyan "  API: http://localhost:8080"
        Write-ColorOutput -ForegroundColor Cyan "  Swagger UI: http://localhost:8080/swagger-ui/index.html"
        Write-ColorOutput -ForegroundColor Cyan "  OpenAPI Docs: http://localhost:8080/v3/api-docs"
    }
    
    "restart" {
        Write-ColorOutput -ForegroundColor Cyan "[INICJUJE RESTART]"
        Write-Host ""
        
        Stop-Backend
        
        $supabaseStarted = Start-Supabase
        if (-not $supabaseStarted) {
            Write-ColorOutput -ForegroundColor Red "`[ERROR`] Nie udalo sie uruchomic Supabase"
            exit 1
        }
        
        $applyMigrations = Apply-Migrations
        if (-not $applyMigrations) {
            Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Problemy z migracjami, kontynuuje..."
        }
        
        $redisStarted = Start-Redis
        if (-not $redisStarted) {
            Write-ColorOutput -ForegroundColor Yellow "`[WARN`] Redis nie dziala, kontynuuje..."
        }
        
        $buildSuccess = Build-Backend
        if (-not $buildSuccess) {
            Get-Logs
            exit 1
        }
        
        $backendStarted = Start-Backend
        if (-not $backendStarted) {
            Get-Logs
            exit 1
        }
        
        Write-Host "[DEBUG] ====== WYWOŁYWANIE Clear-RedisCache =====" -ForegroundColor Magenta
        Clear-RedisCache
        Write-Host "[DEBUG] ====== PO Clear-RedisCache =====" -ForegroundColor Magenta
        
        Write-Host ""
        Write-ColorOutput -ForegroundColor Green "===================================================================="
        Write-ColorOutput -ForegroundColor Green "Backend zrestartowany pomyslnie!"
        Write-ColorOutput -ForegroundColor Green "===================================================================="
        Write-ColorOutput -ForegroundColor Cyan "  API: http://localhost:8080"
        Write-ColorOutput -ForegroundColor Cyan "  Swagger UI: http://localhost:8080/swagger-ui/index.html"
        Write-ColorOutput -ForegroundColor Cyan "  OpenAPI Docs: http://localhost:8080/v3/api-docs"
    }
    
    "logs" {
        Get-Logs
    }
    
    "stop" {
        Write-ColorOutput -ForegroundColor Cyan "[INICJUJE STOP]"
        Stop-Backend
        Write-ColorOutput -ForegroundColor Green "`[OK`] Zakonczono"
    }
    
    "status" {
        Show-Status
    }
}

Write-Host ""

