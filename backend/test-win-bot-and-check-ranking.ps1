Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test wygranej z botem i sprawdzenia rankingu" -ForegroundColor Cyan
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
    $registerResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/auth/register" -Method POST -Headers @{"accept"="*/*"; "content-type"="application/json"} -Body $registerBody -ContentType "application/json"
    $registerData = $registerResponse.Content | ConvertFrom-Json
    $token = $registerData.authToken
    $userId = $registerData.userId
    Write-Host "OK Uzytkownik zarejestrowany: $($registerData.username) (ID: $userId)" -ForegroundColor Green
    Write-Host "  Token: $($token.Substring(0,50))..." -ForegroundColor Gray
} catch {
    Write-Host "ERROR Blad rejestracji: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "KROK 2: Pobranie poczatkowego stanu konta gracza..." -ForegroundColor Yellow
try {
    $initialProfileResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/auth/me" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $token"}
    $initialProfile = $initialProfileResponse.Content | ConvertFrom-Json
    $initialPoints = $initialProfile.totalPoints
    $initialGamesPlayed = $initialProfile.gamesPlayed
    $initialGamesWon = $initialProfile.gamesWon
    Write-Host "OK Stan poczatkowy konta:" -ForegroundColor Green
    Write-Host "  TotalPoints: $initialPoints" -ForegroundColor Gray
    Write-Host "  GamesPlayed: $initialGamesPlayed" -ForegroundColor Gray
    Write-Host "  GamesWon: $initialGamesWon" -ForegroundColor Gray
} catch {
    Write-Host "ERROR Blad pobierania profilu: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "KROK 3: Tworzenie nowej gry vs_bot..." -ForegroundColor Yellow
$createGameBody = @{
    gameType = "vs_bot"
    boardSize = 3
    botDifficulty = "easy"
} | ConvertTo-Json

try {
    $createGameResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games" -Method POST -Headers @{"accept"="*/*"; "authorization"="Bearer $token"; "content-type"="application/json"} -Body $createGameBody -ContentType "application/json"
    $gameData = $createGameResponse.Content | ConvertFrom-Json
    $gameId = $gameData.gameId
    Write-Host "OK Gra utworzona: ID $gameId" -ForegroundColor Green
    Write-Host "  Status: $($gameData.status), CurrentPlayerSymbol: $($gameData.currentPlayerSymbol)" -ForegroundColor Gray
} catch {
    Write-Host "ERROR Blad tworzenia gry: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "KROK 4: Wykonywanie ruchow gracza az do wygranej..." -ForegroundColor Yellow

function Get-EmptyCells {
    param($boardState)
    $empty = @()
    if ($null -eq $boardState -or $boardState.Length -eq 0) {
        return $empty
    }
    for ($i = 0; $i -lt $boardState.Length; $i++) {
        if ($null -eq $boardState[$i] -or $boardState[$i].Length -eq 0) {
            continue
        }
        for ($j = 0; $j -lt $boardState[$i].Length; $j++) {
            if ($null -eq $boardState[$i][$j] -or $boardState[$i][$j] -eq "") {
                $empty += @{row=$i; col=$j}
            }
        }
    }
    return $empty
}

function Find-WinningMove {
    param($boardState, $symbol)
    if ($null -eq $boardState -or $boardState.Length -eq 0) {
        return $null
    }
    $size = $boardState.Length
    
    for ($i = 0; $i -lt $size; $i++) {
        if ($null -eq $boardState[$i] -or $boardState[$i].Length -eq 0) {
            continue
        }
        for ($j = 0; $j -lt $size; $j++) {
            if ($null -eq $boardState[$i][$j] -or $boardState[$i][$j] -eq "") {
                $testBoard = @()
                for ($k = 0; $k -lt $boardState.Length; $k++) {
                    $testRow = @()
                    for ($l = 0; $l -lt $boardState[$k].Length; $l++) {
                        $testRow += $boardState[$k][$l]
                    }
                    $testBoard += ,$testRow
                }
                $testBoard[$i][$j] = $symbol
                
                if (Test-WinCondition -boardState $testBoard -symbol $symbol) {
                    return @{row=$i; col=$j}
                }
            }
        }
    }
    return $null
}

function Test-WinCondition {
    param($boardState, $symbol)
    if ($null -eq $boardState -or $boardState.Length -eq 0) {
        return $false
    }
    $size = $boardState.Length
    
    for ($i = 0; $i -lt $size; $i++) {
        if ($null -eq $boardState[$i] -or $boardState[$i].Length -eq 0) {
            continue
        }
        $rowWin = $true
        $colWin = $true
        for ($j = 0; $j -lt $size; $j++) {
            if ($null -eq $boardState[$i] -or $null -eq $boardState[$i][$j] -or $boardState[$i][$j] -ne $symbol) { 
                $rowWin = $false 
            }
            if ($null -eq $boardState[$j] -or $null -eq $boardState[$j][$i] -or $boardState[$j][$i] -ne $symbol) { 
                $colWin = $false 
            }
        }
        if ($rowWin -or $colWin) { return $true }
    }
    
    $diag1Win = $true
    $diag2Win = $true
    for ($i = 0; $i -lt $size; $i++) {
        if ($null -eq $boardState[$i] -or $boardState[$i].Length -eq 0) {
            $diag1Win = $false
            $diag2Win = $false
            continue
        }
        if ($null -eq $boardState[$i][$i] -or $boardState[$i][$i] -ne $symbol) { 
            $diag1Win = $false 
        }
        if ($null -eq $boardState[$i][$size-1-$i] -or $boardState[$i][$size-1-$i] -ne $symbol) { 
            $diag2Win = $false 
        }
    }
    if ($diag1Win -or $diag2Win) { return $true }
    
    return $false
}

$moveNumber = 0
$gameFinished = $false
$playerWon = $false
$maxMoves = 10

while (-not $gameFinished -and $moveNumber -lt $maxMoves) {
    $moveNumber++
    Write-Host ""
    Write-Host "  Ruch $moveNumber : Sprawdzanie stanu gry..." -ForegroundColor Cyan
    
    try {
        $gameStatusResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games/$gameId" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $token"}
        $gameStatus = $gameStatusResponse.Content | ConvertFrom-Json
        
        if ($gameStatus.status -eq "finished") {
            $gameFinished = $true
            if ($gameStatus.winnerId -eq $userId) {
                $playerWon = $true
                Write-Host "  *** WYGRANA! Gracz wygral gre! ***" -ForegroundColor Green
            }
            break
        }
        
        if ($gameStatus.status -eq "waiting") {
            Write-Host "  Gra w statusie 'waiting' - rozpoczynamy gre pierwszym ruchem..." -ForegroundColor Gray
        } elseif ($gameStatus.status -ne "in_progress") {
            Write-Host "  UWAGA: Gra w statusie: $($gameStatus.status)" -ForegroundColor Yellow
            break
        }
        
        $currentSymbol = $gameStatus.currentPlayerSymbol
        if ($null -ne $currentSymbol -and $currentSymbol -ne "" -and $currentSymbol -ne "x") {
            Write-Host "  Oczekiwanie na ture gracza (aktualny gracz: $currentSymbol)..." -ForegroundColor Gray
            Start-Sleep -Milliseconds 1000
            continue
        }
        
        $boardState = @()
        for ($i = 0; $i -lt $gameStatus.boardSize; $i++) {
            $row = @()
            for ($j = 0; $j -lt $gameStatus.boardSize; $j++) {
                $row += $null
            }
            $boardState += ,$row
        }
        
        try {
            $movesResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games/$gameId/moves" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $token"}
            $movesData = $movesResponse.Content | ConvertFrom-Json
            
            if ($movesData) {
                if ($movesData.PSObject.Properties.Name -contains "content") {
                    $movesList = $movesData.content
                } elseif ($movesData -is [Array]) {
                    $movesList = $movesData
                } else {
                    $movesList = @()
                }
                
                if ($movesList -and $movesList.Count -gt 0) {
                    foreach ($move in $movesList) {
                        if ($move -and $null -ne $move.row -and $null -ne $move.col -and $move.row -ge 0 -and $move.col -ge 0) {
                            if ($move.row -lt $boardState.Length -and $move.col -lt $boardState[$move.row].Length) {
                                $boardState[$move.row][$move.col] = $move.playerSymbol
                            }
                        }
                    }
                }
            }
        } catch {
            Write-Host "  UWAGA: Nie udalo sie pobrac historii ruchow (moze gra nie ma jeszcze ruchow)" -ForegroundColor Yellow
        }
        
        $winningMove = Find-WinningMove -boardState $boardState -symbol "x"
        $nextMove = $null
        
        if ($winningMove) {
            $nextMove = $winningMove
            Write-Host "  Strategia: Wykonanie ruchu wygrywajacego na ($($nextMove.row), $($nextMove.col))" -ForegroundColor Green
        } else {
            $emptyCells = Get-EmptyCells -boardState $boardState
            if ($emptyCells.Count -gt 0) {
                $nextMove = $emptyCells[0]
                Write-Host "  Strategia: Wykonanie ruchu na ($($nextMove.row), $($nextMove.col))" -ForegroundColor Gray
                Write-Host "  Dostepne wolne miejsca: $($emptyCells.Count)" -ForegroundColor DarkGray
            } else {
                Write-Host "  UWAGA: Brak wolnych miejsc na planszy" -ForegroundColor Yellow
                break
            }
        }
        
        if ($nextMove) {
            Write-Host "  Wykonywanie ruchu gracza ('x') na ($($nextMove.row), $($nextMove.col))..." -ForegroundColor Cyan
            
            $playerMoveBody = @{
                row = $nextMove.row
                col = $nextMove.col
                playerSymbol = "x"
            } | ConvertTo-Json
            
            $playerMoveResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games/$gameId/moves" -Method POST -Headers @{"accept"="*/*"; "authorization"="Bearer $token"; "content-type"="application/json"} -Body $playerMoveBody -ContentType "application/json"
            $playerMoveData = $playerMoveResponse.Content | ConvertFrom-Json
            Write-Host "  OK Ruch gracza wykonany: Move ID $($playerMoveData.moveId)" -ForegroundColor Green
            Write-Host "    Position: ($($playerMoveData.row), $($playerMoveData.col)), Symbol: $($playerMoveData.playerSymbol)" -ForegroundColor Gray
            Write-Host "    GameStatus: $($playerMoveData.gameStatus)" -ForegroundColor Gray
            
            Write-Host ""
            Write-Host "    Stan planszy:" -ForegroundColor Gray
            for ($i = 0; $i -lt $playerMoveData.boardState.state.Length; $i++) {
                $row = $playerMoveData.boardState.state[$i] | ForEach-Object { if ($null -eq $_) { "." } else { $_ } }
                Write-Host "    $($row -join ' ')" -ForegroundColor Gray
            }
            
            if ($playerMoveData.gameStatus -eq "finished") {
                $gameFinished = $true
                if ($playerMoveData.winner -and $playerMoveData.winner.userId -eq $userId) {
                    $playerWon = $true
                    Write-Host ""
                    Write-Host "  *** WYGRANA! Gracz wygral gre! ***" -ForegroundColor Green
                    Write-Host "    Winner: $($playerMoveData.winner.username) (ID: $($playerMoveData.winner.userId))" -ForegroundColor Green
                } else {
                    Write-Host ""
                    Write-Host "  Gra zakonczona, ale gracz nie wygral" -ForegroundColor Yellow
                }
                break
            }
            
            if ($playerMoveData.gameStatus -eq "in_progress") {
                Write-Host ""
                Write-Host "  Wykonywanie ruchu bota..." -ForegroundColor Cyan
                try {
                    $botMoveResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games/$gameId/bot-move" -Method POST -Headers @{"accept"="*/*"; "authorization"="Bearer $token"} -ContentType "application/json"
                    $botMoveData = $botMoveResponse.Content | ConvertFrom-Json
                    Write-Host "  OK Ruch bota wykonany: Move ID $($botMoveData.moveId)" -ForegroundColor Green
                    Write-Host "    Position: ($($botMoveData.row), $($botMoveData.col)), Symbol: $($botMoveData.playerSymbol)" -ForegroundColor Gray
                    Write-Host "    GameStatus: $($botMoveData.gameStatus)" -ForegroundColor Gray
                    
                    Write-Host ""
                    Write-Host "    Stan planszy po ruchu bota:" -ForegroundColor Gray
                    for ($i = 0; $i -lt $botMoveData.boardState.state.Length; $i++) {
                        $row = $botMoveData.boardState.state[$i] | ForEach-Object { if ($null -eq $_) { "." } else { $_ } }
                        Write-Host "    $($row -join ' ')" -ForegroundColor Gray
                    }
                    
                    if ($botMoveData.gameStatus -eq "finished") {
                        $gameFinished = $true
                        if ($botMoveData.winner -and $botMoveData.winner.userId -eq $userId) {
                            $playerWon = $true
                            Write-Host ""
                            Write-Host "  *** WYGRANA! Gracz wygral gre! ***" -ForegroundColor Green
                            Write-Host "    Winner: $($botMoveData.winner.username) (ID: $($botMoveData.winner.userId))" -ForegroundColor Green
                        } else {
                            Write-Host ""
                            Write-Host "  Gra zakonczona, ale gracz nie wygral" -ForegroundColor Yellow
                        }
                        break
                    }
                    
                } catch {
                    Write-Host "  ERROR Blad ruchu bota: $($_.Exception.Message)" -ForegroundColor Red
                    if ($_.ErrorDetails) {
                        Write-Host "    Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
                    }
                }
            }
            
            Start-Sleep -Milliseconds 500
        }
    } catch {
        Write-Host "  ERROR Blad podczas wykonywania ruchu: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails) {
            Write-Host "    Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
        exit 1
    }
}

if (-not $gameFinished) {
    Write-Host ""
    Write-Host "  UWAGA: Gra nie zakonczyla sie po wykonanych ruchach" -ForegroundColor Yellow
    Write-Host "  Sprawdzanie aktualnego stanu gry..." -ForegroundColor Yellow
    
    try {
        $gameStatusResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games/$gameId" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $token"}
        $gameStatus = $gameStatusResponse.Content | ConvertFrom-Json
        Write-Host "  Status gry: $($gameStatus.status)" -ForegroundColor Gray
        if ($gameStatus.status -eq "finished" -and $gameStatus.winnerId -eq $userId) {
            $playerWon = $true
            Write-Host "  OK Gracz wygral gre!" -ForegroundColor Green
        }
    } catch {
        Write-Host "  ERROR Blad sprawdzania stanu gry: $($_.Exception.Message)" -ForegroundColor Red
    }
}

if (-not $playerWon) {
    Write-Host ""
    Write-Host "ERROR Gracz nie wygral gry. Test nie moze kontynuowac." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "KROK 5: Sprawdzenie stanu konta gracza po wygranej..." -ForegroundColor Yellow
try {
    $profileResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/auth/me" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $token"}
    $profile = $profileResponse.Content | ConvertFrom-Json
    $newPoints = $profile.totalPoints
    $newGamesPlayed = $profile.gamesPlayed
    $newGamesWon = $profile.gamesWon
    
    Write-Host "OK Stan konta po wygranej:" -ForegroundColor Green
    Write-Host "  TotalPoints: $newPoints (poprzednio: $initialPoints)" -ForegroundColor Gray
    Write-Host "  GamesPlayed: $newGamesPlayed (poprzednio: $initialGamesPlayed)" -ForegroundColor Gray
    Write-Host "  GamesWon: $newGamesWon (poprzednio: $initialGamesWon)" -ForegroundColor Gray
    
    $pointsIncreased = $newPoints -gt $initialPoints
    $gamesPlayedIncreased = $newGamesPlayed -gt $initialGamesPlayed
    $gamesWonIncreased = $newGamesWon -gt $initialGamesWon
    
    Write-Host ""
    if ($pointsIncreased) {
        Write-Host "  OK Punkty zwiekszyly sie o $($newPoints - $initialPoints)" -ForegroundColor Green
    } else {
        Write-Host "  PROBLEM: Punkty nie zwiekszyly sie!" -ForegroundColor Red
    }
    
    if ($gamesPlayedIncreased) {
        Write-Host "  OK Liczba rozegranych gier zwiekszyla sie o $($newGamesPlayed - $initialGamesPlayed)" -ForegroundColor Green
    } else {
        Write-Host "  PROBLEM: Liczba rozegranych gier nie zwiekszyla sie!" -ForegroundColor Red
    }
    
    if ($gamesWonIncreased) {
        Write-Host "  OK Liczba wygranych gier zwiekszyla sie o $($newGamesWon - $initialGamesWon)" -ForegroundColor Green
    } else {
        Write-Host "  PROBLEM: Liczba wygranych gier nie zwiekszyla sie!" -ForegroundColor Red
    }
    
    if (-not ($pointsIncreased -and $gamesPlayedIncreased -and $gamesWonIncreased)) {
        Write-Host ""
        Write-Host "  UWAGA: Nie wszystkie statystyki zostaly zaktualizowane!" -ForegroundColor Yellow
    }
} catch {
    Write-Host "ERROR Blad pobierania profilu: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "KROK 6: Sprawdzenie globalnego rankingu..." -ForegroundColor Yellow
try {
    $rankingsResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/rankings?page=0&size=100" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $token"}
    $rankings = $rankingsResponse.Content | ConvertFrom-Json
    
    Write-Host "OK Globalny ranking pobrany:" -ForegroundColor Green
    Write-Host "  TotalElements: $($rankings.totalElements)" -ForegroundColor Gray
    Write-Host "  TotalPages: $($rankings.totalPages)" -ForegroundColor Gray
    Write-Host "  Size: $($rankings.size)" -ForegroundColor Gray
    
    $playerInRanking = $false
    $playerRankPosition = $null
    
    foreach ($item in $rankings.content) {
        if ($item.userId -eq $userId) {
            $playerInRanking = $true
            $playerRankPosition = $item.rankPosition
            Write-Host ""
            Write-Host "  OK Gracz znaleziony w rankingu:" -ForegroundColor Green
            Write-Host "    Pozycja: $($item.rankPosition)" -ForegroundColor Gray
            Write-Host "    Username: $($item.username)" -ForegroundColor Gray
            Write-Host "    TotalPoints: $($item.totalPoints)" -ForegroundColor Gray
            Write-Host "    GamesPlayed: $($item.gamesPlayed)" -ForegroundColor Gray
            Write-Host "    GamesWon: $($item.gamesWon)" -ForegroundColor Gray
            break
        }
    }
    
    if (-not $playerInRanking) {
        Write-Host ""
        Write-Host "  UWAGA: Gracz nie zostal znaleziony w pierwszej 100 pozycjach rankingu" -ForegroundColor Yellow
        Write-Host "  (Mozliwe, ze jest dalej w rankingu lub ranking nie zostal jeszcze zaktualizowany)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "ERROR Blad pobierania rankingu: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "KROK 7: Sprawdzenie pozycji gracza w rankingu (GET /api/rankings/{userId})..." -ForegroundColor Yellow
try {
    $userRankingResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/rankings/$userId" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $token"}
    $userRanking = $userRankingResponse.Content | ConvertFrom-Json
    
    Write-Host "OK Pozycja gracza w rankingu:" -ForegroundColor Green
    Write-Host "  RankPosition: $($userRanking.rankPosition)" -ForegroundColor Gray
    Write-Host "  UserId: $($userRanking.userId)" -ForegroundColor Gray
    Write-Host "  Username: $($userRanking.username)" -ForegroundColor Gray
    Write-Host "  TotalPoints: $($userRanking.totalPoints)" -ForegroundColor Gray
    Write-Host "  GamesPlayed: $($userRanking.gamesPlayed)" -ForegroundColor Gray
    Write-Host "  GamesWon: $($userRanking.gamesWon)" -ForegroundColor Gray
    
    if ($userRanking.totalPoints -eq $newPoints) {
        Write-Host ""
        Write-Host "  OK Punkty w rankingu sa zgodne z profilem gracza" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "  PROBLEM: Punkty w rankingu ($($userRanking.totalPoints)) nie sa zgodne z profilem ($newPoints)!" -ForegroundColor Red
    }
    
    if ($userRanking.gamesPlayed -eq $newGamesPlayed) {
        Write-Host "  OK Liczba rozegranych gier w rankingu jest zgodna z profilem gracza" -ForegroundColor Green
    } else {
        Write-Host "  PROBLEM: Liczba rozegranych gier w rankingu ($($userRanking.gamesPlayed)) nie jest zgodna z profilem ($newGamesPlayed)!" -ForegroundColor Red
    }
    
    if ($userRanking.gamesWon -eq $newGamesWon) {
        Write-Host "  OK Liczba wygranych gier w rankingu jest zgodna z profilem gracza" -ForegroundColor Green
    } else {
        Write-Host "  PROBLEM: Liczba wygranych gier w rankingu ($($userRanking.gamesWon)) nie jest zgodna z profilem ($newGamesWon)!" -ForegroundColor Red
    }
} catch {
    Write-Host "ERROR Blad pobierania pozycji gracza w rankingu: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json -ErrorAction SilentlyContinue
    if ($errorDetails -and $errorDetails.status -eq 404) {
        Write-Host ""
        Write-Host "  UWAGA: Gracz nie zostal znaleziony w rankingu (404)" -ForegroundColor Yellow
        Write-Host "  (Mozliwe, ze ranking nie zostal jeszcze zaktualizowany)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test zakonczony" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

