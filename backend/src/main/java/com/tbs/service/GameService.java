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
import com.tbs.exception.ForbiddenException;
import com.tbs.exception.GameNotFoundException;
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
import java.util.stream.Collectors;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final UserRepository userRepository;
    private final BoardStateService boardStateService;

    public GameService(GameRepository gameRepository, MoveRepository moveRepository,
                       UserRepository userRepository, BoardStateService boardStateService) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.userRepository = userRepository;
        this.boardStateService = boardStateService;
    }

    @Transactional
    public CreateGameResponse createGame(CreateGameRequest request, Long userId) {
        validateGameRequest(request);

        User player1 = userRepository.findById(userId)
                .orElseThrow(() -> new com.tbs.exception.UserNotFoundException("User not found"));

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

    public GameDetailResponse getGameDetail(Long gameId, Long userId) {
        Game game = gameRepository.findByIdWithPlayers(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        validateParticipation(game, userId);

        List<Move> moves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        BoardState boardState = boardStateService.generateBoardState(game, moves);

        List<MoveListItem> moveListItems = moves.stream()
                .map(move -> mapToMoveListItem(move))
                .collect(Collectors.toList());

        return mapToGameDetailResponse(game, boardState, moves.size(), moveListItems);
    }

    public BoardStateResponse getBoardState(Long gameId, Long userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        validateParticipation(game, userId);

        List<Move> moves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
        BoardState boardState = boardStateService.generateBoardState(game, moves);

        Move lastMove = moveRepository.findFirstByGameIdOrderByMoveOrderDesc(gameId).orElse(null);
        BoardStateResponse.LastMove lastMoveDto = lastMove != null
                ? new BoardStateResponse.LastMove(lastMove.getRow(), lastMove.getCol(),
                lastMove.getPlayerSymbol(), lastMove.getMoveOrder())
                : null;

        return new BoardStateResponse(boardState, game.getBoardSize(), moves.size(), lastMoveDto);
    }

    public GameListResponse getGames(Long userId, GameStatus status, GameType gameType, Pageable pageable) {
        Page<Game> games = gameRepository.findByUserIdAndFilters(userId, status, gameType, pageable);

        List<GameListItem> items = games.getContent().stream()
                .map(g -> {
                    long totalMoves = moveRepository.countByGameId(g.getId());
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

        validateParticipation(game, userId);
        
        GameStatus oldStatus = game.getStatus();
        validateStatusTransition(oldStatus, newStatus);
        
        game.setStatus(newStatus);

        if (newStatus == GameStatus.FINISHED || newStatus == GameStatus.ABANDONED) {
            game.setFinishedAt(Instant.now());
        }

        if (newStatus == GameStatus.FINISHED) {
            User winner = game.getPlayer1().getId().equals(userId)
                    ? game.getPlayer2()
                    : game.getPlayer1();
            if (winner != null) {
                game.setWinner(winner);
            }
        }

        Game updatedGame = gameRepository.save(game);

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

    private void validateParticipation(Game game, Long userId) {
        if (!game.getPlayer1().getId().equals(userId) &&
                (game.getPlayer2() == null || !game.getPlayer2().getId().equals(userId))) {
            throw new ForbiddenException("You are not a participant of this game");
        }
    }

    private void validateStatusTransition(GameStatus currentStatus, GameStatus newStatus) {
        if (currentStatus == GameStatus.FINISHED || currentStatus == GameStatus.ABANDONED) {
            throw new BadRequestException("Cannot change status from " + currentStatus + " to " + newStatus);
        }

        if (currentStatus == GameStatus.WAITING && newStatus == GameStatus.FINISHED) {
            throw new BadRequestException("Cannot finish a game that has not started");
        }

        if (currentStatus == GameStatus.IN_PROGRESS) {
            if (newStatus != GameStatus.FINISHED && newStatus != GameStatus.ABANDONED) {
                throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
            }
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

    private MoveListItem mapToMoveListItem(Move move) {
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

