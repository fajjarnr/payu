-- RELAY-007b: valid_status CHECK still enumerates the pre-ADR-0028/0030 set,
-- so persisting PENDING_COMPLIANCE_REVIEW / PENDING_STEP_UP aborts with 23514.
-- Re-issue the constraint with the full TransactionStatus enum. Scheduled
-- transfers keep their own ACTIVE/PAUSED lifecycle — untouched.
ALTER TABLE transactions DROP CONSTRAINT valid_status;
ALTER TABLE transactions ADD CONSTRAINT valid_status CHECK (status IN ('PENDING', 'VALIDATING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'PENDING_COMPLIANCE_REVIEW', 'PENDING_STEP_UP'));

ALTER TABLE transaction_archives DROP CONSTRAINT valid_status;
ALTER TABLE transaction_archives ADD CONSTRAINT valid_status CHECK (status IN ('PENDING', 'VALIDATING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'PENDING_COMPLIANCE_REVIEW', 'PENDING_STEP_UP'));
