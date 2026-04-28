CREATE TABLE IF NOT EXISTS courier_profiles (
    id               UUID         PRIMARY KEY,
    tenant_id        VARCHAR(255) NOT NULL,
    provider_code    VARCHAR(32)  NOT NULL,
    display_name     VARCHAR(128) NOT NULL,
    is_default       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    credentials_json TEXT,
    config_json      TEXT,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    CONSTRAINT ux_courier_profiles_tenant_provider_name
        UNIQUE (tenant_id, provider_code, display_name)
);

CREATE INDEX IF NOT EXISTS ix_courier_profiles_tenant ON courier_profiles (tenant_id);
