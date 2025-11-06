package com.tbs.service;

import com.tbs.dto.move.BotMoveResponse;
import com.tbs.dto.move.CreateMoveRequest;
import com.tbs.dto.move.CreateMoveResponse;
import com.tbs.dto.move.MoveListItem;
import com.tbs.dto.common.BoardState;
import com.tbs.dto.user.WinnerInfo;
import com.tbs.enums.GameStatus;
import com.tbs.enums.GameType;
import com.tbs.enums.PlayerSymbol;
import com.tbs.exception.ForbiddenException;
import com.tbs.exception.GameNotFoundException;
import com.tbs.exception.GameNotInProgressException;
import com.tbs.exception.InvalidGameTypeException;
import com.tbs.exception.InvalidMoveException;
import com.tbs.exception.UserNotFoundException;
import com.tbs.mapper.MoveMapper;
import com.tbs.model.Game;
import com.tbs.model.Move;
import com.tbs.model.User;
import com.tbs.repository.GameRepository;
import com.tbs.repository.MoveRepository;
import com.tbs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MoveService {

    private static final Logger log = LoggerFactory.getLogger(MoveService.class);

    private final MoveRepository moveRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final BoardStateService boardStateService;
    private final GameLogicService gameLogicService;
    private final BotService botService;
    private final GameValidationService gameValidationService;
    private final com.tbs.websocket.GameWebSocketHandler gameWebSocketHandler;
    private final TurnValidationService turnValidationService;
    private final MoveCreationService moveCreationService;
    private final TurnDeterminationService turnDeterminationService;
    private final BotUserService botUserService;
    private final PointsService pointsService;

    public MoveService(MoveRepository moveRepository, GameRepository gameRepository,
                       UserRepository userRepository, BoardStateService boardStateService,
                       GameLogicService gameLogicService, BotService botService,
                       GameValidationService gameValidationService,
                       com.tbs.websocket.GameWebSocketHandler gameWebSocketHandler,
                       TurnValidationService turnValidationService,
                       MoveCreationService moveCreationService,
                       TurnDeterminationService turnDeterminationService,
                       BotUserService botUserService, PointsService pointsService) {
        this.moveRepository = Objects.requireNonNull(moveRepository, "MoveRepository cannot be null");
        this.gameRepository = Objects.requireNonNull(gameRepository, "GameRepository cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.boardStateService = Objects.requireNonNull(boardStateService, "BoardStateService cannot be null");
        this.gameLogicService = Objects.requireNonNull(gameLogicService, "GameLogicService cannot be null");
        this.botService = Objects.requireNonNull(botService, "BotService cannot be null");
        this.gameValidationService = Objects.requireNonNull(gameValidationService, "GameValidationService cannot be null");
        this.gameWebSocketHandler = Objects.requireNonNull(gameWebSocketHandler, "GameWebSocketHandler cannot be null");
        this.turnValidationService = Objects.requireNonNull(turnValidationService, "TurnValidationService cannot be null");
        this.moveCreationService = Objects.requireNonNull(moveCreationService, "MoveCreationService cannot be null");
        this.turnDeterminationService = Objects.requireNonNull(turnDeterminationService, "TurnDeterminationService cannot be null");
        this.botUserService = Objects.requireNonNull(botUserService, "BotUserService cannot be null");
        this.pointsService = Objects.requireNonNull(pointsService, "PointsService cannot be null");
    }

    @Transactional(readOnly = true)
    public List<MoveListItem> getMovesByGameId(Long gameId, Long userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        gameValidationService.validateParticipation(game, userId);

        List<Move> moves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);

        log.info("Retrieved {} moves for game {} by user {}", moves.size(), gameId, userId);

        return moves.stream()
                .map(MoveMapper::toMoveListItem)
                .collect(Collectors.toList());
    }

    @Transactional
    public CreateMoveResponse createMove(Long gameId, CreateMoveRequest request, Long userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        gameValidationService.validateParticipation(game, userId);

        if (game.getStatus() != GameStatus.WAITING && game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameNotInProgressException("Game is not in progress");
        }

        int boardSize = game.getBoardSize().getValue();
        if (request.row() < 0 || request.row() >= boardSize || 
            request.col() < 0 || request.col() >= boardSize) {
            throw new InvalidMoveException(
                String.format("Move coordinates (%d, %d) are out of bounds for board size %d", 
                    request.row(), request.col(), boardSize));
        }

        List<Move> existingMoves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        
        if (game.getStatus() == GameStatus.WAITING) {
            game.setStatus(GameStatus.IN_PROGRESS);
            if (game.getCurrentPlayerSymbol() == null) {
                game.setCurrentPlayerSymbol(request.playerSymbol());
            }
        }
        
        turnValidationService.validatePlayerTurn(game, existingMoves, request.playerSymbol(), userId);
        
        gameLogicService.validateMove(game, existingMoves, request.row(), request.col(), request.playerSymbol());

        User player = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Move savedMove = moveCreationService.createAndSaveMove(game, request.row(), request.col(), request.playerSymbol(), player);
        log.info("Created move {} for game {} by user {}", savedMove.getId(), gameId, userId);

        List<Move> allMoves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        BoardState boardState = boardStateService.generateBoardState(game, allMoves);

        GameStateUpdateResult stateUpdate = updateGameStateAfterMove(
                game, boardState, request.playerSymbol(), player, userId
        );

        game.setLastMoveAt(Instant.now());
        Game updatedGame = gameRepository.save(game);

        log.info("MoveService.createMove: gameId={}, gameType={}, status={}", 
                gameId, game.getGameType(), updatedGame.getStatus());
        
        if (game.getGameType() != GameType.PVP) {
            log.debug("Game is not PVP (gameType={}), skipping WebSocket notification", game.getGameType());
            return new CreateMoveResponse(
                    savedMove.getId(),
                    gameId,
                    request.row(),
                    request.col(),
                    request.playerSymbol(),
                    savedMove.getMoveOrder(),
                    savedMove.getCreatedAt(),
                    boardState,
                    updatedGame.getStatus(),
                    stateUpdate.winner(),
                    updatedGame.getCurrentPlayerSymbol()
            );
        }
        
        log.info("Game is PVP, calling notifyMoveFromRestApi: gameId={}, userId={}, moveId={}", 
                gameId, userId, savedMove.getId());
        try {
            gameWebSocketHandler.notifyMoveFromRestApi(
                    gameId,
                    userId,
                    savedMove.getId(),
                    request.row(),
                    request.col(),
                    request.playerSymbol(),
                    boardState,
                    updatedGame.getCurrentPlayerSymbol(),
                    updatedGame.getStatus()
            );
            log.info("Successfully called notifyMoveFromRestApi for gameId={}, userId={}", gameId, userId);
        } catch (Exception e) {
            log.error("Failed to notify WebSocket about move from REST API: gameId={}, userId={}", 
                    gameId, userId, e);
        }

        return new CreateMoveResponse(
                savedMove.getId(),
                gameId,
                request.row(),
                request.col(),
                request.playerSymbol(),
                savedMove.getMoveOrder(),
                savedMove.getCreatedAt(),
                boardState,
                updatedGame.getStatus(),
                stateUpdate.winner(),
                updatedGame.getCurrentPlayerSymbol()
        );
    }

    @Transactional
    public BotMoveResponse createBotMove(Long gameId, Long userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        gameValidationService.validateParticipation(game, userId);

        if (game.getPlayer1() == null) {
            throw new IllegalStateException("Game must have player1");
        }

        if (!game.getPlayer1().getId().equals(userId)) {
            log.warn("User {} attempted to trigger bot move for game {} but is not player1", userId, gameId);
            throw new ForbiddenException("Only player1 can trigger bot moves in vs_bot games");
        }

        if (game.getGameType() != GameType.VS_BOT) {
            throw new InvalidGameTypeException("Game is not a vs_bot game");
        }

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameNotInProgressException("Game is not in progress");
        }

        List<Move> existingMoves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        BoardState boardState = boardStateService.generateBoardState(game, existingMoves);

        PlayerSymbol currentSymbol = game.getCurrentPlayerSymbol();
        if (currentSymbol == null) {
            throw new GameNotInProgressException("Game has not started yet");
        }

        PlayerSymbol player1Symbol = turnDeterminationService.determinePlayer1Symbol(game, existingMoves);
        PlayerSymbol botSymbol = gameLogicService.getOppositeSymbol(player1Symbol);

        if (currentSymbol != botSymbol) {
            throw new InvalidMoveException("It's not bot's turn");
        }
        
        BotService.BotMovePosition botPosition = botService.generateBotMove(
                boardState, 
                game.getBotDifficulty(), 
                botSymbol, 
                game.getBoardSize().getValue(),
                game
        );

        User botUser = botUserService.getBotUser();
        Move savedMove = moveCreationService.createAndSaveMove(game, botPosition.row(), botPosition.col(), botSymbol, botUser);
        log.info("Created bot move {} for game {} with difficulty {}", savedMove.getId(), gameId, game.getBotDifficulty());

        List<Move> allMoves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        boardState = boardStateService.generateBoardState(game, allMoves);

        GameStateUpdateResult stateUpdate = updateGameStateAfterBotMove(
                game, boardState, botSymbol, gameId
        );

        game.setLastMoveAt(Instant.now());
        Game updatedGame = gameRepository.save(game);

        return new BotMoveResponse(
                savedMove.getId(),
                gameId,
                botPosition.row(),
                botPosition.col(),
                botSymbol,
                savedMove.getMoveOrder(),
                savedMove.getCreatedAt(),
                boardState,
                updatedGame.getStatus(),
                stateUpdate.winner()
        );
    }

    private GameStateUpdateResult updateGameStateAfterMove(
            Game game, BoardState boardState, PlayerSymbol moveSymbol,
            User player, Long userId) {
        return updateGameStateAfterMoveInternal(game, boardState, moveSymbol, player);
    }

    private GameStateUpdateResult updateGameStateAfterBotMove(
            Game game, BoardState boardState, PlayerSymbol botSymbol, Long gameId) {
        User botUser = botUserService.getBotUser();
        return updateGameStateAfterMoveInternal(game, boardState, botSymbol, botUser);
    }

    private GameStateUpdateResult updateGameStateAfterMoveInternal(
            Game game, BoardState boardState, PlayerSymbol moveSymbol, User winnerUser) {
        boolean isWin = gameLogicService.checkWinCondition(game, boardState, moveSymbol);

        if (isWin) {
            if (winnerUser == null) {
                log.error("Game {} finished but winnerUser is null", game.getId());
                throw new IllegalStateException("Winner user cannot be null when game is won");
            }

            game.setStatus(GameStatus.FINISHED);
            game.setWinner(winnerUser);
            game.setFinishedAt(Instant.now());

            pointsService.awardPointsForWin(game, winnerUser);

            WinnerInfo winner = new WinnerInfo(winnerUser.getId(), winnerUser.getUsername());
            log.info("Game {} finished. Winner: user {}", game.getId(), winnerUser.getId());
            return new GameStateUpdateResult(winner);
        }

        if (gameLogicService.checkDrawCondition(game, boardState)) {
            game.setStatus(GameStatus.DRAW);
            game.setFinishedAt(Instant.now());
            pointsService.awardPointsForDraw(game);
            log.info("Game {} ended in draw", game.getId());
            return new GameStateUpdateResult(null);
        }

        PlayerSymbol nextPlayerSymbol = gameLogicService.getOppositeSymbol(moveSymbol);
        game.setCurrentPlayerSymbol(nextPlayerSymbol);
        return new GameStateUpdateResult(null);
    }

    private static record GameStateUpdateResult(WinnerInfo winner) {}
}

