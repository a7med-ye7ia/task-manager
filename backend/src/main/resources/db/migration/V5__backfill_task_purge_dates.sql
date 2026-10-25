UPDATE tasks
SET purge_at = COALESCE(done_at, CURRENT_TIMESTAMP) + INTERVAL '30 days'
WHERE status = 'DONE' AND purge_at IS NULL;
