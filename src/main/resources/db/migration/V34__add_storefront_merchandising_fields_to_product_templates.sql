ALTER TABLE product_templates
    ADD COLUMN published_to_storefront BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN storefront_slug VARCHAR(255),
    ADD COLUMN storefront_title VARCHAR(255),
    ADD COLUMN storefront_description TEXT,
    ADD COLUMN storefront_sort_order INTEGER,
    ADD COLUMN storefront_seo_title VARCHAR(255),
    ADD COLUMN storefront_seo_description TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_product_templates_storefront_slug_per_tenant
    ON product_templates (tenant_id, storefront_slug)
    WHERE storefront_slug IS NOT NULL;

