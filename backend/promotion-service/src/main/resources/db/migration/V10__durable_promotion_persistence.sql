-- Persist the usage mode so the database can enforce ONCE_PER_USER safely.
ALTER TABLE promo_usage
    ADD COLUMN IF NOT EXISTS usage_type VARCHAR(20) NOT NULL DEFAULT 'UNLIMITED';

ALTER TABLE promo_usage
    ADD CONSTRAINT chk_promo_usage_type
    CHECK (usage_type IN ('ONCE_PER_USER', 'UNLIMITED'));

-- The application pre-check improves the common path; this index closes the
-- race between replicas for once-per-user redemptions.
CREATE UNIQUE INDEX IF NOT EXISTS uq_promo_usage_user_code_once
    ON promo_usage (user_id, promo_code)
    WHERE usage_type = 'ONCE_PER_USER';

-- A transaction/rule pair can be credited only once across replicas.
CREATE UNIQUE INDEX IF NOT EXISTS uq_cashback_records_transaction_rule
    ON cashback_records (transaction_id, rule_id);
