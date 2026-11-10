package com.taskmanagesys.time.dto;

import com.taskmanagesys.task.model.Subtask;
import java.time.LocalDate;

public record TimeSubtaskOptionResponse(Long id, String title, LocalDate deadline) {
    public static TimeSubtaskOptionResponse from(Subtask subtask) { return new TimeSubtaskOptionResponse(subtask.getId(), subtask.getTitle(), subtask.getDeadline()); }
}
