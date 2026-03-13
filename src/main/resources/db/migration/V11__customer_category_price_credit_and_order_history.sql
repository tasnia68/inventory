-- V11: Customer categorization, price list and credit management support

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS category VARCHAR(50),
    ADD COLUMN IF NOT EXISTS outstanding_balance NUMERIC(19, 6);

UPDATE customers
SET category = COALESCE(category, 'OTHER'),
    outstanding_balance = COALESCE(outstanding_balance, 0);

CREATE TABLE IF NOT EXISTS customer_price_lists (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    price NUMERIC(19, 6) NOT NULL,
    currency VARCHAR(3),
    effective_from DATE,
    effective_to DATE,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_customer_price_list_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_price_list_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
);

CREATE TABLE IF NOT EXISTS customer_credit_transactions (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 6) NOT NULL,
    balance_before NUMERIC(19, 6) NOT NULL,
    balance_after NUMERIC(19, 6) NOT NULL,
    reference_number VARCHAR(255),
    notes TEXT,
    transaction_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_customer_credit_tx_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
);