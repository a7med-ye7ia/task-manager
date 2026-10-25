package com.taskmanagesys.task.dto;

import com.taskmanagesys.task.model.SubtaskStatus;
import com.taskmanagesys.user.dto.UserSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class SubtaskResponse {
    private Long id;
    private Long taskId;
    private String title;
    private String description;
    private LocalDate deadline;
    private SubtaskStatus status;
    private int progress;
    private Instant doneAt;
    private Instant createdAt;
    private List<UserSummary> assignees;
    private List<CheckpointResponse> checkpoints;

    public SubtaskResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public SubtaskStatus getStatus() { return status; }
    public void setStatus(SubtaskStatus status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public Instant getDoneAt() { return doneAt; }
    public void setDoneAt(Instant doneAt) { this.doneAt = doneAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<UserSummary> getAssignees() { return assignees; }
    public void setAssignees(List<UserSummary> assignees) { this.assignees = assignees; }

    public List<CheckpointResponse> getCheckpoints() { return checkpoints; }
    public void setCheckpoints(List<CheckpointResponse> checkpoints) { this.checkpoints = checkpoints; }
}
