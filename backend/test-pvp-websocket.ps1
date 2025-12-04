Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test scenariusza gry PvP z WebSocket" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"
$wsBaseUrl = "ws://localhost:8080"

$user1Email = if ($env:TEST_PVP_USER1_EMAIL) { $env:TEST_PVP_USER1_EMAIL } else { "test-pvp-user1@test.local" }
$user1Password = if ($env:TEST_PVP_USER1_PASSWORD) { $env:TEST_PVP_USER1_PASSWORD } else { "test123!!" }
$user1Username = if ($env:TEST_PVP_USER1_USERNAME) { $env:TEST_PVP_USER1_USERNAME } else { "testuser1" }

$user2Email = if ($env:TEST_PVP_USER2_EMAIL) { $env:TEST_PVP_USER2_EMAIL } else { "test-pvp-user2@test.local" }
$user2Password = if ($env:TEST_PVP_USER2_PASSWORD) { $env:TEST_PVP_USER2_PASSWORD } else { "test123!!" }
$user2Username = if ($env:TEST_PVP_USER2_USERNAME) { $env:TEST_PVP_USER2_USERNAME } else { "testuser2" }

$user1Token = $null
$user1Id = $null
$user2Token = $null
$user2Id = $null
$gameId = $null

$user1WsMessages = New-Object System.Collections.ArrayList
$user2WsMessages = New-Object System.Collections.ArrayList

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
                throw
            }
        } else {
            Write-Host "ERROR Blad rejestracji: $($_.Exception.Message)" -ForegroundColor Red
            throw
        }
    }
}

function Receive-WebSocketMessage {
    param(
        [System.Net.WebSockets.ClientWebSocket]$client,
        [System.Collections.ArrayList]$messageList,
        [int]$timeoutMs = 500
    )
    
    if ($client.State -ne [System.Net.WebSockets.WebSocketState]::Open) {
        return $false
    }
    
    try {
        $buffer = New-Object byte[] 4096
        $arraySegment = New-Object System.ArraySegment[byte]($buffer)
        $cancellationTokenSource = New-Object System.Threading.CancellationTokenSource
        $cancellationTokenSource.CancelAfter($timeoutMs)
        
        $task = $client.ReceiveAsync($arraySegment, $cancellationTokenSource.Token)
        
        $waitResult = $task.Wait($timeoutMs + 100)
        if ($waitResult -and $task.IsCompleted -and -not $task.IsFaulted) {
            $result = $task.Result
            if ($result.MessageType -eq [System.Net.WebSockets.WebSocketMessageType]::Text) {
                $messageText = [System.Text.Encoding]::UTF8.GetString($buffer, 0, $result.Count)
                [void]$messageList.Add($messageText)
                return $true
            }
        }
        return $false
    } catch {
        return $false
    }
}

function Poll-WebSocketMessages {
    param(
        [System.Net.WebSockets.ClientWebSocket]$client,
        [System.Collections.ArrayList]$messageList,
        [int]$maxAttempts = 10
    )
    
    $attempts = 0
    $consecutiveFailures = 0
    $maxConsecutiveFailures = 5
    
    while ($attempts -lt $maxAttempts -and $client.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
        $received = Receive-WebSocketMessage -client $client -messageList $messageList -timeoutMs 500
        if ($received) {
            $consecutiveFailures = 0
        } else {
            $consecutiveFailures++
            if ($consecutiveFailures -ge $maxConsecutiveFailures) {
                break
            }
        }
        $attempts++
        Start-Sleep -Milliseconds 50
    }
}

function Connect-WebSocket {
    param(
        [string]$gameId,
        [string]$token,
        [string]$userId,
        [string]$playerLabel,
        [System.Collections.ArrayList]$messageList
    )
    
    Write-Host ""
    Write-Host "KROK: Laczenie WebSocket dla $playerLabel..." -ForegroundColor Yellow
    
    $encodedToken = [System.Web.HttpUtility]::UrlEncode($token)
    $wsUrlWithToken = $wsBaseUrl + "/ws/game/" + $gameId + "?token=" + $encodedToken
    Write-Host "  URL: $wsUrlWithToken" -ForegroundColor Gray
    
    $client = New-Object System.Net.WebSockets.ClientWebSocket
    $uri = New-Object System.Uri($wsUrlWithToken)
    $cancellationTokenSource = New-Object System.Threading.CancellationTokenSource
    
    try {
        $connectTask = $client.ConnectAsync($uri, $cancellationTokenSource.Token)
        
        try {
            $connectTask.Wait(10000) | Out-Null
        } catch {
            if ($connectTask.IsFaulted) {
                $innerEx = $connectTask.Exception.InnerException
                if ($innerEx) {
                    Write-Host "ERROR Blad laczenia WebSocket dla $playerLabel - $($innerEx.Message)" -ForegroundColor Red
                    Write-Host "  Typ bledu: $($innerEx.GetType().FullName)" -ForegroundColor Gray
                } else {
                    Write-Host "ERROR Blad laczenia WebSocket dla $playerLabel - $($_.Exception.Message)" -ForegroundColor Red
                }
            } else {
                Write-Host "ERROR Timeout podczas laczenia WebSocket dla $playerLabel" -ForegroundColor Red
            }
            return $null
        }
        
        if ($connectTask.IsFaulted) {
            $innerEx = $connectTask.Exception.InnerException
            if ($innerEx) {
                Write-Host "ERROR Blad laczenia WebSocket dla $playerLabel - $($innerEx.Message)" -ForegroundColor Red
                Write-Host "  Typ bledu: $($innerEx.GetType().FullName)" -ForegroundColor Gray
            }
            return $null
        }
        
        if ($client.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
            Write-Host "OK WebSocket polaczony dla $playerLabel" -ForegroundColor Green
            
            return @{Client = $client}
        } else {
            Write-Host "ERROR Nie mozna polaczyc WebSocket dla $playerLabel. Stan: $($client.State)" -ForegroundColor Red
            return $null
        }
    } catch {
        Write-Host "ERROR Blad laczenia WebSocket dla ${playerLabel} - $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.InnerException) {
            Write-Host "  Szczegoly: $($_.Exception.InnerException.Message)" -ForegroundColor Gray
        }
        return $null
    }
}

function Wait-ForWebSocketMessage {
    param(
        [System.Collections.ArrayList]$messageList,
        [string]$expectedType,
        [int]$timeoutSeconds = 5
    )
    
    $startTime = Get-Date
    while ($true) {
        if ($messageList.Count -gt 0) {
            foreach ($msg in $messageList) {
                try {
                    $msgObj = $msg | ConvertFrom-Json
                    if ($msgObj.type -eq $expectedType) {
                        return $msgObj
                    }
                } catch {
                }
            }
        }
        
        if (((Get-Date) - $startTime).TotalSeconds -gt $timeoutSeconds) {
            return $null
        }
        
        Start-Sleep -Milliseconds 100
    }
}

function Get-AllWebSocketMessages {
    param(
        [System.Collections.ArrayList]$messageList
    )
    
    $messages = @()
    foreach ($msg in $messageList) {
        try {
            $msgObj = $msg | ConvertFrom-Json
            $messages += $msgObj
        } catch {
        }
    }
    return $messages
}

function Compare-GameState {
    param(
        [object]$restState,
        [object]$wsState,
        [string]$source
    )
    
    Write-Host ""
    Write-Host "  Porownanie stanu z $source..." -ForegroundColor Cyan
    
    $differences = @()
    
    if ($restState.status -and $wsState.payload.status) {
        if ($restState.status -ne $wsState.payload.status) {
            $differences += "Status: REST=$($restState.status), WS=$($wsState.payload.status)"
        } else {
            Write-Host "    OK Status: $($restState.status)" -ForegroundColor Green
        }
    }
    
    if ($restState.boardState -and $wsState.payload.boardState) {
        $restBoard = $restState.boardState.state
        $wsBoard = $wsState.payload.boardState.state
        
        if ($restBoard -and $wsBoard) {
            $restArray = @($restBoard)
            $wsArray = @($wsBoard)
            
            if ($restArray.Count -eq $wsArray.Count) {
                $boardMatch = $true
                for ($i = 0; $i -lt $restArray.Count; $i++) {
                    $restRow = @($restArray[$i])
                    $wsRow = @($wsArray[$i])
                    if ($restRow.Count -ne $wsRow.Count) {
                        $boardMatch = $false
                        break
                    }
                    for ($j = 0; $j -lt $restRow.Count; $j++) {
                        $restCell = if ($restRow[$j]) { $restRow[$j] } else { "" }
                        $wsCell = if ($wsRow[$j]) { $wsRow[$j] } else { "" }
                        if ($restCell -ne $wsCell) {
                            $boardMatch = $false
                            $differences += "Board[$i][$j]: REST='$restCell', WS='$wsCell'"
                        }
                    }
                }
                if ($boardMatch) {
                    Write-Host "    OK Plansza zgodna" -ForegroundColor Green
                }
            } else {
                $differences += "Board rows count: REST=$($restArray.Count), WS=$($wsArray.Count)"
            }
        }
    }
    
    if ($restState.currentPlayerSymbol -and $wsState.payload.currentPlayerSymbol) {
        $restSymbol = if ($restState.currentPlayerSymbol) { $restState.currentPlayerSymbol.ToLower() } else { "" }
        $wsSymbol = if ($wsState.payload.currentPlayerSymbol) { $wsState.payload.currentPlayerSymbol.ToLower() } else { "" }
        if ($restSymbol -ne $wsSymbol) {
            $differences += "CurrentPlayerSymbol: REST=$restSymbol, WS=$wsSymbol"
        } else {
            Write-Host "    OK CurrentPlayerSymbol: $restSymbol" -ForegroundColor Green
        }
    }
    
    if ($differences.Count -gt 0) {
        Write-Host "    ROZNICE:" -ForegroundColor Red
        foreach ($diff in $differences) {
            Write-Host "      - $diff" -ForegroundColor Red
        }
        return $false
    }
    
    Write-Host "    OK Stan zgodny" -ForegroundColor Green
    return $true
}

Write-Host ""
$user1Result = Get-Or-Create-User -email $user1Email -password $user1Password -username $user1Username -userLabel "A"
$user1Token = $user1Result.Token
$user1Id = $user1Result.UserId

Write-Host ""
$user2Result = Get-Or-Create-User -email $user2Email -password $user2Password -username $user2Username -userLabel "B"
$user2Token = $user2Result.Token
$user2Id = $user2Result.UserId

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
Write-Host "KROK: Przygotowanie graczy przed wyzwaniem..." -ForegroundColor Yellow

try {
    $removeFromQueueResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/matching/queue" -Method DELETE -Headers @{"accept"="*/*"; "authorization"="Bearer $user1Token"} -ErrorAction SilentlyContinue
    Write-Host "OK Gracz A usuniety z kolejki matchmaking" -ForegroundColor Green
} catch {
    $statusCode = $null
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
    }
    if ($statusCode -eq 404) {
        Write-Host "INFO Gracz A nie byl w kolejce matchmaking" -ForegroundColor Gray
    }
}

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
    }
}

End-ActivePvpGames -token $user1Token -playerLabel "A"
End-ActivePvpGames -token $user2Token -playerLabel "B"

Write-Host ""
Write-Host "KROK: Tworzenie gry PvP..." -ForegroundColor Yellow

$challengeBody = @{
    boardSize = "THREE"
} | ConvertTo-Json

try {
    $challengeResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/matching/challenge/$user2Id" -Method POST -Headers @{"accept"="*/*"; "authorization"="Bearer $user1Token"; "content-type"="application/json"} -Body $challengeBody -ContentType "application/json" -ErrorAction Stop
    $challengeData = $challengeResponse.Content | ConvertFrom-Json
    $gameId = $challengeData.gameId
    Write-Host "OK Gra PvP utworzona: ID $gameId" -ForegroundColor Green
    Write-Host "  Status: $($challengeData.status)" -ForegroundColor Gray
} catch {
    Write-Host "ERROR Blad tworzenia gry PvP: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$user1Ws = Connect-WebSocket -gameId $gameId -token $user1Token -userId $user1Id -playerLabel "Gracz A" -messageList $user1WsMessages
$user2Ws = Connect-WebSocket -gameId $gameId -token $user2Token -userId $user2Id -playerLabel "Gracz B" -messageList $user2WsMessages

if (-not $user1Ws -or -not $user2Ws) {
    Write-Host "ERROR Nie mozna polaczyc WebSocket dla obu graczy" -ForegroundColor Red
    exit 1
}

Start-Sleep -Seconds 3

Write-Host ""
Write-Host "KROK: Odbieranie poczatkowego stanu z WebSocket..." -ForegroundColor Yellow

if ($user1Ws -and $user1Ws.Client) {
    Poll-WebSocketMessages -client $user1Ws.Client -messageList $user1WsMessages -maxAttempts 50
}

if ($user2Ws -and $user2Ws.Client) {
    Poll-WebSocketMessages -client $user2Ws.Client -messageList $user2WsMessages -maxAttempts 50
}

$user1Messages = Get-AllWebSocketMessages -messageList $user1WsMessages
$user2Messages = Get-AllWebSocketMessages -messageList $user2WsMessages

try {
    Start-Sleep -Milliseconds 500
    $user1MessagesResponse = Invoke-WebRequest -Uri "$baseUrl/api/test/websocket/games/$gameId/users/$user1Id/messages" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $user1Token"} -ErrorAction SilentlyContinue
    $user1MessagesData = $user1MessagesResponse.Content | ConvertFrom-Json
    $user1WsMessagesFromApi = $user1MessagesData.messages
    $user1Messages = @($user1Messages) + @($user1WsMessagesFromApi)
    
    $user2MessagesResponse = Invoke-WebRequest -Uri "$baseUrl/api/test/websocket/games/$gameId/users/$user2Id/messages" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $user2Token"} -ErrorAction SilentlyContinue
    $user2MessagesData = $user2MessagesResponse.Content | ConvertFrom-Json
    $user2WsMessagesFromApi = $user2MessagesData.messages
    $user2Messages = @($user2Messages) + @($user2WsMessagesFromApi)
} catch {
}


$user1InitialState = $user1Messages | Where-Object { $_.type -eq "GAME_UPDATE" } | Select-Object -First 1
$user2InitialState = $user2Messages | Where-Object { $_.type -eq "GAME_UPDATE" } | Select-Object -First 1

if ($user1InitialState) {
    Write-Host "OK Gracz A otrzymal GAME_UPDATE" -ForegroundColor Green
} else {
    Write-Host "WARN Gracz A nie otrzymal GAME_UPDATE" -ForegroundColor Yellow
}

if ($user2InitialState) {
    Write-Host "OK Gracz B otrzymal GAME_UPDATE" -ForegroundColor Green
} else {
    Write-Host "WARN Gracz B nie otrzymal GAME_UPDATE" -ForegroundColor Yellow
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
        
        Start-Sleep -Seconds 3
        
        Write-Host ""
        Write-Host "KROK: Pobieranie stanu gry z REST API..." -ForegroundColor Yellow
        $gameResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games/$gameId" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $token"} -ErrorAction Stop
        $gameData = $gameResponse.Content | ConvertFrom-Json
        
        Write-Host ""
        Write-Host "KROK: Sprawdzanie wiadomosci z WebSocket..." -ForegroundColor Yellow
        
        Start-Sleep -Milliseconds 500
        
        try {
            $user1MessagesResponse = Invoke-WebRequest -Uri "$baseUrl/api/test/websocket/games/$gameId/users/$user1Id/messages" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $user1Token"} -ErrorAction Stop
            $user1MessagesData = $user1MessagesResponse.Content | ConvertFrom-Json
            $user1WsMessagesFromApi = $user1MessagesData.messages
            
            $user2MessagesResponse = Invoke-WebRequest -Uri "$baseUrl/api/test/websocket/games/$gameId/users/$user2Id/messages" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $user2Token"} -ErrorAction Stop
            $user2MessagesData = $user2MessagesResponse.Content | ConvertFrom-Json
            $user2WsMessagesFromApi = $user2MessagesData.messages
            
            Write-Host "  Gracz A otrzymal $($user1WsMessagesFromApi.Count) wiadomosci z REST API" -ForegroundColor Gray
            Write-Host "  Gracz B otrzymal $($user2WsMessagesFromApi.Count) wiadomosci z REST API" -ForegroundColor Gray
            
            $allUser1Messages = @($user1WsMessagesFromApi)
            $allUser2Messages = @($user2WsMessagesFromApi)
            
            $moveAccepted = $null
            $opponentMove = $null
            
            $latestMoveAccepted = $null
            $latestMoveAcceptedMoveId = 0
            $latestOpponentMove = $null
            $latestOpponentMoveIndex = -1
            
            foreach ($msg in $allUser1Messages) {
                if ($msg.type -eq "MOVE_ACCEPTED" -and $msg.payload.moveId) {
                    $moveId = $msg.payload.moveId
                    if ($moveId -gt $latestMoveAcceptedMoveId) {
                        $latestMoveAccepted = $msg
                        $latestMoveAcceptedMoveId = $moveId
                    }
                }
                if ($msg.type -eq "OPPONENT_MOVE") {
                    $msgIndex = $allUser1Messages.IndexOf($msg)
                    if ($msgIndex -gt $latestOpponentMoveIndex) {
                        $latestOpponentMove = $msg
                        $latestOpponentMoveIndex = $msgIndex
                    }
                }
            }
            
            foreach ($msg in $allUser2Messages) {
                if ($msg.type -eq "MOVE_ACCEPTED" -and $msg.payload.moveId) {
                    $moveId = $msg.payload.moveId
                    if ($moveId -gt $latestMoveAcceptedMoveId) {
                        $latestMoveAccepted = $msg
                        $latestMoveAcceptedMoveId = $moveId
                    }
                }
                if ($msg.type -eq "OPPONENT_MOVE") {
                    $msgIndex = $allUser2Messages.IndexOf($msg)
                    if ($msgIndex -gt $latestOpponentMoveIndex) {
                        $latestOpponentMove = $msg
                        $latestOpponentMoveIndex = $msgIndex
                    }
                }
            }
            
            $moveAccepted = $latestMoveAccepted
            $opponentMove = $latestOpponentMove
            
            if ($moveAccepted) {
                Write-Host "  OK Znaleziono MOVE_ACCEPTED" -ForegroundColor Green
                Compare-GameState -restState $gameData -wsState $moveAccepted -source "MOVE_ACCEPTED"
            } else {
                Write-Host "  WARN Nie znaleziono MOVE_ACCEPTED" -ForegroundColor Yellow
            }
            
            if ($opponentMove) {
                Write-Host "  OK Znaleziono OPPONENT_MOVE" -ForegroundColor Green
                Compare-GameState -restState $gameData -wsState $opponentMove -source "OPPONENT_MOVE"
            } else {
                Write-Host "  WARN Nie znaleziono OPPONENT_MOVE" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "  ERROR Blad pobierania wiadomosci z REST API: $($_.Exception.Message)" -ForegroundColor Red
            Write-Host "  WARN Nie mozna pobrac wiadomosci z REST API" -ForegroundColor Yellow
        }
        
        return $moveData
    } catch {
        Write-Host "ERROR Błąd wykonania ruchu: $($_.Exception.Message)" -ForegroundColor Red
        throw
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
$maxAttempts = 20
$attempts = 0

while ($attempts -lt $maxAttempts -and $moveIndex -lt $moveSequence.Length) {
    $gameStateResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/games/$gameId" -Method GET -Headers @{"accept"="*/*"; "authorization"="Bearer $user1Token"} -ErrorAction Stop
    $gameState = $gameStateResponse.Content | ConvertFrom-Json
    
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
    
    $currentPlayerSymbol = if ($gameState.currentPlayerSymbol) { $gameState.currentPlayerSymbol.ToLower() } else { "x" }
    
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
            Make-Move -token $token -gameId $gameId -row $row -col $col -playerSymbol "x" -playerLabel $playerLabel | Out-Null
            $moveIndex++
        } catch {
            $statusCode = $null
            if ($_.Exception.Response) {
                $statusCode = $_.Exception.Response.StatusCode.value__
            }
            if ($statusCode -eq 403) {
                Write-Host "INFO Nie kolej gracza (403)" -ForegroundColor Yellow
            }
            $moveIndex++
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
            Make-Move -token $token -gameId $gameId -row $row -col $col -playerSymbol "o" -playerLabel $playerLabel | Out-Null
            $moveIndex++
        } catch {
            $statusCode = $null
            if ($_.Exception.Response) {
                $statusCode = $_.Exception.Response.StatusCode.value__
            }
            if ($statusCode -eq 403) {
                Write-Host "INFO Nie kolej gracza (403)" -ForegroundColor Yellow
            }
            $moveIndex++
        }
    } else {
        $moveIndex++
    }
    
    $attempts++
    Start-Sleep -Milliseconds 500
}

if ($user1Ws -and $user1Ws.Client) {
    try {
        $closeTokenSource = New-Object System.Threading.CancellationTokenSource
        $closeTokenSource.CancelAfter(5000)
        $user1Ws.Client.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, "Test completed", $closeTokenSource.Token).Wait(5000)
    } catch {
    }
}

if ($user2Ws -and $user2Ws.Client) {
    try {
        $closeTokenSource = New-Object System.Threading.CancellationTokenSource
        $closeTokenSource.CancelAfter(5000)
        $user2Ws.Client.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, "Test completed", $closeTokenSource.Token).Wait(5000)
    } catch {
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test zakonczony" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan


