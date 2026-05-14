CREATE SCHEMA IF NOT EXISTS messaging;

CREATE TABLE messaging.messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
