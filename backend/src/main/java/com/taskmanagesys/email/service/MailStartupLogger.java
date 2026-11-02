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
    @Value("${app.mail.from:}") private String sender;
    @Value("${app.mail.enabled:false}") private boolean enabled;

    @PostConstruct
    void logConfiguration() {
        log.info("SMTP configured: {}:{} sender={} enabled={}", host, port, sender, enabled);
    }
}
