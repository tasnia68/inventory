ALTER TABLE warehouses
    ADD COLUMN IF NOT EXISTS contact_number VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE warehouses
SET is_active = TRUE
WHERE is_active IS NULL;

ALTER TABLE storage_locations
    ADD COLUMN IF NOT EXISTS type VARCHAR(50);

UPDATE storage_locations
SET type = 'BIN'
WHERE type IS NULL;

ALTER TABLE storage_locations
    ALTER COLUMN type SET NOT NULL;

ALTER TABLE stock_movements
    ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(19, 6),
    ADD COLUMN IF NOT EXISTS total_cost NUMERIC(19, 6);

ALTER TABLE stock_transaction_items
    ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(19, 6);

CREATE TABLE IF NOT EXISTS product_costs (
    id UUID PRIMARY KEY,
    product_variant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    average_cost NUMERIC(19, 6) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_product_cost_variant_warehouse_tenant UNIQUE (product_variant_id, warehouse_id, tenant_id),
    CONSTRAINT fk_product_cost_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_product_cost_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

CREATE TABLE IF NOT EXISTS inventory_valuation_layers (
    id UUID PRIMARY KEY,
    product_variant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    quantity_remaining NUMERIC(19, 6) NOT NULL,
    unit_cost NUMERIC(19, 6) NOT NULL,
    received_date TIMESTAMP NOT NULL,
    reference VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_valuation_layer_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_valuation_layer_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

ALTER TABLE stock_transactions
    ADD COLUMN IF NOT EXISTS reversal_of_transaction_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_stock_transaction_reversal'
          AND table_name = 'stock_transactions'
    ) THEN
        ALTER TABLE stock_transactions
            ADD CONSTRAINT fk_stock_transaction_reversal
            FOREIGN KEY (reversal_of_transaction_id) REFERENCES stock_transactions (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_transactions_reversal_of
    ON stock_transactions (reversal_of_transaction_id)
    WHERE reversal_of_transaction_id IS NOT NULL;