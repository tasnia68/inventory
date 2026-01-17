-- V8: Add Serial Number Tracking

-- 1. Update Product Template
ALTER TABLE product_templates ADD COLUMN IF NOT EXISTS is_serial_tracked BOOLEAN DEFAULT FALSE;

-- 2. Create Serial Numbers Table
CREATE TABLE IF NOT EXISTS serial_numbers (
    id UUID PRIMARY KEY,
    serial_number VARCHAR(255) NOT NULL,
    product_variant_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    warehouse_id UUID,
    storage_location_id UUID,
    batch_id UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_serial_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_serial_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_serial_location FOREIGN KEY (storage_location_id) REFERENCES storage_locations (id),
    CONSTRAINT fk_serial_batch FOREIGN KEY (batch_id) REFERENCES batches (id)
);
