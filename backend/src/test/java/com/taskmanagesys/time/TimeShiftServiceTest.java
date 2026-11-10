package com.taskmanagesys.time;

import com.taskmanagesys.audit.service.AuditLogService;
import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.task.repository.SubtaskAssigneeRepository;
import com.taskmanagesys.task.repository.SubtaskRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.task.service.TaskAccessService;
import com.taskmanagesys.task.service.TaskActivityService;
import com.taskmanagesys.time.model.TimeSession;
import com.taskmanagesys.time.model.TimeSessionState;
import com.taskmanagesys.time.repository.TimeSessionRepository;
import com.taskmanagesys.time.service.TimeSessionService;
import com.taskmanagesys.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeShiftServiceTest {
    @Mock TimeSessionRepository sessions; @Mock TaskRepository tasks; @Mock SubtaskRepository subtasks; @Mock SubtaskAssigneeRepository assignees; @Mock TaskAccessService access; @Mock UserRepository users; @Mock AuditLogService audits; @Mock TaskActivityService activity;
    private final Instant now=Instant.parse("2026-11-04T12:00:00Z");
    private TimeSessionService service(){return new TimeSessionService(sessions,tasks,subtasks,assignees,access,users,Clock.fixed(now, ZoneOffset.UTC),audits,activity);}
    private TimeSession session(TimeSessionState state){TimeSession s=new TimeSession();s.setId(4L);s.setUserId(2L);s.setTaskId(10L);s.setTaskTitle("Task");s.setStartedAt(now.minusSeconds(600));s.setState(state);s.setSource(com.taskmanagesys.time.model.TimeSource.TIMER);s.setWorkedSeconds(120);s.setActiveStartedAt(state==TimeSessionState.RUNNING?now.minusSeconds(120):null);s.setLastHeartbeatAt(now);return s;}
    @Test void pauseKeepsSessionOpenAndResumeReopensClock(){TimeSession s=session(TimeSessionState.RUNNING);when(sessions.findById(4L)).thenReturn(Optional.of(s));when(sessions.save(any())).thenAnswer(i->i.getArgument(0));assertEquals(TimeSessionState.PAUSED,service().pause(2L,4L).state());assertNull(s.getEndedAt());assertEquals(TimeSessionState.RUNNING,service().resume(2L,4L).state());}
    @Test void stopFromPausedEndsPermanently(){TimeSession s=session(TimeSessionState.PAUSED);when(sessions.findById(4L)).thenReturn(Optional.of(s));when(sessions.save(any())).thenAnswer(i->i.getArgument(0));assertEquals(TimeSessionState.ENDED,service().stop(2L,4L).state());assertNotNull(s.getEndedAt());}
    @Test void tenHourDailyLimitRejectsAnotherStart(){Task task=new Task();task.setId(10L);task.setTitle("Task");task.setStatus(TaskStatus.IN_PROGRESS);when(tasks.findById(10L)).thenReturn(Optional.of(task));when(access.member(10L,2L)).thenReturn(true);when(sessions.findFirstByUserIdAndEndedAtIsNull(2L)).thenReturn(Optional.empty());TimeSession old=session(TimeSessionState.ENDED);old.setStartedAt(now.minusSeconds(10*3600));old.setEndedAt(now);old.setWorkedSeconds(10*3600);when(sessions.findByUserIdOrderByStartedAtDesc(2L)).thenReturn(List.of(old));AppException error=assertThrows(AppException.class,()->service().start(2L,10L,null));assertEquals("OVERTIME_LIMIT",error.getCode());}
}
