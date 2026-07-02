# ChatOps Slack Bot Commands

> DEVSECOPS-013 — PayU Digital Banking Platform

## Overview

Slack/Teams bot (`@payu-bot`) providing operational commands for incident response, deployment, and status checks.

## Commands

### `/payu-hotfix`
Trigger emergency hotfix deployment pipeline.

```
/payu-hotfix <service> <branch> [--reason "description"]
```

- Checks if hotfix branch exists and has passed CI
- Creates Tekton PipelineRun with `hotfix=true` label
- Bypasses staging approval gate
- Posts deployment status updates to `#payu-incidents`
- Requires: **on-call SRE role**

**Example:**
```
/payu-hotfix transaction-service hotfix/fix-idempotency --reason "Duplicate charge bug — P1"
```

### `/payu-rollback`
Roll back a service to previous deployment.

```
/payu-rollback <service> [--to-version <tag>]
```

- Defaults to previous ArgoCD revision
- Can specify explicit image tag for multi-step rollback
- Posts rollback confirmation with diff link
- Requires: **on-call SRE** or **service owner**
- Confirmation required before execution

**Example:**
```
/payu-rollback wallet-service --to-version 1.8.72
```

### `/payu-status`
Show current deployment status for a service or all services.

```
/payu-status [service]
```

- Queries ArgoCD application status
- Shows: version, health, sync status, last deployed
- If no service specified, shows summary card for all services
- Available to: **all engineering**

**Example output:**
```
📊 PayU Service Status
account-service     1.8.76 ✅ Healthy  Synced  2m ago
wallet-service      1.8.76 ✅ Healthy  Synced  2m ago
transaction-service 1.8.75 ✅ Healthy  Synced  5m ago
gateway-service     1.8.74 ✅ Healthy  Synced  8m ago
...
```

### `/payu-incident`
Declare or update an incident.

```
/payu-incident <declare|update|resolve> <incident-id>
```

- **declare**: Creates `#payu-incidents` thread with severity, summary, affected services
- **update**: Posts status update to incident thread
- **resolve**: Marks incident resolved, triggers postmortem reminder
- Requires: **on-call SRE**

### `/payu-rollout`
Check rollout status or pause/resume a canary deployment.

```
/payu-rollout <status|pause|resume> <service>
```

- **status**: Shows rollout progress (% complete, pods ready)
- **pause**: Pauses ArgoCD sync + rollout (canary gate)
- **resume**: Resumes paused rollout
- Requires: **service owner**

## Architecture

```
Slack → Slack Events API → bot-service (Quarkus/FastAPI) → ArgoCD API / Tekton Triggers / kubectl
```

### Implementation Notes

- Bot runs as Kubernetes deployment in `payu-cicd` namespace
- Authenticates via Slack App token (stored in Vault `secret/payu/chatops`)
- All commands log to audit trail (`payu.audit.bot.command` CloudEvent)
- Rate limited: 10 commands/minute per user
- Commands that mutate infrastructure require confirmation dialog

## Audit Trail

Every bot command publishes a CloudEvent:
```json
{
  "type": "payu.audit.bot.command",
  "source": "/payu/chatops",
  "subject": "rollback",
  "data": {
    "user": "fajjarnr",
    "channel": "C0123456",
    "command": "/payu-rollback wallet-service --to-version 1.8.72",
    "timestamp": "2026-07-02T12:00:00Z",
    "outcome": "confirmed",
    "result": "rollback initiated — Tekton pipelinerun chatops-rollback-abc123"
  }
}
```

## Security Controls

- **RBAC**: Commands gated by Slack user group membership (`@sre`, `@service-owners`)
- **Confirmation**: Destructive commands (hotfix, rollback) require explicit confirmation
- **Rate limit**: Prevents command flooding
- **Audit log**: All commands published to Kafka `payu.audit.bot.command`
- **Vault**: Slack token + ArgoCD token stored in Vault, not in bot config

---

> ⚠️ **Status**: Specification only — bot service not yet implemented. Depends on Slack App provisioning + Vault secret setup. Track as DEVSECOPS-013.
