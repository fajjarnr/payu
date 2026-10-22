# ADR-0075: Ambient Mode Evaluation — Stay on Sidecars

**Status**: Accepted  
**Date**: 2026-10-22  
**Deciders**: Platform Engineering, Principal Architect  
**Relates to**: ADR-0072 (OSSM 3.4 Sail Operator dev rollout), ADR-0024 (tiered chaos & fault injection), MESH-006

---

## Context

OSSM 3.4 marks ambient mode (sidecarless: ztunnel per node + optional waypoint proxies) **GA**, and the cluster operator already ships the `ztunnels.sailoperator.io` CRD + `istio-ztunnel-rhel9` image. The mesh currently runs **sidecar mode** (`Istio` CR `profile: default`, 25 native sidecars in `payu-dev`). MESH-006 asked for a measured build-vs-keep evaluation — not a migration, not a deferral without evidence.

## Decision Drivers

- **Cost**: sidecars consume real quota (measured below).
- **Capability**: chaos testing (ADR-0024), mesh JWT, telemetry, and the just-built L7 policy must keep working.
- **Risk**: ambient changes the dataplane (HBONE, host headers, waypoint hops); migration must not be silent.

## Measured Evidence (cluster `payu-drrpz`, 2026-10-22)

| Signal | Value | Source |
|---|---|---|
| Sidecar cost | **25 sidecars = 2500m CPU + 3200Mi requests** (~100m + 128Mi each) vs app 2750m + 7296Mi; quota `payu-dev` at 8750m/24 CPU, 17600Mi/64Gi mem | pod spec requests live |
| Ambient deployed? | **No**: zero `ZTunnel` CRs, zero `istio.io/dataplane-mode` labels (5 pods `none`, rest unset), no ztunnel DaemonSet | `oc get` live |
| Control plane | `Istio` CR `profile: default`, `version: v1.30.5`; `GatewayClass`es `istio` + `istio-remote` only (no waypoint class installed) | `oc get` live |
| Chaos coupling | ADR-0024 UAT tier **requires Istio VirtualService `fault.delay`/`fault.abort`** via `x-chaos-test` header (zero-privilege L7 faults); ambient does **not** support `HTTPFaultInjection`, JWT claim-based routing, Delegate rules, mesh rate limiting, or `ignoreCase` (RH OSSM 3.4 release notes §3.10) | ADR-0024 + RH docs |
| Telemetry coupling | `telemetry/servicemonitor.yaml` scrapes **per-pod Envoy `:15090`** selected by `sidecar.istio.io/inject: "true"`; ambient moves L4 to node ztunnel (no per-pod 15090) — the Kiali graph (31 nodes/70 edges) and all `payu.*` SLO rules would go dark until re-instrumented | repo manifest |
| Policy coupling | `overlays/payu-dev/authorization-policy.yaml` (MESH-003, just applied) + `overlays/payu/` per-service path/method rules assume sidecar-enforced L7; ambient enforces L4 in ztunnel and needs **waypoint proxies per namespace** for L7 — a second proxy tier to design, secure, and operate | repo manifests |

## Considered Options

### Option 1 — Migrate to ambient (rejected)

Move `payu-dev` (or one namespace) to `istio.io/dataplane-mode=ambient` + `ZTunnel` CR + waypoint proxies for L7.

- **Pros**: reclaims ~2500m CPU + 3200Mi requests; no per-pod proxy restarts on config change.
- **Cons** (each independently blocking): (a) breaks ADR-0024 UAT chaos (no `fault.*`); (b) blinds mesh telemetry/SLOs until ztunnel scraping is built; (c) requires waypoint design + policy rewrite for L7; (d) HBONE changes host-header behavior for services distinguishing internal/public traffic; (e) mixing `istio-injection` and `ambient` labels in one namespace is ambiguous (sidecar wins silently).

### Option 2 — Stay on sidecars, re-evaluate on trigger (chosen)

Keep `profile: default` everywhere. No manifest change, no migration, no ADR-0024 impact.

- **Pros**: zero risk to the money path, chaos program, telemetry, and the new L7 policy; sidecar cost is affordable (quota at 36% CPU / 27% mem).
- **Cons**: keeps paying ~100m + 128Mi per meshed workload.

## Decision

**Stay on sidecar mode. Do not deploy `ZTunnel`, do not label any namespace `ambient`, do not install a waypoint class.**

**Normative:**

1. The `Istio` CR stays `profile: default`. Any ambient experiment needs a new ADR and a dedicated namespace — never `payu-dev`.
2. Re-evaluation triggers (any one reopens MESH-006): (a) quota pressure — `payu-dev` requests exceed 70% CPU or mem; (b) Red Hat GAs the missing L7 features ambient lacks today (`HTTPFaultInjection`, JWT claim routing) in the Sail operator we run; (c) a ztunnel-native telemetry path exists that preserves the Kiali graph + `payu.*` SLO rules without a rewrite.
3. If a future migration happens, it follows the MESH-006 rollout shape (one namespace, no mixed labels, HBONE host-header audit, waypoint threat model) — not an in-place flip.

## Consequences

- MESH-006 is **CLOSED** (evaluated with live measurements + recorded decision, not deferred silently).
- No code/manifest/operator change in this ADR. Sidecar cost stays visible via quota checks in the dev runbook.

*Created 2026-10-22 — closes MESH-006.*
