package com.tbs.service;

import com.tbs.exception.ForbiddenException;
import com.tbs.model.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GameValidationService {

    private static final Logger log = LoggerFactory.getLogger(GameValidationService.class);

    public void validateParticipation(Game game, Long userId) {
        if (game.getPlayer1() == null) {
            log.error("Game {} has null player1", game.getId());
            throw new IllegalStateException("Game must have player1");
        }
        if (!game.getPlayer1().getId().equals(userId) &&
                (game.getPlayer2() == null || !game.getPlayer2().getId().equals(userId))) {
            log.warn("User {} attempted to access game {} they are not a participant of", userId, game.getId());
            throw new ForbiddenException("You are not a participant of this game");
        }
    }
}
