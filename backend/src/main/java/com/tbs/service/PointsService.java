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
    private final long pointsDraw;

    private final UserRepository userRepository;
    private final RankingService rankingService;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    public PointsService(
            UserRepository userRepository,
            RankingService rankingService,
            @Value("${app.points.easy-bot:100}") long pointsEasyBot,
            @Value("${app.points.medium-bot:500}") long pointsMediumBot,
            @Value("${app.points.hard-bot:1000}") long pointsHardBot,
            @Value("${app.points.pvp:1000}") long pointsPvp,
            @Value("${app.points.draw:100}") long pointsDraw
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.rankingService = Objects.requireNonNull(rankingService, "RankingService cannot be null");
        this.pointsEasyBot = pointsEasyBot;
        this.pointsMediumBot = pointsMediumBot;
        this.pointsHardBot = pointsHardBot;
        this.pointsPvp = pointsPvp;
        this.pointsDraw = pointsDraw;
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

        if (Boolean.TRUE.equals(winner.getIsGuest())) {
            log.debug("Skipping points award for guest user {} in game {}", winnerId, game.getId());
            return;
        }

        long pointsToAward = calculatePoints(game);
        if (pointsToAward <= 0) {
            log.warn("No points to award for game {} with type {} and bot difficulty {}", 
                    game.getId(), game.getGameType(), game.getBotDifficulty());
            return;
        }

        User user = userRepository.findById(winnerId)
                .orElseThrow(() -> new IllegalStateException("Winner user not found: " + winnerId));

        long currentPoints = user.getTotalPoints() != null ? user.getTotalPoints() : 0L;
        int currentWins = user.getGamesWon() != null ? user.getGamesWon() : 0;
        int currentPlayed = user.getGamesPlayed() != null ? user.getGamesPlayed() : 0;

        user.setTotalPoints(currentPoints + pointsToAward);
        user.setGamesWon(currentWins + 1);
        user.setGamesPlayed(currentPlayed + 1);

        userRepository.save(user);

        log.info("Awarded {} points to user {} for winning game {} (type: {}, difficulty: {})", 
                pointsToAward, user.getId(), game.getId(), game.getGameType(), game.getBotDifficulty());

        refreshRankingsAsync();
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

        if (game.getGameType() == GameType.PVP) {
            Long player1Id = getPlayerId(game.getPlayer1());
            if (player1Id != null) {
                userRepository.findById(player1Id)
                        .filter(player -> !Boolean.TRUE.equals(player.getIsGuest()))
                        .ifPresent(player -> awardDrawPointsToUser(player1Id));
            }
            Long player2Id = getPlayerId(game.getPlayer2());
            if (player2Id != null) {
                userRepository.findById(player2Id)
                        .filter(player -> !Boolean.TRUE.equals(player.getIsGuest()))
                        .ifPresent(player -> awardDrawPointsToUser(player2Id));
            }
        } else if (game.getGameType() == GameType.VS_BOT) {
            Long player1Id = getPlayerId(game.getPlayer1());
            if (player1Id != null) {
                userRepository.findById(player1Id)
                        .filter(player -> !Boolean.TRUE.equals(player.getIsGuest()))
                        .ifPresent(player -> awardDrawPointsToUser(player1Id));
            }
        }

        log.info("Awarded {} points for draw in game {} (type: {})", 
                pointsDraw, game.getId(), game.getGameType());
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

    private void awardDrawPointsToUser(Long userId) {
        if (userId == null) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        long currentPoints = user.getTotalPoints() != null ? user.getTotalPoints() : 0L;
        int currentPlayed = user.getGamesPlayed() != null ? user.getGamesPlayed() : 0;

        user.setTotalPoints(currentPoints + pointsDraw);
        user.setGamesPlayed(currentPlayed + 1);

        userRepository.save(user);

        log.info("Awarded {} draw points to user {}", pointsDraw, userId);

        refreshRankingsAsync();
    }

    private long calculatePoints(Game game) {
        if (game.getGameType() == GameType.PVP) {
            return pointsPvp;
        } else if (game.getGameType() == GameType.VS_BOT) {
            BotDifficulty difficulty = game.getBotDifficulty();
            if (difficulty == null) {
                log.warn("Game {} is VS_BOT but botDifficulty is null", game.getId());
                return 0L;
            }
            return switch (difficulty) {
                case EASY -> pointsEasyBot;
                case MEDIUM -> pointsMediumBot;
                case HARD -> pointsHardBot;
            };
        }
        return 0L;
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
