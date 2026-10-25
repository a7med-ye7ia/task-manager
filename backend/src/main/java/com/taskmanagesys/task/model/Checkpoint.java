package com.taskmanagesys.task.model;

import com.taskmanagesys.user.model.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "checkpoints")
public class Checkpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subtask_id")
    private Subtask subtask;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "is_done", nullable = false)
    private boolean isDone = false;

    @Column(nullable = false)
    private int position = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "done_by")
    private User doneBy;

    @Column(name = "done_at")
    private Instant doneAt;

    public Checkpoint() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public Subtask getSubtask() { return subtask; }
    public void setSubtask(Subtask subtask) { this.subtask = subtask; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isDone() { return isDone; }
    public void setDone(boolean done) { isDone = done; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public User getDoneBy() { return doneBy; }
    public void setDoneBy(User doneBy) { this.doneBy = doneBy; }

    public Instant getDoneAt() { return doneAt; }
    public void setDoneAt(Instant doneAt) { this.doneAt = doneAt; }
}
