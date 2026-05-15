-- Audit trail for PR → PO conversion: when a Purchase Requisition is converted
-- into a Purchase Order via /purchase-requisitions/{id}/convert-to-po, we stamp
-- the resulting PO id and timestamp on the PR so re-conversion is blocked and
-- the lineage is queryable.

ALTER TABLE purchase_requisitions
    ADD COLUMN converted_at TIMESTAMP(6);

ALTER TABLE purchase_requisitions
    ADD COLUMN converted_purchase_order_id UUID;
