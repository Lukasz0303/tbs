package com.tbs.controller;

import com.tbs.dto.websocket.WebSocketDocumentationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/websocket")
@Tag(name = "WebSocket", description = "WebSocket documentation and endpoints")
public class WebSocketDocumentationController {

    @Value("${server.port:8080}")
    private String serverPort;

    @GetMapping("/docs")
    @Operation(
            summary = "Get WebSocket documentation",
            description = "Returns comprehensive documentation for WebSocket endpoint /ws/game/{gameId}",
            tags = {"WebSocket"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documentation retrieved successfully")
    })
    public ResponseEntity<WebSocketDocumentationResponse> getWebSocketDocumentation() {
        int port = Integer.parseInt(serverPort);
        String baseUrl = "http://localhost:" + port;
        String wsUrl = "ws://localhost:" + port;

        WebSocketDocumentationResponse documentation = new WebSocketDocumentationResponse(
                "/ws/game/{gameId}",
                "WebSocket",
                wsUrl + "/ws/game/{gameId}",
                "game-protocol",
                new WebSocketDocumentationResponse.ConnectionInfo(
                        wsUrl + "/ws/game/{gameId}",
                        "WebSocket",
                        "game-protocol",
                        "const ws = new WebSocket('" + wsUrl + "/ws/game/42', 'game-protocol');\n" +
                                "ws.setRequestHeader('Authorization', 'Bearer <JWT_TOKEN>');"
                ),
                new WebSocketDocumentationResponse.AuthenticationInfo(
                        "JWT Bearer Token",
                        "Authorization: Bearer <token>",
                        "JWT token wydany po poprawnym logowaniu/rejestracji"
                ),
                new WebSocketDocumentationResponse.RequirementsInfo(
                        true,
                        "PVP",
                        "IN_PROGRESS lub WAITING",
                        "Gracz musi być uczestnikiem gry (player1_id lub player2_id), typ gry musi być PVP, status gry musi być IN_PROGRESS lub WAITING"
                ),
                List.of(
                        new WebSocketDocumentationResponse.MessageTypeInfo(
                                "MOVE",
                                "Wysłanie ruchu gracza do serwera",
                                "{\"type\":\"MOVE\",\"payload\":{\"row\":0,\"col\":0,\"playerSymbol\":\"x\"}}",
                                List.of(
                                        "row: Integer >= 0, < board_size",
                                        "col: Integer >= 0, < board_size",
                                        "playerSymbol: Enum ('x' lub 'o')",
                                        "Pole musi być puste",
                                        "Gracz musi być aktualnym graczem"
                                ),
                                List.of(
                                        "Ruch jest zapisywany w bazie danych",
                                        "Stan gry jest aktualizowany",
                                        "Przeciwnik otrzymuje OPPONENT_MOVE",
                                        "Timer jest resetowany"
                                )
                        ),
                        new WebSocketDocumentationResponse.MessageTypeInfo(
                                "SURRENDER",
                                "Poddanie gry przez gracza",
                                "{\"type\":\"SURRENDER\",\"payload\":{}}",
                                List.of(),
                                List.of(
                                        "Przeciwnik automatycznie wygrywa",
                                        "Gra kończy się ze statusem FINISHED",
                                        "Zwycięzca otrzymuje +1000 pkt"
                                )
                        ),
                        new WebSocketDocumentationResponse.MessageTypeInfo(
                                "PING",
                                "Keep-alive - utrzymanie połączenia",
                                "{\"type\":\"PING\",\"payload\":{\"timestamp\":\"2024-01-20T15:30:00Z\"}}",
                                List.of(),
                                List.of(
                                        "Serwer odpowiada PONG",
                                        "Ostatnia aktywność jest aktualizowana",
                                        "Timeout połączenia jest resetowany"
                                )
                        )
                ),
                List.of(
                        new WebSocketDocumentationResponse.MessageTypeInfo(
                                "MOVE_ACCEPTED",
                                "Ruch został zaakceptowany i zapisany",
                                "{\"type\":\"MOVE_ACCEPTED\",\"payload\":{\"moveId\":123,\"row\":0,\"col\":0,\"playerSymbol\":\"x\",\"currentPlayerSymbol\":\"o\",\"nextMoveAt\":\"2024-01-20T15:30:10Z\"}}",
                                List.of(),
                                List.of()
                        ),
                        new WebSocketDocumentationResponse.MessageTypeInfo(
                                "MOVE_REJECTED",
                                "Ruch został odrzucony z powodu błędu walidacji",
                                "{\"type\":\"MOVE_REJECTED\",\"payload\":{\"reason\":\"Invalid move: cell is already occupied\",\"code\":\"MOVE_INVALID_OCCUPIED\"}}",
                                List.of(),
                                List.of(
                                        "Kody błędów: MOVE_INVALID_OCCUPIED, MOVE_INVALID_OUT_OF_BOUNDS, MOVE_INVALID_NOT_YOUR_TURN, MOVE_INVALID_GAME_NOT_ACTIVE, MOVE_INVALID_TIMEOUT"
                                )
                        ),
                        new WebSocketDocumentationResponse.MessageTypeInfo(
                                "OPPONENT_MOVE",
                                "Przeciwnik wykonał ruch",
                                "{\"type\":\"OPPONENT_MOVE\",\"payload\":{\"row\":1,\"col\":1,\"playerSymbol\":\"o\",\"currentPlayerSymbol\":\"x\",\"nextMoveAt\":\"2024-01-20T15:30:20Z\"}}",
                                List.of(),
                                List.of()
                        ),
                        new WebSocketDocumentationResponse.MessageTypeInfo(
                                "GAME_UPDATE",
                                "Status gry się zmienił (np. gra zakończona)",
                                "{\"type\":\"GAME_UPDATE\",\"payload\":{\"gameId\":42,\"status\":\"finished\",\"winner\":{\"userId\":10,\"username\":\"player1\"}}}",
                                List.of(),
                                List.of()
                        ),
                        new WebSocketDocumentationResponse.MessageTypeInfo(
                                "TIMER_UPDATE",
                                "Aktualizacja timera - pozostały czas na ruch",
                                "{\"type\":\"TIMER_UPDATE\",\"payload\":{\"remainingSeconds\":8,\"currentPlayerSymbol\":\"x\"}}",
                                List.of(),
                                List.of(
                                        "Wysyłane co sekundę",
                                        "Timeout po 10 sekundach bez ruchu"
                                )
                        ),
                        new WebSocketDocumentationResponse.MessageTypeInfo(
                                "GAME_ENDED",
                                "Gra zakończona (wywoływane przed zamknięciem połączenia)",
                                "{\"type\":\"GAME_ENDED\",\"payload\":{\"gameId\":42,\"status\":\"finished\",\"winner\":{\"userId\":10,\"username\":\"player1\"},\"totalMoves\":5}}",
                                List.of(),
                                List.of()
                        ),
                        new WebSocketDocumentationResponse.MessageTypeInfo(
                                "PONG",
                                "Odpowiedź keep-alive",
                                "{\"type\":\"PONG\",\"payload\":{\"timestamp\":\"2024-01-20T15:30:00Z\"}}",
                                List.of(),
                                List.of()
                        )
                ),
                new WebSocketDocumentationResponse.FlowInfo(
                        "1. Klient wysyła żądanie WebSocket z JWT tokenem\n" +
                                "2. Serwer weryfikuje token i uczestnictwo w grze\n" +
                                "3. Połączenie jest akceptowane\n" +
                                "4. Serwer wysyła GAME_UPDATE z aktualnym stanem gry",
                        "1. Klient wysyła MOVE z row, col, playerSymbol\n" +
                                "2. Serwer waliduje ruch\n" +
                                "3. Jeśli poprawny: zapisuje w bazie, wysyła MOVE_ACCEPTED do nadawcy i OPPONENT_MOVE do przeciwnika\n" +
                                "4. Jeśli niepoprawny: wysyła MOVE_REJECTED",
                        "1. Klient wysyła SURRENDER\n" +
                                "2. Serwer ustawia przeciwnika jako zwycięzcę\n" +
                                "3. Gra kończy się ze statusem FINISHED\n" +
                                "4. Wysyłane GAME_ENDED do obu graczy\n" +
                                "5. Połączenia są zamykane",
                        "1. Timer 10 sekund na ruch\n" +
                                "2. TIMER_UPDATE wysyłane co sekundę\n" +
                                "3. Po timeout: gra kończy się, przeciwnik wygrywa",
                        "1. Klient wysyła PING co 30 sekund\n" +
                                "2. Serwer odpowiada PONG\n" +
                                "3. Timeout połączenia: 60 sekund bez PING/PONG",
                        "1. Okno rekonnekcji: 20 sekund\n" +
                                "2. Jeśli przeciwnik wraca w czasie okna: gra kontynuowana\n" +
                                "3. Jeśli nie: gra kończy się, przeciwnik wygrywa"
                ),
                new WebSocketDocumentationResponse.ErrorHandlingInfo(
                        List.of(
                                "MOVE_INVALID_OCCUPIED - Pole jest zajęte",
                                "MOVE_INVALID_OUT_OF_BOUNDS - Ruch poza planszą",
                                "MOVE_INVALID_NOT_YOUR_TURN - Nie twoja tura",
                                "MOVE_INVALID_GAME_NOT_ACTIVE - Gra nie jest aktywna",
                                "MOVE_INVALID_TIMEOUT - Przekroczono limit czasu",
                                "CONNECTION_ERROR - Błąd połączenia",
                                "AUTHENTICATION_ERROR - Błąd autentykacji",
                                "AUTHORIZATION_ERROR - Brak uprawnień"
                        ),
                        "20 sekund",
                        "10 sekund na ruch, 60 sekund na połączenie"
                ),
                baseUrl + "/api/websocket/docs lub plik .ai/implementation-plans/websocket/ws-game-gameId.md"
        );

        return ResponseEntity.ok(documentation);
    }
}

