package com.taskmanagesys.task.dto;

import com.taskmanagesys.user.dto.UserSummary;

public class CheckpointResponse {
    private Long id;
    private Long taskId;
    private Long subtaskId;
    private String title;
    private boolean isDone;
    private int position;
    private UserSummary doneBy;

    public CheckpointResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getSubtaskId() { return subtaskId; }
    public void setSubtaskId(Long subtaskId) { this.subtaskId = subtaskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isDone() { return isDone; }
    public void setDone(boolean done) { isDone = done; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public UserSummary getDoneBy() { return doneBy; }
    public void setDoneBy(UserSummary doneBy) { this.doneBy = doneBy; }
}
