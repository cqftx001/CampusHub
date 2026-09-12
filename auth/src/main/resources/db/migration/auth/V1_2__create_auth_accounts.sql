CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE auth.accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(32) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_auth_accounts_username UNIQUE (username),
    CONSTRAINT uk_auth_accounts_email UNIQUE (email)
);
