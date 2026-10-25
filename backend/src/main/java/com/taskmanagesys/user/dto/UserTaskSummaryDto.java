package com.taskmanagesys.user.dto;

import com.taskmanagesys.task.model.TaskMemberRole;
import com.taskmanagesys.task.model.TaskPriority;
import com.taskmanagesys.task.model.TaskStatus;

import java.time.LocalDate;

public class UserTaskSummaryDto {
    private Long id;
    private String title;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate deadline;
    private boolean overdue;
    private TaskMemberRole userRole;
    private int progress;

    public UserTaskSummaryDto() {}

    public UserTaskSummaryDto(Long id, String title, TaskStatus status, TaskPriority priority, LocalDate deadline, boolean overdue, TaskMemberRole userRole, int progress) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.priority = priority;
        this.deadline = deadline;
        this.overdue = overdue;
        this.userRole = userRole;
        this.progress = progress;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public boolean isOverdue() { return overdue; }
    public void setOverdue(boolean overdue) { this.overdue = overdue; }

    public TaskMemberRole getUserRole() { return userRole; }
    public void setUserRole(TaskMemberRole userRole) { this.userRole = userRole; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
}
