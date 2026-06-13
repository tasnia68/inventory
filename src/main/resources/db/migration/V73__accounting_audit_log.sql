CREATE TABLE accounting_audit_log (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    entity_type VARCHAR(128) NOT NULL,
    entity_id VARCHAR(128),
    action VARCHAR(128) NOT NULL,
    before_state JSONB,
    after_state JSONB,
    user_id VARCHAR(255),
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX idx_accounting_audit_log_tenant_time
    ON accounting_audit_log (tenant_id, occurred_at DESC);

CREATE INDEX idx_accounting_audit_log_entity
    ON accounting_audit_log (tenant_id, entity_type, entity_id);
