-- GLOBAL-IMP-007: Payload fingerprint for idempotency tamper guard (Stripe/Adyen)
-- Stores SHA-256(canonical JSON) of business payload; replay with different amount/recipient → 409 IDEMPOTENCY_PAYLOAD_MISMATCH
-- Ponytail: single column on transactions; full idempotency_keys table deferred until cross-table dedup needed (see V28 ponytail note)
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS idempotency_request_hash VARCHAR(64);
COMMENT ON COLUMN transactions.idempotency_request_hash IS 'GLOBAL-IMP-007: SHA-256 Base64 of canonical payload for tamper detection';
-- Backfill null for legacy rows (no enforcement until new writes populate hash)
