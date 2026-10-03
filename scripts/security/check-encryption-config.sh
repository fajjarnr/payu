#!/bin/bash
# Verifies that encryption is properly configured across all services
# Usage: ./check-encryption-config.sh

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BACKEND_DIR="/home/ubuntu/payu/backend"
INFRA_DIR="/home/ubuntu/payu/infrastructure"
REPORT_FILE="/tmp/encryption-config-report-$(date +%Y%m%d-%H%M%S).txt"

echo "========================================" | tee -a "$REPORT_FILE"
echo "PayU Encryption Configuration Audit" | tee -a "$REPORT_FILE"
echo "Generated: $(date)" | tee -a "$REPORT_FILE"
echo "========================================" | tee -a "$REPORT_FILE"
echo "" | tee -a "$REPORT_FILE"

check_app_encryption_config() {
    local service=$1
    local app_yml="$BACKEND_DIR/$service/src/main/resources/application.yml"

    if [ ! -f "$app_yml" ]; then
        echo -e "${YELLOW}WARNING: application.yml not found for $service${NC}" | tee -a "$REPORT_FILE"
        return 1
    fi

    echo "Checking encryption config in $service..." | tee -a "$REPORT_FILE"

    local issues=0

    if grep -q "encryption" "$app_yml" 2>/dev/null; then
        echo -e "${GREEN}PASS: Encryption configuration found${NC}" | tee -a "$REPORT_FILE"

        if grep -q "ENCRYPTION_KEY\|encryption.*password.*:\s*\${" "$app_yml" 2>/dev/null; then
            echo -e "${GREEN}PASS: Encryption key uses environment variable${NC}" | tee -a "$REPORT_FILE"
        elif grep -q "encryption.*password.*:\s*[a-zA-Z0-9]" "$app_yml" 2>/dev/null; then
            echo -e "${RED}FAIL: Hardcoded encryption key detected!${NC}" | tee -a "$REPORT_FILE"
            issues=$((issues + 1))
        fi
    else
        echo -e "${YELLOW}INFO: No explicit encryption configuration (may use defaults)${NC}" | tee -a "$REPORT_FILE"
    fi

    if grep -q "ssl:\|tls:\|server.ssl" "$app_yml" 2>/dev/null; then
        echo -e "${GREEN}PASS: TLS/SSL configuration found${NC}" | tee -a "$REPORT_FILE"
    else
        echo -e "${YELLOW}INFO: No TLS configuration in application.yml (may be handled at infrastructure level)${NC}" | tee -a "$REPORT_FILE"
    fi

    return $issues
}

check_vault_config() {
    local service=$1
    local app_yml="$BACKEND_DIR/$service/src/main/resources/application.yml"

    if [ ! -f "$app_yml" ]; then
        return 0
    fi

    echo "Checking Vault integration in $service..." | tee -a "$REPORT_FILE"

    if grep -q "vault\|hashicorp" "$app_yml" 2>/dev/null; then
        echo -e "${GREEN}PASS: Vault configuration found${NC}" | tee -a "$REPORT_FILE"
        return 0
    else
        echo -e "${YELLOW}INFO: No Vault configuration (may use Kubernetes secrets)${NC}" | tee -a "$REPORT_FILE"
        return 0
    fi
}

check_database_encryption() {
    local service=$1
    local app_yml="$BACKEND_DIR/$service/src/main/resources/application.yml"

    if [ ! -f "$app_yml" ]; then
        return 0
    fi

    echo "Checking database configuration in $service..." | tee -a "$REPORT_FILE"

    if grep -q "sslmode\|sslMode\|ssl=true" "$app_yml" 2>/dev/null; then
        echo -e "${GREEN}PASS: Database SSL enabled${NC}" | tee -a "$REPORT_FILE"
    else
        echo -e "${YELLOW}WARNING: Database SSL not explicitly enabled${NC}" | tee -a "$REPORT_FILE"
    fi

    if grep -q "password.*:\s*\${" "$app_yml" 2>/dev/null; then
        echo -e "${GREEN}PASS: Database password uses environment variable${NC}" | tee -a "$REPORT_FILE"
    elif grep -q "password.*:\s*[^${}" "$app_yml" 2>/dev/null; then
        if grep -q "password.*:\s*postgres\|password.*:\s*password\|password.*:\s*admin" "$app_yml" 2>/dev/null; then
            echo -e "${RED}FAIL: Default database password detected!${NC}" | tee -a "$REPORT_FILE"
        fi
    fi
}

check_hardcoded_secrets() {
    local service=$1
    local service_dir="$BACKEND_DIR/$service"

    if [ ! -d "$service_dir" ]; then
        return 0
    fi

    echo "Checking for hardcoded secrets in $service..." | tee -a "$REPORT_FILE"

    local secrets_found=0

    local patterns=(
        "password:\s*[a-zA-Z0-9]{8,}"
        "secret:\s*[a-zA-Z0-9]{8,}"
        "api_key:\s*[a-zA-Z0-9]{8,}"
        "apikey:\s*[a-zA-Z0-9]{8,}"
        "token:\s*[a-zA-Z0-9]{20,}"
        "private_key"
        "privatekey"
    )

    for pattern in "${patterns[@]}"; do
        local matches=$(find "$service_dir" -name "*.yml" -o -name "*.yaml" -o -name "*.properties" 2>/dev/null | \
            xargs grep -h -E "$pattern" 2>/dev/null | grep -v "^#" | grep -v "\${" | wc -l)

        if [ "$matches" -gt 0 ]; then
            echo -e "${RED}WARNING: Potential hardcoded secret pattern '$pattern' found${NC}" | tee -a "$REPORT_FILE"
            find "$service_dir" -name "*.yml" -o -name "*.yaml" -o -name "*.properties" 2>/dev/null | \
                xargs grep -n -E "$pattern" 2>/dev/null | head -3 | tee -a "$REPORT_FILE"
            secrets_found=$((secrets_found + 1))
        fi
    done

    if [ "$secrets_found" -eq 0 ]; then
        echo -e "${GREEN}PASS: No obvious hardcoded secrets found${NC}" | tee -a "$REPORT_FILE"
    fi

    return $secrets_found
}

check_infrastructure_tls() {
    echo "Checking infrastructure TLS configuration..." | tee -a "$REPORT_FILE"

    local routes_dir="$INFRA_DIR/openshift/overlays"

    if [ -d "$routes_dir" ]; then
        local tls_routes=$(find "$routes_dir" -name "*.yaml" -exec grep -l "tls:" {} \; 2>/dev/null | wc -l)

        if [ "$tls_routes" -gt 0 ]; then
            echo -e "${GREEN}PASS: TLS configuration found in $tls_routes route file(s)${NC}" | tee -a "$REPORT_FILE"
        else
            echo -e "${YELLOW}WARNING: No TLS configuration found in routes${NC}" | tee -a "$REPORT_FILE"
        fi
    fi

    if [ -d "$INFRA_DIR/openshift/infra/base/cert-manager" ]; then
        echo -e "${GREEN}PASS: cert-manager configuration found${NC}" | tee -a "$REPORT_FILE"
    else
        echo -e "${YELLOW}WARNING: cert-manager configuration not found${NC}" | tee -a "$REPORT_FILE"
    fi
}

check_encryption_service() {
    echo "Checking EncryptionService implementation..." | tee -a "$REPORT_FILE"

    local encryption_service="$BACKEND_DIR/shared/security-starter/src/main/java/id/payu/security/crypto/EncryptionService.java"

    if [ ! -f "$encryption_service" ]; then
        echo -e "${RED}FAIL: EncryptionService not found!${NC}" | tee -a "$REPORT_FILE"
        return 1
    fi

    if grep -q "AES/GCM" "$encryption_service"; then
        echo -e "${GREEN}PASS: AES-GCM algorithm detected${NC}" | tee -a "$REPORT_FILE"
    else
        echo -e "${RED}FAIL: AES-GCM not detected${NC}" | tee -a "$REPORT_FILE"
    fi

    if grep -q "256\|KEY_LENGTH.*256" "$encryption_service"; then
        echo -e "${GREEN}PASS: 256-bit key length detected${NC}" | tee -a "$REPORT_FILE"
    fi

    if grep -q "PBKDF2" "$encryption_service"; then
        echo -e "${GREEN}PASS: PBKDF2 key derivation detected${NC}" | tee -a "$REPORT_FILE"
    fi

    if grep -q "SecureRandom" "$encryption_service"; then
        echo -e "${GREEN}PASS: SecureRandom for IV generation detected${NC}" | tee -a "$REPORT_FILE"
    fi
}

main() {
    local services=()

    for dir in "$BACKEND_DIR"/*/; do
        if [ -f "$dir/pom.xml" ]; then
            services+=("$(basename "$dir")")
        fi
    done

    echo "Scanning ${#services[@]} service(s) for encryption configuration..." | tee -a "$REPORT_FILE"
    echo "" | tee -a "$REPORT_FILE"

    local pass_count=0
    local fail_count=0

    echo "========================================" | tee -a "$REPORT_FILE"
    echo "Shared Security Components" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"
    check_encryption_service
    echo "" | tee -a "$REPORT_FILE"

    echo "========================================" | tee -a "$REPORT_FILE"
    echo "Infrastructure Configuration" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"
    check_infrastructure_tls
    echo "" | tee -a "$REPORT_FILE"

    echo "========================================" | tee -a "$REPORT_FILE"
    echo "Service Configuration" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"

    for service in "${services[@]}"; do
        echo "----------------------------------------" | tee -a "$REPORT_FILE"
        echo "Service: $service" | tee -a "$REPORT_FILE"
        echo "----------------------------------------" | tee -a "$REPORT_FILE"

        local service_issues=0

        check_app_encryption_config "$service"
        service_issues=$((service_issues + $?))

        check_vault_config "$service"

        check_database_encryption "$service"

        check_hardcoded_secrets "$service"
        service_issues=$((service_issues + $?))

        if [ "$service_issues" -eq 0 ]; then
            pass_count=$((pass_count + 1))
        else
            fail_count=$((fail_count + 1))
        fi

        echo "" | tee -a "$REPORT_FILE"
    done

    echo "========================================" | tee -a "$REPORT_FILE"
    echo "Summary" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"
    echo "Services Passed: $pass_count" | tee -a "$REPORT_FILE"
    echo "Services with Issues: $fail_count" | tee -a "$REPORT_FILE"
    echo "" | tee -a "$REPORT_FILE"
    echo "Report saved to: $REPORT_FILE" | tee -a "$REPORT_FILE"

    if [ "$fail_count" -eq 0 ]; then
        echo -e "${GREEN}All encryption configuration checks passed!${NC}"
        exit 0
    else
        echo -e "${YELLOW}Some services have encryption configuration issues. Review the report.${NC}"
        exit 1
    fi
}

main "$@"
