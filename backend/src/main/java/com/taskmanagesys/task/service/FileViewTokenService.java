package com.taskmanagesys.task.service;

import com.taskmanagesys.common.error.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Service
public class FileViewTokenService {
    private final String secret;
    private final Clock clock;
    public FileViewTokenService(@Value("${app.file-view-secret:}") String secret, Clock clock) { this.secret = secret; this.clock = clock; }
    public String issue(long userId, long taskId, long fileId) {
        if (secret.length() < 32) throw new IllegalStateException("FILE_VIEW_SECRET must be configured");
        String body = userId + "." + taskId + "." + fileId + "." + Instant.now(clock).plusSeconds(60).getEpochSecond();
        try { return enc(body) + "." + sign(body); } catch (Exception e) { throw new IllegalStateException("Could not sign file view token", e); }
    }
    public long verify(String token, long taskId, long fileId) {
        try {
            String[] parts = token.split("\\.", 2);
            String body = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] fields = body.split("\\.");
            if (parts.length != 2 || fields.length != 4 || !java.security.MessageDigest.isEqual(parts[1].getBytes(StandardCharsets.US_ASCII), sign(body).getBytes(StandardCharsets.US_ASCII)) || Long.parseLong(fields[1]) != taskId || Long.parseLong(fields[2]) != fileId || Instant.now(clock).getEpochSecond() > Long.parseLong(fields[3])) throw new IllegalArgumentException();
            return Long.parseLong(fields[0]);
        } catch (Exception ignored) { throw new AppException(HttpStatus.FORBIDDEN, "INVALID_FILE_VIEW", "The file view link is invalid or expired"); }
    }
    private String enc(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private String sign(String value) throws Exception { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8))); }
}
