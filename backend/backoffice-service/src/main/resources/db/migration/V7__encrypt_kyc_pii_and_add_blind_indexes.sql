-- user_id and account_number were unbounded VARCHAR in V1; no narrowing rewrite is needed.
ALTER TABLE kyc_reviews ALTER COLUMN document_number TYPE VARCHAR(512);
ALTER TABLE kyc_reviews ALTER COLUMN full_name TYPE VARCHAR(512);
ALTER TABLE kyc_reviews ALTER COLUMN phone_number TYPE VARCHAR(512);
ALTER TABLE kyc_reviews ADD COLUMN user_id_blind_index VARCHAR(64);
ALTER TABLE kyc_reviews ADD COLUMN user_id_blind_index_key_version VARCHAR(32);

-- Existing plaintext is intentionally retained here. EncryptedStringConverter dual-reads
-- plaintext and KycPiiBackfillRunner rewrites it in resumable batches using Vault keys.
