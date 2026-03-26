CREATE TABLE treasury_accounts (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    account_code VARCHAR(255) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    CONSTRAINT uk_treasury_accounts_code_tenant UNIQUE (account_code, tenant_id)
);

CREATE TABLE treasury_reconciliations (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    treasury_account_id UUID NOT NULL,
    business_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    statement_balance NUMERIC(19, 6) NOT NULL DEFAULT 0,
    system_balance NUMERIC(19, 6) NOT NULL DEFAULT 0,
    difference_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    notes TEXT,
    completed_at TIMESTAMP,
    CONSTRAINT fk_treasury_reconciliations_account FOREIGN KEY (treasury_account_id) REFERENCES treasury_accounts (id)
);

CREATE TABLE treasury_reconciliation_lines (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    reconciliation_id UUID NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(255) NOT NULL,
    source_reference VARCHAR(255) NOT NULL,
    transaction_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    matched BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_treasury_reconciliation_lines_reconciliation FOREIGN KEY (reconciliation_id) REFERENCES treasury_reconciliations (id)
);

CREATE INDEX idx_treasury_reconciliations_account ON treasury_reconciliations (treasury_account_id);
CREATE INDEX idx_treasury_reconciliations_business_date ON treasury_reconciliations (business_date);
CREATE INDEX idx_treasury_reconciliation_lines_reconciliation ON treasury_reconciliation_lines (reconciliation_id);
