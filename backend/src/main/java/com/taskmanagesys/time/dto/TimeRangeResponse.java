package com.taskmanagesys.time.dto;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public record TimeRangeResponse(LocalDate from, LocalDate to, long workedSeconds, long regularSeconds,
        long overtimeSeconds, long daysWorked, List<Day> days) {
    public record Day(LocalDate date, long workedSeconds, long regularSeconds, long overtimeSeconds,
            List<Entry> entries) { }

    public record Entry(Long id, Long taskId, Long subtaskId, String taskTitle, Instant startedAt, Instant endedAt,
            long workedSeconds, String source, String note, String startNote, String state, boolean editable) { }
}
