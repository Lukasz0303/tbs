Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test scenariusza ruchu bota" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"

Write-Host "KROK 1: Rejestracja uzytkownika..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$registerBody = @{
    email = "test$timestamp@test.com"
    password = "test123!!"
    username = "testuser$timestamp"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/auth/register" -Method POST -Headers @{"accept"="*/*"} -Body $registerBody -ContentType "application/json" -ErrorAction Stop
    $registerData = $registerResponse.Content | ConvertFrom-Json
    $token = $registerData.authToken
    $userId = $registerData.userId
    Write-Host "OK Uzytkownik zarejestrowany: $($registerData.username) (ID: $userId)" -ForegroundColor Green
    Write-Host "  Token: $($token.Substring(0, [Math]::Min(50, $token.Length)))..." -ForegroundColor Gray
} catch {
    Write-Host "ERROR Blad rejestracji: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json -ErrorAction SilentlyContinue
        if ($errorDetails) {
            Write-Host "  Szczegoly: $($errorDetails.error.message)" -ForegroundColor Red
        } else {
            Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
    }
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        $reader.Close()
        Write-Host "  Response body: $responseBody" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "KROK 2: Tworzenie nowej gry vs_bot..." -ForegroundColor Yellow
$createGameBody = @{
    gameType = "vs_bot"
    boardSize = 3
    botDifficulty = "easy"
} | ConvertTo-Json

try {
    $createGameResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games" -Method POST -Headers @{"accept"="*/*"; "authorization"="Bearer $token"} -Body $createGameBody -ContentType "application/json" -ErrorAction Stop
    $gameData = $createGameResponse.Content | ConvertFrom-Json
    $gameId = $gameData.gameId
    Write-Host "OK Gra utworzona: ID $gameId" -ForegroundColor Green
    Write-Host "  Status: $($gameData.status), CurrentPlayerSymbol: $($gameData.currentPlayerSymbol)" -ForegroundColor Gray
} catch {
    Write-Host "ERROR Blad tworzenia gry: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json -ErrorAction SilentlyContinue
        if ($errorDetails) {
            Write-Host "  Szczegoly: $($errorDetails.error.message)" -ForegroundColor Red
        } else {
            Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
    }
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        $reader.Close()
        Write-Host "  Response body: $responseBody" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "KROK 3: Wykonanie ruchu gracza (symbol 'x' na 1,1)..." -ForegroundColor Yellow
$playerMoveBody = @{
    row = 1
    col = 1
    playerSymbol = "x"
} | ConvertTo-Json

try {
    $playerMoveResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games/$gameId/moves" -Method POST -Headers @{"accept"="*/*"; "authorization"="Bearer $token"} -Body $playerMoveBody -ContentType "application/json" -ErrorAction Stop
    $playerMoveData = $playerMoveResponse.Content | ConvertFrom-Json
    Write-Host "OK Ruch gracza wykonany: Move ID $($playerMoveData.moveId)" -ForegroundColor Green
    Write-Host "  Position: ($($playerMoveData.row), $($playerMoveData.col)), Symbol: $($playerMoveData.playerSymbol)" -ForegroundColor Gray
    Write-Host "  GameStatus: $($playerMoveData.gameStatus)" -ForegroundColor Gray
    
    Write-Host ""
    Write-Host "  Stan planszy:" -ForegroundColor Gray
    for ($i = 0; $i -lt $playerMoveData.boardState.state.Length; $i++) {
        $row = $playerMoveData.boardState.state[$i] | ForEach-Object { if ($null -eq $_) { "." } else { $_ } }
        Write-Host "  $($row -join ' ')" -ForegroundColor Gray
    }
} catch {
    Write-Host "ERROR Blad ruchu gracza: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json -ErrorAction SilentlyContinue
        if ($errorDetails) {
            Write-Host "  Szczegoly: $($errorDetails.error.message)" -ForegroundColor Red
        } else {
            Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
    }
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        $reader.Close()
        Write-Host "  Response body: $responseBody" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "KROK 4: Wykonanie ruchu bota..." -ForegroundColor Yellow
try {
    $botMoveResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games/$gameId/bot-move" -Method POST -Headers @{"accept"="*/*"; "authorization"="Bearer $token"} -ContentType "application/json" -ErrorAction Stop
    $botMoveData = $botMoveResponse.Content | ConvertFrom-Json
    Write-Host "OK Ruch bota wykonany: Move ID $($botMoveData.moveId)" -ForegroundColor Green
    Write-Host "  Position: ($($botMoveData.row), $($botMoveData.col)), Symbol: $($botMoveData.playerSymbol)" -ForegroundColor Gray
    Write-Host "  GameStatus: $($botMoveData.gameStatus)" -ForegroundColor Gray
    
    Write-Host ""
    Write-Host "  Stan planszy:" -ForegroundColor Gray
    for ($i = 0; $i -lt $botMoveData.boardState.state.Length; $i++) {
        $row = $botMoveData.boardState.state[$i] | ForEach-Object { if ($null -eq $_) { "." } else { $_ } }
        Write-Host "  $($row -join ' ')" -ForegroundColor Gray
    }
    
    if ($botMoveData.playerSymbol -eq "x") {
        Write-Host ""
        Write-Host "PROBLEM: Bot wykonal ruch 'x' zamiast 'o'!" -ForegroundColor Red
        Write-Host "  Oczekiwano: 'o', otrzymano: '$($botMoveData.playerSymbol)'" -ForegroundColor Red
        exit 1
    } elseif ($botMoveData.playerSymbol -eq "o") {
        Write-Host ""
        Write-Host "OK Ruch bota poprawny: symbol '$($botMoveData.playerSymbol)'" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "PROBLEM: Nieoczekiwany symbol bota: '$($botMoveData.playerSymbol)'" -ForegroundColor Red
        exit 1
    }
    
    if ($botMoveData.row -lt 0 -or $botMoveData.row -ge 3 -or $botMoveData.col -lt 0 -or $botMoveData.col -ge 3) {
        Write-Host ""
        Write-Host "PROBLEM: Bot wykonal ruch poza plansza!" -ForegroundColor Red
        Write-Host "  Position: ($($botMoveData.row), $($botMoveData.col))" -ForegroundColor Red
        exit 1
    }
    
    $boardState = $botMoveData.boardState.state
    if ($boardState[$botMoveData.row][$botMoveData.col] -ne $botMoveData.playerSymbol) {
        Write-Host ""
        Write-Host "PROBLEM: Symbol na planszy nie odpowiada symbolowi ruchu!" -ForegroundColor Red
        Write-Host "  Position: ($($botMoveData.row), $($botMoveData.col))" -ForegroundColor Red
        Write-Host "  Symbol ruchu: $($botMoveData.playerSymbol)" -ForegroundColor Red
        Write-Host "  Symbol na planszy: $($boardState[$botMoveData.row][$botMoveData.col])" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "ERROR Blad ruchu bota: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json -ErrorAction SilentlyContinue
        if ($errorDetails) {
            Write-Host "  Szczegoly: $($errorDetails.error.message)" -ForegroundColor Red
        } else {
            Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
    }
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        $reader.Close()
        Write-Host "  Response body: $responseBody" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test zakonczony" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

