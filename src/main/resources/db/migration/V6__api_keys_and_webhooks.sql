-- V6: API keys and webhooks

CREATE TABLE api_keys (
    id                      UUID PRIMARY KEY,
    organization_id         UUID            NOT NULL REFERENCES organizations (id),
    name                    VARCHAR(255)    NOT NULL,
    key_prefix              VARCHAR(20)     NOT NULL,
    hashed_key              VARCHAR(128)    NOT NULL,
    scopes                  VARCHAR(500)    NOT NULL,
    expires_at              TIMESTAMPTZ,
    revoked                 BOOLEAN         NOT NULL DEFAULT FALSE,
    revoked_at              TIMESTAMPTZ,
    last_used_at            TIMESTAMPTZ,
    rate_limit_per_minute   INT,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_api_keys_hashed_key UNIQUE (hashed_key)
);

CREATE TABLE webhook_subscriptions (
    id                  UUID PRIMARY KEY,
    organization_id     UUID            NOT NULL REFERENCES organizations (id),
    url                 VARCHAR(500)    NOT NULL,
    event_types         VARCHAR(500)    NOT NULL,
    signing_secret      VARCHAR(100)    NOT NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE webhook_delivery_attempts (
    id                          UUID PRIMARY KEY,
    organization_id             UUID            NOT NULL REFERENCES organizations (id),
    webhook_subscription_id     UUID            NOT NULL REFERENCES webhook_subscriptions (id),
    event_id                    UUID            NOT NULL,
    event_type                  VARCHAR(100)    NOT NULL,
    payload                     TEXT            NOT NULL,
    status                      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    http_status                 INT,
    attempt_count               INT             NOT NULL DEFAULT 0,
    last_attempt_at             TIMESTAMPTZ,
    next_retry_at               TIMESTAMPTZ,
    version                     BIGINT          NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ     NOT NULL,
    updated_at                  TIMESTAMPTZ     NOT NULL,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    deleted                     BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_webhook_delivery_attempts_event_id UNIQUE (event_id)
);

-- Indexes supporting the query patterns used by the repositories in this phase
CREATE INDEX idx_api_keys_organization_id ON api_keys (organization_id);
CREATE INDEX idx_webhook_subscriptions_organization_id ON webhook_subscriptions (organization_id);
CREATE INDEX idx_webhook_delivery_attempts_subscription_id ON webhook_delivery_attempts (webhook_subscription_id);
CREATE INDEX idx_webhook_delivery_attempts_status_retry ON webhook_delivery_attempts (status, next_retry_at);
