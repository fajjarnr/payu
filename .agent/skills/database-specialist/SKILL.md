---
name: database-specialist
description: Expert in PostgreSQL database design, optimization, migrations, and JSONB patterns for PayU Digital Banking Platform.
---

# PayU Database Specialist Skill

You are a database expert for the **PayU Digital Banking Platform**. Your expertise covers PostgreSQL design, performance optimization, schema migrations, and JSONB patterns for financial applications.

## 🗄️ Database Architecture

### Database Per Service Pattern

```
┌─────────────────────────────────────────────────────────┐
│                    PostgreSQL Cluster                    │
├─────────────┬─────────────┬─────────────┬──────────────┤
│ payu_account│ payu_wallet │ payu_txn    │ payu_billing │
│   (schema)  │   (schema)  │   (schema)  │   (schema)   │
├─────────────┼─────────────┼─────────────┼──────────────┤
│ • users     │ • wallets   │ • transfers │ • bills      │
│ • accounts  │ • pockets   │ • payments  │ • topups     │
│ • profiles  │ • ledger    │ • reversals │ • vendors    │
│ • kyc_data  │ • cards     │ • audit_log │ • schedules  │
└─────────────┴─────────────┴─────────────┴──────────────┘
```

### Connection Configuration

| Environment | Pool Size | Max Connections | Timeout |
|-------------|-----------|-----------------|---------|
| Development | 5 | 10 | 30s |
| Staging | 10 | 50 | 20s |
| Production | 20 | 100 | 10s |

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 10000
      idle-timeout: 300000
      max-lifetime: 600000
      leak-detection-threshold: 60000
```

---

## 📐 Schema Design Patterns

### 1. UUID Primary Keys

```sql
-- Always use UUID for PKs (distributed-friendly)
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number VARCHAR(20) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create index on foreign keys
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
```

### 2. Audit Columns (Required on ALL Tables)

```sql
-- Standard audit columns
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
created_by VARCHAR(100),
updated_by VARCHAR(100),
version BIGINT NOT NULL DEFAULT 0  -- Optimistic locking
```

### 3. Soft Delete Pattern

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    -- ... other columns
    deleted_at TIMESTAMPTZ,  -- NULL = active, timestamp = deleted
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Partial index for active records only
CREATE UNIQUE INDEX idx_users_email_active 
    ON users(email) 
    WHERE deleted_at IS NULL;
```

### 4. JSONB for Flexible Data

```sql
-- Use JSONB for semi-structured data
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    
    -- Structured fields (indexed, validated)
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    
    -- Flexible fields (JSONB)
    preferences JSONB NOT NULL DEFAULT '{}',
    metadata JSONB NOT NULL DEFAULT '{}',
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- GIN index for JSONB queries
CREATE INDEX idx_profiles_preferences ON user_profiles USING GIN (preferences);

-- Query example
SELECT * FROM user_profiles 
WHERE preferences @> '{"notifications": {"email": true}}';
```

### 5. Money/Currency Pattern

```sql
-- Use DECIMAL(19,4) for money - NEVER float/double!
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount DECIMAL(19, 4) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL DEFAULT 'IDR',
    
    -- Store original amount for FX
    original_amount DECIMAL(19, 4),
    original_currency CHAR(3),
    exchange_rate DECIMAL(19, 8),
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 6. Double-Entry Ledger

```sql
-- Wallet ledger with double-entry bookkeeping
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    transaction_id UUID NOT NULL,
    
    entry_type VARCHAR(20) NOT NULL,  -- DEBIT, CREDIT
    amount DECIMAL(19, 4) NOT NULL,
    balance_before DECIMAL(19, 4) NOT NULL,
    balance_after DECIMAL(19, 4) NOT NULL,
    
    description VARCHAR(500),
    reference_type VARCHAR(50),  -- TRANSFER, TOPUP, PAYMENT
    reference_id UUID,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for wallet history queries
CREATE INDEX idx_ledger_wallet_created 
    ON ledger_entries(wallet_id, created_at DESC);

-- Ensure balance consistency
ALTER TABLE ledger_entries ADD CONSTRAINT chk_balance_consistency
    CHECK (
        (entry_type = 'CREDIT' AND balance_after = balance_before + amount) OR
        (entry_type = 'DEBIT' AND balance_after = balance_before - amount)
    );
```

---

## 🚀 Performance Optimization

### Index Strategy

```sql
-- 1. B-tree for equality and range queries (default)
CREATE INDEX idx_txn_created_at ON transactions(created_at);

-- 2. Composite index for common query patterns
CREATE INDEX idx_txn_wallet_status_created 
    ON transactions(wallet_id, status, created_at DESC);

-- 3. Partial index for specific conditions
CREATE INDEX idx_txn_pending 
    ON transactions(created_at) 
    WHERE status = 'PENDING';

-- 4. Covering index (include columns to avoid table lookup)
CREATE INDEX idx_txn_summary 
    ON transactions(wallet_id, created_at DESC)
    INCLUDE (amount, status, type);

-- 5. GIN index for JSONB and arrays
CREATE INDEX idx_user_preferences ON users USING GIN (preferences);

-- 6. Hash index for exact match only (rare use)
CREATE INDEX idx_users_email_hash ON users USING HASH (email);
```

### Query Optimization

```sql
-- ❌ BAD: Full table scan
SELECT * FROM transactions WHERE YEAR(created_at) = 2026;

-- ✅ GOOD: Index-friendly range query
SELECT * FROM transactions 
WHERE created_at >= '2026-01-01' AND created_at < '2027-01-01';

-- ❌ BAD: Function on indexed column
SELECT * FROM users WHERE LOWER(email) = 'john@example.com';

-- ✅ GOOD: Use expression index or store lowercase
CREATE INDEX idx_users_email_lower ON users(LOWER(email));

-- ❌ BAD: SELECT * with large JSONB
SELECT * FROM user_profiles;

-- ✅ GOOD: Select specific columns
SELECT id, full_name, preferences->>'theme' as theme 
FROM user_profiles;
```

### EXPLAIN ANALYZE

```sql
-- Always check query plans for slow queries
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT t.*, w.account_number
FROM transactions t
JOIN wallets w ON t.wallet_id = w.id
WHERE t.wallet_id = 'uuid-here'
  AND t.created_at > NOW() - INTERVAL '30 days'
ORDER BY t.created_at DESC
LIMIT 20;
```

### Partitioning for Large Tables

```sql
-- Partition transactions by month
CREATE TABLE transactions (
    id UUID NOT NULL,
    wallet_id UUID NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create partitions
CREATE TABLE transactions_2026_01 PARTITION OF transactions
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

CREATE TABLE transactions_2026_02 PARTITION OF transactions
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

-- Auto-create partitions with pg_partman
SELECT partman.create_parent(
    p_parent_table => 'public.transactions',
    p_control => 'created_at',
    p_type => 'native',
    p_interval => 'monthly',
    p_premake => 3
);
```

---

## 🔄 Migration with Flyway

### Naming Convention

```
V{version}__{description}.sql

Examples:
V1__create_users_table.sql
V2__create_accounts_table.sql
V3__add_phone_to_users.sql
V4__create_transactions_indexes.sql
```

### Migration Best Practices

```sql
-- V5__add_status_to_wallets.sql

-- 1. Add column with default (non-blocking in PostgreSQL 11+)
ALTER TABLE wallets 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- 2. Backfill data if needed (in batches for large tables)
-- DO NOT run large UPDATE in single transaction

-- 3. Add constraint after data is valid
ALTER TABLE wallets 
ADD CONSTRAINT chk_wallet_status 
CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'));

-- 4. Create index concurrently (non-blocking)
CREATE INDEX CONCURRENTLY idx_wallets_status ON wallets(status);

-- Rollback script (keep in comments)
-- ALTER TABLE wallets DROP COLUMN status;
```

### Zero-Downtime Migration Pattern

```sql
-- Step 1: Add new column (nullable)
ALTER TABLE users ADD COLUMN phone_verified BOOLEAN;

-- Step 2: Backfill in application (dual-write)
-- Application writes to both old and new columns

-- Step 3: Make column NOT NULL with default
ALTER TABLE users 
ALTER COLUMN phone_verified SET NOT NULL,
ALTER COLUMN phone_verified SET DEFAULT false;

-- Step 4: Remove old column (next release)
-- ALTER TABLE users DROP COLUMN old_phone_status;
```

---

## 🔐 Security & Compliance

### Row-Level Security (RLS)

```sql
-- Enable RLS
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see their own accounts
CREATE POLICY accounts_isolation ON accounts
    FOR ALL
    USING (user_id = current_setting('app.current_user_id')::UUID);

-- Set user context in application
SET app.current_user_id = 'user-uuid-here';
```

### Data Encryption

```sql
-- Use pgcrypto for sensitive data
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Encrypt PII data
INSERT INTO users (email, nik_encrypted)
VALUES (
    'john@example.com',
    pgp_sym_encrypt('3201234567890001', current_setting('app.encryption_key'))
);

-- Decrypt when needed
SELECT pgp_sym_decrypt(
    nik_encrypted::bytea, 
    current_setting('app.encryption_key')
) as nik
FROM users WHERE id = 'uuid';
```

### Audit Trail

```sql
-- Audit trigger function
CREATE OR REPLACE FUNCTION audit_trigger_func()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit_log (
        table_name,
        record_id,
        action,
        old_data,
        new_data,
        changed_by,
        changed_at
    ) VALUES (
        TG_TABLE_NAME,
        COALESCE(NEW.id, OLD.id),
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN row_to_json(OLD) END,
        CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN row_to_json(NEW) END,
        current_setting('app.current_user_id', true),
        NOW()
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Apply to tables
CREATE TRIGGER audit_accounts
    AFTER INSERT OR UPDATE OR DELETE ON accounts
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_func();
```

---

## 🧪 Testing Database

### Testcontainers Setup

```java
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class AccountRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("payu_test")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("db/init-test.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private AccountRepository repository;

    @Test
    void shouldSaveAndRetrieveAccount() {
        var account = Account.builder()
            .accountNumber("1234567890")
            .userId(UUID.randomUUID())
            .status(AccountStatus.ACTIVE)
            .build();
        
        var saved = repository.save(account);
        var found = repository.findById(saved.getId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getAccountNumber()).isEqualTo("1234567890");
    }
}
```

### Database Test Data Builder

```java
public class TestDataBuilder {
    
    public static Account.AccountBuilder anAccount() {
        return Account.builder()
            .id(UUID.randomUUID())
            .accountNumber(generateAccountNumber())
            .userId(UUID.randomUUID())
            .status(AccountStatus.ACTIVE)
            .createdAt(Instant.now());
    }
    
    public static Wallet.WalletBuilder aWallet() {
        return Wallet.builder()
            .id(UUID.randomUUID())
            .balance(BigDecimal.ZERO)
            .currency("IDR")
            .status(WalletStatus.ACTIVE);
    }
}

// Usage in tests
@Test
void shouldDebitWallet() {
    var wallet = aWallet()
        .balance(new BigDecimal("1000000"))
        .build();
    walletRepository.save(wallet);
    
    // ... test
}
```

---

## 📊 Monitoring Queries

```sql
-- Find slow queries
SELECT 
    query,
    calls,
    mean_exec_time,
    total_exec_time,
    rows
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;

-- Check index usage
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;

-- Table bloat check
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename)) as total_size,
    n_dead_tup,
    last_vacuum,
    last_autovacuum
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;

-- Connection stats
SELECT 
    state,
    COUNT(*) as count,
    MAX(NOW() - state_change) as max_duration
FROM pg_stat_activity
WHERE datname = current_database()
GROUP BY state;
```

---

## 📋 Database Checklist

Before deploying schema changes:

- [ ] Migration is backward compatible
- [ ] Indexes added for new foreign keys
- [ ] Proper data types used (UUID, DECIMAL for money)
- [ ] Audit columns present (created_at, updated_at)
- [ ] Constraints added (CHECK, UNIQUE)
- [ ] EXPLAIN ANALYZE run on new queries
- [ ] Rollback script prepared
- [ ] Integration tests pass with Testcontainers

---

*Last Updated: January 2026*
