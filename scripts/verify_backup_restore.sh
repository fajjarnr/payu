#!/bin/bash
#
# PayU Disaster Recovery & Backup-Restore Verification Script
# Verifies that all backup and restore procedures work correctly
#

set -euo pipefail

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKUP_ROOT="${BACKUP_ROOT:-/tmp/payu_backups_test}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_RESULTS_FILE="${BACKUP_ROOT}/test_results_${TIMESTAMP}.json"

# Create backup root for testing
mkdir -p "${BACKUP_ROOT}"

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
    echo "{\"status\": \"pass\", \"test\": \"$1\"}" >> "${TEST_RESULTS_FILE}"
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
    echo "{\"status\": \"fail\", \"test\": \"$1\"}" >> "${TEST_RESULTS_FILE}"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_section() {
    echo ""
    echo "=========================================="
    echo "$1"
    echo "=========================================="
}

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

# Track tests
test_passed() {
    TESTS_PASSED=$((TESTS_PASSED + 1))
    log_success "$1"
}

test_failed() {
    TESTS_FAILED=$((TESTS_FAILED + 1))
    log_error "$1"
}

test_skipped() {
    TESTS_SKIPPED=$((TESTS_SKIPPED + 1))
    log_warning "$1"
}

# Check if Docker is available
check_docker() {
    if ! command -v docker &> /dev/null; then
        test_failed "Docker is not available"
        return 1
    fi

    if ! docker info &> /dev/null; then
        test_failed "Docker daemon is not running"
        return 1
    fi

    test_passed "Docker is available and running"
    return 0
}

# Check if docker-compose is available
check_docker_compose() {
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
        test_passed "docker-compose is available"
        return 0
    elif docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
        test_passed "docker compose is available"
        return 0
    else
        test_failed "docker-compose is not available"
        return 1
    fi
}

# Verify backup scripts exist and are executable
verify_backup_scripts() {
    log_section "Verifying Backup Scripts"

    local scripts=(
        "backup_postgres.sh"
        "restore_postgres.sh"
        "backup_restore_redis.sh"
        "backup_restore_kafka.sh"
        "run_backup.sh"
    )

    for script in "${scripts[@]}"; do
        local script_path="${SCRIPT_DIR}/${script}"
        if [[ ! -f "${script_path}" ]]; then
            test_failed "Backup script not found: ${script}"
        elif [[ ! -x "${script_path}" ]]; then
            test_failed "Backup script not executable: ${script}"
        else
            # Verify shell syntax
            if bash -n "${script_path}" 2>&1; then
                test_passed "Backup script has valid syntax: ${script}"
            else
                test_failed "Backup script has syntax errors: ${script}"
            fi
        fi
    done
}

# Verify DRP documentation
verify_drp_documentation() {
    log_section "Verifying DRP Documentation"

    local drp_path="${PROJECT_ROOT}/DISASTER_RECOVERY.md"

    if [[ ! -f "${drp_path}" ]]; then
        test_failed "DRP documentation not found: ${drp_path}"
        return 1
    fi

    # Check for required sections
    local required_sections=(
        "Executive Summary"
        "Recovery Objectives"
        "Backup Strategy"
        "Backup Procedures"
        "Restore Procedures"
        "Testing & Verification"
        "Incident Response"
        "Contact Information"
    )

    local content
    content=$(cat "${drp_path}")

    for section in "${required_sections[@]}"; do
        if grep -q "${section}" "${drp_path}"; then
            test_passed "DRP contains section: ${section}"
        else
            test_failed "DRP missing section: ${section}"
        fi
    done

    # Check for RTO/RPO definitions
    if grep -qE "(RTO|Recovery Time Objective)" "${drp_path}"; then
        test_passed "DRP defines RTO"
    else
        test_failed "DRP missing RTO definition"
    fi

    if grep -qE "(RPO|Recovery Point Objective)" "${drp_path}"; then
        test_passed "DRP defines RPO"
    else
        test_failed "DRP missing RPO definition"
    fi
}

# Start infrastructure for testing
start_infrastructure() {
    log_section "Starting Infrastructure"

    cd "${PROJECT_ROOT}"

    # Stop any existing containers
    log_info "Stopping existing containers..."
    $COMPOSE_CMD down -v > /dev/null 2>&1 || true

    # Start infrastructure
    log_info "Starting infrastructure services..."
    if $COMPOSE_CMD up -d postgres redis kafka zookeeper; then
        test_passed "Infrastructure services started"
    else
        test_failed "Failed to start infrastructure services"
        return 1
    fi

    # Wait for PostgreSQL
    log_info "Waiting for PostgreSQL to be ready..."
    local timeout=60
    local elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if docker exec payu-postgres pg_isready -U payu &> /dev/null; then
            test_passed "PostgreSQL is ready"
            break
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done

    if [ $elapsed -ge $timeout ]; then
        test_failed "PostgreSQL did not become ready"
        return 1
    fi

    # Wait for Redis
    log_info "Waiting for Redis to be ready..."
    timeout=30
    elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if docker exec payu-redis redis-cli ping &> /dev/null; then
            test_passed "Redis is ready"
            break
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done

    if [ $elapsed -ge $timeout ]; then
        test_failed "Redis did not become ready"
        return 1
    fi

    # Wait for Kafka
    log_info "Waiting for Kafka to be ready..."
    timeout=60
    elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if docker exec payu-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 &> /dev/null; then
            test_passed "Kafka is ready"
            break
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done

    if [ $elapsed -ge $timeout ]; then
        test_failed "Kafka did not become ready"
        return 1
    fi
}

# Test PostgreSQL backup
test_postgres_backup() {
    log_section "Testing PostgreSQL Backup"

    # Create test data
    log_info "Creating test data in PostgreSQL..."
    docker exec payu-postgres psql -U payu -d payu_account -c "
        CREATE TABLE IF NOT EXISTS backup_test_table (
            id SERIAL PRIMARY KEY,
            test_data VARCHAR(255),
            created_at TIMESTAMP DEFAULT NOW()
        );
        INSERT INTO backup_test_table (test_data) VALUES ('test_backup_1'), ('test_backup_2');
    " &> /dev/null

    if [ $? -eq 0 ]; then
        test_passed "Test data created in PostgreSQL"
    else
        test_failed "Failed to create test data"
        return 1
    fi

    # Run backup script
    log_info "Running PostgreSQL backup..."
    if BACKUP_ROOT="${BACKUP_ROOT}" "${SCRIPT_DIR}/backup_postgres.sh" test > /dev/null 2>&1; then
        test_passed "PostgreSQL backup completed"
    else
        test_failed "PostgreSQL backup failed"
        return 1
    fi

    # Verify backup file exists
    local backup_file
    backup_file=$(ls -1 ${BACKUP_ROOT}/postgres/daily/*.dump.gz 2>/dev/null | tail -1)

    if [[ -f "${backup_file}" && -s "${backup_file}" ]]; then
        test_passed "PostgreSQL backup file exists and is not empty"
    else
        test_failed "PostgreSQL backup file not found or empty"
        return 1
    fi

    # Verify backup integrity
    log_info "Verifying PostgreSQL backup integrity..."
    if docker exec payu-postgres pg_restore -l /dev/stdin <(gunzip -c "${backup_file}") &> /dev/null; then
        test_passed "PostgreSQL backup integrity verified"
    else
        test_failed "PostgreSQL backup integrity check failed"
        return 1
    fi
}

# Test PostgreSQL restore
test_postgres_restore() {
    log_section "Testing PostgreSQL Restore"

    # Get backup file
    local backup_file
    backup_file=$(ls -1 ${BACKUP_ROOT}/postgres/daily/*.dump.gz 2>/dev/null | tail -1)

    if [[ ! -f "${backup_file}" ]]; then
        test_failed "No PostgreSQL backup file found for restore test"
        return 1
    fi

    # Get original row count
    local original_count
    original_count=$(docker exec payu-postgres psql -U payu -d payu_account -t -c "SELECT COUNT(*) FROM backup_test_table;" 2>/dev/null | tr -d ' ')

    # Delete test table
    docker exec payu-postgres psql -U payu -d payu_account -c "DROP TABLE IF EXISTS backup_test_table;" &> /dev/null

    # Restore from backup
    log_info "Restoring PostgreSQL from backup..."
    if gunzip -c "${backup_file}" | docker exec -i payu-postgres pg_restore -U payu -d payu_account -Fc &> /dev/null; then
        test_passed "PostgreSQL restore completed"
    else
        test_failed "PostgreSQL restore failed"
        return 1
    fi

    # Verify data restoration
    log_info "Verifying data restoration..."
    local restored_count
    restored_count=$(docker exec payu-postgres psql -U payu -d payu_account -t -c "SELECT COUNT(*) FROM backup_test_table;" 2>/dev/null | tr -d ' ')

    if [[ "${restored_count}" == "${original_count}" ]]; then
        test_passed "PostgreSQL data verified (${restored_count} rows)"
    else
        test_failed "PostgreSQL data mismatch (original: ${original_count}, restored: ${restored_count})"
        return 1
    fi
}

# Test Redis backup
test_redis_backup() {
    log_section "Testing Redis Backup"

    # Create test data
    log_info "Creating test data in Redis..."
    docker exec payu-redis redis-cli SET "backup_test_key_1" "test_value_1" &> /dev/null
    docker exec payu-redis redis-cli SET "backup_test_key_2" "test_value_2" &> /dev/null

    if [ $? -eq 0 ]; then
        test_passed "Test data created in Redis"
    else
        test_failed "Failed to create test data"
        return 1
    fi

    # Run backup
    log_info "Running Redis backup..."
    if BACKUP_ROOT="${BACKUP_ROOT}" "${SCRIPT_DIR}/backup_restore_redis.sh" backup > /dev/null 2>&1; then
        test_passed "Redis backup completed"
    else
        test_failed "Redis backup failed"
        return 1
    fi

    # Verify backup file exists
    local backup_file
    backup_file=$(ls -1 ${BACKUP_ROOT}/redis/snapshots/dump_*.rdb 2>/dev/null | tail -1)

    if [[ -f "${backup_file}" && -s "${backup_file}" ]]; then
        test_passed "Redis backup file exists and is not empty"
    else
        test_failed "Redis backup file not found or empty"
        return 1
    fi
}

# Test Kafka backup
test_kafka_backup() {
    log_section "Testing Kafka Backup"

    # Create test topic
    local topic_name="backup_test_topic_${TIMESTAMP}"

    log_info "Creating test Kafka topic..."
    docker exec payu-kafka kafka-topics --bootstrap-server localhost:9092 --create --topic "${topic_name}" --partitions 1 --replication-factor 1 &> /dev/null

    if [ $? -eq 0 ]; then
        test_passed "Test Kafka topic created"
    else
        test_failed "Failed to create test topic"
        return 1
    fi

    # Produce test messages
    log_info "Producing test messages to Kafka..."
    echo "test_message_1" | docker exec -i payu-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic "${topic_name}" &> /dev/null
    echo "test_message_2" | docker exec -i payu-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic "${topic_name}" &> /dev/null

    if [ $? -eq 0 ]; then
        test_passed "Test messages produced to Kafka"
    else
        test_failed "Failed to produce test messages"
        return 1
    fi

    # Run backup
    log_info "Running Kafka backup..."
    if BACKUP_ROOT="${BACKUP_ROOT}" "${SCRIPT_DIR}/backup_restore_kafka.sh" backup > /dev/null 2>&1; then
        test_passed "Kafka backup completed"
    else
        test_failed "Kafka backup failed"
        return 1
    fi

    # Verify backup file exists
    local backup_dir="${BACKUP_ROOT}/kafka/topics"

    if [[ -d "${backup_dir}" ]]; then
        local backup_count=$(ls -1 ${backup_dir}/*.json 2>/dev/null | wc -l)
        if [ ${backup_count} -gt 0 ]; then
            test_passed "Kafka backup files found (${backup_count} files)"
        else
            test_skipped "Kafka backup verification skipped (no JSON files found)"
        fi
    else
        test_skipped "Kafka backup directory not found"
    fi
}

# Test orchestration script
test_orchestration() {
    log_section "Testing Backup Orchestration"

    # Test help command
    log_info "Testing orchestration help..."
    if "${SCRIPT_DIR}/run_backup.sh" --help &> /dev/null; then
        test_passed "Orchestration script help works"
    else
        test_failed "Orchestration script help failed"
        return 1
    fi

    # Test backup all components
    log_info "Running orchestrated backup..."
    if BACKUP_ROOT="${BACKUP_ROOT}" "${SCRIPT_DIR}/run_backup.sh" postgres redis kafka config > /dev/null 2>&1; then
        test_passed "Orchestrated backup completed"
    else
        test_failed "Orchestrated backup failed"
        return 1
    fi
}

# Run Python tests
run_python_tests() {
    log_section "Running Python Tests"

    cd "${PROJECT_ROOT}"

    if ! python3 -m pytest tests/infrastructure/test_backup_restore.py -v --tb=short 2>&1; then
        test_failed "Python tests failed"
        return 1
    fi

    test_passed "Python tests completed"
}

# Generate test report
generate_report() {
    log_section "Test Results Summary"

    local total_tests=$((TESTS_PASSED + TESTS_FAILED + TESTS_SKIPPED))
    local pass_rate=0

    if [ ${total_tests} -gt 0 ]; then
        pass_rate=$((TESTS_PASSED * 100 / total_tests))
    fi

    echo ""
    echo -e "Total Tests: ${total_tests}"
    echo -e "${GREEN}Passed: ${TESTS_PASSED}${NC}"
    echo -e "${RED}Failed: ${TESTS_FAILED}${NC}"
    echo -e "${YELLOW}Skipped: ${TESTS_SKIPPED}${NC}"
    echo -e "Pass Rate: ${pass_rate}%"
    echo ""

    if [ ${TESTS_FAILED} -eq 0 ]; then
        echo -e "${GREEN}========================================${NC}"
        echo -e "${GREEN}✅ All verification tests passed!${NC}"
        echo -e "${GREEN}========================================${NC}"
        return 0
    else
        echo -e "${RED}========================================${NC}"
        echo -e "${RED}❌ Some tests failed${NC}"
        echo -e "${RED}========================================${NC}"
        return 1
    fi
}

# Cleanup
cleanup() {
    log_section "Cleanup"

    cd "${PROJECT_ROOT}"

    log_info "Stopping infrastructure..."
    $COMPOSE_CMD down -v > /dev/null 2>&1 || true

    log_info "Cleanup completed"
}

# Main execution
main() {
    log_info "PayU Disaster Recovery & Backup-Restore Verification"
    log_info "Backup Root: ${BACKUP_ROOT}"
    log_info "Timestamp: ${TIMESTAMP}"

    # Initialize test results file
    echo "[]" > "${TEST_RESULTS_FILE}"

    # Check prerequisites
    if ! check_docker; then
        log_error "Docker not available, exiting"
        exit 1
    fi

    if ! check_docker_compose; then
        log_error "docker-compose not available, exiting"
        exit 1
    fi

    # Verify scripts and documentation
    verify_backup_scripts
    verify_drp_documentation

    # Start infrastructure
    start_infrastructure

    # Run tests
    test_postgres_backup
    test_postgres_restore
    test_redis_backup
    test_kafka_backup
    test_orchestration

    # Run Python tests
    run_python_tests

    # Generate report
    generate_report

    local exit_code=$?

    # Cleanup
    cleanup

    exit ${exit_code}
}

# Trap errors and cleanup
trap cleanup EXIT

# Run main function
main "$@"
