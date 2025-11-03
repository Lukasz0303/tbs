package com.tbs.service;

import com.tbs.dto.common.BoardState;
import com.tbs.enums.BotDifficulty;
import com.tbs.enums.PlayerSymbol;
import com.tbs.model.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BotService {

    private static final Logger log = LoggerFactory.getLogger(BotService.class);
    private static final int CENTER_POSITION_3X3 = 1;
    private static final int LAST_INDEX_3X3 = 2;

    private final GameLogicService gameLogicService;

    public BotService(GameLogicService gameLogicService) {
        this.gameLogicService = gameLogicService;
    }

    public record BotMovePosition(int row, int col) {}

    public BotMovePosition generateBotMove(BoardState boardState, BotDifficulty difficulty, PlayerSymbol botSymbol, int boardSize, Game game) {
        String[][] cells = boardState.state();
        if (cells.length != boardSize) {
            throw new IllegalArgumentException("Board size mismatch: expected " + boardSize + " rows, got " + cells.length);
        }
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == null || cells[i].length != boardSize) {
                throw new IllegalArgumentException("Board size mismatch: row " + i + " has invalid length");
            }
        }
        
        List<BotMovePosition> availablePositions = getAvailablePositions(boardState, boardSize);
        
        if (availablePositions.isEmpty()) {
            throw new IllegalArgumentException("No available positions for bot move");
        }

        BotMovePosition move = switch (difficulty) {
            case EASY -> generateEasyMove(availablePositions);
            case MEDIUM -> generateMediumMove(boardState, availablePositions, botSymbol, boardSize, game);
            case HARD -> generateHardMove(boardState, availablePositions, botSymbol, boardSize, game);
        };
        
        log.debug("Generated bot move: difficulty={}, position=({}, {})", difficulty, move.row(), move.col());
        return move;
    }

    private BotMovePosition generateEasyMove(List<BotMovePosition> availablePositions) {
        Collections.shuffle(availablePositions, ThreadLocalRandom.current());
        return availablePositions.get(0);
    }

    private BotMovePosition generateMediumMove(BoardState boardState, List<BotMovePosition> availablePositions, 
                                               PlayerSymbol botSymbol, int boardSize, Game game) {
        BotMovePosition winMove = findWinningMove(boardState, availablePositions, botSymbol, boardSize, game);
        if (winMove != null) {
            return winMove;
        }

        PlayerSymbol opponentSymbol = botSymbol == PlayerSymbol.X ? PlayerSymbol.O : PlayerSymbol.X;
        BotMovePosition blockMove = findWinningMove(boardState, availablePositions, opponentSymbol, boardSize, game);
        if (blockMove != null) {
            return blockMove;
        }

        return generateEasyMove(availablePositions);
    }

    private BotMovePosition generateHardMove(BoardState boardState, List<BotMovePosition> availablePositions,
                                             PlayerSymbol botSymbol, int boardSize, Game game) {
        if (boardSize == 3) {
            return generateHardMove3x3(boardState, availablePositions, botSymbol, game);
        }
        return generateMediumMove(boardState, availablePositions, botSymbol, boardSize, game);
    }

    private BotMovePosition generateHardMove3x3(BoardState boardState, List<BotMovePosition> availablePositions,
                                                PlayerSymbol botSymbol, Game game) {
        BotMovePosition winMove = findWinningMove(boardState, availablePositions, botSymbol, 3, game);
        if (winMove != null) {
            return winMove;
        }

        PlayerSymbol opponentSymbol = botSymbol == PlayerSymbol.X ? PlayerSymbol.O : PlayerSymbol.X;
        BotMovePosition blockMove = findWinningMove(boardState, availablePositions, opponentSymbol, 3, game);
        if (blockMove != null) {
            return blockMove;
        }

        BotMovePosition centerOpt = availablePositions.stream()
                .filter(pos -> pos.row() == CENTER_POSITION_3X3 && pos.col() == CENTER_POSITION_3X3)
                .findFirst()
                .orElse(null);
        
        if (centerOpt != null) {
            return centerOpt;
        }

        List<BotMovePosition> corners = List.of(
            new BotMovePosition(0, 0),
            new BotMovePosition(0, LAST_INDEX_3X3),
            new BotMovePosition(LAST_INDEX_3X3, 0),
            new BotMovePosition(LAST_INDEX_3X3, LAST_INDEX_3X3)
        );
        
        List<BotMovePosition> availableCorners = availablePositions.stream()
                .filter(pos -> corners.stream()
                        .anyMatch(corner -> corner.row() == pos.row() && corner.col() == pos.col()))
                .toList();
        
        if (!availableCorners.isEmpty()) {
            return availableCorners.get(ThreadLocalRandom.current().nextInt(availableCorners.size()));
        }

        return generateEasyMove(availablePositions);
    }

    private BotMovePosition findWinningMove(BoardState boardState, List<BotMovePosition> availablePositions,
                                           PlayerSymbol symbol, int boardSize, Game game) {
        String symbolValue = symbol.getValue();
        String[][] originalCells = boardState.state();
        String[][] cells = deepCopyArray(originalCells);

        for (BotMovePosition pos : availablePositions) {
            cells[pos.row()][pos.col()] = symbolValue;
            
            BoardState testBoardState = new BoardState(cells);
            boolean wins = gameLogicService.checkWinCondition(game, testBoardState, symbol);
            
            cells[pos.row()][pos.col()] = originalCells[pos.row()][pos.col()];

            if (wins) {
                return pos;
            }
        }

        return null;
    }

    private String[][] deepCopyArray(String[][] original) {
        String[][] copy = new String[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = Arrays.copyOf(original[i], original[i].length);
        }
        return copy;
    }

    private List<BotMovePosition> getAvailablePositions(BoardState boardState, int boardSize) {
        List<BotMovePosition> positions = new ArrayList<>();
        String[][] cells = boardState.state();

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (cells[row][col] == null || cells[row][col].isEmpty()) {
                    positions.add(new BotMovePosition(row, col));
                }
            }
        }

        return positions;
    }
}

