CREATE TABLE accounts_payable_invoices (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    invoice_number VARCHAR(255) NOT NULL,
    supplier_invoice_number VARCHAR(255),
    supplier_id UUID NOT NULL,
    purchase_order_id UUID,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    balance_due NUMERIC(19, 6) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    notes TEXT,
    CONSTRAINT uk_accounts_payable_invoices_number_tenant UNIQUE (invoice_number, tenant_id),
    CONSTRAINT fk_accounts_payable_invoices_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_accounts_payable_invoices_purchase_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id)
);

CREATE TABLE accounts_payable_payments (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    invoice_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    payment_method VARCHAR(255),
    payment_reference VARCHAR(255),
    notes TEXT,
    CONSTRAINT fk_accounts_payable_payments_invoice FOREIGN KEY (invoice_id) REFERENCES accounts_payable_invoices (id)
);

CREATE INDEX idx_accounts_payable_invoices_supplier ON accounts_payable_invoices (supplier_id);
CREATE INDEX idx_accounts_payable_invoices_purchase_order ON accounts_payable_invoices (purchase_order_id);
CREATE INDEX idx_accounts_payable_invoices_status ON accounts_payable_invoices (status);
CREATE INDEX idx_accounts_payable_payments_invoice ON accounts_payable_payments (invoice_id);

CREATE TABLE accounts_receivable_invoices (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    invoice_number VARCHAR(255) NOT NULL,
    customer_invoice_number VARCHAR(255),
    customer_id UUID NOT NULL,
    sales_order_id UUID,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    balance_due NUMERIC(19, 6) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    notes TEXT,
    CONSTRAINT uk_accounts_receivable_invoices_number_tenant UNIQUE (invoice_number, tenant_id),
    CONSTRAINT fk_accounts_receivable_invoices_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_accounts_receivable_invoices_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id)
);

CREATE TABLE accounts_receivable_payments (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    invoice_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    payment_method VARCHAR(255),
    payment_reference VARCHAR(255),
    notes TEXT,
    CONSTRAINT fk_accounts_receivable_payments_invoice FOREIGN KEY (invoice_id) REFERENCES accounts_receivable_invoices (id)
);

CREATE INDEX idx_accounts_receivable_invoices_customer ON accounts_receivable_invoices (customer_id);
CREATE INDEX idx_accounts_receivable_invoices_sales_order ON accounts_receivable_invoices (sales_order_id);
CREATE INDEX idx_accounts_receivable_invoices_status ON accounts_receivable_invoices (status);
CREATE INDEX idx_accounts_receivable_payments_invoice ON accounts_receivable_payments (invoice_id);
