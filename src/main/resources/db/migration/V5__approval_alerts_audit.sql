-- V5: Approval workflow, alerts, and audit logging

CREATE TABLE approval_policies (
    id                      UUID PRIMARY KEY,
    organization_id         UUID            NOT NULL REFERENCES organizations (id),
    name                    VARCHAR(255)    NOT NULL,
    min_amount              NUMERIC(19,2)   NOT NULL,
    max_amount              NUMERIC(19,2),
    required_approvals      INT             NOT NULL,
    minimum_approver_role   VARCHAR(30),
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE approval_requests (
    id                      UUID PRIMARY KEY,
    organization_id         UUID            NOT NULL REFERENCES organizations (id),
    invoice_id              UUID            NOT NULL REFERENCES invoices (id),
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    required_approvals      INT             NOT NULL,
    approvals_received      INT             NOT NULL DEFAULT 0,
    submitted_by            UUID REFERENCES users (id),
    decided_at              TIMESTAMPTZ,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_approval_requests_invoice UNIQUE (invoice_id)
);

CREATE TABLE approval_steps (
    id                      UUID PRIMARY KEY,
    organization_id         UUID            NOT NULL REFERENCES organizations (id),
    approval_request_id     UUID            NOT NULL REFERENCES approval_requests (id),
    invoice_id              UUID            NOT NULL REFERENCES invoices (id),
    sequence_number         INT             NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    decided_by              UUID REFERENCES users (id),
    decided_at              TIMESTAMPTZ,
    comments                VARCHAR(1000),
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE approval_actions (
    id                      UUID PRIMARY KEY,
    organization_id         UUID            NOT NULL REFERENCES organizations (id),
    approval_request_id     UUID            NOT NULL REFERENCES approval_requests (id),
    invoice_id              UUID            NOT NULL REFERENCES invoices (id),
    action_type             VARCHAR(20)     NOT NULL,
    comments                VARCHAR(1000),
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE alerts (
    id                      UUID PRIMARY KEY,
    organization_id         UUID            NOT NULL REFERENCES organizations (id),
    type                    VARCHAR(40)     NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    severity                VARCHAR(20)     NOT NULL,
    title                   VARCHAR(255)    NOT NULL,
    message                 VARCHAR(1000)   NOT NULL,
    related_entity_type     VARCHAR(50),
    related_entity_id       UUID,
    acknowledged_by         UUID REFERENCES users (id),
    acknowledged_at         TIMESTAMPTZ,
    resolved_by             UUID REFERENCES users (id),
    resolved_at             TIMESTAMPTZ,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE audit_events (
    id                  UUID PRIMARY KEY,
    organization_id     UUID,
    action              VARCHAR(100)    NOT NULL,
    entity_type         VARCHAR(50),
    entity_id           UUID,
    old_values          TEXT,
    new_values          TEXT,
    ip_address          VARCHAR(64),
    user_agent          VARCHAR(255),
    correlation_id      VARCHAR(100),
    result              VARCHAR(20)     NOT NULL,
    failure_reason      TEXT,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE
);

-- Indexes supporting the query patterns used by the repositories in this phase
CREATE INDEX idx_approval_policies_organization_id ON approval_policies (organization_id);
CREATE INDEX idx_approval_requests_status_created_at ON approval_requests (status, created_at);
CREATE INDEX idx_approval_steps_approval_request_id ON approval_steps (approval_request_id);
CREATE INDEX idx_approval_actions_approval_request_id ON approval_actions (approval_request_id);

CREATE INDEX idx_alerts_organization_id ON alerts (organization_id);
CREATE INDEX idx_alerts_status ON alerts (status);
CREATE INDEX idx_alerts_type ON alerts (type);
CREATE INDEX idx_alerts_related_entity ON alerts (related_entity_type, related_entity_id);

CREATE INDEX idx_audit_events_organization_id ON audit_events (organization_id);
CREATE INDEX idx_audit_events_action ON audit_events (action);
CREATE INDEX idx_audit_events_entity_type ON audit_events (entity_type);
CREATE INDEX idx_audit_events_created_at ON audit_events (created_at);
