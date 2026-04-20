INSERT INTO tenant_settings (
    id,
    setting_key,
    setting_value,
    setting_type,
    category,
    created_at,
    updated_at,
    created_by,
    updated_by,
    tenant_id
)
SELECT
    (
        substr(md5(random()::text || clock_timestamp()::text || seed.tenant_id), 1, 8) || '-' ||
        substr(md5(random()::text || clock_timestamp()::text || seed.tenant_id), 9, 4) || '-' ||
        substr(md5(random()::text || clock_timestamp()::text || seed.tenant_id), 13, 4) || '-' ||
        substr(md5(random()::text || clock_timestamp()::text || seed.tenant_id), 17, 4) || '-' ||
        substr(md5(random()::text || clock_timestamp()::text || seed.tenant_id), 21, 12)
    )::uuid,
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
    SELECT DISTINCT ts.tenant_id
    FROM tenant_settings ts
    WHERE ts.setting_key LIKE 'storefront.%'
       OR ts.category = 'STOREFRONT'

    UNION

    SELECT DISTINCT spv.tenant_id
    FROM storefront_publish_versions spv

    UNION

    SELECT DISTINCT sd.tenant_id::text AS tenant_id
    FROM storefront_domains sd

    UNION

    SELECT DISTINCT sa.tenant_id
    FROM storefront_accounts sa

    UNION

    SELECT DISTINCT slc.tenant_id
    FROM storefront_login_challenges slc

    UNION

    SELECT DISTINCT sas.tenant_id
    FROM storefront_account_sessions sas
) seed
LEFT JOIN tenant_settings existing
    ON existing.tenant_id = seed.tenant_id
   AND existing.setting_key = 'tenant.modules.storefront.enabled'
WHERE existing.id IS NULL;