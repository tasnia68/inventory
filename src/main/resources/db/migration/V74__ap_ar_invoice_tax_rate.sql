ALTER TABLE accounts_payable_invoices
    ADD COLUMN tax_rate_id UUID,
    ADD COLUMN net_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD COLUMN tax_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_ap_invoices_tax_rate
        FOREIGN KEY (tax_rate_id) REFERENCES tax_rates(id);

ALTER TABLE accounts_receivable_invoices
    ADD COLUMN tax_rate_id UUID,
    ADD COLUMN net_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD COLUMN tax_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_ar_invoices_tax_rate
        FOREIGN KEY (tax_rate_id) REFERENCES tax_rates(id);

UPDATE accounts_payable_invoices
SET net_amount = total_amount,
    tax_amount = 0
WHERE net_amount = 0
  AND tax_amount = 0;

UPDATE accounts_receivable_invoices
SET net_amount = total_amount,
    tax_amount = 0
WHERE net_amount = 0
  AND tax_amount = 0;
