---
name: data-architect
description: PayU data architecture for PostgreSQL and service-owned schemas, including immutable financial ledgers, Flyway migrations, constraints, indexing, locking, JSONB/PII, outbox and idempotency persistence, replication, backup/restore, and Context7-first database tooling verification.
---

# Data Architect — PayU

Use this skill when designing, reviewing, migrating, tuning, or troubleshooting PayU data stores and persistence boundaries. Treat the database as the enforcement layer for financial invariants, not merely a persistence detail.

## Operating contract

1. Read `AGENTS.md` and the relevant architecture, security, migration, and operations guide before changing a schema or query.
2. Locate the owning service, its migration directory, datasource configuration, entity/repository mappings, and integration tests before proposing SQL.
3. Start from access patterns, invariants, retention, consistency, and failure behavior. Do not start from a fashionable database feature.
4. Before using a third-party database library, extension, operator, or CLI, resolve the official library with Context7 and query the exact installed version. If unavailable, use the nearest documented version, record the mismatch, and avoid undocumented APIs.
5. Keep migrations forward-only and additive in production. Never rewrite an applied versioned migration to “fix” it.
6. Make the smallest safe change. Do not add a replica, partitioning scheme, extension, cache, CDC pipeline, or abstraction without a measured problem and an operational owner.
7. Prove claims with migration tests, query plans, lock/latency evidence, backup/restore evidence, or reconciliation output. Do not call a schema “high availability” because it has replicas on paper.

## Repository baseline

Verify these values against the manifest and cluster overlays before each task; they can drift.

| Area | Observed baseline |
|---|---|
| Database | PostgreSQL 16.8 managed by CloudNativePG, three instances |
| Cluster services | `payu-database-rw`, `payu-database-ro`, and `payu-database-r` |
| Backend | Spring Boot 4.1.0, Java 25, PostgreSQL JDBC 42.7.12 |
| Migrations | Service-local `src/main/resources/db/migration`, loaded as `classpath:db/migration` |
| Migration tests | Prefer Testcontainers PostgreSQL; the parent manages Testcontainers 2.0.5 |
| Shared persistence | `datasource-starter`, `outbox-starter`, and `api-commons` idempotency port |
| CDC status | CNPG baseline uses `wal_level=replica`; logical CDC is not enabled by default |

The service POM, lockfile/BOM, Flyway history, and deployed manifest win over this table.

## Design workflow

1. **Map ownership** — identify the service database, tables, migrations, repositories, writers, readers, and external consumers.
2. **Write invariants** — money precision, uniqueness, valid state transitions, tenant isolation, retention, and reconciliation rules.
3. **List access patterns** — filters, sort order, cardinality, page size, freshness, and read-after-write requirements.
4. **Choose the simplest relational design** — normalized source-of-truth tables, explicit constraints, projections only when measured reads require them.
5. **Plan expand/contract** — add compatible schema first, deploy readers/writers, backfill safely, validate, then enforce or remove old shape in a later migration.
6. **Test the real boundary** — run Flyway against PostgreSQL Testcontainers, exercise concurrent writes and failure paths, and verify the query plan.
7. **Operate it** — define metrics, alerts, backup/PITR, restore drill, retention, and the owner for the new data path.

## Financial data and ledger rules

- Store all financial amounts as `DECIMAL(19,4)`/`NUMERIC(19,4)` and calculate in Java with `BigDecimal` and `RoundingMode.HALF_EVEN`.
- Financial entries are append-only. Never update or delete a posted ledger/journal entry, transaction amount, or balance history. Correct by reversal/compensating entries linked to the original reference.
- A money movement must produce balanced double-entry postings: total debits equal total credits for the transaction, currency is explicit, and the posting is atomic.
- Enforce what PostgreSQL can enforce with `NOT NULL`, `CHECK`, `UNIQUE`, foreign keys, and transaction boundaries. Cross-row accounting balance still needs a transactional application invariant plus reconciliation tests/jobs.
- A mutable wallet balance or materialized view is a derived projection, not the ledger. Update it only in the same protected workflow and reconcile it against journal entries.
- Do not use floating-point columns, implicit casts, database `money`, or a generic JSON amount for financial values.
- Do not use a normal upsert to mutate ledger rows. `ON CONFLICT` is appropriate for idempotency/metadata records only when the conflict semantics and affected-row result are explicit.
- Keep audit/outbox delivery metadata separate from immutable financial facts. Retention cleanup for delivery metadata must never become a pattern for deleting ledger data.

## Schema design

- Prefer UUIDs using the repository's existing generation strategy (`gen_random_uuid()` is used by current migrations); do not change key format without a migration and consumer plan.
- Store instants as `TIMESTAMPTZ` and map them to UTC-aware Java types. Do not use server-local time for ordering or reconciliation.
- Model required query fields as typed columns. Use JSONB for bounded, evolving metadata—not for fields that need financial constraints, joins, authorization, or frequent filtering.
- Give every tenant-owned table an explicit tenant key and enforce tenant scoping in constraints, repository queries, and (where applicable) RLS. Do not assume an application filter is sufficient isolation.
- Name constraints and indexes deterministically. Define foreign-key delete behavior deliberately; financial references should normally be `RESTRICT`, not cascade deletion.
- Keep derived/materialized views disposable and rebuildable. Document refresh lag and the source query.

## Immutable journal and transactional outbox

The current wallet migrations contain `ledger_entries`, journal/chart-of-accounts tables, and an `outbox_events` table. Preserve these boundaries:

```text
command transaction
  ├─ validate ownership, idempotency, and limits
  ├─ post balanced immutable journal entries
  ├─ update derived projection if required
  └─ insert one CloudEvents-compatible outbox record

outbox publisher
  ├─ claim pending rows with short, bounded locks
  ├─ publish through the shared outbox starter
  ├─ mark published or increment retry metadata
  └─ alert/reconcile exhausted or ambiguous records
```

- Never call `kafkaTemplate.send()` directly for PayU domain events; use `outbox-starter` and preserve topic/schema contracts.
- `FOR UPDATE SKIP LOCKED` is suitable for independent outbox/job workers. It is not permission to skip a financial row and continue a posting.
- Keep claiming transactions short. Lock rows in a stable order and never hold database locks across network calls.
- Use a unique business key or durable idempotency record for financial outcomes. A short-lived Redis idempotency entry is useful for coordination, but it cannot be the only durable record when replay must remain safe after TTL expiry.
- Store a request fingerprint and distinguish the same-key/same-request replay from same-key/different-request conflict.

## Flyway migrations

- Follow the service's existing `V<number>__description.sql` naming and `classpath:db/migration` location. Confirm the next version across the service before creating a file.
- Treat applied versioned migrations as immutable. Flyway validation/checksums are a safety boundary; fix a bad migration with a new forward migration and a documented recovery plan.
- Test ordering, repeatability, clean installation, upgrade from a representative prior schema, and application startup against PostgreSQL—not only H2. Use H2 only where the service already has a deliberately compatible `migration-h2` path.
- Do not hide schema drift with indiscriminate `IF NOT EXISTS`. Use it only when the migration is intentionally safe to re-run and add an assertion/test for the expected shape.
- For a large column addition: add nullable, deploy compatible code, backfill in bounded batches, monitor locks/WAL, validate values, then add constraints/defaults in a later step.
- Prefer `NOT VALID` plus a separate `VALIDATE CONSTRAINT` for large existing tables when supported by the migration plan.
- `CREATE INDEX CONCURRENTLY` cannot share the normal transactional migration path. Isolate it according to the installed Flyway version/configuration, run it with the correct operational lock/timeout policy, and test the deployment procedure.
- Never mix a destructive rename/drop with the first deployment that stops writing the old column. Keep a rollback/read compatibility window.
- Rollback for production is normally restore/forward-fix, not blind `clean`, down migrations, or editing history.

## Indexes and query performance

1. Capture the actual query, bind values, result cardinality, and freshness requirement.
2. Inspect existing indexes and write amplification before adding one.
3. Use `EXPLAIN (ANALYZE, BUFFERS)` in a representative non-production environment; compare planning and execution time before/after.
4. Verify the index is used for the real predicate and sort, not merely that it exists.

- Design composite indexes from the equality predicates, then range/order columns; verify selectivity and write cost.
- Use partial indexes for stable hot subsets such as pending outbox rows, with the predicate matching the query.
- Use `INCLUDE` only for a measured index-only read; it increases storage and write cost.
- Use GIN for JSONB containment/path queries only after measuring the access pattern. `jsonb_path_ops` and the default operator class support different query shapes.
- Prefer keyset pagination with a stable `(created_at, id)` cursor for large transaction histories. Do not hide an unbounded `OFFSET` behind a repository method.
- Select required columns, bound result size, avoid accidental N+1 queries, and avoid functions/casts on indexed predicates unless an expression index is intentional.
- Re-check plans after statistics changes, data growth, PostgreSQL upgrades, or a new tenant distribution.

## Transactions, locking, and replicas

- Choose isolation and lock strength from the invariant. Use optimistic version checks for ordinary concurrent edits; use short pessimistic locks for serialized account/queue work.
- Do not lock rows and then call a remote service. Record intent, commit, and process external work through an outbox/saga boundary.
- Use `SKIP LOCKED` only when skipped work can safely be retried by another worker.
- The shared datasource starter exposes an explicit read-replica datasource/JdbcTemplate. `@Transactional(readOnly = true)` alone does not magically route JPA work to a replica; verify the actual routing configuration and repository path.
- Reads after a financial write require primary/read-your-write semantics until the projection is confirmed. Do not send a just-created transaction to a lagging replica and call it missing.
- Monitor connection-pool saturation, transaction age, blocked sessions, deadlocks, replica lag, WAL volume, autovacuum health, and table/index bloat before tuning pool or PostgreSQL parameters.
- Do not copy fixed settings such as `shared_buffers=25% RAM`, pool sizes, or `work_mem` from a generic guide. Tune from workload, pod limits, and measured plans; `work_mem` is per operation and can multiply across connections.

## JSONB, PII, and RLS

- Separate public/queryable metadata from sensitive values. Do not put NIK, PIN, CVV, secrets, or raw tokens in queryable JSONB.
- Prefer application/KMS/Vault-managed encryption for sensitive data. Use `pgcrypto` only with an explicit key lifecycle, rotation, access, and backup policy; never place encryption keys in SQL migrations or source.
- If encrypted data must be looked up, design a keyed blind index with an approved threat model. Do not decrypt every row to filter it.
- RLS policies must cover the relevant commands, use a transaction-scoped tenant context, and be tested with the restricted application role. Clear tenant context on pooled connections.
- Do not rely on RLS when the application connects as a superuser or table owner that bypasses the intended policy. Keep service roles least-privileged.
- Redact PII in SQL logs, migration output, traces, query samples, and incident artifacts.

## Replication, CDC, backup, and recovery

- The deployed CNPG baseline is PostgreSQL 16.8 with `wal_level=replica`, synchronous quorum settings, separate read services, and WAL storage. Verify the live cluster before making HA claims.
- If Debezium/CDC is introduced, plan `wal_level=logical`, publications, replication slots, connector offsets/schema history, privileges, snapshot behavior, duplicate delivery, and WAL-retention alerts as one change. An abandoned slot can exhaust disk and cause catalog/WAL pressure.
- Never configure a CDC decimal mode that converts PayU money to floating point. Resolve the exact Debezium version and precise decimal handling through Context7 before writing connector configuration.
- Backups are not recovery until a restore has been rehearsed. Define RPO/RTO, PITR boundaries, encryption, secret restoration, tenant/database selection, and post-restore Flyway validation.
- Test failover, replica promotion, network partition, backup restore, migration interruption, and outbox replay. Record measured recovery time and data-loss window.

## Extensions and time-series data

- PostgreSQL extensions (`pgcrypto`, `pg_stat_statements`, logical decoding, and others) require an operator/image/privilege decision plus migration and backup support. Do not assume `CREATE EXTENSION` is available in every environment.
- TimescaleDB and `pg_partman` are not part of the observed CNPG baseline. Use them only after an explicit capacity/retention decision, supported image/operator verification, and Context7 documentation check.
- Native PostgreSQL partitioning is appropriate when retention, pruning, or maintenance evidence justifies it. Define the partition key, default/future partition, unique-key implications, migration of existing data, and operational creation process before enabling it.
- Never use partition dropping as a shortcut to delete financial records. Apply regulatory retention and immutable archival requirements first.

## Verification checklist

- [ ] Service owner, schema, migration history, writers, readers, and consumers are identified.
- [ ] Financial amounts use `DECIMAL(19,4)`/`BigDecimal`/`HALF_EVEN`; ledger rows are immutable and double-entry balanced.
- [ ] Idempotency uniqueness/fingerprint and outbox atomicity are tested under replay and concurrency.
- [ ] Constraints cover nullability, ranges, ownership, tenant scope, and delete behavior.
- [ ] Migration is append-only, expand/contract-safe, PostgreSQL-tested, and safe around locks/transactions.
- [ ] Query plan, cardinality, index write cost, pagination, and replica consistency are measured.
- [ ] PII/key lifecycle, RLS role behavior, logs, backups, and retention are reviewed.
- [ ] HA/CDC/partitioning/extension claims match deployed configuration, not a generic template.
- [ ] Restore/failover/reconciliation evidence and the focused test command are attached to the handoff.

## Official documentation to resolve through Context7

Resolve these before relying on current syntax or configuration:

- PostgreSQL 16: https://www.postgresql.org/docs/16/
- Flyway: https://documentation.red-gate.com/flyway
- Debezium PostgreSQL connector: https://debezium.io/documentation/reference/stable/connectors/postgresql.html
- CloudNativePG: https://cloudnative-pg.io/documentation/current/
- Testcontainers Java PostgreSQL module: https://java.testcontainers.org/modules/databases/postgres/
