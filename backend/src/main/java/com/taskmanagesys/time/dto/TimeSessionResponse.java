package com.taskmanagesys.time.dto;

import com.taskmanagesys.time.model.TimeSession;
import com.taskmanagesys.time.model.TimeSessionState;
import java.time.Instant;

public record TimeSessionResponse(Long id, Long taskId, Long subtaskId, String taskTitle, Instant startedAt, Instant endedAt,
                                  Instant lastHeartbeatAt, String source, String endReason, String note, String startNote, long durationSeconds, TimeSessionState state, long workedSeconds, boolean overtimeConfirmed) {
    public static TimeSessionResponse from(TimeSession session, Instant now) {
        Instant end = session.getEndedAt() == null ? now : session.getEndedAt();
        long seconds = session.getWorkedSeconds();
        if (session.getState() == TimeSessionState.RUNNING && session.getActiveStartedAt() != null) seconds += Math.max(0, end.getEpochSecond() - session.getActiveStartedAt().getEpochSecond());
        return new TimeSessionResponse(session.getId(), session.getTaskId(), session.getSubtaskId(), session.getTaskTitle(), session.getStartedAt(), session.getEndedAt(), session.getLastHeartbeatAt(), session.getSource().name(), session.getEndReason(), session.getNote(), session.getStartNote(), seconds, session.getState(), seconds, session.isOvertimeConfirmed());
    }
}
