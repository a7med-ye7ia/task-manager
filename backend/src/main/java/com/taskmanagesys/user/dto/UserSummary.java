package com.taskmanagesys.user.dto;

import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;

public record UserSummary(Long id, String firstName, String lastName, String avatarPath, Role role) {
    public static UserSummary from(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getFirstName(), user.getLastName(), user.getAvatarPath(), user.getRole());
    }
}
