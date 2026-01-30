# PostgreSQL Performance & Optimization Patterns

Reference guide for advanced PostgreSQL usage in PayU microservices.

## 1. Indexing Strategy

| Query Pattern | Index Type | SQL Example |
| :--- | :--- | :--- |
| **Exact Match** | B-tree | `CREATE INDEX idx_users_email ON users (email);` |
| **Range Scan** | B-tree | `CREATE INDEX idx_orders_created ON orders (created_at);` |
| **Multi-Column** | Composite | `CREATE INDEX idx_orders_status_date ON orders (status, created_at);` |
| **JSONB Key** | GIN | `CREATE INDEX idx_payload_gin ON events USING gin (payload);` |
| **Text/Search** | GIN/GIST | `CREATE INDEX idx_search ON products USING gin (to_tsvector('english', description));` |
| **Partial Index** | B-tree | `CREATE INDEX idx_active_users ON users (email) WHERE deleted_at IS NULL;` |

### Covering Index (Index Only Scan)
Avoid visiting the heap for frequently accessed columns.
```sql
CREATE INDEX idx_users_email_include ON users (email) INCLUDE (full_name, status);
```

## 2. Locking & Concurrency Patterns

### Queue Processing (Job Queue)
Use `FOR UPDATE SKIP LOCKED` to implement concurrent job workers without contention. This is critical for `notification-service` and transaction processing.

```sql
UPDATE transaction_jobs
SET status = 'PROCESSING',
    started_at = NOW(),
    worker_id = $1
WHERE id = (
  SELECT id
  FROM transaction_jobs
  WHERE status = 'PENDING'
  ORDER BY priority DESC, created_at ASC
  LIMIT 1
  FOR UPDATE SKIP LOCKED
)
RETURNING *;
```

### Upsert (Idempotent Insert)
Handle duplicate inserts gracefully.

```sql
INSERT INTO user_balances (user_id, amount, currency)
VALUES ($1, $2, 'IDR')
ON CONFLICT (user_id)
DO UPDATE SET
    amount = user_balances.amount + EXCLUDED.amount,
    updated_at = NOW();
```

### Cursor Pagination (No Offset)
Scale pagination to millions of rows.

```sql
-- Bad: O(N) performance degradation
SELECT * FROM transactions ORDER BY created_at DESC LIMIT 20 OFFSET 100000;

-- Good: O(1) Seek Method
SELECT * FROM transactions
WHERE created_at < $last_seen_created_at
   OR (created_at = $last_seen_created_at AND id < $last_seen_id)
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

## 3. Operational Queries

### Detect Slow Queries
Requires `pg_stat_statements` extension.

```sql
SELECT
  substring(query, 1, 50) as query_snippet,
  calls,
  total_exec_time / calls as avg_time_ms,
  max_exec_time
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 10;
```

### Detect Unused Indexes
Identify indexes wasting write performance/storage.

```sql
SELECT
  schemaname || '.' || relname as table,
  indexrelname as index,
  idx_scan as times_used,
  pg_size_pretty(pg_relation_size(indexrelid)) as size
FROM pg_stat_user_indexes
WHERE idx_scan < 50
AND indexrelname NOT LIKE '%pkey'
ORDER BY pg_relation_size(indexrelid) DESC;
```

### Detect Connection Leaks
Check for idle connections hanging around.

```sql
SELECT state, count(*)
FROM pg_stat_activity
GROUP BY state;
```
