ALTER TABLE sales_orders
    ADD COLUMN IF NOT EXISTS external_source         VARCHAR(32),
    ADD COLUMN IF NOT EXISTS external_order_id       VARCHAR(128),
    ADD COLUMN IF NOT EXISTS external_order_ref      VARCHAR(256),
    ADD COLUMN IF NOT EXISTS cod_amount              NUMERIC(19,6),
    ADD COLUMN IF NOT EXISTS delivery_zone           VARCHAR(32),
    ADD COLUMN IF NOT EXISTS courier_profile_id      UUID,
    ADD COLUMN IF NOT EXISTS packaging_completed_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS hold_reason             TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_sales_orders_external_id
    ON sales_orders (tenant_id, external_source, external_order_id)
    WHERE external_source IS NOT NULL;
