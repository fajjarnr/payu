# Test Data Factories for Analytics Service

## Overview

This module provides factory functions for generating realistic test data for analytics, fraud detection, and robo-advisory testing using the Faker library.

## Installation

The `faker` library has been added to `requirements.txt`:

```bash
pip install faker==30.0.0
```

## Usage

### Basic Usage

```python
from tests.factories import transaction_factory, user_metrics_factory

# Generate a random transaction
txn = transaction_factory()
print(txn['transaction_id'])  # e.g., "txn_a1b2c3d4e5f6"
print(txn['amount'])          # e.g., 500000.0
print(txn['type'])            # e.g., "TRANSFER"

# Generate with overrides
high_value_txn = transaction_factory(
    amount=25000000.0,
    type="TRANSFER"
)
```

### Transaction Testing

```python
from tests.factories import transaction_factory

# Generate different transaction types
transfer = transaction_factory(type="TRANSFER")
payment = transaction_factory(type="PAYMENT")
qris = transaction_factory(type="QR_PAYMENT")

# Generate with metadata for fraud testing
suspicious_txn = transaction_factory(
    amount=50000000.0,
    metadata={
        "ip_address": "203.0.113.1",
        "location": "Singapore",
        "is_vpn": True
    }
)
```

### Fraud Detection Testing

```python
from tests.factories import (
    fraud_score_factory,
    fraud_detection_result_factory,
    user_history_factory
)

# Generate fraud scores for different risk levels
minimal = fraud_score_factory(risk_level="MINIMAL")
critical = fraud_score_factory(risk_level="CRITICAL")

# Generate complete fraud detection result
result = fraud_detection_result_factory(risk_level="HIGH")
print(result['is_blocked'])        # True
print(result['requires_review'])   # True

# Generate user history for fraud analysis
trusted_user = user_history_factory(
    total_transactions=500,
    total_amount=150000000.0
)
```

### Analytics Testing

```python
from tests.factories import (
    user_metrics_factory,
    spending_pattern_factory,
    recommendation_factory
)

# Generate user metrics
metrics = user_metrics_factory()
print(metrics['total_transactions'])  # e.g., 150
print(metrics['account_age_days'])     # e.g., 365

# Generate spending patterns
food_spending = spending_pattern_factory(category="FOOD")
transport_spending = spending_pattern_factory(category="TRANSPORT")

# Generate recommendations
savings_goal = recommendation_factory(
    recommendation_type="SAVINGS_GOAL"
)
```

### Robo-Advisory Testing

```python
from tests.factories import (
    risk_assessment_factory,
    portfolio_allocation_factory,
    robo_advisory_response_factory
)

# Generate risk assessment
assessment = risk_assessment_factory(
    risk_tolerance="high",
    investment_goal="wealth_growth"
)

# Generate portfolio allocation
stocks = portfolio_allocation_factory(asset_class="STOCKS")
bonds = portfolio_allocation_factory(asset_class="BONDS")

# Generate complete robo-advisory response
response = robo_advisory_response_factory()
print(response['risk_assessment']['risk_profile'])  # e.g., "MODERATE"
print(len(response['portfolio_allocation']))        # e.g., 4
```

## Available Factories

### `transaction_factory(**kwargs)`

Generate test transaction data with realistic values.

**Parameters:**
- `transaction_id` (str): Unique transaction identifier
- `user_id` (str): User identifier
- `amount` (float): Transaction amount in IDR
- `currency` (str): Currency code (default: "IDR")
- `type` (str): Transaction type
- `category` (str): Transaction category
- `metadata` (dict): Optional metadata (IP, device, location)

**Example:**
```python
txn = transaction_factory(
    type="TRANSFER",
    amount=5000000.0,
    metadata={"ip_address": "192.168.1.1"}
)
```

### `user_history_factory(**kwargs)`

Generate user transaction history data for fraud analysis.

**Parameters:**
- `total_transactions` (int): Total number of transactions
- `total_amount` (float): Total amount transacted
- `average_transaction` (float): Average transaction amount
- `account_created_at` (str): Account creation date
- `num_recent` (int): Number of recent transactions to generate

**Example:**
```python
# Trusted user
trusted = user_history_factory(
    total_transactions=500,
    total_amount=150000000.0
)

# New user
new_user = user_history_factory(
    total_transactions=2,
    total_amount=100000.0,
    num_recent=1
)
```

### `fraud_score_factory(**kwargs)`

Generate fraud score data for testing fraud detection.

**Parameters:**
- `transaction_id` (str): Transaction identifier
- `user_id` (str): User identifier
- `risk_score` (float): Fraud risk score (0-100)
- `risk_level` (str): Risk level (MINIMAL, LOW, MEDIUM, HIGH, CRITICAL)
- `risk_factors` (dict): Individual risk factor scores
- `is_suspicious` (bool): Whether transaction is suspicious

**Example:**
```python
# High risk transaction
high_risk = fraud_score_factory(
    risk_level="HIGH",
    risk_score=75.0
)

# Critical risk
critical = fraud_score_factory(
    risk_level="CRITICAL",
    risk_score=95.0
)
```

### `fraud_detection_result_factory(**kwargs)`

Generate complete fraud detection result with all components.

**Parameters:**
- `fraud_score` (dict): FraudScore data
- `is_blocked` (bool): Whether transaction is blocked
- `requires_review` (bool): Whether transaction requires review
- `rule_triggers` (list): List of triggered fraud rules

**Example:**
```python
result = fraud_detection_result_factory(
    risk_level="CRITICAL"
)
# is_blocked will be True for CRITICAL
```

### `user_metrics_factory(**kwargs)`

Generate user metrics for analytics dashboard.

**Parameters:**
- `user_id` (str): User identifier
- `total_transactions` (int): Total number of transactions
- `total_amount` (float): Total amount transacted
- `average_transaction` (float): Average transaction amount
- `account_age_days` (int): Account age in days
- `kyc_status` (str): KYC verification status

**Example:**
```python
metrics = user_metrics_factory(
    total_transactions=150,
    account_age_days=365
)
```

### `spending_pattern_factory(**kwargs)`

Generate spending pattern data for analytics.

**Parameters:**
- `category` (str): Spending category
- `amount` (float): Amount spent
- `percentage` (float): Percentage of total spending
- `transaction_count` (int): Number of transactions
- `trend` (str): Spending trend (increasing, decreasing, stable)

**Example:**
```python
food_pattern = spending_pattern_factory(
    category="FOOD",
    trend="increasing"
)
```

### `recommendation_factory(**kwargs)`

Generate recommendation data for user insights.

**Parameters:**
- `recommendation_id` (str): Unique recommendation identifier
- `recommendation_type` (str): Type of recommendation
- `title` (str): Recommendation title
- `description` (str): Detailed description
- `priority` (int): Priority score (1-10)

**Example:**
```python
savings_rec = recommendation_factory(
    recommendation_type="SAVINGS_GOAL",
    priority=8
)
```

### `risk_assessment_factory(**kwargs)`

Generate risk assessment data for robo-advisory.

**Parameters:**
- `age` (int): User's age (18-100)
- `monthly_income` (float): Monthly income in IDR
- `monthly_expenses` (float): Monthly expenses in IDR
- `total_savings` (float): Total savings in IDR
- `investment_experience` (int): Years of experience (0-10)
- `risk_tolerance` (str): Risk tolerance (low, medium, high)
- `investment_goal` (str): Investment goal
- `time_horizon` (str): Time horizon

**Example:**
```python
assessment = risk_assessment_factory(
    age=35,
    risk_tolerance="high",
    investment_goal="wealth_growth"
)
```

### `portfolio_allocation_factory(**kwargs)`

Generate portfolio allocation data for investment recommendations.

**Parameters:**
- `asset_class` (str): Asset class type
- `allocation_percentage` (float): Allocation percentage (0-100)
- `expected_return` (float): Expected annual return percentage
- `risk_level` (str): Risk profile
- `description` (str): Asset class description

**Example:**
```python
stocks = portfolio_allocation_factory(
    asset_class="STOCKS",
    allocation_percentage=50.0
)
```

### `robo_advisory_response_factory(**kwargs)`

Generate complete robo-advisory response with all components.

**Parameters:**
- `user_id` (str): User identifier
- `risk_assessment` (dict): Risk assessment result
- `portfolio_allocation` (list): List of portfolio allocations
- `investment_recommendations` (list): List of recommendations
- `monthly_investment_amount` (float): Monthly investment amount
- `expected_annual_return` (float): Expected annual return

**Example:**
```python
response = robo_advisory_response_factory()
```

## Benefits

### 1. Reduced Hardcoded Values

**Before:**
```python
txn = {
    "transaction_id": "txn_123",
    "user_id": "user_456",
    "amount": 500000.0,
    "type": "TRANSFER"
}
```

**After:**
```python
txn = transaction_factory()
```

### 2. Realistic Test Data

Factories generate realistic data:
- Indonesian transaction amounts (IDR)
- Realistic transaction types and categories
- Proper risk score ranges
- Valid portfolio allocations

### 3. Easy Edge Case Testing

```python
# Zero amount transaction
zero_txn = transaction_factory(amount=0.0)

# Extremely high amount
extreme_txn = transaction_factory(amount=999999999.0)

# New user with no history
new_user = user_history_factory(
    total_transactions=0,
    num_recent=0
)

# Critical fraud risk
critical = fraud_score_factory(risk_level="CRITICAL")
```

### 4. Batch Test Generation

```python
# Generate 20 unique transactions
transactions = [transaction_factory() for _ in range(20)]

# All transactions are unique
txn_ids = [t['transaction_id'] for t in transactions]
assert len(set(txn_ids)) == 20
```

## Best Practices

### 1. Use Factories for Fraud Scenario Testing

```python
# Normal transaction
normal_txn = transaction_factory(amount=100000.0)

# Suspicious high-value transaction
suspicious_txn = transaction_factory(
    amount=50000000.0,
    metadata={"location": "Singapore", "is_vpn": True}
)

# New user with large transaction
new_user_large = transaction_factory(
    amount=10000000.0,
    user_id="new_user_123"
)
```

### 2. Generate Realistic User Histories

```python
# Trusted user (low fraud risk)
trusted = user_history_factory(
    total_transactions=500,
    total_amount=150000000.0,
    account_created_at="2022-01-01T00:00:00"
)

# New user (high fraud risk)
new_user = user_history_factory(
    total_transactions=2,
    total_amount=100000.0,
    account_created_at="2024-01-25T00:00:00"
)
```

### 3. Test All Risk Levels

```python
for risk_level in ["MINIMAL", "LOW", "MEDIUM", "HIGH", "CRITICAL"]:
    score = fraud_score_factory(risk_level=risk_level)
    # Test behavior for each risk level
```

### 4. Use in Pytest Fixtures

```python
@pytest.fixture
def normal_transaction():
    return transaction_factory(amount=500000.0)

@pytest.fixture
def high_value_transaction():
    return transaction_factory(amount=25000000.0)

@pytest.fixture
def trusted_user_history():
    return user_history_factory(total_transactions=500)
```

## Examples

See `test_factory_usage_example.py` for comprehensive examples of factory usage.

## Common Patterns

### Testing Transaction Processing

```python
def test_process_transaction():
    txn = transaction_factory(type="TRANSFER")
    result = process_transaction(txn)
    assert result.status == "SUCCESS"
```

### Testing Fraud Detection

```python
def test_fraud_detection():
    # Normal transaction
    normal = transaction_factory(amount=100000.0)
    score = fraud_engine.calculate_score(normal)
    assert score.risk_level == "MINIMAL"

    # Suspicious transaction
    suspicious = transaction_factory(
        amount=50000000.0,
        metadata={"location": "Foreign"}
    )
    score = fraud_engine.calculate_score(suspicious)
    assert score.risk_level in ["HIGH", "CRITICAL"]
```

### Testing Analytics

```python
def test_spending_analytics():
    user_id = "user_123"

    # Generate spending data
    patterns = [
        spending_pattern_factory(category="FOOD", amount=1500000.0),
        spending_pattern_factory(category="TRANSPORT", amount=500000.0),
        spending_pattern_factory(category="SHOPPING", amount=2000000.0),
    ]

    analysis = analyze_spending(user_id, patterns)
    assert analysis.total_spending == 4000000.0
```

## Migration Guide

To migrate existing tests to use factories:

1. **Replace hardcoded data with factories:**
   ```python
   # Before
   txn = {"transaction_id": "txn_123", "amount": 500000.0}

   # After
   txn = transaction_factory()
   ```

2. **Use overrides for specific values:**
   ```python
   # Before
   txn = {"transaction_id": "txn_123", "amount": 25000000.0}

   # After
   txn = transaction_factory(amount=25000000.0)
   ```

3. **Generate multiple instances:**
   ```python
   # Before
   txns = [
       {"transaction_id": f"txn_{i}", "amount": 100000.0 * i}
       for i in range(10)
   ]

   # After
   txns = [transaction_factory() for _ in range(10)]
   ```

## Notes

- All factories use Faker with Indonesian locale (`id_ID`) for realistic data
- Transaction amounts are in IDR (Indonesian Rupiah)
- Risk scores follow standard ranges (0-100)
- Portfolio allocations respect risk profiles
- All generated data is random and unique on each call
