package com.taskmanagesys.task.model;

import com.taskmanagesys.user.model.User;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "subtask_assignees")
@IdClass(SubtaskAssignee.SubtaskAssigneeId.class)
public class SubtaskAssignee {

    @Id
    @Column(name = "subtask_id")
    private Long subtaskId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subtask_id", insertable = false, updatable = false)
    private Subtask subtask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();

    public SubtaskAssignee() {}

    public SubtaskAssignee(Subtask subtask, User user, User assignedBy) {
        this.subtask = subtask;
        this.user = user;
        this.subtaskId = subtask.getId();
        this.userId = user.getId();
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    public Long getSubtaskId() { return subtaskId; }
    public void setSubtaskId(Long subtaskId) { this.subtaskId = subtaskId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Subtask getSubtask() { return subtask; }
    public void setSubtask(Subtask subtask) {
        this.subtask = subtask;
        if (subtask != null) {
            this.subtaskId = subtask.getId();
        }
    }

    public User getUser() { return user; }
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public User getAssignedBy() { return assignedBy; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public static class SubtaskAssigneeId implements Serializable {
        private Long subtaskId;
        private Long userId;

        public SubtaskAssigneeId() {}

        public SubtaskAssigneeId(Long subtaskId, Long userId) {
            this.subtaskId = subtaskId;
            this.userId = userId;
        }

        public Long getSubtaskId() { return subtaskId; }
        public void setSubtaskId(Long subtaskId) { this.subtaskId = subtaskId; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SubtaskAssigneeId that = (SubtaskAssigneeId) o;
            return Objects.equals(subtaskId, that.subtaskId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(subtaskId, userId);
        }
    }
}
