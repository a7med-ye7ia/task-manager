package com.taskmanagesys.task.dto;

import com.taskmanagesys.task.model.TaskFile;
import java.time.Instant;

public record TaskFileResponse(Long id, Long taskId, Long subtaskId, String originalName, String contentType,
                               long sizeBytes, Long uploaderId, String uploaderName, Instant createdAt, Instant updatedAt, Long editedBy) {
    public static TaskFileResponse from(TaskFile file, String uploaderName) {
        return new TaskFileResponse(file.getId(), file.getTaskId(), file.getSubtaskId(), file.getOriginalName(),
                file.getContentType(), file.getSizeBytes(), file.getUploaderId(), uploaderName, file.getCreatedAt(), file.getUpdatedAt(), file.getEditedBy());
    }
}
