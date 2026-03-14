CREATE TABLE damage_record_documents (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    damage_record_id UUID NOT NULL,
    document_type VARCHAR(255) NOT NULL,
    filename VARCHAR(500) NOT NULL,
    content_type VARCHAR(255),
    storage_path VARCHAR(1000) NOT NULL,
    notes TEXT,
    CONSTRAINT fk_damage_record_document_record FOREIGN KEY (damage_record_id) REFERENCES damage_records(id) ON DELETE CASCADE
);

CREATE INDEX idx_damage_record_documents_record ON damage_record_documents(damage_record_id);
CREATE INDEX idx_damage_record_documents_tenant ON damage_record_documents(tenant_id);