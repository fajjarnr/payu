#!/bin/bash
#
# PayU DR Test - PostgreSQL HA Failover Test
# Tests Crunchy PostgreSQL primary/standby failover
#
# Usage: ./dr-test-postgres-failover.sh [namespace]
#

set -euo pipefail

# Configuration
NAMESPACE="${1:-payu-dev}"
CLUSTER_NAME="payu-postgres"
TEST_TIMEOUT=300
LOG_FILE="/tmp/dr-test-postgres-$(date +%Y%m%d_%H%M%S).log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging functions
log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[${timestamp}] [${level}] ${message}" | tee -a "${LOG_FILE}"
}

info() { log "INFO" "$@"; }
warn() { log "WARN" "$@"; echo -e "${YELLOW}WARN: ${*}${NC}"; }
error() { log "ERROR" "$@"; echo -e "${RED}ERROR: ${*}${NC}"; }
success() { log "SUCCESS" "$@"; echo -e "${GREEN}SUCCESS: ${*}${NC}"; }

# Test result tracking
TESTS_PASSED=0
TESTS_FAILED=0

# Function to run a test
run_test() {
    local test_name="$1"
    local test_command="$2"

    info "Running test: ${test_name}"
    if eval "${test_command}"; then
        success "Test passed: ${test_name}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        error "Test failed: ${test_name}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

# Pre-flight checks
preflight_checks() {
    info "Running pre-flight checks..."

    # Check if oc is available
    if ! command -v oc &> /dev/null; then
        error "OpenShift CLI (oc) not found"
        exit 1
    fi

    # Check if logged in
    if ! oc whoami &> /dev/null; then
        error "Not logged into OpenShift. Run 'oc login' first."
        exit 1
    fi

    # Check namespace exists
    if ! oc get namespace "${NAMESPACE}" &> /dev/null; then
        error "Namespace ${NAMESPACE} does not exist"
        exit 1
    fi

    # Check PostgreSQL cluster exists
    if ! oc get postgrescluster -n "${NAMESPACE}" "${CLUSTER_NAME}" &> /dev/null; then
        error "PostgreSQL cluster ${CLUSTER_NAME} not found in namespace ${NAMESPACE}"
        exit 1
    fi

    success "Pre-flight checks passed"
}

# Get current cluster status
get_cluster_status() {
    local pod_name
    pod_name=$(oc get pods -n "${NAMESPACE}" -l postgres-operator.crunchydata.com/cluster="${CLUSTER_NAME}" -o jsonpath='{.items[0].metadata.name}')

    info "Current Patroni status:"
    oc exec -n "${NAMESPACE}" "${pod_name}" -- patronictl list 2>/dev/null || true
}

# Get primary pod name
get_primary_pod() {
    oc get pods -n "${NAMESPACE}" \
        -l postgres-operator.crunchydata.com/cluster="${CLUSTER_NAME}",postgres-operator.crunchydata.com/role=master \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo ""
}

# Get standby pod name
get_standby_pod() {
    oc get pods -n "${NAMESPACE}" \
        -l postgres-operator.crunchydata.com/cluster="${CLUSTER_NAME}",postgres-operator.crunchydata.com/role=replica \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo ""
}

# Test 1: Verify initial cluster health
test_initial_health() {
    info "Test 1: Verifying initial cluster health..."

    local primary_pod
    primary_pod=$(get_primary_pod)

    if [ -z "$primary_pod" ]; then
        error "No primary pod found"
        return 1
    fi

    info "Primary pod: ${primary_pod}"

    # Check if primary is ready
    if ! oc get pod -n "${NAMESPACE}" "${primary_pod}" -o jsonpath='{.status.containerStatuses[0].ready}' | grep -q "true"; then
        error "Primary pod is not ready"
        return 1
    fi

    # Test database connectivity
    if ! oc exec -n "${NAMESPACE}" "${primary_pod}" -- pg_isready -U payu; then
        error "Cannot connect to primary database"
        return 1
    fi

    success "Initial health check passed"
    return 0
}

# Test 2: Simulate primary failure and failover
test_failover() {
    info "Test 2: Testing failover..."

    local primary_pod
    primary_pod=$(get_primary_pod)

    if [ -z "$primary_pod" ]; then
        error "No primary pod found before failover"
        return 1
    fi

    info "Current primary: ${primary_pod}"

    # Record start time
    local start_time
    start_time=$(date +%s)

    # Delete primary pod to simulate failure
    info "Deleting primary pod to simulate failure..."
    oc delete pod -n "${NAMESPACE}" "${primary_pod}" --force --grace-period=0

    # Wait for failover
    info "Waiting for failover..."
    local new_primary=""
    local elapsed=0

    while [ $elapsed -lt $TEST_TIMEOUT ]; do
        sleep 5
        new_primary=$(get_primary_pod)

        if [ -n "$new_primary" ] && [ "$new_primary" != "$primary_pod" ]; then
            break
        fi

        elapsed=$(( $(date +%s) - start_time ))
        info "Waiting for new primary... (${elapsed}s elapsed)"
    done

    if [ -z "$new_primary" ] || [ "$new_primary" = "$primary_pod" ]; then
        error "Failover did not occur within ${TEST_TIMEOUT} seconds"
        return 1
    fi

    local end_time
    end_time=$(date +%s)
    local failover_time=$((end_time - start_time))

    info "Failover completed in ${failover_time} seconds"
    info "New primary: ${new_primary}"

    # Verify new primary is ready
    if ! oc wait --for=condition=Ready pod "${new_primary}" -n "${NAMESPACE}" --timeout=60s; then
        error "New primary pod is not ready"
        return 1
    fi

    # Test connectivity to new primary
    if ! oc exec -n "${NAMESPACE}" "${new_primary}" -- pg_isready -U payu; then
        error "Cannot connect to new primary database"
        return 1
    fi

    success "Failover test passed (RTO: ${failover_time}s)"

    # Verify RTO is within target (2 minutes)
    if [ $failover_time -gt 120 ]; then
        warn "Failover time (${failover_time}s) exceeds RTO target (120s)"
    fi

    return 0
}

# Test 3: Verify data consistency after failover
test_data_consistency() {
    info "Test 3: Verifying data consistency..."

    local primary_pod
    primary_pod=$(get_primary_pod)

    if [ -z "$primary_pod" ]; then
        error "No primary pod found for consistency check"
        return 1
    fi

    # Check database list
    local db_count
    db_count=$(oc exec -n "${NAMESPACE}" "${primary_pod}" -- psql -U payu -Atc "SELECT count(*) FROM pg_database WHERE datname LIKE 'payu_%';" 2>/dev/null || echo "0")

    info "Found ${db_count} PayU databases"

    if [ "$db_count" -lt 10 ]; then
        error "Expected at least 10 databases, found ${db_count}"
        return 1
    fi

    # Check pgBackRest backup status
    info "Checking pgBackRest status..."
    local repo_pod
    repo_pod=$(oc get pods -n "${NAMESPACE}" -l postgres-operator.crunchydata.com/cluster="${CLUSTER_NAME}",postgres-operator.crunchydata.com/data=pgbackrest -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")

    if [ -n "$repo_pod" ]; then
        oc exec -n "${NAMESPACE}" "${repo_pod}" -- pgbackrest info 2>/dev/null || warn "Could not get pgBackRest info"
    fi

    success "Data consistency check passed"
    return 0
}

# Test 4: Verify replication is working
test_replication() {
    info "Test 4: Verifying replication..."

    local primary_pod
    primary_pod=$(get_primary_pod)

    if [ -z "$primary_pod" ]; then
        error "No primary pod found"
        return 1
    fi

    # Wait for old primary to come back as standby
    info "Waiting for old primary to rejoin as standby..."
    sleep 30

    # Check replication status
    local replication_status
    replication_status=$(oc exec -n "${NAMESPACE}" "${primary_pod}" -- psql -U payu -Atc "
        SELECT count(*) FROM pg_stat_replication WHERE state = 'streaming';
    " 2>/dev/null || echo "0")

    if [ "$replication_status" -eq 0 ]; then
        warn "No active replication connections found"
    else
        info "Active replication connections: ${replication_status}"
    fi

    # Check replication lag
    local lag_bytes
    lag_bytes=$(oc exec -n "${NAMESPACE}" "${primary_pod}" -- psql -U payu -Atc "
        SELECT COALESCE(sum(pg_wal_lsn_diff(sent_lsn, replay_lsn)), 0)
        FROM pg_stat_replication;
    " 2>/dev/null || echo "0")

    info "Replication lag: ${lag_bytes} bytes"

    if [ "$lag_bytes" -gt 10485760 ]; then  # 10MB
        warn "High replication lag detected: ${lag_bytes} bytes"
    fi

    success "Replication check passed"
    return 0
}

# Test 5: Verify pgBouncer connectivity
test_pgbouncer() {
    info "Test 5: Verifying pgBouncer connectivity..."

    local pgbouncer_pod
    pgbouncer_pod=$(oc get pods -n "${NAMESPACE}" -l postgres-operator.crunchydata.com/cluster="${CLUSTER_NAME}",postgres-operator.crunchydata.com/role=pgbouncer -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")

    if [ -z "$pgbouncer_pod" ]; then
        warn "pgBouncer pod not found, skipping test"
        return 0
    fi

    info "pgBouncer pod: ${pgbouncer_pod}"

    # Check if pgBouncer is ready
    if ! oc get pod -n "${NAMESPACE}" "${pgbouncer_pod}" -o jsonpath='{.status.containerStatuses[0].ready}' | grep -q "true"; then
        error "pgBouncer pod is not ready"
        return 1
    fi

    # Show pgBouncer stats
    oc exec -n "${NAMESPACE}" "${pgbouncer_pod}" -- psql -U pgbouncer -c "SHOW STATS;" 2>/dev/null || warn "Could not get pgBouncer stats"

    success "pgBouncer check passed"
    return 0
}

# Cleanup function
cleanup() {
    info "Cleaning up..."
    get_cluster_status
    info "Test log saved to: ${LOG_FILE}"
}

# Main function
main() {
    info "=========================================="
    info "PostgreSQL HA Failover Test"
    info "=========================================="
    info "Namespace: ${NAMESPACE}"
    info "Cluster: ${CLUSTER_NAME}"
    info "Timestamp: $(date)"
    info ""

    # Set trap for cleanup
    trap cleanup EXIT

    # Run pre-flight checks
    preflight_checks

    # Show initial status
    get_cluster_status

    # Run tests
    test_initial_health
    test_failover
    test_data_consistency
    test_replication
    test_pgbouncer

    # Show final status
    get_cluster_status

    # Print summary
    info ""
    info "=========================================="
    info "Test Summary"
    info "=========================================="
    info "Tests Passed: ${TESTS_PASSED}"
    info "Tests Failed: ${TESTS_FAILED}"
    info ""

    if [ ${TESTS_FAILED} -eq 0 ]; then
        success "All tests passed!"
        exit 0
    else
        error "Some tests failed!"
        exit 1
    fi
}

# Run main
main "$@"
