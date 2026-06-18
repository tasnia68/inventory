-- Tracks when the customer was emailed the courier tracking link for a shipment,
-- so the central tracking-email handler stays idempotent (one email per shipment).
ALTER TABLE shipments ADD COLUMN tracking_notified_at TIMESTAMP;
