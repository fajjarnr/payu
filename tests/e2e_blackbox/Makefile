.PHONY: help install test test-all test-smoke test-critical test-investment test-lending test-promotion \
	test-compliance test-support test-partner test-analytics test-backoffice clean lint format

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-20s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

install: ## Install test dependencies
	pip install -r requirements.txt
	pip install pytest-timeout

test: ## Run all E2E tests
	pytest -v

test-all: test ## Alias for test

test-smoke: ## Run smoke tests only
	pytest -v -m smoke

test-critical: ## Run critical tests only
	pytest -v -m critical

test-integration: ## Run integration tests only
	pytest -v -m integration

test-e2e: ## Run E2E tests only
	pytest -v -m e2e

test-investment: ## Run investment flow tests
	pytest -v test_investment_flow.py

test-lending: ## Run lending flow tests
	pytest -v test_lending_flow.py

test-promotion: ## Run promotion flow tests
	pytest -v test_promotion_flow.py

test-compliance: ## Run compliance flow tests
	pytest -v test_compliance_flow.py

test-support: ## Run support flow tests
	pytest -v test_support_flow.py

test-partner: ## Run partner flow tests
	pytest -v test_partner_flow.py

test-analytics: ## Run analytics flow tests
	pytest -v test_analytics_flow.py

test-backoffice: ## Run backoffice flow tests
	pytest -v test_backoffice.py

test-journey: ## Run complete user journey tests
	pytest -v test_complete_user_journey.py

test-with-coverage: ## Run tests with coverage
	pytest -v --cov=. --cov-report=html --cov-report=term

test-verbose: ## Run tests with verbose output
	pytest -v -s

test-stop-on-fail: ## Run tests stopping on first failure
	pytest -v -x

clean: ## Clean test artifacts
	find . -type d -name "__pycache__" -exec rm -rf {} +
	find . -type d -name ".pytest_cache" -exec rm -rf {} +
	find . -type d -name "htmlcov" -exec rm -rf {} +
	find . -type f -name ".coverage" -exec rm -f {} +

lint: ## Run linting on test files
	python -m pylint *.py
	python -m flake8 *.py --max-line-length=120

format: ## Format test files with black
	black *.py
