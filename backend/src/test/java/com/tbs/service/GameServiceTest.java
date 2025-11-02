package com.tbs.service;

import com.tbs.dto.common.BoardState;
import com.tbs.dto.game.*;
import com.tbs.enums.*;
import com.tbs.exception.BadRequestException;
import com.tbs.exception.ForbiddenException;
import com.tbs.exception.GameNotFoundException;
import com.tbs.model.Game;
import com.tbs.model.Move;
import com.tbs.model.User;
import com.tbs.repository.GameRepository;
import com.tbs.repository.MoveRepository;
import com.tbs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MoveRepository moveRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BoardStateService boardStateService;

    @InjectMocks
    private GameService gameService;

    private User testUser;
    private Game testGame;
    private CreateGameRequest vsBotRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setIsGuest(false);

        testGame = new Game();
        testGame.setId(42L);
        testGame.setGameType(GameType.VS_BOT);
        testGame.setBoardSize(BoardSize.THREE);
        testGame.setPlayer1(testUser);
        testGame.setBotDifficulty(BotDifficulty.EASY);
        testGame.setStatus(GameStatus.WAITING);

        vsBotRequest = new CreateGameRequest(
                GameType.VS_BOT,
                3,
                BotDifficulty.EASY
        );
    }

    @Test
    void createGame_shouldCreateVsBotGameSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);
        when(boardStateService.generateBoardState(any(Game.class), eq(List.of())))
                .thenReturn(new BoardState(new String[3][3]));

        CreateGameResponse response = gameService.createGame(vsBotRequest, 1L);

        assertThat(response.gameId()).isEqualTo(42L);
        assertThat(response.gameType()).isEqualTo(GameType.VS_BOT);
        assertThat(response.boardSize()).isEqualTo(BoardSize.THREE);
        assertThat(response.player1Id()).isEqualTo(1L);
        assertThat(response.botDifficulty()).isEqualTo(BotDifficulty.EASY);
        assertThat(response.status()).isEqualTo(GameStatus.WAITING);

        verify(gameRepository).save(any(Game.class));
    }

    @Test
    void createGame_shouldThrowBadRequestWhenVsBotWithoutDifficulty() {
        CreateGameRequest invalidRequest = new CreateGameRequest(
                GameType.VS_BOT,
                3,
                null
        );

        assertThatThrownBy(() -> gameService.createGame(invalidRequest, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("botDifficulty is required for vs_bot games");

        verify(gameRepository, never()).save(any());
    }

    @Test
    void createGame_shouldThrowBadRequestWhenPvpWithDifficulty() {
        CreateGameRequest invalidRequest = new CreateGameRequest(
                GameType.PVP,
                3,
                BotDifficulty.EASY
        );

        assertThatThrownBy(() -> gameService.createGame(invalidRequest, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("botDifficulty must be null for pvp games");

        verify(gameRepository, never()).save(any());
    }

    @Test
    void getBoardState_shouldReturnBoardStateForValidGame() {
        when(gameRepository.findById(42L)).thenReturn(Optional.of(testGame));

        Move move = new Move();
        move.setRow((short) 0);
        move.setCol((short) 0);
        move.setPlayerSymbol(PlayerSymbol.X);
        move.setMoveOrder((short) 1);

        when(moveRepository.findByGameIdOrderByMoveOrderAsc(42L)).thenReturn(Arrays.asList(move));
        when(moveRepository.findFirstByGameIdOrderByMoveOrderDesc(42L)).thenReturn(Optional.of(move));
        when(boardStateService.generateBoardState(any(Game.class), any())).thenReturn(new BoardState(new String[3][3]));

        BoardStateResponse response = gameService.getBoardState(42L, 1L);

        assertThat(response.boardSize()).isEqualTo(BoardSize.THREE);
        assertThat(response.totalMoves()).isEqualTo(1);
        assertThat(response.lastMove()).isNotNull();
        assertThat(response.lastMove().row()).isEqualTo(0);
        assertThat(response.lastMove().col()).isEqualTo(0);
        assertThat(response.lastMove().playerSymbol()).isEqualTo(PlayerSymbol.X);
    }

    @Test
    void getBoardState_shouldThrowGameNotFoundForInvalidGameId() {
        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.getBoardState(999L, 1L))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessage("Game not found");
    }

    @Test
    void getBoardState_shouldThrowForbiddenForNonParticipant() {
        testGame.setPlayer2(null);
        when(gameRepository.findById(42L)).thenReturn(Optional.of(testGame));

        assertThatThrownBy(() -> gameService.getBoardState(42L, 999L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You are not a participant of this game");
    }

    @Test
    void updateGameStatus_shouldUpdateStatusSuccessfully() {
        testGame.setStatus(GameStatus.IN_PROGRESS);
        when(gameRepository.findById(42L)).thenReturn(Optional.of(testGame));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);

        UpdateGameStatusResponse response = gameService.updateGameStatus(42L, GameStatus.ABANDONED, 1L);

        assertThat(response.gameId()).isEqualTo(42L);
        assertThat(response.status()).isEqualTo(GameStatus.ABANDONED);
        verify(gameRepository).save(any(Game.class));
    }

    @Test
    void updateGameStatus_shouldThrowBadRequestForInvalidTransition() {
        testGame.setStatus(GameStatus.FINISHED);
        when(gameRepository.findById(42L)).thenReturn(Optional.of(testGame));

        assertThatThrownBy(() -> gameService.updateGameStatus(42L, GameStatus.IN_PROGRESS, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot change status from FINISHED");
    }

    @Test
    void getGames_shouldReturnPaginatedGamesList() {
        Page<Game> gamePage = new PageImpl<>(Arrays.asList(testGame));
        when(gameRepository.findByUserIdAndFilters(eq(1L), any(), any(), any(Pageable.class)))
                .thenReturn(gamePage);
        when(moveRepository.countByGameId(42L)).thenReturn(5L);

        GameListResponse response = gameService.getGames(1L, null, null, Pageable.unpaged());

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).totalMoves()).isEqualTo(5);
    }

    @Test
    void getGameDetail_shouldReturnFullGameDetails() {
        testGame.setStatus(GameStatus.IN_PROGRESS);
        testGame.setCurrentPlayerSymbol(PlayerSymbol.X);
        testGame.setLastMoveAt(Instant.now());

        when(gameRepository.findByIdWithPlayers(42L)).thenReturn(Optional.of(testGame));
        when(moveRepository.findByGameIdOrderByMoveOrderAsc(42L)).thenReturn(List.of());
        when(boardStateService.generateBoardState(any(Game.class), any())).thenReturn(new BoardState(new String[3][3]));

        GameDetailResponse response = gameService.getGameDetail(42L, 1L);

        assertThat(response.gameId()).isEqualTo(42L);
        assertThat(response.gameType()).isEqualTo(GameType.VS_BOT);
        assertThat(response.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(response.currentPlayerSymbol()).isEqualTo(PlayerSymbol.X);
    }
}

