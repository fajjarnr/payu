-- Add tenant_id column for multitenancy support

-- Add tenant_id to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);

-- Add tenant_id to profiles table
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_profiles_tenant_id ON profiles(tenant_id);

-- Add tenant_id to accounts table
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_accounts_tenant_id ON accounts(tenant_id);

-- Update existing records to use default tenant
UPDATE users SET tenant_id = 'default' WHERE tenant_id IS NULL;
UPDATE profiles SET tenant_id = 'default' WHERE tenant_id IS NULL;
UPDATE accounts SET tenant_id = 'default' WHERE tenant_id IS NULL;
