CREATE TABLE damage_records (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    record_number VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    warehouse_id UUID NOT NULL,
    quarantine_location_id UUID,
    reference VARCHAR(255),
    notes TEXT,
    damage_date TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_damage_record_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_damage_record_quarantine_location FOREIGN KEY (quarantine_location_id) REFERENCES storage_locations(id)
);

CREATE INDEX idx_damage_records_tenant ON damage_records(tenant_id);
CREATE INDEX idx_damage_records_warehouse ON damage_records(warehouse_id);
CREATE INDEX idx_damage_records_status ON damage_records(status);

CREATE TABLE damage_record_items (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    damage_record_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    batch_id UUID,
    source_storage_location_id UUID,
    quantity NUMERIC(19, 6) NOT NULL,
    disposition VARCHAR(50) NOT NULL,
    serial_numbers TEXT,
    CONSTRAINT fk_damage_record_item_record FOREIGN KEY (damage_record_id) REFERENCES damage_records(id) ON DELETE CASCADE,
    CONSTRAINT fk_damage_record_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id),
    CONSTRAINT fk_damage_record_item_batch FOREIGN KEY (batch_id) REFERENCES batches(id),
    CONSTRAINT fk_damage_record_item_location FOREIGN KEY (source_storage_location_id) REFERENCES storage_locations(id)
);

CREATE INDEX idx_damage_record_items_record ON damage_record_items(damage_record_id);
CREATE INDEX idx_damage_record_items_variant ON damage_record_items(product_variant_id);