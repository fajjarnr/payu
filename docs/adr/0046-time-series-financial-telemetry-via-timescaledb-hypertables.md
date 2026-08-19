# ADR-0046: Time-Series Financial Telemetry via TimescaleDB Hypertables

**Status**: Accepted  
**Date**: 2026-08-19  
**Deciders**: Principal Architect, Data Architect, AI Engineer  
**Relates to**: ADR-0036 (analytics-service TimescaleDB), READY-062, QAMVP-004  

---

## Context

`analytics-service/src/app/config.py:37` `timescale_hypertable_retention_days=365` `chunk_interval_days=7` + `database_url postgresql+asyncpg` with `TimescaleDB` indicates telemetry need: per-minute `balance`, `transaction`, `fraud_score`, `velocity` aggregates. Plain `PostgreSQL` `500M` rows → `seq scan` slow; need hypertable compression + continuous aggregate.

## Decision Drivers

* **Retention 365d** for OJK audit, compress after 7d.
* **Query** `p95 <100ms` for dashboard `last 30d`.
* **No extra stack** — reuse `PostgreSQL` `CNPG`, not `Influx`.

## Considered Options

### Option 1 — TimescaleDB hypertable + compression + continuous aggregate (dipilih)

Pros: `SQL`, `CNPG` native, `retention 365d`. Cons: extension enable — already in `analytics-service`.

### Option 2 — Plain PG

Pros: no extension. Cons: slow `365d` scan — ditolak.

## Decision

**TimescaleDB hypertables** for `analytics-service` telemetry.

* Table `telemetry_event` `(time TIMESTAMPTZ, tenant_id, metric, value, tags JSONB)` → `SELECT create_hypertable('telemetry_event','time', chunk_time_interval => INTERVAL '7 days')`.
* Policy: `add_retention_policy('telemetry_event', INTERVAL '365 days')`, `add_compression_policy('telemetry_event', INTERVAL '7 days')`, `add_continuous_aggregate_policy` hourly/daily.
* `analytics-service` `asyncpg` `TimescaleDBConfig` `retention 365d` `chunk 7d`; query via `time_bucket`.
* `CNPG` `Postgres` with `timescaledb` image.

## Consequences

**Positive**: fast `365d` query, compressed `10×`.

**Negative**: `timescaledb` image — mitigasi `CNPG` `803` already.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | DDL | `backend/analytics-service/src/main/resources/db/migration/V2__hypertable.sql` |
| 2 | Config | `backend/analytics-service/src/app/config.py:37` |

**Verification**: `SELECT * FROM timescaledb_information.hypertables` shows `telemetry_event`; `chunk_compression` `true`.

---
*Created for analytics telemetry — implementasi wajib refer ADR ini.*
