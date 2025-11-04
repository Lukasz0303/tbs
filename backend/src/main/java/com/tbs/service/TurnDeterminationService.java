package com.tbs.service;

import com.tbs.enums.PlayerSymbol;
import com.tbs.model.Game;
import com.tbs.model.Move;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TurnDeterminationService {

    public PlayerSymbol determinePlayer1Symbol(Game game, List<Move> existingMoves) {
        if (!existingMoves.isEmpty()) {
            for (Move move : existingMoves) {
                if (move.getPlayer() != null && move.getPlayer().getId().equals(game.getPlayer1().getId())) {
                    return move.getPlayerSymbol();
                }
            }
        }
        return PlayerSymbol.X;
    }
}

