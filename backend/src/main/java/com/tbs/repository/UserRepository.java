package com.tbs.repository;

import com.tbs.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAuthUserId(UUID authUserId);
    
    Optional<User> findByIdAndIsGuest(Long id, Boolean isGuest);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
    
    Optional<User> findByUsername(String username);
    
    @Modifying
    @Query("UPDATE User u SET u.lastSeenAt = :timestamp WHERE u.id = :userId")
    int updateLastSeenAt(@Param("userId") Long userId, @Param("timestamp") Instant timestamp);
}

