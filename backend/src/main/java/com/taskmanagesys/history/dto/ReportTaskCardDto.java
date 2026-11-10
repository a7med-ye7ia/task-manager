package com.taskmanagesys.history.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportTaskCardDto {
    private Long taskId;
    private String title;
    private String priority;
    private String status;
    private String role;
    private boolean selfReported;
    private LocalDate deadline;
    private long workedSeconds;
    private double hours;
    private int subtasksCompletedCount;
    private int checkpointsTickedCount;
    private int commentsCount;
    private List<ReportEventDto> events = new ArrayList<>();
    private String summary;

    public ReportTaskCardDto() {}

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isSelfReported() { return selfReported; }
    public void setSelfReported(boolean selfReported) { this.selfReported = selfReported; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public long getWorkedSeconds() { return workedSeconds; }
    public void setWorkedSeconds(long workedSeconds) { this.workedSeconds = workedSeconds; }

    public double getHours() { return hours; }
    public void setHours(double hours) { this.hours = hours; }

    public int getSubtasksCompletedCount() { return subtasksCompletedCount; }
    public void setSubtasksCompletedCount(int subtasksCompletedCount) { this.subtasksCompletedCount = subtasksCompletedCount; }

    public int getCheckpointsTickedCount() { return checkpointsTickedCount; }
    public void setCheckpointsTickedCount(int checkpointsTickedCount) { this.checkpointsTickedCount = checkpointsTickedCount; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public List<ReportEventDto> getEvents() { return events; }
    public void setEvents(List<ReportEventDto> events) { this.events = events; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
