-- Append-only audit of every datasource routing decision. Used to prove the
-- zero-leakage property: each connection acquisition records which tenant was
-- resolved, which DB host it was routed to, and the deciding principal/path.
-- DENIED rows capture fail-closed events (dedicated DB down / migration behind
-- / disabled) — these must NEVER coincide with a shared-DB fallback.
--
-- Control-plane (not tenant-scoped). Never updated or deleted by the app.
CREATE TABLE tenant_routing_audit (
    id            UUID PRIMARY KEY,
    tenant_id     VARCHAR(255) NOT NULL,
    decision      VARCHAR(16)  NOT NULL
                    CHECK (decision IN ('SHARED', 'DEDICATED', 'DENIED')),
    reason        TEXT,
    jdbc_url_host VARCHAR(255),
    principal     VARCHAR(255),
    request_path  VARCHAR(512),
    created_at    TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_tenant_routing_audit_tenant_time
    ON tenant_routing_audit (tenant_id, created_at);
