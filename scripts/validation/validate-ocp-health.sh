#!/bin/bash
# ============================================
# PayU OpenShift Cluster Services Health Check
# Validates all deployed microservices are healthy
# ============================================

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "PayU OpenShift Cluster Health Check"
echo "=========================================="

# Find a healthy microservice pod to use as our curl gateway
GATEWAY_POD=$(oc get pods -n payu-dev -l app.kubernetes.io/part-of=payu -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)

if [ -z "$GATEWAY_POD" ]; then
    echo -e "${RED}Error: No microservice pods found in payu-dev namespace to run health check queries.${NC}"
    exit 1
fi

echo -e "Using pod ${BLUE}$GATEWAY_POD${NC} as curl executor."
echo ""

# Declare array of services to check: "name|type|port|path"
SERVICES=(
    "account-service|Spring|8080|/actuator/health/liveness"
    "auth-service|Spring|8080|/actuator/health/liveness"
    "transaction-service|Spring|8080|/actuator/health/liveness"
    "wallet-service|Spring|8080|/actuator/health/liveness"
    "billing-service|Spring|8080|/actuator/health/liveness"
    "notification-service|Quarkus|8080|/q/health"
    "kyc-service|FastAPI|8080|/health"
    "analytics-service|FastAPI|8080|/health"
    "investment-service|Spring|8080|/actuator/health/liveness"
    "lending-service|Spring|8080|/actuator/health/liveness"
    "backoffice-service|Spring|8080|/actuator/health/liveness"
    "partner-service|Spring|8080|/actuator/health/liveness"
    "promotion-service|Spring|8080|/actuator/health/liveness"
    "support-service|Spring|8080|/actuator/health/liveness"
    "statement-service|Spring|8080|/actuator/health/liveness"
    "api-portal-service|Quarkus|8080|/q/health"
    "gateway-service|Quarkus|8080|/q/health"
    "compliance-service|Spring|8080|/actuator/health/liveness"
    "cms-service|Spring|8080|/actuator/health/liveness"
    "fx-service|Spring|8080|/actuator/health/liveness"
    "dispute-service|Spring|8080|/actuator/health/liveness"
    "product-catalog-service|Spring|8080|/actuator/health/liveness"
    "integration-service|Spring|8080|/actuator/health/liveness"
)

UNHEALTHY=()

for item in "${SERVICES[@]}"; do
    IFS='|' read -r name type port path <<< "$item"
    echo -n -e "Checking ${BLUE}$name${NC} ($type)... "
    
    # Run curl inside the gateway pod
    HTTP_CODE=$(oc exec "$GATEWAY_POD" -n payu-dev -- curl -s -o /dev/null -w "%{http_code}" "http://$name:$port$path" 2>/dev/null || echo "000")
    
    if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "204" ]; then
        echo -e "${GREEN}HEALTHY (HTTP $HTTP_CODE)${NC}"
    else
        echo -e "${RED}UNHEALTHY (HTTP $HTTP_CODE)${NC}"
        UNHEALTHY+=("$name")
    fi
done

echo ""
echo "=========================================="
if [ ${#UNHEALTHY[@]} -eq 0 ]; then
    echo -e "${GREEN}✓ All 23 microservices are healthy on the OpenShift cluster!${NC}"
    exit 0
else
    echo -e "${RED}✗ ${#UNHEALTHY[@]} services are unhealthy:${NC}"
    for svc in "${UNHEALTHY[@]}"; do
        echo -e "  - ${YELLOW}$svc${NC}"
    done
    exit 1
fi
