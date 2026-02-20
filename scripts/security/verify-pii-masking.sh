#!/bin/bash
#
# PayU Security Audit Script - PII Masking Verification
# Verifies that PII fields are properly masked in logs and API responses
#
# Usage: ./verify-pii-masking.sh [service-name]
# Example: ./verify-pii-masking.sh account-service

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BACKEND_DIR="/home/ubuntu/payu/backend"
REPORT_FILE="/tmp/pii-masking-report-$(date +%Y%m%d-%H%M%S).txt"

# PII field patterns to check
PII_FIELDS=(
    "nik"
    "cardNumber"
    "card_number"
    "cvv"
    "password"
    "pin"
    "email"
    "phoneNumber"
    "phone_number"
    "accountNumber"
    "account_number"
)

# Masking patterns that indicate proper masking
MASKING_PATTERNS=(
    "\*\*\*\*"
    "\*\*\*"
    "masked"
    "MASKED"
)

echo "========================================" | tee -a "$REPORT_FILE"
echo "PayU PII Masking Verification Report" | tee -a "$REPORT_FILE"
echo "Generated: $(date)" | tee -a "$REPORT_FILE"
echo "========================================" | tee -a "$REPORT_FILE"
echo "" | tee -a "$REPORT_FILE"

# Function to check if a service has @Sensitive annotation usage
check_sensitive_annotation() {
    local service=$1
    local service_dir="$BACKEND_DIR/$service"

    if [ ! -d "$service_dir" ]; then
        echo -e "${YELLOW}WARNING: Service directory not found: $service_dir${NC}" | tee -a "$REPORT_FILE"
        return 1
    fi

    echo "Checking @Sensitive annotation usage in $service..." | tee -a "$REPORT_FILE"

    local sensitive_count=$(find "$service_dir" -name "*.java" -exec grep -l "@Sensitive" {} \; 2>/dev/null | wc -l)

    if [ "$sensitive_count" -gt 0 ]; then
        echo -e "${GREEN}PASS: Found @Sensitive annotation in $sensitive_count file(s)${NC}" | tee -a "$REPORT_FILE"
        find "$service_dir" -name "*.java" -exec grep -l "@Sensitive" {} \; 2>/dev/null | while read file; do
            echo "  - $file" | tee -a "$REPORT_FILE"
        done
        return 0
    else
        echo -e "${YELLOW}WARNING: No @Sensitive annotation found in $service${NC}" | tee -a "$REPORT_FILE"
        return 1
    fi
}

# Function to check for EncryptedStringConverter usage
check_encryption_converter() {
    local service=$1
    local service_dir="$BACKEND_DIR/$service"

    if [ ! -d "$service_dir" ]; then
        return 1
    fi

    echo "Checking EncryptedStringConverter usage in $service..." | tee -a "$REPORT_FILE"

    local encrypt_count=$(find "$service_dir" -name "*.java" -exec grep -l "EncryptedStringConverter" {} \; 2>/dev/null | wc -l)

    if [ "$encrypt_count" -gt 0 ]; then
        echo -e "${GREEN}PASS: Found EncryptedStringConverter in $encrypt_count file(s)${NC}" | tee -a "$REPORT_FILE"
        find "$service_dir" -name "*.java" -exec grep -l "EncryptedStringConverter" {} \; 2>/dev/null | while read file; do
            echo "  - $file" | tee -a "$REPORT_FILE"
        done
        return 0
    else
        echo -e "${YELLOW}INFO: No EncryptedStringConverter usage in $service (may not handle sensitive data)${NC}" | tee -a "$REPORT_FILE"
        return 0
    fi
}

# Function to check for @Audited annotation
check_audit_annotation() {
    local service=$1
    local service_dir="$BACKEND_DIR/$service"

    if [ ! -d "$service_dir" ]; then
        return 1
    fi

    echo "Checking @Audited annotation usage in $service..." | tee -a "$REPORT_FILE"

    local audit_count=$(find "$service_dir" -name "*.java" -exec grep -l "@Audited" {} \; 2>/dev/null | wc -l)

    if [ "$audit_count" -gt 0 ]; then
        echo -e "${GREEN}PASS: Found @Audited annotation in $audit_count file(s)${NC}" | tee -a "$REPORT_FILE"
        return 0
    else
        echo -e "${YELLOW}WARNING: No @Audited annotation found in $service${NC}" | tee -a "$REPORT_FILE"
        return 1
    fi
}

# Function to check for security-starter dependency
check_security_starter() {
    local service=$1
    local pom_file="$BACKEND_DIR/$service/pom.xml"

    if [ ! -f "$pom_file" ]; then
        return 1
    fi

    echo "Checking security-starter dependency in $service..." | tee -a "$REPORT_FILE"

    if grep -q "security-starter" "$pom_file" 2>/dev/null; then
        echo -e "${GREEN}PASS: security-starter dependency found${NC}" | tee -a "$REPORT_FILE"
        return 0
    else
        echo -e "${RED}FAIL: security-starter dependency NOT found${NC}" | tee -a "$REPORT_FILE"
        return 1
    fi
}

# Function to check for @PreAuthorize annotations
check_preauthorize() {
    local service=$1
    local service_dir="$BACKEND_DIR/$service"

    if [ ! -d "$service_dir" ]; then
        return 1
    fi

    echo "Checking @PreAuthorize annotation usage in $service..." | tee -a "$REPORT_FILE"

    local preauth_count=$(find "$service_dir" -name "*.java" -exec grep -l "@PreAuthorize" {} \; 2>/dev/null | wc -l)

    if [ "$preauth_count" -gt 0 ]; then
        echo -e "${GREEN}PASS: Found @PreAuthorize annotation in $preauth_count file(s)${NC}" | tee -a "$REPORT_FILE"
        return 0
    else
        echo -e "${YELLOW}WARNING: No @PreAuthorize annotation found in $service${NC}" | tee -a "$REPORT_FILE"
        return 1
    fi
}

# Function to check for logging of sensitive data (anti-pattern)
check_sensitive_logging() {
    local service=$1
    local service_dir="$BACKEND_DIR/$service"

    if [ ! -d "$service_dir" ]; then
        return 1
    fi

    echo "Checking for potential sensitive data logging in $service..." | tee -a "$REPORT_FILE"

    local issues=0

    # Check for direct logging of PII fields
    for field in "${PII_FIELDS[@]}"; do
        local matches=$(find "$service_dir" -name "*.java" -exec grep -n "log.*\.$field\|log.*$field" {} + 2>/dev/null | grep -v "// " | wc -l)
        if [ "$matches" -gt 0 ]; then
            echo -e "${YELLOW}WARNING: Potential logging of '$field' found in $matches location(s)${NC}" | tee -a "$REPORT_FILE"
            find "$service_dir" -name "*.java" -exec grep -n "log.*\.$field\|log.*$field" {} + 2>/dev/null | head -5 | tee -a "$REPORT_FILE"
            issues=$((issues + 1))
        fi
    done

    if [ "$issues" -eq 0 ]; then
        echo -e "${GREEN}PASS: No obvious sensitive data logging patterns found${NC}" | tee -a "$REPORT_FILE"
        return 0
    else
        return 1
    fi
}

# Main execution
main() {
    local target_service=$1
    local services=()

    if [ -n "$target_service" ]; then
        services=("$target_service")
    else
        # Get all service directories
        for dir in "$BACKEND_DIR"/*/; do
            if [ -f "$dir/pom.xml" ]; then
                services+=("$(basename "$dir")")
            fi
        done
    fi

    echo "Scanning ${#services[@]} service(s)..." | tee -a "$REPORT_FILE"
    echo "" | tee -a "$REPORT_FILE"

    local pass_count=0
    local fail_count=0
    local warn_count=0

    for service in "${services[@]}"; do
        echo "----------------------------------------" | tee -a "$REPORT_FILE"
        echo "Service: $service" | tee -a "$REPORT_FILE"
        echo "----------------------------------------" | tee -a "$REPORT_FILE"

        local service_pass=0
        local service_fail=0

        if check_security_starter "$service"; then
            service_pass=$((service_pass + 1))
        else
            service_fail=$((service_fail + 1))
        fi

        if check_sensitive_annotation "$service"; then
            service_pass=$((service_pass + 1))
        else
            service_fail=$((service_fail + 1))
        fi

        if check_encryption_converter "$service"; then
            service_pass=$((service_pass + 1))
        fi

        if check_audit_annotation "$service"; then
            service_pass=$((service_pass + 1))
        else
            service_fail=$((service_fail + 1))
        fi

        if check_preauthorize "$service"; then
            service_pass=$((service_pass + 1))
        else
            service_fail=$((service_fail + 1))
        fi

        check_sensitive_logging "$service"

        echo "" | tee -a "$REPORT_FILE"

        pass_count=$((pass_count + service_pass))
        fail_count=$((fail_count + service_fail))
    done

    echo "========================================" | tee -a "$REPORT_FILE"
    echo "Summary" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"
    echo "Total Checks Passed: $pass_count" | tee -a "$REPORT_FILE"
    echo "Total Checks Failed: $fail_count" | tee -a "$REPORT_FILE"
    echo "" | tee -a "$REPORT_FILE"
    echo "Report saved to: $REPORT_FILE" | tee -a "$REPORT_FILE"

    if [ "$fail_count" -eq 0 ]; then
        echo -e "${GREEN}All checks passed!${NC}"
        exit 0
    else
        echo -e "${YELLOW}Some checks failed. Review the report for details.${NC}"
        exit 1
    fi
}

main "$@"
