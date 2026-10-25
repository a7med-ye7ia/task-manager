package com.taskmanagesys.task.dto;

import com.taskmanagesys.task.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public class ChangeTaskStatusRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status;

    private String reason; // required when sending back from REVIEW to IN_PROGRESS

    public ChangeTaskStatusRequest() {}

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
