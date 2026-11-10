CREATE TABLE IF NOT EXISTS task_files (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    subtask_id BIGINT REFERENCES subtasks(id) ON DELETE CASCADE,
    uploader_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    stored_name VARCHAR(255) NOT NULL UNIQUE,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_task_files_task_subtask ON task_files(task_id, subtask_id);
CREATE INDEX IF NOT EXISTS idx_task_files_uploader ON task_files(uploader_id);
