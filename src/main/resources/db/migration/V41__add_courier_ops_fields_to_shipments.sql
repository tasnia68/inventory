ALTER TABLE shipments
    ADD COLUMN courier_provider VARCHAR(120),
    ADD COLUMN courier_service VARCHAR(120),
    ADD COLUMN courier_dispatch_status VARCHAR(40) NOT NULL DEFAULT 'UNASSIGNED',
    ADD COLUMN courier_reference VARCHAR(255),
    ADD COLUMN cash_on_delivery_amount NUMERIC(19, 6),
    ADD COLUMN delivery_fee NUMERIC(19, 6),
    ADD COLUMN last_courier_event TEXT,
    ADD COLUMN last_courier_sync_at TIMESTAMP,
    ADD COLUMN pickup_requested_at TIMESTAMP,
    ADD COLUMN picked_up_at TIMESTAMP,
    ADD COLUMN out_for_delivery_at TIMESTAMP;
