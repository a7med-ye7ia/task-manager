package com.taskmanagesys.time.dto;

import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskStatus;
import java.time.LocalDate;

public record TimeTaskOptionResponse(Long id, String title, TaskStatus status, LocalDate deadline) {
    public static TimeTaskOptionResponse from(Task task) { return new TimeTaskOptionResponse(task.getId(), task.getTitle(), task.getStatus(), task.getDeadline()); }
}
