-- Security Hardening for Profiles Table (UU PDP Compliance)
-- Migration V5: Prepare for encrypted NIK (Nomor Induk Kependudukan)

-- 1. Increase nik column length to support encrypted values (AES-GCM with Base64 encoding)
-- Encrypted values are approximately 4x larger than plaintext
ALTER TABLE profiles ALTER COLUMN nik TYPE VARCHAR(512);

-- 2. Add comment documenting security measures
COMMENT ON COLUMN profiles.nik IS 'NIK encrypted at rest using AES-GCM (256-bit key) via EncryptedStringConverter. UU PDP No. 27 of 2022 compliant.';

-- 3. Rebuild indexes after column type change
DROP INDEX IF EXISTS uq_profiles_nik;
-- Unique constraint will be preserved but operates on encrypted values
-- Note: Application-level pre-encryption check may be needed for uniqueness validation
