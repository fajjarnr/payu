# Billing Service

> Quarkus Native service for bill payments (PLN, PDAM, Pulsa, etc.)

## 🏗️ Architecture

This service uses **Quarkus 3.x** with a simplified layered architecture:

```
src/main/java/id/payu/billing/
├── domain/             # Entities and business logic
├── resource/           # REST endpoints
├── client/             # External service clients (wallet-service)
└── dto/                # Request/Response objects
```

## ✨ Features

- **PLN** - Electricity bill payment
- **PDAM** - Water bill payment
- **Pulsa** - Mobile top-up (Telkomsel, XL, Indosat, etc.)
- **Internet** - Internet/Cable TV payment

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Runtime |
| Quarkus | 3.17.x | Framework (Native) |
| Hibernate Panache | - | PostgreSQL ORM |
| SmallRye Kafka | - | Event streaming |
| SmallRye Fault Tolerance | - | Circuit breaker |

## 🚀 Running Locally

```bash
# Development mode (hot reload)
./mvnw quarkus:dev

# Build JAR
./mvnw package

# Build Native
./mvnw package -Pnative

# Run tests
./mvnw test
```

## 📦 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/billers` | List available billers |
| GET | `/api/v1/billers/{code}` | Get biller details |
| POST | `/api/v1/payments` | Create bill payment |
| GET | `/api/v1/payments/{id}` | Get payment status |

---

*Part of PayU Digital Banking Platform*
