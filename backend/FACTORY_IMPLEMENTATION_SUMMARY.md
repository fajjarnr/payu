# Test Data Factory Implementation Summary

## Overview

Successfully implemented test data factory patterns using Faker for Python services (KYC and Analytics) to improve test maintainability and reduce hardcoded test data.

## Services Enhanced

### 1. KYC Service (`/home/ubuntu/payu/backend/kyc-service`)
### 2. Analytics Service (`/home/ubuntu/payu/backend/analytics-service`)

## Changes Made

### 1. Dependencies Added

**KYC Service** (`/home/ubuntu/payu/backend/kyc-service/requirements.txt`):
- Added `faker==30.0.0` to test dependencies

**Analytics Service** (`/home/ubuntu/payu/backend/analytics-service/requirements.txt`):
- Added `faker==30.0.0` to test dependencies

### 2. Factory Modules Created

#### KYC Service Factories

**Location**: `/home/ubuntu/payu/backend/kyc-service/tests/factories/`

Files created:
- `__init__.py` - Factory module exports
- `kyc_factory.py` - KYC-specific factory functions
- `README.md` - Documentation and usage examples

Factory functions:
- `user_factory(**kwargs)` - Generate test user data
- `ktp_ocr_factory(**kwargs)` - Generate KTP OCR result data
- `liveness_result_factory(**kwargs)` - Generate liveness check results
- `face_match_factory(**kwargs)` - Generate face match results
- `dukcapil_result_factory(**kwargs)` - Generate Dukcapil verification results
- `kyc_verification_factory(**kwargs)` - Generate complete KYC verification data
- `sample_ktp_image_base64()` - Generate sample KTP image
- `sample_selfie_image_base64()` - Generate sample selfie image

#### Analytics Service Factories

**Location**: `/home/ubuntu/payu/backend/analytics-service/tests/factories/`

Files created:
- `__init__.py` - Factory module exports
- `analytics_factory.py` - Analytics-specific factory functions
- `README.md` - Documentation and usage examples

Factory functions:
- `transaction_factory(**kwargs)` - Generate transaction data
- `user_history_factory(**kwargs)` - Generate user transaction history
- `user_metrics_factory(**kwargs)` - Generate user metrics
- `fraud_score_factory(**kwargs)` - Generate fraud score data
- `fraud_detection_result_factory(**kwargs)` - Generate complete fraud detection results
- `spending_pattern_factory(**kwargs)` - Generate spending patterns
- `recommendation_factory(**kwargs)` - Generate recommendations
- `risk_assessment_factory(**kwargs)` - Generate risk assessment data
- `portfolio_allocation_factory(**kwargs)` - Generate portfolio allocations
- `robo_advisory_response_factory(**kwargs)` - Generate complete robo-advisory responses

### 3. Test Configuration Updates

#### KYC Service (`/home/ubuntu/payu/backend/kyc-service/tests/conftest.py`)
Added factory fixtures:
- `user_factory_fixture` - Factory for generating test user data
- `ktp_ocr_factory_fixture` - Factory for generating KTP OCR data
- `kyc_verification_factory_fixture` - Factory for generating KYC verification data

#### Analytics Service (`/home/ubuntu/payu/backend/analytics-service/tests/conftest.py`)
Added factory fixtures:
- `transaction_factory_fixture` - Factory for generating test transaction data
- `user_history_factory_fixture` - Factory for generating test user history data
- `fraud_score_factory_fixture` - Factory for generating test fraud score data
- `user_metrics_factory_fixture` - Factory for generating test user metrics data

### 4. Example Test Files Created

#### KYC Service
**File**: `/home/ubuntu/payu/backend/kyc-service/tests/unit/test_factory_usage_example.py`
- Demonstrates factory pattern usage for KYC testing
- Shows how to generate unique test data
- Examples of edge case testing
- Comparison of hardcoded vs factory-based approaches

**File**: `/home/ubuntu/payu/backend/kyc-service/tests/unit/test_api_with_factories.py`
- Refactored API tests using factory patterns
- Shows integration with existing test infrastructure

#### Analytics Service
**File**: `/home/ubuntu/payu/backend/analytics-service/tests/unit/test_factory_usage_example.py`
- Demonstrates factory pattern usage for analytics testing
- Shows transaction, fraud, and robo-advisory testing
- Examples of batch test generation
- Edge case testing demonstrations

## Benefits Achieved

### 1. Reduced Hardcoded Test Data

**Before**:
```python
user = {
    "user_id": "user_123",
    "email": "user123@example.com",
    "phone": "+628123456789",
    "kyc_status": "PENDING"
}
```

**After**:
```python
user = user_factory()
```

### 2. Realistic Test Data Variation

- Indonesian names and phone numbers
- Valid 16-digit NIK format for KYC
- Realistic transaction amounts in IDR
- Proper risk score ranges for fraud detection
- Valid portfolio allocations for robo-advisory

### 3. Easier Edge Case Testing

```python
# Test with low confidence OCR
poor_quality_ktp = ktp_ocr_factory(confidence=0.65)

# Test new user scenario
new_user = user_history_factory(
    total_transactions=2,
    total_amount=100000.0
)

# Test critical fraud risk
critical = fraud_score_factory(risk_level="CRITICAL")
```

### 4. Batch Test Generation

```python
# Generate 20 unique users
users = [user_factory() for _ in range(20)]

# All users are unique
user_ids = [u['user_id'] for u in users]
assert len(set(user_ids)) == 20
```

### 5. Improved Test Maintainability

- Single source of truth for test data structure
- Easy to update factory when schema changes
- Reduced code duplication across tests
- Clear documentation in factory functions

## Testing

### KYC Service Tests
```bash
cd /home/ubuntu/payu/backend/kyc-service
source .venv/bin/activate
python -m pytest tests/unit/test_factory_usage_example.py -v --no-cov
```

### Analytics Service Tests
```bash
cd /home/ubuntu/payu/backend/analytics-service
source .venv/bin/activate
python -m pytest tests/unit/test_factory_usage_example.py -v --no-cov
```

## Usage Examples

### KYC Service

```python
from tests.factories import (
    user_factory,
    ktp_ocr_factory,
    kyc_verification_factory
)

# Generate test user
user = user_factory(kyc_status="VERIFIED")

# Generate KTP data
ktp = ktp_ocr_factory(province="DKI JAKARTA")

# Generate complete KYC verification
kyc = kyc_verification_factory(status="PENDING")
```

### Analytics Service

```python
from tests.factories import (
    transaction_factory,
    fraud_score_factory,
    user_history_factory
)

# Generate transaction
txn = transaction_factory(amount=5000000.0, type="TRANSFER")

# Generate fraud score
score = fraud_score_factory(risk_level="HIGH")

# Generate user history
history = user_history_factory(total_transactions=500)
```

## Documentation

Comprehensive documentation has been added:

- `/home/ubuntu/payu/backend/kyc-service/tests/factories/README.md`
- `/home/ubuntu/payu/backend/analytics-service/tests/factories/README.md`

Each includes:
- Factory function descriptions
- Parameter documentation
- Usage examples
- Best practices
- Migration guide

## DDD Patterns Applied

### Factory Pattern
- Factory functions encapsulate test data creation logic
- Each factory is responsible for creating specific domain objects
- Supports overriding defaults for flexible testing

### Value Objects
- Generated data represents domain value objects (e.g., KtpOcrResult, FraudScore)
- Factories ensure valid value object states

### Test Data Builders
- Factories act as test data builders
- Support fluent interface through kwargs
- Enable complex test scenario composition

## Compliance with Hexagonal Principles

- **Domain Layer**: No infrastructure dependencies in factories
- **Application Layer**: Factories support application testing
- **Infrastructure Layer**: Factory modules in tests/ directory (test infrastructure)

## Next Steps

### Recommended
1. Gradually migrate existing tests to use factory patterns
2. Add more factory functions as new domain objects are introduced
3. Consider adding factory-based fixtures for common test scenarios
4. Update test documentation to reference factory patterns

### Optional Enhancements
1. Add factory sequences for generating ordered test data
2. Create factory traits for common test scenarios (e.g., "verified_user", "high_risk_transaction")
3. Add factory validation to ensure generated data meets schema requirements
4. Integrate with hypothesis for property-based testing

## File Locations

### KYC Service
- Requirements: `/home/ubuntu/payu/backend/kyc-service/requirements.txt`
- Factories: `/home/ubuntu/payu/backend/kyc-service/tests/factories/`
- Examples: `/home/ubuntu/payu/backend/kyc-service/tests/unit/test_factory_usage_example.py`

### Analytics Service
- Requirements: `/home/ubuntu/payu/backend/analytics-service/requirements.txt`
- Factories: `/home/ubuntu/payu/backend/analytics-service/tests/factories/`
- Examples: `/home/ubuntu/payu/backend/analytics-service/tests/unit/test_factory_usage_example.py`

## Notes

- All factories use Faker with Indonesian locale (`id_ID`) for realistic data
- NIK generation follows Indonesian format but may not be valid NIKs
- Image data factories return minimal 1x1 pixel PNG images for testing
- All generated data is random and unique on each call
- Factory functions are designed to be composable and flexible

## Conclusion

The factory pattern implementation successfully addresses the requirements:

1. ✅ Added `faker` to test dependencies
2. ✅ Created factory classes/functions for generating test data
3. ✅ Demonstrated replacing hardcoded test data with factory-generated data
4. ✅ Documented the factory patterns in comments and README files

The implementation improves test maintainability, enables better edge case testing, and reduces code duplication across both Python services.
