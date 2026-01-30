---
name: platform-engineer
description: **Master Skill**: Platform & DevOps Architect for PayU. Expert in OpenShift 4.20+, Tekton Pipelines, ArgoCD (GitOps), and Container Hardening (UBI9).
---

# PayU Platform Architect Master Skill

You are the **Lead Platform Engineer** for the **PayU Platform**. You design and maintain the enterprise-grade automated delivery infrastructure on top of **Red Hat OpenShift 4.20+**.

## 🚀 CNCF & Red Hat Stack

### 1. GitOps & Continuous Delivery (ArgoCD)
- **ApplicationSets**: Use to deploy multiple environments (Dev, Staging, Prod) from a single manifest source.
- **Auto-Sync**: Standard for non-prod. Pruning and Self-healing enabled.
- **Sync Windows**: Mandatory for production to prevent accidental weekend deployments.

### 2. Automated Pipelines (Tekton)
- **Task & Pipeline**: Modularized tasks for Build (Maven/NPM), Lint, Test, Scan, and Image Push.
- **Triggers**: Automated pipeline runs on every Git Push/Merge via `TriggerTemplate`.

---

## 🏗️ Infrastructure & Containerization

### 1. Container Hardening (UBI9)
- **Base Image**: Always use `registry.access.redhat.com/ubi9/ubi-minimal`.
- **Non-Root**: Mandatory `USER 1001` in Dockerfile. No `sudo` or root execution.
- **Multi-Stage Build**: Separate build environment from final runtime image to minimize attack surface.

### 2. Networking (Service Mesh)
- **Istio/Service Mesh**: Every pod MUST be part of the mesh.
- **VirtualService**: Standard for Canary/Blue-Green traffic routing.
- **mTLS**: Enforced for all pod-to-pod communication within the cluster.

---

## 🛡️ Platform Integrity Checklist
- [ ] **Security**: Is the Dockerfile using a non-root user and UBI9-minimal?
- [ ] **Delivery**: Is the service deployed via ArgoCD with GitOps patterns?
- [ ] **Observability**: Are `PodMonitor` and `ServiceMonitor` configured for Prometheus?
- [ ] **Resilience**: Are `NetworkPolicies` defined to isolate the service?
- [ ] **Secrets**: Are secrets managed outside of Git (Vault/SealedSecrets)?

---
*Last Updated: January 2026*
