package com.taskmanagesys.history.dto;

public class ReportSummaryDto {
    private long totalWorkedSeconds;
    private double totalHours;
    private int tasksWorkedOn;
    private int tasksCompleted;
    private int subtasksCompleted;
    private int checkpointsTicked;

    public ReportSummaryDto() {}

    public ReportSummaryDto(long totalWorkedSeconds, double totalHours, int tasksWorkedOn, int tasksCompleted, int subtasksCompleted, int checkpointsTicked) {
        this.totalWorkedSeconds = totalWorkedSeconds;
        this.totalHours = totalHours;
        this.tasksWorkedOn = tasksWorkedOn;
        this.tasksCompleted = tasksCompleted;
        this.subtasksCompleted = subtasksCompleted;
        this.checkpointsTicked = checkpointsTicked;
    }

    public long getTotalWorkedSeconds() { return totalWorkedSeconds; }
    public void setTotalWorkedSeconds(long totalWorkedSeconds) { this.totalWorkedSeconds = totalWorkedSeconds; }

    public double getTotalHours() { return totalHours; }
    public void setTotalHours(double totalHours) { this.totalHours = totalHours; }

    public int getTasksWorkedOn() { return tasksWorkedOn; }
    public void setTasksWorkedOn(int tasksWorkedOn) { this.tasksWorkedOn = tasksWorkedOn; }

    public int getTasksCompleted() { return tasksCompleted; }
    public void setTasksCompleted(int tasksCompleted) { this.tasksCompleted = tasksCompleted; }

    public int getSubtasksCompleted() { return subtasksCompleted; }
    public void setSubtasksCompleted(int subtasksCompleted) { this.subtasksCompleted = subtasksCompleted; }

    public int getCheckpointsTicked() { return checkpointsTicked; }
    public void setCheckpointsTicked(int checkpointsTicked) { this.checkpointsTicked = checkpointsTicked; }
}
