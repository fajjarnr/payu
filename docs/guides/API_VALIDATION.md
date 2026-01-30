# API Validation with Spectral

This guide explains how to use Spectral for validating OpenAPI specifications in the PayU platform.

## Table of Contents

- [Overview](#overview)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Spectral Ruleset](#spectral-ruleset)
- [Validation Script](#validation-script)
- [CI/CD Integration](#cicd-integration)
- [Rule Reference](#rule-reference)
- [Troubleshooting](#troubleshooting)

## Overview

[Spectral](https://stoplight.io/open-source/spectral) is a powerful OpenAPI linter that helps ensure API specifications follow consistent standards and best practices. PayU uses Spectral to enforce:

- **Response Envelope Standards** - All responses must follow the PayU API envelope format
- **Idempotency Requirements** - POST/PUT operations must support idempotency keys
- **URL Path Conventions** - Kebab-case paths with versioning
- **Documentation Standards** - Complete OpenAPI documentation
- **Security Requirements** - Authentication and authorization patterns

## Installation

### Prerequisites

- Node.js 18+ or 20+
- npm or yarn

### Install Spectral CLI

```bash
# Using npm
npm install -g @stoplight/spectral-cli

# Using yarn
yarn global add @stoplight/spectral-cli

# Or use the provided script
./scripts/validate-api.sh --install
```

### Verify Installation

```bash
spectral --version
```

## Quick Start

### Validate a Single File

```bash
# Basic validation
spectral lint docs/openapi/account-api.yaml --ruleset .spectral.yaml

# Using the validation script
./scripts/validate-api.sh docs/openapi/account-api.yaml
```

### Validate All OpenAPI Files

```bash
# The script will auto-discover OpenAPI files
./scripts/validate-api.sh

# With verbose output
./scripts/validate-api.sh -v

# Save reports to a directory
./scripts/validate-api.sh -o reports/
```

### Validate with Different Output Formats

```bash
# JSON format
./scripts/validate-api.sh -f json docs/openapi/*.yaml

# HTML report
./scripts/validate-api.sh -f html -o reports/ docs/openapi/*.yaml

# JUnit format (for CI/CD)
./scripts/validate-api.sh -f junit -o reports/ docs/openapi/*.yaml
```

## Spectral Ruleset

The PayU Spectral ruleset is defined in `.spectral.yaml` at the project root.

### Rule Categories

#### 1. Response Envelope Rules

Ensures all API responses follow the PayU standard envelope format:

```yaml
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": {
    "timestamp": "2026-01-30T10:00:00Z",
    "requestId": "req-123",
    "pagination": { ... }
  }
}
```

**Rules:**
- `response-envelope-required` - All 200/201 responses must use ApiResponse
- `response-envelope-error-structure` - Error responses must follow PayU format

#### 2. Idempotency Rules

Ensures mutating operations support idempotent execution:

**Rules:**
- `idempotency-header-required` - POST/PUT/PATCH must support `Idempotency-Key` header
- `idempotency-header-description` - Header must have proper description
- `idempotency-header-format` - Header should be UUID format

#### 3. URL Path Rules

Enforces consistent URL naming conventions:

**Rules:**
- `path-kebab-case` - Path segments must use kebab-case (e.g., `/user-accounts`)
- `path-no-trailing-slash` - Paths must not have trailing slashes
- `path-version-prefix` - API paths should include version prefix (e.g., `/v1/`)

#### 4. Documentation Rules

Ensures comprehensive API documentation:

**Rules:**
- `operation-summary-required` - All operations must have a summary
- `operation-summary-length` - Summary should be 10-120 characters
- `operation-description-required` - All operations should have a description
- `operation-operationId-required` - All operations must have operationId
- `operation-operationId-camelCase` - operationId must be camelCase
- `operation-tags-required` - All operations must have at least one tag

#### 5. Security Rules

Enforces security requirements:

**Rules:**
- `security-scheme-required` - API must define security schemes
- `operation-security-required` - Operations must have security (except health/auth)
- `correlation-id-header` - All operations should accept `X-Request-ID`

#### 6. Pagination Rules

Ensures list endpoints support pagination:

**Rules:**
- `pagination-params-for-list` - List endpoints must support pagination
- `array-response-pagination` - Array responses must include pagination metadata

### Rule Severity Levels

| Level | Description | Action |
|-------|-------------|--------|
| `error` | Critical violation | Build fails |
| `warn` | Should fix | Build passes with warning |
| `info` | Informational | No action required |
| `hint` | Suggestion | No action required |

### Rule Overrides

Certain files have relaxed rules:

```yaml
overrides:
  - files:
      - "**/health*.yaml"
    rules:
      operation-security-required: "off"
      response-envelope-required: "off"
```

## Validation Script

The `scripts/validate-api.sh` script provides a convenient wrapper around Spectral.

### Usage

```bash
./scripts/validate-api.sh [OPTIONS] [FILES...]
```

### Options

| Option | Description |
|--------|-------------|
| `-h, --help` | Show help message |
| `-r, --ruleset FILE` | Use custom ruleset |
| `-f, --format FORMAT` | Output format (stylish, json, html, junit) |
| `-s, --severity LEVEL` | Fail on severity (error, warn, info, hint) |
| `-o, --output DIR` | Save reports to directory |
| `-v, --verbose` | Enable verbose output |
| `-w, --watch` | Watch mode - revalidate on changes |
| `--fix` | Attempt to auto-fix violations |
| `-p, --parallel` | Run validations in parallel |
| `-j, --jobs N` | Maximum parallel jobs |
| `--install` | Install/Update Spectral CLI |
| `--version` | Show script version |

### Examples

#### Watch Mode (Development)

```bash
# Automatically revalidate when files change
./scripts/validate-api.sh -w docs/openapi/account-api.yaml
```

#### Parallel Validation

```bash
# Validate multiple files in parallel
./scripts/validate-api.sh -p -j 8 docs/openapi/*.yaml
```

#### Generate HTML Report

```bash
# Create a nice HTML report for sharing
./scripts/validate-api.sh -f html -o reports/ docs/openapi/*.yaml
```

#### Custom Ruleset

```bash
# Use a custom ruleset for specific validation
./scripts/validate-api.sh -r .spectral-strict.yaml docs/openapi/*.yaml
```

## CI/CD Integration

### GitHub Actions

The repository includes a GitHub Actions workflow at `.github/workflows/api-validation.yaml`.

#### Features

- **Auto-discovery** - Automatically finds OpenAPI files
- **Parallel validation** - Validates multiple files concurrently
- **Artifact uploads** - Saves reports as build artifacts
- **PR comments** - Posts validation results as PR comments
- **Breaking change detection** - Compares specs between branches

#### Workflow Triggers

The workflow runs on:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Only when OpenAPI-related files change

#### Manual Trigger

```bash
# Trigger via GitHub CLI
gh workflow run api-validation.yaml
```

### GitLab CI

Example `.gitlab-ci.yml`:

```yaml
api-validation:
  stage: test
  image: node:20-alpine
  before_script:
    - npm install -g @stoplight/spectral-cli
  script:
    - ./scripts/validate-api.sh -f junit -o reports/
  artifacts:
    reports:
      junit: reports/*.xml
    paths:
      - reports/
```

### Jenkins

Example `Jenkinsfile`:

```groovy
pipeline {
    agent any
    stages {
        stage('API Validation') {
            steps {
                sh 'npm install -g @stoplight/spectral-cli'
                sh './scripts/validate-api.sh -f junit -o reports/'
            }
        }
    }
    post {
        always {
            junit 'reports/*.xml'
        }
    }
}
```

## Rule Reference

### Response Envelope

#### response-envelope-required

All 200/201 responses must use the PayU ApiResponse envelope.

**Valid:**
```yaml
responses:
  200:
    content:
      application/json:
        schema:
          type: object
          required: [success]
          properties:
            success:
              type: boolean
            data:
              type: object
            error:
              type: object
            meta:
              type: object
```

**Invalid:**
```yaml
responses:
  200:
    content:
      application/json:
        schema:
          type: object
          properties:
            id:  # Missing success field
              type: string
```

### Idempotency

#### idempotency-header-required

POST/PUT/PATCH operations must support `Idempotency-Key` header.

**Valid:**
```yaml
post:
  parameters:
    - name: Idempotency-Key
      in: header
      required: true
      schema:
        type: string
        format: uuid
```

### URL Paths

#### path-kebab-case

Path segments must use kebab-case.

**Valid:**
- `/user-accounts`
- `/transaction-history`
- `/v1/bank-accounts`

**Invalid:**
- `/userAccounts` (camelCase)
- `/user_accounts` (snake_case)
- `/UserAccounts` (PascalCase)

### Documentation

#### operation-operationId-camelCase

operationId must be in camelCase.

**Valid:**
- `getUserAccount`
- `createTransaction`
- `updateBankAccount`

**Invalid:**
- `get-user-account` (kebab-case)
- `get_user_account` (snake_case)
- `GetUserAccount` (PascalCase)

## Troubleshooting

### Common Issues

#### "Spectral CLI is not installed"

```bash
# Install Spectral
npm install -g @stoplight/spectral-cli

# Or use the script
./scripts/validate-api.sh --install
```

#### "Ruleset not found"

Ensure `.spectral.yaml` exists in the project root:

```bash
ls -la .spectral.yaml
```

#### "No OpenAPI files found"

Check that your OpenAPI files follow naming conventions:
- `openapi*.yaml`
- `*api*.yaml`
- `swagger*.yaml`
- `*spec*.yaml`

Or specify files explicitly:

```bash
./scripts/validate-api.sh path/to/your-api.yaml
```

#### Too many violations

Adjust the fail severity:

```bash
# Only fail on errors (ignore warnings)
./scripts/validate-api.sh -s error docs/openapi/*.yaml

# Show all issues but don't fail
./scripts/validate-api.sh -s hint docs/openapi/*.yaml
```

### Disable Rules Temporarily

For specific files:

```yaml
# In your OpenAPI file
x-spectral-disable:
  - operation-summary-required
  - path-kebab-case
```

Or in the ruleset:

```yaml
overrides:
  - files:
      - "**/legacy-api.yaml"
    rules:
      path-kebab-case: "off"
```

### Debug Mode

Enable verbose output:

```bash
./scripts/validate-api.sh -v docs/openapi/*.yaml
```

Run Spectral directly with debug:

```bash
DEBUG=spectral* spectral lint docs/openapi/*.yaml --ruleset .spectral.yaml
```

## Best Practices

1. **Validate Early** - Run validation before committing changes
2. **Pre-commit Hook** - Add to git pre-commit hooks
3. **CI/CD Gate** - Make validation a required check
4. **Team Standards** - Document any rule exceptions
5. **Regular Updates** - Keep Spectral CLI updated
6. **Custom Rules** - Extend ruleset for domain-specific needs

## Additional Resources

- [Spectral Documentation](https://docs.stoplight.io/docs/spectral/)
- [OpenAPI Specification](https://spec.openapis.org/)
- [PayU API Guidelines](./API_GUIDELINES.md)
- [Spectral Functions Reference](https://docs.stoplight.io/docs/spectral/a781e290eb9f9-custom-functions)

## Contributing

To add new rules or modify existing ones:

1. Edit `.spectral.yaml`
2. Test with `./scripts/validate-api.sh -v`
3. Update this documentation
4. Submit a PR with examples

---

For questions or support, contact the PayU API Team.
