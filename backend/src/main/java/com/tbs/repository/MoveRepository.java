package com.tbs.repository;

import com.tbs.model.Move;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {

    List<Move> findByGameIdOrderByMoveOrderAsc(Long gameId);

    Optional<Move> findFirstByGameIdOrderByMoveOrderDesc(Long gameId);

    long countByGameId(Long gameId);
}

