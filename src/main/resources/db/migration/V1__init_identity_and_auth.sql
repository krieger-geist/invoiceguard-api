-- V1: Core identity & auth schema (organizations, users, membership, tokens)

CREATE TABLE organizations (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    slug            VARCHAR(100)    NOT NULL,
    status          VARCHAR(30)     NOT NULL,
    plan            VARCHAR(50),
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

CREATE TABLE users (
    id                      UUID PRIMARY KEY,
    email                   VARCHAR(255)    NOT NULL,
    password_hash           VARCHAR(255)    NOT NULL,
    first_name              VARCHAR(100)    NOT NULL,
    last_name               VARCHAR(100)    NOT NULL,
    status                  VARCHAR(30)     NOT NULL,
    email_verified          BOOLEAN         NOT NULL DEFAULT FALSE,
    failed_login_attempts   INT             NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ,
    last_login_at           TIMESTAMPTZ,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE organization_members (
    id                  UUID PRIMARY KEY,
    organization_id     UUID            NOT NULL REFERENCES organizations (id),
    user_id             UUID            NOT NULL REFERENCES users (id),
    role                VARCHAR(30)     NOT NULL,
    status              VARCHAR(30)     NOT NULL,
    invited_by          UUID,
    joined_at           TIMESTAMPTZ,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_org_members_org_user UNIQUE (organization_id, user_id)
);

CREATE TABLE refresh_tokens (
    id                      UUID PRIMARY KEY,
    user_id                 UUID            NOT NULL REFERENCES users (id),
    organization_id         UUID            NOT NULL REFERENCES organizations (id),
    token_hash              VARCHAR(128)    NOT NULL,
    expires_at              TIMESTAMPTZ     NOT NULL,
    revoked                 BOOLEAN         NOT NULL DEFAULT FALSE,
    revoked_at              TIMESTAMPTZ,
    replaced_by_token_id    UUID,
    ip_address              VARCHAR(64),
    user_agent              VARCHAR(255),
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE TABLE password_reset_tokens (
    id              UUID PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES users (id),
    token_hash      VARCHAR(128)    NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    used            BOOLEAN         NOT NULL DEFAULT FALSE,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_password_reset_tokens_hash UNIQUE (token_hash)
);

CREATE TABLE email_verification_tokens (
    id              UUID PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES users (id),
    token_hash      VARCHAR(128)    NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    used            BOOLEAN         NOT NULL DEFAULT FALSE,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_email_verification_tokens_hash UNIQUE (token_hash)
);

-- Indexes supporting the query patterns used by the repositories in this phase
CREATE INDEX idx_organization_members_org_id ON organization_members (organization_id);
CREATE INDEX idx_organization_members_user_id ON organization_members (user_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens (user_id);
