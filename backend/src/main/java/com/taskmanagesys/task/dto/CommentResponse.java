package com.taskmanagesys.task.dto;

import com.taskmanagesys.task.model.Comment;
import java.time.Instant;
import com.taskmanagesys.user.dto.UserSummary;

public record CommentResponse(Long id, Long taskId, Long subtaskId, Long authorId, String authorName, String authorAvatarPath,
                              String body, boolean deleted, boolean edited, Instant createdAt, Instant editedAt, java.util.List<Long> mentionedUserIds, java.util.List<MentionedUserResponse> mentionedUsers, UserSummary author) {
    public static CommentResponse from(Comment c) {
        return from(c, java.util.List.of(), java.util.List.of());
    }
    public static CommentResponse from(Comment c, java.util.List<Long> mentionedUserIds) {
        return from(c, mentionedUserIds, java.util.List.of());
    }
    public static CommentResponse from(Comment c, java.util.List<Long> mentionedUserIds, java.util.List<MentionedUserResponse> mentionedUsers) {
        String name = c.getAuthor() == null ? "Deleted user" : c.getAuthor().getFirstName() + " " + c.getAuthor().getLastName();
        return new CommentResponse(c.getId(), c.getTask().getId(), c.getSubtask() == null ? null : c.getSubtask().getId(),
                c.getAuthor() == null ? null : c.getAuthor().getId(), name, c.getAuthor() == null ? null : c.getAuthor().getAvatarPath(),
                c.getDeletedAt() == null ? c.getBody() : "comment deleted", c.getDeletedAt() != null,
                c.getEditedAt() != null, c.getCreatedAt(), c.getEditedAt(), mentionedUserIds, mentionedUsers, UserSummary.from(c.getAuthor()));
    }
}
