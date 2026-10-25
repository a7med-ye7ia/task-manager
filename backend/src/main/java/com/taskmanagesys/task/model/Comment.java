package com.taskmanagesys.task.model;

import com.taskmanagesys.user.model.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "task_id", nullable = false)
    private Task task;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "author_id")
    private User author;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subtask_id")
    private Subtask subtask;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @Column(name = "edited_at") private Instant editedAt;
    @Column(name = "deleted_at") private Instant deletedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    public Long getId() { return id; }
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public Subtask getSubtask() { return subtask; }
    public void setSubtask(Subtask subtask) { this.subtask = subtask; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getEditedAt() { return editedAt; }
    public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
