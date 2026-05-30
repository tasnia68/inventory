-- V64: Record which theme + theme version each published storefront snapshot was authored against.
-- Foundation for Phase 6 marketplace bones (theme upgrade detection per tenant).

ALTER TABLE storefront_publish_versions
    ADD COLUMN IF NOT EXISTS theme_key VARCHAR(128),
    ADD COLUMN IF NOT EXISTS theme_version VARCHAR(64);

-- Backfill: every existing snapshot was authored on the original boutique theme
-- (previously keyed as marland_manor — V65 normalizes the key naming).
UPDATE storefront_publish_versions
SET theme_key = 'marland_manor',
    theme_version = '1.0.0'
WHERE theme_key IS NULL;
-- Note: V65 then rewrites theme_key='marland_manor' → 'boutique' with version 2.0.0.

CREATE INDEX IF NOT EXISTS idx_storefront_publish_versions_theme_key
    ON storefront_publish_versions (tenant_id, theme_key);
