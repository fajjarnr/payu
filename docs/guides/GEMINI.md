# GEMINI.md - PayU Digital Banking Platform

> AI Assistant Guidelines & Project Context for Gemini/Claude

---

## 📋 Project Overview

**PayU** adalah platform digital banking standalone yang dibangun dengan arsitektur microservices di atas **Red Hat OpenShift 4.20+** ecosystem. Platform ini dirancang sebagai payment infrastructure yang dapat digunakan oleh multiple projects.

### Quick Facts

| Attribute | Value |
|-----------|-------|
| **Project Name** | PayU |
| **Type** | Standalone Digital Banking Platform |
| **Architecture** | Microservices + Event-Driven |
| **Platform** | Red Hat OpenShift 4.20+ |
| **Primary Languages** | Java 21, Python 3.12 |
| **Last Updated** | January 2026 |

## ⚡ Quick Commands (for AI Agents)

| Action | Command |
|--------|---------|
| **Build All** | `mvn clean package -DskipTests -T 1C` |
| **Run Tests** | `mvn test` |
| **Local Dev** | `docker-compose up -d` |
| **Check Logs** | `docker-compose logs -f [service-name]` |
| **Infrastructure** | `oc get pods` |

---

## 🏗️ Architecture Overview

### Technology Stack

| Layer | Red Hat Product | Portable Alternative |
|-------|-----------------|----------------------|
| **Container Platform** | Red Hat OpenShift 4.20+ | Kubernetes |
| **Core Banking** | Red Hat Runtimes (Spring Boot 3.4) | Spring Boot |
| **Supporting Services** | Red Hat Build of Quarkus 3.x | Quarkus |
| **ML Services** | Python 3.12 (UBI-based) | FastAPI |
| **Database** | Crunchy PostgreSQL 16 | Any PostgreSQL |
| **Caching** | Red Hat Data Grid (RESP mode) | Redis, ElastiCache |
| **Event Streaming** | AMQ Streams (Kafka) | Apache Kafka |
| **Message Queue** | AMQ Broker (AMQP 1.0) | ActiveMQ Artemis |
| **Identity** | Red Hat SSO (Keycloak) | Keycloak, Auth0 |
| **Logging** | OpenShift Logging (LokiStack) | Grafana Loki |
| **Monitoring** | OpenShift Monitoring | Prometheus/Grafana |
| **Tracing** | OpenShift Distributed Tracing | Jaeger |
| **CI/CD** | OpenShift Pipelines + GitOps | Tekton + ArgoCD |

> **Portability Note**: All components use standard APIs (OIDC, RESP, Kafka Protocol, SQL, AMQP).
> Code remains portable - only configuration changes needed to switch providers.

### Microservices

| Service | Technology | Domain |
|---------|------------|--------|
| `account-service` | Spring Boot 3.4 | User accounts, eKYC, multi-pocket |
| `auth-service` | Spring Boot 3.4 + Red Hat SSO | Authentication, MFA, OAuth2 |
| `transaction-service` | Spring Boot 3.4 | Transfers, BI-FAST, QRIS |
| `wallet-service` | Spring Boot 3.4 | Balance management, ledger |
| `billing-service` | Quarkus 3.x Native | Bill payments, top-ups |
| `notification-service` | Quarkus 3.x Native | Push, SMS, Email |
| `gateway-service` | Quarkus 3.x Native | API Gateway |
| `compliance-service` | Spring Boot 3.4 | Regulatory audits |
| `support-service` | Quarkus 3.x Native | Support training |
| `kyc-service` | Python FastAPI | OCR, liveness detection ML |
| `analytics-service` | Python FastAPI | User insights, ML |

---

## 📁 Project Structure

```
payu/
├── .agent/              # AI agent skills and workflows
├── docs/                # Project documentation
│   ├── architecture/    # ARCHITECTURE.md
│   ├── product/         # PRD.md
│   ├── operations/      # DISASTER_RECOVERY.md
│   ├── security/        # PENTEST_REPORT.md
│   ├── guides/          # GEMINI.md, CONTRIBUTING.md
│   └── roadmap/         # TODOS.md
├── CHANGELOG.md         # Version history
├── README.md            # Project overview
├── docker-compose.yml   # Local development infrastructure
├── backend/             # Microservices implementation
└── infrastructure/      # Kubernetes/OpenShift manifests
```

---

## 🛠️ Development Guidelines

### Spring Boot Services (Core Banking)

**Technology Stack:**
- **Runtime:** Java 21
- **Framework:** Spring Boot 3.4.x
- **Messaging:** Spring Kafka (AMQ Streams)
- **Cache:** Spring Data Redis (Data Grid RESP mode)
- **Database:** Spring Data JPA + PostgreSQL

**Commands:**
```bash
cd backend/account-service
mvn spring-boot:run     # Development
mvn clean package       # Build
mvn test                # Run tests (Unit + Architecture)
mvn test jacoco:report  # Run tests with coverage
```

**Testing Stack:**
- **JUnit 5 + Mockito** - Unit testing
- **Testcontainers** - Integration testing (PostgreSQL, Kafka)
- **ArchUnit** - Architecture rule enforcement
- **JaCoCo** - Code coverage reporting

### Quarkus Services (Supporting)

**Technology Stack:**
- **Runtime:** Java 21 (GraalVM Native)
- **Framework:** Quarkus 3.x
- **Messaging:** SmallRye Reactive Messaging (AMQ)
- **Cache:** Quarkus Redis (Data Grid RESP mode)
- **Database:** Quarkus Hibernate ORM + PostgreSQL

**Commands:**
```bash
cd backend/billing-service
./mvnw quarkus:dev                    # Development (hot reload)
./mvnw package -Pnative               # Native build
./mvnw package -Dquarkus.container-image.build=true  # Container build
```

### Python Services (ML/Data)

**Technology Stack:**
- **Runtime:** Python 3.12 (UBI-based)
- **Framework:** FastAPI
- **ML:** PyTorch, scikit-learn
- **Database:** asyncpg + PostgreSQL

**Commands:**
```bash
cd backend/kyc-service
pip install -r requirements.txt
uvicorn app.main:app --reload   # Development
pytest                          # Run tests
```

---

## 📚 Context7 Integration

Untuk mendapatkan dokumentasi terbaru, gunakan Context7 MCP:

### Recommended Libraries

| Library | Context7 ID | Use Case |
|---------|-------------|----------|
| **Spring Boot** | `/spring-projects/spring-boot` | Core banking services |
| **Quarkus** | `/quarkusio/quarkus` | Supporting services |
| **FastAPI** | `/tiangolo/fastapi` | ML services |
| **Kafka** | `/apache/kafka` | Event streaming |
| **PostgreSQL** | `/postgres/postgres` | Database |

### How to Query

```
# Untuk Spring Boot patterns
Context7 Library: /spring-projects/spring-boot
Query: "Spring Data JPA with PostgreSQL JSONB"

# Untuk Quarkus patterns
Context7 Library: /quarkusio/quarkus
Query: "Quarkus native image with Redis client"
```

---

## 🎯 AI Assistant Instructions

### When Working on Core Banking (Spring Boot)

1. **Use Spring Boot 3.4.x** - Latest version with Java 21
2. **Spring Data JPA** - For PostgreSQL access
3. **Spring Data Redis** - For Data Grid access (RESP mode)
4. **Spring Kafka** - For AMQ Streams
5. **Axon Framework** - For CQRS/Event Sourcing (optional)

### When Working on Supporting Services (Quarkus)

1. **Use Quarkus 3.x** - Target native compilation
2. **Prefer reactive** - Use Mutiny for async operations
3. **SmallRye extensions** - For messaging and health
4. **Quarkus Redis** - Use RESP protocol for Data Grid compatibility

### When Working on ML Services (Python)

1. **Use FastAPI** - Async by default
2. **asyncpg** - For PostgreSQL async access
3. **PyTorch/scikit-learn** - For ML models
4. **UBI base image** - For OpenShift compatibility

### Database Guidelines

1. **PostgreSQL JSONB** - For document-like data (instead of MongoDB)
2. **Standard SQL** - Avoid vendor-specific extensions
3. **Flyway/Liquibase** - For schema migrations
4. **Connection pooling** - Use HikariCP (Spring) or Agroal (Quarkus)

### Cache Guidelines

1. **Use Redis API** - Standard `spring-data-redis` or `quarkus-redis-client`
2. **RESP protocol** - For Data Grid compatibility
3. **Portable code** - Switch providers via config only

### Code Style

- **Naming:** camelCase untuk variables/functions, PascalCase untuk classes
- **File naming:** kebab-case untuk files
- **Packages:** `id.payu.<service>.<layer>`
- **Error Handling:** Always handle errors with proper logging

### Testing Guidelines (TDD)

1. **Write tests first** - Red-Green-Refactor cycle
2. **Unit Tests** - MockitoExtension for service layer
3. **Controller Tests** - @WebMvcTest with security context
4. **Architecture Tests** - ArchUnit for layered architecture enforcement
5. **Integration Tests** - Testcontainers for PostgreSQL/Kafka

**Test Structure:**
```
src/test/java/id/payu/<service>/
├── service/           # Unit tests (Mockito)
├── controller/        # WebMvcTest
├── architecture/      # ArchUnit rules
└── integration/       # Testcontainers
```

### Clean Architecture (Hybrid)

| Service Type | Architecture | Reason |
|-------------|--------------|--------|
| Core Banking | Clean/Hexagonal | Complex business logic, high testability |
| Supporting | Layered | Mostly CRUD, simpler |
| ML Services | Simplified Clean | Focus on ML logic |

### 🎨 Frontend Design System (Premium Emerald)

Untuk menjaga konsistensi UI yang premium dan modern:
1. **Layout**: Selalu gunakan `DashboardLayout` untuk halaman terautentikasi (main app).
2. **Color Palette**:
   - Primary: `bank-green` (#10b981) - Gunakan untuk action buttons dan brand primary.
   - Secondary: `bank-emerald` (#059669) - Gunakan untuk gradients dan hover states.
   - Background: `bg-background` (bg-white / dark:bg-gray-950) - Gunakan `bg-card` untuk mobile cards.
3. **Typography**:
   - Menggunakan Google Fonts **Inter** atau **Outfit**.
   - Headers: Gunakan `font-black` (900) dengan `tracking-tighter` untuk kesan premium.
   - Body: Gunakan `font-medium` (500) untuk readability.
4. **Components Style**:
   - Radius: Gunakan `rounded-[2.5rem]` untuk main cards dan `rounded-3xl` untuk modal/small cards.
   - Border: Gunakan `border-border` yang sangat halus (gray-100/800).
   - Shadows: Gunakan `shadow-2xl shadow-bank-green/20` untuk primary buttons.
5. **Data Formatting**:
   - Mata Uang: Selalu gunakan format Rupiah (`Rp 1.000.000`).
   - Angka: Gunakan `toLocaleString('id-ID')`.
6. **Mobile First**: Pastikan layout responsif dengan `MobileNav` dan `MobileHeader` yang konsisten.

### Git & Changelog Policy ⚠️

Setiap kali melakukan perubahan kode yang signifikan:
1. **Update CHANGELOG.md**: Tambahkan entri di bawah `[Unreleased]` sesuai format Keep a Changelog.
2. **Git Commit**: Gunakan conventional commits (`feat:`, `fix:`, `docs:`, `refactor:`)
3. **Git Push**: Push ke remote repository setelah selesai.

---

## 🔗 Related Resources

| Resource | Path |
|----------|------|
| Resource | Path |
|----------|------|
| Architecture | `docs/architecture/ARCHITECTURE.md` |
| Product Requirements | `docs/product/PRD.md` |
| Changelog | `CHANGELOG.md` |
| README | `README.md` |
| Roadmap | `docs/roadmap/TODOS.md` |

---

## 📞 Support

- **Backend Issues:** backend-team@payu.id
- **Platform Issues:** platform-team@payu.id
- **Architecture:** architect@payu.id

---

*Last Updated: January 2026*
