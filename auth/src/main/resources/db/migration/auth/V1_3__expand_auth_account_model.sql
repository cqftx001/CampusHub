ALTER TABLE auth.accounts
    ADD COLUMN phone_number VARCHAR(16),
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN phone_verified_at TIMESTAMP WITH TIME ZONE,
ALTER COLUMN email DROP NOT NULL;

ALTER TABLE auth.accounts
    ADD CONSTRAINT uk_auth_accounts_phone_number
        UNIQUE (phone_number);

ALTER TABLE auth.accounts
    ADD CONSTRAINT ck_auth_accounts_email_verification
        CHECK (
            email_verified_at IS NULL
                OR email IS NOT NULL
            );

ALTER TABLE auth.accounts
    ADD CONSTRAINT ck_auth_accounts_phone_verification
        CHECK (
            phone_verified_at IS NULL
                OR phone_number IS NOT NULL
            );

CREATE TABLE auth.password_credentials
(
    account_id UUID PRIMARY KEY,

    password_hash VARCHAR(60) NOT NULL,

    password_changed_at TIMESTAMP WITH TIME ZONE NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_auth_password_credentials_account
        FOREIGN KEY (account_id)
            REFERENCES auth.accounts (id)
            ON DELETE RESTRICT
);

INSERT INTO auth.password_credentials (
    account_id,
    password_hash,
    password_changed_at,
    version
)
SELECT
    id,
    password_hash,
    updated_at,
    0
FROM auth.accounts;

ALTER TABLE auth.accounts
DROP COLUMN password_hash;