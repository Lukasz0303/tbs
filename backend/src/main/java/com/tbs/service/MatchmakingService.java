package com.tbs.service;

import com.tbs.dto.matchmaking.*;
import com.tbs.enums.BoardSize;
import com.tbs.enums.GameStatus;
import com.tbs.enums.GameType;
import com.tbs.enums.PlayerSymbol;
import com.tbs.enums.QueuePlayerStatus;
import com.tbs.exception.*;
import com.tbs.model.Game;
import com.tbs.model.User;
import com.tbs.repository.GameRepository;
import com.tbs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);
    private static final int DEFAULT_ESTIMATED_WAIT_TIME = 30;

    private final RedisService redisService;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public MatchmakingService(RedisService redisService,
                             GameRepository gameRepository, UserRepository userRepository) {
        this.redisService = redisService;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MatchmakingQueueResponse addToQueue(Long userId, MatchmakingQueueRequest request) {
        log.debug("Adding user {} to matchmaking queue for board size {}", userId, request.boardSize());

        String userLockKey = userId.toString();
        if (!redisService.acquireLock(userLockKey, 5)) {
            throw new UserAlreadyInQueueException("User is already in the matchmaking queue or operation in progress");
        }

        try {
            boolean added = redisService.addToQueueIfNotActive(userId, request.boardSize());
            if (!added) {
                if (gameRepository.hasActivePvpGame(userId)) {
                    throw new UserHasActiveGameException("User already has an active PvP game");
                }
                throw new UserAlreadyInQueueException("User is already in the matchmaking queue");
            }

            int estimatedWaitTime = calculateEstimatedWaitTime(request.boardSize());

            String matchmakingLockKey = "matchmaking:" + request.boardSize().name();
            if (redisService.acquireLock(matchmakingLockKey, 10)) {
                try {
                    List<Long> potentialMatches = redisService.getQueueForBoardSize(request.boardSize())
                            .stream()
                            .filter(id -> !id.equals(userId))
                            .filter(id -> !gameRepository.hasActivePvpGame(id))
                            .toList();

                    if (!potentialMatches.isEmpty()) {
                        Long matchedUserId = potentialMatches.get(ThreadLocalRandom.current().nextInt(potentialMatches.size()));
                        Game createdGame = createPvpGame(userId, matchedUserId, request.boardSize());
                        if (createdGame.getId() == null) {
                            log.error("Failed to create game: gameId is null after save. userId1={}, userId2={}", 
                                      userId, matchedUserId);
                            throw new IllegalStateException("Failed to create game: gameId is null");
                        }
                        log.info("Match found! Created game {} for users {} and {}", createdGame.getId(), userId, matchedUserId);
                        return new MatchmakingQueueResponse("Match found! Game created", 0);
                    }
                } finally {
                    redisService.releaseLock(matchmakingLockKey);
                }
            }

            log.info("User {} added to matchmaking queue for board size {}", userId, request.boardSize());
            return new MatchmakingQueueResponse("Successfully added to queue", estimatedWaitTime);
        } finally {
            redisService.releaseLock(userLockKey);
        }
    }

    @Transactional
    public LeaveQueueResponse removeFromQueue(Long userId) {
        log.debug("Removing user {} from matchmaking queue", userId);

        boolean removed = redisService.removeFromQueue(userId);

        if (!removed) {
            throw new UserNotInQueueException("User is not in the matchmaking queue");
        }

        log.info("User {} removed from matchmaking queue", userId);
        return new LeaveQueueResponse("Successfully removed from queue");
    }

    @Transactional
    public ChallengeResponse createDirectChallenge(Long challengerId, Long challengedId, ChallengeRequest request) {
        log.debug("Creating direct challenge: challenger {} challenges {} for board size {}",
                challengerId, challengedId, request.boardSize());

        if (challengerId.equals(challengedId)) {
            throw new CannotChallengeSelfException("Users cannot challenge themselves");
        }

        if (!isUserAvailable(challengerId)) {
            throw new UserHasActiveGameException("Challenger already has an active PvP game");
        }

        User challengedUser = userRepository.findById(challengedId)
                .orElseThrow(() -> new UserNotFoundException("Challenged user does not exist"));

        if (!isUserAvailable(challengedId)) {
            throw new UserUnavailableException("Challenged user is currently unavailable");
        }

        User challenger = userRepository.findById(challengerId)
                .orElseThrow(() -> new UserNotFoundException("Challenger not found"));

        redisService.removeFromQueue(challengerId);
        redisService.removeFromQueue(challengedId);

        Game game = new Game();
        game.setGameType(GameType.PVP);
        game.setBoardSize(request.boardSize());
        game.setPlayer1(challenger);
        game.setPlayer2(challengedUser);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentPlayerSymbol(PlayerSymbol.X);

        Game savedGame = gameRepository.save(game);

        log.info("Direct challenge created: game {} for challenger {} and challenged {}", 
                savedGame.getId(), challengerId, challengedId);

        Long gameId = savedGame.getId();
        
        if (gameId == null) {
            log.error("Failed to create challenge game: gameId is null");
            throw new BadRequestException("Failed to create challenge game");
        }
        
        return new ChallengeResponse(
                gameId,
                savedGame.getGameType(),
                savedGame.getBoardSize(),
                challengerId,
                challengedId,
                savedGame.getStatus(),
                savedGame.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public boolean isUserAvailable(Long userId) {
        return !gameRepository.hasActivePvpGame(userId);
    }

    private int calculateEstimatedWaitTime(BoardSize boardSize) {
        int queueSize = redisService.getQueueSize(boardSize);

        if (queueSize > 0) {
            return Math.max(5, queueSize * 5);
        }

        return DEFAULT_ESTIMATED_WAIT_TIME;
    }

    private Game createPvpGame(Long player1Id, Long player2Id, BoardSize boardSize) {
        if (player1Id.equals(player2Id)) {
            throw new IllegalArgumentException("Player 1 and Player 2 cannot be the same");
        }

        if (gameRepository.hasActivePvpGame(player1Id)) {
            throw new UserHasActiveGameException("Player 1 already has an active game");
        }
        if (gameRepository.hasActivePvpGame(player2Id)) {
            throw new UserHasActiveGameException("Player 2 already has an active game");
        }

        User player1 = userRepository.findById(player1Id)
                .orElseThrow(() -> new UserNotFoundException("Player 1 not found"));
        User player2 = userRepository.findById(player2Id)
                .orElseThrow(() -> new UserNotFoundException("Player 2 not found"));

        redisService.removeFromQueue(player1Id);
        redisService.removeFromQueue(player2Id);

        Game game = new Game();
        game.setGameType(GameType.PVP);
        game.setBoardSize(boardSize);
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentPlayerSymbol(PlayerSymbol.X);

        return gameRepository.save(game);
    }

    @Transactional(readOnly = true)
    public QueueStatusResponse getQueueStatus(BoardSize boardSize) {
        log.debug("Retrieving queue status for board size: {}", boardSize);

        List<RedisService.QueueEntry> queueEntries = boardSize == null
                ? redisService.getAllQueueEntries()
                : redisService.getQueueEntriesForBoardSize(boardSize);

        if (queueEntries.isEmpty()) {
            return new QueueStatusResponse(List.of(), 0);
        }

        List<Long> userIds = queueEntries.stream()
                .map(RedisService.QueueEntry::userId)
                .toList();

        Map<Long, User> usersMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<Game> activeGames = gameRepository.findActivePvpGamesForUsers(userIds);
        Map<Long, Game> userGameMap = new HashMap<>();
        for (Game game : activeGames) {
            if (game.getPlayer1() != null && game.getPlayer2() != null) {
                userGameMap.put(game.getPlayer1().getId(), game);
                userGameMap.put(game.getPlayer2().getId(), game);
            }
        }

        Map<Long, String> usernameMap = usersMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            String username = entry.getValue().getUsername();
                            return username != null ? username : "Unknown";
                        }
                ));

        List<PlayerQueueStatus> playerStatuses = queueEntries.stream()
                .map(entry -> determinePlayerStatus(entry, usersMap, userGameMap, usernameMap))
                .filter(Objects::nonNull)
                .toList();

        return new QueueStatusResponse(playerStatuses, playerStatuses.size());
    }

    private PlayerQueueStatus determinePlayerStatus(
            RedisService.QueueEntry entry,
            Map<Long, User> usersMap,
            Map<Long, Game> userGameMap,
            Map<Long, String> usernameMap
    ) {
        Long userId = entry.userId();
        User user = usersMap.get(userId);

        if (user == null) {
            log.warn("User {} found in queue but not found in database", userId);
            return null;
        }

        Game activeGame = userGameMap.get(userId);

        if (activeGame != null) {
            Long matchedUserId = getMatchedUserId(userId, activeGame);
            String matchedUsername = matchedUserId != null ? usernameMap.get(matchedUserId) : null;

            QueuePlayerStatus status = activeGame.getStatus() == GameStatus.IN_PROGRESS
                    ? QueuePlayerStatus.PLAYING
                    : QueuePlayerStatus.MATCHED;

            return new PlayerQueueStatus(
                    userId,
                    user.getUsername(),
                    entry.boardSize(),
                    status,
                    entry.joinedAt(),
                    matchedUserId,
                    matchedUsername,
                    activeGame.getId(),
                    true
            );
        }

        return new PlayerQueueStatus(
                userId,
                user.getUsername(),
                entry.boardSize(),
                QueuePlayerStatus.WAITING,
                entry.joinedAt(),
                null,
                null,
                null,
                false
        );
    }

    private Long getMatchedUserId(Long userId, Game game) {
        if (game.getPlayer1() != null && game.getPlayer2() != null) {
            Long player1Id = game.getPlayer1().getId();
            Long player2Id = game.getPlayer2().getId();

            if (userId.equals(player1Id)) {
                return player2Id;
            } else if (userId.equals(player2Id)) {
                return player1Id;
            }
        }
        return null;
    }
}

