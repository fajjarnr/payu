# Promotion Service
Service for Rewards, Cashback, Referral, and Loyalty Points.

## Tech Stack
- Java 21
- Quarkus 3.x
- Architecture: Layered

## API Endpoints

### Promotions
- `POST /api/v1/promotions` - Create promotion
- `PUT /api/v1/promotions/{id}` - Update promotion
- `POST /api/v1/promotions/{code}/claim` - Claim promotion
- `POST /api/v1/promotions/{id}/activate` - Activate promotion
- `GET /api/v1/promotions` - Get active promotions
- `GET /api/v1/promotions/{id}` - Get promotion by ID
- `GET /api/v1/promotions/code/{code}` - Get promotion by code

### Rewards
- `GET /api/v1/rewards/{id}` - Get reward
- `GET /api/v1/rewards/account/{accountId}` - Get rewards by account
- `GET /api/v1/rewards/account/{accountId}/summary` - Get reward summary

### Cashbacks
- `POST /api/v1/cashbacks` - Create cashback
- `GET /api/v1/cashbacks/{id}` - Get cashback
- `GET /api/v1/cashbacks/account/{accountId}` - Get cashbacks by account
- `GET /api/v1/cashbacks/account/{accountId}/summary` - Get cashback summary

### Referrals
- `POST /api/v1/referrals` - Create referral
- `POST /api/v1/referrals/complete` - Complete referral
- `GET /api/v1/referrals/{id}` - Get referral
- `GET /api/v1/referrals/code/{code}` - Get referral by code
- `GET /api/v1/referrals/referrer/{referrerAccountId}` - Get referrals by referrer
- `GET /api/v1/referrals/referrer/{referrerAccountId}/summary` - Get referral summary

### Loyalty Points
- `POST /api/v1/loyalty-points` - Add points
- `POST /api/v1/loyalty-points/redeem` - Redeem points
- `GET /api/v1/loyalty-points/{id}` - Get loyalty points record
- `GET /api/v1/loyalty-points/account/{accountId}` - Get points by account
- `GET /api/v1/loyalty-points/account/{accountId}/balance` - Get balance