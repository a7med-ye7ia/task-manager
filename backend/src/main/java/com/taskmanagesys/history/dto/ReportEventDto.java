package com.taskmanagesys.history.dto;

import java.time.Instant;

public class ReportEventDto {
    private String eventType;
    private String subtaskTitle;
    private Integer count;
    private Instant occurredAt;

    public ReportEventDto() {}

    public ReportEventDto(String eventType, String subtaskTitle, Integer count, Instant occurredAt) {
        this.eventType = eventType;
        this.subtaskTitle = subtaskTitle;
        this.count = count;
        this.occurredAt = occurredAt;
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getSubtaskTitle() { return subtaskTitle; }
    public void setSubtaskTitle(String subtaskTitle) { this.subtaskTitle = subtaskTitle; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
