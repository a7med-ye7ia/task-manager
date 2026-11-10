package com.taskmanagesys.task.repository;

import com.taskmanagesys.task.model.TaskFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskFileRepository extends JpaRepository<TaskFile, Long> {
    List<TaskFile> findByTaskIdOrderBySubtaskIdAscCreatedAtAsc(Long taskId);
    List<TaskFile> findByTaskId(Long taskId);
    List<TaskFile> findBySubtaskId(Long subtaskId);
    long countByTaskId(Long taskId);
    long countByTaskIdAndSizeBytesGreaterThan(Long taskId, long size);
    long deleteByTaskId(Long taskId);
    long deleteBySubtaskId(Long subtaskId);
}
