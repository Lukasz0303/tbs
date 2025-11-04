package com.tbs.service;

import com.tbs.dto.common.BoardState;
import com.tbs.enums.GameStatus;
import com.tbs.enums.PlayerSymbol;
import com.tbs.exception.BadRequestException;
import com.tbs.exception.GameNotFoundException;
import com.tbs.exception.UserNotFoundException;
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

@Service
public class WebSocketGameService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketGameService.class);

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final UserRepository userRepository;
    private final BoardStateService boardStateService;
    private final GameLogicService gameLogicService;
    private final TurnValidationService turnValidationService;
    private final MoveCreationService moveCreationService;

    public WebSocketGameService(
            GameRepository gameRepository,
            MoveRepository moveRepository,
            UserRepository userRepository,
            BoardStateService boardStateService,
            GameLogicService gameLogicService,
            TurnValidationService turnValidationService,
            MoveCreationService moveCreationService
    ) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.userRepository = userRepository;
        this.boardStateService = boardStateService;
        this.gameLogicService = gameLogicService;
        this.turnValidationService = turnValidationService;
        this.moveCreationService = moveCreationService;
    }

    @Transactional
    public MoveResult processMove(Long gameId, Long userId, int row, int col, PlayerSymbol playerSymbol) {
        Game game = gameRepository.findByIdWithPlayers(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        List<Move> existingMoves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        
        gameLogicService.validateMove(game, existingMoves, row, col, playerSymbol);
        turnValidationService.validatePlayerTurn(game, existingMoves, playerSymbol, userId);

        User player = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Move savedMove = moveCreationService.createAndSaveMove(game, row, col, playerSymbol, player);

        List<Move> allMoves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        BoardState boardState = boardStateService.generateBoardState(game, allMoves);

        processMoveResult(game, boardState, playerSymbol, player);

        game.setLastMoveAt(Instant.now());
        Game updatedGame = gameRepository.save(game);

        return new MoveResult(savedMove, updatedGame, boardState, allMoves.size());
    }

    @Transactional
    public SurrenderResult processSurrender(Long gameId, Long userId) {
        Game game = gameRepository.findByIdWithPlayers(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        if (game.getPlayer1() == null) {
            throw new BadRequestException("Game must have player1");
        }

        User winner = game.getPlayer1().getId().equals(userId) 
                ? game.getPlayer2() 
                : game.getPlayer1();

        if (winner == null) {
            throw new BadRequestException("Cannot surrender: opponent not found");
        }

        game.setStatus(GameStatus.FINISHED);
        game.setWinner(winner);
        game.setFinishedAt(Instant.now());
        gameRepository.save(game);

        List<Move> moves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        BoardState boardState = boardStateService.generateBoardState(game, moves);

        return new SurrenderResult(game, winner, boardState, moves.size());
    }

    private void processMoveResult(Game game, BoardState boardState, PlayerSymbol moveSymbol, User player) {
        boolean isWin = gameLogicService.checkWinCondition(game, boardState, moveSymbol);

        if (isWin) {
            game.setStatus(GameStatus.FINISHED);
            game.setWinner(player);
            game.setFinishedAt(Instant.now());
            log.info("Game {} finished. Winner: user {}", game.getId(), player.getId());
            return;
        }

        if (gameLogicService.checkDrawCondition(game, boardState)) {
            game.setStatus(GameStatus.DRAW);
            game.setFinishedAt(Instant.now());
            log.info("Game {} ended in draw", game.getId());
            return;
        }

        PlayerSymbol nextPlayerSymbol = gameLogicService.getOppositeSymbol(moveSymbol);
        game.setCurrentPlayerSymbol(nextPlayerSymbol);
    }

    public record MoveResult(Move move, Game game, BoardState boardState, int totalMoves) {}
    public record SurrenderResult(Game game, User winner, BoardState boardState, int totalMoves) {}
}

