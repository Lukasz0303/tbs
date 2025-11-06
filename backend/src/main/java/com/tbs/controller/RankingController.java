package com.tbs.controller;

import com.tbs.dto.ranking.RankingAroundResponse;
import com.tbs.dto.ranking.RankingDetailResponse;
import com.tbs.dto.ranking.RankingListResponse;
import com.tbs.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rankings")
@Tag(name = "Rankings", description = "API endpoints for player rankings")
@Validated
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    @Operation(
            summary = "Get rankings list",
            description = "Retrieves a paginated list of player rankings. Can use standard pagination (page, size) or startRank parameter."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rankings list retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<RankingListResponse> getRankings(
            @Parameter(description = "Page number (0-based)", example = "0")
            @PageableDefault(size = 50, sort = "rankPosition", direction = Sort.Direction.ASC) Pageable pageable,
            @Parameter(description = "Alternative pagination - starting rank position", example = "1")
            @RequestParam(required = false)
            @Min(value = 1, message = "Start rank must be at least 1")
            Integer startRank,
            @Parameter(description = "Page size (max 100)", example = "50")
            @RequestParam(required = false)
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100")
            Integer size
    ) {
        if (startRank != null && pageable.getPageNumber() > 0) {
            throw new IllegalArgumentException("Cannot use both startRank and page number. Use either startRank or standard pagination.");
        }

        Pageable adjustedPageable = size != null
                ? Pageable.ofSize(size).withPage(pageable.getPageNumber())
                : pageable;

        RankingListResponse response = rankingService.getRankings(adjustedPageable, startRank);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Get user ranking details",
            description = "Retrieves detailed ranking information for a specific user. Returns 404 for guest users."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User ranking retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID"),
            @ApiResponse(responseCode = "404", description = "User not found or user is a guest"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<RankingDetailResponse> getUserRanking(
            @Parameter(description = "User ID", required = true, example = "123")
            @PathVariable
            @Positive(message = "User ID must be positive")
            Long userId
    ) {
        RankingDetailResponse response = rankingService.getUserRanking(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/around/{userId}")
    @Operation(
            summary = "Get rankings around user",
            description = "Retrieves rankings around a specific user position. Returns players before and after the user. Returns 404 for guest users."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rankings around user retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "User not found or user is a guest"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<RankingAroundResponse> getRankingsAround(
            @Parameter(description = "User ID", required = true, example = "123")
            @PathVariable
            @Positive(message = "User ID must be positive")
            Long userId,
            @Parameter(description = "Number of players before and after user (default: 5, max: 10)", example = "5")
            @RequestParam(required = false, defaultValue = "5")
            @Min(value = 1, message = "Range must be at least 1")
            @Max(value = 10, message = "Range must not exceed 10")
            Integer range
    ) {
        RankingAroundResponse response = rankingService.getRankingsAround(userId, range);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cache")
    @Operation(
            summary = "Clear rankings cache",
            description = "Clears all rankings cache entries from Redis. Useful after schema changes or cache issues."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache cleared successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> clearCache() {
        rankingService.clearRankingsCache();
        return ResponseEntity.ok().build();
    }
}

