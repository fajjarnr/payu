# Product Catalog Service

Database-driven product configuration service for PayU platform.

## Overview

This service replaces hardcoded product parameters across the PayU ecosystem with a flexible, database-driven configuration system. New products can be added without redeploying services.

## Architecture

- **Hexagonal Architecture**: Clean separation between domain, application, and adapter layers
- **Domain Layer**: `ProductDefinition` aggregate root with `ProductType` enum
- **Application Layer**: `ProductCatalogService` implementing use cases with Redis caching
- **Adapter Layer**: JPA persistence, REST controllers (admin and public)

## Product Types

- `SAVINGS` - Savings accounts with configurable minimum balance, interest rates
- `LOAN` - Personal and micro loans with configurable tenors, rates, fees
- `PAYLATER` - Buy now, pay later with credit limits and installment options
- `INVESTMENT` - Deposits, mutual funds, digital gold
- `INSURANCE` - Life and general insurance products
- `CREDIT_CARD` - Credit card products with limits and fees
- `DEPOSIT` - Time deposits with fixed terms

## API Endpoints

### Admin Endpoints (Require ADMIN role)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/admin/products` | Create new product |
| GET | `/admin/products` | List all products |
| GET | `/admin/products/{code}` | Get product detail |
| PUT | `/admin/products/{code}` | Update product |
| DELETE | `/admin/products/{code}` | Soft delete (deactivate) |
| POST | `/admin/products/{code}/activate` | Activate product |
| GET | `/admin/products/type/{type}` | List by type |

### Public Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/products` | List active products |
| GET | `/products/{code}` | Get active product detail |
| GET | `/products/{code}/parameters/{key}` | Get specific parameter |

## Default Products

The following products are seeded on startup:

- `SAVINGS_BASIC` - Standard savings (min balance: IDR 10,000)
- `SAVINGS_PREMIUM` - High-yield savings (min balance: IDR 1,000,000)
- `LOAN_PERSONAL` - Personal loan (5M - 300M IDR, 6-60 months)
- `LOAN_MICRO` - Micro loan (1M - 50M IDR, 3-24 months)
- `PAYLATER_STANDARD` - PayLater (1M - 50M IDR limit)
- `INVESTMENT_DEPOSIT` - Time deposit (10M+ IDR, 1-24 months)
- `INVESTMENT_MUTUAL_FUND` - Mutual funds (100K+ IDR)
- `INVESTMENT_GOLD` - Digital gold (10K+ IDR)
- `INSURANCE_LIFE_BASIC` - Term life insurance
- `CREDIT_CARD_CLASSIC` - Classic credit card

## Configuration

Products are stored with flexible JSONB parameters:

```json
{
  "minimumBalance": 10000,
  "interestRate": 0.015,
  "monthlyFee": 0,
  "currency": "IDR"
}
```

## Caching

- Redis cache with 5-minute TTL
- Cache invalidation on product updates
- Cache warming enabled on startup

## Database

- PostgreSQL with JSONB support
- Flyway migrations for schema and seed data

## Integration

Other services can query product configuration via:

1. **Direct HTTP**: Call public endpoints
2. **Client Library**: Use `ProductCatalogClient` (to be implemented)
3. **Cache Sync**: Subscribe to cache invalidation events

## Future Enhancements

- Admin UI in backoffice-service
- gRPC interface for inter-service communication
- Product versioning and rollback
- A/B testing support for product parameters
