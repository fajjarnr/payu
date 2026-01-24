-- PayU A/B Testing Service - Initial Schema
-- Version: 1.0.0

-- Create experiments table
CREATE TABLE IF NOT EXISTS ab_experiments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    key VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    start_date DATE,
    end_date DATE,
    traffic_split INTEGER NOT NULL CHECK (traffic_split >= 0 AND traffic_split <= 100),
    variant_a_config JSONB,
    variant_b_config JSONB,
    targeting_rules JSONB,
    metrics JSONB,
    confidence_level DOUBLE PRECISION,
    winner VARCHAR(50),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    created_by VARCHAR(100),

    CONSTRAINT chk_status CHECK (status IN ('DRAFT', 'RUNNING', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_winner CHECK (winner IS NULL OR winner IN ('CONTROL', 'VARIANT_B', 'INCONCLUSIVE')),
    CONSTRAINT chk_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_ab_status ON ab_experiments(status);
CREATE INDEX IF NOT EXISTS idx_ab_dates ON ab_experiments(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_ab_created_by ON ab_experiments(created_by);
CREATE INDEX IF NOT EXISTS idx_ab_key ON ab_experiments(key);

-- Create GIN indexes for JSONB columns
CREATE INDEX IF NOT EXISTS idx_ab_variant_a_config ON ab_experiments USING GIN (variant_a_config);
CREATE INDEX IF NOT EXISTS idx_ab_variant_b_config ON ab_experiments USING GIN (variant_b_config);
CREATE INDEX IF NOT EXISTS idx_ab_targeting_rules ON ab_experiments USING GIN (targeting_rules);
CREATE INDEX IF NOT EXISTS idx_ab_metrics ON ab_experiments USING GIN (metrics);

-- Add comments
COMMENT ON TABLE ab_experiments IS 'A/B Testing experiments';
COMMENT ON COLUMN ab_experiments.id IS 'Unique identifier';
COMMENT ON COLUMN ab_experiments.name IS 'Experiment name';
COMMENT ON COLUMN ab_experiments.description IS 'Experiment description';
COMMENT ON COLUMN ab_experiments.key IS 'Unique key for frontend';
COMMENT ON COLUMN ab_experiments.status IS 'Experiment status';
COMMENT ON COLUMN ab_experiments.start_date IS 'Start date';
COMMENT ON COLUMN ab_experiments.end_date IS 'End date';
COMMENT ON COLUMN ab_experiments.traffic_split IS 'Percentage for variant B (0-100)';
COMMENT ON COLUMN ab_experiments.variant_a_config IS 'Control configuration';
COMMENT ON COLUMN ab_experiments.variant_b_config IS 'Test configuration';
COMMENT ON COLUMN ab_experiments.targeting_rules IS 'Targeting rules';
COMMENT ON COLUMN ab_experiments.metrics IS 'Conversion metrics';
COMMENT ON COLUMN ab_experiments.confidence_level IS 'Statistical significance';
COMMENT ON COLUMN ab_experiments.winner IS 'Winner variant';
COMMENT ON COLUMN ab_experiments.created_at IS 'Creation timestamp';
COMMENT ON COLUMN ab_experiments.updated_at IS 'Last update timestamp';
COMMENT ON COLUMN ab_experiments.created_by IS 'Creator username';

-- Insert sample data for testing
INSERT INTO ab_experiments (name, description, key, status, start_date, end_date, traffic_split, variant_a_config, variant_b_config, targeting_rules, metrics, created_by)
VALUES
(
    'Homepage Hero Banner Test',
    'Test different hero banner designs for homepage',
    'homepage_hero_banner',
    'RUNNING',
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '30 days',
    50,
    '{"backgroundColor": "#ffffff", "textColor": "#000000", "ctaText": "Learn More"}'::jsonb,
    '{"backgroundColor": "#10b981", "textColor": "#ffffff", "ctaText": "Get Started"}'::jsonb,
    '{"minAge": 18, "countries": ["ID"]}'::jsonb,
    '{"CONTROL": {"participants": 150, "conversions": 30}, "VARIANT_B": {"participants": 145, "conversions": 42}}'::jsonb,
    'system'
),
(
    'Checkout Button Color',
    'Test green vs blue checkout button',
    'checkout_button_color',
    'DRAFT',
    CURRENT_DATE + INTERVAL '7 days',
    CURRENT_DATE + INTERVAL '37 days',
    50,
    '{"color": "#10b981", "text": "Pay Now"}'::jsonb,
    '{"color": "#3b82f6", "text": "Pay Now"}'::jsonb,
    '{}'::jsonb,
    '{"CONTROL": {"participants": 0, "conversions": 0}, "VARIANT_B": {"participants": 0, "conversions": 0}}'::jsonb,
    'system'
),
(
    'Promo Offer Display',
    'Test different promo offer layouts',
    'promo_offer_display',
    'COMPLETED',
    CURRENT_DATE - INTERVAL '30 days',
    CURRENT_DATE - INTERVAL '1 day',
    50,
    '{"layout": "horizontal", "showDiscount": true}'::jsonb,
    '{"layout": "vertical", "showDiscount": false}'::jsonb,
    '{"minTransactions": 5}'::jsonb,
    '{"CONTROL": {"participants": 500, "conversions": 125}, "VARIANT_B": {"participants": 495, "conversions": 89}}'::jsonb,
    'system'
);

-- Update completed experiment with winner
UPDATE ab_experiments
SET winner = 'CONTROL',
    confidence_level = 0.95
WHERE key = 'promo_offer_display';
