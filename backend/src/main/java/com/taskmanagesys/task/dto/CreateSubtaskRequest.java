package com.taskmanagesys.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class CreateSubtaskRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 150, message = "Title must be between 1 and 150 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Deadline is required")
    private LocalDate deadline;

    @NotNull(message = "At least one assignee is required")
    @Size(min = 1, message = "At least one assignee is required")
    private List<Long> assigneeUserIds;
    private boolean confirmAddMember;

    public CreateSubtaskRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public List<Long> getAssigneeUserIds() { return assigneeUserIds; }
    public void setAssigneeUserIds(List<Long> assigneeUserIds) { this.assigneeUserIds = assigneeUserIds; }
    public boolean isConfirmAddMember() { return confirmAddMember; }
    public void setConfirmAddMember(boolean confirmAddMember) { this.confirmAddMember = confirmAddMember; }
}
