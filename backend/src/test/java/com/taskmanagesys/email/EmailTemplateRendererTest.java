package com.taskmanagesys.email;

import com.taskmanagesys.email.service.EmailTemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplateRendererTest {

    private EmailTemplateRenderer renderer;
    private static final String BASE_URL = "http://localhost:5173";

    @BeforeEach
    void setUp() {
        renderer = new EmailTemplateRenderer(BASE_URL);
    }

    @Test
    @DisplayName("Unknown template throws IllegalArgumentException (fails loudly)")
    void unknownTemplateFailsLoudly() {
        assertThrows(IllegalArgumentException.class, () -> renderer.render("NON_EXISTENT_TEMPLATE", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> renderer.render(null, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> renderer.render("", Map.of()));
    }

    @Test
    @DisplayName("All section 9 email templates render correctly without emojis and with proper links, escaping, and plain-text")
    void allTemplatesRenderSuccessfully() {
        String[] templates = {
                "ACCOUNT_INVITE",
                "VERIFY_EMAIL",
                "CONFIRM_NEW_EMAIL",
                "NEW_SIGNUP_REQUEST",
                "SIGNUP_APPROVED",
                "SIGNUP_REJECTED",
                "PASSWORD_RESET",
                "PASSWORD_CHANGED",
                "ACCOUNT_ALREADY_EXISTS",
                "task-assigned",
                "subtask-assigned",
                "subtask-done",
                "task-review",
                "task-done",
                "task-sent-back",
                "task-restored",
                "archive-warning",
                "lead-vacant",
                "mention",
                "self-reported-created",
                "self-reported-review"
        };

        Map<String, Object> payload = Map.ofEntries(
                Map.entry("token", "secret-token-123"),
                Map.entry("taskId", 42L),
                Map.entry("title", "Sample <script>alert('xss')</script> Task"),
                Map.entry("taskTitle", "Sample <script>alert('xss')</script> Task"),
                Map.entry("subtaskTitle", "Subtask <bold>One</bold>"),
                Map.entry("name", "Applicant <User>"),
                Map.entry("email", "applicant@gmail.com"),
                Map.entry("reason", "Missing <requirements> and tests"),
                Map.entry("actorName", "Manager <Bob>"),
                Map.entry("creatorName", "Employee <Alice>"),
                Map.entry("priority", "HIGH"),
                Map.entry("deadline", "2026-12-31")
        );

        for (String template : templates) {
            EmailTemplateRenderer.RenderedEmail email = renderer.render(template, payload);

            assertNotNull(email.subject(), "Subject must not be null for template: " + template);
            assertFalse(email.subject().isBlank(), "Subject must not be blank for template: " + template);

            assertNotNull(email.html(), "HTML must not be null for template: " + template);
            assertFalse(email.html().isBlank(), "HTML must not be blank for template: " + template);

            assertNotNull(email.plainText(), "Plain text must not be null for template: " + template);
            assertFalse(email.plainText().isBlank(), "Plain text must not be blank for template: " + template);

            // Escaping check: unescaped dangerous HTML should never appear raw
            assertFalse(email.html().contains("<script>"), "HTML must escape script tags for template: " + template);
            assertTrue(email.html().contains("&lt;script&gt;") || !email.html().contains("alert('xss')"), "HTML must escape user inputs");

            // No emojis rule
            assertFalse(email.html().matches(".*[\\x{1F600}-\\x{1F64F}|\\x{1F300}-\\x{1F5FF}|\\x{1F680}-\\x{1F6FF}|\\x{2600}-\\x{26FF}|\\x{2700}-\\x{27BF}].*"),
                    "HTML must not contain emojis for template: " + template);
            assertFalse(email.plainText().matches(".*[\\x{1F600}-\\x{1F64F}|\\x{1F300}-\\x{1F5FF}|\\x{1F680}-\\x{1F6FF}|\\x{2600}-\\x{26FF}|\\x{2700}-\\x{27BF}].*"),
                    "Plain text must not contain emojis for template: " + template);

            // Must include APP_BASE_URL link in html
            assertTrue(email.html().contains(BASE_URL), "HTML must contain app link for template: " + template);
            assertTrue(email.plainText().contains(BASE_URL), "Plain text must contain app link for template: " + template);
        }
    }

    @Test
    @DisplayName("Task assigned template checks")
    void testTaskAssignedTemplate() {
        var email = renderer.render("task-assigned", Map.of(
                "taskId", 101L,
                "title", "Deploy release",
                "actorName", "Alice Manager",
                "priority", "HIGH",
                "deadline", "2026-11-20"
        ));

        assertEquals("New task assigned: Deploy release", email.subject());
        assertTrue(email.html().contains("Alice Manager added you to the task"));
        assertTrue(email.html().contains("Deploy release"));
        assertTrue(email.html().contains("HIGH"));
        assertTrue(email.html().contains("2026-11-20"));
        assertTrue(email.html().contains("http://localhost:5173/tasks/101"));
        assertTrue(email.plainText().contains("View Task: http://localhost:5173/tasks/101"));
    }

    @Test
    @DisplayName("Task sent back template checks with reason")
    void testTaskSentBackTemplate() {
        var email = renderer.render("task-sent-back", Map.of(
                "taskId", 102L,
                "title", "Backend API",
                "actorName", "Bob Manager",
                "reason", "Missing validation tests"
        ));

        assertEquals("Task sent back: Backend API", email.subject());
        assertTrue(email.html().contains("Reason for send back"));
        assertTrue(email.html().contains("Missing validation tests"));
        assertTrue(email.html().contains("http://localhost:5173/tasks/102"));
    }
}
