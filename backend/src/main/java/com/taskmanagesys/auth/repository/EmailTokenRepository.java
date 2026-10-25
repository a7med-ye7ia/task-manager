package com.taskmanagesys.auth.repository;

import com.taskmanagesys.auth.model.EmailToken;
import com.taskmanagesys.auth.model.TokenType;
import com.taskmanagesys.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.Instant;

@Repository
public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {
    Optional<EmailToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE EmailToken t SET t.usedAt = CURRENT_TIMESTAMP WHERE t.user = :user AND t.type = :type AND t.usedAt IS NULL")
    void invalidateActiveTokensForUser(User user, TokenType type);
    long deleteByUsedAtIsNotNullAndUsedAtBefore(Instant cutoff);
    long deleteByExpiresAtBefore(Instant cutoff);
}
