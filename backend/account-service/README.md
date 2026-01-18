# Account Service

> Core Banking Service for User Management and Accounts.

## Overview
The Account Service handles:
- User Registration & Onboarding
- eKYC Verification (via Dukcapil Simulator)
- Account Creation & Management
- Profile Management

## Tech Stack
- **Framework**: Spring Boot 3.4.1
- **Language**: Java 21
- **Database**: PostgreSQL 16
- **Messaging**: Apache Kafka
- **Security**: OAuth2 Resource Server (JWT)
- **API Client**: Spring Cloud OpenFeign

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/accounts/register` | Register new user |

## Configuration
| Environment Variable | Default | Description |
|----------------------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/account_db` | Database URL |
| `KAFKA_BROKERS` | `localhost:9092` | Kafka Brokers |
| `GATEWAY_URL` | `http://localhost:8080` | API Gateway URL (for Feign) |
| `OIDC_ISSUER` | `http://localhost:8180/...` | Keycloak Issuer URI |

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL
- Kafka

### Start Service
```bash
./mvnw spring-boot:run
```
