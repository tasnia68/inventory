CREATE TABLE IF NOT EXISTS pos_terminals (
    id UUID PRIMARY KEY,
    terminal_code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    warehouse_id UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_pos_terminal_code_tenant UNIQUE (terminal_code, tenant_id),
    CONSTRAINT fk_pos_terminal_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

CREATE INDEX IF NOT EXISTS idx_pos_terminals_warehouse
    ON pos_terminals (warehouse_id);

CREATE TABLE IF NOT EXISTS pos_shifts (
    id UUID PRIMARY KEY,
    terminal_id UUID NOT NULL,
    cashier_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    opening_float NUMERIC(19, 6),
    closing_notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_pos_shift_terminal FOREIGN KEY (terminal_id) REFERENCES pos_terminals (id),
    CONSTRAINT fk_pos_shift_cashier FOREIGN KEY (cashier_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_pos_shifts_terminal_status
    ON pos_shifts (terminal_id, status, opened_at DESC);

CREATE INDEX IF NOT EXISTS idx_pos_shifts_cashier_status
    ON pos_shifts (cashier_id, status, opened_at DESC);

CREATE TABLE IF NOT EXISTS pos_sales (
    id UUID PRIMARY KEY,
    receipt_number VARCHAR(255) NOT NULL,
    client_sale_id VARCHAR(255),
    terminal_id UUID NOT NULL,
    shift_id UUID,
    cashier_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    sales_order_id UUID,
    stock_transaction_id UUID,
    sale_status VARCHAR(50) NOT NULL,
    sync_status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    sale_time TIMESTAMP NOT NULL,
    subtotal NUMERIC(19, 6) NOT NULL,
    discount_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19, 6) NOT NULL,
    tendered_amount NUMERIC(19, 6) DEFAULT 0,
    change_amount NUMERIC(19, 6) DEFAULT 0,
    currency VARCHAR(3),
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_pos_sales_receipt_tenant UNIQUE (receipt_number, tenant_id),
    CONSTRAINT uk_pos_sales_client_sale_tenant UNIQUE (client_sale_id, tenant_id),
    CONSTRAINT fk_pos_sale_terminal FOREIGN KEY (terminal_id) REFERENCES pos_terminals (id),
    CONSTRAINT fk_pos_sale_shift FOREIGN KEY (shift_id) REFERENCES pos_shifts (id),
    CONSTRAINT fk_pos_sale_cashier FOREIGN KEY (cashier_id) REFERENCES users (id),
    CONSTRAINT fk_pos_sale_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_pos_sale_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_pos_sale_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id),
    CONSTRAINT fk_pos_sale_stock_tx FOREIGN KEY (stock_transaction_id) REFERENCES stock_transactions (id)
);

CREATE INDEX IF NOT EXISTS idx_pos_sales_cashier_sale_time
    ON pos_sales (cashier_id, sale_time DESC);

CREATE INDEX IF NOT EXISTS idx_pos_sales_terminal_sale_time
    ON pos_sales (terminal_id, sale_time DESC);

CREATE INDEX IF NOT EXISTS idx_pos_sales_shift
    ON pos_sales (shift_id);

CREATE TABLE IF NOT EXISTS pos_sale_items (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    sku_snapshot VARCHAR(255) NOT NULL,
    barcode_snapshot VARCHAR(255),
    description_snapshot TEXT,
    quantity NUMERIC(19, 6) NOT NULL,
    unit_price NUMERIC(19, 6) NOT NULL,
    line_discount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    line_total NUMERIC(19, 6) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_pos_sale_item_sale FOREIGN KEY (sale_id) REFERENCES pos_sales (id) ON DELETE CASCADE,
    CONSTRAINT fk_pos_sale_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
);

CREATE INDEX IF NOT EXISTS idx_pos_sale_items_sale
    ON pos_sale_items (sale_id);