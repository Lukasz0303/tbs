package com.tbs.controller;

import com.tbs.dto.auth.LoginRequest;
import com.tbs.dto.auth.LoginResponse;
import com.tbs.dto.auth.LogoutResponse;
import com.tbs.dto.auth.RegisterRequest;
import com.tbs.dto.auth.RegisterResponse;
import com.tbs.dto.auth.UserProfileResponse;
import com.tbs.exception.RateLimitExceededException;
import com.tbs.service.AuthService;
import com.tbs.service.IpAddressService;
import com.tbs.service.RateLimitingService;
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

import java.time.Duration;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API endpoints for user authentication and profile management")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RateLimitingService rateLimitingService;
    private final IpAddressService ipAddressService;
    private final boolean cookieSecure;
    private final long jwtExpirationSeconds;
    private final int loginRateLimitPerIp;
    private final int loginRateLimitPerAccount;
    private final int registerRateLimitPerIp;
    private final int registerRateLimitPerAccount;

    public AuthController(
            AuthService authService, 
            UserService userService,
            RateLimitingService rateLimitingService,
            IpAddressService ipAddressService,
            @Value("${app.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.jwt.expiration:3600000}") long jwtExpirationMs,
            @Value("${app.rate-limit.login-per-ip:100}") int loginRateLimitPerIp,
            @Value("${app.rate-limit.login-per-account:100}") int loginRateLimitPerAccount,
            @Value("${app.rate-limit.register-per-ip:50}") int registerRateLimitPerIp,
            @Value("${app.rate-limit.register-per-account:50}") int registerRateLimitPerAccount
    ) {
        this.authService = Objects.requireNonNull(authService);
        this.userService = Objects.requireNonNull(userService);
        this.rateLimitingService = Objects.requireNonNull(rateLimitingService);
        this.ipAddressService = Objects.requireNonNull(ipAddressService);
        this.cookieSecure = cookieSecure;
        this.jwtExpirationSeconds = jwtExpirationMs / 1000;
        
        validateRateLimit("login-per-ip", loginRateLimitPerIp, 1, 10000);
        validateRateLimit("login-per-account", loginRateLimitPerAccount, 1, 10000);
        validateRateLimit("register-per-ip", registerRateLimitPerIp, 1, 10000);
        validateRateLimit("register-per-account", registerRateLimitPerAccount, 1, 10000);
        
        this.loginRateLimitPerIp = loginRateLimitPerIp;
        this.loginRateLimitPerAccount = loginRateLimitPerAccount;
        this.registerRateLimitPerIp = registerRateLimitPerIp;
        this.registerRateLimitPerAccount = registerRateLimitPerAccount;
    }

    private void validateRateLimit(String name, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("Rate limit %s must be between %d and %d, got: %d", name, min, max, value)
            );
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates a user and sets JWT token in httpOnly cookie. Rate limited: 5 attempts per IP and per account per 15 minutes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged in"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded - too many login attempts")
    })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, 
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = ipAddressService.getClientIpAddress(httpRequest);
        String ipRateLimitKey = "auth:login:ip:" + clientIp;
        checkRateLimit(ipRateLimitKey, loginRateLimitPerIp, Duration.ofMinutes(15), 
                "Too many login attempts from this IP. Please try again later.");

        String accountRateLimitKey = "auth:login:account:" + request.email().toLowerCase();
        checkRateLimit(accountRateLimitKey, loginRateLimitPerAccount, Duration.ofMinutes(15), 
                "Too many login attempts for this account. Please try again later.");

        LoginResponse response = authService.login(request);
        if (response.userId() == null) {
            throw new IllegalStateException("User ID is missing in response");
        }
        String token = Objects.requireNonNull(authService.generateTokenForUser(response.userId()), "Token cannot be null");
        setAuthCookie(httpResponse, token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new user account and sets JWT token in httpOnly cookie. Rate limited: 3 attempts per IP and per account per hour.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid input"),
            @ApiResponse(responseCode = "409", description = "Email or username already exists"),
            @ApiResponse(responseCode = "422", description = "Validation error"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded - too many registration attempts")
    })
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request, 
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = ipAddressService.getClientIpAddress(httpRequest);
        String ipRateLimitKey = "auth:register:ip:" + clientIp;
        checkRateLimit(ipRateLimitKey, registerRateLimitPerIp, Duration.ofHours(1), 
                "Too many registration attempts from this IP. Please try again later.");

        String emailRateLimitKey = "auth:register:email:" + request.email().toLowerCase();
        checkRateLimit(emailRateLimitKey, registerRateLimitPerAccount, Duration.ofHours(1), 
                "Too many registration attempts for this email. Please try again later.");

        String usernameRateLimitKey = "auth:register:username:" + request.username().toLowerCase();
        checkRateLimit(usernameRateLimitKey, registerRateLimitPerAccount, Duration.ofHours(1), 
                "Too many registration attempts for this username. Please try again later.");

        RegisterResponse response = authService.register(request);
        if (response.userId() == null) {
            throw new IllegalStateException("User ID is missing in response");
        }
        String token = Objects.requireNonNull(authService.generateTokenForUser(response.userId()), "Token cannot be null");
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
                .sameSite(cookieSecure ? "Strict" : "Lax")
                .path("/")
                .maxAge(jwtExpirationSeconds)
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("authToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSecure ? "Strict" : "Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void checkRateLimit(String key, int limit, Duration window, String errorMessage) {
        if (!rateLimitingService.isAllowed(key, limit, window)) {
            long remaining = rateLimitingService.getRemainingRequests(key, limit);
            long timeToReset = rateLimitingService.getTimeToReset(key).getSeconds();
            throw new RateLimitExceededException(errorMessage, remaining, timeToReset);
        }
    }
}

