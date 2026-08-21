# ADR-0068: KEDA Autoscaling — Kafka Lag & Prometheus Triggers Standard

**Status**: Proposed  
**Date**: 2026-08-22  
**Deciders**: Platform Engineering, Core Banking Engineering  
**Relates to**: ADR-0042 (ShedLock), ADR-0064 (Gateway 3scale), ADR-0066 (Polyrepo), ADR-0034 (Observability)

---

## Context

`payu-dev` 23 services + `payu-mlops` `vLLM` sudah `HPA require-hpa` (`DEVSECOPS_ARCHITECTURE.md:531`) tapi `HPA` `CPU 80%` saja tidak responsif terhadap `Kafka lag` (`payu.transaction.initiated.v1` burst `BI-FAST` `1000 req/s`). `ExceededNodeResources` 23 concurrent `PipelineRuns` menunjukkan `ResourceQuota` ketat — scale-to-zero untuk `va/biller-sim` + burst scale untuk `transaction/wallet/gateway` dibutuhkan tanpa `Knative` overhead (`Knative` rejected per `ADR-0067` discussion). `KEDA 2.14` `ScaledObject` (Strimzi Kafka 3.7 `AMQ Streams` `ADR-0005`) + `Prometheus` trigger adalah CNCF best practice 2025 untuk event-driven bank (Kong/KEDA + Kafka).

## Decision Drivers

* **Lag-aware**: `transaction-service` latency `P95 <500ms` (ADR-0030) gagal jika consumer lag `>100` belum scale.
* **Cost**: `va-simulator` idle 90% → scale-to-zero `0` save `ResourceQuota`.
* **Simplicity**: `KEDA` 1 operator vs `Knative` `Serving+Eventing+Istio`.

## Considered Options

### Option A — KEDA `ScaledObject` Kafka + Prometheus (chosen)

* **Pros**: `lagThreshold 10` per `payu.*.v1`, `cooldownPeriod 30s`, `pollingInterval 15s`, `minReplicaCount 0` (sim) / `3` (core) `max 10`, `authentication` via `TriggerAuthentication` `Vault` `payu-kafka` secret (ADR-0044).
* **Cons**: 1 operator extra to monitor (Wazuh).

### Option B — HPA + Knative

* **Cons**: cold start `1-2s` breaks `Internal Transfer <1s`.

## Decision

**Option A — KEDA everywhere event-driven, HPA fallback CPU only elsewhere.**

1. **Core** `transaction/wallet/gateway`: `apiVersion: keda.sh/v1alpha1` `kind: ScaledObject` `spec: scaleTargetRef: Deployment/<service>` `triggers: - type: kafka metadata: bootstrapServers: payu-kafka-kafka-bootstrap:9092, consumerGroup: <service>-group, topic: payu.transaction.initiated.v1, lagThreshold: "10", activationLagThreshold: "50"` `+ prometheus trigger` `serverAddress: http://prometheus-operated:9090, metricName: http_requests_per_second, threshold: "1000", query: sum(rate(http_requests_total{app="<service>"}[1m]))` `cooldownPeriod: 30, pollingInterval: 15, minReplicaCount: 3, maxReplicaCount: 10` `fallback: failureThreshold: 3, replicas: 3`.
2. **Sim/ML** `va/biller/llm`: `minReplicaCount: 0` `lagThreshold: "5"` / `cpu trigger 80%` for `vLLM` — scale-to-zero.
3. **Auth**: `TriggerAuthentication` `kind: Secret` `targetRef: payu-kafka-credentials` (VSO synced `payu/prod/kafka`) — no plain env.
4. **Namespace**: `payu` `keda` operator in `keda` ns, `ScaledObject` per `payu-dev|sit|...` overlay `kustomize` `Infrastructure/workloads/<service>/keda/`.
5. **Observability**: `keda_metrics_adapter` → `Prometheus` `keda_scaler_*` `Grafana` (ADR-0034) + `Wazuh` alert `KEDA scaling failure`.

## Consequences

**Positive**: burst `10x` without `ExceededNodeResources`, zero-cost idle sim, no `Knative` Istio.
**Negative**: `KEDA` operator `HA` `2` replicas + `PDB`.

## Implementation Notes

* Helm `keda` `2.14.0` `install: keda --namespace keda --create-namespace --set watchNamespace=""` (cluster-wide).
* `kustomize` `infrastructure/platform/keda/base/keda-scaledobject.yaml` per service `replicas` overlay `dev: min 1 max 3, prod: min 3 max 10`.
* Test: `kcat -L -b payu-kafka:9092 -t payu.transaction.initiated.v1` produce `1000` msgs → `HPA` `3→10` in `<30s`.

---
*Created for KEDA intent — refs KEDA 2.14 Kafka/Prometheus + Strimzi + payu-dev resource contention.*
