package com.taskmanagesys.task.repository;

import com.taskmanagesys.task.model.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubtaskRepository extends JpaRepository<Subtask, Long> {
    List<Subtask> findByTaskIdOrderByCreatedAtAsc(Long taskId);
    List<Subtask> findByTaskId(Long taskId);
}
