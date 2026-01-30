#!/bin/bash
#
# Debezium Connector Registration Script for PayU Platform
# Usage: ./register-connectors.sh [environment]
# Environment: dev|staging|production (default: dev)
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENVIRONMENT="${1:-dev}"
CONNECTORS_DIR="${SCRIPT_DIR}/connectors"

# Configuration per environment
case "${ENVIRONMENT}" in
  dev)
    DEBEZIUM_URL="${DEBEZIUM_URL:-http://localhost:8083}"
    ;;
  staging)
    DEBEZIUM_URL="${DEBEZIUM_URL:-https://debezium-connect-staging.payu.internal}"
    ;;
  production)
    DEBEZIUM_URL="${DEBEZIUM_URL:-https://debezium-connect.payu.internal}"
    ;;
  *)
    echo "ERROR: Unknown environment: ${ENVIRONMENT}"
    echo "Usage: $0 [dev|staging|production]"
    exit 1
    ;;
esac

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
  echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
  echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
  echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
  log_info "Checking prerequisites..."

  if ! command -v curl &> /dev/null; then
    log_error "curl is required but not installed"
    exit 1
  fi

  if ! command -v jq &> /dev/null; then
    log_error "jq is required but not installed"
    exit 1
  fi

  log_success "Prerequisites check passed"
}

# Check if Debezium Connect is reachable
check_connectivity() {
  log_info "Checking connectivity to Debezium Connect at ${DEBEZIUM_URL}..."

  if ! curl -sf "${DEBEZIUM_URL}/" > /dev/null 2>&1; then
    log_error "Cannot connect to Debezium Connect at ${DEBEZIUM_URL}"
    log_info "Make sure Kafka Connect is running and accessible"
    exit 1
  fi

  log_success "Debezium Connect is reachable"
}

# List existing connectors
list_connectors() {
  log_info "Listing existing connectors..."

  local connectors
  connectors=$(curl -sf "${DEBEZIUM_URL}/connectors" 2>/dev/null | jq -r '.[]' 2>/dev/null || echo "")

  if [[ -z "${connectors}" ]]; then
    log_info "No existing connectors found"
  else
    echo "${connectors}" | while read -r connector; do
      log_info "  - ${connector}"
    done
  fi
}

# Delete a connector if it exists
delete_connector() {
  local connector_name="$1"

  log_warn "Deleting existing connector: ${connector_name}"

  local response
  response=$(curl -sf -X DELETE "${DEBEZIUM_URL}/connectors/${connector_name}" 2>&1 || true)

  if [[ $? -eq 0 ]]; then
    log_success "Deleted connector: ${connector_name}"
  else
    log_warn "Connector ${connector_name} may not exist or deletion failed"
  fi

  # Wait for deletion to propagate
  sleep 2
}

# Register a single connector
register_connector() {
  local connector_file="$1"
  local connector_name

  connector_name=$(jq -r '.name' "${connector_file}")

  log_info "Registering connector: ${connector_name}"
  log_info "  File: ${connector_file}"

  # Check if connector already exists
  local existing
  existing=$(curl -sf "${DEBEZIUM_URL}/connectors/${connector_name}/status" 2>/dev/null | jq -r '.name' 2>/dev/null || echo "")

  if [[ "${existing}" == "${connector_name}" ]]; then
    log_warn "Connector ${connector_name} already exists"
    read -p "Do you want to delete and recreate it? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
      delete_connector "${connector_name}"
    else
      log_info "Skipping ${connector_name}"
      return 0
    fi
  fi

  # Register the connector
  local response
  local http_code

  response=$(curl -sf -w "%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d "@${connector_file}" \
    "${DEBEZIUM_URL}/connectors" 2>&1)

  http_code=$(echo "${response}" | tail -c 4)

  if [[ "${http_code}" == "201" ]] || [[ "${http_code}" == "200" ]]; then
    log_success "Successfully registered connector: ${connector_name}"

    # Verify connector status
    sleep 2
    local status
    status=$(curl -sf "${DEBEZIUM_URL}/connectors/${connector_name}/status" 2>/dev/null | jq -r '.connector.state' 2>/dev/null || echo "UNKNOWN")

    if [[ "${status}" == "RUNNING" ]]; then
      log_success "Connector ${connector_name} is RUNNING"
    else
      log_warn "Connector ${connector_name} status: ${status}"
    fi
  else
    log_error "Failed to register connector: ${connector_name}"
    log_error "HTTP Code: ${http_code}"
    log_error "Response: ${response}"
    return 1
  fi
}

# Validate connector configuration
validate_connector() {
  local connector_file="$1"
  local connector_name

  connector_name=$(jq -r '.name' "${connector_file}")

  log_info "Validating connector: ${connector_name}"

  # Extract config for validation
  local config
  config=$(jq '.config' "${connector_file}")

  # Validate required fields
  local required_fields=(
    "connector.class"
    "database.hostname"
    "database.port"
    "database.user"
    "database.password"
    "database.dbname"
    "database.server.name"
    "plugin.name"
    "slot.name"
  )

  local missing_fields=()
  for field in "${required_fields[@]}"; do
    if ! echo "${config}" | jq -e --arg field "${field}" 'has($field)' > /dev/null 2>&1; then
      # Check if it's using file-based secret reference
      if ! echo "${config}" | jq -r --arg field "${field}" '.[$field]' | grep -q '^\${file:'; then
        missing_fields+=("${field}")
      fi
    fi
  done

  if [[ ${#missing_fields[@]} -gt 0 ]]; then
    log_error "Missing required fields in ${connector_name}:"
    for field in "${missing_fields[@]}"; do
      log_error "  - ${field}"
    done
    return 1
  fi

  log_success "Validation passed for ${connector_name}"
  return 0
}

# Main execution
main() {
  echo "========================================"
  echo "Debezium Connector Registration"
  echo "Environment: ${ENVIRONMENT}"
  echo "Debezium URL: ${DEBEZIUM_URL}"
  echo "========================================"
  echo

  check_prerequisites
  check_connectivity
  list_connectors

  echo
  log_info "Starting connector registration..."
  echo

  local failed_connectors=()
  local success_count=0

  # Process all connector JSON files
  for connector_file in "${CONNECTORS_DIR}"/*.json; do
    if [[ -f "${connector_file}" ]]; then
      echo "----------------------------------------"

      if ! validate_connector "${connector_file}"; then
        failed_connectors+=("$(basename "${connector_file}")")
        continue
      fi

      if register_connector "${connector_file}"; then
        ((success_count++))
      else
        failed_connectors+=("$(basename "${connector_file}")")
      fi

      echo
    fi
  done

  echo "========================================"
  echo "Registration Summary"
  echo "========================================"
  log_success "Successfully registered: ${success_count} connector(s)"

  if [[ ${#failed_connectors[@]} -gt 0 ]]; then
    log_error "Failed connectors:"
    for connector in "${failed_connectors[@]}"; do
      log_error "  - ${connector}"
    done
    exit 1
  fi

  echo
  log_info "Final connector status:"
  list_connectors

  echo
  log_success "All connectors registered successfully!"
}

# Handle script interruption
trap 'log_error "Script interrupted"; exit 130' INT TERM

# Run main function
main "$@"
