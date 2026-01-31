# Phase 2: Backend Standardization (Security Focus)

## 🎯 Objective
Standardize security implementation across remaining services:
- **backoffice-service** (Spring Boot 3.4)
- **partner-service** (Spring Boot 3.4)
- Verification of OpenAPI docs (just to be safe)

## 🔄 Execution Plan

### 1. Security Implementation (High Priority)
- `@cybersecurity-architect`: Add `spring-boot-starter-security` and `oauth2-resource-server` dependencies.
- `@cybersecurity-architect`: Create `SecurityConfig.java` enforcing:
    - JWT Authentication (Stateless)
    - RBAC (Role-Based Access Control)
    - CSRF disabled (API mode)
    - CORS configuration
- `@tester`: Add security tests (verifying 401/403 on secured endpoints).

### 2. Dependency Standardization
- Ensure these services inherit from `payu-backend-parent`.
- Remove manual version declarations for security libs.

### 3. OpenAPI Verification
- Confirm Swagger UI endpoints are accessible/configured.
