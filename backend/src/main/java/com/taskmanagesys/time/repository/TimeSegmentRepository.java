package com.taskmanagesys.time.repository;

import com.taskmanagesys.time.model.TimeSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface TimeSegmentRepository extends JpaRepository<TimeSegment, Long> {
    List<TimeSegment> findBySessionIdOrderByStartedAtAsc(Long sessionId);
    List<TimeSegment> findBySessionIdInOrderByStartedAtAsc(List<Long> sessionIds);
    List<TimeSegment> findByStartedAtBeforeAndEndedAtAfter(Instant before, Instant after);
}
