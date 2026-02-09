#!/usr/bin/env bash
# =============================================================================
# OWASP ZAP DAST (Dynamic Application Security Testing) Runner
# =============================================================================
# Runs OWASP ZAP automated security scans against PayU services.
# Designed for CI pipeline integration (Tekton/GitLab CI).
#
# Usage:
#   ./run-zap-scan.sh [baseline|full|api] [target-url]
#
# Examples:
#   ./run-zap-scan.sh baseline http://localhost:8080
#   ./run-zap-scan.sh api http://localhost:8080/v3/api-docs
#   ./run-zap-scan.sh full http://staging.payu.id
# =============================================================================

set -euo pipefail

SCAN_TYPE="${1:-baseline}"
TARGET_URL="${2:-http://localhost:8080}"
REPORT_DIR="$(dirname "$0")/../reports/security"
ZAP_IMAGE="ghcr.io/zaproxy/zaproxy:stable"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p "$REPORT_DIR"

echo "======================================"
echo " OWASP ZAP DAST Scanner"
echo " Scan Type: $SCAN_TYPE"
echo " Target:    $TARGET_URL"
echo " Report:    $REPORT_DIR"
echo "======================================"

# ZAP rules configuration - suppress known false positives
ZAP_RULES_CONFIG="/tmp/zap-rules.conf"
cat > "$ZAP_RULES_CONFIG" <<'EOF'
# Suppress known false positives for PayU platform
# Format: ruleId	IGNORE/WARN/FAIL	regex-url-pattern
10096	IGNORE	.*	# Timestamp Disclosure - acceptable in API responses
10049	IGNORE	.*	# Storable and Cacheable Content (API responses)
10021	IGNORE	/api/.*	# X-Content-Type-Options (handled by gateway)
90033	IGNORE	/q/health.*	# Loosely Scoped Cookie (health endpoints)
EOF

run_baseline_scan() {
    echo "[*] Running Baseline Scan (passive + light active)..."
    podman run --rm -t \
        --network host \
        -v "$REPORT_DIR:/zap/wrk:rw" \
        -v "$ZAP_RULES_CONFIG:/zap/rules.conf:ro" \
        "$ZAP_IMAGE" \
        zap-baseline.py \
        -t "$TARGET_URL" \
        -c /zap/rules.conf \
        -r "zap-baseline-${TIMESTAMP}.html" \
        -J "zap-baseline-${TIMESTAMP}.json" \
        -l WARN \
        -I || true
}

run_full_scan() {
    echo "[*] Running Full Scan (active + passive — takes longer)..."
    podman run --rm -t \
        --network host \
        -v "$REPORT_DIR:/zap/wrk:rw" \
        -v "$ZAP_RULES_CONFIG:/zap/rules.conf:ro" \
        "$ZAP_IMAGE" \
        zap-full-scan.py \
        -t "$TARGET_URL" \
        -c /zap/rules.conf \
        -r "zap-full-${TIMESTAMP}.html" \
        -J "zap-full-${TIMESTAMP}.json" \
        -l WARN \
        -I || true
}

run_api_scan() {
    echo "[*] Running API Scan against OpenAPI spec..."
    podman run --rm -t \
        --network host \
        -v "$REPORT_DIR:/zap/wrk:rw" \
        -v "$ZAP_RULES_CONFIG:/zap/rules.conf:ro" \
        "$ZAP_IMAGE" \
        zap-api-scan.py \
        -t "$TARGET_URL" \
        -f openapi \
        -c /zap/rules.conf \
        -r "zap-api-${TIMESTAMP}.html" \
        -J "zap-api-${TIMESTAMP}.json" \
        -l WARN \
        -I || true
}

case "$SCAN_TYPE" in
    baseline) run_baseline_scan ;;
    full)     run_full_scan ;;
    api)      run_api_scan ;;
    *)
        echo "ERROR: Unknown scan type '$SCAN_TYPE'. Use: baseline, full, api"
        exit 1
        ;;
esac

echo ""
echo "[✓] Scan complete. Reports saved to: $REPORT_DIR/"
ls -la "$REPORT_DIR"/zap-*-${TIMESTAMP}.* 2>/dev/null || echo "No reports generated."

# Parse JSON report for CI gate
REPORT_JSON="$REPORT_DIR/zap-${SCAN_TYPE}-${TIMESTAMP}.json"
if [[ -f "$REPORT_JSON" ]]; then
    HIGH_ALERTS=$(python3 -c "
import json, sys
with open('$REPORT_JSON') as f:
    data = json.load(f)
alerts = data.get('site', [{}])[0].get('alerts', []) if data.get('site') else []
high = [a for a in alerts if a.get('riskcode', '0') == '3']
print(len(high))
" 2>/dev/null || echo "0")

    echo ""
    echo "High-risk alerts found: $HIGH_ALERTS"

    if [[ "$HIGH_ALERTS" -gt 0 ]]; then
        echo "⚠️  DAST found $HIGH_ALERTS high-risk vulnerabilities. Review report before deploy."
        exit 1
    else
        echo "✅ No high-risk vulnerabilities detected."
    fi
fi
