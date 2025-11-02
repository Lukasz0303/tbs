package com.tbs.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final SecretKey secretKey;
    private final long validityInMilliseconds;
    private final ThreadLocal<Map<String, Claims>> claimsCache = ThreadLocal.withInitial(ConcurrentHashMap::new);

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration:3600000}") long validityInMilliseconds
    ) {
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
        Map<String, Claims> cache = claimsCache.get();
        Claims cachedClaims = cache.get(token);
        
        if (cachedClaims != null) {
            return cachedClaims;
        }
        
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        cache.put(token, claims);
        return claims;
    }

    public void clearClaimsCache() {
        claimsCache.remove();
    }
}

