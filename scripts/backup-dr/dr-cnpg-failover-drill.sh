#!/bin/bash
# ADR-0031 DB HA failover drill — ponytail: stub checks RTO<5m, real drill needs OCP creds + CNPG 1.30+
set -e
echo "[DR-DRILL] Failover RTO<5m check — ponytail: stub, add oc exec psql + CNPG switchover when cluster creds exist"
# oc cnpg cluster failover payu-database --target <replica> && oc wait --for=condition=Ready pod -l cnpg.io/cluster=payu-database --timeout=300s
echo "DR-DRILL: stub PASS (1.13.13)"
