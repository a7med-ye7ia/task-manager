package com.taskmanagesys.auth.service;

import com.taskmanagesys.common.error.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class RateLimiterService {

    private static final long ONE_HOUR_SECONDS = 3600;
    private static final int SIGNUP_LIMIT_PER_IP = 10;
    private static final int FORGOT_PASSWORD_LIMIT_PER_IP = 5;
    private static final int FORGOT_PASSWORD_LIMIT_PER_EMAIL = 5;

    private final Map<String, Queue<Instant>> signupIpRequests = new ConcurrentHashMap<>();
    private final Map<String, Queue<Instant>> forgotPasswordIpRequests = new ConcurrentHashMap<>();
    private final Map<String, Queue<Instant>> forgotPasswordEmailRequests = new ConcurrentHashMap<>();

    public void checkSignUp(String ip) {
        if (!isAllowed(signupIpRequests, ip, SIGNUP_LIMIT_PER_IP)) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "Too many sign-up requests from this IP address. Maximum 10 per hour.");
        }
    }

    public void checkForgotPassword(String ip, String email) {
        if (!isAllowed(forgotPasswordIpRequests, ip, FORGOT_PASSWORD_LIMIT_PER_IP)) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "Too many password reset requests from this IP address. Maximum 5 per hour.");
        }
        if (email != null && !isAllowed(forgotPasswordEmailRequests, email.toLowerCase().trim(), FORGOT_PASSWORD_LIMIT_PER_EMAIL)) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "Too many password reset requests for this email address. Maximum 5 per hour.");
        }
    }

    private boolean isAllowed(Map<String, Queue<Instant>> map, String key, int limit) {
        if (key == null) return true;
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(ONE_HOUR_SECONDS);

        Queue<Instant> timestamps = map.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());
        synchronized (timestamps) {
            Iterator<Instant> it = timestamps.iterator();
            while (it.hasNext()) {
                if (it.next().isBefore(cutoff)) {
                    it.remove();
                } else {
                    break;
                }
            }
            if (timestamps.size() >= limit) {
                return false;
            }
            timestamps.add(now);
            return true;
        }
    }

    // For test resets if needed
    public void resetAll() {
        signupIpRequests.clear();
        forgotPasswordIpRequests.clear();
        forgotPasswordEmailRequests.clear();
    }
}
