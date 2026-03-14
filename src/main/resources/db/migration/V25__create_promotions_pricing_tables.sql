ALTER TABLE sales_orders
ADD COLUMN IF NOT EXISTS subtotal_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS sales_channel VARCHAR(50) NOT NULL DEFAULT 'SALES_ORDER',
ADD COLUMN IF NOT EXISTS applied_coupon_codes TEXT;

ALTER TABLE sales_order_items
ADD COLUMN IF NOT EXISTS base_unit_price NUMERIC(19, 6),
ADD COLUMN IF NOT EXISTS line_discount NUMERIC(19, 6) NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS applied_promotion_codes TEXT;

ALTER TABLE pos_sales
ADD COLUMN IF NOT EXISTS applied_coupon_codes TEXT;

CREATE TABLE promotions (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    scope VARCHAR(50) NOT NULL,
    sales_channel VARCHAR(50),
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP,
    stackable BOOLEAN NOT NULL DEFAULT FALSE,
    coupon_required BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 100,
    exclusion_group VARCHAR(255),
    discount_value NUMERIC(19, 6),
    max_discount_amount NUMERIC(19, 6),
    min_order_amount NUMERIC(19, 6),
    min_quantity NUMERIC(19, 6),
    bundle_quantity NUMERIC(19, 6),
    bundle_price NUMERIC(19, 6),
    buy_quantity NUMERIC(19, 6),
    get_quantity NUMERIC(19, 6),
    usage_limit_total INTEGER,
    usage_limit_per_customer INTEGER,
    customer_category VARCHAR(50),
    warehouse_id UUID,
    terminal_id UUID,
    category_id UUID,
    product_variant_id UUID,
    CONSTRAINT uq_promotions_code_tenant UNIQUE (code, tenant_id),
    CONSTRAINT fk_promotions_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_promotions_terminal FOREIGN KEY (terminal_id) REFERENCES pos_terminals(id),
    CONSTRAINT fk_promotions_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_promotions_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);

CREATE INDEX idx_promotions_status ON promotions(status);
CREATE INDEX idx_promotions_starts_at ON promotions(starts_at);
CREATE INDEX idx_promotions_tenant ON promotions(tenant_id);

CREATE TABLE coupons (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    promotion_id UUID NOT NULL,
    code VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP,
    max_redemptions_total INTEGER,
    max_redemptions_per_customer INTEGER,
    redeemed_count INTEGER NOT NULL DEFAULT 0,
    notes TEXT,
    CONSTRAINT uq_coupons_code_tenant UNIQUE (code, tenant_id),
    CONSTRAINT fk_coupons_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE CASCADE
);

CREATE INDEX idx_coupons_promotion ON coupons(promotion_id);
CREATE INDEX idx_coupons_status ON coupons(status);

CREATE TABLE pricing_rules (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    adjustment_type VARCHAR(50) NOT NULL,
    adjustment_value NUMERIC(19, 6) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 100,
    sales_channel VARCHAR(50),
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP,
    min_quantity NUMERIC(19, 6),
    customer_category VARCHAR(50),
    customer_id UUID,
    warehouse_id UUID,
    terminal_id UUID,
    category_id UUID,
    product_variant_id UUID,
    notes TEXT,
    CONSTRAINT uq_pricing_rules_code_tenant UNIQUE (code, tenant_id),
    CONSTRAINT fk_pricing_rules_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_pricing_rules_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_pricing_rules_terminal FOREIGN KEY (terminal_id) REFERENCES pos_terminals(id),
    CONSTRAINT fk_pricing_rules_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_pricing_rules_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);

CREATE INDEX idx_pricing_rules_status ON pricing_rules(status);
CREATE INDEX idx_pricing_rules_tenant ON pricing_rules(tenant_id);

CREATE TABLE promotion_redemptions (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    promotion_id UUID NOT NULL,
    coupon_id UUID,
    customer_id UUID,
    sales_order_id UUID,
    pos_sale_id UUID,
    sales_channel VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    discount_amount NUMERIC(19, 6) NOT NULL,
    order_subtotal NUMERIC(19, 6),
    reference_number VARCHAR(255),
    abuse_flag BOOLEAN NOT NULL DEFAULT FALSE,
    abuse_reason TEXT,
    redeemed_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_promotion_redemption_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    CONSTRAINT fk_promotion_redemption_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    CONSTRAINT fk_promotion_redemption_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_promotion_redemption_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders(id),
    CONSTRAINT fk_promotion_redemption_pos_sale FOREIGN KEY (pos_sale_id) REFERENCES pos_sales(id)
);

CREATE INDEX idx_promotion_redemptions_promotion ON promotion_redemptions(promotion_id);
CREATE INDEX idx_promotion_redemptions_coupon ON promotion_redemptions(coupon_id);
CREATE INDEX idx_promotion_redemptions_customer ON promotion_redemptions(customer_id);
CREATE INDEX idx_promotion_redemptions_redeemed_at ON promotion_redemptions(redeemed_at);