-- V67: Enable the storefront module for every active tenant.
-- V40 only seeded tenants that already had storefront data, so newer tenants
-- (Asif, test1, roni, hridoy, Platform) had no entitlement row and bounced off
-- the "Storefront Module Disabled" guard. v2 is meant to be on for everyone.
--
-- tenant_settings.tenant_id is VARCHAR and is observed populated as either
-- the tenant UUID or the tenant subdomain depending on insertion path, so we
-- seed BOTH forms. The UNIQUE (setting_key, tenant_id) constraint makes the
-- INSERT idempotent across reruns.

INSERT INTO tenant_settings (
    id, setting_key, setting_value, setting_type, category,
    created_at, updated_at, created_by, updated_by, tenant_id
)
SELECT
    gen_random_uuid(),
    'tenant.modules.storefront.enabled',
    'true',
    'BOOLEAN',
    'modules',
    NOW(),
    NOW(),
    'flyway',
    'flyway',
    seed.tenant_id
FROM (
    SELECT id::text AS tenant_id FROM tenants WHERE status = 'ACTIVE'
    UNION
    SELECT subdomain AS tenant_id FROM tenants WHERE status = 'ACTIVE'
) seed
ON CONFLICT (setting_key, tenant_id) DO NOTHING;
