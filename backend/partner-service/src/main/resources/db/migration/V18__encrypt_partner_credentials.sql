-- PARTNER-PROD-002: encrypt partner credentials at rest (AES-GCM via EncryptedStringConverter).
-- client_id stays plaintext (lookup key, findByClientId). client_secret and api_key are
-- now written as "ENC(<base64 iv+ct>)" ciphertext; widen the columns to fit it.
-- Existing plaintext rows remain readable: EncryptedStringConverter dual-reads plaintext
-- and the next write re-encrypts the value.

BEGIN;

ALTER TABLE partners ALTER COLUMN client_secret TYPE VARCHAR(512);
ALTER TABLE partners ALTER COLUMN api_key TYPE VARCHAR(512);

COMMENT ON COLUMN partners.client_secret IS 'Client secret encrypted at rest using AES-GCM (256-bit key) via EncryptedStringConverter (PARTNER-PROD-002).';
COMMENT ON COLUMN partners.api_key IS 'Legacy API key encrypted at rest using AES-GCM (256-bit key) via EncryptedStringConverter (PARTNER-PROD-002).';

-- Webhook signing secrets are already VARCHAR(512); no widening needed, but record the intent.
COMMENT ON COLUMN webhook_subscriptions.secret IS 'Webhook HMAC secret encrypted at rest using AES-GCM (256-bit key) via EncryptedStringConverter (PARTNER-PROD-002).';

COMMIT;
