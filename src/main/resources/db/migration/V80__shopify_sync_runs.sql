-- Journal for chunked / resumable Shopify sync runs.
CREATE TABLE shopify_sync_runs (
    id              UUID PRIMARY KEY,
    tenant_id       VARCHAR(255) NOT NULL,
    sync_type       VARCHAR(32)  NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    cursor          TEXT,
    incremental     BOOLEAN      NOT NULL DEFAULT FALSE,
    query_filter    TEXT,
    next_watermark  VARCHAR(64),
    pages_processed INTEGER      NOT NULL DEFAULT 0,
    result_json     TEXT,
    message         TEXT,
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX idx_shopify_sync_runs_tenant ON shopify_sync_runs (tenant_id, created_at DESC);
CREATE INDEX idx_shopify_sync_runs_type_status ON shopify_sync_runs (tenant_id, sync_type, status);
