CREATE TABLE task_read_state (
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    chat_last_id BIGINT NOT NULL DEFAULT 0,
    activity_last_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (task_id, user_id)
);
