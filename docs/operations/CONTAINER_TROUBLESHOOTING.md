# Container Troubleshooting Guide

> **Scope**: Podman Compose (Local Dev) | OpenShift/Kubernetes (Production)
> **Last Updated**: February 18, 2026
> **Related**: [LESSONS.md](../guides/LESSONS.md) | [OpenShift Deployment](../operations/INFRASTRUCTURE_DEPLOYMENT.md)

---

## Quick Diagnosis Flowchart

```
Container won't start?
├── Exit Code 137 (OOM Killed)?
│   └── → Memory Limits section
├── CrashLoopBackOff?
│   ├── Exit 1 immediately? → Startup/Config section
│   └── After some time? → Health Check / Resource section
├── ImagePullBackOff?
│   └── → Image/Registry section
└── Pending?
    └── → Resource Quota / Scheduling section
```

---

## 1. Container Startup Failures

### 1.1 Exit Code 137 (OOM Killed)

**Symptoms**:
- Container exits with code 137
- Logs show "Killed" or truncated output
- Happens during startup or model loading (ML services)

**Diagnosis**:
```bash
# Podman Compose
podman inspect <container> | grep -i oom

# OpenShift
oc describe pod <pod-name> -n <namespace> | grep -A5 "Reason:"
oc get events --field-selector reason=OOMKilled
```

**Fixes**:
```yaml
# podman-compose.yml
services:
  kyc-service:
    deploy:
      resources:
        limits:
          memory: 2G  # Was: 512M (too low for PyTorch)
        reservations:
          memory: 1G

# OpenShift (Deployment/DC)
spec:
  containers:
  - name: kyc-service
    resources:
      limits:
        memory: "2Gi"
      requests:
        memory: "1Gi"
```

**PayU Service Memory Requirements**:
| Service Type | Min Memory | Recommended |
|:-------------|:-----------|:------------|
| Standard Java | 512MB | 1GB |
| Quarkus Native | 256MB | 512MB |
| ML (KYC/Analytics) | 1GB | 2GB |
| Keycloak/RHSSO | 1GB | 2GB |
| Kafka Broker | 2GB | 4GB |

---

### 1.2 CrashLoopBackOff (OpenShift)

**Symptoms**:
- Pod status shows `CrashLoopBackOff`
- Container restarts repeatedly

**Diagnosis**:
```bash
# Get exit code and reason
oc describe pod <pod-name> -n payu-dev | grep -A10 "State:"

# Check logs (previous instance if crashing)
oc logs <pod-name> -n payu-dev --previous

# Check events
oc get events -n payu-dev --sort-by='.lastTimestamp' | tail -20
```

**Common Causes & Fixes**:

| Cause | Check | Fix |
|:------|:------|:----|
| Config missing | `env` in `oc describe pod` | Add ConfigMap/Secret |
| Wrong health endpoint | `readinessProbe` path | Match framework (see Health Checks) |
| DB not ready | Connection timeout | Add init-container or retry |
| Port conflict | `EXPOSE` vs `server.port` | Standardize to 8080 |

---

### 1.3 ImagePullBackOff

**Symptoms**:
- Pod status shows `ImagePullBackOff` or `ErrImagePull`

**Diagnosis**:
```bash
# Check image pull errors
oc describe pod <pod-name> | grep -A5 "Failed to pull image"

# Verify image exists
oc get is -n <namespace>  # ImageStream
# Or
skopeo inspect docker://<registry>/<image>:<tag>
```

**Common Fixes**:
```bash
# Missing ImageStream
cat <<EOF | oc apply -f -
apiVersion: image.openshift.io/v1
kind: ImageStream
metadata:
  name: payu-service
  namespace: payu-dev
EOF

# Wrong image tag in Kustomize
# Edit infrastructure/openshift/overlays/dev/kustomization.yaml
# Change: newTag: "1.2.0"  (not latest)

# Private registry auth missing
oc create secret docker-registry regcred \
  --docker-server=<registry> \
  --docker-username=<user> \
  --docker-password=<pass>
oc secrets link default regcred --for=pull
```

---

## 2. Health Check Failures

### 2.1 Framework-Specific Endpoints

**PayU Platform Standards**:

| Framework | Liveness | Readiness | Startup |
|:----------|:---------|:----------|:--------|
| Spring Boot | `/actuator/health/liveness` | `/actuator/health/readiness` | `/actuator/health` |
| Quarkus | `/q/health/live` | `/q/health/ready` | `/q/health` |
| Python FastAPI | `/health` | `/health` | `/health` |

**Important**: Include context path if configured!
```bash
# Spring Boot with context-path
server.servlet.context-path: /compliance-service

# Health URL becomes:
curl http://localhost:8080/compliance-service/actuator/health/liveness
```

### 2.2 401 Unauthorized on Health Endpoints

**Diagnosis**:
```bash
# Test health endpoint manually
curl -v http://<pod-ip>:8080/actuator/health
# Or from inside pod
oc rsh <pod-name>
curl -s localhost:8080/q/health
```

**Fixes**:

**Spring Boot**:
```yaml
# application-container.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
      base-path: /actuator
  endpoint:
    health:
      probes:
        enabled: true

---
# Security config - permit health endpoints
http:
  security:
    matcher:
      permitAll:
        - "/actuator/**"
```

**Quarkus**:
```yaml
# application.properties
quarkus.health.security.enabled=false
# OR
quarkus.http.auth.permission.health.paths=/q/health/*
quarkus.http.auth.permission.health.policy=permit
```

---

## 3. Image Build Issues

### 3.1 Podman Build Failures

**Stale Cache Issues**:
```bash
# Clear build cache
podman build --no-cache -t payu-service:latest .

# Clear all images (nuclear option)
podman rmi -af
```

**Context Contamination**:
```bash
# Problem: target/ from host contaminates build
# Fix: Clean before build
rm -rf backend/*/target
podman build -t payu-service:latest .
```

**Recommended .dockerignore**:
```dockerignore
# Build artifacts
**/target/
**/*.jar
!**/target/app.jar

# IDE
.idea/
*.iml
.vscode/

# Git
.git/
.gitignore

# Node (frontend)
node_modules/
.next/
dist/
```

### 3.2 Multi-Module Maven Issues

**Pattern for PayU Monorepo**:
```dockerfile
# Build stage
FROM registry.access.redhat.com/ubi9/openjdk-21 AS build
WORKDIR /build
COPY . .
# Build only this service + dependencies
RUN mvn package -DskipTests -pl :account-service -am

# Runtime stage
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime
COPY --from=build /build/account-service/target/app.jar /deployments/
```

**Key flags**:
- `-pl :<service-name>`: Project list (build only this)
- `-am`: Also make (build dependencies)
- `-T 1C`: Parallel threads (speed up)

---

## 4. OpenShift-Specific Issues

### 4.1 Permission Denied (Arbitrary User ID)

**Symptoms**:
- Container fails to write to `/deployments`, `/tmp`, etc.
- Permission denied errors in logs

**Root Cause**: OpenShift runs containers with random user IDs (non-root)

**Fix**:
```dockerfile
# Dockerfile - Ensure group writable
RUN chgrp -R 0 /deployments && \
    chmod -R g=u /deployments

# Or for specific directories
RUN mkdir -p /tmp/logs && \
    chgrp -R 0 /tmp/logs && \
    chmod -R g+w /tmp/logs
```

### 4.2 NetworkPolicy Blocking Traffic

**Symptoms**:
- Services can't communicate (connection refused/timeout)
- Gateway returns 502/503 errors

**Check**:
```bash
# List network policies
oc get networkpolicy -n payu-dev

# Check if pods have required labels
oc get pods -n payu-dev --show-labels

# Verify labels match policy selectors
oc describe networkpolicy allow-intra-namespace -n payu-dev
```

**Standard PayU Labels** (must be present):
```yaml
metadata:
  labels:
    app.kubernetes.io/part-of: payu-banking
    app.kubernetes.io/name: account-service
```

**Fix in Kustomize**:
```yaml
# infrastructure/openshift/base/kustomization.yaml
commonLabels:
  app.kubernetes.io/part-of: payu-banking
```

### 4.3 ConfigMap/Secret Not Mounted

**Diagnosis**:
```bash
# Check if ConfigMap exists
oc get configmap -n payu-dev

# Check pod volume mounts
oc describe pod <pod-name> | grep -A20 "Volumes:"

# Check env vars inside container
oc rsh <pod-name> env | grep SPRING
```

**Common Mistakes**:
1. Wrong namespace reference
2. ConfigMap name mismatch
3. Key name mismatch (case-sensitive)

```yaml
# Correct ConfigMap reference
env:
  - name: SPRING_DATASOURCE_URL
    valueFrom:
      configMapKeyRef:
        name: payu-config  # Must match ConfigMap name
        key: database.url   # Must match key in data section
```

---

## 5. Database Connection Issues

### 5.1 Container Can't Connect to Postgres

**Checklist**:
```bash
# 1. Is postgres running?
podman ps | grep postgres
oc get pods -n payu-dev | grep postgres

# 2. Can we resolve the hostname?
podman exec <service> nslookup payu-postgres
oc rsh <pod> nslookup payu-postgres-primary

# 3. Is the port correct?
telnet payu-postgres 5432

# 4. Check credentials
# Password changes in .env don't update existing volumes!
```

**Password Mismatch Fix**:
```bash
# Option 1: Reset volume (DATA LOSS!)
podman volume rm payu_postgres_data

# Option 2: Update password in DB
psql -U postgres -h localhost
c \l  # List databases
ALTER USER payu WITH PASSWORD 'newpassword';
```

### 5.2 Flyway Migration Failures

**Checksum Mismatch**:
```
ERROR: Validate failed: Migration checksum mismatch for version 5
```

**Dev Environment Fix**:
```bash
# Nuclear option - recreate database
dropdb -U postgres -h localhost payu_account
createdb -U postgres -h localhost payu_account
# App will auto-run Flyway on startup
```

**Production Fix**:
```sql
-- Repair checksum in flyway_schema_history
UPDATE flyway_schema_history
SET checksum = <new_checksum>
WHERE version = '5';
```

---

## 6. Quick Reference Commands

### Podman Compose

```bash
# Build all services
podman-compose -f infrastructure/local-podman/podman-compose.yml build

# Start with fresh volumes
podman-compose down -v && podman-compose up -d

# View logs
podman-compose logs -f account-service

# Exec into container
podman exec -it payu-account-service /bin/bash

# Resource stats
podman stats
```

### OpenShift

```bash
# Quick status
oc get pods -n payu-dev
oc get events -n payu-dev --sort-by='.lastTimestamp'

# Pod details
oc describe pod <pod-name> -n payu-dev
oc logs <pod-name> -n payu-dev --tail=100 -f
oc logs <pod-name> -n payu-dev --previous  # Crashed pod

# Debug container
oc rsh <pod-name>
oc debug <pod-name>  # Ephemeral debug container

# Port forward for local testing
oc port-forward pod/<pod-name> 8080:8080 -n payu-dev

# Check resource usage
oc adm top pods -n payu-dev
oc adm top nodes
```

---

## 7. Known PayU Platform Issues

| Issue | Symptoms | Quick Fix |
|:------|:---------|:----------|
| **Keycloak DNS** | ExternalName service fails | Use FQDN: `payu-postgres-primary.payu-dev.svc.cluster.local` |
| **DataGrid RESP** | Gateway connection closed | Use `endpointEncryption.type: None` (dev) |
| **Gateway Redis Auth** | 401 on cache ops | Add auth to URL: `redis://developer:payu-cache-dev@...` |
| **Spring Context Path** | 404 on all endpoints | Check `server.servlet.context-path` matches route |
| **Quarkus Profile** | Config not loading | Use `QUARKUS_PROFILE=prod` (not `dev`) |

---

## Related Documentation

- [LESSONS.md](../guides/LESSONS.md) - Detailed patterns and historical fixes
- [INFRASTRUCTURE_DEPLOYMENT.md](./INFRASTRUCTURE_DEPLOYMENT.md) - OpenShift deployment guide
- [DISASTER_RECOVERY.md](./DISASTER_RECOVERY.md) - Platform recovery procedures

---

_Last Updated: February 18, 2026 | PayU Platform Engineering_
