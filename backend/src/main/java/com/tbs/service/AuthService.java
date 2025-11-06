package com.tbs.service;

import com.tbs.dto.auth.LoginRequest;
import com.tbs.dto.auth.LoginResponse;
import com.tbs.dto.auth.LogoutResponse;
import com.tbs.dto.auth.RegisterRequest;
import com.tbs.dto.auth.RegisterResponse;
import com.tbs.exception.BadRequestException;
import com.tbs.exception.ConflictException;
import com.tbs.exception.ForbiddenException;
import com.tbs.exception.TokenBlacklistException;
import com.tbs.exception.UnauthorizedException;
import com.tbs.model.User;
import com.tbs.repository.UserRepository;
import com.tbs.security.JwtTokenProvider;
import com.tbs.security.TokenBlacklistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthenticationService authenticationService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            TokenBlacklistService tokenBlacklistService,
            AuthenticationService authenticationService
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "PasswordEncoder cannot be null");
        this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "JwtTokenProvider cannot be null");
        this.tokenBlacklistService = Objects.requireNonNull(tokenBlacklistService, "TokenBlacklistService cannot be null");
        this.authenticationService = Objects.requireNonNull(authenticationService, "AuthenticationService cannot be null");
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getId());

        return new LoginResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                false,
                user.getTotalPoints(),
                user.getGamesPlayed(),
                user.getGamesWon(),
                token
        );
    }

    public RegisterResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.email());
        
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists");
        }
        
        try {
            User savedUser = createAndSaveUser(request);
            String token = jwtTokenProvider.generateToken(savedUser.getId());
            return buildRegisterResponse(savedUser, token);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e, request);
            throw new BadRequestException("Registration failed due to constraint violation");
        }
    }

    private User createAndSaveUser(RegisterRequest request) {
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = createNewUser(request, encodedPassword);
        
        log.debug("Saving user to database: email={}, username={}, isGuest={}, authUserId={}, ipAddress={}", 
            user.getEmail(), user.getUsername(), user.getIsGuest(), user.getAuthUserId(), user.getIpAddress());
        
        User savedUser = userRepository.save(user);
        log.info("User successfully saved with ID: {}", savedUser.getId());
        
        if (savedUser.getId() == null) {
            log.error("User saved but ID is null - this should not happen!");
            throw new RuntimeException("User saved but ID is null");
        }
        
        return savedUser;
    }

    private User createNewUser(RegisterRequest request, String encodedPassword) {
        User user = new User();
        user.setAuthUserId(null);
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPasswordHash(encodedPassword);
        user.setIsGuest(false);
        user.setIpAddress(null);
        user.setTotalPoints(0L);
        user.setGamesPlayed(0);
        user.setGamesWon(0);
        return user;
    }

    private RegisterResponse buildRegisterResponse(User savedUser, String token) {
        return new RegisterResponse(
                savedUser.getId().toString(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                false,
                savedUser.getTotalPoints(),
                savedUser.getGamesPlayed(),
                savedUser.getGamesWon(),
                token
        );
    }

    private void handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException e, RegisterRequest request) {
        log.warn("Data integrity violation during user registration: {}", e.getMessage());
        
        String errorMessage = extractConstraintErrorMessage(e);
        if (errorMessage != null) {
            throw new BadRequestException(errorMessage);
        }
        
        log.warn("Constraint violation: users_registered_check. User data: isGuest={}, authUserId={}, username={}, email={}", 
            false, null, request.username(), request.email());
        throw new BadRequestException("Registration failed due to constraint violation");
    }

    private String extractConstraintErrorMessage(org.springframework.dao.DataIntegrityViolationException e) {
        if (!(e.getCause() instanceof org.hibernate.exception.ConstraintViolationException)) {
            return null;
        }
        
        org.hibernate.exception.ConstraintViolationException cve = 
            (org.hibernate.exception.ConstraintViolationException) e.getCause();
        String constraintName = cve.getConstraintName();
        
        if (constraintName != null) {
            String constraintNameLower = constraintName.toLowerCase();
            if (isUsernameConstraint(constraintNameLower)) {
                return "Username already exists";
            }
            
            if (isEmailConstraint(constraintNameLower)) {
                return "Email already exists";
            }
        }
        
        if (cve.getCause() instanceof org.postgresql.util.PSQLException) {
            org.postgresql.util.PSQLException psqlEx = (org.postgresql.util.PSQLException) cve.getCause();
            if (psqlEx.getServerErrorMessage() != null) {
                String detail = psqlEx.getServerErrorMessage().getDetail();
                if (detail != null) {
                    if (detail.contains("username")) {
                        return "Username already exists";
                    }
                    if (detail.contains("email")) {
                        return "Email already exists";
                    }
                }
            }
        }
        
        return null;
    }

    private boolean isUsernameConstraint(String constraintName) {
        return constraintName.contains("username") || constraintName.contains("idx_users_username");
    }

    private boolean isEmailConstraint(String constraintName) {
        return constraintName.contains("email") || constraintName.contains("idx_users_email");
    }

    public LogoutResponse logout(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new UnauthorizedException("Token is required");
        }

        if (!jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Invalid token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        Long currentUserId = authenticationService.getCurrentUserId();

        if (!userId.equals(currentUserId)) {
            log.warn("Token userId {} does not match current user {}", userId, currentUserId);
            throw new ForbiddenException("Token does not belong to current user");
        }

        try {
            String tokenId = jwtTokenProvider.getTokenId(token);
            Date expirationTime = jwtTokenProvider.getExpirationDateFromToken(token);
            tokenBlacklistService.addToBlacklist(tokenId, expirationTime);
            log.debug("Token blacklisted: tokenId={}, userId={}", tokenId, userId);
        } catch (Exception e) {
            log.error("Failed to blacklist token during logout: userId={}", userId, e);
            throw new TokenBlacklistException("Failed to blacklist token");
        }

        try {
            userRepository.updateLastSeenAt(userId, Instant.now());
        } catch (Exception e) {
            log.error("Failed to update last seen timestamp: userId={}", userId, e);
        }

        log.info("User logged out successfully: userId={}", userId);

        return new LogoutResponse("Wylogowano pomyślnie");
    }
}



