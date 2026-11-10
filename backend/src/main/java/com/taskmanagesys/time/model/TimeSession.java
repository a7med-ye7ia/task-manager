package com.taskmanagesys.time.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "time_sessions", indexes = {
        @Index(name = "idx_time_sessions_user_dates", columnList = "user_id,started_at,ended_at")
})
public class TimeSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "task_id") private Long taskId;
    @Column(name = "subtask_id") private Long subtaskId;
    @Column(name = "task_title", nullable = false, length = 150) private String taskTitle;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "ended_at") private Instant endedAt;
    @Column(name = "last_heartbeat_at") private Instant lastHeartbeatAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private TimeSource source;
    @Column(name = "end_reason", length = 20) private String endReason;
    @Column(length = 200) private String note;
    @Column(name = "start_note", length = 200) private String startNote;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "last_edited_by") private Long lastEditedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private TimeSessionState state = TimeSessionState.RUNNING;
    @Column(name = "worked_seconds", nullable = false) private long workedSeconds;
    @Column(name = "active_started_at") private Instant activeStartedAt;
    @Column(name = "overtime_confirmed", nullable = false) private boolean overtimeConfirmed;

    public TimeSession() {}
    public Long getId() { return id; } public void setId(Long v) { id=v; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { userId=v; }
    public Long getTaskId() { return taskId; } public void setTaskId(Long v) { taskId=v; }
    public Long getSubtaskId() { return subtaskId; } public void setSubtaskId(Long v) { subtaskId=v; }
    public String getTaskTitle() { return taskTitle; } public void setTaskTitle(String v) { taskTitle=v; }
    public Instant getStartedAt() { return startedAt; } public void setStartedAt(Instant v) { startedAt=v; }
    public Instant getEndedAt() { return endedAt; } public void setEndedAt(Instant v) { endedAt=v; }
    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; } public void setLastHeartbeatAt(Instant v) { lastHeartbeatAt=v; }
    public TimeSource getSource() { return source; } public void setSource(TimeSource v) { source=v; }
    public String getEndReason() { return endReason; } public void setEndReason(String v) { endReason=v; }
    public String getNote() { return note; } public void setNote(String v) { note=v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt=v; }
    public String getStartNote(){return startNote;} public void setStartNote(String v){startNote=v;}
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt=v; }
    public Long getLastEditedBy() { return lastEditedBy; } public void setLastEditedBy(Long v) { lastEditedBy=v; }
    public TimeSessionState getState(){return state;} public void setState(TimeSessionState v){state=v;}
    public long getWorkedSeconds(){return workedSeconds;} public void setWorkedSeconds(long v){workedSeconds=v;}
    public Instant getActiveStartedAt(){return activeStartedAt;} public void setActiveStartedAt(Instant v){activeStartedAt=v;}
    public boolean isOvertimeConfirmed(){return overtimeConfirmed;} public void setOvertimeConfirmed(boolean v){overtimeConfirmed=v;}
}
