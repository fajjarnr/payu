# Notification Service

> Quarkus Native service for Push, SMS, and Email notifications

## 🏗️ Architecture

```
src/main/java/id/payu/notification/
├── domain/             # Entities (Notification)
├── resource/           # REST endpoints
├── consumer/           # Kafka consumers
├── sender/             # Email, SMS, Push providers
└── dto/                # Request/Response objects
```

## ✨ Features

- **Push Notifications** - Firebase Cloud Messaging (FCM)
- **SMS** - Integration with SMS providers
- **Email** - Transactional emails (OTP, transaction alerts)
- **Templates** - Configurable notification templates
- **Retry** - Automatic retry for failed notifications

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Runtime |
| Quarkus | 3.17.x | Framework (Native) |
| Hibernate Panache | - | PostgreSQL ORM |
| SmallRye Kafka | - | Event consumer |
| Quarkus Mailer | - | Email sending |

## 🚀 Running Locally

```bash
# Development mode
./mvnw quarkus:dev

# Build
./mvnw package

# Run tests
./mvnw test
```

## 📦 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/notifications` | Send notification |
| GET | `/api/v1/notifications/{id}` | Get notification status |
| GET | `/api/v1/notifications/user/{userId}` | Get user notifications |

## 📨 Kafka Topics (Consumer)

| Topic | Action |
|-------|--------|
| `wallet.balance.changed` | Send balance update notification |
| `transaction.completed` | Send transaction success notification |
| `payment-events` | Send bill payment notification |

---

*Part of PayU Digital Banking Platform*
