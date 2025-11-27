param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("start", "restart", "logs", "stop", "status")]
    [string]$Action = "start"
)

$ScriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendScript = Join-Path $ScriptPath "backend\run-backend.ps1"

if (-not (Test-Path $BackendScript)) {
    Write-Host "[ERROR] Skrypt backendu nie istnieje: $BackendScript" -ForegroundColor Red
    Write-Host "[HINT] Upewnij sie ze katalog backend istnieje i zawiera plik run-backend.ps1" -ForegroundColor Yellow
    exit 1
}

& $BackendScript $Action

