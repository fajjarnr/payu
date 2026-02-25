# PayU Digital Banking Platform - Zero-Downtime Deployment Guide

> **Version**: 1.0 | **Last Updated**: February 20, 2026 | **Status**: Production Ready
>
> **Scope**: OpenShift 4.20+ | ArgoCD GitOps | 22 Microservices

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Deployment Strategies Overview](#deployment-strategies-overview)
3. [Blue-Green Deployment](#blue-green-deployment)
4. [Canary Releases](#canary-releases)
5. [Database Migration Safety](#database-migration-safety)
6. [Rollback Procedures](#rollback-procedures)
7. [Deployment Scripts](#deployment-scripts)
8. [Monitoring & Verification](#monitoring--verification)
9. [Testing Procedures](#testing-procedures)
10. [Emergency Procedures](#emergency-procedures)

---

## Executive Summary

This guide provides comprehensive zero-downtime deployment strategies for the PayU Digital Banking Platform, ensuring continuous availability during releases.

### Supported Strategies

| Strategy | Use Case | Complexity | Rollback Time |
|----------|----------|------------|---------------|
| **Blue-Green** | Major releases, database migrations | Medium | ~30 seconds |
| **Canary** | Gradual rollouts, A/B testing | High | ~10 seconds |
| **Rolling** | Patch updates, simple changes | Low | ~2 minutes |

### Key Principles

1. **Backward Compatibility**: Database migrations must be backward-compatible
2. **Health Verification**: Automated health checks before traffic shift
3. **Instant Rollback**: Single command rollback capability
4. **Observability**: Real-time monitoring during deployment
5. **Data Integrity**: Zero data loss guarantee

---

## Deployment Strategies Overview

### Decision Matrix

| Scenario | Recommended Strategy | Reason |
|----------|---------------------|--------|
| Database schema changes | Blue-Green | Requires compatibility window |
| Critical bug fix | Blue-Green | Fast rollback if needed |
| New feature rollout | Canary | Gradual user exposure |
| Configuration changes | Rolling | Simple, low risk |
| Performance optimization | Canary | Measure impact gradually |

---

## Blue-Green Deployment

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Blue-Green Deployment                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌──────────────┐                    ┌──────────────┐          │
│   │   BLUE       │  ←── Traffic ──→   │   GREEN      │          │
│   │  (Active)    │      Router        │  (Standby)   │          │
│   │   v1.2.0     │                    │   v1.3.0     │          │
│   └──────────────┘                    └──────────────┘          │
│         ↑                                   ↑                    │
│         └─────────── Health Checks ─────────┘                    │
│                                                                  │
│   Deployment Flow:                                               │
│   1. Deploy GREEN (v1.3.0)                                      │
│   2. Run health checks on GREEN                                 │
│   3. Switch traffic to GREEN                                    │
│   4. Monitor for issues                                         │
│   5. If issues: switch back to BLUE                             │
│   6. If stable: BLUE becomes standby for next release           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Procedure

#### Step 1: Pre-Deployment Checklist

```bash
# Verify current state
oc get pods -n payu-dev -l version=blue
oc get route -n payu-dev gateway-service

# Check database migration compatibility
./scripts/deployment/verify-db-compatibility.sh

# Run smoke tests on current version
./scripts/deployment/smoke-test.sh blue
```

#### Step 2: Deploy Green Environment

```bash
# Deploy new version to green
oc apply -k infrastructure/openshift/overlays/blue-green/green/

# Wait for rollout
oc rollout status deployment/gateway-service-green -n payu-dev --timeout=300s

# Run health checks
./scripts/deployment/verify-deployment.sh green
```

#### Step 3: Traffic Switch

```bash
# Switch traffic to green (instant)
oc patch route gateway-service -n payu-dev -p \
  '{"spec":{"to":{"name":"gateway-service-green"}}}'

# Monitor for 5 minutes
./scripts/deployment/monitor-deployment.sh green 300
```

#### Step 4: Rollback (if needed)

```bash
# Instant rollback to blue
oc patch route gateway-service -n payu-dev -p \
  '{"spec":{"to":{"name":"gateway-service-blue"}}}'

# Verify rollback
./scripts/deployment/verify-deployment.sh blue
```

---

## Canary Releases

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Canary Deployment                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Incoming Traffic                                               │
│        │                                                         │
│        ▼                                                         │
│   ┌─────────┐                                                    │
│   │ Router  │──┬── 90% ──→ ┌──────────────┐ (v1.2.0 - Stable)   │
│   │ (Istio) │  │           │   STABLE     │                     │
│   │         │  │           │   v1.2.0     │                     │
│   │         │  └── 10% ──→ └──────────────┘                     │
│   │         │              ┌──────────────┐ (v1.3.0 - Canary)    │
│   │         │              │   CANARY     │                     │
│   └─────────┘              │   v1.3.0     │                     │
│                            └──────────────┘                     │
│                                                                  │
│   Progressive Rollout:                                           │
│   Phase 1: 10% → 25% → 50% → 75% → 100%                         │
│   Phase 2: Monitor metrics at each stage                        │
│   Phase 3: Auto-rollback if error rate > 1%                     │
│   Phase 4: Promote to 100% or rollback                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Canary Configuration

```yaml
# VirtualService for traffic splitting
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: gateway-service
  namespace: payu-dev
spec:
  hosts:
    - gateway-dev.payu.fajjjar.my.id
  http:
    - match:
        - headers:
            canary:
              exact: "true"
      route:
        - destination:
            host: gateway-service-canary
          weight: 100
    - route:
        - destination:
            host: gateway-service-stable
          weight: 90
        - destination:
            host: gateway-service-canary
          weight: 10
```

### Procedure

#### Step 1: Deploy Canary (10%)

```bash
# Deploy canary version
./scripts/deployment/canary-deploy.sh gateway-service 1.3.0 10

# Monitor for 10 minutes
./scripts/deployment/monitor-canary.sh gateway-service 600
```

#### Step 2: Progressive Rollout

```bash
# Promote to 25%
./scripts/deployment/canary-promote.sh gateway-service 25

# Monitor
./scripts/deployment/monitor-canary.sh gateway-service 600

# Promote to 50%
./scripts/deployment/canary-promote.sh gateway-service 50

# Monitor
./scripts/deployment/monitor-canary.sh gateway-service 900

# Promote to 100% (remove canary)
./scripts/deployment/canary-promote.sh gateway-service 100
```

#### Step 3: Rollback (if needed)

```bash
# Instant rollback to 0%
./scripts/deployment/canary-rollback.sh gateway-service

# Or automated rollback on threshold breach
./scripts/deployment/canary-auto-rollback.sh gateway-service
```

---

## Database Migration Safety

### Expand-Contract Pattern

```
Phase 1: Expand (Deploy)
┌─────────────────────────────────────────┐
│  Old Code (v1.2.0)   New Code (v1.3.0)  │
│       ↓                    ↓            │
│  ┌─────────┐          ┌─────────┐       │
│  │old_col  │          │old_col  │       │
│  │         │          │new_col  │ ← Add │
│  └─────────┘          └─────────┘       │
│       ↑                    ↑            │
│   Read/Write           Read/Write       │
│   (old only)           (both)           │
└─────────────────────────────────────────┘

Phase 2: Migrate Data
┌─────────────────────────────────────────┐
│  Background migration old_col → new_col │
│  Dual write: old_col AND new_col        │
└─────────────────────────────────────────┘

Phase 3: Contract (Cleanup)
┌─────────────────────────────────────────┐
│  Old Code (v1.3.0)   New Code (v1.4.0)  │
│       ↓                    ↓            │
│  ┌─────────┐          ┌─────────┐       │
│  │old_col  │ ← Remove │old_col  │       │
│  │new_col  │          │new_col  │       │
│  └─────────┘          └─────────┘       │
│       ↑                    ↑            │
│   Read/Write           Read/Write       │
│   (new only)           (new only)       │
└─────────────────────────────────────────┘
```

### Migration Rules

1. **Never** modify existing columns in-place
2. **Always** add new columns/tables first
3. **Maintain** backward compatibility for at least 2 versions
4. **Use** feature flags for new schema usage
5. **Test** rollback before deployment

### Example: Safe Column Rename

```sql
-- Migration V1: Add new column
ALTER TABLE transactions ADD COLUMN transaction_ref VARCHAR(64);

-- Migration V2: Backfill data (run in background job)
UPDATE transactions SET transaction_ref = old_ref WHERE transaction_ref IS NULL;

-- Migration V3: Add NOT NULL constraint (after all data migrated)
ALTER TABLE transactions ALTER COLUMN transaction_ref SET NOT NULL;

-- Migration V4: Drop old column (in next release)
ALTER TABLE transactions DROP COLUMN old_ref;
```

---

## Rollback Procedures

### Rollback Decision Matrix

| Metric | Threshold | Action |
|--------|-----------|--------|
| Error Rate | > 1% | Immediate rollback |
| P95 Latency | > 500ms | Evaluate rollback |
| P99 Latency | > 1000ms | Immediate rollback |
| CPU Usage | > 90% for 5min | Evaluate rollback |
| Memory Usage | > 90% for 5min | Evaluate rollback |
| Custom Health Check | Failed | Immediate rollback |

### Automated Rollback

```bash
#!/bin/bash
# Automated rollback on threshold breach

SERVICE=$1
THRESHOLD_ERROR_RATE=1.0  # 1%
THRESHOLD_LATENCY=500     # ms

# Monitor for 2 minutes
for i in {1..12}; do
    ERROR_RATE=$(curl -s "http://prometheus:9090/api/v1/query?query=rate(http_requests_total{service=\"${SERVICE}\",status=~\"5..\"}[1m])" | jq -r '.data.result[0].value[1] // 0')

    LATENCY=$(curl -s "http://prometheus:9090/api/v1/query?query=histogram_quantile(0.95,rate(http_request_duration_seconds_bucket{service=\"${SERVICE}\"}[1m]))" | jq -r '.data.result[0].value[1] // 0')

    if (( $(echo "$ERROR_RATE > $THRESHOLD_ERROR_RATE" | bc -l) )); then
        echo "Error rate $ERROR_RATE% exceeds threshold. Rolling back..."
        ./scripts/deployment/canary-rollback.sh ${SERVICE}
        exit 1
    fi

    if (( $(echo "$LATENCY > $THRESHOLD_LATENCY" | bc -l) )); then
        echo "Latency ${LATENCY}ms exceeds threshold. Evaluating rollback..."
        # Could auto-rollback or alert
    fi

    sleep 10
done
```

---

## Deployment Scripts

### Quick Reference

```bash
# Blue-Green Deployment
./scripts/deployment/blue-green-deploy.sh <service> <version>

# Canary Deployment
./scripts/deployment/canary-deploy.sh <service> <version> <percentage>
./scripts/deployment/canary-promote.sh <service> <percentage>
./scripts/deployment/canary-rollback.sh <service>

# Verification
./scripts/deployment/verify-deployment.sh <environment>

# Testing
./scripts/deployment/test-zero-downtime.sh
```

### Script Details

See individual scripts in `scripts/deployment/` for detailed usage.

---

## Monitoring & Verification

### Health Check Endpoints

All services must implement:

```
GET /actuator/health       # Overall health
GET /actuator/health/liveness   # Kubernetes liveness
GET /actuator/health/readiness  # Kubernetes readiness
GET /actuator/metrics      # Prometheus metrics
```

### Deployment Dashboard

Monitor during deployment:

| Panel | Metric | Warning | Critical |
|-------|--------|---------|----------|
| Error Rate | rate(http_requests_total{status=~"5.."}[1m]) | > 0.5% | > 1% |
| Latency (p95) | histogram_quantile(0.95, ...) | > 300ms | > 500ms |
| CPU Usage | container_cpu_usage_seconds_total | > 70% | > 90% |
| Memory Usage | container_memory_usage_bytes | > 80% | > 90% |
| Pod Restarts | kube_pod_container_status_restarts_total | > 0 | > 2 |

---

## Testing Procedures

### Pre-Deployment Testing

```bash
# 1. Unit tests
mvn test -f backend/pom.xml

# 2. Integration tests
./scripts/test-single-service.sh <service>

# 3. Contract tests
./scripts/run-contract-tests.sh

# 4. Database migration tests
./scripts/verify-db-migrations.sh
```

### Zero-Downtime Test

```bash
# Run during deployment
./scripts/deployment/test-zero-downtime.sh

# This script:
# 1. Starts load test (k6)
# 2. Triggers deployment
# 3. Monitors for dropped connections
# 4. Verifies zero errors during switch
# 5. Reports deployment time
```

### Post-Deployment Verification

```bash
# Smoke tests
./scripts/deployment/smoke-test.sh

# E2E tests
cd frontend/web-app && npm run test:e2e

# Performance baseline
./tests/performance/k6/run-all-tests.sh --smoke
```

---

## Emergency Procedures

### Scenario 1: Deployment Causes Outage

```bash
# 1. Immediate rollback
./scripts/deployment/emergency-rollback.sh all

# 2. Verify rollback
oc get pods -n payu-dev

# 3. Run health checks
./scripts/deployment/verify-deployment.sh

# 4. Notify stakeholders
./scripts/notify-deployment-status.sh "ROLLBACK_COMPLETE"
```

### Scenario 2: Database Migration Failure

```bash
# 1. Stop all deployments
oc rollout pause deployment -n payu-dev --all

# 2. Assess migration status
./scripts/deployment/verify-db-migrations.sh

# 3. Rollback migrations if needed
./scripts/migration-rollback.sh <migration-version>

# 4. Resume stable version
oc rollout resume deployment -n payu-dev --all
```

### Scenario 3: Split-Brain (Blue-Green)

```bash
# Force all traffic to stable
oc patch route gateway-service -n payu-dev -p \
  '{"spec":{"to":{"name":"gateway-service-blue"}}}'

# Scale down problematic version
oc scale deployment gateway-service-green -n payu-dev --replicas=0

# Investigate issue
oc logs -n payu-dev deployment/gateway-service-green --tail=500
```

---

## Appendix

### A. ArgoCD Sync Waves

```yaml
# Application with sync waves
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: payu-platform
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/fajjarnr/payu.git
    targetRevision: main
    path: infrastructure/openshift/overlays/dev
  destination:
    server: https://kubernetes.default.svc
    namespace: payu-dev
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
  ignoreDifferences:
    - group: apps
      kind: Deployment
      jsonPointers:
        - /spec/replicas
```

### B. Kubernetes Probe Configuration

```yaml
# Optimized probe configuration for zero-downtime
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3

startupProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 30  # 5 minutes max startup
```

### C. Related Documentation

- [DISASTER_RECOVERY.md](./DISASTER_RECOVERY.md)
- [CONTAINER_TROUBLESHOOTING.md](./CONTAINER_TROUBLESHOOTING.md)
- [INFRASTRUCTURE_DEPLOYMENT.md](./INFRASTRUCTURE_DEPLOYMENT.md)
- [PCI-DSS-UU-PDP-AUDIT-REPORT.md](../security/PCI-DSS-UU-PDP-AUDIT-REPORT.md)

---

**Document Owner**: PayU Platform Engineering Team
**Review Cycle**: Monthly
**Next Review**: March 2026
