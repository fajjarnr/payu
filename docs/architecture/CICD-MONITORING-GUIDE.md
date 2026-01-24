# CI/CD Pipelines & Monitoring Implementation Guide

> Complete Infrastructure Enhancement for PayU Digital Banking Platform

## Overview

This guide documents the comprehensive CI/CD pipelines and monitoring infrastructure implemented for the PayU platform on Red Hat OpenShift 4.20+.

## Table of Contents

- [Tekton CI/CD Pipelines](#tekton-cicd-pipelines)
- [ArgoCD GitOps](#argocd-gitops)
- [Grafana Dashboards](#grafana-dashboards)
- [Alerting & Runbooks](#alerting--runbooks)
- [Log Management](#log-management)
- [Cost Optimization](#cost-optimization)

---

## Tekton CI/CD Pipelines

### Pipeline Overview

The CI/CD infrastructure consists of four main pipelines:

1. **Build Pipeline** - Compiles and containers services
2. **Test Pipeline** - Runs comprehensive tests with quality gates
3. **Deploy Pipeline** - Deploys with blue-green strategy
4. **Rollback Pipeline** - Quick recovery from failures

### Build Pipeline

**Location**: `/infrastructure/pipelines/build-pipeline.yaml`

**Features**:
- Multi-language support (Spring Boot, Quarkus, Python)
- Parallel compilation with Maven `-T1C`
- Container image building with Buildah
- Automated security scanning with Trivy
- SBOM generation with Syft

**Usage**:
```bash
# Trigger build for account-service
tkn pipeline start payu-build-pipeline \
  -p service-name=account-service \
  -p git-url=https://github.com/payu-digital/payu-platform.git \
  -p git-revision=main \
  -p service-type=spring-boot \
  -workspace source=workspace-pvc
```

### Test Pipeline

**Location**: `/infrastructure/pipelines/test-pipeline.yaml`

**Features**:
- Parallel test execution (unit, integration, architecture)
- Test coverage validation (80% threshold)
- SonarQube quality gate integration
- Security scanning with Snyk
- Test report generation

**Quality Gates**:
- Minimum code coverage: 80%
- SonarQube quality gate: Must pass
- Critical vulnerabilities: 0 allowed
- High vulnerabilities: Max 3 allowed

**Usage**:
```bash
# Run tests for account-service
tkn pipeline start payu-test-pipeline \
  -p service-name=account-service \
  -p service-type=spring-boot \
  -p coverage-threshold=80 \
  -p run-integration-tests=true \
  -workspace source=workspace-pvc
```

### Deploy Pipeline

**Location**: `/infrastructure/pipelines/deploy-pipeline.yaml`

**Features**:
- Blue-green deployment strategy
- Automated health checks
- HPA integration
- Deployment verification
- Automatic rollback on failure

**Usage**:
```bash
# Deploy to staging
tkn pipeline start payu-deploy-pipeline \
  -p service-name=account-service \
  -p environment=staging \
  -p namespace=payu-staging \
  -p deployment-strategy=bluegreen \
  -workspace manifest-dir=manifests-pvc
```

### Rollback Pipeline

**Location**: `/infrastructure/pipelines/rollback-pipeline.yaml`

**Features**:
- Automatic backup creation
- Deployment history tracking
- Quick rollback to previous version
- Health verification after rollback
- Slack notification

**Usage**:
```bash
# Rollback production service
tkn pipeline start payu-rollback-pipeline \
  -p service-name=account-service \
  -p namespace=payu-prod \
  -p rollback-to= \
  -p notify-slack=true
```

---

## ArgoCD GitOps

### ApplicationSet Configuration

**Location**: `/infrastructure/openshift/argocd/applicationset.yaml`

**Features**:
- Auto-discovery of services from Git
- Multi-environment support (dev, staging, prod)
- PR preview environments
- Sync waves for dependency ordering

**Structure**:
```
payu-services (ApplicationSet)
├── account-service-staging
├── transaction-service-staging
├── account-service-prod
└── transaction-service-prod
```

### Sync Waves

**Location**: `/infrastructure/openshift/argocd/sync-waves.yaml`

**Deployment Order**:
1. **Wave -10**: Infrastructure (Namespaces)
2. **Wave -5**: Dependencies (PostgreSQL, Redis, Kafka)
3. **Wave 0**: Configuration (ConfigMaps, Secrets)
4. **Wave 5**: Core Services (Auth)
5. **Wave 10**: Business Services (Account, Transaction, Wallet)
6. **Wave 15**: Supporting Services (Notification, Billing)
7. **Wave 20**: Edge Services (Gateway)
8. **Wave 25**: Monitoring

### Drift Detection

**Location**: `/infrastructure/openshift/argocd/drift-detection.yaml`

**Features**:
- Automated drift detection every 30 minutes
- Alert on configuration drift
- Automatic remediation suggestions

---

## Grafana Dashboards

### Business Metrics Dashboard

**Location**: `/infrastructure/openshift/monitoring/grafana/dashboards/business-metrics.json`

**Metrics Tracked**:
- Total Payment Volume (TPV)
- Transaction Count (24h)
- Success Rate
- Average Transaction Value
- Transaction Volume by Type
- Conversion Funnel
- Top Products by Revenue

### SLA Dashboard

**Location**: `/infrastructure/openshift/monitoring/grafana/dashboards/sla-dashboard.json`

**Metrics Tracked**:
- Overall Availability (30d)
- Error Budget Remaining
- MTTR (Mean Time To Recover)
- Service Uptime (24h)
- Request Success Rate SLO
- Response Time SLO (p95)
- Incident Count (7d)

### Cost Dashboard

**Location**: `/infrastructure/openshift/monitoring/grafana/dashboards/cost-dashboard.json`

**Metrics Tracked**:
- Total Monthly Cost Estimate
- Cost per Service
- Budget Utilization
- CPU Allocation vs Usage
- Memory Allocation vs Usage
- Resource Utilization by Service
- Idle Resources
- Cost Trend (30d)

### User Journey Dashboard

**Location**: `/infrastructure/openshift/monitoring/grafana/dashboards/user-journey.json`

**Metrics Tracked**:
- Active Users (24h)
- New Registrations (24h)
- Session Duration (avg)
- Bounce Rate
- User Journey Funnel
- Funnel Conversion Rate
- Most Used Features
- User Retention (Cohort)

---

## Alerting & Runbooks

### SLO Alerts

**Location**: `/infrastructure/openshift/monitoring/alerts/slo-alerts.yaml`

**SLO Targets**:
- **Availability**: 99.9% (43.2 minutes/month downtime)
- **Latency**: p95 < 1s
- **Freshness**: Critical data < 5 minutes
- **Correctness**: Transaction mismatch < 0.1%
- **API Success Rate**: 99.9%

**Alerts**:
- `SLOAvailabilityBreached` - Critical
- `ErrorBudgetExhausted` - Critical (< 10% remaining)
- `SLOLatencyBreached` - Warning
- `SLOFreshnessBreached` - Warning
- `SLOCorrectnessBreached` - Critical

### PagerDuty Integration

**Location**: `/infrastructure/openshift/monitoring/alerts/pagerduty-integration.yaml`

**Service Keys**:
- **Critical Incidents**: 24/7 on-call
- **SLO Breaches**: 24/7 on-call
- **Platform Team**: 24/7 on-call
- **Business Team**: Business hours only

### Runbooks

**Location**: `/docs/operations/runbooks/`

**Available Runbooks**:
1. [SLO Availability Breach](/docs/operations/runbooks/slo-availability.md)
2. [Error Budget Exhaustion](/docs/operations/runbooks/error-budget.md)

**Runbook Structure**:
- Alert Information
- Initial Diagnosis
- Troubleshooting Steps
- Resolution Strategies
- Verification
- Prevention
- Escalation Path

---

## Log Management

### Log Correlation

**Location**: `/infrastructure/openshift/logging/log-correlation.yaml`

**Features**:
- Trace ID injection for all logs
- Structured JSON logging
- Log aggregation with Vector
- Enrichment with service metadata

**Log Format**:
```json
{
  "timestamp": "2026-01-24T10:30:45.123Z",
  "level": "ERROR",
  "traceId": "abc123def456",
  "spanId": "span789",
  "userId": "user456",
  "service": "account-service",
  "message": "Transaction failed",
  "exception": "java.lang.NullPointerException"
}
```

### Log Alerts

**Location**: `/infrastructure/openshift/logging/log-alerts.yaml`

**Alerts**:
- `CriticalErrorRateSpike` - Error rate > 5%
- `AuthenticationFailureSpike` - Security alert
- `AuthorizationFailureSpike` - Security alert
- `PaymentFailureRateSpike` - Business critical
- `DatabaseConnectionErrors` - Infrastructure
- `OutOfMemoryErrors` - Infrastructure
- `PIDataLeakage` - Compliance alert

### Log Export to S3

**Location**: `/infrastructure/openshift/logging/log-alerts.yaml`

**Features**:
- Automated export every 6 hours
- Critical logs only
- Compressed with gzip
- Stored in S3 Glacier
- Compliance retention (7 years)

---

## Cost Optimization

### Vertical Pod Autoscaler (VPA)

**Location**: `/infrastructure/openshift/cost-optimization/vpa.yaml`

**Services with VPA**:
- account-service
- transaction-service
- billing-service

**Resource Limits**:
- CPU: 100m - 4000m
- Memory: 256Mi - 8Gi

### Horizontal Pod Autoscaler (HPA)

**Location**: `/infrastructure/openshift/cost-optimization/hpa-enhanced.yaml`

**Scaling Metrics**:
- CPU utilization (70-80%)
- Memory utilization (80%)
- Request rate (custom metrics)

**Replica Ranges**:
- Account Service: 2-10 replicas
- Transaction Service: 3-15 replicas
- Gateway Service: 4-20 replicas

### Cluster Autoscaler

**Location**: `/infrastructure/openshift/cost-optimization/cluster-autoscaler.yaml`

**Configuration**:
- Min Nodes: 3
- Max Nodes: 20
- Scale Down Delay: 10m
- Unneeded Time: 30m

### Cost Allocation

**Location**: `/infrastructure/openshift/cost-optimization/cost-allocation.yaml`

**Cost Centers**:
- Core Banking: CC-1001
- Platform: CC-2001
- Development: CC-3001
- Data Science: CC-4001

**Monthly Reports**:
- Generated on the 1st of each month
- Exported to S3
- Emailed to finance and platform teams

### Budget Alerts

**Location**: `/infrastructure/openshift/cost-optimization/budget-alerts.yaml`

**Budget Thresholds**:
- Monthly Budget: $15,000
- Warning at 80%: $12,000
- Warning at 90%: $13,500
- Critical at 100%: $15,000

**Alerts**:
- `BudgetExceeded` - Warning
- `BudgetWarning80` - Info
- `BudgetWarning90` - Warning
- `BudgetWillExhaustSoon` - Critical

### Idle Resource Detection

**Location**: `/infrastructure/openshift/cost-optimization/idle-resource-detector.yaml`

**Detection Criteria**:
- CPU utilization < 20%
- Memory utilization < 30%
- No traffic in 24h
- Unused PVCs

**Scan Frequency**: Every 6 hours

---

## Quick Start

### 1. Install Tekton Pipelines

```bash
# Install OpenShift Pipelines Operator
oc apply -f infrastructure/pipelines/

# Create workspace PVC
oc apply -f - <<EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: tekton-workspace-pvc
  namespace: payu-cicd
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
EOF
```

### 2. Install ArgoCD

```bash
# Install OpenShift GitOps Operator
oc apply -f infrastructure/openshift/argocd/

# Create App of Apps
oc apply -f infrastructure/openshift/argocd/app-of-apps.yaml
```

### 3. Deploy Monitoring Stack

```bash
# Apply monitoring configurations
oc apply -f infrastructure/openshift/monitoring/alerts/
oc apply -f infrastructure/openshift/monitoring/grafana/dashboards/
```

### 4. Configure Cost Optimization

```bash
# Deploy autoscalers
oc apply -f infrastructure/openshift/cost-optimization/vpa.yaml
oc apply -f infrastructure/openshift/cost-optimization/hpa-enhanced.yaml
oc apply -f infrastructure/openshift/cost-optimization/cluster-autoscaler.yaml

# Deploy cost monitoring
oc apply -f infrastructure/openshift/cost-optimization/budget-alerts.yaml
oc apply -f infrastructure/openshift/cost-optimization/idle-resource-detector.yaml
```

### 5. Setup Log Management

```bash
# Deploy logging stack
oc apply -f infrastructure/openshift/logging/log-correlation.yaml
oc apply -f infrastructure/openshift/logging/log-alerts.yaml
```

---

## Configuration

### Secrets Required

Create the following secrets:

```bash
# PagerDuty
oc create secret generic pagerduty-secrets \
  --from-literal=PAGERDUTY_SERVICE_KEY_CRITICAL='your-key' \
  --from-literal=PAGERDUTY_SERVICE_KEY_SLO='your-key'

# Slack Webhooks
oc create secret generic cost-alert-secrets \
  --from-literal=SLACK_WEBHOOK_URL='https://hooks.slack.com/services/YOUR/WEBHOOK'

# AWS S3 for logs
oc create secret generic s3-credentials \
  --from-literal=AWS_ACCESS_KEY_ID='your-key' \
  --from-literal=AWS_SECRET_ACCESS_KEY='your-secret'

# SonarQube
oc create secret generic sonarqube-credentials \
  --from-literal=SONAR_TOKEN='your-token'
```

---

## Maintenance

### Daily Tasks
- Review Grafana dashboards for anomalies
- Check alert history in Alertmanager
- Monitor cost dashboard for budget utilization

### Weekly Tasks
- Review idle resource detector output
- Optimize resource allocation based on VPA recommendations
- Review incident reports and update runbooks

### Monthly Tasks
- Generate cost reports and distribute to stakeholders
- Review SLO compliance and adjust targets if needed
- Conduct post-incident reviews for major incidents

### Quarterly Tasks
- Review and update budget thresholds
- Conduct capacity planning for next quarter
- Review and optimize CI/CD pipeline performance

---

## Troubleshooting

### Pipeline Failures

**Build Pipeline**:
```bash
# Check pipeline logs
tkn pipeline logs payu-build-pipeline -f

# Check workspace
oc get pvc -n payu-cicd tekton-workspace-pvc
```

**Deploy Pipeline**:
```bash
# Check deployment status
oc rollout status dc/account-service -n payu-prod

# Check deployment logs
oc logs -f -n payu-prod $(oc get pods -n payu-prod -l app=payu -o name | head -1)
```

### Alert Issues

**False Positives**:
```bash
# Check Prometheus query in UI
oc port-forward -n openshift-monitoring svc/prometheus-operated 9090:9090

# Test query
curl 'http://localhost:9090/api/v1/query?query=up{job=~"payu-.*"}'
```

**Missing Alerts**:
```bash
# Check Alertmanager configuration
oc get configmap alertmanager-main -n openshift-monitoring

# Check PrometheusRule
oc get prometheusrule -n openshift-monitoring
```

---

## References

- [Tekton Documentation](https://tekton.dev/docs/)
- [ArgoCD Documentation](https://argoproj.github.io/argo-cd/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [OpenShift Pipelines](https://docs.openshift.com/container-platform/4.20/pipelines/)

---

**Last Updated**: January 2026
**Maintained By**: Platform Team
