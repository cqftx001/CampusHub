CREATE TABLE auth.roles
(
    id UUID PRIMARY KEY,

    code VARCHAR(32) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_auth_roles_code
        UNIQUE (code),

    CONSTRAINT ck_auth_roles_code
        CHECK (
            code IN (
                     'ADMIN',
                     'USER',
                     'MANAGER'
                )
            )
);

CREATE TABLE auth.account_roles
(
    account_id UUID NOT NULL,

    role_id UUID NOT NULL,

    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_auth_account_roles
        PRIMARY KEY (
                     account_id,
                     role_id
            ),

    CONSTRAINT fk_auth_account_roles_account
        FOREIGN KEY (account_id)
            REFERENCES auth.accounts (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_auth_account_roles_role
        FOREIGN KEY (role_id)
            REFERENCES auth.roles (id)
            ON DELETE RESTRICT
);

CREATE INDEX ix_auth_account_roles_role_id
    ON auth.account_roles (role_id);

INSERT INTO auth.roles (
    id,
    code,
    created_at,
    updated_at,
    version
)
VALUES
    (
        '00000000-0000-0000-0000-000000000001',
        'ADMIN',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        'USER',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        'MANAGER',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    );

INSERT INTO auth.account_roles (
    account_id,
    role_id,
    assigned_at,
    version
)
SELECT
    account.id,
    user_role.id,
    CURRENT_TIMESTAMP,
    0
FROM auth.accounts account
         CROSS JOIN auth.roles user_role
WHERE user_role.code = 'USER';