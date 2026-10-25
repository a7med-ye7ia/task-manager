package com.taskmanagesys.notification.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 50) private String type;
    @Column(name = "task_id") private Long taskId;
    @Column(nullable = false, length = 500) private String message;
    @Column(name = "is_read", nullable = false) private boolean read;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    public Notification() {}
    public Notification(Long userId, String type, Long taskId, String message) { this.userId=userId; this.type=type; this.taskId=taskId; this.message=message; }
    public Long getId(){return id;} public Long getUserId(){return userId;} public String getType(){return type;} public Long getTaskId(){return taskId;} public String getMessage(){return message;} public boolean isRead(){return read;} public void setRead(boolean read){this.read=read;} public Instant getCreatedAt(){return createdAt;}
}
