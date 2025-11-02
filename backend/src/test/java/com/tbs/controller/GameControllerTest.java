package com.tbs.controller;

import com.tbs.dto.common.BoardState;
import com.tbs.dto.game.*;
import com.tbs.enums.*;
import com.tbs.exception.BadRequestException;
import com.tbs.exception.ForbiddenException;
import com.tbs.exception.GameNotFoundException;
import com.tbs.service.AuthenticationService;
import com.tbs.service.GameService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private GameController gameController;

    @Test
    void createGame_shouldCreateGameAndReturn201() {
        CreateGameResponse response = new CreateGameResponse(
                42L, GameType.VS_BOT, BoardSize.THREE, 1L, null,
                BotDifficulty.EASY, GameStatus.WAITING, null,
                Instant.now(), new BoardState(new String[3][3])
        );

        when(authenticationService.getCurrentUserId()).thenReturn(1L);
        when(gameService.createGame(any(CreateGameRequest.class), eq(1L))).thenReturn(response);

        CreateGameRequest request = new CreateGameRequest(
                GameType.VS_BOT, 3, BotDifficulty.EASY
        );

        ResponseEntity<CreateGameResponse> result = gameController.createGame(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().gameId()).isEqualTo(42L);
        assertThat(result.getBody().gameType()).isEqualTo(GameType.VS_BOT);
        assertThat(result.getHeaders().getLocation()).isNotNull();
    }

    @Test
    void getGames_shouldReturnGamesList() {
        GameListItem item = new GameListItem(
                42L, GameType.VS_BOT, BoardSize.THREE, GameStatus.WAITING,
                "player1", null, null, BotDifficulty.EASY, 0,
                Instant.now(), null, null
        );
        GameListResponse response = new GameListResponse(
                Arrays.asList(item), 1, 1, 20, 0, true, true
        );

        when(authenticationService.getCurrentUserId()).thenReturn(1L);
        when(gameService.getGames(eq(1L), any(), any(), any(Pageable.class))).thenReturn(response);

        ResponseEntity<GameListResponse> result = gameController.getGames(null, null, Pageable.unpaged());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().content()).hasSize(1);
        assertThat(result.getBody().content().get(0).gameId()).isEqualTo(42L);
    }

    @Test
    void getGameDetail_shouldReturnGameDetails() {
        GameDetailResponse response = new GameDetailResponse(
                42L, GameType.VS_BOT, BoardSize.THREE,
                new com.tbs.dto.user.PlayerInfo(1L, "player1", false),
                null, null, BotDifficulty.EASY, GameStatus.WAITING,
                PlayerSymbol.X, Instant.now(), Instant.now(),
                Instant.now(), null, new BoardState(new String[3][3]),
                0, List.of()
        );

        when(authenticationService.getCurrentUserId()).thenReturn(1L);
        when(gameService.getGameDetail(42L, 1L)).thenReturn(response);

        ResponseEntity<GameDetailResponse> result = gameController.getGameDetail(42L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().gameId()).isEqualTo(42L);
        assertThat(result.getBody().gameType()).isEqualTo(GameType.VS_BOT);
    }

    @Test
    void getGameDetail_shouldThrowGameNotFound() {
        when(authenticationService.getCurrentUserId()).thenReturn(1L);
        when(gameService.getGameDetail(999L, 1L))
                .thenThrow(new GameNotFoundException("Game not found"));

        assertThatThrownBy(() -> gameController.getGameDetail(999L))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessage("Game not found");
    }

    @Test
    void getGameDetail_shouldThrowForbidden() {
        when(authenticationService.getCurrentUserId()).thenReturn(1L);
        when(gameService.getGameDetail(42L, 1L))
                .thenThrow(new ForbiddenException("You are not a participant of this game"));

        assertThatThrownBy(() -> gameController.getGameDetail(42L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You are not a participant of this game");
    }

    @Test
    void getBoardState_shouldReturnBoardState() {
        BoardStateResponse response = new BoardStateResponse(
                new BoardState(new String[3][3]), BoardSize.THREE, 0, null
        );

        when(authenticationService.getCurrentUserId()).thenReturn(1L);
        when(gameService.getBoardState(42L, 1L)).thenReturn(response);

        ResponseEntity<BoardStateResponse> result = gameController.getBoardState(42L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().boardSize()).isEqualTo(BoardSize.THREE);
        assertThat(result.getBody().totalMoves()).isEqualTo(0);
    }

    @Test
    void updateGameStatus_shouldUpdateStatus() {
        UpdateGameStatusResponse response = new UpdateGameStatusResponse(
                42L, GameStatus.ABANDONED, Instant.now()
        );

        when(authenticationService.getCurrentUserId()).thenReturn(1L);
        when(gameService.updateGameStatus(eq(42L), eq(GameStatus.ABANDONED), eq(1L)))
                .thenReturn(response);

        UpdateGameStatusRequest request = new UpdateGameStatusRequest(GameStatus.ABANDONED);

        ResponseEntity<UpdateGameStatusResponse> result = gameController.updateGameStatus(42L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().gameId()).isEqualTo(42L);
        assertThat(result.getBody().status()).isEqualTo(GameStatus.ABANDONED);
    }

    @Test
    void updateGameStatus_shouldThrowBadRequest() {
        when(authenticationService.getCurrentUserId()).thenReturn(1L);
        when(gameService.updateGameStatus(eq(42L), any(), eq(1L)))
                .thenThrow(new BadRequestException("Invalid status transition"));

        UpdateGameStatusRequest request = new UpdateGameStatusRequest(GameStatus.FINISHED);

        assertThatThrownBy(() -> gameController.updateGameStatus(42L, request))
                .isInstanceOf(BadRequestException.class);
    }
}
