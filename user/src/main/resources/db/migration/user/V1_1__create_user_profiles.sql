CREATE SCHEMA IF NOT EXISTS users;

CREATE TABLE users.profiles
(
    id         UUID PRIMARY KEY,

    account_id UUID                     NOT NULL,

    avatar_url VARCHAR(2048),

    gender     VARCHAR(32),

    birth_date DATE,

    first_name VARCHAR(100),

    last_name  VARCHAR(100),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    version    BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT uk_users_profiles_account_id UNIQUE (account_id),

    CONSTRAINT ck_user_profiles_gender
        CHECK (
            gender IS NULL
                OR gender IN (
                    'MALE',
                    'FEMALE',
                    'OTHER',
                    'PREFER_NOT_TO_SAY'
                )
            )
);