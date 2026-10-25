package com.taskmanagesys;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class FlywaySchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testAll14TablesExistInPostgres() {
        List<String> requiredTables = List.of(
                "users",
                "manager_email_prefs",
                "email_tokens",
                "refresh_tokens",
                "tasks",
                "task_members",
                "subtasks",
                "subtask_assignees",
                "checkpoints",
                "comments",
                "task_activity",
                "audit_log",
                "notifications",
                "email_outbox"
        );

        List<String> existingTables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
        );

        for (String table : requiredTables) {
            assertTrue(existingTables.contains(table), "Required table missing in PostgreSQL: " + table);
        }
    }

    @Test
    void testPartialUniqueIndexOnTaskMembersLeadExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'task_members' AND indexname = 'uq_task_members_lead'",
                Integer.class
        );
        assertTrue(count != null && count > 0, "Partial unique index uq_task_members_lead should exist in PostgreSQL");
    }
}
