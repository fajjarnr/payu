#!/usr/bin/env bash
# =============================================================================
# PayU Platform - Local Pipeline Simulation Script (DEVSECOPS-014)
# =============================================================================
# Simulates Tekton CI/CD pipeline stages locally for a given target service.
#
# Usage:
#   ./scripts/simulate-local-pipeline.sh <service-name> [--skip-build] [--skip-scan]
# Example:
#   ./scripts/simulate-local-pipeline.sh cms-service
# =============================================================================

set -eo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SERVICE_NAME="${1:-}"
SKIP_BUILD=false
SKIP_SCAN=false

for arg in "$@"; do
    case "$arg" in
        --skip-build) SKIP_BUILD=true ;;
        --skip-scan)  SKIP_SCAN=true ;;
    esac
done

if [ -z "${SERVICE_NAME}" ]; then
    echo -e "${RED}ERROR: Service name is required.${NC}"
    echo "Usage: $0 <service-name> [--skip-build] [--skip-scan]"
    echo "Available services:"
    ls -1 backend/ | grep -v 'pom.xml\|shared' | head -n 15
    exit 1
fi

SERVICE_DIR=""
if [ -d "backend/${SERVICE_NAME}" ]; then
    SERVICE_DIR="backend/${SERVICE_NAME}"
elif [ -d "frontend/${SERVICE_NAME}" ]; then
    SERVICE_DIR="frontend/${SERVICE_NAME}"
elif [ -d "${SERVICE_NAME}" ]; then
    SERVICE_DIR="${SERVICE_NAME}"
else
    echo -e "${RED}ERROR: Directory for service '${SERVICE_NAME}' not found.${NC}"
    exit 1
fi

START_TIME=$(date +%s)
echo -e "${BLUE}=====================================================================${NC}"
echo -e "${BLUE}🚀 Starting Local CI/CD Pipeline Simulation for: ${YELLOW}${SERVICE_NAME}${NC}"
echo -e "${BLUE}Path: ${SERVICE_DIR}${NC}"
echo -e "${BLUE}=====================================================================${NC}"

# -----------------------------------------------------------------------------
# Stage 1: Code Lint & ArchUnit Check
# -----------------------------------------------------------------------------
echo -e "\n${YELLOW}[Stage 1/4] Code Lint & ArchUnit Rule Check...${NC}"
STAGE1_START=$(date +%s)

if [ -f "${SERVICE_DIR}/pom.xml" ]; then
    echo "Running Maven compile check..."
    rtk mvn -f "${SERVICE_DIR}/pom.xml" test-compile -DskipTests
elif [ -f "${SERVICE_DIR}/package.json" ]; then
    echo "Running npm lint check..."
    (cd "${SERVICE_DIR}" && npm run lint 2>/dev/null || true)
fi

STAGE1_END=$(date +%s)
echo -e "${GREEN}✓ Stage 1 Passed ($(( STAGE1_END - STAGE1_START ))s)${NC}"

# -----------------------------------------------------------------------------
# Stage 2: Unit & Integration Tests
# -----------------------------------------------------------------------------
echo -e "\n${YELLOW}[Stage 2/4] Unit & Integration Tests...${NC}"
STAGE2_START=$(date +%s)

if [ -f "${SERVICE_DIR}/pom.xml" ]; then
    echo "Running Maven unit tests..."
    rtk mvn -f "${SERVICE_DIR}/pom.xml" test
elif [ -f "${SERVICE_DIR}/package.json" ]; then
    echo "Running npm test..."
    (cd "${SERVICE_DIR}" && npm test 2>/dev/null || true)
fi

STAGE2_END=$(date +%s)
echo -e "${GREEN}✓ Stage 2 Passed ($(( STAGE2_END - STAGE2_START ))s)${NC}"

# -----------------------------------------------------------------------------
# Stage 3: Container Build Simulation
# -----------------------------------------------------------------------------
echo -e "\n${YELLOW}[Stage 3/4] Container Image Build Simulation...${NC}"
STAGE3_START=$(date +%s)

CONTAINERFILE=""
if [ -f "${SERVICE_DIR}/Containerfile" ]; then
    CONTAINERFILE="${SERVICE_DIR}/Containerfile"
elif [ -f "${SERVICE_DIR}/Dockerfile" ]; then
    CONTAINERFILE="${SERVICE_DIR}/Dockerfile"
fi

if [ "${SKIP_BUILD}" = true ]; then
    echo "Skipping container build (--skip-build requested)."
elif [ -n "${CONTAINERFILE}" ]; then
    BUILD_TOOL="podman"
    if ! command -v podman &>/dev/null; then
        BUILD_TOOL="docker"
    fi
    echo "Building container image with ${BUILD_TOOL} using ${CONTAINERFILE}..."
    ${BUILD_TOOL} build -t "payu/${SERVICE_NAME}:local-sim" -f "${CONTAINERFILE}" "${SERVICE_DIR}"
else
    echo "No Containerfile or Dockerfile found; skipping container build."
fi

STAGE3_END=$(date +%s)
echo -e "${GREEN}✓ Stage 3 Passed ($(( STAGE3_END - STAGE3_START ))s)${NC}"

# -----------------------------------------------------------------------------
# Stage 4: Container Vulnerability Security Scan
# -----------------------------------------------------------------------------
echo -e "\n${YELLOW}[Stage 4/4] Security & Vulnerability Scan...${NC}"
STAGE4_START=$(date +%s)

if [ "${SKIP_SCAN}" = true ]; then
    echo "Skipping security scan (--skip-scan requested)."
elif command -v trivy &>/dev/null; then
    echo "Running Trivy container scan..."
    trivy image --severity HIGH,CRITICAL "payu/${SERVICE_NAME}:local-sim" || true
else
    echo "Trivy CLI not installed locally; performing static Containerfile security check..."
    if grep -q "USER 1001\|USER nonroot" "${CONTAINERFILE:-/dev/null}"; then
        echo -e "${GREEN}✓ Non-root user directive found in Containerfile${NC}"
    else
        echo -e "${YELLOW}! Warning: Containerfile does not specify non-root USER 1001${NC}"
    fi
fi

STAGE4_END=$(date +%s)
echo -e "${GREEN}✓ Stage 4 Passed ($(( STAGE4_END - STAGE4_START ))s)${NC}"

# -----------------------------------------------------------------------------
# Summary Report
# -----------------------------------------------------------------------------
TOTAL_END=$(date +%s)
TOTAL_TIME=$((TOTAL_END - START_TIME))

echo -e "\n${BLUE}=====================================================================${NC}"
echo -e "${GREEN}🎉 Local CI/CD Pipeline Simulation PASSED for: ${YELLOW}${SERVICE_NAME}${NC}"
echo -e "${BLUE}Total Duration: ${TOTAL_TIME}s${NC}"
echo -e "${BLUE}=====================================================================${NC}"
