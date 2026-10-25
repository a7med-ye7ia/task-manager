package com.taskmanagesys.task.repository;

import com.taskmanagesys.task.model.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {
    List<Checkpoint> findByTaskIdOrderByPositionAscIdAsc(Long taskId);
    List<Checkpoint> findBySubtaskIdOrderByPositionAscIdAsc(Long subtaskId);
    List<Checkpoint> findByTaskIdAndSubtaskIdIsNullOrderByPositionAscIdAsc(Long taskId);
    long countByTaskIdAndSubtaskIdIsNull(Long taskId);
    long countByTaskIdAndSubtaskIdIsNullAndIsDoneTrue(Long taskId);
    long countBySubtaskId(Long subtaskId);
    long countBySubtaskIdAndIsDoneTrue(Long subtaskId);
}
