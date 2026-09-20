-- V4: Risk engine — assessments, findings, per-organisation rule configuration

CREATE TABLE risk_assessments (
    id                          UUID PRIMARY KEY,
    organization_id             UUID            NOT NULL REFERENCES organizations (id),
    invoice_id                  UUID            NOT NULL REFERENCES invoices (id),
    total_risk_score            INT             NOT NULL,
    risk_level                  VARCHAR(30)     NOT NULL,
    recommended_action          VARCHAR(40)     NOT NULL,
    analysed_at                 TIMESTAMPTZ     NOT NULL,
    engine_version              VARCHAR(20)     NOT NULL,
    processing_duration_ms      BIGINT          NOT NULL,
    status                      VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED',
    explanation                 TEXT            NOT NULL,
    ai_summary                  TEXT,
    version                     BIGINT          NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ     NOT NULL,
    updated_at                  TIMESTAMPTZ     NOT NULL,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    deleted                     BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE risk_findings (
    id                      UUID PRIMARY KEY,
    organization_id         UUID            NOT NULL REFERENCES organizations (id),
    risk_assessment_id      UUID            NOT NULL REFERENCES risk_assessments (id),
    invoice_id              UUID            NOT NULL REFERENCES invoices (id),
    rule_code               VARCHAR(40)     NOT NULL,
    title                   VARCHAR(255)    NOT NULL,
    description             TEXT            NOT NULL,
    severity                VARCHAR(20)     NOT NULL,
    points                  INT             NOT NULL,
    evidence                TEXT,
    metadata                TEXT,
    resolved                BOOLEAN         NOT NULL DEFAULT FALSE,
    resolved_by             UUID REFERENCES users (id),
    resolved_at             TIMESTAMPTZ,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE risk_rule_configurations (
    id                  UUID PRIMARY KEY,
    organization_id     UUID            NOT NULL REFERENCES organizations (id),
    rule_code           VARCHAR(40)     NOT NULL,
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    weight_points       INT,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_risk_rule_config_org_rule UNIQUE (organization_id, rule_code)
);

-- Indexes supporting the query patterns used by the repositories in this phase
CREATE INDEX idx_risk_assessments_invoice_id ON risk_assessments (invoice_id);
CREATE INDEX idx_risk_assessments_organization_id ON risk_assessments (organization_id);
CREATE INDEX idx_risk_findings_risk_assessment_id ON risk_findings (risk_assessment_id);
CREATE INDEX idx_risk_findings_invoice_id ON risk_findings (invoice_id);
CREATE INDEX idx_risk_rule_configurations_organization_id ON risk_rule_configurations (organization_id);
