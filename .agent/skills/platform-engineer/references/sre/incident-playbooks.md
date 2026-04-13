# Incident Response Playbooks

## Overview

Panduan langkah-demi-langkah untuk menangani insiden produksi di PayU Platform.

---

## 🚨 Severity Classification

| Severity | Definition | Response Time | Example |
|----------|------------|---------------|---------|
| **SEV-1** | Complete service outage | < 15 min | All payments failing |
| **SEV-2** | Major feature degraded | < 30 min | BI-FAST transfers failing |
| **SEV-3** | Minor feature issue | < 2 hours | Slow dashboard loading |
| **SEV-4** | Low impact issue | < 24 hours | Minor UI bug |

---

## 📋 Playbook: Database Connection Pool Exhausted

### Symptoms
- `HikariPool-1 - Connection is not available` errors
- API latency spikes > 5 seconds
- 5xx error rate increasing

### Immediate Actions (< 5 min)
```bash
# 1. Check connection pool status
oc exec -it <pod> -- curl localhost:8080/actuator/metrics/hikaricp.connections.active

# 2. Identify slow queries
oc exec -it <postgres-pod> -- psql -c "SELECT pid, now() - pg_stat_activity.query_start AS duration, query 
FROM pg_stat_activity WHERE state = 'active' ORDER BY duration DESC LIMIT 10;"

# 3. Kill long-running queries if needed
oc exec -it <postgres-pod> -- psql -c "SELECT pg_terminate_backend(<pid>);"
```

### Mitigation (< 15 min)
```yaml
# Temporary: Scale up pods
oc scale deployment/wallet-service --replicas=5

# Increase connection pool (requires restart)
spring.datasource.hikari.maximum-pool-size: 30
```

### Root Cause Analysis
- [ ] Check for N+1 query patterns
- [ ] Review recent deployments
- [ ] Analyze query EXPLAIN plans
- [ ] Check for missing indexes

---

## 📋 Playbook: Kafka Consumer Lag

### Symptoms
- Consumer lag > 10,000 messages
- Processing delays > 1 minute
- Backpressure alerts firing

### Immediate Actions
```bash
# 1. Check consumer lag
oc exec -it kafka-0 -- kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group wallet-service-group \
  --describe

# 2. Check consumer health
oc logs -l app=wallet-service --tail=100 | grep -i "rebalance\|disconnect"

# 3. Temporary: Reset to latest (DATA LOSS - use carefully)
# kafka-consumer-groups.sh --reset-offsets --to-latest --execute
```

### Mitigation
```yaml
# Scale consumers
oc scale deployment/wallet-service --replicas=10

# Increase partition count (if needed)
kafka-topics.sh --alter --topic transactions --partitions 12
```

---

## 📋 Playbook: High Memory Usage / OOMKilled

### Symptoms
- Pod restarts with OOMKilled
- Memory usage > 90% for extended period
- Slow garbage collection

### Immediate Actions
```bash
# 1. Check memory usage
oc top pods -l app=account-service

# 2. Generate heap dump before OOM
oc exec -it <pod> -- jcmd 1 GC.heap_dump /tmp/heapdump.hprof

# 3. Copy heap dump for analysis
oc cp <pod>:/tmp/heapdump.hprof ./heapdump.hprof
```

### Mitigation
```yaml
# Increase memory limits
resources:
  limits:
    memory: "2Gi"  # was 1Gi
  requests:
    memory: "1Gi"
```

---

## 📋 Playbook: External Service Timeout (BI-FAST)

### Symptoms
- BI-FAST API timeout errors
- Circuit breaker OPEN
- Transfer success rate dropping

### Immediate Actions
```bash
# 1. Check circuit breaker status
curl localhost:8080/actuator/health | jq '.components.circuitBreakers'

# 2. Check external connectivity
oc exec -it <pod> -- curl -v https://bifast.bi.go.id/health

# 3. Check DNS resolution
oc exec -it <pod> -- nslookup bifast.bi.go.id
```

### Mitigation
```java
// Enable fallback mode
@CircuitBreaker(name = "bifast", fallbackMethod = "bifastFallback")
public TransferResult transfer(TransferRequest request) {
    // Queue for retry later
    return bifastFallback(request, new TimeoutException());
}
```

---

## 📋 Playbook: SSL Certificate Expiry

### Symptoms
- `SSLHandshakeException` errors
- External API calls failing
- Certificate expiry warnings

### Immediate Actions
```bash
# 1. Check certificate expiry
openssl s_client -connect api.payu.fajjjar.my.id:443 2>/dev/null | openssl x509 -noout -dates

# 2. Check cert-manager status
oc get certificates -A
oc describe certificate payu-tls -n payu
```

### Mitigation
```bash
# Force certificate renewal
oc delete secret payu-tls -n payu
# cert-manager will auto-regenerate
```

---

## 🔄 Post-Incident Checklist

- [ ] Timeline documented (when detected, actions taken, resolution)
- [ ] Root cause identified
- [ ] Customer communication sent (if SEV-1/SEV-2)
- [ ] Monitoring gaps identified and tickets created
- [ ] Runbook updated with learnings
- [ ] Post-mortem scheduled (within 48 hours for SEV-1/SEV-2)

---

## 📞 Escalation Matrix

| Severity | Primary | Secondary | Executive |
|----------|---------|-----------|-----------|
| SEV-1 | On-call SRE | Platform Lead | CTO |
| SEV-2 | On-call SRE | Service Owner | VP Engineering |
| SEV-3 | Service Owner | Team Lead | - |
| SEV-4 | Developer | - | - |
