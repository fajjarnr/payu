#!/bin/bash
# OWASP ZAP Automated Scan Script for CI/CD
# Usage: ./zap-scan-script.sh <target_url> <report_dir>

set -e

TARGET_URL="${1:-https://staging-api.payu.id}"
REPORT_DIR="${2:-target/zap-reports}"
ZAP_PORT="${ZAP_PORT:-8080}"
ZAP_HOST="${ZAP_HOST:-localhost}"

echo "Starting OWASP ZAP Security Scan against: ${TARGET_URL}"
echo "Report directory: ${REPORT_DIR}"

# Create report directory
mkdir -p "${REPORT_DIR}"

# Start ZAP in daemon mode (if not already running)
echo "Checking if ZAP is running..."
if ! curl -s "http://${ZAP_HOST}:${ZAP_PORT}" > /dev/null; then
    echo "Starting ZAP daemon..."
    docker run -d \
        --name payu-zap-scanner \
        -p "${ZAP_PORT}:8080" \
        -v "$(pwd)/${REPORT_DIR}:/zap/wrk" \
        zaproxy/zap-stable:latest \
        zap.sh -daemon -host 0.0.0.0 -port 8080 -config api.disablekey=true

    # Wait for ZAP to start
    echo "Waiting for ZAP to start..."
    for i in {1..30}; do
        if curl -s "http://${ZAP_HOST}:${ZAP_PORT}" > /dev/null; then
            echo "ZAP is ready!"
            break
        fi
        echo "Waiting... ($i/30)"
        sleep 2
    done
fi

# Run ZAP baseline scan
echo "Running ZAP baseline scan..."
docker exec payu-zap-scanner \
    zap-baseline.py \
    -t "${TARGET_URL}" \
    -g gen.conf \
    -r "${REPORT_DIR}/zap-report.html" \
    -w "${REPORT_DIR}/zap-report.md" \
    -x "${REPORT_DIR}/zap-report.xml" \
    -a \
    -z "-config api.disablekey=true"

# Check for high risk alerts
echo "Checking for high-risk alerts..."
HIGH_RISK=$(docker exec payu-zap-scanner \
    grep -c "Risk (High):" "${REPORT_DIR}/zap-report.html" || true)

if [ "$HIGH_RISK" -gt 0 ]; then
    echo "❌ Found ${HIGH_RISK} high-risk security issues!"
    echo "Please review the report at: ${REPORT_DIR}/zap-report.html"
    # Exit with error code to fail the pipeline
    # Comment out the next line if you want to allow the build to proceed
    exit 1
else
    echo "✅ No high-risk security issues found!"
fi

# Run spider scan (optional, for more comprehensive testing)
echo "Running ZAP spider scan..."
docker exec payu-zap-scanner \
    zap-cli spider "${TARGET_URL}"
docker exec payu-zap-scanner \
    zap-cli active-scan "${TARGET_URL}"

# Generate final report
docker exec payu-zap-scanner \
    zap-cli report -o "${REPORT_DIR}/zap-full-report.html" -f html

echo "Security scan completed!"
echo "Reports available at: ${REPORT_DIR}"

# Cleanup (optional)
# docker stop payu-zap-scanner
# docker rm payu-zap-scanner
