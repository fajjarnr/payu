-- GAP-1 fix: Enable pgcrypto extension for column-level encryption of PII.
--
-- account-service V6__encrypt_pii_columns.sql already expanded `users.email`
-- and `users.phone_number` columns to VARCHAR(512) to hold AES-256-GCM ciphertext
-- produced by EncryptedStringConverter in security-starter.
--
-- This migration enables the `pgcrypto` PostgreSQL extension so future migrations
-- can use `pgp_sym_encrypt()` / `pgp_sym_decrypt()` for additional PII columns
-- (NIK, etc.) that are better protected at the database layer with a key that
-- never leaves the database (separate from the application-level AES key).
--
-- The encryption key for pgp_sym_encrypt is stored in Vault and injected as
-- ENCRYPTION_KEY (same env var as payu.security.encryption.password).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

COMMENT ON EXTENSION pgcrypto IS 'GAP-1 fix: enables pgp_sym_encrypt/decrypt for PII column encryption';
