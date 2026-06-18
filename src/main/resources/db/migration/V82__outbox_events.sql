-- Transactional outbox: durable, retryable side effects (Shopify pushes, tracking
-- emails, fulfillment write-back) driven by OutboxRelay. Cross-tenant by design
-- (no tenant FK / filter); the relay re-establishes tenant context per row.
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY,
    tenant_id       VARCHAR(255) NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    payload         TEXT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    attempts        INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP,
    last_error      TEXT,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP
);

CREATE INDEX idx_outbox_dispatch ON outbox_events (status, next_attempt_at);
