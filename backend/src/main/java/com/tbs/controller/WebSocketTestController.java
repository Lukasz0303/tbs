package com.tbs.controller;

import com.tbs.dto.websocket.BaseWebSocketMessage;
import com.tbs.service.WebSocketMessageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test/websocket")
@Tag(name = "WebSocket Test", description = "Test endpoints for WebSocket messages (testing only, not for production)")
public class WebSocketTestController {

    private final WebSocketMessageStorageService messageStorageService;

    public WebSocketTestController(WebSocketMessageStorageService messageStorageService) {
        this.messageStorageService = messageStorageService;
    }

    @GetMapping("/games/{gameId}/messages")
    @Operation(
            summary = "Get WebSocket messages for a game (TEST ONLY)",
            description = "Returns all WebSocket messages sent to a specific game. This endpoint is for testing purposes only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Game not found")
    })
    public ResponseEntity<WebSocketMessagesResponse> getMessagesForGame(@PathVariable Long gameId) {
        List<BaseWebSocketMessage> messages = messageStorageService.getMessagesForGame(gameId);
        return ResponseEntity.ok(new WebSocketMessagesResponse(gameId, messages));
    }

    @GetMapping("/games/{gameId}/users/{userId}/messages")
    @Operation(
            summary = "Get WebSocket messages for a specific user in a game (TEST ONLY)",
            description = "Returns all WebSocket messages sent to a specific user in a game. This endpoint is for testing purposes only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Game or user not found")
    })
    public ResponseEntity<WebSocketMessagesResponse> getMessagesForUser(
            @PathVariable Long gameId,
            @PathVariable Long userId
    ) {
        List<BaseWebSocketMessage> messages = messageStorageService.getMessagesForUser(gameId, userId);
        return ResponseEntity.ok(new WebSocketMessagesResponse(gameId, messages));
    }

    @DeleteMapping("/games/{gameId}/messages")
    @Operation(
            summary = "Clear WebSocket messages for a game (TEST ONLY)",
            description = "Clears all stored WebSocket messages for a specific game. This endpoint is for testing purposes only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages cleared successfully")
    })
    public ResponseEntity<Void> clearMessagesForGame(@PathVariable Long gameId) {
        messageStorageService.clearMessagesForGame(gameId);
        return ResponseEntity.ok().build();
    }

    public record WebSocketMessagesResponse(Long gameId, List<BaseWebSocketMessage> messages) {}
}

