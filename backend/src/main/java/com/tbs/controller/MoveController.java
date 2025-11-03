package com.tbs.controller;

import com.tbs.dto.move.BotMoveResponse;
import com.tbs.dto.move.CreateMoveRequest;
import com.tbs.dto.move.CreateMoveResponse;
import com.tbs.dto.move.MoveListItem;
import com.tbs.service.AuthenticationService;
import com.tbs.service.MoveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/games")
@Tag(name = "Moves", description = "API endpoints for game moves management")
@PreAuthorize("isAuthenticated()")
public class MoveController {

    private final MoveService moveService;
    private final AuthenticationService authenticationService;

    public MoveController(MoveService moveService, AuthenticationService authenticationService) {
        this.moveService = moveService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/{gameId}/moves")
    @Operation(
            summary = "Get moves history",
            description = "Retrieves all moves for a specific game"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Moves retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "You are not a participant of this game"),
            @ApiResponse(responseCode = "404", description = "Game not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<MoveListItem>> getMoves(@PathVariable Long gameId) {
        Long userId = authenticationService.getCurrentUserId();
        List<MoveListItem> response = moveService.getMovesByGameId(gameId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gameId}/moves")
    @Operation(
            summary = "Create move",
            description = "Creates a new move in a game"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Move created successfully"),
            @ApiResponse(responseCode = "400", description = "Game is not in progress"),
            @ApiResponse(responseCode = "403", description = "You are not a participant or not the current player"),
            @ApiResponse(responseCode = "404", description = "Game not found"),
            @ApiResponse(responseCode = "422", description = "Invalid move")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreateMoveResponse> createMove(
            @PathVariable Long gameId,
            @Valid @RequestBody CreateMoveRequest request
    ) {
        Long userId = authenticationService.getCurrentUserId();
        CreateMoveResponse response = moveService.createMove(gameId, request, userId);
        URI locationUri = URI.create("/api/games/" + gameId + "/moves/" + response.moveId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(locationUri)
                .body(response);
    }

    @PostMapping("/{gameId}/bot-move")
    @Operation(
            summary = "Create bot move",
            description = "Creates a bot move in a vs_bot game (internal endpoint)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bot move created successfully"),
            @ApiResponse(responseCode = "400", description = "Game is not vs_bot or not in progress"),
            @ApiResponse(responseCode = "403", description = "Only player1 can trigger bot moves"),
            @ApiResponse(responseCode = "404", description = "Game not found"),
            @ApiResponse(responseCode = "500", description = "Failed to generate bot move")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BotMoveResponse> createBotMove(@PathVariable Long gameId) {
        Long userId = authenticationService.getCurrentUserId();
        BotMoveResponse response = moveService.createBotMove(gameId, userId);
        return ResponseEntity.ok(response);
    }
}

