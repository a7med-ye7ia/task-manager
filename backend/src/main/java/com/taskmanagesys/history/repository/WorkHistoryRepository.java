package com.taskmanagesys.history.repository;

import com.taskmanagesys.history.model.WorkHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkHistoryRepository extends JpaRepository<WorkHistory, Long> {
    Optional<WorkHistory> findByUserIdAndTaskId(Long userId, Long taskId);
    List<WorkHistory> findByUserId(Long userId);
    long deleteByJoinedAtBefore(Instant cutoff);
}
