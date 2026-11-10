package com.taskmanagesys.time.repository;

import com.taskmanagesys.time.model.TimeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TimeSessionRepository extends JpaRepository<TimeSession, Long> {
    Optional<TimeSession> findFirstByUserIdAndEndedAtIsNull(Long userId);
    List<TimeSession> findByUserIdOrderByStartedAtDesc(Long userId);
    List<TimeSession> findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(Long userId, Instant from, Instant to);
    Optional<TimeSession> findFirstByUserIdAndTaskIdAndEndedAtIsNotNullAndStartedAtBetweenOrderByStartedAtDesc(Long userId, Long taskId, Instant from, Instant to);
    List<TimeSession> findByEndedAtIsNullAndLastHeartbeatAtBefore(Instant cutoff);
    List<TimeSession> findByEndedAtIsNullAndStartedAtBefore(Instant cutoff);
    long deleteByStartedAtBefore(Instant cutoff);
    boolean existsByTaskId(Long taskId);

    @Query("SELECT s FROM TimeSession s WHERE s.userId = :userId AND s.startedAt < :rangeEnd AND (s.endedAt IS NULL OR s.endedAt > :rangeStart) ORDER BY s.startedAt ASC")
    List<TimeSession> findSessionsForReport(@Param("userId") Long userId, @Param("rangeStart") Instant rangeStart, @Param("rangeEnd") Instant rangeEnd);
}
