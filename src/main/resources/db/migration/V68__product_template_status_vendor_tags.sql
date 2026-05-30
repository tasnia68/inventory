-- V68: Product editor v2 schema additions.
--
-- ProductTemplate gains the first-class fields Shopify exposes on the product
-- page: status (DRAFT/ACTIVE/ARCHIVED), vendor, product type, tags.
-- ProductVariant gains a base cost (today's CreateProduct UI collects this but
-- ProductVariant has no column, so the value was silently discarded).

ALTER TABLE product_templates
    ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS vendor VARCHAR(255),
    ADD COLUMN IF NOT EXISTS product_type VARCHAR(128),
    ADD COLUMN IF NOT EXISTS tags TEXT;

-- Backfill status from existing is_active flag so historical rows keep semantics.
UPDATE product_templates
SET status = CASE WHEN is_active IS DISTINCT FROM FALSE THEN 'ACTIVE' ELSE 'ARCHIVED' END
WHERE status IS NULL OR status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_product_templates_status ON product_templates (tenant_id, status);

ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS cost NUMERIC(19, 6);
