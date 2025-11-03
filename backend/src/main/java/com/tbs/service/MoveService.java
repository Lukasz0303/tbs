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
import java.util.stream.Collectors;

@Service
public class MoveService {

    private static final Logger log = LoggerFactory.getLogger(MoveService.class);
    private static final Long BOT_USER_ID = 0L;
    private static final String BOT_USERNAME = "Bot";

    private final MoveRepository moveRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final BoardStateService boardStateService;
    private final GameLogicService gameLogicService;
    private final BotService botService;

    public MoveService(MoveRepository moveRepository, GameRepository gameRepository,
                       UserRepository userRepository, BoardStateService boardStateService,
                       GameLogicService gameLogicService, BotService botService) {
        this.moveRepository = moveRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.boardStateService = boardStateService;
        this.gameLogicService = gameLogicService;
        this.botService = botService;
    }

    @Transactional(readOnly = true)
    public List<MoveListItem> getMovesByGameId(Long gameId, Long userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        validateParticipation(game, userId);

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

        validateParticipation(game, userId);

        if (game.getStatus() != GameStatus.WAITING && game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameNotInProgressException("Game is not in progress");
        }

        List<Move> existingMoves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        
        if (game.getStatus() == GameStatus.WAITING) {
            game.setStatus(GameStatus.IN_PROGRESS);
            if (game.getCurrentPlayerSymbol() == null) {
                game.setCurrentPlayerSymbol(request.playerSymbol());
            }
        }
        
        validatePlayerTurn(game, existingMoves, request.playerSymbol(), userId);
        
        gameLogicService.validateMove(game, existingMoves, request.row(), request.col(), request.playerSymbol());

        User player = userRepository.findById(userId)
                .orElseThrow(() -> new com.tbs.exception.UserNotFoundException("User not found"));

        Move savedMove = createAndSaveMove(game, request.row(), request.col(), request.playerSymbol(), player);
        log.info("Created move {} for game {} by user {}", savedMove.getId(), gameId, userId);

        List<Move> allMoves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        BoardState boardState = boardStateService.generateBoardState(game, allMoves);

        GameStateUpdateResult stateUpdate = updateGameStateAfterMove(
                game, boardState, request.playerSymbol(), player, userId
        );

        game.setLastMoveAt(Instant.now());
        Game updatedGame = gameRepository.save(game);

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
                stateUpdate.winner()
        );
    }

    @Transactional
    public BotMoveResponse createBotMove(Long gameId, Long userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        validateParticipation(game, userId);

        if (game.getPlayer1() == null) {
            throw new IllegalStateException("Game must have player1");
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

        PlayerSymbol player1Symbol = determinePlayer1SymbolOrDefault(game, existingMoves);
        PlayerSymbol botSymbol = gameLogicService.getOppositeSymbol(player1Symbol);

        if (currentSymbol != botSymbol) {
            throw new com.tbs.exception.InvalidMoveException("It's not bot's turn");
        }
        
        BotService.BotMovePosition botPosition = botService.generateBotMove(
                boardState, 
                game.getBotDifficulty(), 
                botSymbol, 
                game.getBoardSize().getValue(),
                game
        );

        Move savedMove = createAndSaveMove(game, botPosition.row(), botPosition.col(), botSymbol, null);
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
        return updateGameStateAfterMoveInternal(game, boardState, botSymbol, null);
    }

    private GameStateUpdateResult updateGameStateAfterMoveInternal(
            Game game, BoardState boardState, PlayerSymbol moveSymbol, User winnerUser) {
        boolean isWin = gameLogicService.checkWinCondition(game, boardState, moveSymbol);
        WinnerInfo winner = null;

        if (isWin) {
            game.setStatus(GameStatus.FINISHED);
            game.setWinner(winnerUser);
            game.setFinishedAt(Instant.now());
            if (winnerUser != null) {
                winner = new WinnerInfo(winnerUser.getId(), winnerUser.getUsername());
                log.info("Game {} finished. Winner: user {}", game.getId(), winnerUser.getId());
                return new GameStateUpdateResult(winner);
            }
            winner = new WinnerInfo(BOT_USER_ID, BOT_USERNAME);
            log.info("Game {} finished. Bot won", game.getId());
            return new GameStateUpdateResult(winner);
        }

        if (gameLogicService.checkDrawCondition(game, boardState)) {
            game.setStatus(GameStatus.DRAW);
            game.setFinishedAt(Instant.now());
            log.info("Game {} ended in draw", game.getId());
            return new GameStateUpdateResult(null);
        }

        PlayerSymbol nextPlayerSymbol = gameLogicService.getOppositeSymbol(moveSymbol);
        game.setCurrentPlayerSymbol(nextPlayerSymbol);
        return new GameStateUpdateResult(null);
    }

    private PlayerSymbol determinePlayer1SymbolOrDefault(Game game, List<Move> existingMoves) {
        if (game.getPlayer1() == null) {
            throw new IllegalStateException("Game must have player1");
        }
        
        if (!existingMoves.isEmpty()) {
            for (Move move : existingMoves) {
                if (move.getPlayer() != null && move.getPlayer().getId().equals(game.getPlayer1().getId())) {
                    return move.getPlayerSymbol();
                }
            }
        }
        return PlayerSymbol.X;
    }

    private Move createAndSaveMove(Game game, int row, int col, PlayerSymbol symbol, User player) {
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

    private void validatePlayerTurn(Game game, List<Move> existingMoves, PlayerSymbol playerSymbol, Long userId) {
        PlayerSymbol currentPlayerSymbol = game.getCurrentPlayerSymbol();
        if (game.getStatus() == GameStatus.IN_PROGRESS && currentPlayerSymbol == null) {
            throw new IllegalStateException("Game is IN_PROGRESS but currentPlayerSymbol is null");
        }
        
        if (currentPlayerSymbol == null) {
            return;
        }
        
        PlayerSymbol player1Symbol = determinePlayer1SymbolOrDefault(game, existingMoves);
        boolean isPlayer1Turn = currentPlayerSymbol == player1Symbol;
        boolean isCurrentUserPlayer1 = game.getPlayer1().getId().equals(userId);
        
        if (isPlayer1Turn && !isCurrentUserPlayer1) {
            throw new ForbiddenException("It's not your turn");
        }
        if (!isPlayer1Turn && (game.getPlayer2() == null || !game.getPlayer2().getId().equals(userId))) {
            throw new ForbiddenException("It's not your turn");
        }
    }

    private void validateParticipation(Game game, Long userId) {
        if (game.getPlayer1() == null) {
            throw new IllegalStateException("Game must have player1");
        }
        if (!game.getPlayer1().getId().equals(userId) &&
                (game.getPlayer2() == null || !game.getPlayer2().getId().equals(userId))) {
            throw new ForbiddenException("You are not a participant of this game");
        }
    }

    private static record GameStateUpdateResult(WinnerInfo winner) {}
}

