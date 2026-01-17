-- V6: Create Stock Transactions and ensure missing stock/product tables exist

-- 1. Product Catalog Tables (Ensure they exist)

CREATE TABLE IF NOT EXISTS attribute_groups (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS product_attributes (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    required BOOLEAN DEFAULT FALSE,
    validation_regex VARCHAR(255),
    group_id UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_attribute_group FOREIGN KEY (group_id) REFERENCES attribute_groups (id)
);

CREATE TABLE IF NOT EXISTS product_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id UUID,
    uom_id UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL
    -- Constraints for category_id and uom_id might be added by other migrations if table existed,
    -- but here we assume if it didn't exist, we need to add them.
    -- However, V4 and V5 alter this table. So it MUST exist.
    -- If it exists, these statements do nothing.
);

CREATE TABLE IF NOT EXISTS product_variants (
    id UUID PRIMARY KEY,
    sku VARCHAR(255) NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    template_id UUID NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_variant_sku_tenant UNIQUE (sku, tenant_id),
    CONSTRAINT fk_variant_template FOREIGN KEY (template_id) REFERENCES product_templates (id)
);

CREATE TABLE IF NOT EXISTS product_attribute_values (
    id UUID PRIMARY KEY,
    variant_id UUID NOT NULL,
    attribute_id UUID NOT NULL,
    value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_val_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_val_attribute FOREIGN KEY (attribute_id) REFERENCES product_attributes (id)
);


-- 2. Warehouse & Stock Tables (Ensure they exist)

CREATE TABLE IF NOT EXISTS warehouses (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    type VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS storage_locations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    warehouse_id UUID NOT NULL,
    parent_id UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_location_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_location_parent FOREIGN KEY (parent_id) REFERENCES storage_locations (id)
);

CREATE TABLE IF NOT EXISTS stocks (
    id UUID PRIMARY KEY,
    product_variant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    storage_location_id UUID,
    quantity NUMERIC(19, 6) NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_stock_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_location FOREIGN KEY (storage_location_id) REFERENCES storage_locations (id)
);

CREATE TABLE IF NOT EXISTS stock_movements (
    id UUID PRIMARY KEY,
    product_variant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    storage_location_id UUID,
    quantity NUMERIC(19, 6) NOT NULL,
    type VARCHAR(50) NOT NULL,
    reason TEXT,
    reference_id VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_movement_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_movement_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_movement_location FOREIGN KEY (storage_location_id) REFERENCES storage_locations (id)
);


-- 3. Stock Transactions Tables (New)

CREATE TABLE stock_transactions (
    id UUID PRIMARY KEY,
    transaction_number VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    source_warehouse_id UUID,
    destination_warehouse_id UUID,
    reference VARCHAR(255),
    notes TEXT,
    transaction_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_transaction_number_tenant UNIQUE (transaction_number, tenant_id),
    CONSTRAINT fk_transaction_source_wh FOREIGN KEY (source_warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_transaction_dest_wh FOREIGN KEY (destination_warehouse_id) REFERENCES warehouses (id)
);

CREATE TABLE stock_transaction_items (
    id UUID PRIMARY KEY,
    stock_transaction_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    quantity NUMERIC(19, 6) NOT NULL,
    source_storage_location_id UUID,
    destination_storage_location_id UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_item_transaction FOREIGN KEY (stock_transaction_id) REFERENCES stock_transactions (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_item_source_loc FOREIGN KEY (source_storage_location_id) REFERENCES storage_locations (id),
    CONSTRAINT fk_item_dest_loc FOREIGN KEY (destination_storage_location_id) REFERENCES storage_locations (id)
);
