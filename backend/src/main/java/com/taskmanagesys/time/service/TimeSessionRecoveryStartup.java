package com.taskmanagesys.time.service;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class TimeSessionRecoveryStartup {
    private final TimeSessionService service; private final Clock clock;
    public TimeSessionRecoveryStartup(TimeSessionService service, Clock clock){this.service=service;this.clock=clock;}
    @PostConstruct void recover(){service.recoverStale(Instant.now(clock));}
}
