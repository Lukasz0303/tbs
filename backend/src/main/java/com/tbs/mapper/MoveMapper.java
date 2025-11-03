package com.tbs.mapper;

import com.tbs.dto.move.MoveListItem;
import com.tbs.model.Move;

public class MoveMapper {

    private MoveMapper() {
    }

    public static MoveListItem toMoveListItem(Move move) {
        return new MoveListItem(
                move.getId(),
                move.getRow(),
                move.getCol(),
                move.getPlayerSymbol(),
                move.getMoveOrder(),
                move.getPlayer() != null ? move.getPlayer().getId() : null,
                move.getPlayer() != null ? move.getPlayer().getUsername() : null,
                move.getCreatedAt()
        );
    }
}

