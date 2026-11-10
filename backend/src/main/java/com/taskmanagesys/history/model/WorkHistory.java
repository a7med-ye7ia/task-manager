package com.taskmanagesys.history.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "work_history", indexes = {
        @Index(name = "idx_work_history_user_dates", columnList = "user_id,joined_at,left_at")
})
public class WorkHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "task_id") private Long taskId;
    @Column(name = "task_title", nullable = false, length = 150) private String taskTitle;
    @Column(length = 10) private String priority;
    private LocalDate deadline;
    @Column(name = "task_role", length = 10) private String taskRole;
    @Column(name = "joined_at", nullable = false) private Instant joinedAt;
    @Column(name = "left_at") private Instant leftAt;
    @Column(name = "self_reported", nullable = false) private boolean selfReported;
    @Column(name = "final_status", length = 20) private String finalStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public String getTaskRole() { return taskRole; }
    public void setTaskRole(String taskRole) { this.taskRole = taskRole; }
    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
    public Instant getLeftAt() { return leftAt; }
    public void setLeftAt(Instant leftAt) { this.leftAt = leftAt; }
    public boolean isSelfReported() { return selfReported; }
    public void setSelfReported(boolean selfReported) { this.selfReported = selfReported; }
    public String getFinalStatus() { return finalStatus; }
    public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }
}
