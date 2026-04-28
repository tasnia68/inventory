-- Seed one MANUAL courier profile per tenant (always available, no credentials).
INSERT INTO courier_profiles (id, tenant_id, provider_code, display_name, is_default, is_active,
                              credentials_json, config_json, created_at)
SELECT gen_random_uuid(), t.tenant_id, 'MANUAL', 'Manual / Hand-to-hand', FALSE, TRUE,
       NULL, NULL, NOW()
FROM (SELECT DISTINCT tenant_id FROM sales_orders) t
WHERE NOT EXISTS (
    SELECT 1 FROM courier_profiles cp
    WHERE cp.tenant_id = t.tenant_id AND UPPER(cp.provider_code) = 'MANUAL'
);

-- Seed a STEADFAST profile per tenant that already has legacy steadfast.api_key / steadfast.secret_key
-- in tenant_settings. Credentials are materialised into credentials_json so the provider can read
-- them without falling back to TenantSettings.
INSERT INTO courier_profiles (id, tenant_id, provider_code, display_name, is_default, is_active,
                              credentials_json, config_json, created_at)
SELECT gen_random_uuid(),
       k.tenant_id,
       'STEADFAST',
       'Steadfast Courier',
       TRUE,
       TRUE,
       '{"api_key":' || to_json(k.setting_value)::text ||
       ',"secret_key":' || to_json(s.setting_value)::text || '}',
       NULL,
       NOW()
FROM tenant_settings k
JOIN tenant_settings s
  ON s.tenant_id = k.tenant_id AND s.setting_key = 'steadfast.secret_key'
WHERE k.setting_key = 'steadfast.api_key'
  AND k.setting_value IS NOT NULL AND k.setting_value <> ''
  AND s.setting_value IS NOT NULL AND s.setting_value <> ''
  AND NOT EXISTS (
      SELECT 1 FROM courier_profiles cp
      WHERE cp.tenant_id = k.tenant_id AND UPPER(cp.provider_code) = 'STEADFAST'
  );
