-- V21__dual_control_maker_checker.sql
-- ADR-0035: Dual-control onboarding — adds maker/checker columns, CHECK constraint, index, status migration.

ALTER TABLE partners ADD COLUMN IF NOT EXISTS maker_id VARCHAR(64);
ALTER TABLE partners ADD COLUMN IF NOT EXISTS checker_id VARCHAR(64);
ALTER TABLE partners ADD COLUMN IF NOT EXISTS requested_at TIMESTAMPTZ;
ALTER TABLE partners ADD COLUMN IF NOT EXISTS decided_at TIMESTAMPTZ;
ALTER TABLE partners ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(512);

-- migrate legacy PENDING_VERIFICATION -> PENDING_APPROVAL
UPDATE partners SET status = 'PENDING_APPROVAL' WHERE status = 'PENDING_VERIFICATION';

-- ensure active flag consistent: only ACTIVE is active
UPDATE partners SET active = (status = 'ACTIVE');

-- maker != checker enforced at DB level (ADR-0035)
ALTER TABLE partners DROP CONSTRAINT IF EXISTS chk_maker_checker;
ALTER TABLE partners ADD CONSTRAINT chk_maker_checker CHECK (maker_id IS NULL OR checker_id IS NULL OR maker_id <> checker_id);

-- index for SLA scheduler: pending approvals ordered by requested_at
CREATE INDEX IF NOT EXISTS idx_partners_status_requested ON partners(status, requested_at) WHERE status = 'PENDING_APPROVAL';
