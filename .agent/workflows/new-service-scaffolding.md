---
description: Workflow untuk scaffolding microservice baru di platform PayU dengan arsitektur Hexagonal dan konfigurasi standar.
---

// turbo-all

# New Service Scaffolding Workflow

Gunakan workflow ini saat membuat microservice baru di PayU platform.

## Pre-requisites

- Nama service sudah disetujui (e.g., `reward-service`)
- Domain scope sudah jelas (termasuk dalam domain mana)
- ADR sudah dibuat jika ini adalah keputusan arsitektur besar

## Phase 1: Context & Validation

1. **Load Context**
   - Baca `docs/architecture/ARCHITECTURE.md` untuk memahami landscape service.
   - Baca `@payu-development` skill untuk tech stack dan konvensi.

2. **Define Service Type**
   | Type | Framework | Use Case |
   | :--- | :--- | :--- |
   | **Core Banking** | Spring Boot 3.4 | Account, Transaction, Wallet |
   | **Supporting** | Quarkus Native | Gateway, Notification, Billing |
   | **Analytics/ML** | Python FastAPI | KYC, Fraud Scoring |

## Phase 2: Scaffold Generation

1. **Spring Boot (Core Banking)**

   ```bash
   mkdir -p backend/<service-name>
   cd backend/<service-name>
   # Use Spring Initializr or copy from template
   ```

2. **Directory Structure (Hexagonal)**

   ```
   src/main/java/id/payu/<service>/
   ├── domain/              # Entities, Value Objects, Repository Ports
   │   ├── model/
   │   └── port/
   ├── application/         # Use Cases, Command/Query Handlers
   │   ├── command/
   │   └── query/
   ├── infrastructure/      # JPA, Kafka, External Clients
   │   ├── persistence/
   │   └── messaging/
   └── interfaces/          # REST Controllers, DTOs
       ├── rest/
       └── dto/
   ```

3. **Essential Files Checklist**
   - [ ] `pom.xml` with shared starters: `security-starter`, `resilience-starter`, `cache-starter`
   - [ ] `application.yml` with environment placeholders (NO hardcoded secrets!)
   - [ ] `Dockerfile` using UBI-9 multi-stage build
   - [ ] `ArchitectureTest.java` for layering enforcement
   - [ ] `db/migration/V1__init_schema.sql` (Flyway)

## Phase 3: Integration

1. **Register in API Gateway**
   - Add route to `gateway-service` configuration.

2. **Add to Observability Stack**
   - Ensure `management.endpoints.web.exposure.include` is set.
   - Add Prometheus scrape target.

3. **Add to CI/CD**
   - Create Tekton PipelineRun or add to mono-repo pipeline.
   - Create ArgoCD Application manifest.

## Phase 4: Verification

// turbo

1. **Run ArchUnit Test**

   ```bash
   mvn test -Dtest=*Arch*
   ```

2. **Verify Startup**
   ```bash
   mvn spring-boot:run
   # Check /actuator/health
   ```

---

_Last Updated: January 2026_
