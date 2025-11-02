package com.tbs.service;

import com.tbs.dto.auth.LoginRequest;
import com.tbs.dto.auth.LoginResponse;
import com.tbs.dto.auth.LogoutResponse;
import com.tbs.dto.auth.RegisterRequest;
import com.tbs.dto.auth.RegisterResponse;
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
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.authenticationService = authenticationService;
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
        
        validateRegistrationRequest(request);
        
        try {
            User savedUser = createAndSaveUser(request);
            String token = jwtTokenProvider.generateToken(savedUser.getId());

            return buildRegisterResponse(savedUser, token);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e, request);
            throw e;
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void validateRegistrationRequest(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed: Email already exists: {}", request.email());
            throw new com.tbs.exception.BadRequestException("Email already exists");
        }
        if (userRepository.existsByUsername(request.username())) {
            log.warn("Registration failed: Username already exists: {}", request.username());
            throw new com.tbs.exception.BadRequestException("Username already exists");
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
        if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
            org.hibernate.exception.ConstraintViolationException cve = 
                (org.hibernate.exception.ConstraintViolationException) e.getCause();
            if (cve.getConstraintName() != null && cve.getConstraintName().contains("users_registered_check")) {
                log.warn("Constraint violation: users_registered_check. User data: isGuest={}, authUserId={}, username={}, email={}", 
                    false, null, request.username(), request.email());
                throw new com.tbs.exception.BadRequestException("Registration failed: constraint violation. Please check database constraints.");
            }
        }
    }

    public LogoutResponse logout(String token) {
        if (token == null || token.isEmpty()) {
            throw new UnauthorizedException("Token is required");
        }

        Long userId = authenticationService.getCurrentUserId();
        
        try {
            String tokenId = jwtTokenProvider.getTokenId(token);
            Date expirationTime = jwtTokenProvider.getExpirationDateFromToken(token);
            tokenBlacklistService.addToBlacklist(tokenId, expirationTime);
            log.debug("Token blacklisted: tokenId={}, userId={}", tokenId, userId);
        } catch (Exception e) {
            log.error("Failed to blacklist token during logout: userId={}", userId, e);
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

