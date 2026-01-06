-- V3: Create Tenant Settings Table

-- Ensure tenants table exists (defensive, based on Tenant entity)
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    subscription_plan VARCHAR(50) NOT NULL,
    configuration TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE tenant_settings (
    id UUID PRIMARY KEY,
    setting_key VARCHAR(255) NOT NULL,
    setting_value TEXT,
    setting_type VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_tenant_settings_key_tenant UNIQUE (setting_key, tenant_id)
);
