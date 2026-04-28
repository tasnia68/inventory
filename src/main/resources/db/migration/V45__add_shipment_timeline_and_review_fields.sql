ALTER TABLE shipments
    ADD COLUMN delivery_review_status VARCHAR(40) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN delivery_review_reason TEXT,
    ADD COLUMN delivery_review_requested_at TIMESTAMP,
    ADD COLUMN delivery_review_resolved_at TIMESTAMP,
    ADD COLUMN proof_of_delivery_url TEXT,
    ADD COLUMN proof_of_delivery_recipient_name VARCHAR(255),
    ADD COLUMN proof_of_delivery_captured_at TIMESTAMP;

CREATE TABLE shipment_timeline_events (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_source VARCHAR(100),
    summary VARCHAR(255) NOT NULL,
    details TEXT,
    event_at TIMESTAMP NOT NULL,
    shipment_status VARCHAR(50),
    courier_dispatch_status VARCHAR(40),
    delivery_review_status VARCHAR(40),
    customer_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_shipment_timeline_event_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id) ON DELETE CASCADE
);

CREATE INDEX idx_shipment_timeline_events_shipment_event_at
    ON shipment_timeline_events (shipment_id, event_at);