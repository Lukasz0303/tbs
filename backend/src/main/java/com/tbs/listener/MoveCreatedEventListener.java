package com.tbs.listener;

import com.tbs.event.MoveCreatedEvent;
import com.tbs.websocket.GameWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MoveCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(MoveCreatedEventListener.class);

    private final GameWebSocketHandler gameWebSocketHandler;

    public MoveCreatedEventListener(GameWebSocketHandler gameWebSocketHandler) {
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMoveCreated(MoveCreatedEvent event) {
        try {
            log.debug("Processing MoveCreatedEvent: gameId={}, userId={}, moveId={}", 
                    event.gameId(), event.userId(), event.moveId());
            gameWebSocketHandler.notifyMoveFromRestApi(
                    event.gameId(),
                    event.userId(),
                    event.moveId(),
                    event.row(),
                    event.col(),
                    event.playerSymbol(),
                    event.boardState(),
                    event.currentPlayerSymbol(),
                    event.gameStatus()
            );
            log.debug("Successfully processed MoveCreatedEvent for gameId={}", event.gameId());
        } catch (Exception e) {
            log.error("Failed to process MoveCreatedEvent: gameId={}, userId={}", 
                    event.gameId(), event.userId(), e);
        }
    }
}

