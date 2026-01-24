#!/bin/bash
set -e

# ============================================
# PayU Test Database Cleanup Script
# Resets test databases to clean state between test runs
# ============================================

echo "=========================================="
echo "PayU Test Database Cleanup"
echo "=========================================="

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
        exit 1
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Confirm cleanup
echo ""
print_warning "This will DELETE all test data from test databases!"
echo ""
read -p "Are you sure you want to continue? (type 'yes' to confirm): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "Cleanup cancelled."
    exit 0
fi

# Check if test environment is running
echo ""
echo "Checking test environment..."
if ! docker ps | grep -q "payu-postgres-test"; then
    print_warning "Test environment not running. No cleanup needed."
    exit 0
fi

POSTGRES_CMD="docker exec payu-postgres-test psql -U payu_test"

echo ""
echo "Step 1: Cleaning account service test data..."

# Drop and recreate tables in payu_test_account
$POSTGRES_CMD -d payu_test_account << 'EOF' > /dev/null 2>&1
DROP TABLE IF EXISTS test_accounts CASCADE;
DROP TABLE IF EXISTS test_pockets CASCADE;
DROP TABLE IF EXISTS test_kyc_submissions CASCADE;
DROP TABLE IF EXISTS test_user_sessions CASCADE;
EOF
print_status $? "Account service test data cleaned"

echo ""
echo "Step 2: Cleaning auth service test data..."

$POSTGRES_CMD -d payu_test_auth << 'EOF' > /dev/null 2>&1
DROP TABLE IF EXISTS test_auth_tokens CASCADE;
DROP TABLE IF EXISTS test_refresh_tokens CASCADE;
DROP TABLE IF EXISTS test_mfa_secrets CASCADE;
DROP TABLE IF EXISTS test_login_attempts CASCADE;
EOF
print_status $? "Auth service test data cleaned"

echo ""
echo "Step 3: Cleaning transaction service test data..."

$POSTGRES_CMD -d payu_test_transaction << 'EOF' > /dev/null 2>&1
DROP TABLE IF EXISTS test_transactions CASCADE;
DROP TABLE IF EXISTS test_recipients CASCADE;
DROP TABLE IF EXISTS test_scheduled_transfers CASCADE;
DROP TABLE IF EXISTS test_split_bills CASCADE;
EOF
print_status $? "Transaction service test data cleaned"

echo ""
echo "Step 4: Cleaning wallet service test data..."

$POSTGRES_CMD -d payu_test_wallet << 'EOF' > /dev/null 2>&1
DROP TABLE IF EXISTS test_wallets CASCADE;
DROP TABLE IF EXISTS test_wallet_ledger CASCADE;
DROP TABLE IF EXISTS test_pockets CASCADE;
EOF
print_status $? "Wallet service test data cleaned"

echo ""
echo "Step 5: Cleaning billing service test data..."

$POSTGRES_CMD -d payu_test_billing << 'EOF' > /dev/null 2>&1
DROP TABLE IF EXISTS test_bills CASCADE;
DROP TABLE IF EXISTS test_bill_payments CASCADE;
DROP TABLE IF EXISTS test_e_wallet_transactions CASCADE;
EOF
print_status $? "Billing service test data cleaned"

echo ""
echo "Step 6: Cleaning notification service test data..."

$POSTGRES_CMD -d payu_test_notification << 'EOF' > /dev/null 2>&1
DROP TABLE IF EXISTS test_notifications CASCADE;
DROP TABLE IF EXISTS test_notification_templates CASCADE;
DROP TABLE IF EXISTS test_notification_logs CASCADE;
EOF
print_status $? "Notification service test data cleaned"

echo ""
echo "Step 7: Cleaning KYC service test data..."

$POSTGRES_CMD -d payu_test_kyc << 'EOF' > /dev/null 2>&1
DROP TABLE IF EXISTS test_kyc_documents CASCADE;
DROP TABLE IF EXISTS test_kyc_verifications CASCADE;
DROP TABLE IF EXISTS test_liveness_checks CASCADE;
EOF
print_status $? "KYC service test data cleaned"

echo ""
echo "Step 8: Cleaning analytics service test data..."

if docker ps | grep -q "payu-timescaledb-test"; then
    docker exec payu-timescaledb-test psql -U payu_test -d payu_test_analytics << 'EOF' > /dev/null 2>&1
DROP TABLE IF EXISTS test_user_analytics CASCADE;
DROP TABLE IF EXISTS test_transaction_analytics CASCADE;
DROP TABLE IF EXISTS test_session_analytics CASCADE;
EOF
    print_status $? "Analytics service test data cleaned"
else
    print_info "TimescaleDB not running, skipping analytics cleanup"
fi

echo ""
echo "Step 9: Resetting sequences..."

# Reset sequences for all test databases
for db in payu_test_account payu_test_auth payu_test_transaction payu_test_wallet payu_test_billing payu_test_notification payu_test_kyc; do
    $POSTGRES_CMD -d $db << 'EOF' > /dev/null 2>&1
        DO $$
        DECLARE
            seq RECORD;
        BEGIN
            FOR seq IN SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'public' LOOP
                EXECUTE 'ALTER SEQUENCE ' || quote_ident(seq.sequence_name) || ' RESTART WITH 1';
            END LOOP;
        END $$;
EOF
done
print_status $? "All sequences reset"

echo ""
echo "Step 10: Cleaning Kafka test topics..."

if docker ps | grep -q "payu-kafka-test"; then
    # Delete test topics if they exist
    docker exec payu-kafka-test kafka-topics --bootstrap-server localhost:9093 --list 2>/dev/null | grep -E "test.*-topic" | while read topic; do
        docker exec payu-kafka-test kafka-topics --bootstrap-server localhost:9093 --delete --topic "$topic" > /dev/null 2>&1 || true
    done
    print_status $? "Kafka test topics cleaned"
else
    print_info "Kafka not running, skipping topic cleanup"
fi

echo ""
echo "Step 11: Cleaning Redis test data..."

if docker ps | grep -q "payu-redis-test"; then
    docker exec payu-redis-test redis-cli FLUSHDB > /dev/null 2>&1
    print_status $? "Redis test data cleaned"
else
    print_info "Redis not running, skipping cache cleanup"
fi

echo ""
echo "=========================================="
echo "Cleanup Complete"
echo "=========================================="
echo ""
echo -e "${GREEN}✓ All test databases have been reset${NC}"
echo ""
echo "Run './scripts/seed-test-data.sh' to populate fresh test data."
echo ""
