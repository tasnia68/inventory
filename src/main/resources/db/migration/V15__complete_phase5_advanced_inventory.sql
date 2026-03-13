CREATE TABLE IF NOT EXISTS stock_reservations (
    id UUID PRIMARY KEY,
    product_variant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    storage_location_id UUID,
    batch_id UUID,
    quantity NUMERIC(19, 6) NOT NULL,
    reserved_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    reference_id VARCHAR(255),
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_stock_reservation_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_stock_reservation_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_reservation_location FOREIGN KEY (storage_location_id) REFERENCES storage_locations (id),
    CONSTRAINT fk_stock_reservation_batch FOREIGN KEY (batch_id) REFERENCES batches (id)
);

CREATE INDEX IF NOT EXISTS idx_stock_reservations_variant_warehouse_status
    ON stock_reservations (product_variant_id, warehouse_id, status);

CREATE INDEX IF NOT EXISTS idx_stock_reservations_reference
    ON stock_reservations (reference_id);

CREATE TABLE IF NOT EXISTS replenishment_rules (
    id UUID PRIMARY KEY,
    product_variant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    min_stock NUMERIC(19, 6) NOT NULL,
    max_stock NUMERIC(19, 6) NOT NULL,
    reorder_quantity NUMERIC(19, 6),
    safety_stock NUMERIC(19, 6),
    lead_time_days INTEGER,
    is_enabled BOOLEAN,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_replenishment_rule_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_replenishment_rule_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

CREATE INDEX IF NOT EXISTS idx_replenishment_rules_warehouse_enabled
    ON replenishment_rules (warehouse_id, is_enabled);

CREATE TABLE IF NOT EXISTS cycle_counts (
    id UUID PRIMARY KEY,
    reference VARCHAR(255) NOT NULL,
    warehouse_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    due_date DATE,
    completion_date DATE,
    description TEXT,
    assigned_user_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_cycle_count_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_cycle_count_assigned_user FOREIGN KEY (assigned_user_id) REFERENCES users (id),
    CONSTRAINT uk_cycle_count_reference_tenant UNIQUE (reference, tenant_id)
);

CREATE TABLE IF NOT EXISTS cycle_count_items (
    id UUID PRIMARY KEY,
    cycle_count_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    storage_location_id UUID,
    batch_id UUID,
    system_quantity NUMERIC(19, 4),
    counted_quantity NUMERIC(19, 4),
    variance NUMERIC(19, 4),
    system_serial_numbers TEXT,
    counted_serial_numbers TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_cycle_count_item_cycle_count FOREIGN KEY (cycle_count_id) REFERENCES cycle_counts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cycle_count_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_cycle_count_item_location FOREIGN KEY (storage_location_id) REFERENCES storage_locations (id),
    CONSTRAINT fk_cycle_count_item_batch FOREIGN KEY (batch_id) REFERENCES batches (id)
);

CREATE INDEX IF NOT EXISTS idx_cycle_count_items_cycle_count
    ON cycle_count_items (cycle_count_id);

ALTER TABLE cycle_count_items
    ADD COLUMN IF NOT EXISTS system_serial_numbers TEXT,
    ADD COLUMN IF NOT EXISTS counted_serial_numbers TEXT;