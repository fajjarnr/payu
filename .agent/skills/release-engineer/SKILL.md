---
name: release-engineer
version: 2.0.0
maturity: stable
updated: 2026-01-30
author: payu-platform-team
requires: [platform-engineer]
tags: [release, cicd, feature-flags, rollouts]
related: [platform-engineer]
description: **Master Skill**: Release Engineering & Deployment Strategies. Covers Feature Flags, Blue-Green/Canary Deployments, Progressive Rollouts, and Rollback Automation.
---

# PayU Release Engineer Master Skill

You are the **Lead Release Engineer (AI)** for the **PayU Platform**. You ensure that every release is safe, reversible, and invisible to users when problems occur. Zero-downtime deployments are not optional—they are mandatory.

## 🎯 Core Principles

- **Progressive Delivery**: Never release to 100% users at once
- **Instant Rollback**: Any release must be reversible in < 2 minutes
- **Feature Isolation**: Code deployment ≠ Feature activation
- **Data Compatibility**: Database changes must be backward compatible

---

## 🚦 Feature Flag System

### Flag Types

| Type | Lifespan | Use Case | Example |
|:-----|:---------|:---------|:--------|
| **Release Flag** | Days-Weeks | Control feature rollout | `enable-new-transfer-ui` |
| **Experiment Flag** | Weeks-Months | A/B testing | `experiment-checkout-flow-v2` |
| **Ops Flag** | Permanent | Runtime configuration | `enable-bifast-fallback` |
| **Permission Flag** | Permanent | Entitlement control | `feature-premium-insights` |

### Flag Configuration Schema

```yaml
# feature-flags.yaml
flags:
  - key: enable-instant-transfer
    name: Instant Transfer Feature
    description: Enable real-time BI-FAST transfers
    type: release
    default: false
    
    # Targeting rules (evaluated in order)
    rules:
      # Rule 1: Internal testing
      - name: internal-users
        conditions:
          - attribute: user.email
            operator: endsWith
            value: "@payu.id"
        serve: true
        
      # Rule 2: Beta users
      - name: beta-segment
        conditions:
          - attribute: user.segment
            operator: in
            value: ["beta", "early-adopter"]
        serve: true
        
      # Rule 3: Percentage rollout
      - name: gradual-rollout
        conditions:
          - attribute: user.id
            operator: percentageRollout
            value: 25  # 25% of users
        serve: true
    
    # Fallback for all other users
    offVariation: false
    
    # Kill switch
    killSwitch:
      enabled: false
      reason: null
      
    # Metrics
    metrics:
      - name: transfer-success-rate
        type: ratio
        numerator: transfer.completed
        denominator: transfer.initiated
      - name: transfer-latency-p95
        type: percentile
        event: transfer.duration
        percentile: 95
```

### Implementation Pattern

```java
@Service
@RequiredArgsConstructor
public class TransferService {
    
    private final FeatureFlagClient featureFlags;
    private final BiFastClient biFastClient;
    private final LegacyTransferClient legacyClient;
    
    public TransferResult initiateTransfer(TransferRequest request, User user) {
        // Evaluate feature flag with user context
        boolean useInstantTransfer = featureFlags.evaluate(
            "enable-instant-transfer",
            FlagContext.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userSegment(user.getSegment())
                .customAttribute("account_age_days", user.getAccountAgeDays())
                .build(),
            false  // default value
        );
        
        if (useInstantTransfer) {
            // Track feature usage for metrics
            featureFlags.track("instant-transfer-used", user.getId());
            return biFastClient.transfer(request);
        } else {
            return legacyClient.transfer(request);
        }
    }
}
```

### Flag Lifecycle

```
┌──────────────────────────────────────────────────────────────┐
│                   FEATURE FLAG LIFECYCLE                      │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  1. CREATE         2. ROLLOUT         3. MONITOR              │
│  ┌─────────┐      ┌─────────────┐    ┌─────────────┐         │
│  │ Define  │ ──►  │  0% ──► 25% │ ──►│  Dashboard  │         │
│  │ Flag    │      │  ──► 50%   │    │  & Alerts   │         │
│  └─────────┘      │  ──► 100%  │    └─────────────┘         │
│                   └─────────────┘           │                 │
│                         │                   │                 │
│                         ▼                   ▼                 │
│  6. CLEANUP       5. FULL RELEASE    4. EVALUATE              │
│  ┌─────────┐      ┌─────────────┐    ┌─────────────┐         │
│  │ Remove  │ ◄──  │  Remove     │ ◄──│  Success?   │         │
│  │ Code    │      │  Flag Logic │    │  Metrics OK?│         │
│  └─────────┘      └─────────────┘    └─────────────┘         │
│                                             │                 │
│                                             ▼ (if no)         │
│                                      ┌─────────────┐         │
│                                      │  ROLLBACK   │         │
│                                      │  to 0%      │         │
│                                      └─────────────┘         │
└──────────────────────────────────────────────────────────────┘
```

---

## 🔵🟢 Blue-Green Deployment

### Architecture

```
                    ┌─────────────────────────────────────┐
                    │           LOAD BALANCER              │
                    │        (Istio VirtualService)        │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
              weight: 100%                  weight: 0%
                    │                             │
                    ▼                             ▼
           ┌───────────────┐            ┌───────────────┐
           │  🔵 BLUE      │            │  🟢 GREEN     │
           │  (Current)    │            │  (New)        │
           │               │            │               │
           │  v1.2.3       │            │  v1.2.4       │
           │  3 replicas   │            │  3 replicas   │
           └───────────────┘            └───────────────┘
                    │                             │
                    └──────────────┬──────────────┘
                                   │
                    ┌──────────────▼──────────────┐
                    │         DATABASE            │
                    │   (Shared, Compatible)      │
                    └─────────────────────────────┘
```

### Deployment Script

```bash
#!/bin/bash
# scripts/blue-green-deploy.sh

SERVICE=$1
NEW_VERSION=$2
NAMESPACE=${3:-payu-prod}

CURRENT_COLOR=$(oc get service $SERVICE -n $NAMESPACE -o jsonpath='{.spec.selector.color}')
NEW_COLOR=$([[ "$CURRENT_COLOR" == "blue" ]] && echo "green" || echo "blue")

echo "📦 Deploying $SERVICE $NEW_VERSION to $NEW_COLOR..."

# 1. Deploy new version to inactive color
oc set image deployment/${SERVICE}-${NEW_COLOR} \
  $SERVICE=registry.payu.id/$SERVICE:$NEW_VERSION \
  -n $NAMESPACE

# 2. Wait for rollout
oc rollout status deployment/${SERVICE}-${NEW_COLOR} -n $NAMESPACE --timeout=5m

# 3. Run smoke tests against new deployment
echo "🧪 Running smoke tests..."
SMOKE_RESULT=$(curl -s -o /dev/null -w "%{http_code}" \
  http://${SERVICE}-${NEW_COLOR}.${NAMESPACE}.svc:8080/health)

if [[ "$SMOKE_RESULT" != "200" ]]; then
  echo "❌ Smoke test failed! Aborting deployment."
  exit 1
fi

# 4. Switch traffic (atomic operation)
echo "🔄 Switching traffic to $NEW_COLOR..."
oc patch service $SERVICE -n $NAMESPACE \
  -p "{\"spec\":{\"selector\":{\"color\":\"${NEW_COLOR}\"}}}"

# 5. Verify switch
echo "✅ Traffic now routing to $NEW_COLOR"
echo "💡 To rollback: oc patch service $SERVICE -n $NAMESPACE -p '{\"spec\":{\"selector\":{\"color\":\"${CURRENT_COLOR}\"}}}'"
```

### Istio VirtualService Configuration

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: wallet-service
  namespace: payu-prod
spec:
  hosts:
    - wallet-service
  http:
    - match:
        - headers:
            x-canary:
              exact: "true"
      route:
        - destination:
            host: wallet-service-green
            port:
              number: 8080
    - route:
        - destination:
            host: wallet-service-blue
            port:
              number: 8080
          weight: 100
        - destination:
            host: wallet-service-green
            port:
              number: 8080
          weight: 0
```

---

## 🐤 Canary Deployment

### Progressive Rollout Strategy

| Stage | Traffic % | Duration | Success Criteria |
|:------|:----------|:---------|:-----------------|
| 1 | 1% | 10 min | Error rate < 0.1%, P95 < 300ms |
| 2 | 5% | 30 min | Error rate < 0.1%, P95 < 300ms |
| 3 | 25% | 1 hour | Error rate < 0.5%, P95 < 500ms |
| 4 | 50% | 2 hours | Error rate < 0.5%, P95 < 500ms |
| 5 | 100% | - | Full rollout |

### Argo Rollouts Configuration

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: wallet-service
  namespace: payu-prod
spec:
  replicas: 10
  selector:
    matchLabels:
      app: wallet-service
  template:
    metadata:
      labels:
        app: wallet-service
    spec:
      containers:
        - name: wallet-service
          image: registry.payu.id/wallet-service:v1.2.4
          ports:
            - containerPort: 8080
  strategy:
    canary:
      # Traffic management
      canaryService: wallet-service-canary
      stableService: wallet-service-stable
      trafficRouting:
        istio:
          virtualService:
            name: wallet-service
            routes:
              - primary
      
      # Progressive rollout steps
      steps:
        - setWeight: 1
        - pause: { duration: 10m }
        - analysis:
            templates:
              - templateName: success-rate
              - templateName: latency-p95
        - setWeight: 5
        - pause: { duration: 30m }
        - analysis:
            templates:
              - templateName: success-rate
        - setWeight: 25
        - pause: { duration: 1h }
        - setWeight: 50
        - pause: { duration: 2h }
        - setWeight: 100
      
      # Automatic rollback triggers
      analysis:
        successfulRunHistoryLimit: 3
        unsuccessfulRunHistoryLimit: 3
      
      # Anti-affinity for canary pods
      antiAffinity:
        preferredDuringSchedulingIgnoredDuringExecution:
          weight: 100
---
apiVersion: argoproj.io/v1alpha1
kind: AnalysisTemplate
metadata:
  name: success-rate
spec:
  metrics:
    - name: success-rate
      interval: 1m
      successCondition: result[0] >= 0.995
      failureLimit: 3
      provider:
        prometheus:
          address: http://prometheus:9090
          query: |
            sum(rate(http_requests_total{app="wallet-service",status!~"5.."}[5m]))
            /
            sum(rate(http_requests_total{app="wallet-service"}[5m]))
---
apiVersion: argoproj.io/v1alpha1
kind: AnalysisTemplate
metadata:
  name: latency-p95
spec:
  metrics:
    - name: latency-p95
      interval: 1m
      successCondition: result[0] <= 0.3
      failureLimit: 3
      provider:
        prometheus:
          address: http://prometheus:9090
          query: |
            histogram_quantile(0.95,
              sum(rate(http_request_duration_seconds_bucket{app="wallet-service"}[5m])) by (le)
            )
```

---

## ⏪ Rollback Automation

### Automatic Rollback Triggers

```yaml
# ArgoCD Application with auto-rollback
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: wallet-service
spec:
  project: payu-prod
  source:
    repoURL: https://github.com/payu/wallet-service
    targetRevision: HEAD
    path: k8s/overlays/prod
  destination:
    server: https://kubernetes.default.svc
    namespace: payu-prod
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
    # Rollback on sync failure
    retry:
      limit: 3
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```

### Manual Rollback Commands

```bash
# Argo Rollouts - Instant rollback
kubectl argo rollouts undo wallet-service -n payu-prod

# Argo Rollouts - Abort current rollout
kubectl argo rollouts abort wallet-service -n payu-prod

# Standard Kubernetes rollback
oc rollout undo deployment/wallet-service -n payu-prod

# Rollback to specific revision
oc rollout undo deployment/wallet-service -n payu-prod --to-revision=5

# Blue-Green instant switch
oc patch service wallet-service -n payu-prod \
  -p '{"spec":{"selector":{"color":"blue"}}}'
```

---

## 📊 Release Metrics Dashboard

### Key Release Metrics

| Metric | Target | Alert Threshold |
|:-------|:-------|:----------------|
| **Deployment Frequency** | Daily | < 1/week |
| **Lead Time for Changes** | < 1 day | > 1 week |
| **Change Failure Rate** | < 5% | > 15% |
| **Mean Time to Recovery** | < 1 hour | > 4 hours |
| **Rollback Rate** | < 5% | > 10% |

### Prometheus Queries

```promql
# Deployment frequency (deployments per day)
increase(argocd_app_sync_total{dest_namespace="payu-prod"}[24h])

# Change failure rate
sum(increase(argocd_app_sync_total{phase="Failed"}[30d]))
/
sum(increase(argocd_app_sync_total[30d]))

# Mean time to recovery (from rollback initiation to completion)
histogram_quantile(0.5,
  sum(rate(rollout_phase_duration_seconds_bucket{phase="Degraded"}[30d])) by (le)
)
```

---

## 🔍 Release Engineer Checklist

- [ ] **Feature Flags**: Are new features behind flags with kill switch?
- [ ] **DB Compatibility**: Are migrations backward compatible?
- [ ] **Rollout Strategy**: Is progressive rollout configured (Canary/Blue-Green)?
- [ ] **Analysis**: Are success metrics defined for automatic rollback?
- [ ] **Smoke Tests**: Are post-deployment tests configured?
- [ ] **Rollback Plan**: Is instant rollback tested and documented?
- [ ] **Communication**: Are stakeholders notified of release schedule?

---
*Last Updated: January 2026*

```
