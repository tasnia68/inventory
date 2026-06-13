ALTER TABLE accounts_payable_invoices
    ADD COLUMN supplier_name VARCHAR(255),
    ADD COLUMN purchase_order_number VARCHAR(255);

UPDATE accounts_payable_invoices api
SET supplier_name = s.name
FROM suppliers s
WHERE api.supplier_id = s.id
  AND api.supplier_name IS NULL;

UPDATE accounts_payable_invoices api
SET purchase_order_number = po.po_number
FROM purchase_orders po
WHERE api.purchase_order_id = po.id
  AND api.purchase_order_number IS NULL;

ALTER TABLE accounts_receivable_invoices
    ADD COLUMN customer_name VARCHAR(255),
    ADD COLUMN sales_order_number VARCHAR(255);

UPDATE accounts_receivable_invoices ari
SET customer_name = c.name
FROM customers c
WHERE ari.customer_id = c.id
  AND ari.customer_name IS NULL;

UPDATE accounts_receivable_invoices ari
SET sales_order_number = so.so_number
FROM sales_orders so
WHERE ari.sales_order_id = so.id
  AND ari.sales_order_number IS NULL;

CREATE INDEX idx_accounts_payable_invoices_supplier_ref
    ON accounts_payable_invoices (tenant_id, supplier_id, supplier_name);

CREATE INDEX idx_accounts_receivable_invoices_customer_ref
    ON accounts_receivable_invoices (tenant_id, customer_id, customer_name);
