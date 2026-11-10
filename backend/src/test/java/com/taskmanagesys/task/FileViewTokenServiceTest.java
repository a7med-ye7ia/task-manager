package com.taskmanagesys.task;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.service.FileViewTokenService;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class FileViewTokenServiceTest {
    private final Instant now = Instant.parse("2026-11-05T10:00:00Z");
    private FileViewTokenService service(Clock clock) { return new FileViewTokenService("test-file-view-secret-that-is-at-least-32-bytes", clock); }
    @Test void validTokenReturnsBoundUser() { String token=service(Clock.fixed(now,ZoneOffset.UTC)).issue(2,10,4); assertEquals(2,service(Clock.fixed(now,ZoneOffset.UTC)).verify(token,10,4)); }
    @Test void tamperingExpiryAndDifferentFileFail() { FileViewTokenService s=service(Clock.fixed(now,ZoneOffset.UTC)); String token=s.issue(2,10,4); assertThrows(AppException.class,()->s.verify(token+"x",10,4)); assertThrows(AppException.class,()->s.verify(token,10,5)); assertThrows(AppException.class,()->service(Clock.fixed(now.plusSeconds(61),ZoneOffset.UTC)).verify(token,10,4)); }
    @Test void anotherUserTokenRemainsBoundToOriginalUser() { FileViewTokenService s=service(Clock.fixed(now,ZoneOffset.UTC)); assertEquals(7,s.verify(s.issue(7,10,4),10,4)); }
}
