package com.tbs.service;

import com.tbs.enums.GameType;
import com.tbs.enums.PlayerSymbol;
import com.tbs.exception.ForbiddenException;
import com.tbs.model.Game;
import com.tbs.model.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TurnValidationService {

    private static final Logger log = LoggerFactory.getLogger(TurnValidationService.class);

    private final TurnDeterminationService turnDeterminationService;

    public TurnValidationService(TurnDeterminationService turnDeterminationService) {
        this.turnDeterminationService = turnDeterminationService;
    }

    public void validatePlayerTurn(Game game, List<Move> existingMoves, PlayerSymbol playerSymbol, Long userId) {
        PlayerSymbol currentPlayerSymbol = game.getCurrentPlayerSymbol();
        
        if (game.getStatus() == com.tbs.enums.GameStatus.IN_PROGRESS && currentPlayerSymbol == null) {
            throw new IllegalStateException("Game is IN_PROGRESS but currentPlayerSymbol is null");
        }
        
        if (currentPlayerSymbol == null) {
            return;
        }
        
        if (game.getGameType() == GameType.VS_BOT) {
            PlayerSymbol player1Symbol = turnDeterminationService.determinePlayer1Symbol(game, existingMoves);
            boolean isPlayer1Turn = currentPlayerSymbol == player1Symbol;
            boolean isCurrentUserPlayer1 = game.getPlayer1().getId().equals(userId);
            
            if (!isCurrentUserPlayer1) {
                throw new ForbiddenException("Only player1 can make moves in vs_bot games");
            }
            
            if (!isPlayer1Turn) {
                throw new ForbiddenException("It's not your turn");
            }
            
            if (currentPlayerSymbol != playerSymbol) {
                throw new ForbiddenException("It's not your turn");
            }
            
            return;
        }
        
        PlayerSymbol player1Symbol = turnDeterminationService.determinePlayer1Symbol(game, existingMoves);
        boolean isPlayer1Turn = currentPlayerSymbol == player1Symbol;
        boolean isCurrentUserPlayer1 = game.getPlayer1().getId().equals(userId);
        
        if (isPlayer1Turn && !isCurrentUserPlayer1) {
            throw new ForbiddenException("It's not your turn");
        }
        if (!isPlayer1Turn && (game.getPlayer2() == null || !game.getPlayer2().getId().equals(userId))) {
            throw new ForbiddenException("It's not your turn");
        }
        
        if (currentPlayerSymbol != playerSymbol) {
            throw new ForbiddenException("It's not your turn");
        }
    }
}

