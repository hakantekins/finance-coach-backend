-- Feedback table (user suggestions / bug reports)
-- Flyway çalıştırma: Render ortamında şema otomatik oluşsun.

CREATE TABLE IF NOT EXISTS feedback (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_feedback_user_id
    ON feedback (user_id);

