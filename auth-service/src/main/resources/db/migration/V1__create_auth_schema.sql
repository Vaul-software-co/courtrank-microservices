CREATE TABLE authentications
(
    id                       UUID PRIMARY KEY,
    email                    VARCHAR(255) NOT NULL,
    password_hash            TEXT         NOT NULL,
    role                     VARCHAR(32)  NOT NULL,

    is_email_verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE,

    terms_version_accepted   VARCHAR(32),
    terms_accepted_at        TIMESTAMPTZ,
    data_consent_accepted_at TIMESTAMPTZ,

    deleted_at               TIMESTAMPTZ,
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,

    CONSTRAINT authentications_role_check
        CHECK (role IN ('MEMBER', 'ADMIN', 'SUPER_ADMIN'))
);

CREATE UNIQUE INDEX authentications_email_unique_idx
    ON authentications (LOWER(email));

CREATE INDEX authentications_deleted_at_idx
    ON authentications (deleted_at);


CREATE TABLE sessions
(
    id                 UUID PRIMARY KEY,
    user_id            UUID        NOT NULL,
    refresh_token_hash TEXT        NOT NULL,
    client             VARCHAR(64) NOT NULL,
    ip                 VARCHAR(64),
    user_agent         TEXT,

    replaced_by        UUID,
    status             VARCHAR(32) NOT NULL,

    revoked_at         TIMESTAMPTZ,
    expires_at         TIMESTAMPTZ NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL,

    CONSTRAINT sessions_user_id_fkey
        FOREIGN KEY (user_id)
            REFERENCES authentications (id),

    CONSTRAINT sessions_replaced_by_fkey
        FOREIGN KEY (replaced_by)
            REFERENCES sessions (id),

    CONSTRAINT sessions_status_check
        CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED', 'REPLACED'))
);

CREATE UNIQUE INDEX sessions_refresh_token_hash_unique_idx
    ON sessions (refresh_token_hash);

CREATE INDEX sessions_user_id_idx
    ON sessions (user_id);

CREATE INDEX sessions_user_id_status_idx
    ON sessions (user_id, status);

CREATE INDEX sessions_expires_at_idx
    ON sessions (expires_at);


CREATE TABLE verification_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    token_hash TEXT        NOT NULL,
    type       VARCHAR(64) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    attempts   INTEGER     NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT verification_tokens_user_id_fkey
        FOREIGN KEY (user_id)
            REFERENCES authentications (id),

    CONSTRAINT verification_tokens_type_check
        CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),

    CONSTRAINT verification_tokens_attempts_check
        CHECK (attempts >= 0)
);

CREATE UNIQUE INDEX verification_tokens_token_hash_unique_idx
    ON verification_tokens (token_hash);

CREATE INDEX verification_tokens_user_id_type_idx
    ON verification_tokens (user_id, type);

CREATE INDEX verification_tokens_expires_at_idx
    ON verification_tokens (expires_at);
