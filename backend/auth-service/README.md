# Auth Service

> Identity & Access Management Service acting as a bridge to Red Hat SSO (Keycloak).

## Overview
The Auth Service handles:
- Login Proxy (Resource Owner Password Credentials Grant / Standard Flow support)
- User Registration syncing with Keycloak (Admin API)
- Token Management

## Tech Stack
- **Framework**: Spring Boot 3.4.1
- **Language**: Java 21
- **IAM**: Red Hat SSO 7.x / Keycloak 24+
- **API Client**: Keycloak Admin Client

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/callback` | OIDC authorization-code + PKCE exchange (LOGIN-003) |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/device` | Initiate RFC 8628 device authorization |
| POST | `/api/v1/auth/device/token` | Poll device token (returns 202 while pending) |
| POST | `/api/v1/auth/register` | Register user in IAM |
| DELETE | `/api/v1/auth/users/{userId}` | Delete user in IAM (saga compensation) |
| GET | `/api/v1/auth/validate` | Validate session |
| POST | `/api/v1/auth/logout` | Logout and revoke session (LOGIN-002) |

## Configuration
| Environment Variable | Default | Description |
|----------------------|---------|-------------|
| `KEYCLOAK_URL` | `http://localhost:8180` | Keycloak Base URL |
| `KEYCLOAK_REALM` | `payu` | Keycloak Realm |
| `KEYCLOAK_CLIENT_ID` | `auth-service` | Client ID for Admin operations |
| `KEYCLOAK_CLIENT_SECRET` | `secret` | Client Secret |

## Dependencies
Requires a running Keycloak instance.

```bash
docker run -d --name keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:24.0.0 \
  start-dev
```

## Running Locally

```bash
./mvnw spring-boot:run
```
