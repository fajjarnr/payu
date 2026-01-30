# PostgreSQL Design Rules & optimizations

Advanced design patterns for ensuring long-term performance and maintainability.

## 1. Data Type "Do's & Don'ts"

| Type Class | ❌ Avoid | ✅ Use | Reason |
| :--- | :--- | :--- | :--- |
| **Text** | `char(n)`, `varchar(n)` | `text` | In Postgres, `text` has no performance penalty. `varchar(n)` just adds CPU cycles to check length. |
| **Time** | `timestamp`, `timetz` | `timestamptz` | Always store UTC. Avoid confusion with client timezones. |
| **ID** | `serial` | `GENERATED ALWAYS AS IDENTITY` | SQL Standard compliant. |
| **Money** | `money`, `float` | `numeric`, `decimal` | Precision guarantees. `money` type is locale-dependent (bad). |

## 2. Update-Heavy Workloads (e.g., Balances)

Tables with high update rates (like `wallets`) suffer from bloating because Postgres uses MVCC (Copy-on-Write).

### Optimization 1: Fillfactor & HOT Updates
Set `fillfactor` < 100 (e.g., 90) to leave space on the page. This allows Postgres to perform **Heap-Only Tuple (HOT)** updates, which are much faster because they don't require updating indexes.

```sql
CREATE TABLE wallets (
    -- cols
) WITH (fillfactor = 90);
```

### Optimization 2: Avoid Indexing Volatile Columns
Every update to an indexed column forces an index update.
**Rule:** Do NOT index columns that change frequently (like `last_updated_at` or `balance`) unless absolutely necessary for queries.

## 3. High-Volume Inserts (e.g., Audio Logs)

### Optimization 1: Unlogged Tables (Staging)
For temporary data or staging tables that can be lost on crash:
```sql
CREATE UNLOGGED TABLE staging_logs (...);
```
Writes are **2-3x faster** (skips WAL).

### Optimization 2: Partitioning
For time-series data (Audit Logs, Transaction History):
```sql
CREATE TABLE audit_log (
    created_at TIMESTAMPTZ NOT NULL,
    -- ...
) PARTITION BY RANGE (created_at);
```
Allows dropping old data via `DROP TABLE` (instant) vs `DELETE` (slow + bloom).

## 4. Safe Schema Evolution

### Defaults & Rewrites
Adding a column with a **volatile** default (e.g., `gen_random_uuid()` or `now()`) causes a full table rewrite in older Postgres versions (pre-11).
**Safe Pattern:**
1. Add column nullable (instant).
2. Add default.
3. Backfill data in batches.
4. Add NOT NULL constraint.

### Concurrent Indexing
Never run `CREATE INDEX` on production without `CONCURRENTLY`.
```sql
-- ❌ Blocks writes to table
CREATE INDEX idx_users_email ON users(email);

-- ✅ Background operation (slower build, safe for prod)
CREATE INDEX CONCURRENTLY idx_users_email ON users(email);
```
