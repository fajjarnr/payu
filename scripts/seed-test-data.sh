#!/bin/bash
set -e

# ============================================
# PayU Test Data Seeding Script
# Populates test databases with known test data
# ============================================

echo "=========================================="
echo "PayU Test Data Seeding"
echo "=========================================="

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
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

# Check if test environment is running
echo ""
echo "Checking test environment..."
if ! docker ps | grep -q "payu-postgres-test"; then
    echo "Test environment not running. Starting..."
    docker compose -f docker-compose.test.yml up -d postgres-test redis-test
    sleep 10
fi

POSTGRES_CMD="docker exec payu-postgres-test psql -U payu_test"

echo ""
echo "Step 1: Creating test users table..."
$POSTGRES_CMD -d payu_test_account << 'EOF' > /dev/null 2>&1
-- Create test users table if not exists
CREATE TABLE IF NOT EXISTS test_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    pin_hash VARCHAR(255) NOT NULL,
    kyc_status VARCHAR(20) DEFAULT 'pending',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
EOF
print_status $? "Test users table created"

echo ""
echo "Step 2: Seeding test users..."

# Test User 1: Full KYC user with balance
$POSTGRES_CMD -d payu_test_account << 'EOF' > /dev/null 2>&1
INSERT INTO test_users (phone_number, email, full_name, pin_hash, kyc_status, is_active)
VALUES
    ('6281234567890', 'test.user1@payu.test', 'Test User One', '$2a$10$TestPinHashForUser1...', 'verified', true)
ON CONFLICT (phone_number) DO NOTHING;
EOF
print_status $? "Test user 1 created (6281234567890 / test.user1@payu.test)"

# Test User 2: Pending KYC
$POSTGRES_CMD -d payu_test_account << 'EOF' > /dev/null 2>&1
INSERT INTO test_users (phone_number, email, full_name, pin_hash, kyc_status, is_active)
VALUES
    ('6281234567891', 'test.user2@payu.test', 'Test User Two', '$2a$10$TestPinHashForUser2...', 'pending', true)
ON CONFLICT (phone_number) DO NOTHING;
EOF
print_status $? "Test user 2 created (6281234567891 / test.user2@payu.test)"

# Test User 3: Inactive user
$POSTGRES_CMD -d payu_test_account << 'EOF' > /dev/null 2>&1
INSERT INTO test_users (phone_number, email, full_name, pin_hash, kyc_status, is_active)
VALUES
    ('6281234567892', 'test.user3@payu.test', 'Test User Three', '$2a$10$TestPinHashForUser3...', 'verified', false)
ON CONFLICT (phone_number) DO NOTHING;
EOF
print_status $? "Test user 3 created (6281234567892 / test.user3@payu.test - inactive)"

echo ""
echo "Step 3: Creating test wallets table..."
$POSTGRES_CMD -d payu_test_wallet << 'EOF' > /dev/null 2>&1
CREATE TABLE IF NOT EXISTS test_wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'IDR',
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_test_wallets_user_id ON test_wallets(user_id);
EOF
print_status $? "Test wallets table created"

echo ""
echo "Step 4: Seeding test wallets..."

# Get user IDs
USER1_ID=$($POSTGRES_CMD -d payu_test_account -tAc "SELECT id FROM test_users WHERE phone_number='6281234567890' LIMIT 1" 2>/dev/null | tr -d ' ')
USER2_ID=$($POSTGRES_CMD -d payu_test_account -tAc "SELECT id FROM test_users WHERE phone_number='6281234567891' LIMIT 1" 2>/dev/null | tr -d ' ')

# Create wallets for test users
$POSTGRES_CMD -d payu_test_wallet << 'EOF' > /dev/null 2>&1
INSERT INTO test_wallets (user_id, account_number, balance, status)
SELECT
    id,
    'TEST' || LPAD(substring(id::text, 1, 8), 12, '0'),
    1000000.00,
    'active'
FROM test_users
WHERE phone_number IN ('6281234567890', '6281234567891', '6281234567892')
ON CONFLICT (account_number) DO NOTHING;
EOF
print_status $? "Test wallets created"

echo ""
echo "Step 5: Creating test accounts table..."
$POSTGRES_CMD -d payu_test_account << 'EOF' > /dev/null 2>&1
CREATE TABLE IF NOT EXISTS test_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    account_type VARCHAR(20) DEFAULT 'SAVINGS',
    balance DECIMAL(15,2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
EOF
print_status $? "Test accounts table created"

echo ""
echo "Step 6: Creating test transactions table..."
$POSTGRES_CMD -d payu_test_transaction << 'EOF' > /dev/null 2>&1
CREATE TABLE IF NOT EXISTS test_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_number VARCHAR(50) UNIQUE NOT NULL,
    from_account VARCHAR(20) NOT NULL,
    to_account VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'IDR',
    status VARCHAR(20) DEFAULT 'completed',
    transaction_type VARCHAR(20) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_test_transactions_from_account ON test_transactions(from_account);
CREATE INDEX IF NOT EXISTS idx_test_transactions_to_account ON test_transactions(to_account);
CREATE INDEX IF NOT EXISTS idx_test_transactions_status ON test_transactions(status);
EOF
print_status $? "Test transactions table created"

echo ""
echo "Step 7: Seeding sample transactions..."

# Get wallet account numbers
ACCOUNT1=$($POSTGRES_CMD -d payu_test_wallet -tAc "SELECT account_number FROM test_wallets WHERE user_id = (SELECT id FROM payu_test_account.test_users WHERE phone_number='6281234567890' LIMIT 1) LIMIT 1" 2>/dev/null | tr -d ' ')
ACCOUNT2=$($POSTGRES_CMD -d payu_test_wallet -tAc "SELECT account_number FROM test_wallets WHERE user_id = (SELECT id FROM payu_test_account.test_users WHERE phone_number='6281234567891' LIMIT 1) LIMIT 1" 2>/dev/null | tr -d ' ')

if [ -n "$ACCOUNT1" ] && [ -n "$ACCOUNT2" ]; then
    $POSTGRES_CMD -d payu_test_transaction << EOF > /dev/null 2>&1
    INSERT INTO test_transactions (reference_number, from_account, to_account, amount, transaction_type, status, description)
    VALUES
        ('TEST-REF-001', '$ACCOUNT1', '$ACCOUNT2', 50000.00, 'transfer', 'completed', 'Test transfer 1'),
        ('TEST-REF-002', '$ACCOUNT2', '$ACCOUNT1', 25000.00, 'transfer', 'completed', 'Test transfer 2'),
        ('TEST-REF-003', '$ACCOUNT1', '$ACCOUNT2', 100000.00, 'transfer', 'pending', 'Pending transfer')
    ON CONFLICT (reference_number) DO NOTHING;
EOF
    print_status $? "Sample transactions created"
else
    print_warning "Could not create sample transactions - account numbers not found"
fi

echo ""
echo "Step 8: Creating test recipients table..."
$POSTGRES_CMD -d payu_test_transaction << 'EOF' > /dev/null 2>&1
CREATE TABLE IF NOT EXISTS test_recipients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_account VARCHAR(20) NOT NULL,
    bank_code VARCHAR(10),
    is_favorite BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_test_recipients_user_id ON test_recipients(user_id);
EOF
print_status $? "Test recipients table created"

echo ""
echo "Step 9: Creating test bills table..."
$POSTGRES_CMD -d payu_test_billing << 'EOF' > /dev/null 2>&1
CREATE TABLE IF NOT EXISTS test_bills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    biller_code VARCHAR(20) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    due_date DATE,
    status VARCHAR(20) DEFAULT 'unpaid',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_test_bills_biller_code ON test_bills(biller_code);
CREATE INDEX IF NOT EXISTS idx_test_bills_status ON test_bills(status);
EOF
print_status $? "Test bills table created"

echo ""
echo "Step 10: Seeding sample bills..."
$POSTGRES_CMD -d payu_test_billing << 'EOF' > /dev/null 2>&1
INSERT INTO test_bills (biller_code, customer_id, amount, due_date, status)
VALUES
    ('PLN', '12345678901', 250000.00, CURRENT_DATE + INTERVAL '7 days', 'unpaid'),
    ('TELKOM', '98765432109', 150000.00, CURRENT_DATE + INTERVAL '3 days', 'unpaid'),
    ('PDAM', '55555555555', 75000.00, CURRENT_DATE - INTERVAL '2 days', 'paid')
ON CONFLICT DO NOTHING;
EOF
print_status $? "Sample bills created"

echo ""
echo "Step 11: Creating test authentication data..."
$POSTGRES_CMD -d payu_test_auth << 'EOF' > /dev/null 2>&1
CREATE TABLE IF NOT EXISTS test_auth_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_value TEXT NOT NULL,
    token_type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_test_auth_tokens_user_id ON test_auth_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_test_auth_tokens_token_value ON test_auth_tokens(token_value);
EOF
print_status $? "Test auth tokens table created"

echo ""
echo "Step 12: Creating test notifications table..."
$POSTGRES_CMD -d payu_test_notification << 'EOF' > /dev/null 2>&1
CREATE TABLE IF NOT EXISTS test_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_test_notifications_user_id ON test_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_test_notifications_is_read ON test_notifications(is_read);
EOF
print_status $? "Test notifications table created"

# Final Summary
echo ""
echo "=========================================="
echo "Test Data Seeding Complete"
echo "=========================================="
echo ""
echo -e "${GREEN}✓ Test databases populated with sample data${NC}"
echo ""
echo "Test User Credentials:"
echo "  User 1: 6281234567890 / test.user1@payu.test (KYC Verified, Active)"
echo "  User 2: 6281234567891 / test.user2@payu.test (KYC Pending, Active)"
echo "  User 3: 6281234567892 / test.user3@payu.test (KYC Verified, Inactive)"
echo ""
echo "Note: PIN hashes are placeholder values. Update with actual bcrypt hashes."
echo ""
