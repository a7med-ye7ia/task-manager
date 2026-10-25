package com.taskmanagesys.task.dto;

import com.taskmanagesys.task.model.CommentMention;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.model.Role;

public record MentionedUserResponse(Long id, String firstName, String lastName, String avatarPath, Role role, boolean active) {
    public static MentionedUserResponse from(CommentMention mention) {
        var user = mention.getUser();
        return new MentionedUserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getAvatarPath(), user.getRole(), user.getDeletedAt() == null && user.getStatus() == UserStatus.ACTIVE);
    }
}
