# Test Data Factories for KYC Service

## Overview

This module provides factory functions for generating realistic test data using the Faker library. Factories help reduce hardcoded test values, improve test maintainability, and enable easier edge case testing.

## Installation

The `faker` library has been added to `requirements.txt`:

```bash
pip install faker==30.0.0
```

## Usage

### Basic Usage

```python
from tests.factories import user_factory, ktp_ocr_factory

# Generate a random user
user = user_factory()
print(user['user_id'])  # e.g., "user_a1b2c3d4"
print(user['email'])    # e.g., "john.doe@example.com"
print(user['kyc_status'])  # e.g., "PENDING"

# Generate with overrides
verified_user = user_factory(kyc_status="VERIFIED")
print(verified_user['kyc_status'])  # "VERIFIED"
```

### KYC Verification Testing

```python
from tests.factories import kyc_verification_factory

# Generate pending verification
pending = kyc_verification_factory(status="PENDING")

# Generate verified verification
verified = kyc_verification_factory(
    status="VERIFIED",
    user_id="custom_user_id"
)
```

### KTP OCR Testing

```python
from tests.factories import ktp_ocr_factory

# Generate realistic KTP data
ktp = ktp_ocr_factory()
print(ktp['nik'])  # 16-digit NIK
print(ktp['name'])  # Indonesian name in uppercase
print(ktp['province'])  # Indonesian province

# Generate with specific confidence
low_confidence_ktp = ktp_ocr_factory(confidence=0.65)
```

### Image Data Testing

```python
from tests.factories import sample_ktp_image_base64, sample_selfie_image_base64

# Get sample base64 images for testing
ktp_image = sample_ktp_image_base64()
selfie_image = sample_selfie_image_base64()
```

## Available Factories

### `user_factory(**kwargs)`

Generate test user data with realistic Indonesian information.

**Parameters:**
- `user_id` (str): User identifier
- `email` (str): Email address
- `phone` (str): Indonesian phone number
- `full_name` (str): Full name
- `kyc_status` (str): One of PENDING, PROCESSING, VERIFIED, REJECTED, FAILED
- `account_created_at` (datetime): Account creation date
- `date_of_birth` (date): Date of birth

**Example:**
```python
user = user_factory(
    kyc_status="VERIFIED",
    account_created_at="2024-01-01T00:00:00"
)
```

### `ktp_ocr_factory(**kwargs)`

Generate KTP OCR result data with realistic Indonesian KTP information.

**Parameters:**
- `nik` (str): 16-digit Indonesian NIK
- `name` (str): Full name in uppercase
- `birth_date` (str): Date in DD-MM-YYYY format
- `gender` (str): LAKI-LAKI or PEREMPUAN
- `address` (str): Street address
- `province` (str): Indonesian province
- `city` (str): Indonesian city/kabupaten
- `district` (str): Indonesian kecamatan
- `confidence` (float): OCR confidence score (0.0 to 1.0)

**Example:**
```python
ktp = ktp_ocr_factory(
    province="DKI JAKARTA",
    confidence=0.95
)
```

### `liveness_result_factory(**kwargs)`

Generate liveness check result data.

**Parameters:**
- `is_live` (bool): Whether the selfie is live
- `confidence` (float): Liveness confidence score
- `face_detected` (bool): Whether face was detected
- `face_quality_score` (float): Face image quality score

**Example:**
```python
# Successful liveness check
live = liveness_result_factory(is_live=True)

# Failed liveness check
spoof = liveness_result_factory(is_live=False)
```

### `face_match_factory(**kwargs)`

Generate face match result between KTP and selfie.

**Parameters:**
- `is_match` (bool): Whether faces match
- `similarity_score` (float): Similarity score (0.0 to 1.0)
- `threshold` (float): Matching threshold used
- `ktp_face_found` (bool): Face detected on KTP
- `selfie_face_found` (bool): Face detected on selfie

**Example:**
```python
# Matching faces
match = face_match_factory(is_match=True)

# Non-matching faces
no_match = face_match_factory(is_match=False)
```

### `dukcapil_result_factory(**kwargs)`

Generate Dukcapil (civil registry) verification result.

**Parameters:**
- `nik` (str): 16-digit NIK
- `is_valid` (bool): Whether NIK is valid
- `name` (str): Name from Dukcapil
- `status` (str): Verification status
- `match_score` (float): Optional match score

**Example:**
```python
valid = dukcapil_result_factory(is_valid=True)
invalid = dukcapil_result_factory(is_valid=True, status="NOT_FOUND")
```

### `kyc_verification_factory(**kwargs)`

Generate complete KYC verification data.

**Parameters:**
- `user_id` (str): User identifier
- `verification_type` (str): FULL_KYC or BASIC_KYC
- `status` (str): KYC status
- `ktp_ocr_result` (dict): KTP OCR result
- `liveness_result` (dict): Liveness check result
- `face_match_result` (dict): Face match result
- `rejection_reason` (str): Optional rejection reason

**Example:**
```python
verified = kyc_verification_factory(status="VERIFIED")
rejected = kyc_verification_factory(
    status="REJECTED",
    rejection_reason="Face mismatch detected"
)
```

## Benefits

### 1. Reduced Hardcoded Values

**Before:**
```python
user = {
    "user_id": "user_123",
    "email": "user123@example.com",
    "phone": "+628123456789",
    "kyc_status": "PENDING"
}
```

**After:**
```python
user = user_factory()
```

### 2. Realistic Test Data

Factories generate realistic Indonesian data:
- Indonesian names (e.g., "BUDI SANTOSO", "SITI AMINAH")
- Indonesian phone numbers (e.g., "+628123456789")
- Indonesian provinces and cities
- Valid 16-digit NIK format
- Realistic dates and confidence scores

### 3. Easy Edge Case Testing

```python
# Test with low confidence OCR
poor_quality = ktp_ocr_factory(confidence=0.65)

# Test new user
new_user = user_factory(
    kyc_status="UNVERIFIED",
    account_created_at="2024-01-01T00:00:00"
)

# Test rejected KYC
rejected = kyc_verification_factory(
    status="REJECTED",
    rejection_reason="Document expired"
)
```

### 4. Batch Test Generation

```python
# Generate 10 unique users
users = [user_factory() for _ in range(10)]

# All users are unique
user_ids = [u['user_id'] for u in users]
assert len(set(user_ids)) == 10
```

## Best Practices

### 1. Use Factories for New Tests

Always use factories instead of hardcoded values in new tests:

```python
def test_new_verification():
    user = user_factory()
    kyc = kyc_verification_factory(user_id=user['user_id'])
    # ... test logic
```

### 2. Use Overrides for Specific Values

Need specific values? Use kwargs:

```python
user = user_factory(kyc_status="VERIFIED")
ktp = ktp_ocr_factory(province="DKI JAKARTA")
```

### 3. Generate Realistic Scenarios

```python
# Trusted user scenario
trusted_user = user_history_factory(
    total_transactions=500,
    total_amount=150000000.0
)

# New user scenario (higher fraud risk)
new_user = user_history_factory(
    total_transactions=2,
    total_amount=100000.0
)
```

### 4. Use in Pytest Fixtures

```python
@pytest.fixture
def test_user():
    return user_factory()

@pytest.fixture
def verified_user():
    return user_factory(kyc_status="VERIFIED")
```

## Examples

See `test_factory_usage_example.py` for comprehensive examples of factory usage.

## Migration Guide

To migrate existing tests to use factories:

1. **Replace hardcoded data with factories:**
   ```python
   # Before
   user = {"user_id": "user_123", "email": "test@example.com"}

   # After
   user = user_factory()
   ```

2. **Use overrides for specific values:**
   ```python
   # Before
   user = {"user_id": "user_123", "kyc_status": "VERIFIED"}

   # After
   user = user_factory(kyc_status="VERIFIED")
   ```

3. **Generate multiple instances:**
   ```python
   # Before
   users = [
       {"user_id": f"user_{i}", "email": f"user{i}@example.com"}
       for i in range(10)
   ]

   # After
   users = [user_factory() for _ in range(10)]
   ```

## Notes

- All factories use Faker with Indonesian locale (`id_ID`) for realistic data
- NIK generation follows Indonesian format but may not be valid NIKs
- Image data factories return minimal 1x1 pixel PNG images for testing
- All generated data is random and unique on each call
