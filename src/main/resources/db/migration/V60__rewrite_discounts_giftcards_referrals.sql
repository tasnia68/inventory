-- Ground-up rewrite of promotions/coupons/pricing-rules into a unified discounts
-- domain, plus first-class gift cards and a referral program.
-- Destructive: drops V25 tables and their data.

-- 1. Drop legacy tables (V25). Sales-order columns kept (TEXT cols are harmless).
DROP TABLE IF EXISTS promotion_redemptions CASCADE;
DROP TABLE IF EXISTS coupons CASCADE;
DROP TABLE IF EXISTS pricing_rules CASCADE;
DROP TABLE IF EXISTS promotions CASCADE;

-- 2. Add gift-card amount column to order tables.
ALTER TABLE sales_orders
    ADD COLUMN IF NOT EXISTS gift_card_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS applied_gift_card_codes TEXT,
    ADD COLUMN IF NOT EXISTS referral_code VARCHAR(64);

ALTER TABLE pos_sales
    ADD COLUMN IF NOT EXISTS gift_card_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS applied_gift_card_codes TEXT;

-- 3. discounts: master rule table.
CREATE TABLE discounts (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,                      -- DRAFT|SCHEDULED|ACTIVE|PAUSED|EXPIRED
    kind VARCHAR(48) NOT NULL,                        -- AMOUNT_OFF_ORDER|AMOUNT_OFF_PRODUCTS|FREE_SHIPPING|BOGO|BUNDLE|TIERED_AMOUNT_OFF_ORDER|TIERED_AMOUNT_OFF_PRODUCTS|REFERRAL_REFERRER|REFERRAL_REFEREE
    value_type VARCHAR(32),                           -- PERCENTAGE|FIXED_AMOUNT (null for FREE_SHIPPING & tiered)
    value NUMERIC(19, 6),                             -- null for tiered (see discount_tiers) and FREE_SHIPPING
    max_discount_amount NUMERIC(19, 6),               -- cap on percentage discounts
    applies_to_scope VARCHAR(32) NOT NULL DEFAULT 'ALL', -- ALL|PRODUCTS|CATEGORIES|COLLECTIONS
    customer_eligibility VARCHAR(48) NOT NULL DEFAULT 'ALL', -- ALL|FIRST_ORDER_ONLY|SPECIFIC_CUSTOMERS|CUSTOMER_GROUPS
    min_purchase_type VARCHAR(32) NOT NULL DEFAULT 'NONE',   -- NONE|AMOUNT|QUANTITY
    min_purchase_amount NUMERIC(19, 6),
    min_purchase_quantity NUMERIC(19, 6),
    usage_limit_total INTEGER,
    usage_limit_per_customer INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP,
    schedule_days_of_week VARCHAR(64),                -- CSV: MON,TUE,WED,...; null = every day
    schedule_start_time TIME,                         -- time-of-day window start; null = all day
    schedule_end_time TIME,                           -- time-of-day window end
    schedule_timezone VARCHAR(64),                    -- e.g. Asia/Dhaka; null = server TZ
    stackable BOOLEAN NOT NULL DEFAULT FALSE,
    combine_with_order_discounts BOOLEAN NOT NULL DEFAULT FALSE,
    combine_with_product_discounts BOOLEAN NOT NULL DEFAULT FALSE,
    combine_with_shipping_discounts BOOLEAN NOT NULL DEFAULT TRUE,
    exclusion_group VARCHAR(64),                      -- only one promo per group may apply
    priority INTEGER NOT NULL DEFAULT 100,
    auto_apply BOOLEAN NOT NULL DEFAULT FALSE,        -- true = no code needed
    sales_channel VARCHAR(32) NOT NULL DEFAULT 'ALL', -- ONLINE|POS|ALL
    -- BOGO fields
    bogo_buy_quantity NUMERIC(19, 6),
    bogo_get_quantity NUMERIC(19, 6),
    bogo_get_value_type VARCHAR(32),                  -- PERCENTAGE|FIXED_AMOUNT (typically PERCENTAGE 100)
    bogo_get_value NUMERIC(19, 6),
    -- Bundle fields
    bundle_quantity NUMERIC(19, 6),
    bundle_price NUMERIC(19, 6),
    -- Free shipping fields
    free_shipping_max_amount NUMERIC(19, 6),          -- cap; null = full waive
    free_shipping_countries TEXT,                     -- CSV ISO codes; null = all
    CONSTRAINT chk_discounts_status CHECK (status IN ('DRAFT','SCHEDULED','ACTIVE','PAUSED','EXPIRED')),
    CONSTRAINT chk_discounts_kind CHECK (kind IN ('AMOUNT_OFF_ORDER','AMOUNT_OFF_PRODUCTS','FREE_SHIPPING','BOGO','BUNDLE','TIERED_AMOUNT_OFF_ORDER','TIERED_AMOUNT_OFF_PRODUCTS','REFERRAL_REFERRER','REFERRAL_REFEREE')),
    CONSTRAINT chk_discounts_channel CHECK (sales_channel IN ('ONLINE','POS','ALL'))
);
CREATE INDEX idx_discounts_tenant ON discounts(tenant_id);
CREATE INDEX idx_discounts_status_starts ON discounts(status, starts_at);
CREATE INDEX idx_discounts_auto_apply ON discounts(auto_apply) WHERE auto_apply = TRUE;

-- 4. discount_codes: redeemable codes pointing at a discount.
CREATE TABLE discount_codes (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    discount_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,                      -- ACTIVE|DISABLED|EXPIRED
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    max_redemptions INTEGER,
    max_redemptions_per_customer INTEGER,
    redeemed_count INTEGER NOT NULL DEFAULT 0,
    notes TEXT,
    CONSTRAINT uq_discount_codes_code_tenant UNIQUE (code, tenant_id),
    CONSTRAINT fk_discount_codes_discount FOREIGN KEY (discount_id) REFERENCES discounts(id) ON DELETE CASCADE,
    CONSTRAINT chk_discount_codes_status CHECK (status IN ('ACTIVE','DISABLED','EXPIRED'))
);
CREATE INDEX idx_discount_codes_discount ON discount_codes(discount_id);
CREATE INDEX idx_discount_codes_status ON discount_codes(status);

-- 5. discount_tiers: breakpoint table for tiered discounts.
CREATE TABLE discount_tiers (
    id UUID PRIMARY KEY,
    discount_id UUID NOT NULL,
    min_subtotal NUMERIC(19, 6),
    min_quantity NUMERIC(19, 6),
    value_type VARCHAR(32) NOT NULL,                  -- PERCENTAGE|FIXED_AMOUNT
    value NUMERIC(19, 6) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_discount_tiers_discount FOREIGN KEY (discount_id) REFERENCES discounts(id) ON DELETE CASCADE,
    CONSTRAINT chk_discount_tiers_value_type CHECK (value_type IN ('PERCENTAGE','FIXED_AMOUNT'))
);
CREATE INDEX idx_discount_tiers_discount ON discount_tiers(discount_id);

-- 6. discount_product_inclusions: product/category include/exclude lists.
CREATE TABLE discount_product_inclusions (
    id UUID PRIMARY KEY,
    discount_id UUID NOT NULL,
    scope VARCHAR(32) NOT NULL,                       -- PRODUCT|VARIANT|CATEGORY
    entity_id UUID NOT NULL,
    mode VARCHAR(16) NOT NULL,                        -- INCLUDE|EXCLUDE
    CONSTRAINT fk_dpi_discount FOREIGN KEY (discount_id) REFERENCES discounts(id) ON DELETE CASCADE,
    CONSTRAINT chk_dpi_scope CHECK (scope IN ('PRODUCT','VARIANT','CATEGORY')),
    CONSTRAINT chk_dpi_mode CHECK (mode IN ('INCLUDE','EXCLUDE')),
    CONSTRAINT uq_dpi_unique UNIQUE (discount_id, scope, entity_id, mode)
);
CREATE INDEX idx_dpi_discount ON discount_product_inclusions(discount_id);
CREATE INDEX idx_dpi_entity ON discount_product_inclusions(scope, entity_id);

-- 7. discount_customer_inclusions: customer/group include/exclude lists.
CREATE TABLE discount_customer_inclusions (
    id UUID PRIMARY KEY,
    discount_id UUID NOT NULL,
    scope VARCHAR(32) NOT NULL,                       -- CUSTOMER|CUSTOMER_CATEGORY
    entity_id VARCHAR(64) NOT NULL,                   -- UUID for CUSTOMER, enum string for CUSTOMER_CATEGORY
    mode VARCHAR(16) NOT NULL,                        -- INCLUDE|EXCLUDE
    CONSTRAINT fk_dci_discount FOREIGN KEY (discount_id) REFERENCES discounts(id) ON DELETE CASCADE,
    CONSTRAINT chk_dci_scope CHECK (scope IN ('CUSTOMER','CUSTOMER_CATEGORY')),
    CONSTRAINT chk_dci_mode CHECK (mode IN ('INCLUDE','EXCLUDE')),
    CONSTRAINT uq_dci_unique UNIQUE (discount_id, scope, entity_id, mode)
);
CREATE INDEX idx_dci_discount ON discount_customer_inclusions(discount_id);

-- 8. discount_redemptions: audit log of applied discounts.
CREATE TABLE discount_redemptions (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    discount_id UUID NOT NULL,
    discount_code_id UUID,
    customer_id UUID,
    sales_order_id UUID,
    pos_sale_id UUID,
    sales_channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,                      -- APPLIED|REJECTED|FLAGGED|REVERSED
    discount_amount NUMERIC(19, 6) NOT NULL,
    order_subtotal NUMERIC(19, 6),
    abuse_flag BOOLEAN NOT NULL DEFAULT FALSE,
    abuse_reason TEXT,
    redeemed_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_dr_discount FOREIGN KEY (discount_id) REFERENCES discounts(id),
    CONSTRAINT fk_dr_code FOREIGN KEY (discount_code_id) REFERENCES discount_codes(id),
    CONSTRAINT fk_dr_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_dr_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders(id),
    CONSTRAINT fk_dr_pos FOREIGN KEY (pos_sale_id) REFERENCES pos_sales(id)
);
CREATE INDEX idx_dr_discount ON discount_redemptions(discount_id);
CREATE INDEX idx_dr_code ON discount_redemptions(discount_code_id);
CREATE INDEX idx_dr_customer ON discount_redemptions(customer_id);
CREATE INDEX idx_dr_redeemed_at ON discount_redemptions(redeemed_at);

-- 9. gift_cards: balance-bearing redeemable cards.
CREATE TABLE gift_cards (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,                      -- ACTIVE|DISABLED|EXPIRED|REDEEMED
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    initial_balance NUMERIC(19, 6) NOT NULL,
    current_balance NUMERIC(19, 6) NOT NULL,
    issued_to_customer_id UUID,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',     -- MANUAL|PURCHASED|REFUND|REFERRAL_REWARD|PROMOTION
    notes TEXT,
    CONSTRAINT uq_gift_cards_code_tenant UNIQUE (code, tenant_id),
    CONSTRAINT fk_gift_cards_customer FOREIGN KEY (issued_to_customer_id) REFERENCES customers(id),
    CONSTRAINT chk_gift_cards_status CHECK (status IN ('ACTIVE','DISABLED','EXPIRED','REDEEMED'))
);
CREATE INDEX idx_gift_cards_tenant ON gift_cards(tenant_id);
CREATE INDEX idx_gift_cards_status ON gift_cards(status);
CREATE INDEX idx_gift_cards_customer ON gift_cards(issued_to_customer_id);

-- 10. gift_card_transactions: append-only ledger.
CREATE TABLE gift_card_transactions (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    gift_card_id UUID NOT NULL,
    type VARCHAR(32) NOT NULL,                        -- ISSUE|REDEEM|REFUND|ADJUSTMENT|EXPIRE|REVERSAL
    amount NUMERIC(19, 6) NOT NULL,                   -- signed: + adds balance, - debits
    balance_after NUMERIC(19, 6) NOT NULL,
    sales_order_id UUID,
    pos_sale_id UUID,
    reference VARCHAR(255),
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_gct_card FOREIGN KEY (gift_card_id) REFERENCES gift_cards(id) ON DELETE CASCADE,
    CONSTRAINT fk_gct_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders(id),
    CONSTRAINT fk_gct_pos FOREIGN KEY (pos_sale_id) REFERENCES pos_sales(id),
    CONSTRAINT chk_gct_type CHECK (type IN ('ISSUE','REDEEM','REFUND','ADJUSTMENT','EXPIRE','REVERSAL'))
);
CREATE INDEX idx_gct_card ON gift_card_transactions(gift_card_id);
CREATE INDEX idx_gct_occurred_at ON gift_card_transactions(occurred_at);

-- 11. referral_programs: one-per-tenant config wiring two Discounts.
CREATE TABLE referral_programs (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,                      -- ACTIVE|PAUSED|ENDED
    referrer_discount_id UUID,                        -- discount the referrer gets after qualification
    referee_discount_id UUID,                         -- discount the referee gets on signup or first order
    reward_trigger VARCHAR(48) NOT NULL,              -- ON_REFEREE_SIGNUP|ON_REFEREE_FIRST_ORDER|ON_REFEREE_NTH_ORDER
    min_referee_order_amount NUMERIC(19, 6),
    referee_nth_order INTEGER,                        -- used when trigger = ON_REFEREE_NTH_ORDER
    max_referrals_per_customer INTEGER,
    description TEXT,
    CONSTRAINT uq_referral_programs_tenant UNIQUE (tenant_id),
    CONSTRAINT fk_rp_referrer_discount FOREIGN KEY (referrer_discount_id) REFERENCES discounts(id),
    CONSTRAINT fk_rp_referee_discount FOREIGN KEY (referee_discount_id) REFERENCES discounts(id),
    CONSTRAINT chk_rp_status CHECK (status IN ('ACTIVE','PAUSED','ENDED'))
);

-- 12. referral_codes: one per referring customer.
CREATE TABLE referral_codes (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    program_id UUID NOT NULL,
    customer_id UUID NOT NULL,                        -- the referrer
    code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',     -- ACTIVE|DISABLED
    referees_count INTEGER NOT NULL DEFAULT 0,
    rewards_paid_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_referral_codes_code_tenant UNIQUE (code, tenant_id),
    CONSTRAINT uq_referral_codes_customer_program UNIQUE (program_id, customer_id),
    CONSTRAINT fk_rc_program FOREIGN KEY (program_id) REFERENCES referral_programs(id) ON DELETE CASCADE,
    CONSTRAINT fk_rc_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);
CREATE INDEX idx_rc_program ON referral_codes(program_id);
CREATE INDEX idx_rc_customer ON referral_codes(customer_id);

-- 13. referral_attributions: tracks referee -> referrer link and reward state.
CREATE TABLE referral_attributions (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    referral_code_id UUID NOT NULL,
    referee_customer_id UUID NOT NULL,
    referee_order_id UUID,                            -- order that satisfied the trigger
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',    -- PENDING|QUALIFIED|REWARDED|REJECTED
    qualified_at TIMESTAMP,
    rewarded_at TIMESTAMP,
    referrer_reward_amount NUMERIC(19, 6),
    referee_reward_amount NUMERIC(19, 6),
    notes TEXT,
    CONSTRAINT uq_ra_referee UNIQUE (tenant_id, referee_customer_id),
    CONSTRAINT fk_ra_code FOREIGN KEY (referral_code_id) REFERENCES referral_codes(id),
    CONSTRAINT fk_ra_referee FOREIGN KEY (referee_customer_id) REFERENCES customers(id),
    CONSTRAINT fk_ra_order FOREIGN KEY (referee_order_id) REFERENCES sales_orders(id),
    CONSTRAINT chk_ra_status CHECK (status IN ('PENDING','QUALIFIED','REWARDED','REJECTED'))
);
CREATE INDEX idx_ra_code ON referral_attributions(referral_code_id);
CREATE INDEX idx_ra_referee ON referral_attributions(referee_customer_id);
