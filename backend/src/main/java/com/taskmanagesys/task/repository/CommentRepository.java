package com.taskmanagesys.task.repository;

import com.taskmanagesys.task.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
    List<Comment> findByTaskIdInOrderByIdAsc(Collection<Long> taskIds);
    Optional<Comment> findByIdAndTaskId(Long id, Long taskId);
}
