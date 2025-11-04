package com.tbs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void isAllowed_shouldReturnTrueWhenUnderLimit() {
        String key = "test-key";
        int limit = 10;
        Duration window = Duration.ofMinutes(1);

        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(5L);

        boolean result = rateLimitingService.isAllowed(key, limit, window);

        assertThat(result).isTrue();
        verify(redisTemplate, times(1)).execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    void isAllowed_shouldReturnFalseWhenLimitExceeded() {
        String key = "test-key";
        int limit = 10;
        Duration window = Duration.ofMinutes(1);

        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(11L);

        boolean result = rateLimitingService.isAllowed(key, limit, window);

        assertThat(result).isFalse();
        verify(redisTemplate, times(1)).execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    void isAllowed_shouldSetExpireOnlyOnFirstRequest() {
        String key = "test-key";
        int limit = 10;
        Duration window = Duration.ofMinutes(1);

        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(1L, 2L);

        rateLimitingService.isAllowed(key, limit, window);
        rateLimitingService.isAllowed(key, limit, window);

        verify(redisTemplate, times(2)).execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    void isAllowed_shouldReturnFalseWhenExecuteReturnsNull() {
        String key = "test-key";
        int limit = 10;
        Duration window = Duration.ofMinutes(1);

        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(null);

        boolean result = rateLimitingService.isAllowed(key, limit, window);

        assertThat(result).isFalse();
        verify(redisTemplate, times(1)).execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    void getRemainingRequests_shouldReturnLimitWhenKeyDoesNotExist() {
        String key = "test-key";
        int limit = 10;

        when(valueOperations.get(anyString())).thenReturn(null);

        long remaining = rateLimitingService.getRemainingRequests(key, limit);

        assertThat(remaining).isEqualTo(limit);
        verify(valueOperations, times(1)).get("rate_limit:" + key);
    }

    @Test
    void getRemainingRequests_shouldReturnCorrectRemaining() {
        String key = "test-key";
        int limit = 10;

        when(valueOperations.get(anyString())).thenReturn("5");

        long remaining = rateLimitingService.getRemainingRequests(key, limit);

        assertThat(remaining).isEqualTo(5);
        verify(valueOperations, times(1)).get("rate_limit:" + key);
    }

    @Test
    void getRemainingRequests_shouldReturnZeroWhenLimitExceeded() {
        String key = "test-key";
        int limit = 10;

        when(valueOperations.get(anyString())).thenReturn("15");

        long remaining = rateLimitingService.getRemainingRequests(key, limit);

        assertThat(remaining).isEqualTo(0);
    }

    @Test
    void getRemainingRequests_shouldHandleInvalidNumberFormat() {
        String key = "test-key";
        int limit = 10;

        when(valueOperations.get(anyString())).thenReturn("invalid");

        long remaining = rateLimitingService.getRemainingRequests(key, limit);

        assertThat(remaining).isEqualTo(limit);
        verify(valueOperations, times(1)).get("rate_limit:" + key);
    }

    @Test
    void getTimeToReset_shouldReturnZeroWhenKeyDoesNotExist() {
        String key = "test-key";

        when(redisTemplate.getExpire(anyString(), any(TimeUnit.class))).thenReturn(null);

        Duration result = rateLimitingService.getTimeToReset(key);

        assertThat(result).isEqualTo(Duration.ZERO);
        verify(redisTemplate, times(1)).getExpire(eq("rate_limit:" + key), eq(TimeUnit.SECONDS));
    }

    @Test
    void getTimeToReset_shouldReturnZeroWhenTtlIsNegative() {
        String key = "test-key";

        when(redisTemplate.getExpire(anyString(), any(TimeUnit.class))).thenReturn(-1L);

        Duration result = rateLimitingService.getTimeToReset(key);

        assertThat(result).isEqualTo(Duration.ZERO);
    }

    @Test
    void getTimeToReset_shouldReturnCorrectDuration() {
        String key = "test-key";
        long ttlSeconds = 30;

        when(redisTemplate.getExpire(anyString(), any(TimeUnit.class))).thenReturn(ttlSeconds);

        Duration result = rateLimitingService.getTimeToReset(key);

        assertThat(result).isEqualTo(Duration.ofSeconds(30));
        verify(redisTemplate, times(1)).getExpire(eq("rate_limit:" + key), eq(TimeUnit.SECONDS));
    }
}

