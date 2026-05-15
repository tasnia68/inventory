-- Per-tenant entitlement + quotas for the virtual try-on feature.
-- Super-admin toggles `enabled` and adjusts the two quotas; the public
-- /api/v1/storefront/public/virtual-try-on endpoint enforces them.

CREATE TABLE tenant_virtual_try_on_settings (
    tenant_id                  VARCHAR(255) PRIMARY KEY,
    enabled                    BOOLEAN NOT NULL DEFAULT FALSE,
    max_per_customer_per_day   INTEGER NOT NULL DEFAULT 3,
    max_per_tenant_per_month   INTEGER NOT NULL DEFAULT 500,
    created_at                 TIMESTAMP(6) NOT NULL,
    created_by                 VARCHAR(255),
    updated_at                 TIMESTAMP(6),
    updated_by                 VARCHAR(255)
);

-- Append-only audit log used to count usage against the two caps above.
-- Customer identifier is whatever the storefront supplies — typically the
-- logged-in storefront-account email, falling back to a hashed IP+UA pseudonym.
CREATE TABLE virtual_try_on_attempts (
    id                  UUID PRIMARY KEY,
    tenant_id           VARCHAR(255) NOT NULL,
    customer_identifier VARCHAR(512) NOT NULL,
    product_variant_id  UUID,
    attempted_at        TIMESTAMP(6) NOT NULL,
    success             BOOLEAN NOT NULL,
    error_message       TEXT,
    created_at          TIMESTAMP(6) NOT NULL,
    created_by          VARCHAR(255),
    updated_at          TIMESTAMP(6),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_vtoa_tenant_attempted ON virtual_try_on_attempts(tenant_id, attempted_at);
CREATE INDEX idx_vtoa_tenant_customer_attempted ON virtual_try_on_attempts(tenant_id, customer_identifier, attempted_at);
