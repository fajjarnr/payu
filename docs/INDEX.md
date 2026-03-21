# PayU Digital Banking Platform

Welcome to the PayU Developer Documentation. This platform provides comprehensive core banking and payment gateway services for the Indonesian market.

## Quick Start

- [Developer Onboarding](guides/ONBOARDING.md) - Set up your development environment
- [API Standards](api/API_STANDARDS.md) - REST API design guidelines
- [Contributing Guide](guides/CONTRIBUTING.md) - Git workflow and coding standards

## Architecture

- [System Architecture](architecture/ARCHITECTURE.md) - High-level system design and C4 diagrams
- [Service Catalog](architecture/SERVICE_CATALOG.md) - Complete list of microservices
- [Event Catalog](architecture/EVENT_CATALOG.md) - Domain events and Kafka topics
- [Gateway Architecture](roadmap/GATEWAY_ARCH.md) - Payment gateway design for partners

## Services

### Core Banking Services (Spring Boot)

| Service | Description | Port |
|---------|-------------|------|
| account-service | User management and onboarding | 8001 |
| auth-service | Authentication and authorization | 8002 |
| transaction-service | Fund transfers and payments | 8003 |
| wallet-service | Balance management and ledger | 8004 |
| investment-service | Mutual funds and gold | 8009 |
| lending-service | Loans and PayLater | 8010 |
| fx-service | Foreign exchange | 8011 |
| statement-service | PDF statements | 8012 |
| compliance-service | Regulatory compliance | 8087 |
| dispute-service | Refunds and disputes | 8088 |

### Supporting Services (Quarkus)

| Service | Description | Port |
|---------|-------------|------|
| billing-service | Bill payments | 8005 |
| notification-service | Multi-channel notifications | 8006 |
| gateway-service | API gateway | 8080 |
| partner-service | Partner integration | 8013 |
| promotion-service | Vouchers and cashback | 8014 |
| backoffice-service | Admin dashboard | 8015 |
| support-service | Customer support | 8086 |
| api-portal-service | Developer portal | 8016 |

### ML/Data Services (Python/FastAPI)

| Service | Description | Port |
|---------|-------------|------|
| kyc-service | OCR and face matching | 8007 |
| analytics-service | Insights and recommendations | 8008 |

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

- Account Service: http://localhost:8001/swagger-ui.html
- Wallet Service: http://localhost:8004/swagger-ui.html
- Transaction Service: http://localhost:8003/swagger-ui.html
- Gateway Service: http://localhost:8080/swagger-ui

## Security

- [Security Policy](security/SECURITY.md)
- [PCI-DSS Compliance](security/PCI-DSS-UU-PDP-AUDIT-REPORT.md)

## Support

- [Troubleshooting Guide](TROUBLESHOOTING.md)
- [Operations Runbooks](operations/runbooks/)

---

*Last Updated: March 2026*
