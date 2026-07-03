-- V3__add_customer_segments.sql
-- Description: Create customer_segments table for personalized marketing
-- Rollback: DROP TABLE IF EXISTS customer_segments CASCADE;

CREATE TABLE IF NOT EXISTS customer_segments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    rules JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    
    CONSTRAINT idx_segment_name UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_segment_active ON customer_segments (is_active);

CREATE TABLE IF NOT EXISTS segment_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    segment_id UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_evaluated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_membership_account ON segment_memberships(account_id);
CREATE INDEX IF NOT EXISTS idx_membership_segment ON segment_memberships(segment_id);
CREATE INDEX IF NOT EXISTS idx_membership_account_segment ON segment_memberships(account_id, segment_id);
CREATE INDEX IF NOT EXISTS idx_membership_evaluated ON segment_memberships(last_evaluated_at);
