package com.tbs.controller;

import com.tbs.dto.auth.LoginRequest;
import com.tbs.dto.auth.LoginResponse;
import com.tbs.dto.auth.LogoutResponse;
import com.tbs.dto.auth.RegisterRequest;
import com.tbs.dto.auth.RegisterResponse;
import com.tbs.dto.auth.UserProfileResponse;
import com.tbs.service.AuthService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API endpoints for user authentication and profile management")
public class AuthController {

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates a user and returns JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged in"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new user account and returns JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid input"),
            @ApiResponse(responseCode = "409", description = "Email or username already exists"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current user profile",
            description = "Retrieves the profile of the currently authenticated user including statistics and metadata"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Logs out the currently authenticated user and returns a confirmation message"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User logged out successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        LogoutResponse response = authService.logout(token);
        return ResponseEntity.ok(response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTH_HEADER);
        if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}

