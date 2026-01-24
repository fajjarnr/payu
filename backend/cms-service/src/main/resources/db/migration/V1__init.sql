-- ==================================================
-- PayU CMS Service - Initial Schema
-- Version: 1.0.0
-- ==================================================

-- Create enum type for content status
CREATE TYPE content_status AS ENUM (
    'DRAFT',
    'SCHEDULED',
    'ACTIVE',
    'PAUSED',
    'ARCHIVED'
);

-- Create cms_contents table
CREATE TABLE cms_contents (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Content basic fields
    content_type VARCHAR(50) NOT NULL CHECK (content_type IN ('BANNER', 'PROMO', 'ALERT', 'POPUP')),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    action_url VARCHAR(500),
    action_type VARCHAR(50) CHECK (action_type IN ('LINK', 'DEEP_LINK', 'DISMISS')),

    -- Date fields
    start_date DATE,
    end_date DATE,

    -- Priority and status
    priority INTEGER DEFAULT 0,
    status content_status NOT NULL DEFAULT 'DRAFT',

    -- JSONB fields for flexible data
    targeting_rules JSONB,
    metadata JSONB,

    -- Version for optimistic locking
    version INTEGER NOT NULL DEFAULT 1,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT chk_date_range CHECK (
        end_date IS NULL OR
        start_date IS NULL OR
        end_date >= start_date
    ),
    CONSTRAINT chk_priority_range CHECK (priority >= 0 AND priority <= 1000)
);

-- Create indexes for common queries
CREATE INDEX idx_cms_content_type ON cms_contents(content_type);
CREATE INDEX idx_cms_status ON cms_contents(status);
CREATE INDEX idx_cms_dates ON cms_contents(start_date, end_date);
CREATE INDEX idx_cms_priority ON cms_contents(priority DESC);
CREATE INDEX idx_cms_created_by ON cms_contents(created_by);

-- Create GIN indexes for JSONB searches
CREATE INDEX idx_cms_targeting_rules ON cms_contents USING GIN (targeting_rules);
CREATE INDEX idx_cms_metadata ON cms_contents USING GIN (metadata);

-- Add comments for documentation
COMMENT ON TABLE cms_contents IS 'Content Management System table for banners, promos, alerts, and popups';

COMMENT ON COLUMN cms_contents.id IS 'Unique identifier for the content';
COMMENT ON COLUMN cms_contents.content_type IS 'Type of content: BANNER, PROMO, ALERT, or POPUP';
COMMENT ON COLUMN cms_contents.title IS 'Content title (must be unique)';
COMMENT ON COLUMN cms_contents.description IS 'Detailed description of the content';
COMMENT ON COLUMN cms_contents.image_url IS 'URL to the content image';
COMMENT ON COLUMN cms_contents.action_url IS 'URL to navigate when content is clicked';
COMMENT ON COLUMN cms_contents.action_type IS 'Type of action: LINK, DEEP_LINK, or DISMISS';
COMMENT ON COLUMN cms_contents.start_date IS 'Start date for content visibility';
COMMENT ON COLUMN cms_contents.end_date IS 'End date for content visibility';
COMMENT ON COLUMN cms_contents.priority IS 'Display priority (higher = shown first)';
COMMENT ON COLUMN cms_contents.status IS 'Content status: DRAFT, SCHEDULED, ACTIVE, PAUSED, or ARCHIVED';
COMMENT ON COLUMN cms_contents.targeting_rules IS 'JSONB field for user targeting rules';
COMMENT ON COLUMN cms_contents.metadata IS 'JSONB field for custom metadata';
COMMENT ON COLUMN cms_contents.version IS 'Optimistic lock version';
COMMENT ON COLUMN cms_contents.created_at IS 'Timestamp when content was created';
COMMENT ON COLUMN cms_contents.updated_at IS 'Timestamp when content was last updated';
COMMENT ON COLUMN cms_contents.created_by IS 'User who created the content';
COMMENT ON COLUMN cms_contents.updated_by IS 'User who last updated the content';

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-update updated_at
CREATE TRIGGER trigger_update_cms_contents_updated_at
    BEFORE UPDATE ON cms_contents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data for testing
INSERT INTO cms_contents (
    content_type,
    title,
    description,
    image_url,
    action_url,
    action_type,
    start_date,
    end_date,
    priority,
    status,
    targeting_rules,
    metadata,
    created_by,
    updated_by
) VALUES
(
    'BANNER',
    'Welcome Bonus Promo',
    'Get Rp 50.000 bonus on your first transaction',
    'https://cdn.payu.id/images/welcome-bonus.png',
    'https://payu.id/promos/welcome-bonus',
    'LINK',
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '30 days',
    100,
    'ACTIVE',
    '{"segment": "NEW_USER", "location": "ALL", "device": "ALL"}'::jsonb,
    '{"campaign": "WELCOME_2026", "abTest": "A"}'::jsonb,
    'admin@payu.id',
    'admin@payu.id'
),
(
    'PROMO',
    'Weekend Cashback',
    '20% cashback on all transactions this weekend',
    'https://cdn.payu.id/images/weekend-cashback.png',
    'https://payu.id/promos/weekend',
    'DEEP_LINK',
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '7 days',
    90,
    'ACTIVE',
    '{"segment": "ALL", "location": "ALL", "device": "MOBILE"}'::jsonb,
    '{"campaign": "WEEKEND_2026"}'::jsonb,
    'admin@payu.id',
    'admin@payu.id'
),
(
    'ALERT',
    'Scheduled Maintenance',
    'System maintenance on Sunday 2-4 AM WIB',
    NULL,
    NULL,
    'DISMISS',
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '3 days',
    200,
    'SCHEDULED',
    '{"segment": "ALL", "location": "ALL", "device": "ALL"}'::jsonb,
    '{"type": "SYSTEM_ALERT"}'::jsonb,
    'admin@payu.id',
    'admin@payu.id'
);

-- Create view for active content (for easy querying)
CREATE OR REPLACE VIEW active_contents AS
SELECT *
FROM cms_contents
WHERE status = 'ACTIVE'
  AND (start_date IS NULL OR start_date <= CURRENT_DATE)
  AND (end_date IS NULL OR end_date >= CURRENT_DATE)
ORDER BY priority DESC, created_at DESC;

COMMENT ON VIEW active_contents IS 'View showing all currently active content';

-- Grant permissions (adjust as needed for your environment)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON cms_contents TO payu_app;
-- GRANT SELECT, UPDATE ON SEQUENCE cms_contents_id_seq TO payu_app;
-- GRANT EXECUTE ON FUNCTION update_updated_at_column() TO payu_app;
