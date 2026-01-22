#!/bin/bash
#
# PayU PostgreSQL Restore Script
# Supports restoring from pg_dump backups and base backups
#

set -euo pipefail

# Configuration
CONTAINER_NAME="payu-postgres"
POSTGRES_USER="payu"
BACKUP_DIR="/backups/postgres"
LOG_FILE="/var/log/payu/restore_postgres_$(date +%Y%m%d_%H%M%S).log"

# Create log directory if it doesn't exist
mkdir -p "$(dirname ${LOG_FILE})"

# Logging function
log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[${timestamp}] [${level}] ${message}" | tee -a "${LOG_FILE}"
}

# Check if container is running
check_container() {
    log "INFO" "Checking if PostgreSQL container is running..."
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        log "ERROR" "Container ${CONTAINER_NAME} is not running"
        return 1
    fi
    log "INFO" "Container ${CONTAINER_NAME} is running"
    return 0
}

# Test database connectivity
test_connection() {
    log "INFO" "Testing database connectivity..."
    if ! docker exec "${CONTAINER_NAME}" pg_isready -U "${POSTGRES_USER}" > /dev/null 2>&1; then
        log "ERROR" "Cannot connect to PostgreSQL database"
        return 1
    fi
    log "INFO" "Database connectivity verified"
    return 0
}

# List available backups
list_backups() {
    local backup_type="${1:-all}"

    log "INFO" "Available backups:"

    case "${backup_type}" in
        daily|all)
            echo ""
            echo "Daily Backups:"
            ls -lh ${BACKUP_DIR}/daily/*.sql.gz 2>/dev/null | awk '{print "  " $9 " (" $5 ")"}'
            ls -lh ${BACKUP_DIR}/daily/*.dump.gz 2>/dev/null | awk '{print "  " $9 " (" $5 ")"}'
            ;;
        weekly|all)
            echo ""
            echo "Weekly Backups:"
            ls -lh ${BACKUP_DIR}/weekly/*.tar.gz 2>/dev/null | awk '{print "  " $9 " (" $5 ")"}'
            ;;
    esac
}

# Restore all databases from pg_dumpall
restore_all_databases() {
    local backup_file="$1"
    local force="${2:-false}"

    log "INFO" "Restoring all databases from: ${backup_file}"

    if [[ ! -f "${backup_file}" ]]; then
        log "ERROR" "Backup file not found: ${backup_file}"
        return 1
    fi

    if [[ "${force}" != "true" ]]; then
        echo ""
        read -p "WARNING: This will DROP ALL DATABASES and restore from backup. Continue? (yes/no): " confirm
        if [[ "${confirm}" != "yes" ]]; then
            log "INFO" "Restore cancelled by user"
            return 1
        fi
    fi

    # Stop all application services to prevent data corruption
    log "INFO" "Stopping application services..."
    docker-compose stop account-service auth-service transaction-service wallet-service billing-service notification-service kyc-service analytics-service 2>/dev/null || true

    # Restore from backup
    if gunzip -c "${backup_file}" | docker exec -i "${CONTAINER_NAME}" psql -U "${POSTGRES_USER}" -d postgres; then
        log "INFO" "Successfully restored all databases from: ${backup_file}"
        return 0
    else
        log "ERROR" "Failed to restore databases from: ${backup_file}"
        return 1
    fi
}

# Restore a single database from pg_dump
restore_database() {
    local db_name="$1"
    local backup_file="$2"
    local force="${3:-false}"

    log "INFO" "Restoring database '${db_name}' from: ${backup_file}"

    if [[ ! -f "${backup_file}" ]]; then
        log "ERROR" "Backup file not found: ${backup_file}"
        return 1
    fi

    if [[ "${force}" != "true" ]]; then
        echo ""
        read -p "WARNING: This will DROP database '${db_name}' and restore from backup. Continue? (yes/no): " confirm
        if [[ "${confirm}" != "yes" ]]; then
            log "INFO" "Restore cancelled by user"
            return 1
        fi
    fi

    # Stop services that depend on this database
    case "${db_name}" in
        payu_account)
            docker-compose stop account-service transaction-service 2>/dev/null || true
            ;;
        payu_auth)
            docker-compose stop auth-service 2>/dev/null || true
            ;;
        payu_transaction)
            docker-compose stop transaction-service 2>/dev/null || true
            ;;
        payu_wallet)
            docker-compose stop wallet-service transaction-service billing-service 2>/dev/null || true
            ;;
        payu_notification)
            docker-compose stop notification-service 2>/dev/null || true
            ;;
        payu_billing)
            docker-compose stop billing-service 2>/dev/null || true
            ;;
        payu_kyc)
            docker-compose stop kyc-service 2>/dev/null || true
            ;;
        payu_analytics)
            docker-compose stop analytics-service 2>/dev/null || true
            ;;
    esac

    # Drop existing database
    log "INFO" "Dropping existing database '${db_name}'..."
    docker exec "${CONTAINER_NAME}" psql -U "${POSTGRES_USER}" -d postgres -c "DROP DATABASE IF EXISTS ${db_name};" || true

    # Recreate database
    log "INFO" "Creating database '${db_name}'..."
    docker exec "${CONTAINER_NAME}" psql -U "${POSTGRES_USER}" -d postgres -c "CREATE DATABASE ${db_name};" || true

    # Restore from backup
    local temp_file=$(mktemp)
    gunzip -c "${backup_file}" > "${temp_file}"

    if docker exec -i "${CONTAINER_NAME}" pg_restore -U "${POSTGRES_USER}" -d "${db_name}" -Fc "${temp_file}"; then
        log "INFO" "Successfully restored database '${db_name}' from: ${backup_file}"
        rm -f "${temp_file}"
        return 0
    else
        log "ERROR" "Failed to restore database '${db_name}' from: ${backup_file}"
        rm -f "${temp_file}"
        return 1
    fi
}

# Restore specific tables from a backup
restore_tables() {
    local db_name="$1"
    local backup_file="$2"
    shift 2
    local tables=("$@")
    local force="${3:-false}"

    log "INFO" "Restoring tables ${tables[@]} from '${backup_file}' to '${db_name}'"

    if [[ ! -f "${backup_file}" ]]; then
        log "ERROR" "Backup file not found: ${backup_file}"
        return 1
    fi

    if [[ "${force}" != "true" ]]; then
        echo ""
        read -p "WARNING: This will DROP and REPLACE the specified tables. Continue? (yes/no): " confirm
        if [[ "${confirm}" != "yes" ]]; then
            log "INFO" "Restore cancelled by user"
            return 1
        fi
    fi

    # Extract and restore each table
    for table in "${tables[@]}"; do
        log "INFO" "Restoring table '${table}'..."
        gunzip -c "${backup_file}" | docker exec -i "${CONTAINER_NAME}" pg_restore -U "${POSTGRES_USER}" -d "${db_name}" -Fc -t "${table}"
    done

    log "INFO" "Successfully restored tables"
    return 0
}

# Verify restore integrity
verify_restore() {
    local db_name="$1"
    local expected_tables="${2:-}"

    log "INFO" "Verifying restore for database '${db_name}'..."

    # Check if database exists
    if ! docker exec "${CONTAINER_NAME}" psql -U "${POSTGRES_USER}" -d postgres -c "SELECT 1 FROM pg_database WHERE datname='${db_name}'" -t | grep -q "1"; then
        log "ERROR" "Database '${db_name}' does not exist after restore"
        return 1
    fi

    # Check table count
    local table_count=$(docker exec "${CONTAINER_NAME}" psql -U "${POSTGRES_USER}" -d "${db_name}" -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public'" -t | tr -d ' ')

    if [[ -z "${table_count}" || "${table_count}" == "0" ]]; then
        log "ERROR" "No tables found in database '${db_name}'"
        return 1
    fi

    log "INFO" "Database '${db_name}' restored successfully with ${table_count} tables"

    # If expected tables specified, check for them
    if [[ -n "${expected_tables}" ]]; then
        for table in ${expected_tables}; do
            if ! docker exec "${CONTAINER_NAME}" psql -U "${POSTGRES_USER}" -d "${db_name}" -c "SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='${table}'" -t | grep -q "1"; then
                log "ERROR" "Expected table '${table}' not found in database '${db_name}'"
                return 1
            fi
        done
        log "INFO" "All expected tables found"
    fi

    return 0
}

# Find the latest backup for a database
find_latest_backup() {
    local db_name="$1"
    local backup_type="${2:-daily}"

    case "${backup_type}" in
        daily)
            local backup_file=$(ls -t ${BACKUP_DIR}/daily/${db_name}_*.dump.gz 2>/dev/null | head -1)
            ;;
        all)
            local backup_file=$(ls -t ${BACKUP_DIR}/daily/all_databases_*.sql.gz 2>/dev/null | head -1)
            ;;
        *)
            log "ERROR" "Unknown backup type: ${backup_type}"
            return 1
            ;;
    esac

    if [[ -z "${backup_file}" ]]; then
        log "ERROR" "No backup found for database '${db_name}'"
        return 1
    fi

    echo "${backup_file}"
    return 0
}

# Main restore routine
main() {
    local action="$1"
    shift

    log "INFO" "=========================================="
    log "INFO" "PayU PostgreSQL Restore"
    log "INFO" "Action: ${action}"
    log "INFO" "=========================================="

    if ! check_container; then
        exit 1
    fi

    if ! test_connection; then
        exit 1
    fi

    case "${action}" in
        list)
            list_backups "$@"
            ;;
        restore-all)
            local backup_file="${1:-}"
            local force="${2:-false}"
            if [[ -z "${backup_file}" ]]; then
                backup_file=$(find_latest_backup "all")
            fi
            if restore_all_databases "${backup_file}" "${force}"; then
                log "INFO" "✓ All databases restored successfully"
                exit 0
            else
                log "ERROR" "✗ Restore failed"
                exit 1
            fi
            ;;
        restore-db)
            local db_name="$1"
            local backup_file="${2:-}"
            local force="${3:-false}"
            if [[ -z "${db_name}" ]]; then
                log "ERROR" "Database name required"
                exit 1
            fi
            if [[ -z "${backup_file}" ]]; then
                backup_file=$(find_latest_backup "${db_name}")
            fi
            if restore_database "${db_name}" "${backup_file}" "${force}"; then
                verify_restore "${db_name}"
                log "INFO" "✓ Database '${db_name}' restored successfully"
                exit 0
            else
                log "ERROR" "✗ Restore failed"
                exit 1
            fi
            ;;
        verify)
            local db_name="$1"
            local expected_tables="${2:-}"
            if verify_restore "${db_name}" "${expected_tables}"; then
                log "INFO" "✓ Verification passed"
                exit 0
            else
                log "ERROR" "✗ Verification failed"
                exit 1
            fi
            ;;
        *)
            echo "Usage: $0 {list|restore-all|restore-db|verify} [options]"
            echo ""
            echo "Commands:"
            echo "  list [daily|weekly|all]        - List available backups"
            echo "  restore-all [file] [force]     - Restore all databases from backup"
            echo "  restore-db <name> [file] [force] - Restore specific database"
            echo "  verify <db> [tables]           - Verify database restore"
            echo ""
            echo "Examples:"
            echo "  $0 list"
            echo "  $0 restore-all /backups/postgres/daily/all_databases_20250122_020000.sql.gz"
            echo "  $0 restore-db payu_account"
            echo "  $0 restore-db payu_account /backups/postgres/daily/payu_account_20250122_020000.dump.gz force"
            echo "  $0 verify payu_account"
            exit 1
            ;;
    esac
}

main "$@"
