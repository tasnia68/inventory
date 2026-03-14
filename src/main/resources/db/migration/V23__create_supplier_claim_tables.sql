CREATE TABLE supplier_claims (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    claim_number VARCHAR(255) NOT NULL UNIQUE,
    goods_receipt_note_id UUID NOT NULL,
    supplier_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    damage_record_id UUID,
    supplier_return_id UUID,
    status VARCHAR(50) NOT NULL,
    claim_type VARCHAR(50) NOT NULL,
    reason TEXT,
    notes TEXT,
    claimed_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    CONSTRAINT fk_supplier_claim_grn FOREIGN KEY (goods_receipt_note_id) REFERENCES goods_receipt_notes(id),
    CONSTRAINT fk_supplier_claim_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_supplier_claim_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_supplier_claim_damage_record FOREIGN KEY (damage_record_id) REFERENCES damage_records(id),
    CONSTRAINT fk_supplier_claim_supplier_return FOREIGN KEY (supplier_return_id) REFERENCES supplier_returns(id)
);

CREATE UNIQUE INDEX idx_supplier_claims_supplier_return ON supplier_claims(supplier_return_id) WHERE supplier_return_id IS NOT NULL;
CREATE INDEX idx_supplier_claims_tenant ON supplier_claims(tenant_id);
CREATE INDEX idx_supplier_claims_grn ON supplier_claims(goods_receipt_note_id);
CREATE INDEX idx_supplier_claims_damage_record ON supplier_claims(damage_record_id);
CREATE INDEX idx_supplier_claims_status ON supplier_claims(status);

CREATE TABLE supplier_claim_items (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    supplier_claim_id UUID NOT NULL,
    goods_receipt_note_item_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    quantity NUMERIC(19, 6) NOT NULL,
    unit_cost NUMERIC(19, 6),
    claimed_amount NUMERIC(19, 6),
    reason TEXT,
    CONSTRAINT fk_supplier_claim_item_claim FOREIGN KEY (supplier_claim_id) REFERENCES supplier_claims(id) ON DELETE CASCADE,
    CONSTRAINT fk_supplier_claim_item_grn_item FOREIGN KEY (goods_receipt_note_item_id) REFERENCES goods_receipt_note_items(id),
    CONSTRAINT fk_supplier_claim_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);

CREATE INDEX idx_supplier_claim_items_claim ON supplier_claim_items(supplier_claim_id);
CREATE INDEX idx_supplier_claim_items_grn_item ON supplier_claim_items(goods_receipt_note_item_id);