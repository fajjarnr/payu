#!/bin/bash
#
# PayU Seed Data Initialization Script
# =====================================
# Initializes test data across all services for development/testing.
#
# Usage:
#   ./scripts/seed-data.sh              # Initialize all seed data
#   ./scripts/seed-data.sh --keycloak  # Only Keycloak
#   ./scripts/seed-data.sh --db        # Only database
#   ./scripts/seed-data.sh --verify    # Verify seed data
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8099}"
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-payu}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:?ERROR: POSTGRES_PASSWORD must be set}"

print_header() {
    echo ""
    echo -e "${CYAN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║  $1${NC}"
    echo -e "${CYAN}╚═══════════════════════════════════════════════════════════════╝${NC}"
}

print_success() { echo -e "${GREEN}✓${NC} $1"; }
print_error() { echo -e "${RED}✗${NC} $1"; }
print_info() { echo -e "${CYAN}ℹ${NC} $1"; }

# ============================================================================
# KEYCLOAK SEED DATA
# ============================================================================

seed_keycloak() {
    print_header "Seeding Keycloak Data"

    # Check if Keycloak is running
    if ! curl -s -f "$KEYCLOAK_URL" > /dev/null 2>&1; then
        print_error "Keycloak not reachable at $KEYCLOAK_URL"
        return 1
    fi

    # Get admin token
    print_info "Getting Keycloak admin token..."
    KEYCLOAK_ADMIN_PWD="${KEYCLOAK_ADMIN_PASSWORD:?ERROR: KEYCLOAK_ADMIN_PASSWORD must be set}"
    ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=admin-cli" \
        -d "username=admin" \
        -d "password=$KEYCLOAK_ADMIN_PWD" \
        -d "grant_type=password" | jq -r '.access_token')

    if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
        print_error "Failed to get Keycloak admin token"
        return 1
    fi

    print_success "Admin token obtained"

    # Create realm if not exists
    print_info "Checking if 'payu' realm exists..."
    REALM_EXISTS=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms" \
        -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[] | select(.realm=="payu") | .realm' // empty)

    if [ -z "$REALM_EXISTS" ]; then
        print_info "Creating 'payu' realm..."
        curl -s -X POST "$KEYCLOAK_URL/admin/realms" \
            -H "Authorization: Bearer $ADMIN_TOKEN" \
            -H "Content-Type: application/json" \
            -d @infrastructure/keycloak/payu-realm-export.json > /dev/null
        print_success "Realm created"
    else
        print_info "Realm 'payu' already exists"
    fi

    # Verify users
    print_info "Verifying test users..."
    USERS=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/payu/users?max=10" \
        -H "Authorization: Bearer $ADMIN_TOKEN")

    echo ""
    echo -e "${CYAN}Test Users:${NC}"
    echo "$USERS" | jq -r '.[] | "  - \(.username) (\(.email))"'

    print_success "Keycloak seed data complete"
}

# ============================================================================
# DATABASE SEED DATA
# ============================================================================

seed_database() {
    print_header "Seeding Database Data"

    # Check PostgreSQL connection
    if ! PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d postgres -c "SELECT 1" > /dev/null 2>&1; then
        print_error "PostgreSQL not reachable at $POSTGRES_HOST:$POSTGRES_PORT"
        return 1
    fi

    # Run seed data migrations for each service
    local services=("payu_account" "payu_wallet" "payu_transaction")

    for service in "${services[@]}"; do
        print_info "Seeding $service..."

        # Check if V99 migration exists
        if [ "$service" = "payu_account" ]; then
            MIGRATION_FILE="backend/account-service/src/main/resources/db/migration/V99__seed_test_data.sql"
        elif [ "$service" = "payu_wallet" ]; then
            MIGRATION_FILE="backend/wallet-service/src/main/resources/db/migration/V99__seed_test_data.sql"
        elif [ "$service" = "payu_transaction" ]; then
            MIGRATION_FILE="backend/transaction-service/src/main/resources/db/migration/V99__seed_test_data.sql"
        fi

        if [ -f "$MIGRATION_FILE" ]; then
            PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$service" -f "$MIGRATION_FILE" 2>/dev/null || true
            print_success "Seeded $service"
        else
            print_info "No seed data for $service"
        fi
    done

    print_success "Database seed data complete"
}

# ============================================================================
# VERIFICATION
# ============================================================================

verify_seed_data() {
    print_header "Verifying Seed Data"

    # Verify Keycloak
    print_info "Verifying Keycloak users..."
    ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=admin-cli" \
        -d "username=admin" \
        -d "password=$KEYCLOAK_ADMIN_PWD" \
        -d "grant_type=password" | jq -r '.access_token')

    KEYCLOAK_USERS=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/payu/users?max=10" \
        -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.length')

    print_success "Keycloak users: $KEYCLOAK_USERS"

    # Verify Database
    print_info "Verifying database records..."

    # Account service
    USER_COUNT=$(PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d payu_account -tAc "SELECT COUNT(*) FROM users" 2>/dev/null || echo "0")
    ACCOUNT_COUNT=$(PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d payu_account -tAc "SELECT COUNT(*) FROM accounts" 2>/dev/null || echo "0")

    # Wallet service
    WALLET_COUNT=$(PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d payu_wallet -tAc "SELECT COUNT(*) FROM wallets" 2>/dev/null || echo "0")

    echo ""
    echo -e "${CYAN}Database Records:${NC}"
    echo "  Users:    $USER_COUNT"
    echo "  Accounts: $ACCOUNT_COUNT"
    echo "  Wallets:  $WALLET_COUNT"

    # Test API access
    print_info "Testing API access..."

    # Test login with customer1
    TEST_USER_PWD="${KEYCLOAK_TEST_USER_PASSWORD:-P@ssw0rd123}"
    LOGIN_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"customer1\",\"password\":\"$TEST_USER_PWD\"}")

    if echo "$LOGIN_RESPONSE" | jq -e '.access_token' > /dev/null 2>&1; then
        print_success "customer1 login: WORKING"
    else
        print_error "customer1 login: FAILED"
    fi
}

# ============================================================================
# MAIN
# ============================================================================

main() {
    cd "$(dirname "$0")/.."

    case "${1:-all}" in
        --keycloak|-k)
            seed_keycloak
            ;;
        --db|-d)
            seed_database
            ;;
        --verify|-v)
            verify_seed_data
            ;;
        --help|-h)
            cat << EOF
PayU Seed Data Initialization Script

Usage: ./scripts/seed-data.sh [OPTION]

Options:
  (none)      Initialize all seed data (Keycloak + Database)
  --keycloak  Only Keycloak realm and users
  --db        Only database seed data
  --verify    Verify seed data was created correctly
  --help      Show this help message

Examples:
  ./scripts/seed-data.sh           # Initialize all
  ./scripts/seed-data.sh --keycloak # Only Keycloak
  ./scripts/seed-data.sh --verify   # Verify data
EOF
            exit 0
            ;;
        *)
            seed_keycloak
            echo ""
            seed_database
            echo ""
            verify_seed_data
            ;;
    esac

    echo ""
    print_success "Seed data initialization complete!"
    echo ""
    echo "Test Credentials:"
    echo "  customer1 | P@ssw0rd123 | customer1@payu.fajjjar.my.id"
    echo "  customer2 | P@ssw0rd123 | customer2@payu.fajjjar.my.id"
    echo "  admin     | P@ssw0rd123 | admin@payu.fajjjar.my.id"
}

main "$@"
