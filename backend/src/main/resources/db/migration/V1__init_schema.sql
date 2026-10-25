-- ==============================================================================
-- Flyway Migration V1: Initial Database Schema (Section 4 of SRS)
-- ==============================================================================

-- 1. USERS
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    phone VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255),
    role VARCHAR(20) NOT NULL CHECK (role IN ('MANAGER', 'EMPLOYEE')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('INVITED', 'PENDING_EMAIL', 'PENDING_APPROVAL', 'ACTIVE', 'REJECTED', 'DISABLED')),
    avatar_path VARCHAR(255),
    hired_at DATE,
    deactivated_at DATE,
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. MANAGER_EMAIL_PREFS
CREATE TABLE manager_email_prefs (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    emails_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    on_signup_request BOOLEAN NOT NULL DEFAULT TRUE,
    on_task_completed BOOLEAN NOT NULL DEFAULT FALSE,
    on_archive_warning BOOLEAN NOT NULL DEFAULT FALSE
);

-- 3. EMAIL_TOKENS
CREATE TABLE email_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL CHECK (type IN ('VERIFY_EMAIL', 'INVITE', 'RESET_PASSWORD', 'CHANGE_EMAIL')),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    new_email VARCHAR(254),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);

-- 4. REFRESH_TOKENS
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

-- 5. TASKS
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    deadline DATE,
    deadline_manual BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    status VARCHAR(20) NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'REVIEW', 'DONE')),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    done_at TIMESTAMPTZ,
    purge_at TIMESTAMPTZ,
    purge_warning_sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. TASK_MEMBERS
CREATE TABLE task_members (
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(10) NOT NULL CHECK (role IN ('LEAD', 'MEMBER')),
    added_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, user_id)
);

CREATE UNIQUE INDEX uq_task_members_lead ON task_members (task_id) WHERE role = 'LEAD';

-- 7. SUBTASKS
CREATE TABLE subtasks (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    deadline DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    done_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. SUBTASK_ASSIGNEES
CREATE TABLE subtask_assignees (
    subtask_id BIGINT NOT NULL REFERENCES subtasks(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (subtask_id, user_id)
);

-- 9. CHECKPOINTS
CREATE TABLE checkpoints (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    subtask_id BIGINT REFERENCES subtasks(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    is_done BOOLEAN NOT NULL DEFAULT FALSE,
    position INT NOT NULL DEFAULT 0,
    done_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    done_at TIMESTAMPTZ
);

-- 10. COMMENTS
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    author_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    subtask_id BIGINT REFERENCES subtasks(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. TASK_ACTIVITY
CREATE TABLE task_activity (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. AUDIT_LOG
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    target_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 13. NOTIFICATIONS
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    task_id BIGINT REFERENCES tasks(id) ON DELETE CASCADE,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 14. EMAIL_OUTBOX
CREATE TABLE email_outbox (
    id BIGSERIAL PRIMARY KEY,
    to_email VARCHAR(254) NOT NULL,
    template VARCHAR(100) NOT NULL,
    payload JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Required Indexes (Section 4 of SRS):
CREATE INDEX idx_tasks_status_deadline ON tasks (status, deadline);
CREATE INDEX idx_tasks_purge_at ON tasks (purge_at);
CREATE INDEX idx_task_members_user_id ON task_members (user_id);
CREATE INDEX idx_subtask_assignees_user_id ON subtask_assignees (user_id);
CREATE INDEX idx_comments_task_created ON comments (task_id, created_at);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read, created_at);
CREATE INDEX idx_email_outbox_status_next ON email_outbox (status, next_attempt_at);
