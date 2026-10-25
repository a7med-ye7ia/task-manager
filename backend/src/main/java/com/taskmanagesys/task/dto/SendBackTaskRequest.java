package com.taskmanagesys.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class SendBackTaskRequest {
    @NotBlank
    @Size(min = 3, max = 2000)
    private String reason;
    private List<Long> subtaskIds = List.of();
    private List<Long> checkpointIds = List.of();

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public List<Long> getSubtaskIds() { return subtaskIds == null ? List.of() : subtaskIds; }
    public void setSubtaskIds(List<Long> subtaskIds) { this.subtaskIds = subtaskIds; }
    public List<Long> getCheckpointIds() { return checkpointIds == null ? List.of() : checkpointIds; }
    public void setCheckpointIds(List<Long> checkpointIds) { this.checkpointIds = checkpointIds; }
}
