-- V65: Rename marland_manor templateKey → boutique across all tenant state.
-- The backend registry still resolves "marland_manor" via the boutique manifest's
-- aliases array, so this migration is purely cosmetic — but normalizes data so
-- the admin Themes gallery shows the correct active theme without alias resolution.

-- 1. tenant_settings rows that store individual site/theme settings as JSON
UPDATE tenant_settings
SET setting_value = REPLACE(setting_value, '"templateKey":"marland_manor"', '"templateKey":"boutique"')
WHERE setting_value LIKE '%"templateKey":"marland_manor"%';

UPDATE tenant_settings
SET setting_value = REPLACE(setting_value, '"templateKey": "marland_manor"', '"templateKey": "boutique"')
WHERE setting_value LIKE '%"templateKey": "marland_manor"%';

-- 2. storefront_publish_versions.theme_key column (set in V64)
UPDATE storefront_publish_versions
SET theme_key = 'boutique',
    theme_version = '2.0.0'
WHERE theme_key = 'marland_manor';

-- 3. storefront_publish_versions.snapshot_json — replace inside the JSON blob
UPDATE storefront_publish_versions
SET snapshot_json = REPLACE(snapshot_json, '"templateKey":"marland_manor"', '"templateKey":"boutique"')
WHERE snapshot_json LIKE '%"templateKey":"marland_manor"%';

UPDATE storefront_publish_versions
SET snapshot_json = REPLACE(snapshot_json, '"templateKey": "marland_manor"', '"templateKey": "boutique"')
WHERE snapshot_json LIKE '%"templateKey": "marland_manor"%';
