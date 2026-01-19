# Wallet Service

> Core Banking Service for balance management and ledger operations

## 🏗️ Architecture

This service follows **Hexagonal Architecture (Ports & Adapters)** pattern:

```
src/main/java/id/payu/wallet/
├── domain/
│   ├── model/          # Business entities (Wallet, WalletTransaction)
│   └── port/
│       ├── in/         # Use case interfaces (Input Ports)
│       └── out/        # Repository/Event interfaces (Output Ports)
├── application/
│   └── service/        # Use case implementations
├── adapter/
│   ├── persistence/    # JPA implementations
│   ├── web/            # REST Controllers
│   └── messaging/      # Kafka producers
├── dto/                # Data Transfer Objects
├── config/             # Spring configurations
└── exception/          # Exception handlers
```

## ✨ Features

- **Balance Management** - Get current balance, available balance
- **Reserve Balance** - Lock funds for pending transactions
- **Commit/Release** - Finalize or cancel reserved amounts
- **Ledger Entries** - Full transaction history (CREDIT/DEBIT)
- **Multi-Pocket Support** - Multiple sub-wallets per account

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
| GET | `/api/v1/wallets/{accountId}` | Get wallet by account ID |
| GET | `/api/v1/wallets/{accountId}/balance` | Get current balance |
| POST | `/api/v1/wallets/{accountId}/reserve` | Reserve balance |
| POST | `/api/v1/wallets/{accountId}/commit` | Commit reserved amount |
| POST | `/api/v1/wallets/{accountId}/release` | Release reserved amount |
| GET | `/api/v1/wallets/{accountId}/transactions` | Get ledger entries |

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
