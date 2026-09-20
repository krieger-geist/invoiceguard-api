-- V3: Invoice management — invoices, line items, status history, documents, idempotency

CREATE TABLE invoices (
    id                          UUID PRIMARY KEY,
    organization_id             UUID            NOT NULL REFERENCES organizations (id),
    invoice_number               VARCHAR(100)    NOT NULL,
    normalized_invoice_number   VARCHAR(100)    NOT NULL,
    vendor_id                   UUID            NOT NULL REFERENCES vendors (id),
    invoice_date                 DATE            NOT NULL,
    due_date                     DATE,
    currency                    VARCHAR(3)      NOT NULL,
    subtotal                    NUMERIC(19,2)   NOT NULL,
    tax_amount                  NUMERIC(19,2)   NOT NULL,
    discount_amount              NUMERIC(19,2)   NOT NULL DEFAULT 0,
    total_amount                 NUMERIC(19,2)   NOT NULL,
    purchase_order_number        VARCHAR(100),
    payment_reference            VARCHAR(100),
    description                 VARCHAR(1000),
    status                      VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    risk_level                  VARCHAR(30),
    latest_risk_score            INT,
    document_hash                VARCHAR(128),
    idempotency_key              VARCHAR(255),
    version                     BIGINT          NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ     NOT NULL,
    updated_at                  TIMESTAMPTZ     NOT NULL,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    deleted                     BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE invoice_items (
    id                  UUID PRIMARY KEY,
    organization_id     UUID            NOT NULL REFERENCES organizations (id),
    invoice_id          UUID            NOT NULL REFERENCES invoices (id),
    description         VARCHAR(500)    NOT NULL,
    quantity            NUMERIC(19,4)   NOT NULL,
    unit_price          NUMERIC(19,4)   NOT NULL,
    tax_rate            NUMERIC(7,4)    NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(19,2)   NOT NULL,
    line_total          NUMERIC(19,2)   NOT NULL,
    category            VARCHAR(100),
    product_code        VARCHAR(100),
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE invoice_status_history (
    id                  UUID PRIMARY KEY,
    organization_id     UUID            NOT NULL REFERENCES organizations (id),
    invoice_id          UUID            NOT NULL REFERENCES invoices (id),
    from_status         VARCHAR(30),
    to_status           VARCHAR(30)     NOT NULL,
    reason              VARCHAR(1000),
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE invoice_documents (
    id                  UUID PRIMARY KEY,
    organization_id     UUID            NOT NULL REFERENCES organizations (id),
    invoice_id          UUID            NOT NULL REFERENCES invoices (id),
    original_filename   VARCHAR(255)    NOT NULL,
    generated_filename  VARCHAR(255)    NOT NULL,
    mime_type           VARCHAR(100)    NOT NULL,
    size_bytes          BIGINT          NOT NULL,
    checksum            VARCHAR(128)    NOT NULL,
    storage_location    VARCHAR(500)    NOT NULL,
    uploaded_by         UUID REFERENCES users (id),
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE idempotency_records (
    id                  UUID PRIMARY KEY,
    organization_id     UUID            NOT NULL REFERENCES organizations (id),
    idempotency_key     VARCHAR(255)    NOT NULL,
    request_hash        VARCHAR(128)    NOT NULL,
    response_reference  UUID,
    http_status         INT             NOT NULL,
    expires_at          TIMESTAMPTZ     NOT NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_idempotency_org_key UNIQUE (organization_id, idempotency_key)
);

-- Indexes supporting the query patterns used by the repositories/specifications in this phase
CREATE INDEX idx_invoices_organization_id ON invoices (organization_id);
CREATE INDEX idx_invoices_vendor_id ON invoices (vendor_id);
CREATE INDEX idx_invoices_normalized_invoice_number ON invoices (normalized_invoice_number);
CREATE INDEX idx_invoices_invoice_date ON invoices (invoice_date);
CREATE INDEX idx_invoices_status ON invoices (status);
CREATE INDEX idx_invoices_risk_level ON invoices (risk_level);
CREATE INDEX idx_invoices_purchase_order_number ON invoices (purchase_order_number);
CREATE INDEX idx_invoices_created_at ON invoices (created_at);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items (invoice_id);
CREATE INDEX idx_invoice_status_history_invoice_id ON invoice_status_history (invoice_id);
CREATE INDEX idx_invoice_documents_invoice_id ON invoice_documents (invoice_id);
CREATE INDEX idx_idempotency_records_expires_at ON idempotency_records (expires_at);
