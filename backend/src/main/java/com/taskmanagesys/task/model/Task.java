package com.taskmanagesys.task.model;

import com.taskmanagesys.user.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate deadline;

    @Column(name = "deadline_manual", nullable = false)
    private boolean deadlineManual = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "self_reported", nullable = false)
    private boolean selfReported = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "done_at")
    private Instant doneAt;

    @Column(name = "purge_at")
    private Instant purgeAt;

    @Column(name = "purge_warning_sent_at")
    private Instant purgeWarningSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subtask> subtasks = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Checkpoint> checkpoints = new ArrayList<>();

    public Task() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public boolean isDeadlineManual() { return deadlineManual; }
    public void setDeadlineManual(boolean deadlineManual) { this.deadlineManual = deadlineManual; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public boolean isSelfReported() { return selfReported; }
    public void setSelfReported(boolean selfReported) { this.selfReported = selfReported; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Instant getDoneAt() { return doneAt; }
    public void setDoneAt(Instant doneAt) { this.doneAt = doneAt; }

    public Instant getPurgeAt() { return purgeAt; }
    public void setPurgeAt(Instant purgeAt) { this.purgeAt = purgeAt; }

    public Instant getPurgeWarningSentAt() { return purgeWarningSentAt; }
    public void setPurgeWarningSentAt(Instant purgeWarningSentAt) { this.purgeWarningSentAt = purgeWarningSentAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<TaskMember> getMembers() { return members; }
    public void setMembers(List<TaskMember> members) { this.members = members; }

    public List<Subtask> getSubtasks() { return subtasks; }
    public void setSubtasks(List<Subtask> subtasks) { this.subtasks = subtasks; }

    public List<Checkpoint> getCheckpoints() { return checkpoints; }
    public void setCheckpoints(List<Checkpoint> checkpoints) { this.checkpoints = checkpoints; }
}
