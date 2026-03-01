-- V4__create_promo_cashback_tables.sql
-- Description: Schema for promo code redemption and cashback auto-apply features (Epic E-17)
-- Rollback: DROP TABLE IF EXISTS promo_codes, promo_usage, cashback_rules, cashback_records CASCADE;

-- Promo codes table (rich domain model)
CREATE TABLE IF NOT EXISTS promo_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    discount_value DECIMAL(10, 4) NOT NULL,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
    usage_type VARCHAR(20) NOT NULL DEFAULT 'UNLIMITED' CHECK (usage_type IN ('ONCE_PER_USER', 'UNLIMITED')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED')),
    minimum_amount DECIMAL(19, 4),
    max_discount_amount DECIMAL(19, 4),
    max_usage_count INTEGER,
    current_usage_count INTEGER DEFAULT 0,
    expiry_date TIMESTAMP,
    excluded_partner_ids JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT chk_positive_discount CHECK (discount_value >= 0),
    CONSTRAINT chk_positive_min_amount CHECK (minimum_amount IS NULL OR minimum_amount >= 0),
    CONSTRAINT chk_positive_max_discount CHECK (max_discount_amount IS NULL OR max_discount_amount >= 0),
    CONSTRAINT chk_non_negative_usage CHECK (current_usage_count >= 0),
    CONSTRAINT chk_max_usage CHECK (max_usage_count IS NULL OR current_usage_count <= max_usage_count)
);

-- Promo usage tracking table
CREATE TABLE IF NOT EXISTS promo_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    promo_code VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    discount_amount DECIMAL(19, 4) NOT NULL,
    final_amount DECIMAL(19, 4) NOT NULL,
    idempotency_key VARCHAR(255),
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_positive_discount_amount CHECK (discount_amount >= 0),
    CONSTRAINT chk_positive_final_amount CHECK (final_amount >= 0),
    CONSTRAINT uq_idempotency_key UNIQUE (idempotency_key)
);

-- Cashback rules table
CREATE TABLE IF NOT EXISTS cashback_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    cashback_type VARCHAR(20) NOT NULL CHECK (cashback_type IN ('FIXED', 'PERCENTAGE', 'TIERED')),
    cashback_amount DECIMAL(19, 4),
    cashback_percentage DECIMAL(5, 2),
    max_cashback DECIMAL(19, 4),
    min_amount DECIMAL(19, 4),
    exact_amount DECIMAL(19, 4),
    tiered_cashback JSONB,
    applicable_merchant_codes JSONB,
    applicable_categories JSONB,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT chk_positive_cashback_amount CHECK (cashback_amount IS NULL OR cashback_amount >= 0),
    CONSTRAINT chk_positive_percentage CHECK (cashback_percentage IS NULL OR cashback_percentage >= 0),
    CONSTRAINT chk_positive_max_cashback CHECK (max_cashback IS NULL OR max_cashback >= 0),
    CONSTRAINT chk_positive_min_amount CHECK (min_amount IS NULL OR min_amount >= 0)
);

-- Cashback records table
CREATE TABLE IF NOT EXISTS cashback_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id VARCHAR(255) NOT NULL,
    account_id VARCHAR(255) NOT NULL,
    rule_id VARCHAR(50) NOT NULL,
    cashback_amount DECIMAL(19, 4) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CREDITED', 'FAILED')),
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    wallet_reference_id VARCHAR(255),

    CONSTRAINT chk_positive_cashback CHECK (cashback_amount >= 0)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_promo_codes_code ON promo_codes (code);
CREATE INDEX IF NOT EXISTS idx_promo_codes_status ON promo_codes (status);
CREATE INDEX IF NOT EXISTS idx_promo_codes_expiry ON promo_codes (expiry_date);

CREATE INDEX IF NOT EXISTS idx_promo_usage_user_code ON promo_usage (user_id, promo_code);
CREATE INDEX IF NOT EXISTS idx_promo_usage_transaction ON promo_usage (transaction_id);
CREATE INDEX IF NOT EXISTS idx_promo_usage_idempotency ON promo_usage (idempotency_key);

CREATE INDEX IF NOT EXISTS idx_cashback_rules_active ON cashback_rules (active);
CREATE INDEX IF NOT EXISTS idx_cashback_rules_validity ON cashback_rules (valid_from, valid_until);

CREATE INDEX IF NOT EXISTS idx_cashback_records_transaction ON cashback_records (transaction_id);
CREATE INDEX IF NOT EXISTS idx_cashback_records_account ON cashback_records (account_id);
CREATE INDEX IF NOT EXISTS idx_cashback_records_status ON cashback_records (status);

-- Comments
COMMENT ON TABLE promo_codes IS 'Promo codes with rich domain behavior for redemption';
COMMENT ON TABLE promo_usage IS 'Tracks promo code usage by users for idempotency and analytics';
COMMENT ON TABLE cashback_rules IS 'Rules for automatic cashback application on transactions';
COMMENT ON TABLE cashback_records IS 'Records of cashback processed for transactions';
