#!/bin/bash
set -euo pipefail
# sso-verify.sh — SSO-ENV-002: per-env Keycloak isolation verify (keda/chaos style)
# Checks: sso route per env, secret per env, deployment issuer per env, realm import, 0 ERROR/WARN
# Usage: ./scripts/sso-verify.sh
NS_LIST="payu-dev payu-sit payu-uat payu-preprod payu-prod"
echo "[sso-verify] SSO per-env isolation check"
mkdir -p reports/sso
for NS in payu-sso payu-dev payu-sit payu-uat payu-preprod; do
  echo "--- oc get pods -n $NS ---" | tee -a reports/sso/verify.log
  rtk oc get pods -n $NS --no-headers 2>&1 | head -20 | tee -a reports/sso/verify.log
done
echo "--- oc get route -n payu-sso ---" | tee -a reports/sso/verify.log
oc get route -n payu-sso --no-headers 2>&1 | tee -a reports/sso/verify.log
for NS in payu-dev payu-sit payu-uat payu-preprod payu-prod; do
  echo "--- secret payu-keycloak-client-secrets -n $NS ---" | tee -a reports/sso/verify.log
  oc get secret -n $NS payu-keycloak-client-secrets --no-headers 2>&1 | tee -a reports/sso/verify.log || echo "MISSING $NS" | tee -a reports/sso/verify.log
done
for NS in payu-dev payu-sit; do
  echo "--- deployment issuer -n $NS ---" | tee -a reports/sso/verify.log
  oc get deployment -n $NS -o yaml 2>&1 | grep -A1 "OIDC_ISSUER" | head -5 | tee -a reports/sso/verify.log
done
echo "--- oc get keycloakrealmimport -n payu-sso ---" | tee -a reports/sso/verify.log
oc get keycloakrealmimport -n payu-sso --no-headers 2>&1 | tee -a reports/sso/verify.log
echo "--- rtk logs ERROR/WARN per env ---" | tee -a reports/sso/verify.log
for NS in payu-dev payu-sit; do
  echo "NS $NS" | tee -a reports/sso/verify.log
  rtk oc logs --since=60s -n $NS --all-containers 2>&1 | grep -c '"level":"ERROR"' | tee -a reports/sso/verify.log; echo "errors" | tee -a reports/sso/verify.log
  rtk oc logs --since=60s -n $NS --all-containers 2>&1 | grep -c '"level":"WARN"' | tee -a reports/sso/verify.log; echo "warns" | tee -a reports/sso/verify.log
done
echo "[sso-verify] done reports/sso/verify.log"
