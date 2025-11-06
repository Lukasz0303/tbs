Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test scenariusza gry PvP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"

$user1Email = "lukasz.zielinski0303@gmail.com"
$user1Password = "u331ty!!"
$user1Username = "lukasz.zielinski"

$user2Email = "l.zzzielinski@gmail.com"
$user2Password = "u331ty!!"
$user2Username = "karol.zielinski"

$user1Token = $null
$user1Id = $null
$user2Token = $null
$user2Id = $null
$gameId = $null

function Get-Or-Create-User {
    param(
        [string]$email,
        [string]$password,
        [string]$username,
        [string]$userLabel
    )
    
    Write-Host "KROK: Rejestracja/logowanie uzytkownika $userLabel..." -ForegroundColor Yellow
    Write-Host "  Email: $email" -ForegroundColor Gray
    Write-Host "  Username: $username" -ForegroundColor Gray
    
    $registerBody = @{
        email = $email
        password = $password
        username = $username
    } | ConvertTo-Json
    
    try {
        $registerResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/auth/register" -Method POST -Headers @{"accept"="*/*"; "content-type"="application/json"} -Body $registerBody -ContentType "application/json" -ErrorAction Stop
        $registerData = $registerResponse.Content | ConvertFrom-Json
        $token = $registerData.authToken
        $userId = $registerData.userId
        Write-Host "OK Uzytkownik zarejestrowany: $($registerData.username) (ID: $userId)" -ForegroundColor Green
        Write-Host "  Token: $($token.Substring(0, [Math]::Min(50, $token.Length)))..." -ForegroundColor Gray
        return @{Token = $token; UserId = $userId}
    } catch {
        $statusCode = $null
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode.value__
        }
        
        $shouldTryLogin = $false
        if ($statusCode -eq 409) {
            $shouldTryLogin = $true
        } elseif ($statusCode -eq 400) {
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $responseBody = $reader.ReadToEnd()
                $reader.Close()
                $stream.Close()
                $errorData = $responseBody | ConvertFrom-Json -ErrorAction SilentlyContinue
                if ($errorData -and $errorData.error -and ($errorData.error.message -like "*already exists*" -or $errorData.error.message -like "*Email already*")) {
                    $shouldTryLogin = $true
                }
            } catch {
            }
        }
        
        if ($shouldTryLogin) {
            Write-Host "INFO Uzytkownik juz istnieje, probuje zalogowac..." -ForegroundColor Yellow
            $loginBody = @{
                email = $email
                password = $password
            } | ConvertTo-Json
            
            try {
                $loginResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/auth/login" -Method POST -Headers @{"accept"="*/*"; "content-type"="application/json"} -Body $loginBody -ContentType "application/json" -ErrorAction Stop
                $loginData = $loginResponse.Content | ConvertFrom-Json
                $token = $loginData.authToken
                $userId = $loginData.userId
                Write-Host "OK Uzytkownik zalogowany: $($loginData.username) (ID: $userId)" -ForegroundColor Green
                Write-Host "  Token: $($token.Substring(0, [Math]::Min(50, $token.Length)))..." -ForegroundColor Gray
                return @{Token = $token; UserId = $userId}
            } catch {
                Write-Host "ERROR Blad logowania: $($_.Exception.Message)" -ForegroundColor Red
                if ($_.ErrorDetails) {
                    $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json -ErrorAction SilentlyContinue
                    if ($errorDetails) {
                        Write-Host "  Szczegoly: $($errorDetails.error.message)" -ForegroundColor Red
                    } else {
                        Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
                    }
                }
                throw
            }
        } else {
            Write-Host "ERROR Blad rejestracji: $($_.Exception.Message)" -ForegroundColor Red
            Write-Host "  Status Code: $statusCode" -ForegroundColor Red
            if ($_.ErrorDetails) {
                $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json -ErrorAction SilentlyContinue
                if ($errorDetails) {
                    Write-Host "  Szczegoly: $($errorDetails.error.message)" -ForegroundColor Red
                } else {
                    Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
                }
            } elseif ($_.Exception.Response) {
                try {
                    $stream = $_.Exception.Response.GetResponseStream()
                    $reader = New-Object System.IO.StreamReader($stream)
                    $responseBody = $reader.ReadToEnd()
                    $reader.Close()
                    $stream.Close()
                    Write-Host "  Response body: $responseBody" -ForegroundColor Red
                } catch {
                    Write-Host "  Nie mozna odczytac odpowiedzi serwera" -ForegroundColor Red
                }
            }
            throw
        }
    }
}

Write-Host ""
$user1Result = Get-Or-Create-User -email $user1Email -password $user1Password -username $user1Username -userLabel "A"
$user1Token = $user1Result.Token
$user1Id = $user1Result.UserId

Write-Host ""
$user2Result = Get-Or-Create-User -email $user2Email -password $user2Password -username $user2Username -userLabel "B"
$user2Token = $user2Result.Token
$user2Id = $user2Result.UserId

Write-Host ""
Write-Host "KROK: Przygotowanie gracza B przed wyzwaniem..." -ForegroundColor Yellow
Write-Host "  Probuje usunac gracza B z kolejki matchmaking (jesli jest w kolejce)..." -ForegroundColor Gray

try {
    $removeFromQueueResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/matching/queue" -Method DELETE -Headers @{"accept"="*/*"; "authorization"="Bearer $user2Token"} -ErrorAction SilentlyContinue
    Write-Host "OK Gracz B usuniety z kolejki matchmaking" -ForegroundColor Green
} catch {
    $statusCode = $null
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
    }
    if ($statusCode -eq 404) {
        Write-Host "INFO Gracz B nie byl w kolejce matchmaking" -ForegroundColor Gray
    } else {
        Write-Host "INFO Nie mozna usunac gracza B z kolejki (moze nie byl w kolejce)" -ForegroundColor Gray
    }
}

function End-ActivePvpGames {
    param(
        [string]$token,
        [string]$playerLabel
    )
    
    Write-Host ""
    Write-Host "KROK: Sprawdzanie aktywnych gier PvP $playerLabel..." -ForegroundColor Yellow
    
    $allActiveGames = @()
    
    $statusesToCheck = @("IN_PROGRESS", "WAITING")
    
    foreach ($status in $statusesToCheck) {
        try {
            $gamesResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games?status=$status&gameType=PVP" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $token"} -ErrorAction Stop
            $gamesData = $gamesResponse.Content | ConvertFrom-Json
            $games = $gamesData.content
            
            if ($games -and $games.Count -gt 0) {
                $allActiveGames += $games
            }
        } catch {
        }
    }
    
    if ($allActiveGames.Count -gt 0) {
        Write-Host "INFO Znaleziono $($allActiveGames.Count) aktywnych gier PvP" -ForegroundColor Yellow
        foreach ($game in $allActiveGames) {
            Write-Host "  Konczenie gry ID: $($game.gameId) (status: $($game.status))..." -ForegroundColor Gray
            $gameEnded = $false
            
            $statusesToTry = @("abandoned", "finished")
            foreach ($endStatus in $statusesToTry) {
                if (-not $gameEnded) {
                    try {
                        $endGameBody = @{
                            status = $endStatus
                        } | ConvertTo-Json
                        
                        $endGameResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games/$($game.gameId)/status" -Method PUT -Headers @{"accept"="*/*"; "authorization"="Bearer $token"; "content-type"="application/json"} -Body $endGameBody -ContentType "application/json" -ErrorAction Stop
                        Write-Host "  OK Gra ID $($game.gameId) zakonczona ze statusem $endStatus" -ForegroundColor Green
                        $gameEnded = $true
                    } catch {
                        if ($endStatus -eq $statusesToTry[-1]) {
                            Write-Host "  ERROR Nie mozna zakonczyc gry ID $($game.gameId): $($_.Exception.Message)" -ForegroundColor Red
                            $errorStatusCode = $null
                            if ($_.Exception.Response) {
                                $errorStatusCode = $_.Exception.Response.StatusCode.value__
                                Write-Host "    Status Code: $errorStatusCode" -ForegroundColor Red
                            }
                            if ($_.Exception.Response) {
                                try {
                                    $errorStream = $_.Exception.Response.GetResponseStream()
                                    $errorReader = New-Object System.IO.StreamReader($errorStream)
                                    $errorResponseBody = $errorReader.ReadToEnd()
                                    $errorReader.Close()
                                    $errorStream.Close()
                                    $errorData = $errorResponseBody | ConvertFrom-Json -ErrorAction SilentlyContinue
                                    if ($errorData -and $errorData.error) {
                                        Write-Host "    Szczegoly: $($errorData.error.message)" -ForegroundColor Red
                                    }
                                } catch {
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Write-Host "INFO Brak aktywnych gier PvP (IN_PROGRESS lub WAITING)" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "KROK: Tworzenie gry PvP..." -ForegroundColor Yellow
Write-Host "  Gracz A (ID: $user1Id) wyzywa gracza B (ID: $user2Id)" -ForegroundColor Gray

$challengeBody = @{
    boardSize = "THREE"
} | ConvertTo-Json

$maxRetries = 2
$retryCount = 0
$challengeSuccess = $false
$gameId = $null

while ($retryCount -lt $maxRetries -and -not $challengeSuccess) {
    try {
        $challengeResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/matching/challenge/$user2Id" -Method POST -Headers @{"accept"="*/*"; "authorization"="Bearer $user1Token"; "content-type"="application/json"} -Body $challengeBody -ContentType "application/json" -ErrorAction Stop
        $challengeData = $challengeResponse.Content | ConvertFrom-Json
        $gameId = $challengeData.gameId
        Write-Host "OK Gra PvP utworzona: ID $gameId" -ForegroundColor Green
        Write-Host "  Status: $($challengeData.status)" -ForegroundColor Gray
        Write-Host "  BoardSize: $($challengeData.boardSize)" -ForegroundColor Gray
        Write-Host "  Player1: $($challengeData.player1Id), Player2: $($challengeData.player2Id)" -ForegroundColor Gray
        $challengeSuccess = $true
    } catch {
        $statusCode = $null
        $errorMessage = $null
        
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode.value__
        }
        
        if ($_.ErrorDetails) {
            $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json -ErrorAction SilentlyContinue
            if ($errorDetails) {
                $errorMessage = $errorDetails.error.message
            } else {
                $errorMessage = $_.ErrorDetails.Message
            }
        } elseif ($_.Exception.Response) {
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $responseBody = $reader.ReadToEnd()
                $reader.Close()
                $stream.Close()
                $errorData = $responseBody | ConvertFrom-Json -ErrorAction SilentlyContinue
                if ($errorData -and $errorData.error) {
                    $errorMessage = $errorData.error.message
                }
            } catch {
            }
        }
        
        if ($statusCode -eq 409 -and $errorMessage -like "*unavailable*") {
            $retryCount++
            if ($retryCount -lt $maxRetries) {
                Write-Host "WARN Gracz B jest niedostepny (Status: $statusCode)" -ForegroundColor Yellow
                Write-Host "  Szczegoly: $errorMessage" -ForegroundColor Yellow
                Write-Host "  Probuje zakonczyc aktywne gry gracza B..." -ForegroundColor Yellow
                End-ActivePvpGames -token $user2Token -playerLabel "B"
                Start-Sleep -Seconds 1
                Write-Host "  Probuje ponownie utworzyc wyzwanie..." -ForegroundColor Yellow
            } else {
                Write-Host "ERROR Blad tworzenia gry PvP po $maxRetries probach: $($_.Exception.Message)" -ForegroundColor Red
                Write-Host "  Status Code: $statusCode" -ForegroundColor Red
                Write-Host "  Szczegoly: $errorMessage" -ForegroundColor Red
                exit 1
            }
        } else {
            Write-Host "ERROR Blad tworzenia gry PvP: $($_.Exception.Message)" -ForegroundColor Red
            Write-Host "  Status Code: $statusCode" -ForegroundColor Red
            Write-Host "  Szczegoly: $errorMessage" -ForegroundColor Red
            exit 1
        }
    }
}

function Make-Move {
    param(
        [string]$token,
        [long]$gameId,
        [int]$row,
        [int]$col,
        [string]$playerSymbol,
        [string]$playerLabel
    )
    
    Write-Host ""
    Write-Host "KROK: Wykonanie ruchu przez $playerLabel (symbol '$playerSymbol' na pozycji $row,$col)..." -ForegroundColor Yellow
    
    $moveBody = @{
        row = $row
        col = $col
        playerSymbol = $playerSymbol
    } | ConvertTo-Json
    
    try {
        $moveResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games/$gameId/moves" -Method POST -Headers @{"accept"="*/*"; "authorization"="Bearer $token"; "content-type"="application/json"} -Body $moveBody -ContentType "application/json" -ErrorAction Stop
        $moveData = $moveResponse.Content | ConvertFrom-Json
        Write-Host "OK Ruch wykonany: Move ID $($moveData.moveId)" -ForegroundColor Green
        Write-Host "  Position: ($($moveData.row), $($moveData.col)), Symbol: $($moveData.playerSymbol)" -ForegroundColor Gray
        Write-Host "  GameStatus: $($moveData.gameStatus)" -ForegroundColor Gray
        
        if ($moveData.boardState -and $moveData.boardState.state) {
            Write-Host ""
            Write-Host "  Stan planszy:" -ForegroundColor Gray
            try {
                $stateProperty = $moveData.boardState.state
                
                if ($stateProperty -eq $null) {
                    return $moveData
                }
                
                $stateArray = @($stateProperty)
                
                foreach ($rowItem in $stateArray) {
                    if ($rowItem -ne $null) {
                        $rowArray = @($rowItem)
                        $rowStr = @()
                        foreach ($cellItem in $rowArray) {
                            if ($null -eq $cellItem -or $cellItem -eq "") {
                                $rowStr += "."
                            } else {
                                $rowStr += [string]$cellItem
                            }
                        }
                        Write-Host "  $($rowStr -join ' ')" -ForegroundColor Gray
                    }
                }
            } catch {
                Write-Host "  Nie mozna wyswietlic planszy: $($_.Exception.Message)" -ForegroundColor Gray
            }
        }
        
        if ($moveData.winner) {
            Write-Host ""
            Write-Host "  ZWYCIĘZCA: $($moveData.winner.username) (ID: $($moveData.winner.userId))" -ForegroundColor Green
        }
        
        return $moveData
    } catch {
        Write-Host "ERROR Błąd wykonania ruchu: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails) {
            $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json -ErrorAction SilentlyContinue
            if ($errorDetails) {
                Write-Host "  Szczegoly: $($errorDetails.error.message)" -ForegroundColor Red
            } else {
                Write-Host "  Szczegoly: $($_.ErrorDetails.Message)" -ForegroundColor Red
            }
        }
        throw
    }
}

function Get-GameState {
    param(
        [string]$token,
        [long]$gameId
    )
    
    try {
        $gameResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games/$gameId" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $token"} -ErrorAction Stop
        $gameData = $gameResponse.Content | ConvertFrom-Json
        return $gameData
    } catch {
        return $null
    }
}

Write-Host ""
Write-Host "KROK: Rozpoczecie rozgrywki PvP..." -ForegroundColor Yellow
Write-Host ""

$moveSequence = @(
    @{row = 1; col = 1},
    @{row = 0; col = 0},
    @{row = 0; col = 1},
    @{row = 0; col = 2},
    @{row = 1; col = 0},
    @{row = 1; col = 2},
    @{row = 2; col = 0},
    @{row = 2; col = 1},
    @{row = 2; col = 2}
)

$moveIndex = 0
$lastMoveResult = $null
$maxAttempts = 20
$attempts = 0

while ($attempts -lt $maxAttempts -and $moveIndex -lt $moveSequence.Length) {
    $gameState = Get-GameState -token $user1Token -gameId $gameId
    
    if (-not $gameState) {
        Write-Host "ERROR Nie mozna pobrac stanu gry" -ForegroundColor Red
        break
    }
    
    if ($gameState.status -eq "FINISHED" -or $gameState.status -eq "DRAW") {
        Write-Host ""
        Write-Host "Gra zakonczona ze statusem: $($gameState.status)" -ForegroundColor Cyan
        break
    }
    
    $player1Id = $null
    $player2Id = $null
    if ($gameState.player1) {
        $player1Id = $gameState.player1.userId
    }
    if ($gameState.player2) {
        $player2Id = $gameState.player2.userId
    }
    
    $currentPlayerSymbol = $null
    if ($gameState.currentPlayerSymbol) {
        $currentPlayerSymbol = $gameState.currentPlayerSymbol.ToLower()
    } elseif ($lastMoveResult -and $lastMoveResult.currentPlayerSymbol) {
        $currentPlayerSymbol = $lastMoveResult.currentPlayerSymbol.ToLower()
    }
    
    if (-not $currentPlayerSymbol) {
        if ($gameState.status -eq "WAITING") {
            $currentPlayerSymbol = "x"
        } else {
            Write-Host "INFO Nie mozna okreslic aktualnego gracza, probuje nastepnej pozycji..." -ForegroundColor Yellow
            $moveIndex++
            $attempts++
            continue
        }
    }
    
    $move = $moveSequence[$moveIndex]
    $row = $move.row
    $col = $move.col
    
    if ($currentPlayerSymbol -eq "x") {
        if ($player1Id -eq $user1Id) {
            $token = $user1Token
            $playerLabel = "Gracz A"
        } else {
            $token = $user2Token
            $playerLabel = "Gracz B"
        }
        
        try {
            $moveResult = Make-Move -token $token -gameId $gameId -row $row -col $col -playerSymbol "x" -playerLabel $playerLabel
            $lastMoveResult = $moveResult
            if ($moveResult.gameStatus -eq "FINISHED" -or $moveResult.gameStatus -eq "DRAW") {
                Write-Host ""
                Write-Host "Gra zakonczona ze statusem: $($moveResult.gameStatus)" -ForegroundColor Cyan
                break
            }
            $moveIndex++
        } catch {
            $statusCode = $null
            if ($_.Exception.Response) {
                $statusCode = $_.Exception.Response.StatusCode.value__
            }
            if ($statusCode -eq 403) {
                Write-Host "INFO Nie kolej gracza (403), probuje nastepnej pozycji..." -ForegroundColor Yellow
            } else {
                Write-Host "INFO Proba wykonania ruchu na zajete pole lub nieprawidlowy ruch ($row,$col), probuje nastepnej pozycji..." -ForegroundColor Yellow
            }
            $moveIndex++
            continue
        }
    } elseif ($currentPlayerSymbol -eq "o") {
        if ($player2Id -eq $user2Id) {
            $token = $user2Token
            $playerLabel = "Gracz B"
        } else {
            $token = $user1Token
            $playerLabel = "Gracz A"
        }
        
        try {
            $moveResult = Make-Move -token $token -gameId $gameId -row $row -col $col -playerSymbol "o" -playerLabel $playerLabel
            $lastMoveResult = $moveResult
            if ($moveResult.gameStatus -eq "FINISHED" -or $moveResult.gameStatus -eq "DRAW") {
                Write-Host ""
                Write-Host "Gra zakonczona ze statusem: $($moveResult.gameStatus)" -ForegroundColor Cyan
                break
            }
            $moveIndex++
        } catch {
            $statusCode = $null
            if ($_.Exception.Response) {
                $statusCode = $_.Exception.Response.StatusCode.value__
            }
            if ($statusCode -eq 403) {
                Write-Host "INFO Nie kolej gracza (403), probuje nastepnej pozycji..." -ForegroundColor Yellow
            } else {
                Write-Host "INFO Proba wykonania ruchu na zajete pole lub nieprawidlowy ruch ($row,$col), probuje nastepnej pozycji..." -ForegroundColor Yellow
            }
            $moveIndex++
            continue
        }
    } else {
        Write-Host "INFO Nieznany symbol gracza: $currentPlayerSymbol, probuje nastepnej pozycji..." -ForegroundColor Yellow
        $moveIndex++
        continue
    }
    
    $attempts++
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test zakonczony" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
