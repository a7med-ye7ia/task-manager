package com.taskmanagesys.email.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMailService implements MailService {
    private static final Logger log = LoggerFactory.getLogger(NoOpMailService.class);

    @Override
    public void send(Message message) {
        log.info("Mail delivery disabled; skipped template message to recipient hash={}", Integer.toHexString(message.to().hashCode()));
    }
}
