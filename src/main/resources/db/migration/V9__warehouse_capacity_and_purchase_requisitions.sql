-- V9: Warehouse capacity + Purchase Requisition tables

ALTER TABLE warehouses
    ADD COLUMN IF NOT EXISTS capacity NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS used_capacity NUMERIC(19, 4);

CREATE TABLE IF NOT EXISTS purchase_requisitions (
    id UUID PRIMARY KEY,
    reference VARCHAR(255) NOT NULL,
    warehouse_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes TEXT,
    requested_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_pr_reference_tenant UNIQUE (reference, tenant_id),
    CONSTRAINT fk_pr_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

CREATE TABLE IF NOT EXISTS purchase_requisition_items (
    id UUID PRIMARY KEY,
    purchase_requisition_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    quantity NUMERIC(19, 6) NOT NULL,
    suggested_quantity NUMERIC(19, 6),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_pr_item_pr FOREIGN KEY (purchase_requisition_id) REFERENCES purchase_requisitions (id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
);
