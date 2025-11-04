package com.tbs.service;

import com.tbs.enums.PlayerSymbol;
import com.tbs.model.Game;
import com.tbs.model.Move;
import com.tbs.model.User;
import com.tbs.repository.MoveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoveCreationService {

    private final MoveRepository moveRepository;

    public MoveCreationService(MoveRepository moveRepository) {
        this.moveRepository = moveRepository;
    }

    @Transactional
    public Move createAndSaveMove(Game game, int row, int col, PlayerSymbol symbol, User player) {
        short moveOrder = moveRepository.getNextMoveOrder(game.getId());
        
        Move move = new Move();
        move.setGame(game);
        move.setPlayer(player);
        move.setRow((short) row);
        move.setCol((short) col);
        move.setPlayerSymbol(symbol);
        move.setMoveOrder(moveOrder);
        
        return moveRepository.save(move);
    }
}

