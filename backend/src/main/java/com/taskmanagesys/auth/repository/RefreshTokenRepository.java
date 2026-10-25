package com.taskmanagesys.auth.repository;

import com.taskmanagesys.auth.model.RefreshToken;
import com.taskmanagesys.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.Instant;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = CURRENT_TIMESTAMP WHERE r.user = :user AND r.revokedAt IS NULL")
    void revokeAllForUser(User user);
    long deleteByExpiresAtBeforeOrRevokedAtBefore(Instant expiresCutoff, Instant revokedCutoff);
}
