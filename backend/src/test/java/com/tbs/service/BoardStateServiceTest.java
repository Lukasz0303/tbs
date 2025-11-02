package com.tbs.service;

import com.tbs.dto.common.BoardState;
import com.tbs.enums.BoardSize;
import com.tbs.enums.GameType;
import com.tbs.enums.PlayerSymbol;
import com.tbs.model.Game;
import com.tbs.model.Move;
import com.tbs.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BoardStateServiceTest {

    @InjectMocks
    private BoardStateService boardStateService;

    private Game game;
    private User player1;
    private User player2;

    @BeforeEach
    void setUp() {
        player1 = new User();
        player1.setId(1L);
        player1.setUsername("player1");

        player2 = new User();
        player2.setId(2L);
        player2.setUsername("player2");

        game = new Game();
        game.setId(1L);
        game.setGameType(GameType.VS_BOT);
        game.setBoardSize(BoardSize.THREE);
        game.setPlayer1(player1);
    }

    @Test
    void generateBoardState_shouldReturnEmptyBoardForNoMoves() {
        BoardState result = boardStateService.generateBoardState(game, List.of());

        String[][] state = result.state();
        assertThat(state).hasDimensions(3, 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertThat(state[i][j]).isNull();
            }
        }
    }

    @Test
    void generateBoardState_shouldGenerateCorrectBoardWithMoves() {
        Move move1 = new Move();
        move1.setRow((short) 0);
        move1.setCol((short) 0);
        move1.setPlayerSymbol(PlayerSymbol.X);

        Move move2 = new Move();
        move2.setRow((short) 1);
        move2.setCol((short) 1);
        move2.setPlayerSymbol(PlayerSymbol.O);

        Move move3 = new Move();
        move3.setRow((short) 0);
        move3.setCol((short) 2);
        move3.setPlayerSymbol(PlayerSymbol.X);

        List<Move> moves = Arrays.asList(move1, move2, move3);

        BoardState result = boardStateService.generateBoardState(game, moves);

        String[][] state = result.state();
        assertThat(state[0][0]).isEqualTo("x");
        assertThat(state[1][1]).isEqualTo("o");
        assertThat(state[0][2]).isEqualTo("x");
        assertThat(state[0][1]).isNull();
        assertThat(state[1][0]).isNull();
        assertThat(state[2][2]).isNull();
    }

    @Test
    void generateBoardState_shouldHandleLargeBoard() {
        game.setBoardSize(BoardSize.FIVE);

        Move move1 = new Move();
        move1.setRow((short) 0);
        move1.setCol((short) 0);
        move1.setPlayerSymbol(PlayerSymbol.X);

        Move move2 = new Move();
        move2.setRow((short) 4);
        move2.setCol((short) 4);
        move2.setPlayerSymbol(PlayerSymbol.O);

        BoardState result = boardStateService.generateBoardState(game, Arrays.asList(move1, move2));

        String[][] state = result.state();
        assertThat(state).hasDimensions(5, 5);
        assertThat(state[0][0]).isEqualTo("x");
        assertThat(state[4][4]).isEqualTo("o");
        assertThat(state[2][2]).isNull();
    }
}

