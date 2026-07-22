-- IMP-052: Add sandbox flag to API keys for test environment support

-- Add sandbox column to api_keys table
ALTER TABLE api_keys
ADD COLUMN sandbox BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index for sandbox lookups
CREATE INDEX IF NOT EXISTS idx_api_key_sandbox ON api_keys(sandbox);

-- Add composite index for active sandbox keys
CREATE INDEX IF NOT EXISTS idx_api_key_sandbox_active ON api_keys(sandbox, status);

-- Update existing SANDBOX environment keys to have sandbox=true
UPDATE api_keys
SET sandbox = TRUE
WHERE environment = 'SANDBOX';
