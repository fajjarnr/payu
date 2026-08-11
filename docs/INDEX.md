# PayU Digital Banking Platform

Welcome to the PayU Developer Documentation. This platform provides comprehensive core banking and payment gateway services for the Indonesian market.

## Quick Start & Developer Guides

- [Developer Onboarding](guides/ONBOARDING.md) - Set up your development environment
- [API Standards](api/API_STANDARDS.md) - REST API design guidelines
- [Database & Cache Optimization](guides/DATABASE_CACHE_OPTIMIZATION.md) - Best practices for DB & Redis
- [Java Container Strategy](guides/JAVA_CONTAINER_STRATEGY.md) - Container configurations
- [HashiCorp Vault Integration](guides/VAULT.md) - Vault integration & secrets
- [Webhook Handling](guides/WEBHOOK_HANDLING.md) - Integrating and verifying webhooks
- [TDD Quick Reference](guides/TDD_QUICK_REFERENCE.md) - Unit & Integration testing reference
- [Agent Skills & Architecture](guides/AGENT_SKILLS_GUIDE.md) - Multi-Engineer AI Ecosystem Guide
- [Lessons Learned Registry](guides/LESSONS.md) - Post-mortem and optimization registry

## Architecture

- [System Architecture](architecture/ARCHITECTURE.md) - High-level system design and C4 diagrams
- [Service Catalog](architecture/SERVICE_CATALOG.md) - Complete list of microservices
- [Service Status](roadmap/SERVICES.md) - Detailed implementation status & test results
- [Event Catalog](architecture/EVENT_CATALOG.md) - Domain events and Kafka topics
- [Gateway Architecture](roadmap/GATEWAY_ARCH.md) - Payment gateway design for partners
- [DevSecOps Architecture](architecture/DEVSECOPS_ARCHITECTURE.md) - Pipeline, namespace strategy, security gates

### Backend Services (Port-Ordered Registry)

| Service                 | Description                         | Port   |
| ----------------------- | ----------------------------------- | ------ |
| account-service         | User management and onboarding      | 8001   |
| auth-service            | Authentication and authorization    | 8002   |
| transaction-service     | Fund transfers and payments         | 8003   |
| wallet-service          | Balance management and ledger       | 8004   |
| billing-service         | Bill payments (PLN, PDAM, etc)      | 8005   |
| notification-service    | Push, SMS, Email, WhatsApp          | 8006   |
| kyc-service             | OCR and face matching               | 8007   |
| analytics-service       | Insights and recommendations        | 8008   |
| investment-service      | Mutual funds and gold               | 8009   |
| lending-service         | Loans and PayLater                  | 8010   |
| backoffice-service      | Internal admin dashboard            | 8011   |
| partner-service         | Partner integration & management    | 8012   |
| promotion-service       | Promo campaigns and vouchers        | 8013   |
| support-service         | Customer support and ticketing      | 8014   |
| statement-service       | PDF e-statements generation         | 8015   |
| api-portal-service      | Developer OpenAPI portal            | 8021   |
| gateway-service         | API Gateway & Rate limiting         | 8080   |
| compliance-service      | Regulatory compliance and AML       | 8087   |
| cms-service             | Banners and dynamic content         | 8095   |
| fx-service              | Foreign exchange rates & conversion | 8096   |
| dispute-service         | Refunds and dispute management      | 8098   |
| product-catalog-service | Banking products and fees catalog   | 8100   |
| integration-service     | External Swift/ISO20022 Adapter     | 8101   |

### External Service Simulators

| Simulator          | Purpose                            | Port   |
| ------------------ | ---------------------------------- | ------ |
| bi-fast-simulator  | Mock for BI-FAST infrastructure    | 8090   |
| dukcapil-simulator | Mock for Identity verification     | 8091   |
| qris-simulator     | Mock for QRIS payment engine       | 8092   |
| biller-simulator   | Mock for PPOB billers (PLN/PDAM)   | 8093   |
| va-simulator       | Mock for Virtual Account providers | 8085   |

*\* Simulators not yet mapped in default compose, using default internal ports.*

## Infrastructure

The platform includes a pre-configured observability and storage stack:

| Tool        | Purpose                           | Port   |
| ----------- | --------------------------------- | ------ |
| Grafana     | Visualization dashboards          | 3000   |
| Loki        | Log aggregation platform          | 3100   |
| Postgres    | Primary relational storage        | 5432   |
| Redis       | Multi-layer caching               | 6379   |
| Kafka UI    | Message & topic browser           | 8088   |
| Keycloak    | Identity and access management    | 8099   |
| Vault       | Secrets and encryption management | 8200   |
| RustFS (S3) | Object storage for statements     | 9000   |
| Prometheus  | Metrics collection                | 9090   |
| Kafka       | Event streaming platform          | 9092   |
| Jaeger      | Distributed tracing UI            | 16686  |

## Development

### Building

```bash
# Build all services
mvn -f backend/pom.xml clean package -DskipTests -T 1C

# Build single service
cd backend/account-service && mvn clean package
```

### Testing

```bash
# Run all tests
make test

# Run single service tests
./scripts/test-single-service.sh account-service
```

### Running Locally

```bash
# Start infrastructure
podman compose up -d

# Run service
cd backend/account-service && mvn spring-boot:run
```

## API Documentation

Each service exposes OpenAPI documentation:

- Account Service: <http://localhost:8001/swagger-ui.html>
- Wallet Service: <http://localhost:8004/swagger-ui.html>
- Transaction Service: <http://localhost:8003/swagger-ui.html>
- Gateway Service: <http://localhost:8080/swagger-ui>

## Security

- [Security Policy](security/SECURITY.md)
- [PCI-DSS Compliance](security/PCI-DSS-UU-PDP-AUDIT-REPORT.md)
- [Penetration Testing Schedule](security/PENTEST_SCHEDULE.md)

## Support

- [Troubleshooting Guide](TROUBLESHOOTING.md)
- [Operations Runbooks](operations/runbooks/)

---

*Last Updated: August 2026*
