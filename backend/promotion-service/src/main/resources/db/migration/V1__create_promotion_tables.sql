-- V1__create_promotion_tables.sql
-- Description: Initial schema for promotion service - promotions, rewards, cashbacks, referrals, loyalty_points tables
-- Rollback: DROP TABLE IF EXISTS promotions, rewards, cashbacks, referrals, loyalty_points CASCADE;

CREATE TABLE IF NOT EXISTS promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    promotion_type VARCHAR(50) NOT NULL,
    reward_type VARCHAR(50) NOT NULL,
    reward_value DECIMAL(19, 4) NOT NULL,
    max_redemptions INTEGER,
    redemption_count INTEGER DEFAULT 0,
    min_transaction_amount DECIMAL(19, 4),
    merchant_codes JSONB,
    category_codes JSONB,
    status VARCHAR(20) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_positive_reward_value CHECK (reward_value >= 0),
    CONSTRAINT chk_valid_promotion_type CHECK (promotion_type IN ('CASHBACK', 'DISCOUNT', 'REWARD_POINTS', 'REFERRAL_BONUS')),
    CONSTRAINT chk_valid_reward_type CHECK (reward_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'POINTS')),
    CONSTRAINT chk_valid_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT chk_valid_redemptions CHECK (redemption_count >= 0 AND (max_redemptions IS NULL OR redemption_count <= max_redemptions)),
    CONSTRAINT chk_valid_dates CHECK (end_date > start_date)
);

CREATE TABLE IF NOT EXISTS rewards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255),
    promotion_code VARCHAR(50),
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    points_earned INTEGER,
    transaction_amount DECIMAL(19, 4) NOT NULL,
    merchant_code VARCHAR(100),
    category_code VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    expiry_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_positive_reward_amount CHECK (amount >= 0),
    CONSTRAINT chk_positive_points CHECK (points_earned IS NULL OR points_earned >= 0),
    CONSTRAINT chk_valid_reward_status CHECK (status IN ('PENDING', 'AWARDED', 'EXPIRED', 'VOIDED'))
);

CREATE TABLE IF NOT EXISTS cashbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    cashback_amount DECIMAL(19, 4) NOT NULL,
    transaction_amount DECIMAL(19, 4) NOT NULL,
    percentage DECIMAL(19, 4) NOT NULL,
    merchant_code VARCHAR(100),
    category_code VARCHAR(100),
    cashback_code VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    credited_at TIMESTAMP,
    expiry_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_positive_cashback CHECK (cashback_amount > 0 AND transaction_amount > 0 AND percentage >= 0),
    CONSTRAINT chk_valid_cashback_status CHECK (status IN ('PENDING', 'CREDITED', 'EXPIRED', 'VOIDED'))
);

CREATE TABLE IF NOT EXISTS referrals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_account_id VARCHAR(255) NOT NULL,
    referee_account_id VARCHAR(255),
    referral_code VARCHAR(50) NOT NULL UNIQUE,
    referrer_reward DECIMAL(19, 4) NOT NULL,
    referee_reward DECIMAL(19, 4) NOT NULL,
    reward_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    completed_at TIMESTAMP,
    expiry_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_positive_referral_rewards CHECK (referrer_reward >= 0 AND referee_reward >= 0),
    CONSTRAINT chk_valid_referral_reward_type CHECK (reward_type IN ('CASHBACK', 'POINTS', 'FIXED_AMOUNT')),
    CONSTRAINT chk_valid_referral_status CHECK (status IN ('PENDING', 'COMPLETED', 'EXPIRED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS loyalty_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255),
    transaction_type VARCHAR(50) NOT NULL,
    points INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    expiry_date TIMESTAMP,
    redeemed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_valid_points CHECK (points IS NOT NULL),
    CONSTRAINT chk_valid_balance CHECK (balance_after >= 0),
    CONSTRAINT chk_valid_loyalty_transaction_type CHECK (transaction_type IN ('EARNED', 'REDEEMED', 'EXPIRED', 'ADJUSTED', 'REFERRAL_BONUS'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_promotion_type ON promotions (promotion_type);
CREATE INDEX IF NOT EXISTS idx_promotion_status ON promotions (status);
CREATE INDEX IF NOT EXISTS idx_promotion_dates ON promotions (start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_reward_account ON rewards (account_id);
CREATE INDEX IF NOT EXISTS idx_reward_transaction ON rewards (transaction_id);
CREATE INDEX IF NOT EXISTS idx_reward_date ON rewards (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cashback_account ON cashbacks (account_id);
CREATE INDEX IF NOT EXISTS idx_cashback_transaction ON cashbacks (transaction_id);
CREATE INDEX IF NOT EXISTS idx_cashback_status ON cashbacks (status);
CREATE INDEX IF NOT EXISTS idx_cashback_date ON cashbacks (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_referral_referrer ON referrals (referrer_account_id);
CREATE INDEX IF NOT EXISTS idx_referral_referee ON referrals (referee_account_id);
CREATE INDEX IF NOT EXISTS idx_referral_code ON referrals (referral_code);
CREATE INDEX IF NOT EXISTS idx_referral_status ON referrals (status);
CREATE INDEX IF NOT EXISTS idx_loyalty_account ON loyalty_points (account_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_expiry ON loyalty_points (expiry_date);

-- Comments
COMMENT ON TABLE promotions IS 'Promotional campaigns and offers';
COMMENT ON TABLE rewards IS 'Reward transactions earned by users';
COMMENT ON TABLE cashbacks IS 'Cashback transactions';
COMMENT ON TABLE referrals IS 'Referral relationships and rewards';
COMMENT ON TABLE loyalty_points IS 'Loyalty points transactions and balances';
