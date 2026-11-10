-- Phase 8: durable work history and time tracking.
ALTER TABLE manager_email_prefs
    ADD COLUMN IF NOT EXISTS on_self_reported BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS self_reported BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS work_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_id BIGINT,
    task_title VARCHAR(150) NOT NULL,
    priority VARCHAR(10),
    deadline DATE,
    task_role VARCHAR(10),
    joined_at TIMESTAMPTZ NOT NULL,
    left_at TIMESTAMPTZ,
    self_reported BOOLEAN NOT NULL DEFAULT FALSE,
    final_status VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS work_history_events (
    id BIGSERIAL PRIMARY KEY,
    history_id BIGINT NOT NULL REFERENCES work_history(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    subtask_title VARCHAR(150),
    event_count INTEGER,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS time_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_id BIGINT,
    subtask_id BIGINT,
    task_title VARCHAR(150) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    last_heartbeat_at TIMESTAMPTZ,
    source VARCHAR(10) NOT NULL CHECK (source IN ('TIMER', 'MANUAL')),
    end_reason VARCHAR(20),
    note VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edited_by BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_work_history_user_dates ON work_history(user_id, joined_at, left_at);
CREATE INDEX IF NOT EXISTS idx_work_history_task ON work_history(task_id);
CREATE INDEX IF NOT EXISTS idx_work_history_events_history_date ON work_history_events(history_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_time_sessions_user_dates ON time_sessions(user_id, started_at, ended_at);
CREATE INDEX IF NOT EXISTS idx_time_sessions_task ON time_sessions(task_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_time_sessions_running_user ON time_sessions(user_id) WHERE ended_at IS NULL;

INSERT INTO work_history (user_id, task_id, task_title, priority, deadline, task_role, joined_at, self_reported, final_status)
SELECT tm.user_id, t.id, t.title, t.priority, t.deadline, tm.role, t.created_at, COALESCE(t.self_reported, FALSE), t.status
FROM task_members tm
JOIN tasks t ON t.id = tm.task_id
WHERE NOT EXISTS (
    SELECT 1 FROM work_history h WHERE h.user_id = tm.user_id AND h.task_id = t.id
);
