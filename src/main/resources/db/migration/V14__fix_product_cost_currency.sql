ALTER TABLE IF EXISTS product_costs
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3);

UPDATE product_costs
SET currency = 'USD'
WHERE currency IS NULL OR TRIM(currency) = '';

ALTER TABLE IF EXISTS product_costs
    ALTER COLUMN currency SET DEFAULT 'USD';

ALTER TABLE IF EXISTS product_costs
    ALTER COLUMN currency SET NOT NULL;