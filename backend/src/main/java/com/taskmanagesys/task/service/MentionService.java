package com.taskmanagesys.task.service;

import com.taskmanagesys.task.model.CommentMention;
import com.taskmanagesys.task.repository.CommentMentionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MentionService {
    private final CommentMentionRepository mentions;
    private final TaskAccessService access;
    public MentionService(CommentMentionRepository mentions, TaskAccessService access) { this.mentions = mentions; this.access = access; }
    @Transactional(readOnly = true) public long totalUnseen(Long userId) { return mentions.countByUserIdAndSeenAtIsNull(userId); }
    @Transactional(readOnly = true) public long taskUnseen(Long taskId, Long userId) { access.requireMember(taskId, userId); return mentions.countByUserIdAndCommentTaskIdAndSeenAtIsNull(userId, taskId); }
    @Transactional public void markTaskSeen(Long taskId, Long userId) { access.requireMember(taskId, userId); for (CommentMention m : mentions.findByUserIdAndCommentTaskIdAndSeenAtIsNull(userId, taskId)) m.setSeenAt(java.time.Instant.now()); }
}
