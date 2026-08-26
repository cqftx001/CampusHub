CREATE TABLE auth.login_sessions
(
    id UUID PRIMARY KEY,

    account_id UUID NOT NULL,

    status VARCHAR(16) NOT NULL,

    last_used_at TIMESTAMP WITH TIME ZONE NOT NULL,

    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

    revoked_at TIMESTAMP WITH TIME ZONE,

    user_agent VARCHAR(512),

    ip_address VARCHAR(45),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_auth_login_sessions_account
        FOREIGN KEY (account_id)
            REFERENCES auth.accounts (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_auth_login_sessions_status
        CHECK (
            status IN (
                       'ACTIVE',
                       'REVOKED'
                )
            ),

    CONSTRAINT ck_auth_login_sessions_revocation
        CHECK (
            (
                status = 'ACTIVE'
                    AND revoked_at IS NULL
                )
                OR
            (
                status = 'REVOKED'
                    AND revoked_at IS NOT NULL
                )
            )
);

CREATE INDEX ix_auth_login_sessions_account_id
    ON auth.login_sessions (account_id);

CREATE INDEX ix_auth_login_sessions_status_expires_at
    ON auth.login_sessions (
                            status,
                            expires_at
        );

CREATE TABLE auth.refresh_tokens
(
    id UUID PRIMARY KEY,

    session_id UUID NOT NULL,

    token_hash VARCHAR(64) NOT NULL,

    status VARCHAR(16) NOT NULL,

    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

    used_at TIMESTAMP WITH TIME ZONE,

    revoked_at TIMESTAMP WITH TIME ZONE,

    replaced_by_token_id UUID,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_auth_refresh_tokens_token_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_auth_refresh_tokens_session
        FOREIGN KEY (session_id)
            REFERENCES auth.login_sessions (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_auth_refresh_tokens_replacement
        FOREIGN KEY (replaced_by_token_id)
            REFERENCES auth.refresh_tokens (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_auth_refresh_tokens_status
        CHECK (
            status IN (
                       'ACTIVE',
                       'USED',
                       'REVOKED'
                )
            ),

    CONSTRAINT ck_auth_refresh_tokens_state
        CHECK (
            (
                status = 'ACTIVE'
                    AND used_at IS NULL
                    AND revoked_at IS NULL
                )
                OR
            (
                status = 'USED'
                    AND used_at IS NOT NULL
                    AND revoked_at IS NULL
                )
                OR
            (
                status = 'REVOKED'
                    AND used_at IS NULL
                    AND revoked_at IS NOT NULL
                )
            )
);

CREATE INDEX ix_auth_refresh_tokens_session_status
    ON auth.refresh_tokens (
                            session_id,
                            status
        );

CREATE INDEX ix_auth_refresh_tokens_expires_at
    ON auth.refresh_tokens (expires_at);