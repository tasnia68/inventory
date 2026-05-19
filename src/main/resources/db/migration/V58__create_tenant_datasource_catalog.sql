-- Control-plane catalog for hybrid ("Bridge") multi-tenancy.
--
-- Most tenants stay on the shared schema (single Postgres + the tenant_id
-- discriminator @Filter on BaseEntity). A row here ONLY exists for a tenant
-- that has been explicitly configured. The routing layer treats the ABSENCE
-- of a row as mode=SHARED/status=ACTIVE — i.e. the safe default is implicit,
-- so a missing/!-misread row can never accidentally route to a dedicated DB.
--
-- This table is NOT tenant-scoped (no tenant_id @Filter): it is global
-- control-plane data read before a tenant's DB is even known. Credentials
-- are stored as AES-GCM ciphertext (see CredentialCipher); the KEK lives in
-- the environment, never in this table.
CREATE TABLE tenant_datasource (
    tenant_id          VARCHAR(255) PRIMARY KEY,
    mode               VARCHAR(16)  NOT NULL DEFAULT 'SHARED'
                         CHECK (mode IN ('SHARED', 'DEDICATED')),
    status             VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'
                         CHECK (status IN ('PENDING', 'MIGRATING', 'ACTIVE', 'DISABLED')),

    -- Encrypted connection details (only populated when mode = DEDICATED).
    jdbc_url_enc       TEXT,
    jdbc_username_enc  TEXT,
    jdbc_password_enc  TEXT,
    key_id             VARCHAR(64),

    -- Per-tenant pool sizing (NULL = use routing defaults).
    pool_max_size      INTEGER,
    pool_min_idle      INTEGER,
    idle_timeout_ms    BIGINT,

    -- Schema-baseline gate: a dedicated DB is only routable when this equals
    -- the application's Flyway baseline.
    flyway_version     VARCHAR(32),

    provisioned_at     TIMESTAMP(6),
    last_migrated_at   TIMESTAMP(6),
    last_routed_at     TIMESTAMP(6),
    last_error         TEXT,

    created_at         TIMESTAMP(6) NOT NULL,
    created_by         VARCHAR(255),
    updated_at         TIMESTAMP(6),
    updated_by         VARCHAR(255)
);

CREATE INDEX idx_tenant_datasource_mode_status ON tenant_datasource (mode, status);
