package com.taskmanagesys.history.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "work_history_events")
public class WorkHistoryEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "history_id", nullable = false) private Long historyId;
    @Column(name = "event_type", nullable = false, length = 50) private String eventType;
    @Column(name = "subtask_title", length = 150) private String subtaskTitle;
    @Column(name = "event_count") private Integer eventCount;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;

    public WorkHistoryEvent() {}
    public WorkHistoryEvent(Long historyId, String eventType, String subtaskTitle, Integer eventCount, Instant occurredAt) {
        this.historyId = historyId; this.eventType = eventType; this.subtaskTitle = subtaskTitle; this.eventCount = eventCount; this.occurredAt = occurredAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getHistoryId() { return historyId; }
    public void setHistoryId(Long historyId) { this.historyId = historyId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSubtaskTitle() { return subtaskTitle; }
    public void setSubtaskTitle(String subtaskTitle) { this.subtaskTitle = subtaskTitle; }
    public Integer getEventCount() { return eventCount; }
    public void setEventCount(Integer eventCount) { this.eventCount = eventCount; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
