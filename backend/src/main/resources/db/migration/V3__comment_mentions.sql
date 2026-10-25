CREATE TABLE comment_mentions (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (comment_id, user_id)
);
CREATE INDEX idx_comment_mentions_user ON comment_mentions (user_id);
