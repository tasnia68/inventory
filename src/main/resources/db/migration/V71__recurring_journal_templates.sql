CREATE TABLE recurring_journal_templates (
    id UUID PRIMARY KEY,
    template_code VARCHAR(128) NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    journal_id UUID NOT NULL,
    memo TEXT,
    currency VARCHAR(3) NOT NULL,
    cadence VARCHAR(32) NOT NULL,
    next_run_date DATE NOT NULL,
    last_run_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_recurring_journal_templates_code_tenant UNIQUE (template_code, tenant_id),
    CONSTRAINT fk_recurring_journal_templates_journal FOREIGN KEY (journal_id) REFERENCES accounting_journals (id)
);

CREATE TABLE recurring_journal_template_lines (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL,
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
    CONSTRAINT fk_recurring_template_lines_template FOREIGN KEY (template_id) REFERENCES recurring_journal_templates (id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_template_lines_account FOREIGN KEY (account_id) REFERENCES chart_of_accounts (id)
);

CREATE INDEX idx_recurring_journal_templates_due ON recurring_journal_templates (tenant_id, active, next_run_date);
CREATE INDEX idx_recurring_journal_template_lines_template ON recurring_journal_template_lines (template_id);
