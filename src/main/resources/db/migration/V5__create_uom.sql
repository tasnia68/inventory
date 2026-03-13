-- Create units_of_measure table
CREATE TABLE IF NOT EXISTS units_of_measure (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL CHECK (category IN ('WEIGHT', 'VOLUME', 'LENGTH', 'QUANTITY', 'TIME')),
    is_base BOOLEAN NOT NULL DEFAULT FALSE,
    conversion_factor NUMERIC(19, 6) NOT NULL,
    CONSTRAINT uq_uom_code_tenant UNIQUE (code, tenant_id)
);

-- Add uom_id to product_templates table
ALTER TABLE product_templates ADD COLUMN IF NOT EXISTS uom_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_template_uom'
          AND table_name = 'product_templates'
    ) THEN
        ALTER TABLE product_templates
            ADD CONSTRAINT fk_template_uom FOREIGN KEY (uom_id) REFERENCES units_of_measure (id);
    END IF;
END $$;
