# Infrastructure As Code (IaC)

## Overview

Guidelines for managing PayU infrastructure using Terraform and Helm.

## Best Practices

### 1. Terraform for Platform
- Use Terraform for managing OpenShift Projects, Service Accounts, and RBAC Roles.
- **State Management**: Use remote state in S3/GCS or OpenShift Secret backend.

### 2. Helm for Applications
- Use Helm Charts for microservice manifests.
- Standardize charts in `infrastructure/charts/payu-service/`.

## Anti-Patterns
- **Hardcoding Values**: Never hardcode environment-specific values. Use `values.yaml` and Helm secrets.

---
*Last Updated: January 2026*
