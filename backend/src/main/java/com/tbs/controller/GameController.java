package com.tbs.controller;

import com.tbs.dto.game.*;
import com.tbs.enums.GameStatus;
import com.tbs.enums.GameType;
import com.tbs.service.AuthenticationService;
import com.tbs.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
@Tag(name = "Games", description = "API endpoints for game management")
@PreAuthorize("isAuthenticated()")
public class GameController {

    private final GameService gameService;
    private final AuthenticationService authenticationService;

    public GameController(GameService gameService, AuthenticationService authenticationService) {
        this.gameService = gameService;
        this.authenticationService = authenticationService;
    }

    @PostMapping
    @Operation(summary = "Create new game", description = "Creates a new game (vs_bot or pvp)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Game created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid game parameters or missing botDifficulty for VS_BOT games"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "422", description = "Validation errors")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreateGameResponse> createGame(@Valid @RequestBody CreateGameRequest request) {
        Long userId = authenticationService.getCurrentUserId();
        CreateGameResponse response = gameService.createGame(request, userId);
        URI locationUri = URI.create("/api/v1/games/" + response.gameId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(locationUri)
                .body(response);
    }

    @GetMapping
    @Operation(summary = "Get games list", description = "Retrieves a paginated list of games for the current user with filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Games list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<GameListResponse> getGames(
            @RequestParam(required = false) List<GameStatus> status,
            @RequestParam(required = false) GameType gameType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = authenticationService.getCurrentUserId();
        GameListResponse response = gameService.getGames(userId, status, gameType, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{gameId}")
    @Operation(
            summary = "Get game details",
            description = "Retrieves detailed information about a game including board state and move history"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game details retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "You are not a participant of this game"),
            @ApiResponse(responseCode = "404", description = "Game not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<GameDetailResponse> getGameDetail(@PathVariable Long gameId) {
        Long userId = authenticationService.getCurrentUserId();
        GameDetailResponse response = gameService.getGameDetail(gameId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{gameId}/board")
    @Operation(summary = "Get board state", description = "Retrieves the current board state for a game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Board state retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "You are not a participant of this game"),
            @ApiResponse(responseCode = "404", description = "Game not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BoardStateResponse> getBoardState(@PathVariable Long gameId) {
        Long userId = authenticationService.getCurrentUserId();
        BoardStateResponse response = gameService.getBoardState(gameId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{gameId}/status")
    @Operation(summary = "Update game status", description = "Updates the status of a game (surrender, abandon)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game status updated successfully"),
            @ApiResponse(responseCode = "403", description = "You are not a participant or invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Game not found"),
            @ApiResponse(responseCode = "422", description = "Invalid status transition")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UpdateGameStatusResponse> updateGameStatus(
            @PathVariable Long gameId,
            @Valid @RequestBody UpdateGameStatusRequest request
    ) {
        Long userId = authenticationService.getCurrentUserId();
        UpdateGameStatusResponse response = gameService.updateGameStatus(gameId, request.status(), userId);
        return ResponseEntity.ok(response);
    }
}

