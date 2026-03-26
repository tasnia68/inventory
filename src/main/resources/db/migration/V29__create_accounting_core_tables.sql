CREATE TABLE chart_of_accounts (
    id UUID PRIMARY KEY,
    account_code VARCHAR(128) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    parent_account_id UUID,
    allow_manual_posting BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_chart_of_accounts_code_tenant UNIQUE (account_code, tenant_id),
    CONSTRAINT fk_chart_of_accounts_parent FOREIGN KEY (parent_account_id) REFERENCES chart_of_accounts (id)
);

CREATE TABLE accounting_journals (
    id UUID PRIMARY KEY,
    journal_code VARCHAR(128) NOT NULL,
    journal_name VARCHAR(255) NOT NULL,
    description TEXT,
    system_journal BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_accounting_journals_code_tenant UNIQUE (journal_code, tenant_id)
);

CREATE TABLE journal_entries (
    id UUID PRIMARY KEY,
    entry_number VARCHAR(128) NOT NULL,
    journal_id UUID NOT NULL,
    financial_event_id UUID,
    status VARCHAR(32) NOT NULL,
    entry_date TIMESTAMP NOT NULL,
    source_document_type VARCHAR(128) NOT NULL,
    source_document_id VARCHAR(128) NOT NULL,
    source_document_number VARCHAR(255),
    memo TEXT,
    currency VARCHAR(3) NOT NULL,
    total_debits NUMERIC(19, 6) NOT NULL DEFAULT 0,
    total_credits NUMERIC(19, 6) NOT NULL DEFAULT 0,
    posted_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_journal_entries_number_tenant UNIQUE (entry_number, tenant_id),
    CONSTRAINT fk_journal_entries_journal FOREIGN KEY (journal_id) REFERENCES accounting_journals (id),
    CONSTRAINT fk_journal_entries_financial_event FOREIGN KEY (financial_event_id) REFERENCES financial_events (id)
);

CREATE TABLE journal_entry_lines (
    id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL,
    account_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    description TEXT,
    debit_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    credit_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_journal_entry_lines_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_journal_entry_lines_account FOREIGN KEY (account_id) REFERENCES chart_of_accounts (id)
);

CREATE INDEX idx_journal_entries_status ON journal_entries (status);
CREATE INDEX idx_journal_entries_financial_event ON journal_entries (financial_event_id);
CREATE INDEX idx_journal_entry_lines_entry ON journal_entry_lines (journal_entry_id);
