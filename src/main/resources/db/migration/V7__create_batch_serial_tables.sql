-- V7: Add Batch Tracking

-- 1. Update Product Template
ALTER TABLE product_templates ADD COLUMN IF NOT EXISTS is_batch_tracked BOOLEAN DEFAULT FALSE;

-- 2. Create Batches Table
CREATE TABLE IF NOT EXISTS batches (
    id UUID PRIMARY KEY,
    batch_number VARCHAR(255) NOT NULL,
    manufacturing_date DATE,
    expiry_date DATE,
    product_variant_id UUID NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_batch_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
);

-- 3. Update Stocks Table
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS batch_id UUID;
ALTER TABLE stocks ADD CONSTRAINT fk_stock_batch FOREIGN KEY (batch_id) REFERENCES batches (id);

-- 4. Update Stock Movements Table
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS batch_id UUID;
ALTER TABLE stock_movements ADD CONSTRAINT fk_movement_batch FOREIGN KEY (batch_id) REFERENCES batches (id);
