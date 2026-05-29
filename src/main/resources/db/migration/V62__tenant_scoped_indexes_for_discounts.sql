-- Tenant-scoped composite indexes so per-tenant lookups (the only kind the
-- Hibernate `tenantFilter` ever issues) don't degrade to full-table scans
-- once a single shared table holds many tenants.
--
-- Each index leads with tenant_id because every query first filters by it.

-- discounts: list/filter by tenant + status + window
CREATE INDEX IF NOT EXISTS idx_discounts_tenant_status_starts
    ON discounts(tenant_id, status, starts_at);

CREATE INDEX IF NOT EXISTS idx_discounts_tenant_autoapply
    ON discounts(tenant_id, auto_apply) WHERE auto_apply = TRUE;

-- discount_codes: code lookup at checkout is the hottest path
CREATE INDEX IF NOT EXISTS idx_discount_codes_tenant_code
    ON discount_codes(tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_discount_codes_tenant_status
    ON discount_codes(tenant_id, status);

-- discount_redemptions: analytics + per-customer cap checks
CREATE INDEX IF NOT EXISTS idx_discount_redemptions_tenant_redeemed_at
    ON discount_redemptions(tenant_id, redeemed_at);

CREATE INDEX IF NOT EXISTS idx_discount_redemptions_tenant_customer_status
    ON discount_redemptions(tenant_id, customer_id, status);

CREATE INDEX IF NOT EXISTS idx_discount_redemptions_tenant_code_customer
    ON discount_redemptions(tenant_id, discount_code_id, customer_id);

-- gift_cards: balance lookup by code on storefront is hot
CREATE INDEX IF NOT EXISTS idx_gift_cards_tenant_code
    ON gift_cards(tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_gift_cards_tenant_status
    ON gift_cards(tenant_id, status);

-- gift_card_transactions: order-reversal lookup
CREATE INDEX IF NOT EXISTS idx_gct_tenant_sales_order_type
    ON gift_card_transactions(tenant_id, sales_order_id, type);

-- referral_codes: tenant-scoped code lookup
CREATE INDEX IF NOT EXISTS idx_referral_codes_tenant_code
    ON referral_codes(tenant_id, code);

-- referral_attributions: per-referee lookup
CREATE INDEX IF NOT EXISTS idx_referral_attributions_tenant_referee
    ON referral_attributions(tenant_id, referee_customer_id);
