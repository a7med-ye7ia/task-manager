package com.taskmanagesys.task.model;

import jakarta.persistence.*;

@Entity
@Table(name = "task_read_state")
public class TaskReadState {
    @EmbeddedId private Id id;
    @Column(name = "chat_last_id", nullable = false) private Long chatLastId = 0L;
    @Column(name = "activity_last_id", nullable = false) private Long activityLastId = 0L;
    public TaskReadState() {}
    public TaskReadState(Long taskId, Long userId) { this.id = new Id(taskId, userId); }
    public Id getId() { return id; }
    public Long getChatLastId() { return chatLastId; }
    public void setChatLastId(Long value) { chatLastId = value; }
    public Long getActivityLastId() { return activityLastId; }
    public void setActivityLastId(Long value) { activityLastId = value; }
    @Embeddable public static class Id implements java.io.Serializable {
        @Column(name = "task_id") private Long taskId;
        @Column(name = "user_id") private Long userId;
        public Id() {}
        public Id(Long taskId, Long userId) { this.taskId = taskId; this.userId = userId; }
        public Long getTaskId() { return taskId; }
        public Long getUserId() { return userId; }
        @Override public boolean equals(Object o) { if (!(o instanceof Id other)) return false; return java.util.Objects.equals(taskId, other.taskId) && java.util.Objects.equals(userId, other.userId); }
        @Override public int hashCode() { return java.util.Objects.hash(taskId, userId); }
    }
}
