ALTER TABLE accounts_payable_invoices
    ADD COLUMN source_system VARCHAR(64) NOT NULL DEFAULT 'INVENTORY',
    ADD COLUMN source_document_type VARCHAR(64);

UPDATE accounts_payable_invoices
SET source_document_type = CASE
    WHEN purchase_order_id IS NOT NULL THEN 'PURCHASE_ORDER'
    ELSE 'SUPPLIER'
END
WHERE source_document_type IS NULL;

ALTER TABLE accounts_payable_invoices
    ALTER COLUMN source_document_type SET NOT NULL;

CREATE INDEX idx_accounts_payable_invoices_source_metadata
    ON accounts_payable_invoices (tenant_id, source_system, source_document_type);

ALTER TABLE accounts_receivable_invoices
    ADD COLUMN source_system VARCHAR(64) NOT NULL DEFAULT 'INVENTORY',
    ADD COLUMN source_document_type VARCHAR(64);

UPDATE accounts_receivable_invoices
SET source_document_type = CASE
    WHEN sales_order_id IS NOT NULL THEN 'SALES_ORDER'
    ELSE 'CUSTOMER'
END
WHERE source_document_type IS NULL;

ALTER TABLE accounts_receivable_invoices
    ALTER COLUMN source_document_type SET NOT NULL;

CREATE INDEX idx_accounts_receivable_invoices_source_metadata
    ON accounts_receivable_invoices (tenant_id, source_system, source_document_type);
