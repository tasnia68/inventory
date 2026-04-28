CREATE TABLE IF NOT EXISTS inbound_webhook_events (
    id                 UUID         PRIMARY KEY,
    tenant_id          VARCHAR(255),
    source             VARCHAR(32)  NOT NULL,
    external_event_id  VARCHAR(128),
    topic              VARCHAR(128),
    payload            TEXT         NOT NULL,
    signature          VARCHAR(512),
    status             VARCHAR(32)  NOT NULL,
    error              TEXT,
    sales_order_id     UUID,
    received_at        TIMESTAMP    NOT NULL,
    processed_at       TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP,
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_inbound_webhook_events_source_eventid
    ON inbound_webhook_events (source, external_event_id)
    WHERE external_event_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_inbound_webhook_events_tenant
    ON inbound_webhook_events (tenant_id);
CREATE INDEX IF NOT EXISTS ix_inbound_webhook_events_status
    ON inbound_webhook_events (status);
