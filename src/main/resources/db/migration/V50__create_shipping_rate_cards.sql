CREATE TABLE IF NOT EXISTS shipping_rate_cards (
    id                 UUID           PRIMARY KEY,
    tenant_id          VARCHAR(255)   NOT NULL,
    courier_profile_id UUID           NOT NULL REFERENCES courier_profiles(id) ON DELETE CASCADE,
    zone               VARCHAR(32)    NOT NULL,
    customer_charge    NUMERIC(19,6)  NOT NULL,
    courier_cost       NUMERIC(19,6)  NOT NULL,
    cod_fee_percent    NUMERIC(7,4)   NOT NULL DEFAULT 0,
    weight_kg_included NUMERIC(7,3),
    per_kg_overage     NUMERIC(19,6),
    effective_from     TIMESTAMP,
    effective_to       TIMESTAMP,
    created_at         TIMESTAMP      NOT NULL,
    updated_at         TIMESTAMP,
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS ix_shipping_rate_cards_profile_zone
    ON shipping_rate_cards (courier_profile_id, zone);
CREATE INDEX IF NOT EXISTS ix_shipping_rate_cards_tenant
    ON shipping_rate_cards (tenant_id);
