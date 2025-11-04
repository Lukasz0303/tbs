package com.tbs.service;

import com.tbs.enums.BoardSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void addToQueue_shouldAddUserToQueue() {
        Long userId = 1L;
        BoardSize boardSize = BoardSize.THREE;

        doReturn(true).when(zSetOperations).add(anyString(), anyString(), anyLong());
        doNothing().when(valueOperations).set(anyString(), anyString(), any(java.time.Duration.class));
        doReturn(true).when(redisTemplate).expire(anyString(), any(java.time.Duration.class));

        redisService.addToQueue(userId, boardSize);

        verify(redisTemplate).opsForZSet();
        verify(redisTemplate).opsForValue();
        verify(redisTemplate).expire(anyString(), any(java.time.Duration.class));
    }

    @Test
    void removeFromQueue_shouldRemoveUserFromQueue() {
        Long userId = 1L;
        String userKey = "matchmaking:user:1";

        when(valueOperations.get(userKey)).thenReturn("THREE");
        when(zSetOperations.remove(anyString(), anyString())).thenReturn(1L);
        when(redisTemplate.delete(userKey)).thenReturn(true);

        boolean result = redisService.removeFromQueue(userId);

        assertThat(result).isTrue();
        verify(zSetOperations).remove(eq("matchmaking:queue:THREE"), eq("1"));
        verify(redisTemplate).delete(userKey);
    }

    @Test
    void removeFromQueue_shouldReturnFalseWhenUserNotInQueue() {
        Long userId = 1L;
        String userKey = "matchmaking:user:1";

        when(valueOperations.get(userKey)).thenReturn(null);

        boolean result = redisService.removeFromQueue(userId);

        assertThat(result).isFalse();
        verify(zSetOperations, never()).remove(anyString(), anyString());
    }

    @Test
    void removeFromQueue_shouldHandleInvalidBoardSize() {
        Long userId = 1L;
        String userKey = "matchmaking:user:1";

        when(valueOperations.get(userKey)).thenReturn("INVALID");
        when(redisTemplate.delete(userKey)).thenReturn(true);

        boolean result = redisService.removeFromQueue(userId);

        assertThat(result).isFalse();
        verify(zSetOperations, never()).remove(anyString(), anyString());
        verify(redisTemplate).delete(userKey);
    }

    @Test
    void isUserInQueue_shouldReturnTrueWhenUserInQueue() {
        Long userId = 1L;
        String userKey = "matchmaking:user:1";

        when(redisTemplate.hasKey(userKey)).thenReturn(true);

        boolean result = redisService.isUserInQueue(userId);

        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(userKey);
    }

    @Test
    void isUserInQueue_shouldReturnFalseWhenUserNotInQueue() {
        Long userId = 1L;
        String userKey = "matchmaking:user:1";

        when(redisTemplate.hasKey(userKey)).thenReturn(false);

        boolean result = redisService.isUserInQueue(userId);

        assertThat(result).isFalse();
    }

    @Test
    void getQueueForBoardSize_shouldReturnListOfUserIds() {
        BoardSize boardSize = BoardSize.THREE;
        Set<String> userIds = Set.of("1", "2", "3");

        when(zSetOperations.range("matchmaking:queue:THREE", 0, -1)).thenReturn(userIds);

        List<Long> result = redisService.getQueueForBoardSize(boardSize);

        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void getQueueForBoardSize_shouldReturnEmptyListWhenQueueIsNull() {
        BoardSize boardSize = BoardSize.THREE;

        when(zSetOperations.range("matchmaking:queue:THREE", 0, -1)).thenReturn(null);

        List<Long> result = redisService.getQueueForBoardSize(boardSize);

        assertThat(result).isEmpty();
    }

    @Test
    void getUserBoardSize_shouldReturnBoardSize() {
        Long userId = 1L;
        String userKey = "matchmaking:user:1";

        when(valueOperations.get(userKey)).thenReturn("THREE");

        BoardSize result = redisService.getUserBoardSize(userId);

        assertThat(result).isEqualTo(BoardSize.THREE);
    }

    @Test
    void getUserBoardSize_shouldReturnNullWhenUserNotInQueue() {
        Long userId = 1L;
        String userKey = "matchmaking:user:1";

        when(valueOperations.get(userKey)).thenReturn(null);

        BoardSize result = redisService.getUserBoardSize(userId);

        assertThat(result).isNull();
    }

    @Test
    void getUserBoardSize_shouldReturnNullForInvalidBoardSize() {
        Long userId = 1L;
        String userKey = "matchmaking:user:1";

        when(valueOperations.get(userKey)).thenReturn("INVALID");

        BoardSize result = redisService.getUserBoardSize(userId);

        assertThat(result).isNull();
    }

    @Test
    void getQueueSize_shouldReturnQueueSize() {
        BoardSize boardSize = BoardSize.THREE;

        when(zSetOperations.zCard("matchmaking:queue:THREE")).thenReturn(5L);

        int result = redisService.getQueueSize(boardSize);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void getQueueSize_shouldReturnZeroWhenQueueIsNull() {
        BoardSize boardSize = BoardSize.THREE;

        when(zSetOperations.zCard("matchmaking:queue:THREE")).thenReturn(null);

        int result = redisService.getQueueSize(boardSize);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void getAllQueueEntries_shouldReturnAllEntries() {
        ZSetOperations.TypedTuple<String> tuple1 = mock(ZSetOperations.TypedTuple.class);
        ZSetOperations.TypedTuple<String> tuple2 = mock(ZSetOperations.TypedTuple.class);

        when(tuple1.getValue()).thenReturn("1");
        when(tuple1.getScore()).thenReturn((double) Instant.now().toEpochMilli());
        when(tuple2.getValue()).thenReturn("2");
        when(tuple2.getScore()).thenReturn((double) Instant.now().toEpochMilli());

        Set<ZSetOperations.TypedTuple<String>> tuples = Set.of(tuple1, tuple2);

        when(zSetOperations.rangeWithScores("matchmaking:queue:THREE", 0, -1)).thenReturn(tuples);
        when(zSetOperations.rangeWithScores("matchmaking:queue:FOUR", 0, -1)).thenReturn(null);
        when(zSetOperations.rangeWithScores("matchmaking:queue:FIVE", 0, -1)).thenReturn(null);

        List<RedisService.QueueEntry> result = redisService.getAllQueueEntries();

        assertThat(result).hasSize(2);
    }

    @Test
    void getQueueEntriesForBoardSize_shouldReturnEntriesForSpecificBoardSize() {
        BoardSize boardSize = BoardSize.THREE;
        ZSetOperations.TypedTuple<String> tuple1 = mock(ZSetOperations.TypedTuple.class);

        when(tuple1.getValue()).thenReturn("1");
        when(tuple1.getScore()).thenReturn((double) Instant.now().toEpochMilli());

        Set<ZSetOperations.TypedTuple<String>> tuples = Set.of(tuple1);

        when(zSetOperations.rangeWithScores("matchmaking:queue:THREE", 0, -1)).thenReturn(tuples);

        List<RedisService.QueueEntry> result = redisService.getQueueEntriesForBoardSize(boardSize);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(1L);
        assertThat(result.get(0).boardSize()).isEqualTo(BoardSize.THREE);
    }

    @Test
    void getQueueEntriesForBoardSize_shouldReturnEmptyListWhenQueueIsNull() {
        BoardSize boardSize = BoardSize.THREE;

        when(zSetOperations.rangeWithScores("matchmaking:queue:THREE", 0, -1)).thenReturn(null);

        List<RedisService.QueueEntry> result = redisService.getQueueEntriesForBoardSize(boardSize);

        assertThat(result).isEmpty();
    }

    @Test
    void getJoinedAtTimestamps_shouldReturnTimestamps() {
        List<Long> userIds = List.of(1L, 2L);
        BoardSize boardSize = BoardSize.THREE;

        when(zSetOperations.score("matchmaking:queue:THREE", "1")).thenReturn((double) Instant.now().toEpochMilli());
        when(zSetOperations.score("matchmaking:queue:THREE", "2")).thenReturn((double) Instant.now().toEpochMilli());
        when(zSetOperations.score("matchmaking:queue:FOUR", "1")).thenReturn(null);
        when(zSetOperations.score("matchmaking:queue:FOUR", "2")).thenReturn(null);
        when(zSetOperations.score("matchmaking:queue:FIVE", "1")).thenReturn(null);
        when(zSetOperations.score("matchmaking:queue:FIVE", "2")).thenReturn(null);

        Map<Long, Instant> result = redisService.getJoinedAtTimestamps(userIds);

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys(1L, 2L);
    }

    @Test
    void getJoinedAtTimestamps_shouldReturnEmptyMapWhenNoTimestampsFound() {
        List<Long> userIds = List.of(1L, 2L);

        when(zSetOperations.score(anyString(), anyString())).thenReturn(null);

        Map<Long, Instant> result = redisService.getJoinedAtTimestamps(userIds);

        assertThat(result).isEmpty();
    }
}

