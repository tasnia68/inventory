ALTER TABLE product_variants
    ADD COLUMN compare_at_price NUMERIC(19, 2),
    ADD COLUMN storefront_badge VARCHAR(255),
    ADD COLUMN storefront_featured BOOLEAN NOT NULL DEFAULT FALSE;
