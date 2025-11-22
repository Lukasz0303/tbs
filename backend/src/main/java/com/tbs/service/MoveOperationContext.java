package com.tbs.service;

import org.springframework.stereotype.Component;

@Component
public class MoveOperationContext {
    private final BoardStateService boardStateService;
    private final GameLogicService gameLogicService;
    private final BotService botService;
    private final GameValidationService gameValidationService;
    private final TurnValidationService turnValidationService;
    private final MoveCreationService moveCreationService;
    private final TurnDeterminationService turnDeterminationService;
    private final BotUserService botUserService;
    private final PointsService pointsService;

    public MoveOperationContext(
            BoardStateService boardStateService,
            GameLogicService gameLogicService,
            BotService botService,
            GameValidationService gameValidationService,
            TurnValidationService turnValidationService,
            MoveCreationService moveCreationService,
            TurnDeterminationService turnDeterminationService,
            BotUserService botUserService,
            PointsService pointsService
    ) {
        this.boardStateService = boardStateService;
        this.gameLogicService = gameLogicService;
        this.botService = botService;
        this.gameValidationService = gameValidationService;
        this.turnValidationService = turnValidationService;
        this.moveCreationService = moveCreationService;
        this.turnDeterminationService = turnDeterminationService;
        this.botUserService = botUserService;
        this.pointsService = pointsService;
    }

    public BoardStateService getBoardStateService() {
        return boardStateService;
    }

    public GameLogicService getGameLogicService() {
        return gameLogicService;
    }

    public BotService getBotService() {
        return botService;
    }

    public GameValidationService getGameValidationService() {
        return gameValidationService;
    }

    public TurnValidationService getTurnValidationService() {
        return turnValidationService;
    }

    public MoveCreationService getMoveCreationService() {
        return moveCreationService;
    }

    public TurnDeterminationService getTurnDeterminationService() {
        return turnDeterminationService;
    }

    public BotUserService getBotUserService() {
        return botUserService;
    }

    public PointsService getPointsService() {
        return pointsService;
    }
}

