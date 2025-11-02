package com.tbs.service;

import com.tbs.dto.common.BoardState;
import com.tbs.model.Game;
import com.tbs.model.Move;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardStateService {

    public BoardState generateBoardState(Game game, List<Move> moves) {
        int size = game.getBoardSize().getValue();
        String[][] cells = new String[size][size];
        
        for (Move move : moves) {
            int row = move.getRow();
            int col = move.getCol();
            
            if (row < 0 || row >= size || col < 0 || col >= size) {
                throw new IllegalArgumentException(
                    String.format("Move position (%d, %d) is out of bounds for board size %d", row, col, size)
                );
            }
            
            if (cells[row][col] != null) {
                throw new IllegalArgumentException(
                    String.format("Duplicate move at position (%d, %d)", row, col)
                );
            }
            
            cells[row][col] = move.getPlayerSymbol().getValue();
        }
        
        return new BoardState(cells);
    }
}

