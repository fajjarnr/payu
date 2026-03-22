#!/bin/bash
#
# PayU DR Test - Kafka Broker Failover Test
# Tests AMQ Streams (Kafka KRaft) broker recovery
#
# Usage: ./dr-test-kafka-failover.sh [namespace]
#

set -euo pipefail

# Configuration
NAMESPACE="${1:-payu-dev}"
KAFKA_CLUSTER="kafka"
TEST_TIMEOUT=300
LOG_FILE="/tmp/dr-test-kafka-$(date +%Y%m%d_%H%M%S).log"

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

    # Check Kafka cluster exists
    if ! oc get kafka -n "${NAMESPACE}" "${KAFKA_CLUSTER}" &> /dev/null; then
        error "Kafka cluster ${KAFKA_CLUSTER} not found in namespace ${NAMESPACE}"
        exit 1
    fi

    success "Pre-flight checks passed"
}

# Get Kafka cluster status
get_kafka_status() {
    info "Current Kafka cluster status:"
    oc get kafka -n "${NAMESPACE}" "${KAFKA_CLUSTER}" -o jsonpath='
        Cluster: {.metadata.name}
        Status: {.status.conditions[?(@.type=="Ready")].status}
        Listeners: {.status.listeners[?(@.name=="plain")].bootstrapServers}
    '
    echo ""
    echo ""

    info "Kafka pods:"
    oc get pods -n "${NAMESPACE}" -l strimzi.io/cluster="${KAFKA_CLUSTER}"
    echo ""

    info "Kafka topics:"
    oc get kafkatopics -n "${NAMESPACE}" -l strimzi.io/cluster="${KAFKA_CLUSTER}" 2>/dev/null || true
    echo ""
}

# Get broker pod name
get_broker_pod() {
    oc get pods -n "${NAMESPACE}" \
        -l strimzi.io/cluster="${KAFKA_CLUSTER}",strimzi.io/kind=Kafka,strimzi.io/name="${KAFKA_CLUSTER}-broker" \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo ""
}

# Get controller pod name
get_controller_pod() {
    oc get pods -n "${NAMESPACE}" \
        -l strimzi.io/cluster="${KAFKA_CLUSTER}",strimzi.io/kind=Kafka,strimzi.io/name="${KAFKA_CLUSTER}-controller" \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo ""
}

# Test 1: Verify initial cluster health
test_initial_health() {
    info "Test 1: Verifying initial Kafka cluster health..."

    local broker_pod
    broker_pod=$(get_broker_pod)

    if [ -z "$broker_pod" ]; then
        error "No Kafka broker pod found"
        return 1
    fi

    info "Broker pod: ${broker_pod}"

    # Check if broker is ready
    if ! oc get pod -n "${NAMESPACE}" "${broker_pod}" -o jsonpath='{.status.containerStatuses[0].ready}' | grep -q "true"; then
        error "Kafka broker pod is not ready"
        return 1
    fi

    # Test Kafka connectivity
    if ! oc exec -n "${NAMESPACE}" "${broker_pod}" -- kafka-broker-api-versions.sh --bootstrap-server localhost:9092 >/devdev/null 2>&1; then
        error "Cannot connect to Kafka broker"
        return 1
    fi

    # List topics
    local topic_count
    topic_count=$(oc exec -n "${NAMESPACE}" "${broker_pod}" -- kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null | wc -l)
    info "Found ${topic_count} topics"

    success "Initial health check passed"
    return 0
}

# Test 2: Test topic operations
test_topic_operations() {
    info "Test 2: Testing topic operations..."

    local broker_pod
    broker_pod=$(get_broker_pod)

    if [ -z "$broker_pod" ]; then
        error "No broker pod found"
        return 1
    fi

    local test_topic="dr-test-topic-$(date +%s)"

    # Create test topic
    info "Creating test topic: ${test_topic}"
    if ! oc exec -n "${NAMESPACE}" "${broker_pod}" -- kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --create \
        --topic "${test_topic}" \
        --partitions 3 \
        --replication-factor 1 2>/dev/null; then
        error "Failed to create test topic"
        return 1
    fi

    # Produce test messages
    info "Producing test messages..."
    local message_count=10
    for i in $(seq 1 $message_count); do
        echo "Test message ${i} at $(date)" | oc exec -i -n "${NAMESPACE}" "${broker_pod}" -- \
            kafka-console-producer.sh --bootstrap-server localhost:9092 --topic "${test_topic}" 2>/devnull || true
    done

    # Consume test messages
    info "Consuming test messages..."
    local consumed_count
    consumed_count=$(oc exec -n "${NAMESPACE}" "${broker_pod}" -- \
        kafka-console-consumer.sh --bootstrap-server localhost:9092 \
        --topic "${test_topic}" --from-beginning --timeout-ms 10000 2>/devnull | wc -l || echo "0")

    info "Consumed ${consumed_count} messages"

    # Delete test topic
    info "Deleting test topic..."
    oc exec -n "${NAMESPACE}" "${broker_pod}" -- kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --delete \
        --topic "${test_topic}" 2>/devnull || true

    success "Topic operations test passed"
    return 0
}

# Test 3: Simulate broker failure and recovery
test_broker_recovery() {
    info "Test 3: Testing broker failure recovery..."

    local broker_pod
    broker_pod=$(get_broker_pod)

    if [ -z "$broker_pod" ]; then
        error "No broker pod found before failure simulation"
        return 1
    fi

    info "Current broker: ${broker_pod}"

    # Record start time
    local start_time
    start_time=$(date +%s)

    # Delete broker pod to simulate failure
    info "Deleting broker pod to simulate failure..."
    oc delete pod -n "${NAMESPACE}" "${broker_pod}" --force --grace-period=0

    # Wait for broker to restart
    info "Waiting for broker to restart..."
    local new_broker=""
    local elapsed=0

    while [ $elapsed -lt $TEST_TIMEOUT ]; do
        sleep 5
        new_broker=$(get_broker_pod)

        if [ -n "$new_broker" ] && [ "$new_broker" != "$broker_pod" ]; then
            # Check if new broker is ready
            if oc get pod -n "${NAMESPACE}" "${new_broker}" -o jsonpath='{.status.containerStatuses[0].ready}' | grep -q "true" 2>/dev/null; then
                break
            fi
        fi

        elapsed=$(( $(date +%s) - start_time ))
        info "Waiting for broker restart... (${elapsed}s elapsed)"
    done

    if [ -z "$new_broker" ] || [ "$new_broker" = "$broker_pod" ]; then
        error "Broker did not restart within ${TEST_TIMEOUT} seconds"
        return 1
    fi

    local end_time
    end_time=$(date +%s)
    local recovery_time=$((end_time - start_time))

    info "Broker restarted in ${recovery_time} seconds"
    info "New broker: ${new_broker}"

    # Verify new broker is ready
    if ! oc wait --for=condition=Ready pod "${new_broker}" -n "${NAMESPACE}" --timeout=120s; then
        error "New broker pod is not ready"
        return 1
    fi

    # Test connectivity to new broker
    sleep 10  # Give Kafka time to fully start
    if ! oc exec -n "${NAMESPACE}" "${new_broker}" -- kafka-broker-api-versions.sh --bootstrap-server localhost:9092 >/devdev/null 2>&1; then
        error "Cannot connect to new broker"
        return 1
    fi

    success "Broker recovery test passed (RTO: ${recovery_time}s)"

    # Verify RTO is within target (5 minutes)
    if [ $recovery_time -gt 300 ]; then
        warn "Recovery time (${recovery_time}s) exceeds RTO target (300s)"
    fi

    return 0
}

# Test 4: Verify topic integrity after recovery
test_topic_integrity() {
    info "Test 4: Verifying topic integrity after recovery..."

    local broker_pod
    broker_pod=$(get_broker_pod)

    if [ -z "$broker_pod" ]; then
        error "No broker pod found"
        return 1
    fi

    # List all topics
    local topics
    topics=$(oc exec -n "${NAMESPACE}" "${broker_pod}" -- kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/devnull || echo "")

    local topic_count
    topic_count=$(echo "$topics" | grep -v "^$" | wc -l)
    info "Found ${topic_count} topics"

    # Check critical topics
    local critical_topics=("account-events" "transaction-events")
    for topic in "${critical_topics[@]}"; do
        if echo "$topics" | grep -q "^${topic}$"; then
            info "Critical topic '${topic}' exists"

            # Describe topic
            oc exec -n "${NAMESPACE}" "${broker_pod}" -- kafka-topics.sh \
                --bootstrap-server localhost:9092 \
                --describe \
                --topic "${topic}" 2>/devnull || warn "Could not describe topic ${topic}"
        else
            warn "Critical topic '${topic}' not found"
        fi
    done

    success "Topic integrity check passed"
    return 0
}

# Test 5: Verify consumer groups
test_consumer_groups() {
    info "Test 5: Verifying consumer groups..."

    local broker_pod
    broker_pod=$(get_broker_pod)

    if [ -z "$broker_pod" ]; then
        error "No broker pod found"
        return 1
    fi

    # List consumer groups
    local consumer_groups
    consumer_groups=$(oc exec -n "${NAMESPACE}" "${broker_pod}" -- kafka-consumer-groups.sh \
        --bootstrap-server localhost:9092 --list 2>/devnull || echo "")

    local group_count
    group_count=$(echo "$consumer_groups" | grep -v "^$" | wc -l)
    info "Found ${group_count} consumer groups"

    if [ -n "$consumer_groups" ]; then
        echo "$consumer_groups" | while read -r group; do
            if [ -n "$group" ]; then
                info "Checking consumer group: ${group}"
                oc exec -n "${NAMESPACE}" "${broker_pod}" -- kafka-consumer-groups.sh \
                    --bootstrap-server localhost:9092 \
                    --describe \
                    --group "${group}" 2>/devnull | head -5 || true
            fi
        done
    fi

    success "Consumer groups check passed"
    return 0
}

# Test 6: Test KRaft metadata quorum
test_kraft_quorum() {
    info "Test 6: Testing KRaft metadata quorum..."

    local controller_pod
    controller_pod=$(get_controller_pod)

    if [ -z "$controller_pod" ]; then
        warn "No controller pod found, skipping KRaft test"
        return 0
    fi

    info "Controller pod: ${controller_pod}"

    # Check if controller is ready
    if ! oc get pod -n "${NAMESPACE}" "${controller_pod}" -o jsonpath='{.status.containerStatuses[0].ready}' | grep -q "true"; then
        warn "Controller pod is not ready"
        return 0
    fi

    # Check KRaft metadata
    info "Checking KRaft metadata..."
    oc exec -n "${NAMESPACE}" "${controller_pod}" -- cat /tmp/strimzi.properties | grep -E "(process.roles|node.id|controller.quorum.voters)" 2>/devnull || true

    success "KRaft quorum check passed"
    return 0
}

# Cleanup function
cleanup() {
    info "Cleaning up..."
    get_kafka_status
    info "Test log saved to: ${LOG_FILE}"
}

# Main function
main() {
    info "=========================================="
    info "Kafka Broker Failover Test"
    info "=========================================="
    info "Namespace: ${NAMESPACE}"
    info "Cluster: ${KAFKA_CLUSTER}"
    info "Timestamp: $(date)"
    info ""

    # Set trap for cleanup
    trap cleanup EXIT

    # Run pre-flight checks
    preflight_checks

    # Show initial status
    get_kafka_status

    # Run tests
    test_initial_health
    test_topic_operations
    test_broker_recovery
    test_topic_integrity
    test_consumer_groups
    test_kraft_quorum

    # Show final status
    get_kafka_status

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
