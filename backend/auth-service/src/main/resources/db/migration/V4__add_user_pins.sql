-- ADR-0028 Step-Up Auth & Dynamic Linking: user_pins with Argon2id hash + 3-strike lockout 15m soft-lock
-- ponytail: minimal table, no per-user salt column (salt embedded in Argon2 hash)
CREATE TABLE IF NOT EXISTS user_pins (
    user_id VARCHAR(36) PRIMARY KEY,
    pin_hash VARCHAR(512) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_pins_locked_until ON user_pins(locked_until) WHERE locked_until IS NOT NULL;
