package com.tbs.service;

import com.tbs.dto.common.BoardState;
import com.tbs.enums.PlayerSymbol;
import com.tbs.model.Game;
import com.tbs.model.Move;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class GameLogicService {

    public void validateMove(Game game, List<Move> existingMoves, int row, int col, PlayerSymbol playerSymbol) {
        int boardSize = game.getBoardSize().getValue();
        
        if (row < 0 || row >= boardSize) {
            throw new com.tbs.exception.InvalidMoveException(
                String.format("Row %d is out of board bounds (0-%d)", row, boardSize - 1)
            );
        }
        
        if (col < 0 || col >= boardSize) {
            throw new com.tbs.exception.InvalidMoveException(
                String.format("Column %d is out of board bounds (0-%d)", col, boardSize - 1)
            );
        }
        
        List<Move> movesToCheck = existingMoves != null ? existingMoves : Collections.emptyList();
        
        boolean positionOccupied = movesToCheck.stream()
            .anyMatch(m -> m.getRow() == row && m.getCol() == col);
        
        if (positionOccupied) {
            throw new com.tbs.exception.InvalidMoveException(
                String.format("Position (%d, %d) is already occupied", row, col)
            );
        }
        
        PlayerSymbol currentSymbol = game.getCurrentPlayerSymbol();
        boolean isFirstMove = movesToCheck.isEmpty();
        
        if (currentSymbol == null && !isFirstMove) {
            throw new com.tbs.exception.InvalidMoveException(
                "Game is in progress but current player symbol is not set"
            );
        }
        
        if (currentSymbol != null && currentSymbol != playerSymbol) {
            throw new com.tbs.exception.InvalidMoveException(
                String.format("It's not %s turn. Current player: %s", 
                    playerSymbol.getValue(), 
                    currentSymbol.getValue())
            );
        }
    }

    public boolean checkWinCondition(Game game, BoardState boardState, PlayerSymbol symbol) {
        String symbolValue = symbol.getValue();
        int size = game.getBoardSize().getValue();
        String[][] cells = boardState.state();
        
        for (int i = 0; i < size; i++) {
            boolean rowWin = true;
            boolean colWin = true;
            
            for (int j = 0; j < size; j++) {
                if (!Objects.equals(symbolValue, cells[i][j])) {
                    rowWin = false;
                }
                if (!Objects.equals(symbolValue, cells[j][i])) {
                    colWin = false;
                }
            }
            
            if (rowWin || colWin) {
                return true;
            }
        }
        
        boolean diag1Win = true;
        boolean diag2Win = true;
        
        for (int i = 0; i < size; i++) {
            if (!Objects.equals(symbolValue, cells[i][i])) {
                diag1Win = false;
            }
            if (!Objects.equals(symbolValue, cells[i][size - 1 - i])) {
                diag2Win = false;
            }
        }
        
        return diag1Win || diag2Win;
    }

    public boolean checkDrawCondition(Game game, BoardState boardState) {
        PlayerSymbol playerX = PlayerSymbol.X;
        PlayerSymbol playerO = PlayerSymbol.O;
        
        if (checkWinCondition(game, boardState, playerX) || checkWinCondition(game, boardState, playerO)) {
            return false;
        }
        
        int size = game.getBoardSize().getValue();
        String[][] cells = boardState.state();
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (cells[i][j] == null || cells[i][j].isEmpty()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    public PlayerSymbol getOppositeSymbol(PlayerSymbol symbol) {
        return symbol == PlayerSymbol.X ? PlayerSymbol.O : PlayerSymbol.X;
    }
}

