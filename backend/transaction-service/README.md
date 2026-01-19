# Transaction Service

> Core Banking Service for handling transfers, BI-FAST, and QRIS transactions

## 🏗️ Architecture

This service follows **Hexagonal Architecture (Ports & Adapters)** pattern:

```
src/main/java/id/payu/transaction/
├── domain/
│   ├── model/          # Business entities
│   └── port/
│       ├── in/         # Use case interfaces (Input Ports)
│       └── out/        # Repository/Client interfaces (Output Ports)
├── application/
│   └── service/        # Use case implementations
├── adapter/
│   ├── persistence/    # JPA implementations
│   ├── web/            # REST Controllers
│   ├── client/         # External service clients
│   └── messaging/      # Kafka producers/consumers
├── dto/                # Data Transfer Objects
├── config/             # Spring configurations
└── exception/          # Exception handlers
```

## ✨ Features

- **Internal Transfer** - Account-to-account transfers within PayU
- **BI-FAST Integration** - Real-time transfers to external banks
- **QRIS Payments** - QR code-based payments
- **Transaction History** - Query and track transactions

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Runtime |
| Spring Boot | 3.4.x | Framework |
| Spring Data JPA | - | PostgreSQL access |
| Spring Kafka | - | Event streaming |
| Flyway | - | DB migrations |

## 🚀 Running Locally

```bash
# Development mode
mvn spring-boot:run

# Build
mvn clean package

# Run tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## 📦 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/transactions/transfer` | Initiate internal transfer |
| POST | `/api/v1/transactions/bifast` | Initiate BI-FAST transfer |
| POST | `/api/v1/transactions/qris/pay` | Process QRIS payment |
| GET | `/api/v1/transactions/{id}` | Get transaction by ID |

## 🧪 Testing

```bash
# Unit tests
mvn test -Dtest=*ServiceTest

# Controller tests
mvn test -Dtest=*ControllerTest

# Architecture tests (ArchUnit)
mvn test -Dtest=ArchitectureTest
```

---

*Part of PayU Digital Banking Platform*
