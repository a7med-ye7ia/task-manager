package com.taskmanagesys.task.repository;

import com.taskmanagesys.task.model.SubtaskAssignee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubtaskAssigneeRepository extends JpaRepository<SubtaskAssignee, SubtaskAssignee.SubtaskAssigneeId> {
    List<SubtaskAssignee> findBySubtaskId(Long subtaskId);
    List<SubtaskAssignee> findByUserId(Long userId);
    boolean existsBySubtaskIdAndUserId(Long subtaskId, Long userId);
    void deleteBySubtaskIdAndUserId(Long subtaskId, Long userId);
    void deleteBySubtaskId(Long subtaskId);
    long countBySubtaskId(Long subtaskId);
}
