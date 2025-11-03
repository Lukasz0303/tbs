package com.tbs.repository;

import com.tbs.model.Move;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {

    @Query("SELECT m FROM Move m LEFT JOIN FETCH m.player WHERE m.game.id = :gameId ORDER BY m.moveOrder ASC")
    List<Move> findByGameIdOrderByMoveOrderAsc(@Param("gameId") Long gameId);

    @Query("SELECT m FROM Move m WHERE m.game.id = :gameId ORDER BY m.moveOrder DESC")
    Optional<Move> findFirstByGameIdOrderByMoveOrderDesc(@Param("gameId") Long gameId);

    long countByGameId(Long gameId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COALESCE(MAX(m.moveOrder), 0) + 1 FROM Move m WHERE m.game.id = :gameId")
    short getNextMoveOrder(@Param("gameId") Long gameId);

    @Query("SELECT m.game.id, COUNT(m.id) FROM Move m WHERE m.game.id IN :gameIds GROUP BY m.game.id")
    List<Object[]> countByGameIds(@Param("gameIds") List<Long> gameIds);

    default Map<Long, Long> getMoveCountsByGameIds(List<Long> gameIds) {
        if (gameIds == null || gameIds.isEmpty()) {
            return Map.of();
        }
        return countByGameIds(gameIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Long) row[0]),
                        row -> ((Long) ((Number) row[1]).longValue())
                ));
    }
}

