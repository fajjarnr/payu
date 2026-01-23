# Vault Integration Guide

This document describes the HashiCorp Vault integration for managing secrets in the PayU Digital Banking Platform.

## Overview

Vault provides secure secret management, allowing applications to retrieve sensitive configuration without hardcoding credentials in configuration files or environment variables.

## Architecture

- **Vault Server**: Stores and manages secrets securely
- **Spring Cloud Vault**: Enables Spring Boot applications to retrieve secrets from Vault
- **Docker Compose**: Runs Vault in development environment
- **Environment Fallback**: Applications can fallback to environment variables if Vault is unavailable

## Local Development

### Starting Vault

```bash
docker-compose up -d vault
```

### Initializing Vault

Run the initialization script to populate Vault with secrets:

```bash
./infrastructure/docker/init-vault.sh
```

This script creates the following secret paths:

- `secret/account-service/db` - Database credentials for account-service
- `secret/auth-service/db` - Database credentials for auth-service
- `secret/transaction-service/db` - Database credentials for transaction-service
- `secret/wallet-service/db` - Database credentials for wallet-service
- `secret/auth-service/keycloak` - Keycloak configuration
- `secret/common/kafka` - Kafka bootstrap servers
- `secret/gateway-service/redis` - Redis configuration
- `secret/common/grafana` - Grafana admin credentials

### Accessing Vault UI

Navigate to `http://localhost:8200/ui` to access the Vault web UI.

Use the token: `dev-only-token` (for development only)

## Configuration

### Spring Boot Integration

Applications are configured to read secrets from Vault via `application.yaml`:

```yaml
spring:
  config:
    import: vault://
  cloud:
    vault:
      enabled: ${VAULT_ENABLED:true}
      uri: ${VAULT_URI:http://localhost:8200}
      token: ${VAULT_TOKEN:dev-only-token}
      kv:
        enabled: true
        backend: secret
        application-name: <service-name>
```

### Disabling Vault (for Testing)

To disable Vault and use environment variables:

```bash
export VAULT_ENABLED=false
```

## Secret Naming Convention

Secrets follow the pattern: `secret/<service-name>/<category>`

Examples:
- `secret/account-service/db` - Database credentials
- `secret/auth-service/keycloak` - Keycloak integration
- `secret/common/kafka` - Shared services

## Adding New Secrets

1. Write the secret to Vault:

```bash
vault kv put secret/<service-name>/<category> \
    key1="value1" \
    key2="value2"
```

2. Update the initialization script (`infrastructure/docker/init-vault.sh`) with the new secret

3. Reference the secret in your application:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

Spring Cloud Vault will automatically inject these values from `secret/<service-name>/db`

## Production Considerations

For production deployment:

1. **Use Vault with TLS**: Enable HTTPS for secure communication
2. **Use AppRole Authentication**: Replace static tokens with AppRole
3. **Use Vault Auto-Authentication**: Configure Kubernetes authentication for OpenShift
4. **Rotate Secrets Regularly**: Implement secret rotation policies
5. **Audit Logging**: Enable audit logs for all secret access
6. **Backup Vault Data**: Regularly backup Vault storage

## OpenShift Deployment

For OpenShift deployment, configure Vault using:

- Vault Operator (recommended)
- External Vault service
- Kubernetes authentication method

See `infrastructure/openshift/` for OpenShift-specific configuration.

## Security Best Practices

1. Never commit Vault tokens to version control
2. Use short-lived tokens in production
3. Enable transit encryption for sensitive data
4. Implement secret versioning and rotation
5. Monitor Vault access logs
6. Use least-privilege access policies

## Troubleshooting

### Vault Connection Issues

Check Vault is running:
```bash
docker ps | grep vault
```

Check Vault status:
```bash
vault status
```

### Application Cannot Connect to Vault

Verify environment variables:
```bash
echo $VAULT_URI
echo $VAULT_TOKEN
```

Check application logs for Vault connection errors.

### Secrets Not Loading

Verify the secret path exists:
```bash
vault kv list secret/<service-name>
```

Check the secret structure:
```bash
vault kv get secret/<service-name>/<category>
```

## References

- [HashiCorp Vault Documentation](https://www.vaultproject.io/docs)
- [Spring Cloud Vault Documentation](https://cloud.spring.io/spring-cloud-vault/)
- [Vault Best Practices](https://www.vaultproject.io/docs/best-practices/best-practices)
