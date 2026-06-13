CREATE TABLE tax_rates (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    rate NUMERIC(7, 4) NOT NULL,
    output_account_id UUID,
    input_account_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uk_tax_rates_code_tenant UNIQUE (tenant_id, code),
    CONSTRAINT fk_tax_rates_output_account FOREIGN KEY (output_account_id) REFERENCES chart_of_accounts (id),
    CONSTRAINT fk_tax_rates_input_account FOREIGN KEY (input_account_id) REFERENCES chart_of_accounts (id),
    CONSTRAINT chk_tax_rates_rate_non_negative CHECK (rate >= 0)
);

CREATE INDEX idx_tax_rates_tenant_active ON tax_rates (tenant_id, is_active);
CREATE INDEX idx_tax_rates_output_account ON tax_rates (output_account_id);
CREATE INDEX idx_tax_rates_input_account ON tax_rates (input_account_id);
