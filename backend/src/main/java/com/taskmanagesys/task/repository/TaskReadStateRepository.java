package com.taskmanagesys.task.repository;

import com.taskmanagesys.task.model.TaskReadState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TaskReadStateRepository extends JpaRepository<TaskReadState, TaskReadState.Id> {
    Optional<TaskReadState> findByIdTaskIdAndIdUserId(Long taskId, Long userId);
}
