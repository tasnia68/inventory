-- V79: Wire batch + serial tracking through Goods Receipt and POS flows.
-- Adds capture columns on GRN items, batch/serial linkage on POS sale items.

-- 1. GRN items: capture batch metadata + serials at receive time
ALTER TABLE goods_receipt_note_items
    ADD COLUMN IF NOT EXISTS batch_number VARCHAR(128),
    ADD COLUMN IF NOT EXISTS manufacturing_date DATE,
    ADD COLUMN IF NOT EXISTS expiry_date DATE,
    ADD COLUMN IF NOT EXISTS batch_id UUID REFERENCES batches(id),
    ADD COLUMN IF NOT EXISTS serial_numbers TEXT;

CREATE INDEX IF NOT EXISTS idx_grn_items_batch ON goods_receipt_note_items(batch_id);

-- 2. POS sale items: link to specific batch (FEFO-selected) and carry sold serials
ALTER TABLE pos_sale_items
    ADD COLUMN IF NOT EXISTS batch_id UUID REFERENCES batches(id),
    ADD COLUMN IF NOT EXISTS serial_numbers TEXT;

CREATE INDEX IF NOT EXISTS idx_pos_sale_items_batch ON pos_sale_items(batch_id);

-- 3. Helpful index on batches for FEFO lookups
CREATE INDEX IF NOT EXISTS idx_batches_variant_expiry
    ON batches(product_variant_id, expiry_date)
    WHERE expiry_date IS NOT NULL;
