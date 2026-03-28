ALTER TABLE categories
    ADD COLUMN published_to_storefront BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN storefront_slug VARCHAR(255),
    ADD COLUMN storefront_title VARCHAR(255),
    ADD COLUMN storefront_description TEXT,
    ADD COLUMN storefront_sort_order INTEGER;

CREATE UNIQUE INDEX IF NOT EXISTS uk_categories_storefront_slug_per_tenant
    ON categories (tenant_id, storefront_slug)
    WHERE storefront_slug IS NOT NULL;

