CREATE TABLE IF NOT EXISTS suppliers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255),
    email VARCHAR(255),
    phone_number VARCHAR(255),
    address TEXT,
    payment_terms VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    rating DOUBLE PRECISION,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS supplier_products (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    supplier_sku VARCHAR(255),
    price NUMERIC(19, 6),
    currency VARCHAR(3),
    lead_time_days INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_supplier_product_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_supplier_product_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
);

CREATE TABLE IF NOT EXISTS purchase_orders (
    id UUID PRIMARY KEY,
    po_number VARCHAR(255) NOT NULL,
    supplier_id UUID NOT NULL,
    order_date TIMESTAMP NOT NULL,
    expected_delivery_date DATE,
    status VARCHAR(50) NOT NULL,
    total_amount NUMERIC(19, 6) NOT NULL,
    currency VARCHAR(3),
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_purchase_orders_po_number_tenant UNIQUE (po_number, tenant_id),
    CONSTRAINT fk_purchase_orders_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

CREATE TABLE IF NOT EXISTS purchase_order_items (
    id UUID PRIMARY KEY,
    purchase_order_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 6) NOT NULL,
    total_price NUMERIC(19, 6) NOT NULL,
    received_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_purchase_order_items_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_order_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
);

CREATE TABLE IF NOT EXISTS goods_receipt_notes (
    id UUID PRIMARY KEY,
    grn_number VARCHAR(255) NOT NULL,
    purchase_order_id UUID NOT NULL,
    supplier_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    received_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_goods_receipt_notes_grn_number_tenant UNIQUE (grn_number, tenant_id),
    CONSTRAINT fk_goods_receipt_notes_po FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id),
    CONSTRAINT fk_goods_receipt_notes_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_goods_receipt_notes_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

CREATE TABLE IF NOT EXISTS goods_receipt_note_items (
    id UUID PRIMARY KEY,
    goods_receipt_note_id UUID NOT NULL,
    purchase_order_item_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    received_quantity INTEGER NOT NULL,
    accepted_quantity INTEGER NOT NULL,
    rejected_quantity INTEGER NOT NULL,
    returned_quantity INTEGER NOT NULL DEFAULT 0,
    rejection_reason VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_grn_items_grn FOREIGN KEY (goods_receipt_note_id) REFERENCES goods_receipt_notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_grn_items_po_item FOREIGN KEY (purchase_order_item_id) REFERENCES purchase_order_items (id),
    CONSTRAINT fk_grn_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
);

ALTER TABLE goods_receipt_note_items
    ADD COLUMN IF NOT EXISTS returned_quantity INTEGER NOT NULL DEFAULT 0;

UPDATE goods_receipt_note_items
SET returned_quantity = 0
WHERE returned_quantity IS NULL;

CREATE TABLE IF NOT EXISTS supplier_documents (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL,
    document_type VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255),
    storage_path VARCHAR(500) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_supplier_documents_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS supplier_returns (
    id UUID PRIMARY KEY,
    return_number VARCHAR(255) NOT NULL,
    goods_receipt_note_id UUID NOT NULL,
    supplier_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason TEXT,
    notes TEXT,
    requested_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_supplier_returns_number_tenant UNIQUE (return_number, tenant_id),
    CONSTRAINT fk_supplier_returns_grn FOREIGN KEY (goods_receipt_note_id) REFERENCES goods_receipt_notes (id),
    CONSTRAINT fk_supplier_returns_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_supplier_returns_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

CREATE TABLE IF NOT EXISTS supplier_return_items (
    id UUID PRIMARY KEY,
    supplier_return_id UUID NOT NULL,
    goods_receipt_note_item_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    quantity NUMERIC(19, 6) NOT NULL,
    unit_cost NUMERIC(19, 6),
    reason TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_supplier_return_items_return FOREIGN KEY (supplier_return_id) REFERENCES supplier_returns (id) ON DELETE CASCADE,
    CONSTRAINT fk_supplier_return_items_grn_item FOREIGN KEY (goods_receipt_note_item_id) REFERENCES goods_receipt_note_items (id),
    CONSTRAINT fk_supplier_return_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
);