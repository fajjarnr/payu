#!/bin/bash
#
# Keycloak User Seeder for PayU
# Creates test users in Keycloak realm
#

set -e

KEYCLOAK_URL="https://keycloak-payu-dev.apps.payu.ocp.fajjjar.my.id"
ADMIN_USER="admin"
ADMIN_PASS="EONue7dM1Zx4_Q=="
REALM="payu"

echo "=========================================="
echo "PayU Keycloak User Seeder"
echo "=========================================="

# Get admin access token
echo "Getting admin access token..."
ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/auth/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASS}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
    echo "ERROR: Failed to get admin token"
    exit 1
fi

echo "✓ Admin token obtained"

# Function to create user
create_user() {
    local username=$1
    local email=$2
    local firstname=$3
    local lastname=$4
    local password=$5

    echo ""
    echo "Creating user: $username"

    # Check if user exists
    USER_ID=$(curl -s -X GET "${KEYCLOAK_URL}/auth/admin/realms/${REALM}/users?username=${username}" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      -H "Content-Type: application/json" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ -n "$USER_ID" ]; then
        echo "  User $username already exists (ID: $USER_ID)"
        # Update password
        curl -s -X PUT "${KEYCLOAK_URL}/auth/admin/realms/${REALM}/users/${USER_ID}/reset-password" \
          -H "Authorization: Bearer ${ADMIN_TOKEN}" \
          -H "Content-Type: application/json" \
          -d "{\"type\":\"password\",\"value\":\"${password}\",\"temporary\":false}"
        echo "  ✓ Password updated"
        return
    fi

    # Create user
    USER_ID=$(curl -s -X POST "${KEYCLOAK_URL}/auth/admin/realms/${REALM}/users" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"${username}\",\"email\":\"${email}\",\"firstName\":\"${firstname}\",\"lastName\":\"${lastname}\",\"enabled\":true,\"emailVerified\":true}" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

    if [ -z "$USER_ID" ]; then
        # Get user ID after creation
        USER_ID=$(curl -s -X GET "${KEYCLOAK_URL}/auth/admin/realms/${REALM}/users?username=${username}" \
          -H "Authorization: Bearer ${ADMIN_TOKEN}" \
          -H "Content-Type: application/json" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    fi

    if [ -n "$USER_ID" ]; then
        # Set password
        curl -s -X PUT "${KEYCLOAK_URL}/auth/admin/realms/${REALM}/users/${USER_ID}/reset-password" \
          -H "Authorization: Bearer ${ADMIN_TOKEN}" \
          -H "Content-Type: application/json" \
          -d "{\"type\":\"password\",\"value\":\"${password}\",\"temporary\":false}"

        echo "  ✓ User created with password"
    else
        echo "  ✗ Failed to create user"
    fi
}

# Create test users
create_user "customer1" "customer1@payu.fajjjar.my.id" "Customer" "One" "password123"
create_user "customer2" "customer2@payu.fajjjar.my.id" "Customer" "Two" "password123"
create_user "admin" "admin@payu.fajjjar.my.id" "System" "Administrator" "admin123"

echo ""
echo "=========================================="
echo "Test Users Created Successfully!"
echo "=========================================="
echo ""
echo "Login Credentials:"
echo "  Username: customer1 | Password: password123"
echo "  Username: customer2 | Password: password123"
echo "  Username: admin     | Password: admin123"
echo "=========================================="
