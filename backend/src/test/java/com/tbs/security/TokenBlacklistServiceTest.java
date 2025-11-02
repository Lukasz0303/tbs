package com.tbs.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenBlacklistService = new TokenBlacklistService(redisTemplate);
    }

    @Test
    void shouldAddTokenToBlacklist() {
        String tokenId = "test-token-id";
        Date expirationTime = new Date(System.currentTimeMillis() + 3600000);

        tokenBlacklistService.addToBlacklist(tokenId, expirationTime);

        verify(valueOperations, times(1)).set(anyString(), eq("true"), any());
    }

    @Test
    void shouldNotAddExpiredTokenToBlacklist() {
        String tokenId = "expired-token-id";
        Date expirationTime = new Date(System.currentTimeMillis() - 1000);

        tokenBlacklistService.addToBlacklist(tokenId, expirationTime);

        verify(redisTemplate.opsForValue(), never()).set(anyString(), anyString(), any());
    }

    @Test
    void shouldReturnTrueForBlacklistedToken() {
        String tokenId = "blacklisted-token-id";
        when(redisTemplate.hasKey("token:blacklist:" + tokenId)).thenReturn(true);

        boolean result = tokenBlacklistService.isBlacklisted(tokenId);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseForNonBlacklistedToken() {
        String tokenId = "valid-token-id";
        when(redisTemplate.hasKey("token:blacklist:" + tokenId)).thenReturn(false);

        boolean result = tokenBlacklistService.isBlacklisted(tokenId);

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseForNullTokenId() {
        boolean result = tokenBlacklistService.isBlacklisted(null);

        assertThat(result).isFalse();
    }

    @Test
    void shouldRemoveTokenFromBlacklist() {
        String tokenId = "token-to-remove";
        when(redisTemplate.delete("token:blacklist:" + tokenId)).thenReturn(true);
        
        tokenBlacklistService.removeFromBlacklist(tokenId);

        verify(redisTemplate, times(1)).delete("token:blacklist:" + tokenId);
    }

    @Test
    void shouldHandleNullTokenIdOnRemove() {
        tokenBlacklistService.removeFromBlacklist(null);

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void shouldHandleExceptionGracefullyOnAdd() {
        String tokenId = "token-id";
        Date expirationTime = new Date(System.currentTimeMillis() + 3600000);
        doThrow(new RuntimeException("Redis error")).when(valueOperations).set(anyString(), anyString(), any());

        try {
            tokenBlacklistService.addToBlacklist(tokenId, expirationTime);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Failed to blacklist token");
        }
    }

    @Test
    void shouldHandleExceptionGracefullyOnCheck() {
        String tokenId = "token-id";
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis error"));

        boolean result = tokenBlacklistService.isBlacklisted(tokenId);

        assertThat(result).isFalse();
    }
}

