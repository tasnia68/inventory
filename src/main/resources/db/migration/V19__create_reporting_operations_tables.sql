CREATE TABLE IF NOT EXISTS report_shares (
    id UUID PRIMARY KEY,
    report_configuration_id UUID NOT NULL,
    shared_with_user_id UUID NOT NULL,
    access_level VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_report_shares_configuration FOREIGN KEY (report_configuration_id) REFERENCES report_configurations (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_shares_user FOREIGN KEY (shared_with_user_id) REFERENCES users (id),
    CONSTRAINT uk_report_shares_configuration_user UNIQUE (report_configuration_id, shared_with_user_id, tenant_id)
);

CREATE TABLE IF NOT EXISTS data_import_history (
    id UUID PRIMARY KEY,
    dataset VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    total_records INTEGER,
    processed_records INTEGER,
    successful_records INTEGER,
    failed_records INTEGER,
    validation_errors TEXT,
    summary_message TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_data_import_history_requested_at
    ON data_import_history (requested_at DESC);

CREATE TABLE IF NOT EXISTS webhook_endpoints (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    subscribed_events TEXT NOT NULL,
    secret_key VARCHAR(255),
    headers_json TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id UUID PRIMARY KEY,
    webhook_endpoint_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payload_json TEXT,
    response_status INTEGER,
    response_body TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_webhook_deliveries_endpoint FOREIGN KEY (webhook_endpoint_id) REFERENCES webhook_endpoints (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_created_at
    ON webhook_deliveries (created_at DESC);