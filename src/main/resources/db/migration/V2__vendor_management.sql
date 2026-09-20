-- V2: Vendor management, bank accounts, and bank-change-request workflow

CREATE TABLE vendors (
    id                      UUID PRIMARY KEY,
    organization_id         UUID            NOT NULL REFERENCES organizations (id),
    vendor_code             VARCHAR(50)     NOT NULL,
    legal_name              VARCHAR(255)    NOT NULL,
    display_name            VARCHAR(255),
    email                   VARCHAR(255),
    phone                   VARCHAR(50),
    address                 VARCHAR(500),
    country                 VARCHAR(2)      NOT NULL,
    tax_number              VARCHAR(50),
    gst_number              VARCHAR(50),
    verification_status     VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    risk_status              VARCHAR(30)     NOT NULL DEFAULT 'LOW',
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_vendors_org_code UNIQUE (organization_id, vendor_code)
);

CREATE TABLE vendor_bank_accounts (
    id                          UUID PRIMARY KEY,
    organization_id             UUID            NOT NULL REFERENCES organizations (id),
    vendor_id                   UUID            NOT NULL REFERENCES vendors (id),
    account_holder_name         VARCHAR(255)    NOT NULL,
    masked_account_number       VARCHAR(50)     NOT NULL,
    encrypted_account_number    TEXT            NOT NULL,
    bank_name                   VARCHAR(255)    NOT NULL,
    branch_name                 VARCHAR(255),
    routing_code                VARCHAR(50)     NOT NULL,
    account_type                VARCHAR(30)     NOT NULL,
    verification_status         VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    effective_from               TIMESTAMPTZ,
    effective_to                 TIMESTAMPTZ,
    active                      BOOLEAN         NOT NULL DEFAULT FALSE,
    version                     BIGINT          NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ     NOT NULL,
    updated_at                  TIMESTAMPTZ     NOT NULL,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    deleted                     BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE bank_change_requests (
    id                              UUID PRIMARY KEY,
    organization_id                 UUID            NOT NULL REFERENCES organizations (id),
    vendor_id                       UUID            NOT NULL REFERENCES vendors (id),
    requested_bank_account_id       UUID            NOT NULL REFERENCES vendor_bank_accounts (id),
    previous_bank_account_id        UUID            REFERENCES vendor_bank_accounts (id),
    status                          VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    requested_by                    UUID            NOT NULL REFERENCES users (id),
    reviewed_by                     UUID            REFERENCES users (id),
    reviewed_at                     TIMESTAMPTZ,
    notes                           VARCHAR(1000),
    version                         BIGINT          NOT NULL DEFAULT 0,
    created_at                      TIMESTAMPTZ     NOT NULL,
    updated_at                      TIMESTAMPTZ     NOT NULL,
    created_by                      VARCHAR(100),
    updated_by                      VARCHAR(100),
    deleted                         BOOLEAN         NOT NULL DEFAULT FALSE
);

-- Indexes supporting the query patterns used by the repositories/specifications in this phase
CREATE INDEX idx_vendors_organization_id ON vendors (organization_id);
CREATE INDEX idx_vendors_verification_status ON vendors (verification_status);
CREATE INDEX idx_vendors_risk_status ON vendors (risk_status);

CREATE INDEX idx_vendor_bank_accounts_vendor_id ON vendor_bank_accounts (vendor_id);
CREATE INDEX idx_vendor_bank_accounts_organization_id ON vendor_bank_accounts (organization_id);
-- Partial index: at most one active bank account per vendor should ever exist,
-- and this is exactly the row every payment-related lookup needs quickly.
CREATE UNIQUE INDEX uq_vendor_bank_accounts_one_active_per_vendor
    ON vendor_bank_accounts (vendor_id)
    WHERE active = TRUE;

CREATE INDEX idx_bank_change_requests_vendor_id ON bank_change_requests (vendor_id);
CREATE INDEX idx_bank_change_requests_organization_id ON bank_change_requests (organization_id);
CREATE INDEX idx_bank_change_requests_status ON bank_change_requests (status);
