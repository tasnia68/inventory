-- V10: Shipping, Delivery and Return Merchandise Authorization (RMA)

CREATE TABLE IF NOT EXISTS shipments (
    id UUID PRIMARY KEY,
    shipment_number VARCHAR(255) NOT NULL,
    sales_order_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    carrier VARCHAR(255),
    tracking_number VARCHAR(255),
    tracking_url TEXT,
    shipping_label_url TEXT,
    shipped_date TIMESTAMP,
    delivered_date TIMESTAMP,
    delivery_note TEXT,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_shipment_number_tenant UNIQUE (shipment_number, tenant_id),
    CONSTRAINT fk_shipment_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id),
    CONSTRAINT fk_shipment_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

CREATE TABLE IF NOT EXISTS shipment_items (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    sales_order_item_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    quantity NUMERIC(19, 6) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_shipment_item_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id) ON DELETE CASCADE,
    CONSTRAINT fk_shipment_item_so_item FOREIGN KEY (sales_order_item_id) REFERENCES sales_order_items (id),
    CONSTRAINT fk_shipment_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
);

CREATE TABLE IF NOT EXISTS return_merchandise_authorizations (
    id UUID PRIMARY KEY,
    rma_number VARCHAR(255) NOT NULL,
    sales_order_id UUID NOT NULL,
    shipment_id UUID,
    status VARCHAR(50) NOT NULL,
    reason TEXT,
    notes TEXT,
    requested_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    received_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_rma_number_tenant UNIQUE (rma_number, tenant_id),
    CONSTRAINT fk_rma_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id),
    CONSTRAINT fk_rma_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id)
);

CREATE TABLE IF NOT EXISTS return_merchandise_items (
    id UUID PRIMARY KEY,
    rma_id UUID NOT NULL,
    sales_order_item_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    quantity NUMERIC(19, 6) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_rma_item_rma FOREIGN KEY (rma_id) REFERENCES return_merchandise_authorizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_rma_item_so_item FOREIGN KEY (sales_order_item_id) REFERENCES sales_order_items (id),
    CONSTRAINT fk_rma_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
);