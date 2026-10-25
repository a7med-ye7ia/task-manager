package com.taskmanagesys.task.repository;

import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.Instant;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByStatusAndPurgeAtIsNotNullAndPurgeAtLessThanEqual(TaskStatus status, Instant time);
    List<Task> findByStatusAndPurgeAtIsNotNullAndPurgeAtBetween(TaskStatus status, Instant from, Instant to);

    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN t.members m " +
           "LEFT JOIN t.subtasks s " +
           "LEFT JOIN s.assignees a " +
           "WHERE m.userId = :userId OR a.userId = :userId " +
           "ORDER BY t.createdAt DESC")
    List<Task> findTasksForUser(@Param("userId") Long userId);
}
