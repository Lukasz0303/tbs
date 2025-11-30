package com.tbs.controller;

import com.tbs.dto.user.LastSeenResponse;
import com.tbs.dto.user.UpdateUserRequest;
import com.tbs.dto.user.UpdateUserResponse;
import com.tbs.dto.user.UserProfileResponse;
import com.tbs.exception.ForbiddenException;
import com.tbs.exception.RateLimitExceededException;
import com.tbs.service.AuthenticationService;
import com.tbs.service.IpAddressService;
import com.tbs.service.RateLimitingService;
import com.tbs.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "API endpoints for user profile management")
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final RateLimitingService rateLimitingService;
    private final IpAddressService ipAddressService;
    private final int profileRateLimit;
    private final int lastSeenRateLimit;
    private final int updateRateLimit;

    public UserController(
            UserService userService,
            AuthenticationService authenticationService,
            RateLimitingService rateLimitingService,
            IpAddressService ipAddressService,
            @Value("${app.rate-limit.profile:100}") int profileRateLimit,
            @Value("${app.rate-limit.last-seen:30}") int lastSeenRateLimit,
            @Value("${app.rate-limit.update:30}") int updateRateLimit
    ) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.rateLimitingService = rateLimitingService;
        this.ipAddressService = ipAddressService;
        this.profileRateLimit = profileRateLimit;
        this.lastSeenRateLimit = lastSeenRateLimit;
        this.updateRateLimit = updateRateLimit;
    }

    private void checkRateLimit(String key, int limit, Duration window, String errorMessage) {
        if (!rateLimitingService.isAllowed(key, limit, window)) {
            long remaining = rateLimitingService.getRemainingRequests(key, limit);
            long timeToReset = rateLimitingService.getTimeToReset(key).getSeconds();
            throw new RateLimitExceededException(errorMessage, remaining, timeToReset);
        }
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Get user profile",
            description = "Retrieves the profile of a user by ID. Public profiles of registered users are accessible to all registered users. Guest profiles are only accessible to the owner."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Access denied to guest profile"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded - maximum 100 requests per minute")
    })
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable @Min(1) Long userId,
            HttpServletRequest request
    ) {
        String clientIp = ipAddressService.getClientIpAddress(request);
        String rateLimitKey = "users:profile:" + clientIp;
        checkRateLimit(rateLimitKey, profileRateLimit, Duration.ofMinutes(1), 
                "Rate limit exceeded. Maximum " + profileRateLimit + " requests per minute.");

        Long currentUserId = authenticationService.getCurrentUserIdOrNull();
        UserProfileResponse profile = userService.getUserProfile(userId, currentUserId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/{userId}/last-seen")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Update last seen timestamp",
            description = "Updates the last seen timestamp for the authenticated user. This endpoint is used for matchmaking to identify active players. Only the owner can update their own timestamp."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Last seen updated successfully",
                    content = @Content(schema = @Schema(implementation = LastSeenResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - can only update own timestamp"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded - maximum 30 requests per minute")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<LastSeenResponse> updateLastSeen(@PathVariable @Min(1) Long userId) {
        Long currentUserId = authenticationService.getCurrentUserId();

        String rateLimitKey = "users:last-seen:" + currentUserId;
        checkRateLimit(rateLimitKey, lastSeenRateLimit, Duration.ofMinutes(1), 
                "Rate limit exceeded. Maximum " + lastSeenRateLimit + " requests per minute.");

        if (!userId.equals(currentUserId)) {
            throw new ForbiddenException("You can only update your own last seen timestamp");
        }

        LastSeenResponse response = userService.updateLastSeen(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Update user profile",
            description = "Updates the profile of an authenticated user. Supports updating username (3-50 characters) and avatar (1-6). Only the owner can update their own profile."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UpdateUserResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data - validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - can only update own profile"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Username already exists"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded - maximum 30 requests per minute")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UpdateUserResponse> updateUserProfile(
            @PathVariable @Min(1) Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        Long currentUserId = authenticationService.getCurrentUserId();

        String rateLimitKey = "users:update:" + currentUserId;
        checkRateLimit(rateLimitKey, updateRateLimit, Duration.ofMinutes(1), 
                "Rate limit exceeded. Maximum " + updateRateLimit + " requests per minute.");

        if (!userId.equals(currentUserId)) {
            throw new ForbiddenException("You can only update your own profile");
        }

        UpdateUserResponse response = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/avatar")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Update user avatar",
            description = "Updates the avatar of an authenticated user. Avatar must be between 1 and 6. Only the owner can update their own avatar."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Avatar updated successfully",
                    content = @Content(schema = @Schema(implementation = UpdateUserResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid avatar value - must be between 1 and 6"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - can only update own avatar"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded - maximum 30 requests per minute")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UpdateUserResponse> updateUserAvatar(
            @PathVariable @Min(1) Long userId,
            @RequestBody @Valid com.tbs.dto.user.UpdateAvatarRequest request
    ) {
        Long currentUserId = authenticationService.getCurrentUserId();

        String rateLimitKey = "users:update-avatar:" + currentUserId;
        checkRateLimit(rateLimitKey, updateRateLimit, Duration.ofMinutes(1),
                "Rate limit exceeded. Maximum " + updateRateLimit + " requests per minute.");

        if (!userId.equals(currentUserId)) {
            throw new ForbiddenException("You can only update your own avatar");
        }

        UpdateUserResponse response = userService.updateUserAvatar(userId, request);
        return ResponseEntity.ok(response);
    }
}

