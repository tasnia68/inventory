-- V66: Wipe all storefront state. v2 is not live yet; this lets every tenant
-- regenerate fresh defaults from the new manifest-driven code path on next read.
-- After this runs, no marland_manor / atelier / legacy v1 schema data survives.

DELETE FROM storefront_publish_versions;

DELETE FROM tenant_settings
WHERE setting_key LIKE 'storefront.%'
   OR category = 'STOREFRONT';
