package com.tbs.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final int MIN_KEY_LENGTH_BITS = 256;
    private static final int MIN_TOKEN_LENGTH = 10;

    private final SecretKey secretKey;
    private final long validityInMilliseconds;
    private final Map<String, Claims> claimsCache = new ConcurrentHashMap<>();
    private final Environment environment;

    private static final String DEFAULT_SECRET_OLD = "V2FyOiBUaGlzIGlzIGEgdG9wIHNlY3JldCBmb3IgSldUIGVuY29kaW5nLiBJbiBwcm9kdWN0aW9uIHVzZSBhIHN0cm9uZyByYW5kb20gc2VjcmV0IQ==";
    private static final String DEFAULT_SECRET_NEW = "lo3Rp/t44UeFUOrB+qKxISaK/nyOsILpmDN06/yoUto=";

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration:3600000}") long validityInMilliseconds,
            Environment environment
    ) {
        this.environment = environment;
        
        if (secret == null || secret.trim().isEmpty()) {
            String errorMessage = "JWT_SECRET environment variable must be set!";
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        if (isProductionEnvironment() && (secret.equals(DEFAULT_SECRET_OLD) || secret.equals(DEFAULT_SECRET_NEW))) {
            String errorMessage = "Default JWT secret cannot be used in production! Please set JWT_SECRET environment variable with a strong random secret.";
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        if (!isProductionEnvironment() && (secret.equals(DEFAULT_SECRET_OLD) || secret.equals(DEFAULT_SECRET_NEW))) {
            log.warn("Using default JWT secret for local development. This should never be used in production!");
        }
        
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        int keyLengthBits = keyBytes.length * 8;
        
        if (keyLengthBits < MIN_KEY_LENGTH_BITS) {
            String errorMessage = String.format(
                "JWT secret key is too short. Minimum length is %d bits, but got %d bits. Please use a stronger secret key.",
                MIN_KEY_LENGTH_BITS,
                keyLengthBits
            );
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.validityInMilliseconds = validityInMilliseconds;
        log.debug("JWT token provider initialized with key length: {} bits", keyLengthBits);
    }

    public String generateToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration();
    }

    public String getTokenId(String token) {
        Claims claims = parseClaims(token);
        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isEmpty()) {
            String errorMessage = "Token ID (JTI) is missing. All tokens must have a unique identifier (UUID).";
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        return tokenId;
    }

    private Claims parseClaims(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        
        if (token.length() < MIN_TOKEN_LENGTH) {
            throw new IllegalArgumentException("Token is too short to be valid");
        }
        
        Claims cachedClaims = claimsCache.get(token);
        
        if (cachedClaims != null) {
            Date expiration = cachedClaims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                claimsCache.remove(token);
            } else if (expiration == null || expiration.after(new Date())) {
                return cachedClaims;
            }
        }
        
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.after(new Date())) {
                claimsCache.put(token, claims);
            }
            
            return claims;
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("JWT parsing failed: {}", e.getMessage());
            throw e;
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void clearExpiredTokens() {
        Date now = new Date();
        int removedCount = 0;
        
        claimsCache.entrySet().removeIf(entry -> {
            try {
                Claims claims = entry.getValue();
                Date expiration = claims.getExpiration();
                return expiration != null && expiration.before(now);
            } catch (Exception e) {
                return true;
            }
        });
        
        if (removedCount > 0) {
            log.debug("Cleared {} expired tokens from cache", removedCount);
        }
    }

    public void clearClaimsCache() {
        claimsCache.clear();
    }
    
    private boolean isProductionEnvironment() {
        if (environment == null) {
            return false;
        }
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles == null || activeProfiles.length == 0) {
            String defaultProfile = environment.getProperty("spring.profiles.default", "dev");
            return "prod".equals(defaultProfile) || "production".equals(defaultProfile);
        }
        for (String profile : activeProfiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}

