package com.tbs.service;

import com.tbs.enums.BotDifficulty;
import com.tbs.enums.GameType;
import com.tbs.model.Game;
import com.tbs.model.User;
import com.tbs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PointsService {

    private static final Logger log = LoggerFactory.getLogger(PointsService.class);

    private final long pointsEasyBot;
    private final long pointsMediumBot;
    private final long pointsHardBot;
    private final long pointsPvp;

    private final UserRepository userRepository;
    private final RankingService rankingService;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    public PointsService(
            UserRepository userRepository,
            RankingService rankingService,
            @Value("${app.points.easy-bot:100}") long pointsEasyBot,
            @Value("${app.points.medium-bot:500}") long pointsMediumBot,
            @Value("${app.points.hard-bot:1000}") long pointsHardBot,
            @Value("${app.points.pvp:1000}") long pointsPvp
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.rankingService = Objects.requireNonNull(rankingService, "RankingService cannot be null");
        this.pointsEasyBot = pointsEasyBot;
        this.pointsMediumBot = pointsMediumBot;
        this.pointsHardBot = pointsHardBot;
        this.pointsPvp = pointsPvp;
    }

    @Transactional
    public void awardPointsForWin(Game game, User winner) {
        if (game == null) {
            log.warn("Attempted to award points but game is null");
            return;
        }

        if (winner == null) {
            log.warn("Attempted to award points but winner is null for game {}", game.getId());
            return;
        }

        if (!isPlayerInGame(game, winner)) {
            log.warn("Attempted to award points to user {} who is not a player in game {}", winner.getId(), game.getId());
            return;
        }

        Long winnerId = winner.getId();
        if (winnerId == null) {
            log.error("Winner user ID is null for game {}", game.getId());
            throw new IllegalStateException("Winner user ID cannot be null");
        }

        long pointsToAward = calculatePoints(game);
        if (pointsToAward <= 0) {
            log.warn("No points to award for game {} with type {} and bot difficulty {}", 
                    game.getId(), game.getGameType(), game.getBotDifficulty());
        }

        if (game.getGameType() == GameType.PVP) {
            updatePlayerStatsForPvpWin(game, winnerId, pointsToAward);
        } else if (game.getGameType() == GameType.VS_BOT) {
            updatePlayerStatsForBotWin(game, winnerId, pointsToAward);
        }

        refreshRankingsAsync();
    }

    private void updatePlayerStatsForPvpWin(Game game, Long winnerId, long pointsToAward) {
        Long player1Id = getPlayerId(game.getPlayer1());
        Long player2Id = getPlayerId(game.getPlayer2());

        if (player1Id != null) {
            userRepository.findById(player1Id)
                    .filter(player -> !Boolean.TRUE.equals(player.getIsGuest()))
                    .ifPresent(player1 -> {
                        int currentPlayed = player1.getGamesPlayed() != null ? player1.getGamesPlayed() : 0;
                        player1.setGamesPlayed(currentPlayed + 1);

                        if (player1Id.equals(winnerId)) {
                            long currentPoints = player1.getTotalPoints() != null ? player1.getTotalPoints() : 0L;
                            int currentWins = player1.getGamesWon() != null ? player1.getGamesWon() : 0;
                            player1.setTotalPoints(currentPoints + pointsToAward);
                            player1.setGamesWon(currentWins + 1);
                            log.info("Awarded {} points to user {} for winning PvP game {}", 
                                    pointsToAward, player1Id, game.getId());
                        }

                        userRepository.save(player1);
                    });
        }

        if (player2Id != null) {
            userRepository.findById(player2Id)
                    .filter(player -> !Boolean.TRUE.equals(player.getIsGuest()))
                    .ifPresent(player2 -> {
                        int currentPlayed = player2.getGamesPlayed() != null ? player2.getGamesPlayed() : 0;
                        player2.setGamesPlayed(currentPlayed + 1);

                        if (player2Id.equals(winnerId)) {
                            long currentPoints = player2.getTotalPoints() != null ? player2.getTotalPoints() : 0L;
                            int currentWins = player2.getGamesWon() != null ? player2.getGamesWon() : 0;
                            player2.setTotalPoints(currentPoints + pointsToAward);
                            player2.setGamesWon(currentWins + 1);
                            log.info("Awarded {} points to user {} for winning PvP game {}", 
                                    pointsToAward, player2Id, game.getId());
                        }

                        userRepository.save(player2);
                    });
        }
    }

    private void updatePlayerStatsForBotWin(Game game, Long winnerId, long pointsToAward) {
        Long player1Id = getPlayerId(game.getPlayer1());
        boolean isBotWinner = winnerId != null && isBotUser(winnerId);
        
        if (isBotWinner && player1Id != null) {
            userRepository.findById(player1Id)
                    .filter(player -> !Boolean.TRUE.equals(player.getIsGuest()))
                    .ifPresent(player -> {
                        int currentPlayed = player.getGamesPlayed() != null ? player.getGamesPlayed() : 0;
                        player.setGamesPlayed(currentPlayed + 1);
                        userRepository.save(player);
                        log.info("Recorded game played for user {} who lost to bot in game {}", player1Id, game.getId());
                    });
        } else if (winnerId != null) {
            userRepository.findById(winnerId)
                    .filter(user -> !Boolean.TRUE.equals(user.getIsGuest()))
                    .ifPresentOrElse(
                            user -> {
                                long currentPoints = user.getTotalPoints() != null ? user.getTotalPoints() : 0L;
                                int currentWins = user.getGamesWon() != null ? user.getGamesWon() : 0;
                                int currentPlayed = user.getGamesPlayed() != null ? user.getGamesPlayed() : 0;

                                user.setTotalPoints(currentPoints + pointsToAward);
                                user.setGamesWon(currentWins + 1);
                                user.setGamesPlayed(currentPlayed + 1);

                                userRepository.save(user);

                                log.info("Awarded {} points to user {} for winning game {} (type: {}, difficulty: {})", 
                                        pointsToAward, user.getId(), game.getId(), game.getGameType(), game.getBotDifficulty());
                            },
                            () -> log.debug("Skipping points award for guest user {} in game {}", winnerId, game.getId())
                    );
        }
    }

    private boolean isBotUser(Long userId) {
        if (userId == null) {
            return false;
        }
        return userRepository.findById(userId)
                .map(user -> "Bot".equals(user.getUsername()))
                .orElse(false);
    }

    private boolean isPlayerInGame(Game game, User player) {
        if (game == null || player == null) {
            return false;
        }
        Long playerId = player.getId();
        if (playerId == null) {
            return false;
        }
        if (game.getPlayer1() != null && playerId.equals(game.getPlayer1().getId())) {
            return true;
        }
        if (game.getPlayer2() != null && playerId.equals(game.getPlayer2().getId())) {
            return true;
        }
        return false;
    }

    @Transactional
    public void recordGamePlayed(Game game, User player) {
        if (player == null) {
            log.warn("Attempted to record game played but player is null for game {}", game.getId());
            return;
        }

        if (Boolean.TRUE.equals(player.getIsGuest())) {
            log.debug("Skipping game played record for guest user {} in game {}", player.getId(), game.getId());
            return;
        }

        Long playerId = player.getId();
        if (playerId == null) {
            log.error("Player user ID is null for game {}", game.getId());
            throw new IllegalStateException("Player user ID cannot be null");
        }
        User user = userRepository.findById(playerId)
                .orElseThrow(() -> new IllegalStateException("Player user not found: " + playerId));

        int currentPlayed = user.getGamesPlayed() != null ? user.getGamesPlayed() : 0;
        user.setGamesPlayed(currentPlayed + 1);

        userRepository.save(user);

        log.debug("Recorded game played for user {} in game {}", user.getId(), game.getId());
    }

    @Transactional
    public void awardPointsForDraw(Game game) {
        if (game == null) {
            log.warn("Attempted to award draw points but game is null");
            return;
        }

        long drawPoints = calculateDrawPoints(game);

        if (game.getGameType() == GameType.PVP) {
            Long player1Id = getPlayerId(game.getPlayer1());
            if (player1Id != null) {
                userRepository.findById(player1Id)
                        .filter(player -> !Boolean.TRUE.equals(player.getIsGuest()))
                        .ifPresent(player -> awardDrawPointsToUser(player1Id, drawPoints));
            }
            Long player2Id = getPlayerId(game.getPlayer2());
            if (player2Id != null) {
                userRepository.findById(player2Id)
                        .filter(player -> !Boolean.TRUE.equals(player.getIsGuest()))
                        .ifPresent(player -> awardDrawPointsToUser(player2Id, drawPoints));
            }
        } else if (game.getGameType() == GameType.VS_BOT) {
            Long player1Id = getPlayerId(game.getPlayer1());
            if (player1Id != null) {
                userRepository.findById(player1Id)
                        .filter(player -> !Boolean.TRUE.equals(player.getIsGuest()))
                        .ifPresent(player -> awardDrawPointsToUser(player1Id, drawPoints));
            }
        }

        log.info("Awarded {} points for draw in game {} (type: {})", 
                drawPoints, game.getId(), game.getGameType());
    }
    
    private long calculateDrawPoints(Game game) {
        long pointsAtStake = calculatePoints(game);
        return Math.round(pointsAtStake / 10.0);
    }

    private Long getPlayerId(User player) {
        if (player == null) {
            return null;
        }
        try {
            Long id = player.getId();
            if (id == null) {
                log.warn("Player ID is null for player object");
                return null;
            }
            return id;
        } catch (org.hibernate.LazyInitializationException e) {
            log.warn("Lazy initialization exception when getting player ID: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Error getting player ID: {}", e.getMessage());
            return null;
        }
    }

    private void awardDrawPointsToUser(Long userId, long drawPoints) {
        if (userId == null) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        long currentPoints = user.getTotalPoints() != null ? user.getTotalPoints() : 0L;
        int currentPlayed = user.getGamesPlayed() != null ? user.getGamesPlayed() : 0;

        user.setTotalPoints(currentPoints + drawPoints);
        user.setGamesPlayed(currentPlayed + 1);

        userRepository.save(user);

        log.info("Awarded {} draw points to user {}", drawPoints, userId);

        refreshRankingsAsync();
    }

    private long calculatePoints(Game game) {
        long basePoints = 0L;
        
        if (game.getGameType() == GameType.PVP) {
            basePoints = pointsPvp;
        } else if (game.getGameType() == GameType.VS_BOT) {
            BotDifficulty difficulty = game.getBotDifficulty();
            if (difficulty == null) {
                log.warn("Game {} is VS_BOT but botDifficulty is null", game.getId());
                return 0L;
            }
            basePoints = switch (difficulty) {
                case EASY -> pointsEasyBot;
                case MEDIUM -> pointsMediumBot;
                case HARD -> pointsHardBot;
            };
        } else {
            return 0L;
        }
        
        double multiplier = getBoardSizeMultiplier(game.getBoardSize());
        return Math.round(basePoints * multiplier);
    }
    
    private double getBoardSizeMultiplier(com.tbs.enums.BoardSize boardSize) {
        if (boardSize == null) {
            return 1.0;
        }
        return switch (boardSize.getValue()) {
            case 3 -> 1.0;
            case 4 -> 1.5;
            case 5 -> 2.0;
            default -> 1.0;
        };
    }

    @Async("rankingRefreshExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshRankingsAsync() {
        if (!isRefreshing.compareAndSet(false, true)) {
            log.debug("Ranking refresh already in progress, skipping");
            return;
        }

        try {
            rankingService.refreshPlayerRankings();
            log.debug("Refreshed player_rankings materialized view asynchronously");
        } catch (Exception e) {
            log.error("Error refreshing player_rankings materialized view asynchronously", e);
        } finally {
            isRefreshing.set(false);
        }
    }
}
