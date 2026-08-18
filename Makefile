# PayU Digital Banking - Makefile for Testing
# Provides convenient targets for running tests

.PHONY: help test test-all test-unit test-integration test-e2e test-coverage \
        test-local test-podman test-backend test-frontend test-mobile \
        test-account test-auth test-transaction test-wallet \
        clean clean-test build-test-deps \
        seed-test-data cleanup-test-db \
        test-health-check podman-test-up podman-test-down \
        codegraph-refresh

# Default target
.DEFAULT_GOAL := help

# ============================================
# Help Target
# ============================================
help: ## Show this help message
	@echo "PayU Digital Banking - Test Targets"
	@echo ""
	@echo "Test Execution:"
	@echo "  make test              - Run all tests (unit, integration, E2E)"
	@echo "  make test-all          - Run all tests (alias for 'make test')"
	@echo "  make test-unit         - Run unit tests only"
	@echo "  make test-integration  - Run integration tests only"
	@echo "  make test-e2e          - Run E2E tests only"
	@echo "  make test-coverage     - Generate coverage reports only"
	@echo ""
	@echo "Component-Specific Tests:"
	@echo "  make test-backend      - Run all backend tests only"
	@echo "  make test-frontend     - Run frontend tests only"
	@echo "  make test-mobile       - Run mobile tests only"
	@echo ""
	@echo "Test Environments:"
	@echo "  make test-local        - Run tests in local environment"
	@echo "  make test-podman       - Run tests in Podman test environment"
	@echo ""
	@echo "Service-Specific Tests:"
	@echo "  make test-account      - Test account-service"
	@echo "  make test-auth         - Test auth-service"
	@echo "  make test-transaction  - Test transaction-service"
	@echo "  make test-wallet       - Test wallet-service"
	@echo "  make test-billing      - Test billing-service"
	@echo "  make test-notification - Test notification-service"
	@echo "  make test-gateway      - Test gateway-service"
	@echo "  make test-kyc          - Test kyc-service"
	@echo "  make test-analytics    - Test analytics-service"
	@echo ""
	@echo "Test Infrastructure:"
	@echo "  make test-health-check      - Check test environment health"
	@echo "  make podman-test-up         - Start Podman test environment"
	@echo "  make podman-test-down       - Stop Podman test environment"
	@echo "  make seed-test-data         - Seed test databases"
	@echo "  make cleanup-test-db        - Reset test databases"
	@echo ""
	@echo "Build & Setup:"
	@echo "  make build-test-deps   - Install shared dependencies"
	@echo "  make clean-test        - Clean test artifacts"
	@echo "  make clean             - Clean all artifacts"
	@echo ""

# ============================================
# Main Test Targets
# ============================================

test: ## Run all tests
	@./scripts/run-all-tests.sh

test-all: ## Run all tests (alias)
	@./scripts/run-all-tests.sh

test-unit: ## Run unit tests only
	@./scripts/run-all-tests.sh --skip-integration --skip-e2e

test-integration: ## Run integration tests only
	@./scripts/run-all-tests.sh --skip-unit --skip-e2e --skip-frontend --skip-mobile

test-e2e: ## Run E2E tests only
	@./scripts/run-all-tests.sh --skip-unit --skip-integration --skip-backend

test-coverage: ## Generate coverage reports
	@./scripts/run-all-tests.sh --coverage

test-local: ## Run tests in local environment
	@./scripts/run-all-tests.sh --skip-build

test-podman: ## Run tests in Podman test environment
	@$(MAKE) podman-test-up
	@sleep 10
	@./scripts/run-all-tests.sh
	@$(MAKE) podman-test-down

test-backend: ## Run all backend tests only
	@./scripts/run-all-tests.sh --skip-frontend --skip-mobile

test-frontend: ## Run frontend tests only
	@./scripts/run-all-tests.sh --skip-backend --skip-mobile --skip-integration --skip-e2e

test-mobile: ## Run mobile tests only
	@./scripts/run-all-tests.sh --skip-backend --skip-frontend --skip-integration --skip-e2e

# ============================================
# Backend Service Tests
# ============================================

test-account: ## Test account-service
	@./scripts/test-single-service.sh account-service

test-auth: ## Test auth-service
	@./scripts/test-single-service.sh auth-service

test-transaction: ## Test transaction-service
	@./scripts/test-single-service.sh transaction-service

test-wallet: ## Test wallet-service
	@./scripts/test-single-service.sh wallet-service

test-billing: ## Test billing-service
	@./scripts/test-single-service.sh billing-service

test-notification: ## Test notification-service
	@./scripts/test-single-service.sh notification-service

test-gateway: ## Test gateway-service
	@./scripts/test-single-service.sh gateway-service

test-kyc: ## Test kyc-service
	@./scripts/test-single-service.sh kyc-service

test-analytics: ## Test analytics-service
	@./scripts/test-single-service.sh analytics-service

# ============================================
# Test Infrastructure
# ============================================

test-health-check: ## Check test environment health
	@./scripts/test-health-check.sh

podman-test-up: ## Start Podman test environment
	@podman compose -f infrastructure/local-podman/podman-compose.test.yml up -d
	@echo "Waiting for services to be healthy..."
	@sleep 15
	@./scripts/test-health-check.sh

podman-test-down: ## Stop Podman test environment
	@podman compose -f infrastructure/local-podman/podman-compose.test.yml down -v

seed-test-data: ## Seed test databases with test data
	@./scripts/seed-test-data.sh

cleanup-test-db: ## Reset test databases
	@./scripts/cleanup-test-db.sh

# ============================================
# Build & Setup
# ============================================

build-test-deps: ## Install shared dependencies
	@echo "Installing shared libraries..."
	@cd backend/shared/api-commons && mvn clean install -DskipTests -q
	@cd backend/shared/cache-starter && mvn clean install -DskipTests -q
	@cd backend/shared/resilience-starter && mvn clean install -DskipTests -q
	@cd backend/shared/security-starter && mvn clean install -DskipTests -q
	@cd backend/shared/outbox-starter && mvn clean install -DskipTests -q
	@cd backend/shared/saga-starter && mvn clean install -DskipTests -q
	@cd backend/shared/events-starter && mvn clean install -DskipTests -q
	@cd backend/shared/archunit-starter && mvn clean install -DskipTests -q
	@echo "Shared dependencies installed"

# ============================================
# CodeGraph (DX-CODEGRAPH-001)
# ============================================

codegraph-refresh: ## Re-sync / rebuild the CodeGraph index
	@./scripts/refresh-codegraph.sh

# ============================================
# Cleanup
# ============================================

clean-test: ## Clean test artifacts
	@echo "Cleaning test artifacts..."
	@find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name ".pytest_cache" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name "htmlcov" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name ".next" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name "node_modules/.cache" -exec rm -rf {} + 2>/dev/null || true
	@find . -type f -name "jacoco.exec" -delete 2>/dev/null || true
	@echo "Test artifacts cleaned"

clean: ## Clean all artifacts
	@$(MAKE) clean-test
	@echo "Cleaning build artifacts..."
	@podman compose -f infrastructure/local-podman/podman-compose.test.yml down -v 2>/dev/null || true
	@echo "All artifacts cleaned"
