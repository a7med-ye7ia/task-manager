package com.taskmanagesys.task.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "task_files")
public class TaskFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "task_id", nullable = false) private Long taskId;
    @Column(name = "subtask_id") private Long subtaskId;
    @Column(name = "uploader_id", nullable = false) private Long uploaderId;
    @Column(name = "stored_name", nullable = false, unique = true) private String storedName;
    @Column(name = "original_name", nullable = false) private String originalName;
    @Column(name = "content_type", nullable = false) private String contentType;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @Column(name = "edited_by") private Long editedBy;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; } public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getSubtaskId() { return subtaskId; } public void setSubtaskId(Long subtaskId) { this.subtaskId = subtaskId; }
    public Long getUploaderId() { return uploaderId; } public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }
    public String getStoredName() { return storedName; } public void setStoredName(String storedName) { this.storedName = storedName; }
    public String getOriginalName() { return originalName; } public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getContentType() { return contentType; } public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSizeBytes() { return sizeBytes; } public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getEditedBy() { return editedBy; } public void setEditedBy(Long editedBy) { this.editedBy = editedBy; }
}
