package com.taskmanagesys.history.repository;

import com.taskmanagesys.history.model.WorkHistoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface WorkHistoryEventRepository extends JpaRepository<WorkHistoryEvent, Long> {
    List<WorkHistoryEvent> findByHistoryIdInOrderByOccurredAtAsc(List<Long> historyIds);
    long deleteByOccurredAtBefore(Instant cutoff);
}
