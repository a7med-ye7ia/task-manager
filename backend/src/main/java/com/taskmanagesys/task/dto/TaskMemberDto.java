package com.taskmanagesys.task.dto;

import com.taskmanagesys.task.model.TaskMemberRole;
import com.taskmanagesys.user.dto.UserSummary;

public class TaskMemberDto {
    private UserSummary user;
    private TaskMemberRole role;

    public TaskMemberDto() {}

    public TaskMemberDto(UserSummary user, TaskMemberRole role) {
        this.user = user;
        this.role = role;
    }

    public UserSummary getUser() { return user; }
    public void setUser(UserSummary user) { this.user = user; }

    public TaskMemberRole getRole() { return role; }
    public void setRole(TaskMemberRole role) { this.role = role; }
}
