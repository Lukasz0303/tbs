package com.tbs.repository;

import com.tbs.model.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    @Query("SELECT g FROM Game g " +
           "LEFT JOIN FETCH g.player1 p1 " +
           "LEFT JOIN FETCH g.player2 p2 " +
           "LEFT JOIN FETCH g.winner w " +
           "WHERE g.id = :gameId")
    Optional<Game> findByIdWithPlayers(@Param("gameId") Long gameId);

    @Query("SELECT g FROM Game g " +
           "LEFT JOIN FETCH g.player1 p1 " +
           "LEFT JOIN FETCH g.player2 p2 " +
           "LEFT JOIN FETCH g.winner w " +
           "WHERE (g.player1.id = :userId OR g.player2.id = :userId) " +
           "AND (:status IS NULL OR g.status = :status) " +
           "AND (:gameType IS NULL OR g.gameType = :gameType)")
    Page<Game> findByUserIdAndFilters(
        @Param("userId") Long userId,
        @Param("status") com.tbs.enums.GameStatus status,
        @Param("gameType") com.tbs.enums.GameType gameType,
        Pageable pageable
    );

    @Query(value = "SELECT COUNT(*) > 0 FROM games " +
           "WHERE (player1_id = :userId OR player2_id = :userId) " +
           "AND game_type = 'pvp' " +
           "AND status IN ('waiting', 'in_progress')", nativeQuery = true)
    boolean hasActivePvpGame(@Param("userId") Long userId);

    @Query("SELECT g FROM Game g " +
           "LEFT JOIN FETCH g.player1 p1 " +
           "LEFT JOIN FETCH g.player2 p2 " +
           "WHERE (g.player1.id = :userId OR g.player2.id = :userId) " +
           "AND g.gameType = com.tbs.enums.GameType.PVP " +
           "AND g.status IN (com.tbs.enums.GameStatus.WAITING, com.tbs.enums.GameStatus.IN_PROGRESS)")
    java.util.Optional<Game> findActivePvpGameForUser(@Param("userId") Long userId);

    @Query("SELECT g FROM Game g " +
           "LEFT JOIN FETCH g.player1 p1 " +
           "LEFT JOIN FETCH g.player2 p2 " +
           "WHERE (g.player1.id IN :userIds OR g.player2.id IN :userIds) " +
           "AND g.gameType = com.tbs.enums.GameType.PVP " +
           "AND g.status IN (com.tbs.enums.GameStatus.WAITING, com.tbs.enums.GameStatus.IN_PROGRESS)")
    java.util.List<Game> findActivePvpGamesForUsers(@Param("userIds") java.util.List<Long> userIds);
}

