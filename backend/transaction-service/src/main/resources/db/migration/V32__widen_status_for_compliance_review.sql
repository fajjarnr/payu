-- RELAY-007: TransactionStatus gained PENDING_COMPLIANCE_REVIEW (25 chars, ADR-0030)
-- and PENDING_STEP_UP (15 chars, ADR-0028) but status columns are VARCHAR(20),
-- so persisting a held transfer aborts with SQLState 22001 and the API 500s.
-- Widen lifecycle status columns to VARCHAR(40). Currency/money columns untouched.
ALTER TABLE transactions ALTER COLUMN status TYPE VARCHAR(40);
ALTER TABLE scheduled_transfers ALTER COLUMN status TYPE VARCHAR(40);
ALTER TABLE transaction_archives ALTER COLUMN status TYPE VARCHAR(40);
