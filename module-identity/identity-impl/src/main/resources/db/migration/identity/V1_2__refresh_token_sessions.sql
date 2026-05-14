ALTER TABLE identity.refresh_tokens
    ADD COLUMN session_id UUID,
    ADD COLUMN token_family_id UUID,
    ADD COLUMN token_hash VARCHAR(64),
    ADD COLUMN last_used_at TIMESTAMPTZ,
    ADD COLUMN revoked_at TIMESTAMPTZ,
    ADD COLUMN revoke_reason VARCHAR(100);

UPDATE identity.refresh_tokens
SET session_id = id,
    token_family_id = id,
    token_hash = token,
    revoked = TRUE,
    revoked_at = NOW(),
    revoke_reason = 'LEGACY_PLAINTEXT_TOKEN_MIGRATED'
WHERE token_hash IS NULL;

ALTER TABLE identity.refresh_tokens
    ALTER COLUMN session_id SET NOT NULL,
ALTER COLUMN token_family_id SET NOT NULL,
    ALTER COLUMN token_hash SET NOT NULL;

ALTER TABLE identity.refresh_tokens
DROP CONSTRAINT IF EXISTS refresh_tokens_token_key;

DROP INDEX IF EXISTS identity.idx_refresh_tokens_token;

ALTER TABLE identity.refresh_tokens
DROP COLUMN token;

CREATE UNIQUE INDEX idx_refresh_tokens_token_hash
    ON identity.refresh_tokens(token_hash);

CREATE INDEX idx_refresh_tokens_session
    ON identity.refresh_tokens(session_id);

CREATE INDEX idx_refresh_tokens_user_revoked
    ON identity.refresh_tokens(user_id, revoked);
