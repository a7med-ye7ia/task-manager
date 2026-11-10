ALTER TABLE time_sessions ADD COLUMN IF NOT EXISTS state VARCHAR(10) NOT NULL DEFAULT 'RUNNING';
ALTER TABLE time_sessions ADD COLUMN IF NOT EXISTS worked_seconds BIGINT NOT NULL DEFAULT 0;
ALTER TABLE time_sessions ADD COLUMN IF NOT EXISTS active_started_at TIMESTAMPTZ;
ALTER TABLE time_sessions ADD COLUMN IF NOT EXISTS overtime_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE time_sessions SET state = CASE WHEN ended_at IS NULL THEN 'RUNNING' ELSE 'ENDED' END WHERE state IS NULL OR state = 'RUNNING';
CREATE TABLE IF NOT EXISTS time_segments (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES time_sessions(id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ NOT NULL,
    worked_seconds BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_time_segments_session_dates ON time_segments(session_id, started_at, ended_at);
