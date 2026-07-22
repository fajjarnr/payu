-- IMP-054: Budget Management - Create budgets table

CREATE TABLE IF NOT EXISTS budgets (
    id                    UUID PRIMARY KEY,
    user_id               UUID NOT NULL,
    category              VARCHAR(100) NOT NULL,
    limit_amount          NUMERIC(19, 2) NOT NULL,
    period                VARCHAR(20) NOT NULL,
    current_spent         NUMERIC(19, 2) NOT NULL DEFAULT 0,
    reset_date            DATE,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    warning_threshold     NUMERIC(5, 2) DEFAULT 0.8,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP,
    version               BIGINT DEFAULT 0,

    CONSTRAINT chk_budget_period CHECK (period IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_budget_limit_positive CHECK (limit_amount > 0),
    CONSTRAINT chk_budget_spent_non_negative CHECK (current_spent >= 0)
);

-- Indexes for budgets
CREATE INDEX IF NOT EXISTS idx_budget_user_id ON budgets(user_id);
CREATE INDEX IF NOT EXISTS idx_budget_category ON budgets(category);
CREATE INDEX IF NOT EXISTS idx_budget_active ON budgets(active);
CREATE INDEX IF NOT EXISTS idx_budget_reset_date ON budgets(reset_date);
CREATE INDEX IF NOT EXISTS idx_budget_user_category ON budgets(user_id, category);
CREATE INDEX IF NOT EXISTS idx_budget_user_active ON budgets(user_id, active);

-- Composite unique constraint: one active budget per user per category
CREATE UNIQUE INDEX IF NOT EXISTS idx_budget_unique_active_category
    ON budgets(user_id, category)
    WHERE active = TRUE;

-- Comments
COMMENT ON TABLE budgets IS 'User budgets for spending limits by category';
COMMENT ON COLUMN budgets.limit_amount IS 'Maximum allowed spending for the period';
COMMENT ON COLUMN budgets.current_spent IS 'Amount spent in current period';
COMMENT ON COLUMN budgets.reset_date IS 'Date when the budget period resets';
COMMENT ON COLUMN budgets.warning_threshold IS 'Percentage (0-1) at which to trigger warnings';
