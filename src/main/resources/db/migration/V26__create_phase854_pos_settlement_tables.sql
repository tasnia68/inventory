ALTER TABLE pos_sales
    ADD COLUMN IF NOT EXISTS suspended_sale_id UUID;

ALTER TABLE pos_shifts
    ADD COLUMN IF NOT EXISTS expected_cash_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS declared_cash_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS over_short_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS settlement_approval_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN IF NOT EXISTS settlement_approved_by UUID,
    ADD COLUMN IF NOT EXISTS settlement_approved_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS settlement_approval_notes TEXT;

ALTER TABLE pos_shifts
    ADD CONSTRAINT fk_pos_shifts_settlement_approved_by
        FOREIGN KEY (settlement_approved_by) REFERENCES users(id);

CREATE TABLE IF NOT EXISTS pos_suspended_sales (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    suspended_number VARCHAR(64) NOT NULL,
    terminal_id UUID NOT NULL,
    cashier_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    completed_sale_id UUID,
    status VARCHAR(32) NOT NULL,
    suspended_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    manual_discount_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    subtotal_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    currency VARCHAR(3),
    coupon_codes TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT,
    CONSTRAINT uk_pos_suspended_sales_number UNIQUE (tenant_id, suspended_number),
    CONSTRAINT fk_pos_suspended_sales_terminal FOREIGN KEY (terminal_id) REFERENCES pos_terminals(id),
    CONSTRAINT fk_pos_suspended_sales_cashier FOREIGN KEY (cashier_id) REFERENCES users(id),
    CONSTRAINT fk_pos_suspended_sales_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_pos_suspended_sales_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_pos_suspended_sales_completed_sale FOREIGN KEY (completed_sale_id) REFERENCES pos_sales(id)
);

CREATE INDEX IF NOT EXISTS idx_pos_suspended_sales_terminal_status
    ON pos_suspended_sales (tenant_id, terminal_id, status);

CREATE TABLE IF NOT EXISTS pos_suspended_sale_items (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    suspended_sale_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    sku_snapshot VARCHAR(255) NOT NULL,
    description_snapshot TEXT,
    quantity NUMERIC(19, 6) NOT NULL,
    unit_price NUMERIC(19, 6) NOT NULL,
    line_discount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    line_total NUMERIC(19, 6) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT,
    CONSTRAINT fk_pos_suspended_sale_items_sale FOREIGN KEY (suspended_sale_id) REFERENCES pos_suspended_sales(id),
    CONSTRAINT fk_pos_suspended_sale_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);

CREATE INDEX IF NOT EXISTS idx_pos_suspended_sale_items_sale
    ON pos_suspended_sale_items (tenant_id, suspended_sale_id);

CREATE TABLE IF NOT EXISTS pos_sale_payments (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    sale_id UUID NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 6) NOT NULL,
    reference_number VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT,
    CONSTRAINT fk_pos_sale_payments_sale FOREIGN KEY (sale_id) REFERENCES pos_sales(id)
);

CREATE INDEX IF NOT EXISTS idx_pos_sale_payments_sale
    ON pos_sale_payments (tenant_id, sale_id);

CREATE INDEX IF NOT EXISTS idx_pos_sale_payments_method
    ON pos_sale_payments (tenant_id, payment_method);

CREATE TABLE IF NOT EXISTS pos_cash_movements (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    shift_id UUID NOT NULL,
    terminal_id UUID NOT NULL,
    cashier_id UUID NOT NULL,
    type VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 6) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    reference_number VARCHAR(255),
    reason VARCHAR(255) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT,
    CONSTRAINT fk_pos_cash_movements_shift FOREIGN KEY (shift_id) REFERENCES pos_shifts(id),
    CONSTRAINT fk_pos_cash_movements_terminal FOREIGN KEY (terminal_id) REFERENCES pos_terminals(id),
    CONSTRAINT fk_pos_cash_movements_cashier FOREIGN KEY (cashier_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_pos_cash_movements_shift
    ON pos_cash_movements (tenant_id, shift_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS pos_shift_tender_counts (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    shift_id UUID NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    expected_amount NUMERIC(19, 6) NOT NULL,
    declared_amount NUMERIC(19, 6) NOT NULL,
    variance_amount NUMERIC(19, 6) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT,
    CONSTRAINT fk_pos_shift_tender_counts_shift FOREIGN KEY (shift_id) REFERENCES pos_shifts(id)
);

CREATE INDEX IF NOT EXISTS idx_pos_shift_tender_counts_shift
    ON pos_shift_tender_counts (tenant_id, shift_id);

CREATE TABLE IF NOT EXISTS pos_refund_settlement_impacts (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    sales_refund_id UUID NOT NULL,
    shift_id UUID,
    terminal_id UUID,
    payment_method VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 6) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    reference_number VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT,
    CONSTRAINT fk_pos_refund_settlement_impacts_refund FOREIGN KEY (sales_refund_id) REFERENCES sales_refunds(id),
    CONSTRAINT fk_pos_refund_settlement_impacts_shift FOREIGN KEY (shift_id) REFERENCES pos_shifts(id),
    CONSTRAINT fk_pos_refund_settlement_impacts_terminal FOREIGN KEY (terminal_id) REFERENCES pos_terminals(id)
);

CREATE INDEX IF NOT EXISTS idx_pos_refund_settlement_impacts_shift
    ON pos_refund_settlement_impacts (tenant_id, shift_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_pos_refund_settlement_impacts_terminal
    ON pos_refund_settlement_impacts (tenant_id, terminal_id, occurred_at DESC);

ALTER TABLE pos_sales
    ADD CONSTRAINT fk_pos_sales_suspended_sale
        FOREIGN KEY (suspended_sale_id) REFERENCES pos_suspended_sales(id);