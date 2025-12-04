package com.tbs.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String testSecret;
    
    @Mock
    private Environment environment;

    @BeforeEach
    void setUp() throws Exception {
        byte[] keyBytes = new byte[64];
        SecureRandom.getInstanceStrong().nextBytes(keyBytes);
        testSecret = Base64.getEncoder().encodeToString(keyBytes);
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        lenient().when(environment.getProperty("spring.datasource.url", "")).thenReturn("jdbc:h2:mem:test");
        jwtTokenProvider = new JwtTokenProvider(testSecret, 3600000L, environment);
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() throws InterruptedException {
        JwtTokenProvider shortExpiryProvider = new JwtTokenProvider(testSecret, 1000L, environment);
        String token = shortExpiryProvider.generateToken(123L);
        
        Thread.sleep(1100);
        
        boolean isValid = shortExpiryProvider.validateToken(token);
        assertThat(isValid).isFalse();
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtTokenProvider.generateToken(123L);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void generateToken_shouldThrowExceptionForNullUserId() {
        assertThatThrownBy(() -> jwtTokenProvider.generateToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID cannot be null");
    }

    @Test
    void generateToken_shouldIncludeJtiClaim() {
        String token = jwtTokenProvider.generateToken(123L);

        assertThat(token).isNotNull();
        String tokenId = jwtTokenProvider.getTokenId(token);
        assertThat(tokenId).isNotNull();
        assertThat(tokenId).isNotEmpty();
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtTokenProvider.generateToken(123L);

        boolean isValid = jwtTokenProvider.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        boolean isValid = jwtTokenProvider.validateToken("invalid-token");

        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForEmptyToken() {
        boolean isValid = jwtTokenProvider.validateToken("");

        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForNullToken() {
        boolean isValid = jwtTokenProvider.validateToken(null);

        assertThat(isValid).isFalse();
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        Long expectedUserId = 456L;
        String token = jwtTokenProvider.generateToken(expectedUserId);

        Long actualUserId = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(actualUserId).isEqualTo(expectedUserId);
    }

    @Test
    void getExpirationDateFromToken_shouldReturnValidExpiration() {
        String token = jwtTokenProvider.generateToken(789L);

        Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);

        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void getTokenId_shouldReturnTokenId() {
        String token = jwtTokenProvider.generateToken(999L);

        String tokenId = jwtTokenProvider.getTokenId(token);

        assertThat(tokenId).isNotNull();
        assertThat(tokenId).isNotEmpty();
    }

    @Test
    void getTokenId_shouldThrowExceptionIfNoJtiClaim() {
        JwtTokenProvider provider = new JwtTokenProvider(testSecret, 3600000L, environment);
        String tokenWithoutJti = Jwts.builder()
                .subject("123")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(testSecret)))
                .compact();

        assertThatThrownBy(() -> provider.getTokenId(tokenWithoutJti))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Token ID (JTI) is missing");
    }

    @Test
    void constructor_shouldThrowExceptionForShortKey() throws NoSuchAlgorithmException {
        byte[] shortKeyBytes = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(shortKeyBytes);
        String shortSecret = Base64.getEncoder().encodeToString(shortKeyBytes);

        assertThatThrownBy(() -> new JwtTokenProvider(shortSecret, 3600000L, environment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT secret key is too short");
    }

    @Test
    void constructor_shouldAcceptValidKeyLength() throws NoSuchAlgorithmException {
        byte[] validKeyBytes = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(validKeyBytes);
        String validSecret = Base64.getEncoder().encodeToString(validKeyBytes);

        JwtTokenProvider provider = new JwtTokenProvider(validSecret, 3600000L, environment);

        assertThat(provider).isNotNull();
        String token = provider.generateToken(123L);
        assertThat(token).isNotNull();
    }
}

