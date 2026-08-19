# ADR-0044: Secrets Lifecycle & Zero-Trust Secrets Management with Vault & ESO

**Status**: Accepted  
**Date**: 2026-08-19  
**Deciders**: Principal Architect, Cybersecurity Architect, Platform Engineer  
**Relates to**: DEVSECOPS-017, ADR-0040, ADR-0039, INFRA-026  

---

## Context

Secrets are currently via `payu.security.encryption.password` (`ENCRYPTION_KEY`), `partner.jwt.secret`, `payu.callback.signature.secret` injected as env; Vault HA live + `ESO` scaffold exists (`INFRA-026` live, `payu-vault` `ClusterSecretStore` planned, `DEVSECOPS-017` controls). Without `ExternalSecret` + `ClusterSecretStore` + `VaultAuth` (K8s SA `JWT`), risk: secret in git/image/env, no rotation audit.

PayU mTLS Istio handles transport; app `PLAINTEXT` (ADR-0037).

## Decision Drivers

* **Zero-trust** — no secret in code/properties/image/env dump.
* **Rotation** — `90d` + on leak + staff exit, without pod restart manually.
* **Audit** — who read which secret when (Vault audit + K8s event).

## Considered Options

### Option 1 — Vault + ESO `ExternalSecret` + K8s SA JWT (dipilih)

Pros: central `KEK/DEK`, `ESO` sync to `Secret`, short-lived `JWT` auth, rotation via Vault. Cons: Vault HA ops — already live (INFRA-026).

### Option 2 — Sealed Secrets / env

Pros: simple. Cons: long-lived ciphertext, no audit — ditolak.

## Decision

**Vault (Transit/KV) + ESO `ClusterSecretStore(payu-vault)` + `ExternalSecret` per service.**

* Vault HA (`Raft` + `auto-unseal` S3) `payu-kek` `transit`, `payu/*` `kv-v2`.
* ESO `ClusterSecretStore` `vault` with `kubernetes` auth `role=payu-<service>` bound `SA`.
* `ExternalSecret` `payu-encryption-key` → `Secret ENCRYPTION_KEY`, `payu-blind-index-key`, `partner-jwt-secret`, `callback-secret` → `Secret` `refreshInterval 1h`.
* App reads via `envFrom Secret`, never git; `SecurityAutoConfiguration` fail-fast if `ENCRYPTION_KEY` missing in `prod`.
* Rotation: Vault `rotate` → ESO sync → rolling restart via `Reloader` annotation; old `DEK` retained `90d` (ADR-0040).

## Consequences

**Positive**: zero-trust, audited, rotatable.

**Negative**: Vault HA dependency — mitigasi `Raft 3` + `PDB 2`.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | Store | `infrastructure/platform/vault/ClusterSecretStore-payu-vault.yaml` |
| 2 | Secret | `backend/*/k8s/ExternalSecret-enc.yaml` |
| 3 | App | `SecurityAutoConfiguration.validateProductionDefaults` |

**Verification**: `kubectl get externalsecret` `Synced`; `vault audit` log shows read.

---
*Created for DEVSECOPS-017 — implementasi wajib refer ADR ini.*
