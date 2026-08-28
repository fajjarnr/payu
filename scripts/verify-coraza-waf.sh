#!/usr/bin/env bash
# WAF-CORAZA-001 — verify coraza-waf ConfigMap drift and pod health
set -euo pipefail
KUBECTL="${KUBECTL:-oc}"
if ! command -v "$KUBECTL" >/dev/null 2>&1; then echo "[!] oc not found — local verification only"; echo "Checking git manifest..."; grep -q "waf-proxy.py" infrastructure/platform/security/coraza/configmap.yaml && echo "[✓] configmap.yaml contains waf-proxy.py" || echo "[✗] missing waf-proxy.py"; grep -q "waf-proxy.py" infrastructure/platform/security/coraza/deployment.yaml && echo "[✓] deployment.yaml references waf-proxy.py" || echo "[✗] deployment missing ref"; exit 0; fi
echo "[*] Checking live ConfigMap coraza-waf-config -n coraza-waf"
$KUBECTL get cm coraza-waf-config -n coraza-waf -o jsonpath='{.data}' 2>&1 | head -c 500; echo
echo "[*] Checking deployment command"
$KUBECTL get deploy coraza-waf -n coraza-waf -o jsonpath='{.spec.template.spec.containers[0].command}' 2>&1; echo
echo "[*] Checking pod status"
$KUBECTL get pods -n coraza-waf 2>&1 | head -20
echo "[*] Re-apply if drift: oc apply -f infrastructure/platform/security/coraza/configmap.yaml"
echo "[*] WAF 403 test: curl -H 'X-Attack: <script>alert(1)</script>' https://gateway-dev.apps.fajjjar.my.id/api/v1/test expect 403"
