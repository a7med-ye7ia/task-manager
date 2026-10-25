package com.taskmanagesys.task.repository;

import com.taskmanagesys.task.model.TaskMember;
import com.taskmanagesys.task.model.TaskMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskMemberRepository extends JpaRepository<TaskMember, TaskMember.TaskMemberId> {
    List<TaskMember> findByTaskId(Long taskId);
    List<TaskMember> findByUserId(Long userId);
    Optional<TaskMember> findByTaskIdAndRole(Long taskId, TaskMemberRole role);
    Optional<TaskMember> findByTaskIdAndUserId(Long taskId, Long userId);
    boolean existsByTaskIdAndUserId(Long taskId, Long userId);
    void deleteByTaskIdAndUserId(Long taskId, Long userId);
}
