#!/bin/bash
#
# Deployment Verification Script
# ==============================
# Verifies deployment health across multiple dimensions
#
# Usage: ./verify-deployment.sh [environment]
# Example: ./verify-deployment.sh green
#          ./verify-deployment.sh (uses current route target)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${NAMESPACE:-payu-dev}"
ENVIRONMENT="${1:-}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Verification results
CHECKS_PASSED=0
CHECKS_FAILED=0
WARNINGS=0

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Deployment Verification${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
    ((CHECKS_PASSED++)) || true
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
    ((WARNINGS++)) || true
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
    ((CHECKS_FAILED++)) || true
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

print_section() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo "----------------------------------------"
}

detect_services() {
    # Get list of services from deployments
    SERVICES=$(oc get deployments -n "${NAMESPACE}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null | grep -v 'canary' | sort -u || echo "")

    if [ -z "$SERVICES" ]; then
        print_error "No deployments found in namespace ${NAMESPACE}"
        exit 1
    fi

    print_info "Found $(echo "$SERVICES" | wc -l) services to verify"
}

check_pod_health() {
    local service=$1
    local label="app=${service}"

    # Check if pods exist
    POD_COUNT=$(oc get pods -n "${NAMESPACE}" -l "${label}" --no-headers 2>/dev/null | wc -l)

    if [ "$POD_COUNT" -eq 0 ]; then
        # Try with different label format
        POD_COUNT=$(oc get pods -n "${NAMESPACE}" -l "deployment=${service}" --no-headers 2>/dev/null | wc -l)
        if [ "$POD_COUNT" -eq 0 ]; then
            print_warning "No pods found for ${service}"
            return 0
        fi
        label="deployment=${service}"
    fi

    # Check running pods
    RUNNING_PODS=$(oc get pods -n "${NAMESPACE}" -l "${label}" --field-selector=status.phase=Running --no-headers 2>/dev/null | wc -l)

    # Check ready pods
    READY_PODS=$(oc get pods -n "${NAMESPACE}" -l "${label}" -o jsonpath='{range .items[*]}{range .status.conditions[?(@.type=="Ready")]}{@.status}{"\n"}{end}{end}' 2>/dev/null | grep -c "True" || echo "0")

    if [ "$RUNNING_PODS" -eq "$POD_COUNT" ] && [ "$READY_PODS" -eq "$POD_COUNT" ]; then
        print_success "${service}: ${READY_PODS}/${POD_COUNT} pods ready and running"
        return 0
    else
        print_error "${service}: Only ${READY_PODS}/${POD_COUNT} pods ready (${RUNNING_PODS} running)"

        # Show failing pods
        echo "    Failing pods:"
        oc get pods -n "${NAMESPACE}" -l "${label}" --no-headers 2>/dev/null | grep -v "Running" | head -3 | while read pod_line; do
            echo "      - ${pod_line}"
        done
        return 1
    fi
}

check_deployment_status() {
    local service=$1

    # Check deployment exists
    if ! oc get deployment "${service}" -n "${NAMESPACE}" >/dev/null 2>&1; then
        print_warning "No deployment found for ${service}"
        return 0
    fi

    # Get deployment details
    DESIRED=$(oc get deployment "${service}" -n "${NAMESPACE}" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "0")
    READY=$(oc get deployment "${service}" -n "${NAMESPACE}" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    AVAILABLE=$(oc get deployment "${service}" -n "${NAMESPACE}" -o jsonpath='{.status.availableReplicas}' 2>/dev/null || echo "0")

    UPDATED=$(oc get deployment "${service}" -n "${NAMESPACE}" -o jsonpath='{.status.updatedReplicas}' 2>/dev/null || echo "0")
    UNAVAILABLE=$(oc get deployment "${service}" -n "${NAMESPACE}" -o jsonpath='{.status.unavailableReplicas}' 2>/dev/null || echo "0")

    if [ "$READY" -eq "$DESIRED" ] && [ "$AVAILABLE" -eq "$DESIRED" ]; then
        if [ "$UNAVAILABLE" -eq 0 ]; then
            print_success "${service}: Deployment healthy (${READY}/${DESIRED} ready)"
            return 0
        else
            print_warning "${service}: Available but has ${UNAVAILABLE} unavailable replicas"
            return 0
        fi
    else
        print_error "${service}: Deployment incomplete (${READY}/${DESIRED} ready, ${UNAVAILABLE} unavailable)"
        return 1
    fi
}

check_health_endpoints() {
    local service=$1

    # Get route URL
    ROUTE_URL=$(oc get route "${service}" -n "${NAMESPACE}" -o jsonpath='{.spec.host}' 2>/dev/null || echo "")

    if [ -z "$ROUTE_URL" ]; then
        print_warning "${service}: No route found, skipping health check"
        return 0
    fi

    # Try health endpoint
    HEALTH_URL="https://${ROUTE_URL}/actuator/health"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "${HEALTH_URL}" 2>/dev/null || echo "000")

    if [ "$HTTP_CODE" = "200" ]; then
        print_success "${service}: Health endpoint returning 200 OK"
        return 0
    elif [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
        print_warning "${service}: Health endpoint requires auth (${HTTP_CODE})"
        return 0
    elif [ "$HTTP_CODE" = "000" ]; then
        print_warning "${service}: Could not connect to health endpoint"
        return 0
    else
        print_error "${service}: Health endpoint returned HTTP ${HTTP_CODE}"
        return 1
    fi
}

check_readiness_probes() {
    local service=$1

    # Get pods with failing readiness probes
    FAILING_PODS=$(oc get pods -n "${NAMESPACE}" -l "app=${service}" -o jsonpath='{range .items[*]}{@.metadata.name}{":"}{range @.status.conditions[?(@.type=="Ready")]}{@.status}{"\n"}{end}{end}' 2>/dev/null | grep ":False" | wc -l)

    if [ "$FAILING_PODS" -eq 0 ]; then
        print_success "${service}: All readiness probes passing"
        return 0
    else
        print_error "${service}: ${FAILING_PODS} pods with failing readiness probes"
        return 1
    fi
}

check_resource_usage() {
    local service=$1

    # Check if metrics are available
    if ! oc top pod -n "${NAMESPACE}" -l "app=${service}" >/dev/null 2>&1; then
        print_warning "${service}: Metrics not available for resource check"
        return 0
    fi

    # Get resource usage
    HIGH_CPU=$(oc top pod -n "${NAMESPACE}" -l "app=${service}" --no-headers 2>/dev/null | awk '{if ($2+0 > 90) print $1}' | wc -l)
    HIGH_MEM=$(oc top pod -n "${NAMESPACE}" -l "app=${service}" --no-headers 2>/dev/null | awk '{if ($3+0 > 90) print $1}' | wc -l)

    if [ "$HIGH_CPU" -eq 0 ] && [ "$HIGH_MEM" -eq 0 ]; then
        print_success "${service}: Resource usage within normal limits"
        return 0
    else
        print_warning "${service}: High resource usage detected (${HIGH_CPU} high CPU, ${HIGH_MEM} high memory)"
        return 0
    fi
}

check_events() {
    local service=$1

    # Check for warning events in the last 5 minutes
    RECENT_WARNINGS=$(oc get events -n "${NAMESPACE}" --field-selector type=Warning --sort-by='.lastTimestamp' 2>/dev/null | grep -i "${service}" | tail -5 | wc -l)

    if [ "$RECENT_WARNINGS" -eq 0 ]; then
        print_success "${service}: No recent warning events"
        return 0
    else
        print_warning "${service}: ${RECENT_WARNINGS} recent warning events"
        return 0
    fi
}

check_logs_for_errors() {
    local service=$1
    local error_count=0

    # Sample logs from the last 2 minutes for errors
    error_count=$(oc logs -n "${NAMESPACE}" -l "app=${service}" --since=2m 2>/dev/null | grep -iE "(error|exception|fatal)" | wc -l || echo "0")

    if [ "$error_count" -eq 0 ]; then
        print_success "${service}: No errors in recent logs"
        return 0
    else
        print_warning "${service}: ${error_count} errors/exceptions in recent logs"
        return 0
    fi
}

check_database_connectivity() {
    local service=$1

    # Check if service uses database by looking for DB env vars
    HAS_DB=$(oc get deployment "${service}" -n "${NAMESPACE}" -o jsonpath='{.spec.template.spec.containers[0].env}' 2>/dev/null | grep -i "database\|postgres\|jdbc" | wc -l || echo "0")

    if [ "$HAS_DB" -eq 0 ]; then
        # No database expected
        return 0
    fi

    # Check for database connection errors in logs
    DB_ERRORS=$(oc logs -n "${NAMESPACE}" -l "app=${service}" --since=5m 2>/dev/null | grep -iE "(connection refused|database|jdbc.*error)" | wc -l || echo "0")

    if [ "$DB_ERRORS" -eq 0 ]; then
        print_success "${service}: Database connectivity appears healthy"
        return 0
    else
        print_error "${service}: Database connectivity issues detected (${DB_ERRORS} errors)"
        return 1
    fi
}

generate_summary() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Verification Summary${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo -e "Checks Passed:  ${GREEN}${CHECKS_PASSED}${NC}"
    echo -e "Checks Failed:  ${RED}${CHECKS_FAILED}${NC}"
    echo -e "Warnings:       ${YELLOW}${WARNINGS}${NC}"
    echo ""

    if [ $CHECKS_FAILED -eq 0 ]; then
        echo -e "${GREEN}✓ All critical checks passed!${NC}"
        return 0
    elif [ $CHECKS_FAILED -lt 3 ]; then
        echo -e "${YELLOW}⚠ Some checks failed but deployment may be functional${NC}"
        return 0
    else
        echo -e "${RED}✗ Multiple critical checks failed - deployment may be unstable${NC}"
        return 1
    fi
}

verify_single_service() {
    local service=$1

    print_section "Verifying: ${service}"

    check_deployment_status "${service}"
    check_pod_health "${service}"
    check_readiness_probes "${service}"
    check_health_endpoints "${service}"
    check_database_connectivity "${service}"
    check_resource_usage "${service}"
    check_logs_for_errors "${service}"
    check_events "${service}"
}

main() {
    print_header

    # Validate OpenShift CLI
    if ! command -v oc &> /dev/null; then
        print_error "OpenShift CLI (oc) not found"
        exit 1
    fi

    # If environment specified, verify specific deployment
    if [ -n "$ENVIRONMENT" ]; then
        # Check for blue/green deployments
        if [ "$ENVIRONMENT" = "blue" ] || [ "$ENVIRONMENT" = "green" ]; then
            # Find services with blue/green deployments
            for svc in $(oc get deployments -n "${NAMESPACE}" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null | tr ' ' '\n' | grep "-${ENVIRONMENT}$" | sed "s/-${ENVIRONMENT}$//" | sort -u); do
                verify_single_service "${svc}-${ENVIRONMENT}"
            done
        else
            verify_single_service "${ENVIRONMENT}"
        fi
    else
        # Verify all services
        detect_services

        for service in $SERVICES; do
            # Skip canary deployments
            if [[ "$service" == *"-canary" ]]; then
                continue
            fi
            verify_single_service "$service"
        done
    fi

    # Generate summary
    generate_summary
    exit $?
}

main "$@"
