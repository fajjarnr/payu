-- Security Hardening for Cards Table (PCI-DSS Compliance)
-- Migration V7: Remove CVV storage and prepare for encrypted card numbers

-- 1. Drop the CVV column (PCI-DSS violation - CVV must never be stored)
ALTER TABLE cards DROP COLUMN IF EXISTS cvv;

-- 2. Increase card_number column length to support encrypted values (AES-GCM with Base64 encoding)
-- Encrypted values are approximately 4x larger than plaintext
ALTER TABLE cards ALTER COLUMN card_number TYPE VARCHAR(512);

-- 3. Add comment documenting security measures
COMMENT ON COLUMN cards.card_number IS 'Card number encrypted at rest using AES-GCM (256-bit key) via EncryptedStringConverter. PCI-DSS compliant.';

-- 4. Rebuild indexes after column type change
DROP INDEX IF EXISTS idx_cards_card_number;
CREATE INDEX idx_cards_card_number ON cards(card_number);
