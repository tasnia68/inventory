ALTER TABLE accounts_payable_invoices
    DROP CONSTRAINT IF EXISTS fk_accounts_payable_invoices_supplier,
    DROP CONSTRAINT IF EXISTS fk_accounts_payable_invoices_purchase_order,
    ALTER COLUMN supplier_id DROP NOT NULL,
    ADD COLUMN source_party_id VARCHAR(128),
    ADD COLUMN source_party_name VARCHAR(255),
    ADD COLUMN source_document_id VARCHAR(128),
    ADD COLUMN source_document_number VARCHAR(255);

UPDATE accounts_payable_invoices
SET source_party_id = supplier_id::text,
    source_party_name = supplier_name,
    source_document_id = COALESCE(purchase_order_id::text, supplier_id::text),
    source_document_number = COALESCE(purchase_order_number, supplier_invoice_number, supplier_name)
WHERE source_party_id IS NULL;

CREATE INDEX idx_accounts_payable_invoices_source_party
    ON accounts_payable_invoices (tenant_id, source_system, source_party_id);

CREATE INDEX idx_accounts_payable_invoices_source_document
    ON accounts_payable_invoices (tenant_id, source_system, source_document_type, source_document_id);

ALTER TABLE accounts_receivable_invoices
    DROP CONSTRAINT IF EXISTS fk_accounts_receivable_invoices_customer,
    DROP CONSTRAINT IF EXISTS fk_accounts_receivable_invoices_sales_order,
    ALTER COLUMN customer_id DROP NOT NULL,
    ADD COLUMN source_party_id VARCHAR(128),
    ADD COLUMN source_party_name VARCHAR(255),
    ADD COLUMN source_document_id VARCHAR(128),
    ADD COLUMN source_document_number VARCHAR(255);

UPDATE accounts_receivable_invoices
SET source_party_id = customer_id::text,
    source_party_name = customer_name,
    source_document_id = COALESCE(sales_order_id::text, customer_id::text),
    source_document_number = COALESCE(sales_order_number, customer_invoice_number, customer_name)
WHERE source_party_id IS NULL;

CREATE INDEX idx_accounts_receivable_invoices_source_party
    ON accounts_receivable_invoices (tenant_id, source_system, source_party_id);

CREATE INDEX idx_accounts_receivable_invoices_source_document
    ON accounts_receivable_invoices (tenant_id, source_system, source_document_type, source_document_id);
