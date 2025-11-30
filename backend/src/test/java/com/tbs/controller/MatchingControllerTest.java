package com.tbs.controller;

import com.tbs.dto.matchmaking.*;
import com.tbs.enums.BoardSize;
import com.tbs.enums.GameStatus;
import com.tbs.enums.GameType;
import com.tbs.enums.QueuePlayerStatus;
import com.tbs.exception.*;
import com.tbs.service.AuthenticationService;
import com.tbs.service.MatchmakingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchingControllerTest {

    @Mock
    private MatchmakingService matchmakingService;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private MatchingController matchingController;

    @Test
    void addToQueue_shouldReturn200WhenSuccessfullyAdded() {
        Long userId = 1L;
        MatchmakingQueueRequest request = new MatchmakingQueueRequest(BoardSize.THREE);
        MatchmakingQueueResponse response = new MatchmakingQueueResponse("Successfully added to queue", 30);

        when(authenticationService.getCurrentUserId()).thenReturn(userId);
        when(matchmakingService.addToQueue(userId, request)).thenReturn(response);

        ResponseEntity<MatchmakingQueueResponse> result = matchingController.addToQueue(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().message()).isEqualTo("Successfully added to queue");
        assertThat(result.getBody().estimatedWaitTime()).isEqualTo(30);
    }

    @Test
    void addToQueue_shouldReturn200WhenMatchFound() {
        Long userId = 1L;
        MatchmakingQueueRequest request = new MatchmakingQueueRequest(BoardSize.THREE);
        MatchmakingQueueResponse response = new MatchmakingQueueResponse("Match found! Game created", 0);

        when(authenticationService.getCurrentUserId()).thenReturn(userId);
        when(matchmakingService.addToQueue(userId, request)).thenReturn(response);

        ResponseEntity<MatchmakingQueueResponse> result = matchingController.addToQueue(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().message()).isEqualTo("Match found! Game created");
        assertThat(result.getBody().estimatedWaitTime()).isEqualTo(0);
    }

    @Test
    void addToQueue_shouldThrowExceptionWhenUserAlreadyInQueue() {
        Long userId = 1L;
        MatchmakingQueueRequest request = new MatchmakingQueueRequest(BoardSize.THREE);

        when(authenticationService.getCurrentUserId()).thenReturn(userId);
        when(matchmakingService.addToQueue(userId, request))
                .thenThrow(new UserAlreadyInQueueException("User is already in the matchmaking queue"));

        assertThatThrownBy(() -> matchingController.addToQueue(request))
                .isInstanceOf(UserAlreadyInQueueException.class)
                .hasMessageContaining("User is already in the matchmaking queue");
    }

    @Test
    void addToQueue_shouldThrowExceptionWhenUserHasActiveGame() {
        Long userId = 1L;
        MatchmakingQueueRequest request = new MatchmakingQueueRequest(BoardSize.THREE);

        when(authenticationService.getCurrentUserId()).thenReturn(userId);
        when(matchmakingService.addToQueue(userId, request))
                .thenThrow(new UserHasActiveGameException("User already has an active PvP game"));

        assertThatThrownBy(() -> matchingController.addToQueue(request))
                .isInstanceOf(UserHasActiveGameException.class)
                .hasMessageContaining("User already has an active PvP game");
    }

    @Test
    void removeFromQueue_shouldReturn200WhenSuccessfullyRemoved() {
        Long userId = 1L;
        LeaveQueueResponse response = new LeaveQueueResponse("Successfully removed from queue");

        when(authenticationService.getCurrentUserId()).thenReturn(userId);
        when(matchmakingService.removeFromQueue(userId)).thenReturn(response);

        ResponseEntity<LeaveQueueResponse> result = matchingController.removeFromQueue();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().message()).isEqualTo("Successfully removed from queue");
    }

    @Test
    void removeFromQueue_shouldThrowExceptionWhenUserNotInQueue() {
        Long userId = 1L;

        when(authenticationService.getCurrentUserId()).thenReturn(userId);
        when(matchmakingService.removeFromQueue(userId))
                .thenThrow(new UserNotInQueueException("User is not in the matchmaking queue"));

        assertThatThrownBy(() -> matchingController.removeFromQueue())
                .isInstanceOf(UserNotInQueueException.class)
                .hasMessageContaining("User is not in the matchmaking queue");
    }

    @Test
    void getQueueStatus_shouldReturn200WithQueueStatus() {
        Long userId = 1L;
        BoardSize boardSize = BoardSize.THREE;
        PlayerQueueStatus playerStatus = new PlayerQueueStatus(
                1L, "user1", BoardSize.THREE, QueuePlayerStatus.WAITING,
                Instant.now(), null, null, null, false, 1000L
        );
        QueueStatusResponse response = new QueueStatusResponse(List.of(playerStatus), 1);

        when(authenticationService.getCurrentUserId()).thenReturn(userId);
        when(matchmakingService.getQueueStatus(boardSize)).thenReturn(response);

        ResponseEntity<QueueStatusResponse> result = matchingController.getQueueStatus("THREE");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().players()).hasSize(1);
        assertThat(result.getBody().totalCount()).isEqualTo(1);
        assertThat(result.getBody().players().get(0).username()).isEqualTo("user1");
    }

    @Test
    void getQueueStatus_shouldReturn200WithEmptyQueue() {
        Long userId = 1L;
        QueueStatusResponse response = new QueueStatusResponse(List.of(), 0);

        when(authenticationService.getCurrentUserId()).thenReturn(userId);
        when(matchmakingService.getQueueStatus(null)).thenReturn(response);

        ResponseEntity<QueueStatusResponse> result = matchingController.getQueueStatus(null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().players()).isEmpty();
        assertThat(result.getBody().totalCount()).isEqualTo(0);
    }

    @Test
    void createChallenge_shouldReturn201WhenChallengeCreated() {
        Long challengerId = 1L;
        Long challengedId = 2L;
        ChallengeRequest request = new ChallengeRequest(BoardSize.THREE);
        ChallengeResponse response = new ChallengeResponse(
                42L, GameType.PVP, BoardSize.THREE, 1L, 2L,
                GameStatus.WAITING, Instant.now()
        );

        when(authenticationService.getCurrentUserId()).thenReturn(challengerId);
        when(matchmakingService.createDirectChallenge(challengerId, challengedId, request)).thenReturn(response);

        ResponseEntity<ChallengeResponse> result = matchingController.createChallenge(challengedId, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().gameId()).isEqualTo(42L);
        assertThat(result.getBody().gameType()).isEqualTo(GameType.PVP);
        assertThat(result.getBody().player1Id()).isEqualTo(1L);
        assertThat(result.getBody().player2Id()).isEqualTo(2L);
        assertThat(result.getHeaders().getLocation()).isNotNull();
        assertThat(result.getHeaders().getLocation().getPath()).isEqualTo("/api/v1/games/42");
    }

    @Test
    void createChallenge_shouldThrowExceptionWhenChallengingSelf() {
        Long challengerId = 1L;
        ChallengeRequest request = new ChallengeRequest(BoardSize.THREE);

        when(authenticationService.getCurrentUserId()).thenReturn(challengerId);
        when(matchmakingService.createDirectChallenge(challengerId, challengerId, request))
                .thenThrow(new CannotChallengeSelfException("Users cannot challenge themselves"));

        assertThatThrownBy(() -> matchingController.createChallenge(challengerId, request))
                .isInstanceOf(CannotChallengeSelfException.class)
                .hasMessageContaining("Users cannot challenge themselves");
    }

    @Test
    void createChallenge_shouldThrowExceptionWhenChallengedUserNotFound() {
        Long challengerId = 1L;
        Long challengedId = 2L;
        ChallengeRequest request = new ChallengeRequest(BoardSize.THREE);

        when(authenticationService.getCurrentUserId()).thenReturn(challengerId);
        when(matchmakingService.createDirectChallenge(challengerId, challengedId, request))
                .thenThrow(new UserNotFoundException("Challenged user does not exist"));

        assertThatThrownBy(() -> matchingController.createChallenge(challengedId, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Challenged user does not exist");
    }

    @Test
    void createChallenge_shouldThrowExceptionWhenChallengedUserUnavailable() {
        Long challengerId = 1L;
        Long challengedId = 2L;
        ChallengeRequest request = new ChallengeRequest(BoardSize.THREE);

        when(authenticationService.getCurrentUserId()).thenReturn(challengerId);
        when(matchmakingService.createDirectChallenge(challengerId, challengedId, request))
                .thenThrow(new UserUnavailableException("Challenged user is currently unavailable"));

        assertThatThrownBy(() -> matchingController.createChallenge(challengedId, request))
                .isInstanceOf(UserUnavailableException.class)
                .hasMessageContaining("Challenged user is currently unavailable");
    }
}

