package com.taskmanagesys.time.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Instant;

@Component
@ConditionalOnProperty(name="app.jobs.enabled", havingValue="true", matchIfMissing=true)
public class TimeRecoveryJob {
    private final TimeSessionService sessions; private final Clock clock;
    public TimeRecoveryJob(TimeSessionService sessions, Clock clock){this.sessions=sessions;this.clock=clock;}
    @PostConstruct public void recoverOnStartup(){sessions.recoverStale(Instant.now(clock)); sessions.enforceOpenLimits(Instant.now(clock));}
    @Scheduled(fixedDelayString="${app.time.recovery-delay-ms:60000}") public void recover(){Instant now=Instant.now(clock); sessions.recoverStale(now); sessions.enforceOpenLimits(now);}
}
