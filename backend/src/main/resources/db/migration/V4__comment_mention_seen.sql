ALTER TABLE comment_mentions ADD COLUMN seen_at TIMESTAMPTZ;
CREATE INDEX idx_comment_mentions_unseen ON comment_mentions (user_id, seen_at);
