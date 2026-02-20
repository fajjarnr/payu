#!/bin/bash
#
# PayU Security Audit Script - Audit Logger Verification
# Verifies that audit logging is properly configured and functioning
#
# Usage: ./audit-logger-verification.sh [service-name]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BACKEND_DIR="/home/ubuntu/payu/backend"
REPORT_FILE="/tmp/audit-logger-report-$(date +%Y%m%d-%H%M%S).txt"

# Critical operations that should be audited
CRITICAL_OPERATIONS=(
    "LOGIN"
    "LOGOUT"
    "TRANSFER"
    "CREATE"
    "UPDATE"
    "DELETE"
    "KYC_APPROVE"
    "KYC_REJECT"
)

echo "========================================" | tee -a "$REPORT_FILE"
echo "PayU Audit Logger Verification Report" | tee -a "$REPORT_FILE"
echo "Generated: $(date)" | tee -a "$REPORT_FILE"
echo "========================================" | tee -a "$REPORT_FILE"
echo "" | tee -a "$REPORT_FILE"

# Function to check AuditAspect configuration
check_audit_aspect() {
    local audit_aspect="$BACKEND_DIR/shared/security-starter/src/main/java/id/payu/security/audit/AuditAspect.java"

    echo "Checking AuditAspect configuration..." | tee -a "$REPORT_FILE"

    if [ ! -f "$audit_aspect" ]; then
        echo -e "${RED}FAIL: AuditAspect not found!${NC}" | tee -a "$REPORT_FILE"
        return 1
    fi

    echo -e "${GREEN}PASS: AuditAspect found${NC}" | tee -a "$REPORT_FILE"

    # Check for @Around annotation
    if grep -q "@Around.*@annotation.*Audited" "$audit_aspect"; then
        echo -e "${GREEN}PASS: @Around advice for @Audited annotation found${NC}" | tee -a "$REPORT_FILE"
    fi

    # Check for IP address capture
    if grep -q "ipAddress\|getClientIpAddress" "$audit_aspect"; then
        echo -e "${GREEN}PASS: IP address capture implemented${NC}" | tee -a "$REPORT_FILE"
    fi

    # Check for user agent capture
    if grep -q "userAgent" "$audit_aspect"; then
        echo -e "${GREEN}PASS: User agent capture implemented${NC}" | tee -a "$REPORT_FILE"
    fi

    # Check for success/failure tracking
    if grep -q "setSuccess\|success" "$audit_aspect"; then
        echo -e "${GREEN}PASS: Success/failure tracking implemented${NC}" | tee -a "$REPORT_FILE"
    fi

    return 0
}

# Function to check AuditEvent structure
check_audit_event() {
    local audit_event="$BACKEND_DIR/shared/security-starter/src/main/java/id/payu/security/audit/AuditEvent.java"

    echo "Checking AuditEvent structure..." | tee -a "$REPORT_FILE"

    if [ ! -f "$audit_event" ]; then
        echo -e "${RED}FAIL: AuditEvent not found!${NC}" | tee -a "$REPORT_FILE"
        return 1
    fi

    echo -e "${GREEN}PASS: AuditEvent found${NC}" | tee -a "$REPORT_FILE"

    # Check for required fields
    local required_fields=("eventType" "operation" "entityType" "timestamp" "userId" "success")
    for field in "${required_fields[@]}"; do
        if grep -q "$field" "$audit_event"; then
            echo -e "${GREEN}PASS: Field '$field' found in AuditEvent${NC}" | tee -a "$REPORT_FILE"
        else
            echo -e "${YELLOW}WARNING: Field '$field' not found in AuditEvent${NC}" | tee -a "$REPORT_FILE"
        fi
    done

    return 0
}

# Function to check AuditLogPublisher
check_audit_publisher() {
    local audit_publisher="$BACKEND_DIR/shared/security-starter/src/main/java/id/payu/security/audit/AuditLogPublisher.java"

    echo "Checking AuditLogPublisher..." | tee -a "$REPORT_FILE"

    if [ ! -f "$audit_publisher" ]; then
        echo -e "${RED}FAIL: AuditLogPublisher not found!${NC}" | tee -a "$REPORT_FILE"
        return 1
    fi

    echo -e "${GREEN}PASS: AuditLogPublisher found${NC}" | tee -a "$REPORT_FILE"

    # Check for Kafka integration
    if grep -q "KafkaTemplate\|kafkaTemplate" "$audit_publisher"; then
        echo -e "${GREEN}PASS: Kafka integration found${NC}" | tee -a "$REPORT_FILE"
    fi

    # Check for fallback logging
    if grep -q "publishSafe\|fallback" "$audit_publisher"; then
        echo -e "${GREEN}PASS: Fallback logging mechanism found${NC}" | tee -a "$REPORT_FILE"
    fi

    return 0
}

# Function to check @Audited annotation usage in a service
check_audited_usage() {
    local service=$1
    local service_dir="$BACKEND_DIR/$service"

    if [ ! -d "$service_dir" ]; then
        return 1
    fi

    echo "Checking @Audited usage in $service..." | tee -a "$REPORT_FILE"

    local audited_count=$(find "$service_dir" -name "*.java" -exec grep -l "@Audited" {} \; 2>/dev/null | wc -l)

    if [ "$audited_count" -gt 0 ]; then
        echo -e "${GREEN}PASS: @Audited annotation used in $audited_count file(s)${NC}" | tee -a "$REPORT_FILE"

        # List the operations being audited
        echo "  Audited operations:" | tee -a "$REPORT_FILE"
        find "$service_dir" -name "*.java" -exec grep -A 5 "@Audited" {} \; 2>/dev/null | \
            grep "operation.*=.*Audited.Operation" | head -10 | \
            sed 's/.*Operation\.\([A-Z_]*\).*/    - \1/' | tee -a "$REPORT_FILE"

        return 0
    else
        echo -e "${YELLOW}WARNING: No @Audited annotation found in $service${NC}" | tee -a "$REPORT_FILE"
        return 1
    fi
}

# Function to check for critical operations that should be audited
check_critical_operations() {
    local service=$1
    local service_dir="$BACKEND_DIR/$service"

    if [ ! -d "$service_dir" ]; then
        return 0
    fi

    echo "Checking critical operations coverage in $service..." | tee -a "$REPORT_FILE"

    # Check for controller methods that should be audited
    local controller_dir="$service_dir/src/main/java/id/payu/$service/adapter/web"

    if [ ! -d "$controller_dir" ]; then
        controller_dir="$service_dir/src/main/java/id/payu/$service/controller"
    fi

    if [ ! -d "$controller_dir" ]; then
        echo -e "${YELLOW}INFO: No controller directory found${NC}" | tee -a "$REPORT_FILE"
        return 0
    fi

    # Count POST/PUT/DELETE methods (mutating operations)
    local mutating_methods=$(find "$controller_dir" -name "*.java" -exec grep -c "@PostMapping\|@PutMapping\|@DeleteMapping" {} \; 2>/dev/null | awk '{sum+=$1} END {print sum}')

    # Count @Audited annotations
    local audited_methods=$(find "$controller_dir" -name "*.java" -exec grep -c "@Audited" {} \; 2>/dev/null | awk '{sum+=$1} END {print sum}')

    if [ -z "$mutating_methods" ]; then
        mutating_methods=0
    fi

    if [ -z "$audited_methods" ]; then
        audited_methods=0
    fi

    echo "  Mutating operations (POST/PUT/DELETE): $mutating_methods" | tee -a "$REPORT_FILE"
    echo "  Audited operations: $audited_methods" | tee -a "$REPORT_FILE"

    if [ "$audited_methods" -ge "$mutating_methods" ] && [ "$mutating_methods" -gt 0 ]; then
        echo -e "${GREEN}PASS: All mutating operations appear to be audited${NC}" | tee -a "$REPORT_FILE"
    elif [ "$mutating_methods" -eq 0 ]; then
        echo -e "${YELLOW}INFO: No mutating operations found${NC}" | tee -a "$REPORT_FILE"
    else
        echo -e "${YELLOW}WARNING: Some mutating operations may not be audited${NC}" | tee -a "$REPORT_FILE"
    fi
}

# Function to check audit configuration in application.yml
check_audit_config() {
    local service=$1
    local app_yml="$BACKEND_DIR/$service/src/main/resources/application.yml"

    if [ ! -f "$app_yml" ]; then
        return 0
    fi

    echo "Checking audit configuration in $service..." | tee -a "$REPORT_FILE"

    if grep -q "audit" "$app_yml" 2>/dev/null; then
        echo -e "${GREEN}PASS: Audit configuration found${NC}" | tee -a "$REPORT_FILE"

        # Check if audit is enabled
        if grep -q "audit.*enabled.*true\|audit-enabled.*true" "$app_yml" 2>/dev/null; then
            echo -e "${GREEN}PASS: Audit logging is enabled${NC}" | tee -a "$REPORT_FILE"
        elif grep -q "audit.*enabled.*false\|audit-enabled.*false" "$app_yml" 2>/dev/null; then
            echo -e "${RED}FAIL: Audit logging is disabled!${NC}" | tee -a "$REPORT_FILE"
        fi
    else
        echo -e "${YELLOW}INFO: No explicit audit configuration (may use defaults)${NC}" | tee -a "$REPORT_FILE"
    fi
}

# Function to verify audit log retention
check_retention_config() {
    local security_props="$BACKEND_DIR/shared/security-starter/src/main/java/id/payu/security/config/SecurityProperties.java"

    echo "Checking audit retention configuration..." | tee -a "$REPORT_FILE"

    if [ -f "$security_props" ]; then
        if grep -q "retentionDays\|retention" "$security_props"; then
            local retention=$(grep -A 2 "retentionDays" "$security_props" | grep -o "[0-9]*" | head -1)
            if [ -n "$retention" ]; then
                echo -e "${GREEN}PASS: Audit retention configured for $retention days${NC}" | tee -a "$REPORT_FILE"

                if [ "$retention" -ge 365 ]; then
                    echo -e "${GREEN}PASS: Retention period meets minimum requirement (365 days)${NC}" | tee -a "$REPORT_FILE"
                else
                    echo -e "${YELLOW}WARNING: Retention period may not meet regulatory requirements${NC}" | tee -a "$REPORT_FILE"
                fi
            fi
        fi
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

    echo "Verifying audit logging for ${#services[@]} service(s)..." | tee -a "$REPORT_FILE"
    echo "" | tee -a "$REPORT_FILE"

    local pass_count=0
    local fail_count=0

    # Check shared components first
    echo "========================================" | tee -a "$REPORT_FILE"
    echo "Shared Audit Components" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"

    check_audit_aspect
    check_audit_event
    check_audit_publisher
    check_retention_config

    echo "" | tee -a "$REPORT_FILE"

    # Check each service
    echo "========================================" | tee -a "$REPORT_FILE"
    echo "Service Audit Configuration" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"

    for service in "${services[@]}"; do
        echo "----------------------------------------" | tee -a "$REPORT_FILE"
        echo "Service: $service" | tee -a "$REPORT_FILE"
        echo "----------------------------------------" | tee -a "$REPORT_FILE"

        local service_pass=0
        local service_fail=0

        if check_audited_usage "$service"; then
            service_pass=$((service_pass + 1))
        else
            service_fail=$((service_fail + 1))
        fi

        check_critical_operations "$service"
        check_audit_config "$service"

        if [ "$service_fail" -eq 0 ]; then
            pass_count=$((pass_count + 1))
        else
            fail_count=$((fail_count + 1))
        fi

        echo "" | tee -a "$REPORT_FILE"
    done

    echo "========================================" | tee -a "$REPORT_FILE"
    echo "Summary" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"
    echo "Services with Audit Coverage: $pass_count" | tee -a "$REPORT_FILE"
    echo "Services Needing Attention: $fail_count" | tee -a "$REPORT_FILE"
    echo "" | tee -a "$REPORT_FILE"
    echo "Report saved to: $REPORT_FILE" | tee -a "$REPORT_FILE"

    if [ "$fail_count" -eq 0 ]; then
        echo -e "${GREEN}All audit logging checks passed!${NC}"
        exit 0
    else
        echo -e "${YELLOW}Some services need audit logging improvements. Review the report.${NC}"
        exit 1
    fi
}

main "$@"
