CREATE TABLE financial_events (
    id UUID PRIMARY KEY,
    event_number VARCHAR(255) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    source_document_type VARCHAR(128) NOT NULL,
    source_document_id VARCHAR(128) NOT NULL,
    source_document_number VARCHAR(255),
    external_reference VARCHAR(255),
    summary TEXT,
    total_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    posting_status VARCHAR(32) NOT NULL,
    failure_reason TEXT,
    occurred_at TIMESTAMP NOT NULL,
    actor_name VARCHAR(255),
    metadata_json TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_financial_event_number_tenant UNIQUE (event_number, tenant_id),
    CONSTRAINT uk_financial_event_source_tenant UNIQUE (source_document_type, source_document_id, tenant_id)
);

CREATE INDEX idx_financial_events_status ON financial_events (posting_status);
CREATE INDEX idx_financial_events_type ON financial_events (event_type);
CREATE INDEX idx_financial_events_occurred_at ON financial_events (occurred_at);

CREATE TABLE subledger_entries (
    id UUID PRIMARY KEY,
    financial_event_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    entry_type VARCHAR(16) NOT NULL,
    account_code VARCHAR(128) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    description TEXT,
    amount NUMERIC(19, 6) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    source_document_type VARCHAR(128) NOT NULL,
    source_document_id VARCHAR(128) NOT NULL,
    source_document_number VARCHAR(255),
    posting_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_subledger_event FOREIGN KEY (financial_event_id) REFERENCES financial_events (id) ON DELETE CASCADE
);

CREATE INDEX idx_subledger_event ON subledger_entries (financial_event_id);
CREATE INDEX idx_subledger_status ON subledger_entries (posting_status);
