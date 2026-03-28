CREATE TABLE storefront_publish_versions (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    version_number INTEGER NOT NULL,
    snapshot_json TEXT NOT NULL,
    note TEXT,
    published_at TIMESTAMP NOT NULL,
    restored_from_version_number INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX uk_storefront_publish_versions_tenant_version
    ON storefront_publish_versions (tenant_id, version_number);

CREATE INDEX idx_storefront_publish_versions_tenant_published_at
    ON storefront_publish_versions (tenant_id, published_at DESC);

