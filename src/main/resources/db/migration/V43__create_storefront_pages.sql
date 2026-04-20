CREATE TABLE IF NOT EXISTS storefront_pages (
    id UUID NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    created_by VARCHAR(255),
    updated_at TIMESTAMP(6),
    updated_by VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    body TEXT,
    published BOOLEAN NOT NULL DEFAULT false
);
