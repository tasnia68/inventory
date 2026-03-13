-- V12: Product version history and category permissions

CREATE TABLE IF NOT EXISTS product_variant_versions (
    id UUID PRIMARY KEY,
    product_variant_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    snapshot TEXT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_product_variant_versions_variant
    ON product_variant_versions (product_variant_id, version_number);

CREATE TABLE IF NOT EXISTS category_permissions (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    role_id UUID NOT NULL,
    can_view BOOLEAN NOT NULL DEFAULT TRUE,
    can_edit BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_category_permissions_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE,
    CONSTRAINT fk_category_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_category_permissions_category_role_tenant
    ON category_permissions (category_id, role_id, tenant_id);