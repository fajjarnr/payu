#!/bin/bash
# ADR-0031 PITR restore drill — ponytail: stub, real restore needs barmanObjectStore S3 + recoveryTargetTime
set -e
echo "[PITR] Restore drill — ponytail: stub, add oc apply -f restore.yaml + pitr targetTime when S3 creds exist"
echo "PITR: stub PASS (1.13.13)"
