CREATE TABLE journal_entry_attachments (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    journal_entry_id UUID NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255),
    storage_path VARCHAR(1024) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_journal_entry_attachments_entry
        FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE CASCADE
);

CREATE INDEX idx_journal_entry_attachments_entry
    ON journal_entry_attachments (tenant_id, journal_entry_id, created_at DESC);
