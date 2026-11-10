-- V12: Performance indexes for task pool, reports and time tracking

CREATE INDEX IF NOT EXISTS idx_tasks_status_priority_deadline
    ON tasks (status, priority, deadline);

CREATE INDEX IF NOT EXISTS idx_tasks_self_reported
    ON tasks (self_reported);

CREATE INDEX IF NOT EXISTS idx_tasks_done_at_purge_at
    ON tasks (done_at, purge_at);

CREATE INDEX IF NOT EXISTS idx_task_members_user_role
    ON task_members (user_id, role);

CREATE INDEX IF NOT EXISTS idx_time_sessions_task_user
    ON time_sessions (task_id, user_id);

CREATE INDEX IF NOT EXISTS idx_work_history_user_task
    ON work_history (user_id, task_id);
