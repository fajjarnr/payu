-- Create savings_goals table (IMP-039)
CREATE TABLE IF NOT EXISTS savings_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pocket_id UUID NOT NULL,
    user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    target_amount DECIMAL(19,2) NOT NULL,
    current_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
    deadline DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    icon VARCHAR(50),
    color VARCHAR(7),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT fk_savings_goal_pocket FOREIGN KEY (pocket_id) REFERENCES pockets(id) ON DELETE CASCADE,
    CONSTRAINT valid_savings_goal_status CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT positive_target_amount CHECK (target_amount > 0),
    CONSTRAINT non_negative_current_amount CHECK (current_amount >= 0)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_savings_goals_pocket_id ON savings_goals(pocket_id);
CREATE INDEX IF NOT EXISTS idx_savings_goals_user_id ON savings_goals(user_id);
CREATE INDEX IF NOT EXISTS idx_savings_goals_status ON savings_goals(status);
CREATE INDEX IF NOT EXISTS idx_savings_goals_user_status ON savings_goals(user_id, status);
CREATE INDEX IF NOT EXISTS idx_savings_goals_deadline ON savings_goals(deadline) WHERE deadline IS NOT NULL;

-- Add comments
COMMENT ON TABLE savings_goals IS 'User savings goals within pockets';
COMMENT ON COLUMN savings_goals.target_amount IS 'Target amount to save';
COMMENT ON COLUMN savings_goals.current_amount IS 'Current saved amount (auto-calculated from pocket)';
COMMENT ON COLUMN savings_goals.deadline IS 'Optional deadline for the savings goal';
COMMENT ON COLUMN savings_goals.icon IS 'Icon identifier for UI display';
COMMENT ON COLUMN savings_goals.color IS 'Hex color code for UI display';
