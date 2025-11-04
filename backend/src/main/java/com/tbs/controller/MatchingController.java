package com.tbs.controller;

import com.tbs.dto.matchmaking.*;
import com.tbs.enums.BoardSize;
import com.tbs.service.AuthenticationService;
import com.tbs.service.MatchmakingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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

@RestController
@RequestMapping("/api/matching")
@Tag(name = "Matchmaking", description = "API endpoints for matchmaking and player challenges")
@PreAuthorize("isAuthenticated()")
public class MatchingController {

    private final MatchmakingService matchmakingService;
    private final AuthenticationService authenticationService;

    public MatchingController(MatchmakingService matchmakingService, AuthenticationService authenticationService) {
        this.matchmakingService = matchmakingService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/queue")
    @Operation(
            summary = "Get matchmaking queue status",
            description = "Retrieves the list of all players in the matchmaking queue with their status " +
                    "(waiting, matched, playing). Optionally filtered by board size."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Queue status retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid board size parameter"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @Parameter(description = "Filter by board size (3, 4, or 5)", schema = @Schema(type = "string", allowableValues = {"THREE", "FOUR", "FIVE", "3", "4", "5"}))
            @RequestParam(required = false) String boardSizeParam
    ) {
        BoardSize boardSize = null;
        if (boardSizeParam != null && !boardSizeParam.trim().isEmpty()) {
            try {
                boardSize = BoardSize.fromValue(boardSizeParam);
            } catch (IllegalArgumentException e) {
                throw new com.tbs.exception.BadRequestException("Invalid board size parameter: " + boardSizeParam);
            }
        }
        return ResponseEntity.ok(matchmakingService.getQueueStatus(boardSize));
    }

    @PostMapping("/queue")
    @Operation(
            summary = "Join matchmaking queue",
            description = "Adds the current user to the matchmaking queue for the specified board size. " +
                    "The system will automatically try to match players and create a game."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added to queue or match found"),
            @ApiResponse(responseCode = "400", description = "Invalid board size parameter"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "409", description = "User already in queue or has active PvP game")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MatchmakingQueueResponse> addToQueue(@Valid @RequestBody MatchmakingQueueRequest request) {
        Long userId = authenticationService.getCurrentUserId();
        MatchmakingQueueResponse response = matchmakingService.addToQueue(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/queue")
    @Operation(
            summary = "Leave matchmaking queue",
            description = "Removes the current user from the matchmaking queue"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully removed from queue"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "User is not in the matchmaking queue")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<LeaveQueueResponse> removeFromQueue() {
        Long userId = authenticationService.getCurrentUserId();
        LeaveQueueResponse response = matchmakingService.removeFromQueue(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/challenge/{userId}")
    @Operation(
            summary = "Challenge a specific player",
            description = "Creates a direct challenge to a specific player for a game with the specified board size"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Challenge created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid board size parameter or userId"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Cannot challenge yourself"),
            @ApiResponse(responseCode = "404", description = "Challenged user does not exist"),
            @ApiResponse(responseCode = "409", description = "Challenged user is unavailable")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ChallengeResponse> createChallenge(
            @PathVariable Long userId,
            @Valid @RequestBody ChallengeRequest request
    ) {
        Long challengerId = authenticationService.getCurrentUserId();
        ChallengeResponse response = matchmakingService.createDirectChallenge(challengerId, userId, request);
        String locationPath = "/api/games/" + response.gameId();
        URI locationUri = URI.create(locationPath);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(locationUri)
                .body(response);
    }
}

