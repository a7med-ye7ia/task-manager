package com.taskmanagesys.task.repository;

import com.taskmanagesys.task.model.CommentMention;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentMentionRepository extends JpaRepository<CommentMention, Long> {
    List<CommentMention> findByCommentId(Long commentId);
    void deleteByCommentId(Long commentId);
    long countByUserIdAndSeenAtIsNull(Long userId);
    long countByUserIdAndCommentTaskIdAndSeenAtIsNull(Long userId, Long taskId);
    List<CommentMention> findByUserIdAndCommentTaskIdAndSeenAtIsNull(Long userId, Long taskId);
    @Query("select m from CommentMention m where m.user.id = :userId and m.comment.task.id in :taskIds and m.seenAt is null")
    List<CommentMention> findUnseenByUserAndTaskIds(@Param("userId") Long userId, @Param("taskIds") Collection<Long> taskIds);
}
