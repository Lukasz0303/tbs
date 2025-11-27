package com.tbs.service;

import com.tbs.dto.matchmaking.*;
import com.tbs.enums.BoardSize;
import com.tbs.enums.GameStatus;
import com.tbs.enums.GameType;
import com.tbs.enums.QueuePlayerStatus;
import com.tbs.exception.*;
import com.tbs.model.Game;
import com.tbs.model.User;
import com.tbs.repository.GameRepository;
import com.tbs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {

    @Mock
    private RedisService redisService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MatchmakingService matchmakingService;

    private User testUser1;
    private User testUser2;
    private Game testGame;

    @BeforeEach
    void setUp() {
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setUsername("user1");

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setUsername("user2");

        testGame = new Game();
        testGame.setId(42L);
        testGame.setGameType(GameType.PVP);
        testGame.setBoardSize(BoardSize.THREE);
        testGame.setPlayer1(testUser1);
        testGame.setPlayer2(testUser2);
        testGame.setStatus(GameStatus.WAITING);
    }

    @Test
    void addToQueue_shouldAddUserToQueueSuccessfully() {
        MatchmakingQueueRequest request = new MatchmakingQueueRequest(BoardSize.THREE);

        when(redisService.acquireLock("1", 5)).thenReturn(true);
        when(redisService.addToQueueIfNotActive(1L, BoardSize.THREE)).thenReturn(true);
        when(redisService.acquireLock("matchmaking:THREE", 10)).thenReturn(false);

        MatchmakingQueueResponse response = matchmakingService.addToQueue(1L, request);

        assertThat(response.message()).isEqualTo("Successfully added to queue");
        assertThat(response.estimatedWaitTime()).isEqualTo(30);
        verify(redisService).releaseLock("1");
    }

    @Test
    void addToQueue_shouldThrowExceptionWhenUserAlreadyInQueue() {
        MatchmakingQueueRequest request = new MatchmakingQueueRequest(BoardSize.THREE);

        when(redisService.acquireLock("1", 5)).thenReturn(false);

        assertThatThrownBy(() -> matchmakingService.addToQueue(1L, request))
                .isInstanceOf(UserAlreadyInQueueException.class)
                .hasMessageContaining("User is already in the matchmaking queue or operation in progress");

        verify(redisService, never()).addToQueueIfNotActive(any(), any());
    }

    @Test
    void addToQueue_shouldThrowExceptionWhenUserHasActiveGame() {
        MatchmakingQueueRequest request = new MatchmakingQueueRequest(BoardSize.THREE);

        when(redisService.acquireLock("1", 5)).thenReturn(true);
        when(redisService.addToQueueIfNotActive(1L, BoardSize.THREE)).thenReturn(false);
        when(gameRepository.hasActivePvpGame(1L)).thenReturn(true);

        assertThatThrownBy(() -> matchmakingService.addToQueue(1L, request))
                .isInstanceOf(UserHasActiveGameException.class)
                .hasMessageContaining("User already has an active PvP game");

        verify(redisService).releaseLock("1");
    }

    @Test
    void addToQueue_shouldCreateGameWhenMatchFound() {
        MatchmakingQueueRequest request = new MatchmakingQueueRequest(BoardSize.THREE);

        when(redisService.acquireLock("1", 5)).thenReturn(true);
        when(redisService.addToQueueIfNotActive(1L, BoardSize.THREE)).thenReturn(true);
        when(redisService.acquireLock("matchmaking:THREE", 10)).thenReturn(true);
        when(redisService.getQueueForBoardSize(BoardSize.THREE)).thenReturn(List.of(2L));
        when(gameRepository.hasActivePvpGame(2L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));
        when(redisService.removeFromQueue(1L)).thenReturn(true);
        when(redisService.removeFromQueue(2L)).thenReturn(true);
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);

        MatchmakingQueueResponse response = matchmakingService.addToQueue(1L, request);

        assertThat(response.message()).isEqualTo("Match found! Game created");
        assertThat(response.estimatedWaitTime()).isEqualTo(0);
        verify(redisService).releaseLock("matchmaking:THREE");
        verify(redisService).releaseLock("1");
        verify(gameRepository).save(any(Game.class));
    }

    @Test
    void removeFromQueue_shouldRemoveUserFromQueueSuccessfully() {
        when(redisService.removeFromQueue(1L)).thenReturn(true);

        LeaveQueueResponse response = matchmakingService.removeFromQueue(1L);

        assertThat(response.message()).isEqualTo("Successfully removed from queue");
        verify(redisService).removeFromQueue(1L);
    }

    @Test
    void removeFromQueue_shouldThrowExceptionWhenUserNotInQueue() {
        when(redisService.removeFromQueue(1L)).thenReturn(false);

        assertThatThrownBy(() -> matchmakingService.removeFromQueue(1L))
                .isInstanceOf(UserNotInQueueException.class)
                .hasMessageContaining("User is not in the matchmaking queue");
    }

    @Test
    void createDirectChallenge_shouldCreateChallengeSuccessfully() {
        ChallengeRequest request = new ChallengeRequest(BoardSize.THREE);

        when(gameRepository.hasActivePvpGame(1L)).thenReturn(false);
        when(gameRepository.hasActivePvpGame(2L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);

        ChallengeResponse response = matchmakingService.createDirectChallenge(1L, 2L, request);

        assertThat(response.gameId()).isEqualTo(42L);
        assertThat(response.gameType()).isEqualTo(GameType.PVP);
        assertThat(response.boardSize()).isEqualTo(BoardSize.THREE);
        assertThat(response.player1Id()).isEqualTo(1L);
        assertThat(response.player2Id()).isEqualTo(2L);
        verify(gameRepository).save(any(Game.class));
    }

    @Test
    void createDirectChallenge_shouldThrowExceptionWhenChallengingSelf() {
        ChallengeRequest request = new ChallengeRequest(BoardSize.THREE);

        assertThatThrownBy(() -> matchmakingService.createDirectChallenge(1L, 1L, request))
                .isInstanceOf(CannotChallengeSelfException.class)
                .hasMessageContaining("Users cannot challenge themselves");

        verify(gameRepository, never()).save(any());
    }

    @Test
    void createDirectChallenge_shouldThrowExceptionWhenChallengedUserNotFound() {
        ChallengeRequest request = new ChallengeRequest(BoardSize.THREE);

        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchmakingService.createDirectChallenge(1L, 2L, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Challenged user does not exist");

        verify(gameRepository, never()).save(any());
    }

    @Test
    void createDirectChallenge_shouldThrowExceptionWhenChallengedUserUnavailable() {
        ChallengeRequest request = new ChallengeRequest(BoardSize.THREE);

        when(gameRepository.hasActivePvpGame(1L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));
        when(gameRepository.hasActivePvpGame(2L)).thenReturn(true);

        assertThatThrownBy(() -> matchmakingService.createDirectChallenge(1L, 2L, request))
                .isInstanceOf(UserUnavailableException.class)
                .hasMessageContaining("Challenged user is currently unavailable");

        verify(gameRepository, never()).save(any());
    }


    @Test
    void isUserAvailable_shouldReturnFalseWhenUserHasActiveGame() {
        when(gameRepository.hasActivePvpGame(1L)).thenReturn(true);

        boolean result = matchmakingService.isUserAvailable(1L);

        assertThat(result).isFalse();
        verify(redisService, never()).isUserInQueue(any());
    }

    @Test
    void isUserAvailable_shouldReturnTrueWhenUserIsAvailable() {
        when(gameRepository.hasActivePvpGame(1L)).thenReturn(false);

        boolean result = matchmakingService.isUserAvailable(1L);

        assertThat(result).isTrue();
        verify(redisService, never()).isUserInQueue(any());
    }

    @Test
    void getQueueStatus_shouldReturnEmptyQueueWhenNoPlayers() {
        when(redisService.getQueueEntriesForBoardSize(BoardSize.THREE)).thenReturn(Collections.emptyList());

        QueueStatusResponse response = matchmakingService.getQueueStatus(BoardSize.THREE);

        assertThat(response.players()).isEmpty();
        assertThat(response.totalCount()).isEqualTo(0);
    }

    @Test
    void getQueueStatus_shouldReturnWaitingPlayers() {
        RedisService.QueueEntry entry1 = new RedisService.QueueEntry(1L, BoardSize.THREE, Instant.now());
        RedisService.QueueEntry entry2 = new RedisService.QueueEntry(2L, BoardSize.THREE, Instant.now());

        when(redisService.getQueueEntriesForBoardSize(BoardSize.THREE))
                .thenReturn(List.of(entry1, entry2));
        when(userRepository.findAllById(any())).thenReturn(List.of(testUser1, testUser2));
        when(gameRepository.findActivePvpGamesForUsers(any())).thenReturn(Collections.emptyList());

        QueueStatusResponse response = matchmakingService.getQueueStatus(BoardSize.THREE);

        assertThat(response.players()).hasSize(2);
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.players().get(0).status()).isEqualTo(QueuePlayerStatus.WAITING);
        assertThat(response.players().get(1).status()).isEqualTo(QueuePlayerStatus.WAITING);
    }

    @Test
    void getQueueStatus_shouldReturnMatchedPlayers() {
        RedisService.QueueEntry entry1 = new RedisService.QueueEntry(1L, BoardSize.THREE, Instant.now());
        testGame.setStatus(GameStatus.WAITING);

        when(redisService.getQueueEntriesForBoardSize(BoardSize.THREE))
                .thenReturn(List.of(entry1));
        when(userRepository.findAllById(any())).thenReturn(List.of(testUser1, testUser2));
        when(gameRepository.findActivePvpGamesForUsers(any())).thenReturn(List.of(testGame));

        QueueStatusResponse response = matchmakingService.getQueueStatus(BoardSize.THREE);

        assertThat(response.players()).hasSize(1);
        assertThat(response.players().get(0).status()).isEqualTo(QueuePlayerStatus.MATCHED);
        assertThat(response.players().get(0).matchedWith()).isEqualTo(2L);
        assertThat(response.players().get(0).matchedWithUsername()).isEqualTo("user2");
    }

    @Test
    void getQueueStatus_shouldReturnPlayingPlayers() {
        RedisService.QueueEntry entry1 = new RedisService.QueueEntry(1L, BoardSize.THREE, Instant.now());
        testGame.setStatus(GameStatus.IN_PROGRESS);

        when(redisService.getQueueEntriesForBoardSize(BoardSize.THREE))
                .thenReturn(List.of(entry1));
        when(userRepository.findAllById(any())).thenReturn(List.of(testUser1));
        when(gameRepository.findActivePvpGamesForUsers(any())).thenReturn(List.of(testGame));

        QueueStatusResponse response = matchmakingService.getQueueStatus(BoardSize.THREE);

        assertThat(response.players()).hasSize(1);
        assertThat(response.players().get(0).status()).isEqualTo(QueuePlayerStatus.PLAYING);
    }

    @Test
    void getQueueStatus_shouldReturnAllBoardSizesWhenBoardSizeIsNull() {
        when(redisService.getAllQueueEntries()).thenReturn(Collections.emptyList());

        QueueStatusResponse response = matchmakingService.getQueueStatus(null);

        assertThat(response.players()).isEmpty();
        verify(redisService).getAllQueueEntries();
        verify(redisService, never()).getQueueEntriesForBoardSize(any());
    }
}

