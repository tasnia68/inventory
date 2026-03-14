ALTER TABLE IF EXISTS pos_suspended_sales
    ALTER COLUMN tenant_id TYPE VARCHAR(255) USING tenant_id::text,
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::text,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::text;

ALTER TABLE IF EXISTS pos_suspended_sale_items
    ALTER COLUMN tenant_id TYPE VARCHAR(255) USING tenant_id::text,
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::text,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::text;

ALTER TABLE IF EXISTS pos_sale_payments
    ALTER COLUMN tenant_id TYPE VARCHAR(255) USING tenant_id::text,
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::text,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::text;

ALTER TABLE IF EXISTS pos_cash_movements
    ALTER COLUMN tenant_id TYPE VARCHAR(255) USING tenant_id::text,
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::text,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::text;

ALTER TABLE IF EXISTS pos_shift_tender_counts
    ALTER COLUMN tenant_id TYPE VARCHAR(255) USING tenant_id::text,
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::text,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::text;

ALTER TABLE IF EXISTS pos_refund_settlement_impacts
    ALTER COLUMN tenant_id TYPE VARCHAR(255) USING tenant_id::text,
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::text,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::text;