package com.taskmanagesys.task.dto;
import java.time.Instant;
public record TaskFileDetailResponse(Long id, Long taskId, Long subtaskId, String taskTitle, String subtaskTitle, String originalName, String contentType, long sizeBytes, Long uploaderId, String uploaderName, Instant createdAt, Instant updatedAt, Long editedBy, Long previousId, Long nextId) { }
