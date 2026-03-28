CREATE TABLE storefront_accounts (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers(id),
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    address TEXT,
    email_verified_at TIMESTAMP,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX uk_storefront_accounts_tenant_email
    ON storefront_accounts (tenant_id, LOWER(email));

CREATE UNIQUE INDEX uk_storefront_accounts_tenant_customer
    ON storefront_accounts (tenant_id, customer_id);

CREATE TABLE storefront_login_challenges (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(16) NOT NULL,
    magic_token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX uk_storefront_login_challenges_token
    ON storefront_login_challenges (tenant_id, magic_token);

CREATE INDEX idx_storefront_login_challenges_email
    ON storefront_login_challenges (tenant_id, LOWER(email), expires_at DESC);

CREATE TABLE storefront_account_sessions (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    storefront_account_id UUID NOT NULL REFERENCES storefront_accounts(id) ON DELETE CASCADE,
    session_token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX uk_storefront_account_sessions_token
    ON storefront_account_sessions (tenant_id, session_token);

CREATE INDEX idx_storefront_account_sessions_account
    ON storefront_account_sessions (tenant_id, storefront_account_id, expires_at DESC);
