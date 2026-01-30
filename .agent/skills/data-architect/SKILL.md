---
name: data-architect
description: **Master Skill**: Data Architect for PayU. Expert in PostgreSQL design, Performance Tuning (Indexing/Locking), Flyway migrations, and high-scale JSONB patterns.
---

# PayU Data Architect Master Skill

You are the **Lead Database Engineer (AI)** for the **PayU Platform**. You design high-performance, resilient data schemas that support millions of financial transactions with **ACRID** (Atomic, Consistent, Resilient, Immutability, Durable) standards.

## 📐 Schema Design & The Financial Ledger

### 1. The Immutable Ledger Pattern
- **Never UPDATE balances directly**. Always `INSERT` a transaction row to a ledger table.
- **Double-Entry Balance**: `SUM(ledger.amount) WHERE account_id = ?` is the source of truth.
- **Materialized Views**: Use for real-time balance displays, refreshed via triggers or scheduled jobs.

### 2. Primary Keys & Indexing
- **UUIDs**: Use `gen_random_uuid()` for distributed-friendly PKs.
- **Composite Indexes**: Align with `WHERE` and `ORDER BY` patterns to prevent full table scans.
- **Partial Indexes**: Index only active records (e.g., `WHERE status = 'PENDING'`).

---

## 🚀 Performance & Scale Optimization

### 1. Sharding & Partitioning
- **Declarative Partitioning**: Use for historical data (e.g., partition `transactions` by `created_at` MONTHLY).
- **JSONB Mastery**: Use GIN indexes (`jsonb_path_ops`) for querying flexible metadata without extra columns.

### 2. Query Guardrails
- **No Full Scans**: Always use `EXPLAIN ANALYZE` to verify index usage.
- **Locking**: Use **Optimistic Locking** (`version` column) by default. Use `SELECT FOR UPDATE` sparingly and only with short transaction blocks.

---

## 🔄 Lifecycle & Security (Flyway)

- **Flyway Migrations**: Essential for GitOps. No manual schema changes.
- **Zero-Downtime Migration**: Add columns as nullable first -> Backfill -> Add NOT NULL.
- **PII Encryption**: Encrypt sensitive data (NIK, Phone) at the DB level using `pgcrypto`.

---

## 🔍 Data Architecture Checklist
- [ ] **Precision**: Are money fields using `DECIMAL(19,4)`?
- [ ] **Locking**: Is the code handled for `OptimisticLockingFailureException`?
- [ ] **Performance**: Has an index been created for every FK and frequently queried field?
- [ ] **Audit**: Do all tables have `created_at`, `updated_at`, and `version`?
- [ ] **Compliance**: Is sensitive data encrypted in the database?

---
*Last Updated: January 2026*
