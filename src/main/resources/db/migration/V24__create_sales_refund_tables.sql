ALTER TABLE customers
ADD COLUMN IF NOT EXISTS store_credit_balance NUMERIC(19, 6) NOT NULL DEFAULT 0;

CREATE TABLE sales_refunds (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    refund_number VARCHAR(255) NOT NULL UNIQUE,
    credit_note_number VARCHAR(255) UNIQUE,
    sales_order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    rma_id UUID,
    pos_sale_id UUID,
    replacement_sales_order_id UUID,
    status VARCHAR(50) NOT NULL,
    refund_type VARCHAR(50) NOT NULL,
    refund_method VARCHAR(50) NOT NULL,
    original_payment_method VARCHAR(50),
    requested_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    completed_at TIMESTAMP,
    rejected_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    reason TEXT,
    notes TEXT,
    subtotal_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    replacement_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    net_refund_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    amount_due_from_customer NUMERIC(19, 6) NOT NULL DEFAULT 0,
    store_credit_issued NUMERIC(19, 6) NOT NULL DEFAULT 0,
    exchange_price_difference NUMERIC(19, 6) NOT NULL DEFAULT 0,
    document_generated_at TIMESTAMP,
    document_content TEXT,
    CONSTRAINT fk_sales_refund_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders(id),
    CONSTRAINT fk_sales_refund_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_sales_refund_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_sales_refund_rma FOREIGN KEY (rma_id) REFERENCES return_merchandise_authorizations(id),
    CONSTRAINT fk_sales_refund_pos_sale FOREIGN KEY (pos_sale_id) REFERENCES pos_sales(id),
    CONSTRAINT fk_sales_refund_replacement_order FOREIGN KEY (replacement_sales_order_id) REFERENCES sales_orders(id)
);

CREATE INDEX idx_sales_refunds_tenant ON sales_refunds(tenant_id);
CREATE INDEX idx_sales_refunds_order ON sales_refunds(sales_order_id);
CREATE INDEX idx_sales_refunds_customer ON sales_refunds(customer_id);
CREATE INDEX idx_sales_refunds_status ON sales_refunds(status);
CREATE INDEX idx_sales_refunds_requested_at ON sales_refunds(requested_at);

CREATE TABLE sales_refund_items (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    sales_refund_id UUID NOT NULL,
    sales_order_item_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    batch_id UUID,
    storage_location_id UUID,
    quantity NUMERIC(19, 6) NOT NULL,
    unit_price NUMERIC(19, 6) NOT NULL,
    refund_amount NUMERIC(19, 6) NOT NULL,
    return_disposition VARCHAR(50) NOT NULL,
    reason TEXT,
    serial_numbers TEXT,
    CONSTRAINT fk_sales_refund_item_refund FOREIGN KEY (sales_refund_id) REFERENCES sales_refunds(id) ON DELETE CASCADE,
    CONSTRAINT fk_sales_refund_item_order_item FOREIGN KEY (sales_order_item_id) REFERENCES sales_order_items(id),
    CONSTRAINT fk_sales_refund_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id),
    CONSTRAINT fk_sales_refund_item_batch FOREIGN KEY (batch_id) REFERENCES batches(id),
    CONSTRAINT fk_sales_refund_item_location FOREIGN KEY (storage_location_id) REFERENCES storage_locations(id)
);

CREATE INDEX idx_sales_refund_items_refund ON sales_refund_items(sales_refund_id);
CREATE INDEX idx_sales_refund_items_order_item ON sales_refund_items(sales_order_item_id);

CREATE TABLE sales_refund_audit_entries (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    sales_refund_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50),
    notes TEXT,
    acted_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_sales_refund_audit_refund FOREIGN KEY (sales_refund_id) REFERENCES sales_refunds(id) ON DELETE CASCADE
);

CREATE INDEX idx_sales_refund_audit_refund ON sales_refund_audit_entries(sales_refund_id);
CREATE INDEX idx_sales_refund_audit_acted_at ON sales_refund_audit_entries(acted_at);

CREATE TABLE customer_store_credit_transactions (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    customer_id UUID NOT NULL,
    sales_refund_id UUID,
    type VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 6) NOT NULL,
    balance_before NUMERIC(19, 6) NOT NULL,
    balance_after NUMERIC(19, 6) NOT NULL,
    reference_number VARCHAR(255),
    notes TEXT,
    transaction_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_store_credit_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_store_credit_refund FOREIGN KEY (sales_refund_id) REFERENCES sales_refunds(id)
);

CREATE INDEX idx_store_credit_customer ON customer_store_credit_transactions(customer_id);
CREATE INDEX idx_store_credit_refund ON customer_store_credit_transactions(sales_refund_id);
CREATE INDEX idx_store_credit_transaction_date ON customer_store_credit_transactions(transaction_date);