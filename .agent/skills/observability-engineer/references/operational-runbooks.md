# PayU Operational Runbooks

Standard Operating Procedures (SOP) for handling critical production incidents.

## 1. Service Outage ("Red Health")

**Trigger**: `ServiceDown` alert or P0 from Support.

### Immediate Mitigation (Triage)
1.  **Check Pod Status**:
    ```bash
    oc get pods -n payu-prod -l app=<service_name>
    ```
2.  **CrashLoopBackOff?**: Check logs for startup errors.
    ```bash
    oc logs -n payu-prod -l app=<service_name> --tail=100
    ```
3.  **Recent Deployment?**:
    ```bash
    oc rollout history deployment/<service_name>
    # If suspicious, ROLLBACK IMMEDIATELY
    oc rollout undo deployment/<service_name>
    ```

### High Latency ("Yellow Health")

**Trigger**: `HighLatency` (P95 > 2s) alert.

1.  **Check DB Connection Pool**:
    ```bash
    oc rsh deployment/<service_name> curl -s localhost:8080/actuator/metrics/hikaricp.connections.active
    ```
    If pool is saturated, scale up checking database CPU first.
2.  **Check Slow Queries**:
    ```sql
    SELECT pid, now() - query_start AS duration, query
    FROM pg_stat_activity
    WHERE state = 'active' AND duration > interval '2 seconds';
    ```

## 2. Database Incidents (PostgreSQL)

### Connection Pool Exhaustion
**Symptoms**: `ConnectionTimeoutException` in logs.

```sql
-- Kill idle connections > 10m
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle' AND query_start < now() - interval '10 minutes';
```

### Disk Space Critical
**Symptoms**: DB entering read-only mode.

1.  **Identify Blob Tables**:
    ```sql
    SELECT relname, pg_size_pretty(pg_total_relation_size(relid))
    FROM pg_catalog.pg_statio_user_tables
    ORDER BY pg_total_relation_size(relid) DESC LIMIT 5;
    ```
2.  **Emergency Vacuum**:
    ```sql
    VACUUM (VERBOSE, ANALYZE) large_table_name;
    ```

## 3. Communication Template

**Slack Channel**: `#incident-war-room`

```text
🚨 **INCIDENT ALERT** 🚨
**Service**: [Service Name]
**Severiy**: P0 / P1
**Impact**: [Description, e.g., Users cannot login]
**Status**: Investigating
**Commander**: @[YourName]
**Video Link**: [Zoom/Meet Link]
```
