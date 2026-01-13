-- Create categories table
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
);

-- Create category_attributes table (join table)
CREATE TABLE category_attributes (
    category_id UUID NOT NULL,
    attribute_id UUID NOT NULL,
    PRIMARY KEY (category_id, attribute_id),
    CONSTRAINT fk_cat_attr_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_cat_attr_attribute FOREIGN KEY (attribute_id) REFERENCES product_attributes (id)
);

-- Add category_id to product_templates table
ALTER TABLE product_templates ADD COLUMN category_id UUID;
ALTER TABLE product_templates ADD CONSTRAINT fk_template_category FOREIGN KEY (category_id) REFERENCES categories (id);
