# Database and Caching Optimization Guide

## Overview

This document describes the comprehensive database and caching optimizations implemented across all PayU backend services to improve performance, scalability, and reliability.

## Table of Contents

1. [Cache Starter Enhancements](#cache-starter-enhancements)
2. [Database Optimization](#database-optimization)
3. [Caching Strategy](#caching-strategy)
4. [Health Check Improvements](#health-check-improvements)
5. [Service Configuration](#service-configuration)
6. [Materialized Views](#materialized-views)
7. [Query Optimization](#query-optimization)
8. [Monitoring and Metrics](#monitoring-and-metrics)

---

## Cache Starter Enhancements

### Location
`backend/shared/cache-starter/src/main/java/id/payu/cache/`

### New Features

#### 1. Cache Warming on Startup
**File:** `CacheWarmingService.java`

- Pre-loads cache entries on application startup
- Configurable delay before starting warm-up
- Async execution with thread pool
- Metrics tracking for warm-up operations

**Configuration:**
```yaml
payu:
  cache:
    cache-warming:
      enabled: true
      startup-delay: 10s
      async: true
      thread-pool-size: 4
```

#### 2. Kafka-based Cache Invalidation
**Files:**
- `CacheInvalidationEvent.java` - Event model
- `CacheInvalidationPublisher.java` - Event publisher
- `CacheInvalidationConsumer.java` - Event consumer

- Cross-service cache invalidation via Kafka
- Support for single key, pattern, and all keys invalidation
- Tenant-aware invalidation support
- Automatic consumer group management

**Configuration:**
```yaml
payu:
  cache:
    invalidation:
      enabled: true
      topic: cache-invalidation
      consumer-group: account-cache-invalidation-group
```

**Usage:**
```java
@Autowired
private CacheInvalidationPublisher publisher;

// Invalidate single key
publisher.invalidateKey("accounts", "account:123", "account-service");

// Invalidate by pattern
publisher.invalidatePattern("accounts", "account:*", "account-service");

// Invalidate all
publisher.invalidateAll("accounts", "account-service");
```

#### 3. Enhanced Metrics Dashboard
**File:** Enhanced `LocalCacheService.java` with Micrometer metrics

Metrics exposed:
- `cache.local.hits` - Local cache hits
- `cache.local.misses` - Local cache misses
- `cache.local.evictions` - Cache evictions
- `cache.local.size` - Current cache size (gauge)
- `cache.distributed.hits` - Redis cache hits
- `cache.distributed.misses` - Redis cache misses
- `cache.distributed.stale` - Stale data served
- `cache.distributed.errors` - Redis errors

**Configuration:**
```yaml
payu:
  cache:
    metrics:
      enabled: true
      prefix: cache
      percentiles: true
      histogram: true
```

#### 4. Improved Local Cache Fallback
**File:** Enhanced `LocalCacheService.java`

- Circuit breaker pattern for Redis failures
- Automatic recovery after cooldown (30s)
- Health status reporting
- Statistics tracking with hit rate

**Features:**
- Automatic fallback to Caffeine when Redis is unavailable
- Cooldown period before attempting Redis recovery
- Health monitoring via `getHealth()` method
- Record stats option for production monitoring

---

## Database Optimization

### Primary and Read Replica Configuration

**Location:** Each service's `application.yml` and `DataSourceConfiguration.java`

#### Primary Database (Write Operations)
```yaml
spring:
  datasource:
    primary:
      jdbc-url: jdbc:postgresql://localhost:5432/payu_db
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
      hikari:
        maximum-pool-size: 20
        minimum-idle: 10
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
        connection-test-query: SELECT 1
        validation-timeout: 5000
        auto-commit: false
        leak-detection-threshold: 60000
```

#### Read Replica (Read Operations)
```yaml
spring:
  datasource:
    read-replica:
      enabled: ${READ_REPLICA_ENABLED:false}
      jdbc-url: jdbc:postgresql://read-replica:5432/payu_db
      hikari:
        maximum-pool-size: 40  # 2x primary for reads
        minimum-idle: 20
        read-only: true
```

### HikariCP Tuning

**Optimal Settings:**
- **maximum-pool-size**: `cores * 2 + effective_spindle_count` (typically 20)
- **minimum-idle**: Same as maximum-pool-size for consistent performance
- **connection-timeout**: 30 seconds
- **idle-timeout**: 10 minutes (600,000ms)
- **max-lifetime**: 30 minutes (1,800,000ms)
- **leak-detection-threshold**: 60 seconds (development)

**Connection Pool Formula:**
```
pool_size = cores * 2 + effective_spindle_count
```

For most systems:
- 4 cores → 10 connections
- 8 cores → 20 connections
- 16 cores → 40 connections

### Hibernate Optimization

**Configuration:**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
          order_inserts: true
          order_updates: true
        connection:
          provider_disables_autocommit: true
        generate_statistics: false  # Disable in production
        use_sql_comments: true
```

---

## Caching Strategy

### Cache Configuration

**Per-Service Configuration:**

#### Account Service
```yaml
payu:
  cache:
    caches:
      accounts:
        ttl: 10m
        stale-while-revalidate: true
      profiles:
        ttl: 5m
        stale-while-revalidate: false
```

#### Transaction Service
```yaml
payu:
  cache:
    caches:
      transactions:
        ttl: 5m
        stale-while-revalidate: true
      rates:
        ttl: 30m
        stale-while-revalidate: false
```

#### Wallet Service
```yaml
payu:
  cache:
    caches:
      balances:
        ttl: 1m
        stale-while-revalidate: true
      pockets:
        ttl: 5m
        stale-while-revalidate: false
```

### Cache TTL Guidelines

| Data Type | TTL | Reason |
|-----------|-----|---------|
| Balances | 1m | High frequency updates, need consistency |
| Account Details | 10m | Low change rate, can tolerate staleness |
| Transaction History | 5m | Recent transactions important |
| Exchange Rates | 30m | External data, low change rate |
| User Profiles | 5m | Moderate change rate |
| Configuration | 30m | Rarely changes |

---

## Health Check Improvements

### Liveness vs Readiness Separation

**Location:** `backend/account-service/src/main/java/id/payu/account/health/`

#### Liveness Probe
**File:** `LivenessHealthIndicator.java`

- Lightweight check for JVM status
- Monitors heap memory, thread count, uptime
- Use for Kubernetes liveness probes
- Should always return UP if JVM is alive

**Endpoint:** `/actuator/health/liveness`

#### Readiness Probe
**File:** `ReadinessHealthIndicator.java`

- Checks if all dependencies are available
- Tests DB connection (no query)
- Tests Redis connection (no ping)
- Checks Kafka listener status
- Use for Kubernetes readiness probes

**Endpoint:** `/actuator/health/readiness`

#### Deep Health Check
**File:** `DeepHealthIndicator.java`

- Performs active dependency verification
- Executes actual queries against DB
- PINGs Redis
- Tests Kafka producer
- Use for monitoring dashboards

**Endpoint:** `/actuator/health/deepHealth`

#### Dependency Health
**File:** `DependencyHealthIndicator.java`

- Consolidated view of all dependencies
- Includes liveness and readiness state
- Shows individual dependency status
- Use for service dependency overview

**Endpoint:** `/actuator/health/dependencies`

### Health Check Configuration

**Application Configuration:**
```yaml
management:
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
      db:
        enabled: true
      redis:
        enabled: true
      kafka:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

**Kubernetes Probes:**
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 10
  periodSeconds: 5
```

---

## Service Configuration

### Account Service

**Location:** `backend/account-service/src/main/resources/application.yaml`

Key optimizations:
- Primary + read replica datasource
- Optimized HikariCP settings
- Enhanced cache configuration
- Kafka producer with idempotence
- Health probe separation

### Transaction Service

**Location:** `backend/transaction-service/src/main/resources/application.yml`

Key optimizations:
- Primary + read replica datasource
- Kafka producer optimization
- Batch processing settings
- Improved consumer configuration
- Cache invalidation enabled

### Wallet Service

**Location:** `backend/wallet-service/src/main/resources/application.yaml`

Key optimizations:
- Primary + read replica datasource
- Balance caching with 1m TTL
- Pocket caching with 5m TTL
- Circuit breaker for account service
- Enhanced metrics

---

## Materialized Views

### Account Service
**Migration:** `V3__Create_materialized_views.sql`

Views created:
1. `mv_account_statistics` - Daily account statistics
2. `mv_account_balance_summary` - Daily balance summaries
3. `mv_kyc_processing_metrics` - KYC performance metrics
4. `mv_account_creation_trends` - Monthly creation trends

**Refresh function:** `refresh_analytics_views()`

### Transaction Service
**Migration:** `V6__Create_materialized_views.sql`

Views created:
1. `mv_transaction_daily_metrics` - Daily volume and value metrics
2. `mv_transaction_success_rates` - Success rates by type/channel
3. `mv_high_value_transactions` - High-value transaction tracking
4. `mv_transaction_hourly_patterns` - Hourly patterns
5. `mv_transaction_fee_summary` - Fee summaries

**Refresh function:** `refresh_transaction_analytics_views()`

### Wallet Service
**Migration:** `V5__Create_materialized_views.sql`

Views created:
1. `mv_wallet_balance_summary` - Daily wallet balances
2. `mv_pocket_balance_distribution` - Pocket distribution
3. `mv_ledger_daily_summary` - Ledger summaries
4. `mv_card_transaction_summary` - Card transaction metrics
5. `mv_wallet_active_users` - Active user counts

**Refresh function:** `refresh_wallet_analytics_views()`

### Scheduling Refresh

**Cron Job Example:**
```sql
-- Refresh every hour
CREATE EXTENSION IF NOT EXISTS pg_cron;

SELECT cron.schedule('refresh-analytics', '0 * * * *',
    'SELECT refresh_analytics_views()');
```

**Java Scheduled Task:**
```java
@Scheduled(cron = "0 0 * * * *")  // Every hour
public void refreshMaterializedViews() {
    jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_account_statistics");
    // ... other views
}
```

---

## Query Optimization

### Index Strategy

#### Account Service
**Migration:** `V4__Add_performance_indexes.sql`

Key indexes:
- `idx_accounts_customer_status` - Customer account lookups
- `idx_accounts_account_number` - Account number searches
- `idx_accounts_kyc_status_created` - KYC status queries
- `idx_balances_account_updated` - Balance queries
- `idx_profiles_email_unique` - Login/email lookups

#### Transaction Service
**Migration:** `V7__Add_performance_indexes.sql`

Key indexes:
- `idx_transactions_sender_created` - Sender history
- `idx_transactions_receiver_created` - Receiver history
- `idx_transactions_status_created` - Pending/failed queries
- `idx_transactions_amount_desc` - High-value transactions
- `idx_transactions_reference_unique` - Idempotency checks

#### Wallet Service
**Migration:** `V6__Add_performance_indexes.sql`

Key indexes:
- `idx_wallets_customer_created` - Customer wallets
- `idx_balances_wallet_updated` - Balance lookups
- `idx_ledger_wallet_created` - Ledger queries
- `idx_cards_wallet_status` - Active cards
- `idx_card_transactions_card_created` - Card transactions

### Index Types

#### B-tree indexes (default)
Best for:
- Equality queries
- Range queries
- Sort operations

```sql
CREATE INDEX idx_accounts_customer ON accounts(customer_id);
```

#### Partial indexes
Best for:
- Filtering common subsets
- Reducing index size

```sql
CREATE INDEX idx_accounts_active
ON accounts(customer_id, status)
WHERE status = 'ACTIVE';
```

#### Covering indexes (INCLUDE)
Best for:
- Index-only scans
- Hot query paths

```sql
CREATE INDEX idx_accounts_covering
ON accounts(customer_id)
INCLUDE (account_number, status, kyc_status);
```

#### BRIN indexes
Best for:
- Time-series data
- Very large tables

```sql
CREATE INDEX idx_transactions_created_brin
ON transactions USING BRIN (created_at);
```

### Query Optimization Tips

1. **Use EXPLAIN ANALYZE**
```sql
EXPLAIN ANALYZE
SELECT * FROM accounts WHERE customer_id = '123';
```

2. **Avoid SELECT ***
```sql
-- Good
SELECT id, account_number, status FROM accounts WHERE customer_id = '123';

-- Bad
SELECT * FROM accounts WHERE customer_id = '123';
```

3. **Use prepared statements**
```java
@Query("SELECT a FROM Account a WHERE a.customerId = :customerId")
Account findByCustomerId(@Param("customerId") String customerId);
```

4. **Limit results**
```java
@Query("SELECT a FROM Account a WHERE a.customerId = :customerId")
Page<Account> findByCustomerId(@Param("customerId") String customerId, Pageable pageable);
```

---

## Monitoring and Metrics

### Prometheus Metrics

**Cache Metrics:**
```
cache_local_hits_total
cache_local_misses_total
cache_local_evictions_total
cache_local_size
cache_distributed_hits_total
cache_distributed_misses_total
cache_distributed_stale_total
cache_distributed_errors_total
```

**Database Metrics:**
```
hikaricp_connections_active
hikaricp_connections_idle
hikaricp_connections_max
hikaricp_connections_min
hikaricp_connections_pending
```

**Health Metrics:**
```
health_deep_database
health_deep_redis
health_deep_kafka
```

### Grafana Dashboard Queries

**Cache Hit Rate:**
```promql
rate(cache_local_hits_total[5m]) / (rate(cache_local_hits_total[5m]) + rate(cache_local_misses_total[5m]))
```

**DB Connection Pool Usage:**
```promql
hikaricp_connections_active / hikaricp_connections_max
```

**Deep Health Status:**
```promql
health_deep_database + health_deep_redis + health_deep_kafka
```

### Alerting Rules

**High Cache Miss Rate:**
```yaml
- alert: HighCacheMissRate
  expr: rate(cache_local_misses_total[5m]) / rate(cache_local_hits_total[5m]) > 0.5
  for: 5m
  annotations:
    summary: "High cache miss rate detected"
```

**DB Pool Exhaustion:**
```yaml
- alert: DBPoolNearCapacity
  expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
  for: 5m
  annotations:
    summary: "Database connection pool near capacity"
```

**Deep Health Failure:**
```yaml
- alert: DeepHealthUnhealthy
  expr: health_deep_database + health_deep_redis + health_deep_kafka < 3
  for: 1m
  annotations:
    summary: "Deep health check failed"
```

---

## Best Practices

### 1. Database
- Use read replicas for reporting queries
- Keep connection pools properly sized
- Monitor for connection leaks
- Use batch operations when possible
- Analyze slow query logs regularly

### 2. Caching
- Set appropriate TTLs based on data volatility
- Use stale-while-revalidate for hot data
- Enable cache warming for critical data
- Monitor cache hit rates
- Implement cache invalidation for data changes

### 3. Health Checks
- Separate liveness and readiness probes
- Use deep health for monitoring
- Set appropriate timeouts and thresholds
- Test health checks in staging
- Monitor health check endpoint response times

### 4. Performance
- Use EXPLAIN ANALYZE for slow queries
- Create indexes based on actual query patterns
- Use partial indexes to reduce size
- Monitor index usage
- Rebuild indexes periodically

### 5. Monitoring
- Set up meaningful dashboards
- Configure alerting for critical metrics
- Track SLIs (Service Level Indicators)
- Monitor trends over time
- Regular performance reviews

---

## Migration Checklist

### Prerequisites
- [ ] Backup all databases
- [ ] Test in staging environment
- [ ] Prepare rollback plan
- [ ] Schedule maintenance window

### Steps
1. [ ] Deploy cache-starter changes
2. [ ] Update service configurations
3. [ ] Run database migrations
4. [ ] Create materialized views
5. [ ] Verify health endpoints
6. [ ] Check cache metrics
7. [ ] Test read replica queries
8. [ ] Monitor for errors

### Validation
- [ ] Health endpoints responding correctly
- [ ] Cache metrics being collected
- [ ] Read replica queries working
- [ ] Materialized views populated
- [ ] No increase in error rate
- [ ] Performance improvements observed

---

## Troubleshooting

### Cache Not Warming
**Symptoms:** High cache miss rate on startup

**Solutions:**
1. Check cache warming is enabled
2. Verify warm keys are configured
3. Check logs for errors during warm-up
4. Ensure Redis is available

### Read Replica Not Used
**Symptoms:** All queries going to primary

**Solutions:**
1. Verify read replica is enabled
2. Check read replica datasource configuration
3. Verify read replica is reachable
4. Check application logs for connection errors

### High DB Connection Usage
**Symptoms:** Connections near max pool size

**Solutions:**
1. Check for connection leaks
2. Increase pool size if needed
3. Reduce connection timeout
4. Review query performance

### Materialized View Stale
**Symptoms:** Old data in analytics

**Solutions:**
1. Verify refresh function exists
2. Check refresh schedule
3. Manually refresh: `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_name`
4. Check for long-running refresh queries

### Health Check Failing
**Symptoms:** Readiness probe failing

**Solutions:**
1. Check individual dependency status
2. Verify network connectivity
3. Check credentials
4. Review logs for specific errors

---

## References

- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [PostgreSQL Index Types](https://www.postgresql.org/docs/current/indexes-types.html)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Kafka Best Practices](https://kafka.apache.org/documentation/#producerconfigs)
- [Redis Caching Best Practices](https://redis.io/topics/lru-cache)

---

*Last Updated: January 2026*
