-- V2__create_gamification_tables.sql
-- Description: Gamification system tables for daily check-ins, user levels, and badges
-- Rollback: DROP TABLE IF EXISTS user_badges, badges, user_levels, daily_checkins CASCADE;

CREATE TABLE IF NOT EXISTS daily_checkins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    checkin_date DATE NOT NULL,
    streak_count INTEGER NOT NULL DEFAULT 1,
    points_earned INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_account_checkin UNIQUE (account_id, checkin_date),
    CONSTRAINT chk_positive_streak CHECK (streak_count > 0),
    CONSTRAINT chk_non_negative_points CHECK (points_earned >= 0)
);

CREATE TABLE IF NOT EXISTS badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon_url VARCHAR(500),
    requirement_type VARCHAR(50) NOT NULL,
    requirement_value DECIMAL(19, 4) NOT NULL,
    points_reward INTEGER NOT NULL DEFAULT 0,
    category VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_valid_badge_requirement CHECK (requirement_type IN ('TRANSACTION_COUNT', 'TOTAL_AMOUNT', 'STREAK_DAYS', 'REFERRED_USERS', 'LEVEL_REACHED')),
    CONSTRAINT chk_positive_points_reward CHECK (points_reward >= 0),
    CONSTRAINT chk_positive_requirement_value CHECK (requirement_value >= 0)
);

CREATE TABLE IF NOT EXISTS user_badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    badge_id UUID NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    earned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_account_badge UNIQUE (account_id, badge_id)
);

CREATE TABLE IF NOT EXISTS user_levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL UNIQUE,
    level INTEGER NOT NULL DEFAULT 1,
    xp INTEGER NOT NULL DEFAULT 0,
    level_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_positive_level CHECK (level > 0),
    CONSTRAINT chk_non_negative_xp CHECK (xp >= 0)
);

CREATE TABLE IF NOT EXISTS xp_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255),
    source_type VARCHAR(50) NOT NULL,
    xp_earned INTEGER NOT NULL,
    xp_after INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_valid_xp_source CHECK (source_type IN ('TRANSACTION', 'BADGE', 'CHECKIN', 'REFERRAL', 'ADJUSTMENT'))
);

CREATE TABLE IF NOT EXISTS level_rewards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    level INTEGER NOT NULL UNIQUE,
    points_reward INTEGER NOT NULL DEFAULT 0,
    bonus_description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_positive_level_reward CHECK (level > 0),
    CONSTRAINT chk_positive_level_points CHECK (points_reward >= 0)
);

INSERT INTO level_rewards (level, points_reward, bonus_description) VALUES
    (1, 0, 'Welcome to PayU'),
    (2, 100, 'Level 2 Reward'),
    (3, 250, 'Level 3 Reward'),
    (4, 500, 'Level 4 Reward'),
    (5, 1000, 'Level 5 Reward'),
    (6, 1500, 'Level 6 Reward'),
    (7, 2500, 'Level 7 Reward'),
    (8, 4000, 'Level 8 Reward'),
    (9, 6000, 'Level 9 Reward'),
    (10, 10000, 'Level 10 Master Reward')
ON CONFLICT (level) DO NOTHING;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_checkin_account ON daily_checkins (account_id);
CREATE INDEX IF NOT EXISTS idx_checkin_date ON daily_checkins (checkin_date);
CREATE INDEX IF NOT EXISTS idx_badge_active ON badges (is_active);
CREATE INDEX IF NOT EXISTS idx_badge_requirement ON badges (requirement_type, requirement_value);
CREATE INDEX IF NOT EXISTS idx_user_badge_account ON user_badges (account_id);
CREATE INDEX IF NOT EXISTS idx_user_level_account ON user_levels (account_id);
CREATE INDEX IF NOT EXISTS idx_user_level_level ON user_levels (level);
CREATE INDEX IF NOT EXISTS idx_xp_transaction_account ON xp_transactions (account_id);

-- Comments
COMMENT ON TABLE daily_checkins IS 'User daily check-in records with streak tracking';
COMMENT ON TABLE badges IS 'Available badges users can earn';
COMMENT ON TABLE user_badges IS 'Badges earned by users';
COMMENT ON TABLE user_levels IS 'User levels and XP progression';
COMMENT ON TABLE xp_transactions IS 'XP earned/lost transactions';
COMMENT ON TABLE level_rewards IS 'Rewards granted at each level';
