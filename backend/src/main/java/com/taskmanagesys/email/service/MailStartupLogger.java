package com.taskmanagesys.email.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MailStartupLogger {
    private static final Logger log = LoggerFactory.getLogger(MailStartupLogger.class);

    @Value("${spring.mail.host:localhost}") private String host;
    @Value("${spring.mail.port:1025}") private int port;
    @Value("${spring.mail.username:}") private String username;
    @Value("${app.mail.enabled:false}") private boolean enabled;
    @Value("${app.base-url:http://localhost:5173}") private String appBaseUrl;

    @PostConstruct
    void logConfiguration() {
        log.info("SMTP mail: host={} port={} username={} enabled={} appBaseUrl={}", host, port, username, enabled, appBaseUrl);
    }
}
