-- V6: Expand PII columns for AES-256-GCM encrypted values
-- Required by security-starter EncryptedStringConverter
-- Audit Finding #3 (phone) and #4 (email) remediation

-- Expand email column to hold encrypted ciphertext
ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(512);

-- Expand phone_number column to hold encrypted ciphertext
ALTER TABLE users ALTER COLUMN phone_number TYPE VARCHAR(512);

-- Drop unique constraints that won't work with encrypted values
-- Encrypted same plaintext produces different ciphertext each time (random IV)
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_phone_number_key;

-- Add functional indexes will be handled at application level
-- (encrypted columns cannot have DB-level unique constraints)

COMMENT ON COLUMN users.email IS 'AES-256-GCM encrypted email address';
COMMENT ON COLUMN users.phone_number IS 'AES-256-GCM encrypted phone number';

CREATE TABLE IF NOT EXISTS sensitive_user_data (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    nik VARCHAR(512) NOT NULL,
    tax_id VARCHAR(512),
    mother_maiden_name VARCHAR(512),
    address JSONB,
    phone_primary VARCHAR(512),
    phone_secondary VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
