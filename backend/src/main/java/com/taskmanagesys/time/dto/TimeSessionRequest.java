package com.taskmanagesys.time.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class TimeSessionRequest {
    @NotNull private Long taskId;
    private Long subtaskId;
    private Instant startedAt;
    private Instant endedAt;
    @Size(min = 3, max = 200) private String note;
    private boolean confirmSwitch;
    private boolean confirmOvertime;
    private String startNote;
    public Long getTaskId() { return taskId; } public void setTaskId(Long v) { taskId=v; }
    public Long getSubtaskId() { return subtaskId; } public void setSubtaskId(Long v) { subtaskId=v; }
    public Instant getStartedAt() { return startedAt; } public void setStartedAt(Instant v) { startedAt=v; }
    public Instant getEndedAt() { return endedAt; } public void setEndedAt(Instant v) { endedAt=v; }
    public String getNote() { return note; } public void setNote(String v) { note=v; }
    public boolean isConfirmSwitch(){return confirmSwitch;} public void setConfirmSwitch(boolean v){confirmSwitch=v;}
    public boolean isConfirmOvertime(){return confirmOvertime;} public void setConfirmOvertime(boolean v){confirmOvertime=v;}
    public String getStartNote(){return startNote;} public void setStartNote(String v){startNote=v;}
}
