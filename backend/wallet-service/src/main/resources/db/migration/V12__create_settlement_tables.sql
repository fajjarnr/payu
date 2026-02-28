-- V12: Create settlement and revenue split tables
-- Supports E-12: Settlement & Financial Operations (GAP-003, GAP-013)

-- 1. Settlement batches for daily settlement processing
CREATE TABLE settlement_batches (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id      VARCHAR(128) NOT NULL,
    settlement_date DATE NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'IDR',
    total_amount    DECIMAL(19,4) NOT NULL DEFAULT 0,
    fee_amount      DECIMAL(19,4) DEFAULT 0,
    net_amount      DECIMAL(19,4) NOT NULL DEFAULT 0,
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reconciliation_report TEXT,
    failure_reason  VARCHAR(512),
    processed_by    VARCHAR(100),
    processed_at    TIMESTAMP,
    tenant_id       VARCHAR(64),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_settlement_partner ON settlement_batches (partner_id);
CREATE INDEX idx_settlement_date ON settlement_batches (settlement_date);
CREATE INDEX idx_settlement_status ON settlement_batches (status);
CREATE INDEX idx_settlement_partner_date ON settlement_batches (partner_id, settlement_date);
CREATE INDEX idx_settlement_tenant ON settlement_batches (tenant_id);

-- 2. Settlement entries (individual transactions within a batch)
CREATE TABLE settlement_entries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_batch_id UUID NOT NULL REFERENCES settlement_batches(id) ON DELETE CASCADE,
    transaction_id      VARCHAR(128),
    reference_type      VARCHAR(50),
    reference_id        VARCHAR(128),
    amount              DECIMAL(19,4) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'IDR',
    fee                 DECIMAL(19,4) DEFAULT 0,
    net_amount          DECIMAL(19,4),
    status              VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_settlement_entry_batch ON settlement_entries (settlement_batch_id);
CREATE INDEX idx_settlement_entry_tx ON settlement_entries (transaction_id);
CREATE INDEX idx_settlement_entry_status ON settlement_entries (status);

-- 3. Settlement discrepancies for reconciliation
CREATE TABLE settlement_discrepancies (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_batch_id UUID NOT NULL REFERENCES settlement_batches(id) ON DELETE CASCADE,
    transaction_id      VARCHAR(128),
    type                VARCHAR(20) NOT NULL,
    description         VARCHAR(512),
    expected_amount     DECIMAL(19,4),
    actual_amount       DECIMAL(19,4),
    difference          DECIMAL(19,4),
    resolved            BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by         VARCHAR(100),
    resolved_at         TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_discrepancy_batch ON settlement_discrepancies (settlement_batch_id);
CREATE INDEX idx_discrepancy_resolved ON settlement_discrepancies (resolved);

-- 4. Revenue split configurations (GAP-013)
CREATE TABLE revenue_splits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id      VARCHAR(128) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     VARCHAR(512),
    split_type      VARCHAR(16) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from  TIMESTAMP,
    effective_until TIMESTAMP,
    created_by      VARCHAR(100),
    tenant_id       VARCHAR(64),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_revenue_split_partner ON revenue_splits (partner_id);
CREATE INDEX idx_revenue_split_active ON revenue_splits (active);
CREATE INDEX idx_revenue_split_tenant ON revenue_splits (tenant_id);

-- 5. Revenue split stakeholders
CREATE TABLE revenue_split_stakeholders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    revenue_split_id UUID NOT NULL REFERENCES revenue_splits(id) ON DELETE CASCADE,
    account_id      VARCHAR(128) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    percentage      DECIMAL(8,4),
    fixed_amount    DECIMAL(19,4),
    priority        INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stakeholder_split ON revenue_split_stakeholders (revenue_split_id);
CREATE INDEX idx_stakeholder_account ON revenue_split_stakeholders (account_id);
