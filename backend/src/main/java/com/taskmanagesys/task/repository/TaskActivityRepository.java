package com.taskmanagesys.task.repository;

import com.taskmanagesys.task.model.TaskActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;

@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {
    List<TaskActivity> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    List<TaskActivity> findByTaskIdInOrderByIdAsc(Collection<Long> taskIds);
    Page<TaskActivity> findByTaskIdOrderByCreatedAtDesc(Long taskId, Pageable pageable);
}
