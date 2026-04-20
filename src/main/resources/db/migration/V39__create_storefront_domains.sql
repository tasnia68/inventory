CREATE TABLE storefront_domains (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    hostname VARCHAR(255) NOT NULL UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    verification_status VARCHAR(32) NOT NULL,
    tls_status VARCHAR(32) NOT NULL,
    verification_checked_at TIMESTAMP,
    activated_at TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_storefront_domains_tenant_id
    ON storefront_domains (tenant_id);

INSERT INTO storefront_domains (
    id,
    tenant_id,
    hostname,
    is_primary,
    active,
    verification_status,
    tls_status,
    verification_checked_at,
    activated_at,
    last_error,
    created_at,
    updated_at
)
SELECT
    (
        substr(md5(random()::text || clock_timestamp()::text || t.id::text), 1, 8) || '-' ||
        substr(md5(random()::text || clock_timestamp()::text || t.id::text), 9, 4) || '-' ||
        substr(md5(random()::text || clock_timestamp()::text || t.id::text), 13, 4) || '-' ||
        substr(md5(random()::text || clock_timestamp()::text || t.id::text), 17, 4) || '-' ||
        substr(md5(random()::text || clock_timestamp()::text || t.id::text), 21, 12)
    )::uuid,
    t.id,
    lower(trim(BOTH '"' FROM ts.setting_value::jsonb ->> 'domain')),
    TRUE,
    TRUE,
    'VERIFIED',
    'READY',
    NOW(),
    NOW(),
    NULL,
    NOW(),
    NOW()
FROM tenant_settings ts
JOIN tenants t
  ON t.id::text = ts.tenant_id
WHERE ts.setting_key = 'storefront.site'
  AND ts.setting_value IS NOT NULL
  AND trim(coalesce(ts.setting_value::jsonb ->> 'domain', '')) <> ''
  AND position('.' IN trim(coalesce(ts.setting_value::jsonb ->> 'domain', ''))) > 0
ON CONFLICT (hostname) DO NOTHING;
