package com.tbs.service;

import com.tbs.dto.common.BoardState;
import com.tbs.dto.game.*;
import com.tbs.dto.move.MoveListItem;
import com.tbs.dto.user.PlayerInfo;
import com.tbs.dto.user.WinnerInfo;
import com.tbs.enums.BoardSize;
import com.tbs.enums.GameStatus;
import com.tbs.enums.GameType;
import com.tbs.exception.BadRequestException;
import com.tbs.exception.GameNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final UserRepository userRepository;
    private final BoardStateService boardStateService;
    private final GameValidationService gameValidationService;
    private final PointsService pointsService;

    public GameService(GameRepository gameRepository, MoveRepository moveRepository,
                       UserRepository userRepository, BoardStateService boardStateService,
                       GameValidationService gameValidationService, PointsService pointsService) {
        this.gameRepository = Objects.requireNonNull(gameRepository, "GameRepository cannot be null");
        this.moveRepository = Objects.requireNonNull(moveRepository, "MoveRepository cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.boardStateService = Objects.requireNonNull(boardStateService, "BoardStateService cannot be null");
        this.gameValidationService = Objects.requireNonNull(gameValidationService, "GameValidationService cannot be null");
        this.pointsService = Objects.requireNonNull(pointsService, "PointsService cannot be null");
    }

    @Transactional
    public CreateGameResponse createGame(CreateGameRequest request, Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        log.debug("Creating game: type={}, boardSize={}, userId={}", request.gameType(), request.boardSize(), userId);
        validateGameRequest(request);

        User player1 = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Game game = new Game();
        game.setGameType(request.gameType());
        game.setBoardSize(BoardSize.fromValue(request.boardSize()));
        game.setPlayer1(player1);
        game.setPlayer2(null);
        game.setBotDifficulty(request.botDifficulty());
        game.setStatus(GameStatus.WAITING);
        game.setCurrentPlayerSymbol(null);

        Game savedGame = gameRepository.save(game);

        BoardState boardState = boardStateService.generateBoardState(savedGame, List.of());

        return mapToCreateGameResponse(savedGame, boardState);
    }

    @Transactional(readOnly = true)
    public GameDetailResponse getGameDetail(Long gameId, Long userId) {
        if (gameId == null) {
            throw new BadRequestException("Game ID cannot be null");
        }
        log.debug("Retrieving game detail: gameId={}, userId={}", gameId, userId);
        Game game = gameRepository.findByIdWithPlayers(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        gameValidationService.validateParticipation(game, userId);

        List<Move> moves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        BoardState boardState = boardStateService.generateBoardState(game, moves);

        List<MoveListItem> moveListItems = moves.stream()
                .map(MoveMapper::toMoveListItem)
                .collect(Collectors.toList());

        return mapToGameDetailResponse(game, boardState, moves.size(), moveListItems);
    }

    @Transactional(readOnly = true)
    public BoardStateResponse getBoardState(Long gameId, Long userId) {
        log.debug("Retrieving board state: gameId={}, userId={}", gameId, userId);
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        gameValidationService.validateParticipation(game, userId);

        List<Move> moves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        BoardState boardState = boardStateService.generateBoardState(game, moves);

        BoardStateResponse.LastMove lastMoveDto = moveRepository
                .findFirstByGameIdOrderByMoveOrderDesc(gameId)
                .map(move -> new BoardStateResponse.LastMove(
                        move.getRow(),
                        move.getCol(),
                        move.getPlayerSymbol(),
                        move.getMoveOrder()))
                .orElse(null);

        return new BoardStateResponse(boardState, game.getBoardSize(), moves.size(), lastMoveDto);
    }

    @Transactional(readOnly = true)
    public GameListResponse getGames(Long userId, GameStatus status, GameType gameType, Pageable pageable) {
        log.debug("Retrieving games list: userId={}, status={}, gameType={}, page={}", userId, status, gameType, pageable.isPaged() ? pageable.getPageNumber() : "unpaged");
        Page<Game> games = gameRepository.findByUserIdAndFilters(userId, status, gameType, pageable);

        List<Long> gameIds = games.getContent().stream()
                .map(Game::getId)
                .collect(Collectors.toList());

        Map<Long, Long> moveCountsMap = gameIds.isEmpty()
                ? Map.of()
                : moveRepository.getMoveCountsByGameIds(gameIds);

        List<GameListItem> items = games.getContent().stream()
                .map(g -> {
                    long totalMoves = moveCountsMap.getOrDefault(g.getId(), 0L);
                    return mapToGameListItem(g, totalMoves);
                })
                .collect(Collectors.toList());

        return new GameListResponse(
                items,
                games.getTotalElements(),
                games.getTotalPages(),
                games.getSize(),
                games.getNumber(),
                games.isFirst(),
                games.isLast()
        );
    }

    @Transactional
    public UpdateGameStatusResponse updateGameStatus(Long gameId, GameStatus newStatus, Long userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        gameValidationService.validateParticipation(game, userId);
        
        GameStatus oldStatus = game.getStatus();
        validateStatusTransition(oldStatus, newStatus);
        
        game.setStatus(newStatus);

        if (newStatus == GameStatus.FINISHED || newStatus == GameStatus.ABANDONED) {
            game.setFinishedAt(Instant.now());
        }

        User winner = null;
        if (newStatus == GameStatus.FINISHED) {
            winner = determineWinnerOnSurrender(game, userId, gameId);
        }

        Game updatedGame = gameRepository.save(game);
        
        if (newStatus == GameStatus.FINISHED && winner != null) {
            pointsService.awardPointsForWin(updatedGame, winner);
        }

        log.info("Game {} status updated from {} to {} by user {}", gameId, oldStatus, newStatus, userId);

        return new UpdateGameStatusResponse(
                updatedGame.getId(),
                updatedGame.getStatus(),
                updatedGame.getUpdatedAt()
        );
    }

    private void validateGameRequest(CreateGameRequest request) {
        if (request.gameType() == GameType.VS_BOT && request.botDifficulty() == null) {
            throw new BadRequestException("botDifficulty is required for vs_bot games");
        }
        if (request.gameType() == GameType.PVP && request.botDifficulty() != null) {
            throw new BadRequestException("botDifficulty must be null for pvp games");
        }
    }


    private void validateStatusTransition(GameStatus currentStatus, GameStatus newStatus) {
        if (currentStatus == GameStatus.FINISHED || currentStatus == GameStatus.ABANDONED) {
            throw new BadRequestException("Cannot change status from " + currentStatus + " to " + newStatus);
        }

        if (currentStatus == GameStatus.WAITING && newStatus == GameStatus.FINISHED) {
            throw new BadRequestException("Cannot finish a game that has not started");
        }

         if (currentStatus == GameStatus.WAITING && newStatus == GameStatus.IN_PROGRESS) {
            throw new BadRequestException("Use POST /api/games/{gameId}/moves to start the game");
        }

        if (newStatus == GameStatus.FINISHED && currentStatus != GameStatus.IN_PROGRESS) {
            throw new BadRequestException("Can only finish game that is in progress");
        }

        if (currentStatus == GameStatus.IN_PROGRESS 
                && newStatus != GameStatus.FINISHED 
                && newStatus != GameStatus.ABANDONED) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }

    private CreateGameResponse mapToCreateGameResponse(Game game, BoardState boardState) {
        return new CreateGameResponse(
                game.getId(),
                game.getGameType(),
                game.getBoardSize(),
                game.getPlayer1().getId(),
                game.getPlayer2() != null ? game.getPlayer2().getId() : null,
                game.getBotDifficulty(),
                game.getStatus(),
                game.getCurrentPlayerSymbol(),
                game.getCreatedAt(),
                boardState
        );
    }

    private GameDetailResponse mapToGameDetailResponse(Game game, BoardState boardState, int totalMoves,
                                                       List<MoveListItem> moves) {
        return new GameDetailResponse(
                game.getId(),
                game.getGameType(),
                game.getBoardSize(),
                mapToPlayerInfo(game.getPlayer1()),
                game.getPlayer2() != null ? mapToPlayerInfo(game.getPlayer2()) : null,
                game.getWinner() != null ? mapToWinnerInfo(game.getWinner()) : null,
                game.getBotDifficulty(),
                game.getStatus(),
                game.getCurrentPlayerSymbol(),
                game.getLastMoveAt(),
                game.getCreatedAt(),
                game.getUpdatedAt(),
                game.getFinishedAt(),
                boardState,
                totalMoves,
                moves
        );
    }

    private GameListItem mapToGameListItem(Game game, long totalMoves) {
        return new GameListItem(
                game.getId(),
                game.getGameType(),
                game.getBoardSize(),
                game.getStatus(),
                game.getPlayer1().getUsername(),
                game.getPlayer2() != null ? game.getPlayer2().getUsername() : null,
                game.getWinner() != null ? game.getWinner().getUsername() : null,
                game.getBotDifficulty(),
                (int) totalMoves,
                game.getCreatedAt(),
                game.getLastMoveAt(),
                game.getFinishedAt()
        );
    }

    private PlayerInfo mapToPlayerInfo(User user) {
        return new PlayerInfo(
                user.getId(),
                user.getUsername(),
                user.getIsGuest()
        );
    }

    private WinnerInfo mapToWinnerInfo(User user) {
        return new WinnerInfo(
                user.getId(),
                user.getUsername()
        );
    }

    private User determineWinnerOnSurrender(Game game, Long userId, Long gameId) {
        if (game.getGameType() == GameType.PVP) {
            User winner = determinePvpWinner(game, userId);
            if (winner != null) {
                game.setWinner(winner);
                log.info("Game {} finished by surrender. Winner: user {}", gameId, winner.getId());
            }
            return winner;
        } else if (game.getGameType() == GameType.VS_BOT) {
            game.setWinner(null);
            log.info("Game {} finished by surrender - bot wins", gameId);
            return null;
        }
        return null;
    }

    private User determinePvpWinner(Game game, Long userId) {
        if (game.getPlayer1() == null || game.getPlayer2() == null) {
            return null;
        }
        
        return game.getPlayer1().getId().equals(userId)
                ? game.getPlayer2()
                : game.getPlayer1();
    }
}

