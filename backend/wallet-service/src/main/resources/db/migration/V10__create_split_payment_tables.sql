-- V10: Create split payment tables for marketplace multi-merchant disbursement
-- Supports E-10: Escrow & Marketplace Payments (GAP-011)

-- 1. Split payment rules (reusable configurations)
CREATE TABLE IF NOT EXISTS split_payment_rules (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id  VARCHAR(128) NOT NULL,
    rule_name   VARCHAR(128) NOT NULL,
    split_type  VARCHAR(16) NOT NULL DEFAULT 'PERCENTAGE',
    currency    VARCHAR(3) NOT NULL DEFAULT 'IDR',
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id   VARCHAR(64),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_split_rule_partner_name UNIQUE (partner_id, rule_name)
);

CREATE INDEX IF NOT EXISTS idx_split_rule_partner ON split_payment_rules (partner_id);
CREATE INDEX IF NOT EXISTS idx_split_rule_active ON split_payment_rules (partner_id, active);
CREATE INDEX IF NOT EXISTS idx_split_rule_tenant ON split_payment_rules (tenant_id);

-- 2. Split recipients (per-rule recipient configuration)
CREATE TABLE IF NOT EXISTS split_recipients (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    split_rule_id        UUID NOT NULL REFERENCES split_payment_rules(id) ON DELETE CASCADE,
    recipient_account_id VARCHAR(128) NOT NULL,
    recipient_label      VARCHAR(128) NOT NULL,
    type                 VARCHAR(16) DEFAULT 'MERCHANT',
    percentage           DECIMAL(8,4),
    fixed_amount         DECIMAL(19,4),
    priority             INT NOT NULL DEFAULT 0,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_split_recipient_rule ON split_recipients (split_rule_id);
CREATE INDEX IF NOT EXISTS idx_split_recipient_account ON split_recipients (recipient_account_id);

-- 3. Split payment executions (one-time execution records)
CREATE TABLE IF NOT EXISTS split_payment_executions (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    split_rule_id         UUID REFERENCES split_payment_rules(id),
    payer_account_id      VARCHAR(128) NOT NULL,
    total_amount          DECIMAL(19,4) NOT NULL,
    currency              VARCHAR(3) NOT NULL DEFAULT 'IDR',
    idempotency_key       VARCHAR(128),
    external_reference_id VARCHAR(128),
    description           VARCHAR(512),
    status                VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    failure_reason        VARCHAR(512),
    reservation_id        VARCHAR(64),
    tenant_id             VARCHAR(64),
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at          TIMESTAMP,
    CONSTRAINT uq_split_exec_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_split_exec_payer ON split_payment_executions (payer_account_id);
CREATE INDEX IF NOT EXISTS idx_split_exec_status ON split_payment_executions (status);
CREATE INDEX IF NOT EXISTS idx_split_exec_rule ON split_payment_executions (split_rule_id);
CREATE INDEX IF NOT EXISTS idx_split_exec_ext_ref ON split_payment_executions (external_reference_id);
CREATE INDEX IF NOT EXISTS idx_split_exec_tenant ON split_payment_executions (tenant_id);

-- 4. Split payment legs (individual recipient credits within an execution)
CREATE TABLE IF NOT EXISTS split_payment_legs (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_id         UUID NOT NULL REFERENCES split_payment_executions(id) ON DELETE CASCADE,
    recipient_account_id VARCHAR(128) NOT NULL,
    recipient_label      VARCHAR(128),
    amount               DECIMAL(19,4) NOT NULL,
    status               VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    journal_entry_id     UUID,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    credited_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_split_leg_execution ON split_payment_legs (execution_id);
CREATE INDEX IF NOT EXISTS idx_split_leg_recipient ON split_payment_legs (recipient_account_id);
CREATE INDEX IF NOT EXISTS idx_split_leg_status ON split_payment_legs (status);
