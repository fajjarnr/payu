-- GAP-005: API Key Management
-- Dedicated API key entity with hashing, rotation, revocation, and rate plan linkage

CREATE TABLE api_keys (
    id                    BIGSERIAL PRIMARY KEY,
    partner_id            BIGINT NOT NULL REFERENCES partners(id) ON DELETE CASCADE,
    key_prefix            VARCHAR(32) NOT NULL,
    key_hash              VARCHAR(64) NOT NULL UNIQUE,
    key_suffix            VARCHAR(8),
    name                  VARCHAR(255),
    status                VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    environment           VARCHAR(16) NOT NULL DEFAULT 'LIVE',
    rate_plan             VARCHAR(64) DEFAULT 'standard',
    rate_limit_rpm        INTEGER DEFAULT 100,
    rate_limit_rpd        INTEGER DEFAULT 10000,
    expires_at            TIMESTAMP,
    grace_period_ends_at  TIMESTAMP,
    last_used_at          TIMESTAMP,
    revoked_at            TIMESTAMP,
    revoked_reason        VARCHAR(255),
    tenant_id             VARCHAR(64) DEFAULT 'default',
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_key_status CHECK (status IN ('ACTIVE', 'ROTATED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT chk_key_env CHECK (environment IN ('LIVE', 'SANDBOX'))
);

-- Indexes for api_keys
CREATE INDEX idx_api_key_partner ON api_keys(partner_id);
CREATE INDEX idx_api_key_prefix ON api_keys(key_prefix);
CREATE INDEX idx_api_key_status ON api_keys(status);
CREATE INDEX idx_api_key_tenant ON api_keys(tenant_id);

-- Composite index for key validation
CREATE INDEX idx_api_key_active ON api_keys(key_hash, status);
