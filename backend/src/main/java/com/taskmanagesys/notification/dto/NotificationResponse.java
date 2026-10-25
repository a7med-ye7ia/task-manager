package com.taskmanagesys.notification.dto;

import com.taskmanagesys.notification.model.Notification;
import java.time.Instant;

public record NotificationResponse(Long id, String type, Long taskId, String message, boolean read, Instant createdAt) {
    public static NotificationResponse from(Notification n) { return new NotificationResponse(n.getId(), n.getType(), n.getTaskId(), n.getMessage(), n.isRead(), n.getCreatedAt()); }
}
