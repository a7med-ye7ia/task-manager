package com.taskmanagesys.task.dto;

import com.taskmanagesys.task.model.TaskActivity;
import java.time.Instant;

public record TaskActivityResponse(Long id, Long taskId, Long actorId, String action, String details, Instant createdAt) {
    public static TaskActivityResponse from(TaskActivity a) {
        return new TaskActivityResponse(a.getId(), a.getTaskId(), a.getActorId(), a.getAction(), a.getDetails(), a.getCreatedAt());
    }
}
