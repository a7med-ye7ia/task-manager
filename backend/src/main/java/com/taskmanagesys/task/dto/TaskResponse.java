package com.taskmanagesys.task.dto;

import com.taskmanagesys.task.model.TaskPriority;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.user.dto.UserSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDate deadline;
    private boolean deadlineManual;
    private boolean isOverdue;
    private TaskPriority priority;
    private TaskStatus status;
    private int progress;
    private UserSummary lead;
    private List<TaskMemberDto> members;
    private List<SubtaskResponse> subtasks;
    private List<CheckpointResponse> checkpoints; // task-level checkpoints
    private UserSummary createdBy;
    private Instant doneAt;
    private Instant purgeAt;
    private Instant createdAt;
    private Instant updatedAt;
    private long mentionUnreadCount;
    private long unreadCount;

    public TaskResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public boolean isDeadlineManual() { return deadlineManual; }
    public void setDeadlineManual(boolean deadlineManual) { this.deadlineManual = deadlineManual; }

    public boolean isOverdue() { return isOverdue; }
    public void setOverdue(boolean overdue) { isOverdue = overdue; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public UserSummary getLead() { return lead; }
    public void setLead(UserSummary lead) { this.lead = lead; }

    public List<TaskMemberDto> getMembers() { return members; }
    public void setMembers(List<TaskMemberDto> members) { this.members = members; }

    public List<SubtaskResponse> getSubtasks() { return subtasks; }
    public void setSubtasks(List<SubtaskResponse> subtasks) { this.subtasks = subtasks; }

    public List<CheckpointResponse> getCheckpoints() { return checkpoints; }
    public void setCheckpoints(List<CheckpointResponse> checkpoints) { this.checkpoints = checkpoints; }

    public UserSummary getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserSummary createdBy) { this.createdBy = createdBy; }

    public Instant getDoneAt() { return doneAt; }
    public void setDoneAt(Instant doneAt) { this.doneAt = doneAt; }
    public Instant getPurgeAt() { return purgeAt; }
    public void setPurgeAt(Instant purgeAt) { this.purgeAt = purgeAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getMentionUnreadCount() { return mentionUnreadCount; }
    public void setMentionUnreadCount(long mentionUnreadCount) { this.mentionUnreadCount = mentionUnreadCount; }
    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }
}
