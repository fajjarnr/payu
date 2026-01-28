# Deployment Strategies

## Overview

Comprehensive guide for deployment strategies within the PayU Digital Banking ecosystem.

## Patterns and Practices

### Pattern 1: Blue-Green Deployment

**Description:**
Running two identical production environments to minimize downtime and risk.

**When to Use:**
- Major version upgrades.
- Changes involving database schema migrations (with backward compatibility).

**Implementation:**
- Traffic is switched at the Load Balancer/Route level in OpenShift.

**Benefits:**
- Instant rollback.
- Zero downtime.

### Pattern 2: Canary Deployment

**Description:**
Gradual rollout of a new version to a subset of users.

**Implementation:**
Utilize Argo Rollouts or Istio VirtualServices for traffic splitting.

## Anti-Patterns to Avoid

### Anti-Pattern 1: Manual Deployments
Deploying directly using `oc apply` or `kubectl apply` from local machines. Always use the CI/CD pipeline and GitOps.

---
*Last Updated: January 2026*
