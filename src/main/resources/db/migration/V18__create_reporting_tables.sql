CREATE TABLE IF NOT EXISTS report_configurations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    widget_type VARCHAR(50),
    configuration_json TEXT,
    filter_preset_json TEXT,
    columns_json TEXT,
    schedule_cron VARCHAR(255),
    shared_with TEXT,
    export_formats VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT uk_report_configurations_code_tenant UNIQUE (code, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_report_configurations_category_active
    ON report_configurations (category, is_active);

CREATE TABLE IF NOT EXISTS report_execution_history (
    id UUID PRIMARY KEY,
    report_configuration_id UUID,
    report_name VARCHAR(255) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    output_format VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    row_count INTEGER,
    filters_json TEXT,
    error_message TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_report_execution_configuration FOREIGN KEY (report_configuration_id) REFERENCES report_configurations (id)
);

CREATE INDEX IF NOT EXISTS idx_report_execution_history_requested_at
    ON report_execution_history (requested_at DESC);