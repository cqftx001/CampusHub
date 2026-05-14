CREATE SCHEMA IF NOT EXISTS identity;

-- 认证核心表（瘦表，登录热路径）
CREATE TABLE identity.users (
                                id              UUID PRIMARY KEY,
                                email           VARCHAR(320) NOT NULL UNIQUE,
                                username        VARCHAR(20) NOT NULL UNIQUE,
                                password_hash   VARCHAR(255),
                                display_name    VARCHAR(30) NOT NULL,
                                status          VARCHAR(20) NOT NULL,

    -- OAuth
                                oauth_provider  VARCHAR(20),
                                oauth_id        VARCHAR(255),

    -- Audit
                                created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                created_by      UUID,
                                updated_by      UUID
);

CREATE INDEX idx_users_email ON identity.users(email);
CREATE INDEX idx_users_username ON identity.users(username);
CREATE INDEX idx_users_status ON identity.users(status);

-- 个人资料表（胖表，低频访问）
CREATE TABLE identity.user_profiles (
                                        id              UUID PRIMARY KEY,
                                        user_id         UUID NOT NULL UNIQUE,
                                        first_name      VARCHAR(50),
                                        last_name       VARCHAR(50),
                                        phone           VARCHAR(20),
                                        avatar_url      VARCHAR(500),
                                        bio             VARCHAR(500),

    -- School
                                        school_id       VARCHAR(50),
                                        major           VARCHAR(100),
                                        graduation_year VARCHAR(4),

    -- Address
                                        address_line1   VARCHAR(200),
                                        address_line2   VARCHAR(200),
                                        city            VARCHAR(100),
                                        state           VARCHAR(100),
                                        zip_code        VARCHAR(20),
                                        country         VARCHAR(100),

    -- Contact
                                        preferred_contact_method VARCHAR(20),

    -- Audit
                                        created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_profiles_user ON identity.user_profiles(user_id);

-- Refresh Token
CREATE TABLE identity.refresh_tokens (
                                         id              UUID PRIMARY KEY,
                                         user_id         UUID NOT NULL,
                                         token           VARCHAR(255) NOT NULL UNIQUE,
                                         expires_at      TIMESTAMPTZ NOT NULL,
                                         revoked         BOOLEAN NOT NULL DEFAULT FALSE,
                                         created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                         updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON identity.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON identity.refresh_tokens(token);