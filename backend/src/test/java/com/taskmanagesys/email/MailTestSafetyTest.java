package com.taskmanagesys.email;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
class MailTestSafetyTest {
    @Value("${spring.mail.host}") private String host;
    @Value("${app.mail.enabled}") private boolean mailEnabled;
    @Value("${app.email.sender.enabled}") private boolean senderEnabled;

    @Test
    void testProfileCannotSendRealMail() {
        assertEquals("localhost", host);
        assertFalse(mailEnabled);
        assertFalse(senderEnabled);
    }
}
