CREATE TABLE accounting_periods (
    id          UUID PRIMARY KEY,
    tenant_id   VARCHAR(255) NOT NULL,
    period_start DATE NOT NULL,
    period_end   DATE NOT NULL,
    status      VARCHAR(16) NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    closed_at   TIMESTAMP(6),
    closed_by   VARCHAR(255),
    reopened_at TIMESTAMP(6),
    reopened_by VARCHAR(255),
    notes       TEXT,
    created_at  TIMESTAMP(6) NOT NULL,
    created_by  VARCHAR(255),
    updated_at  TIMESTAMP(6),
    updated_by  VARCHAR(255),
    CONSTRAINT chk_accounting_period_range CHECK (period_end >= period_start)
);

CREATE INDEX idx_accounting_periods_tenant_status ON accounting_periods(tenant_id, status);
CREATE INDEX idx_accounting_periods_range ON accounting_periods(tenant_id, period_start, period_end);
