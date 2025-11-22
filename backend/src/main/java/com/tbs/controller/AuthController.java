package com.tbs.controller;

import com.tbs.dto.auth.LoginRequest;
import com.tbs.dto.auth.LoginResponse;
import com.tbs.dto.auth.LogoutResponse;
import com.tbs.dto.auth.RegisterRequest;
import com.tbs.dto.auth.RegisterResponse;
import com.tbs.dto.auth.UserProfileResponse;
import com.tbs.service.AuthService;
import com.tbs.service.UserService;
import com.tbs.util.CookieTokenExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API endpoints for user authentication and profile management")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final boolean cookieSecure;

    public AuthController(
            AuthService authService, 
            UserService userService,
            @Value("${app.cookie.secure:false}") boolean cookieSecure
    ) {
        this.authService = authService;
        this.userService = userService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates a user and sets JWT token in httpOnly cookie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged in"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        LoginResponse response = authService.login(request);
        if (response.userId() == null) {
            throw new IllegalStateException("User ID is missing in response");
        }
        String token = authService.generateTokenForUser(response.userId());
        setAuthCookie(httpResponse, token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new user account and sets JWT token in httpOnly cookie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid input"),
            @ApiResponse(responseCode = "409", description = "Email or username already exists"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse httpResponse) {
        RegisterResponse response = authService.register(request);
        if (response.userId() == null) {
            throw new IllegalStateException("User ID is missing in response");
        }
        String token = authService.generateTokenForUser(response.userId());
        setAuthCookie(httpResponse, token);
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
            description = "Logs out the currently authenticated user and clears auth cookie"
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
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request, HttpServletResponse httpResponse) {
        String token = extractTokenFromRequest(request);
        LogoutResponse response = authService.logout(token);
        clearAuthCookie(httpResponse);
        return ResponseEntity.ok(response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        return CookieTokenExtractor.extractToken(request);
    }

    private void setAuthCookie(HttpServletResponse response, @NonNull String token) {
        ResponseCookie cookie = ResponseCookie.from("authToken", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(3600)
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("authToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

