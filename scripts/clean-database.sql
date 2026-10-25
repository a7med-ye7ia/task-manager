-- Dry-run database cleanup for PostgreSQL.
-- Usage (dry run): psql "$DATABASE_URL" -f scripts/clean-database.sql
-- Usage (destructive, explicit): psql -v cleanup_confirm=true -v allow_prod_cleanup=true "$DATABASE_URL" -f scripts/clean-database.sql
-- The users table is never touched. Uploaded avatar files are never touched by this SQL.

\set ON_ERROR_STOP on
\if :{?cleanup_confirm}
\else
\set cleanup_confirm false
\endif
\if :{?allow_prod_cleanup}
\else
\set allow_prod_cleanup false
\endif

BEGIN;

SELECT (current_database() ~* '(prod|production)'
        OR coalesce(current_setting('app.active_profile', true), '') ~* 'prod') AS looks_prod \gset
\if :looks_prod
    \if :allow_prod_cleanup
    \else
        \echo 'Refusing cleanup for production-looking database/profile; pass -v allow_prod_cleanup=true only after a backup'
        ROLLBACK
        \quit 2
    \endif
\endif

CREATE TEMP TABLE cleanup_plan(table_name text, action text, row_count bigint) ON COMMIT DROP;

INSERT INTO cleanup_plan
SELECT 'comment_mentions', 'clear', count(*) FROM comment_mentions
UNION ALL SELECT 'task_read_state', 'clear', count(*) FROM task_read_state
UNION ALL SELECT 'notifications', 'clear', count(*) FROM notifications
UNION ALL SELECT 'audit_log', 'clear', count(*) FROM audit_log
UNION ALL SELECT 'email_tokens', 'clear', count(*) FROM email_tokens
UNION ALL SELECT 'refresh_tokens', 'clear', count(*) FROM refresh_tokens
UNION ALL SELECT 'email_outbox', 'clear', count(*) FROM email_outbox
UNION ALL SELECT 'comments', 'clear', count(*) FROM comments
UNION ALL SELECT 'checkpoints', 'clear', count(*) FROM checkpoints
UNION ALL SELECT 'subtask_assignees', 'clear', count(*) FROM subtask_assignees
UNION ALL SELECT 'subtasks', 'clear', count(*) FROM subtasks
UNION ALL SELECT 'task_members', 'clear', count(*) FROM task_members
UNION ALL SELECT 'task_activity', 'clear', count(*) FROM task_activity
UNION ALL SELECT 'tasks', 'clear', count(*) FROM tasks
UNION ALL SELECT 'users', 'KEEP', count(*) FROM users
UNION ALL SELECT 'manager_email_prefs', 'KEEP', count(*) FROM manager_email_prefs
UNION ALL SELECT 'flyway_schema_history', 'KEEP', count(*) FROM flyway_schema_history;

SELECT table_name, action, row_count FROM cleanup_plan ORDER BY action DESC, table_name;

\if :cleanup_confirm
    DELETE FROM comment_mentions;
    DELETE FROM task_read_state;
    DELETE FROM notifications;
    DELETE FROM audit_log;
    DELETE FROM email_tokens;
    DELETE FROM refresh_tokens;
    DELETE FROM email_outbox;
    DELETE FROM comments;
    DELETE FROM checkpoints;
    DELETE FROM subtask_assignees;
    DELETE FROM subtasks;
    DELETE FROM task_members;
    DELETE FROM task_activity;
    DELETE FROM tasks;

    ALTER SEQUENCE comment_mentions_id_seq RESTART WITH 1;
    ALTER SEQUENCE audit_log_id_seq RESTART WITH 1;
    ALTER SEQUENCE email_tokens_id_seq RESTART WITH 1;
    ALTER SEQUENCE refresh_tokens_id_seq RESTART WITH 1;
    ALTER SEQUENCE email_outbox_id_seq RESTART WITH 1;
    ALTER SEQUENCE comments_id_seq RESTART WITH 1;
    ALTER SEQUENCE checkpoints_id_seq RESTART WITH 1;
    ALTER SEQUENCE subtasks_id_seq RESTART WITH 1;
    ALTER SEQUENCE task_activity_id_seq RESTART WITH 1;
    ALTER SEQUENCE tasks_id_seq RESTART WITH 1;
    \echo 'Cleanup committed; users, manager_email_prefs, avatar paths and avatar files were preserved.'
    COMMIT;
\else
    \echo 'Dry run only; no rows were deleted.'
    ROLLBACK;
\endif
