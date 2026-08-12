-- V113: WALLET-002 — align split payment schema with JPA entities
-- Entities map split_recipients.recipient_type and split_payment_legs.settled_at,
-- but V10 created `type` and `credited_at`. Fresh installs failed Hibernate
-- validate; live DBs were hand-patched. Idempotent so it applies to both.

ALTER TABLE split_recipients ADD COLUMN IF NOT EXISTS recipient_type VARCHAR(16);
UPDATE split_recipients SET recipient_type = type WHERE recipient_type IS NULL AND type IS NOT NULL;
ALTER TABLE split_recipients ALTER COLUMN recipient_type SET DEFAULT 'MERCHANT';
ALTER TABLE split_recipients ALTER COLUMN recipient_type SET NOT NULL;
ALTER TABLE split_recipients DROP COLUMN IF EXISTS type;

ALTER TABLE split_payment_legs ADD COLUMN IF NOT EXISTS settled_at TIMESTAMP;
