package com.taskmanagesys.task.dto;

import com.taskmanagesys.task.model.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    private LocalDate deadline;

    @NotNull(message = "Priority is required")
    private TaskPriority priority = TaskPriority.MEDIUM;

    @NotNull(message = "At least one employee must be selected")
    @Size(min = 1, message = "At least one employee must be selected")
    private List<Long> memberUserIds;

    private Long leadUserId;

    private List<CreateSubtaskRequest> subtasks;

    private List<CreateCheckpointRequest> checkpoints;

    public CreateTaskRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public List<Long> getMemberUserIds() { return memberUserIds; }
    public void setMemberUserIds(List<Long> memberUserIds) { this.memberUserIds = memberUserIds; }

    public Long getLeadUserId() { return leadUserId; }
    public void setLeadUserId(Long leadUserId) { this.leadUserId = leadUserId; }

    public List<CreateSubtaskRequest> getSubtasks() { return subtasks; }
    public void setSubtasks(List<CreateSubtaskRequest> subtasks) { this.subtasks = subtasks; }

    public List<CreateCheckpointRequest> getCheckpoints() { return checkpoints; }
    public void setCheckpoints(List<CreateCheckpointRequest> checkpoints) { this.checkpoints = checkpoints; }
}
