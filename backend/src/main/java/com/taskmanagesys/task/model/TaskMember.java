package com.taskmanagesys.task.model;

import com.taskmanagesys.user.model.User;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "task_members")
@IdClass(TaskMember.TaskMemberId.class)
public class TaskMember {

    @Id
    @Column(name = "task_id")
    private Long taskId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskMemberRole role = TaskMemberRole.MEMBER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private User addedBy;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt = Instant.now();

    public TaskMember() {}

    public TaskMember(Task task, User user, TaskMemberRole role, User addedBy) {
        this.task = task;
        this.user = user;
        this.taskId = task.getId();
        this.userId = user.getId();
        this.role = role;
        this.addedBy = addedBy;
        this.addedAt = Instant.now();
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Task getTask() { return task; }
    public void setTask(Task task) {
        this.task = task;
        if (task != null) {
            this.taskId = task.getId();
        }
    }

    public User getUser() { return user; }
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public TaskMemberRole getRole() { return role; }
    public void setRole(TaskMemberRole role) { this.role = role; }

    public User getAddedBy() { return addedBy; }
    public void setAddedBy(User addedBy) { this.addedBy = addedBy; }

    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }

    public static class TaskMemberId implements Serializable {
        private Long taskId;
        private Long userId;

        public TaskMemberId() {}

        public TaskMemberId(Long taskId, Long userId) {
            this.taskId = taskId;
            this.userId = userId;
        }

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TaskMemberId that = (TaskMemberId) o;
            return Objects.equals(taskId, that.taskId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskId, userId);
        }
    }
}
