"""
Analytics Test Data Factory

Factory functions for generating analytics-related test data using Faker.
Provides realistic variations for transactions, user metrics, fraud scenarios,
and investment data.

Patterns:
- Use Faker for realistic data (names, addresses, dates)
- Support kwargs for overrides
- Return Pydantic models when applicable
- Support edge case generation for testing
"""

import random
from datetime import datetime, timedelta
from typing import Optional, Dict, Any, List
from faker import Faker
from decimal import Decimal

fake = Faker('id_ID')


def transaction_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate test transaction data with realistic values.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with transaction data:
            - transaction_id: Unique transaction identifier
            - user_id: User identifier
            - amount: Transaction amount in IDR
            - currency: Currency code (default: IDR)
            - type: Transaction type (TRANSFER, PAYMENT, etc.)
            - category: Transaction category
            - metadata: Optional metadata (IP, device, location)

    Example:
        >>> txn = transaction_factory()
        >>> txn['amount']
        500000.0
        >>> high_value = transaction_factory(amount=25000000.0, type='TRANSFER')
    """
    transaction_type = kwargs.get('type', random.choice([
        "TRANSFER", "PAYMENT", "TOPUP", "BILL_PAYMENT",
        "QR_PAYMENT", "WITHDRAWAL"
    ]))

    # Generate realistic amount based on transaction type
    if 'amount' not in kwargs:
        if transaction_type == "QR_PAYMENT":
            amount = fake.pyfloat(left_digits=8, right_digits=2, positive=True, min_value=1000, max_value=10000000)
        elif transaction_type == "BILL_PAYMENT":
            amount = fake.pyfloat(left_digits=7, right_digits=2, positive=True, min_value=50000, max_value=5000000)
        elif transaction_type == "TRANSFER":
            amount = fake.pyfloat(left_digits=8, right_digits=2, positive=True, min_value=10000, max_value=50000000)
        else:
            amount = fake.pyfloat(left_digits=8, right_digits=2, positive=True, min_value=10000, max_value=20000000)
    else:
        amount = kwargs['amount']

    defaults = {
        "transaction_id": f"txn_{fake.uuid4()[:12]}",
        "user_id": f"user_{fake.uuid4()[:8]}",
        "amount": round(amount, 2),
        "currency": "IDR",
        "type": transaction_type,
        "category": random.choice([
            "FOOD", "TRANSPORT", "SHOPPING", "BILLS",
            "ENTERTAINMENT", "HEALTH", "EDUCATION", "TRANSFER", "OTHER"
        ]),
        "recipient_id": f"recipient_{fake.uuid4()[:8]}" if random.choice([True, False]) else None,
        "merchant_id": f"merchant_{fake.uuid4()[:8]}" if random.choice([True, False]) else None,
        "metadata": {
            "ip_address": fake.ipv4(),
            "device_id": f"device_{fake.uuid4()[:8]}",
            "location": f"{fake.city()}, Indonesia",
            "user_agent": fake.user_agent(),
        } if random.choice([True, True, False]) else {},  # 66% have metadata
    }
    defaults.update(kwargs)

    # Ensure metadata is properly merged
    if 'metadata' in kwargs and isinstance(kwargs['metadata'], dict):
        defaults['metadata'].update(kwargs['metadata'])

    return defaults


def user_history_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate user transaction history data for fraud analysis.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with user history:
            - total_transactions: Total number of transactions
            - total_amount: Total amount transacted
            - average_transaction: Average transaction amount
            - account_created_at: Account creation date
            - recent_transactions: List of recent transactions

    Example:
        >>> history = user_history_factory()
        >>> history['total_transactions']
        50
        >>> new_user = user_history_factory(total_transactions=2, total_amount=100000.0)
    """
    total_transactions = kwargs.get('total_transactions', fake.random_int(min=5, max=500))
    total_amount = kwargs.get('total_amount', fake.pyfloat(left_digits=8, right_digits=2, positive=True, min_value=500000, max_value=50000000))
    average_transaction = kwargs.get('average_transaction', round(total_amount / max(total_transactions, 1), 2))

    # Generate recent transactions
    num_recent = kwargs.get('num_recent', min(20, total_transactions))
    recent_transactions = []
    for i in range(num_recent):
        txn_time = datetime.utcnow() - timedelta(hours=i*2)
        recent_transactions.append({
            "transaction_id": f"txn_{fake.uuid4()[:12]}",
            "amount": fake.pyfloat(left_digits=7, right_digits=2, positive=True, min_value=50000, max_value=5000000),
            "type": random.choice(["TRANSFER", "PAYMENT", "TOPUP"]),
            "timestamp": txn_time.isoformat(),
            "recipient_id": f"recipient_{fake.random_int(min=1, max=100)}",
        })

    defaults = {
        "total_transactions": total_transactions,
        "total_amount": round(total_amount, 2),
        "average_transaction": average_transaction,
        "account_created_at": fake.date_time_between(start_date='-2y', end_date='-1d').isoformat(),
        "recent_transactions": recent_transactions,
    }
    defaults.update(kwargs)
    return defaults


def user_metrics_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate user metrics for analytics dashboard.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with user metrics matching UserMetricsResponse schema:
            - user_id: User identifier
            - total_transactions: Total number of transactions
            - total_amount: Total amount transacted
            - average_transaction: Average transaction amount
            - last_transaction_date: Last transaction date
            - account_age_days: Account age in days
            - kyc_status: KYC verification status

    Example:
        >>> metrics = user_metrics_factory()
        >>> metrics['total_transactions']
        150
    """
    account_age_days = fake.random_int(min=30, max=730)  # 1 month to 2 years

    defaults = {
        "user_id": f"user_{fake.uuid4()[:8]}",
        "total_transactions": fake.random_int(min=10, max=500),
        "total_amount": round(fake.pyfloat(left_digits=7, right_digits=0, positive=True, min_value=1000000, max_value=100000000), 2),
        "average_transaction": round(fake.pyfloat(left_digits=5, right_digits=0, positive=True, min_value=100000, max_value=500000), 2),
        "last_transaction_date": fake.date_time_between(start_date='-7d', end_date='now'),
        "account_age_days": account_age_days,
        "kyc_status": random.choice(["VERIFIED", "PENDING", "UNVERIFIED"]),
    }
    defaults.update(kwargs)
    return defaults


def fraud_score_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate fraud score data for testing fraud detection.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with fraud score matching FraudScore schema:
            - transaction_id: Transaction identifier
            - user_id: User identifier
            - risk_score: Fraud risk score (0-100)
            - risk_level: Risk level category
            - risk_factors: Individual risk factor scores
            - is_suspicious: Whether transaction is suspicious
            - recommended_action: Recommended action

    Example:
        >>> score = fraud_score_factory(risk_level="HIGH")
        >>> score['risk_score']
        75
    """
    risk_level = kwargs.get('risk_level', random.choice([
        "MINIMAL", "LOW", "MEDIUM", "HIGH", "CRITICAL"
    ]))

    # Map risk level to score range
    risk_score_ranges = {
        "MINIMAL": (0, 20),
        "LOW": (20, 40),
        "MEDIUM": (40, 60),
        "HIGH": (60, 85),
        "CRITICAL": (85, 100)
    }

    min_score, max_score = risk_score_ranges[risk_level]
    risk_score = kwargs.get('risk_score', fake.pyfloat(left_digits=2, right_digits=0, positive=True, min_value=min_score, max_value=max_score))

    defaults = {
        "transaction_id": f"txn_{fake.uuid4()[:12]}",
        "user_id": f"user_{fake.uuid4()[:8]}",
        "risk_score": round(risk_score, 2),
        "risk_level": risk_level,
        "risk_factors": {
            "amount_anomaly": fake.pyfloat(left_digits=3, right_digits=2, positive=True, min_value=0.1, max_value=100),
            "location_anomaly": fake.pyfloat(left_digits=3, right_digits=2, positive=True, min_value=0.1, max_value=100),
            "frequency_anomaly": fake.pyfloat(left_digits=3, right_digits=2, positive=True, min_value=0.1, max_value=100),
            "device_anomaly": fake.pyfloat(left_digits=3, right_digits=2, positive=True, min_value=0.1, max_value=100),
        },
        "is_suspicious": risk_level in ["HIGH", "CRITICAL"],
        "recommended_action": random.choice([
            "APPROVE", "REVIEW", "BLOCK", "REQUIRE_MFA"
        ] if risk_level == "MINIMAL" else [
            "REVIEW", "BLOCK", "REQUIRE_MFA"
        ]),
    }
    defaults.update(kwargs)
    return defaults


def fraud_detection_result_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate complete fraud detection result with all components.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with complete fraud detection result:
            - fraud_score: FraudScore data
            - is_blocked: Whether transaction is blocked
            - requires_review: Whether transaction requires review
            - rule_triggers: List of triggered fraud rules

    Example:
        >>> result = fraud_detection_result_factory(risk_level="CRITICAL")
        >>> result['is_blocked']
        True
    """
    fraud_score = fraud_score_factory(**kwargs.get('fraud_score', {}))
    risk_level = fraud_score['risk_level']

    defaults = {
        "fraud_score": fraud_score,
        "is_blocked": risk_level == "CRITICAL",
        "requires_review": risk_level in ["HIGH", "MEDIUM"],
        "rule_triggers": fake.random_elements(
            elements=("HIGH_VALUE_TRANSACTION", "SUSPICIOUS_LOCATION", "NEW_DEVICE",
                     "RAPID_TRANSACTIONS", "FOREIGN_TRANSACTION", "UNUSUAL_HOURS"),
            length=fake.random_int(min=0, max=3)
        ) if risk_level in ["MEDIUM", "HIGH", "CRITICAL"] else [],
    }
    defaults.update(kwargs)
    return defaults


def spending_pattern_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate spending pattern data for analytics.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with spending pattern matching SpendingPattern schema:
            - category: Spending category
            - amount: Amount spent
            - percentage: Percentage of total spending
            - transaction_count: Number of transactions
            - trend: Spending trend direction

    Example:
        >>> pattern = spending_pattern_factory(category="FOOD")
        >>> pattern['amount']
        1500000.0
    """
    defaults = {
        "category": kwargs.get('category', random.choice([
            "FOOD", "TRANSPORT", "SHOPPING", "BILLS",
            "ENTERTAINMENT", "HEALTH", "EDUCATION", "OTHER"
        ])),
        "amount": round(fake.pyfloat(left_digits=6, right_digits=0, positive=True, min_value=100000, max_value=10000000), 2),
        "percentage": round(fake.pyfloat(left_digits=1, right_digits=2, positive=True, min_value=1, max_value=100), 2),
        "transaction_count": fake.random_int(min=5, max=100),
        "trend": random.choice(["increasing", "decreasing", "stable"]),
    }
    defaults.update(kwargs)
    return defaults


def recommendation_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate recommendation data for user insights.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with recommendation matching Recommendation schema:
            - recommendation_id: Unique recommendation identifier
            - recommendation_type: Type of recommendation
            - title: Recommendation title
            - description: Detailed description
            - action_url: Optional action URL
            - priority: Priority score (higher = more important)
            - metadata: Additional metadata

    Example:
        >>> rec = recommendation_factory(recommendation_type="SAVINGS_GOAL")
        >>> rec['priority']
        5
    """
    rec_type = kwargs.get('recommendation_type', random.choice([
        "SAVINGS_GOAL", "BUDGET_ALERT", "SPENDING_TREND",
        "NEW_FEATURE", "PROMOTION", "INVESTMENT"
    ]))

    # Type-specific content
    type_content = {
        "SAVINGS_GOAL": {
            "title": "Set Up Your Savings Goal",
            "description": fake.sentence(),
        },
        "BUDGET_ALERT": {
            "title": "Budget Alert: Spending Exceeded",
            "description": fake.sentence(),
        },
        "SPENDING_TREND": {
            "title": "Your Spending This Month",
            "description": fake.sentence(),
        },
        "NEW_FEATURE": {
            "title": "Try Our New Feature",
            "description": fake.sentence(),
        },
        "PROMOTION": {
            "title": "Special Offer Just For You",
            "description": fake.sentence(),
        },
        "INVESTMENT": {
            "title": "Start Investing Today",
            "description": fake.sentence(),
        }
    }

    content = type_content[rec_type]

    defaults = {
        "recommendation_id": f"rec_{fake.uuid4()[:12]}",
        "recommendation_type": rec_type,
        "title": kwargs.get('title', content['title']),
        "description": kwargs.get('description', content['description']),
        "action_url": fake.url() if random.choice([True, False]) else None,
        "priority": fake.random_int(min=1, max=10),
        "metadata": {
            "category": rec_type,
            "created_at": fake.date_time_between(start_date='-30d', end_date='now').isoformat(),
        }
    }
    defaults.update(kwargs)
    return defaults


def risk_assessment_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate risk assessment data for robo-advisory.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with risk assessment matching RiskAssessmentQuestions schema:
            - age: User's age (18-100)
            - monthly_income: Monthly income in IDR
            - monthly_expenses: Monthly expenses in IDR
            - total_savings: Total savings in IDR
            - investment_experience: Years of experience (0-10)
            - risk_tolerance: Risk tolerance level
            - investment_goal: Investment goal
            - time_horizon: Investment time horizon

    Example:
        >>> assessment = risk_assessment_factory(risk_tolerance="high")
        >>> assessment['age']
        35
    """
    age = fake.random_int(min=18, max=65)
    monthly_income = fake.pyfloat(left_digits=7, right_digits=0, positive=True, min_value=5000000, max_value=100000000)

    defaults = {
        "age": age,
        "monthly_income": round(monthly_income, 2),
        "monthly_expenses": round(monthly_income * fake.pyfloat(left_digits=1, right_digits=2, positive=True, min_value=0.3, max_value=0.8), 2),
        "total_savings": round(fake.pyfloat(left_digits=8, right_digits=0, positive=True, min_value=10000000, max_value=500000000), 2),
        "investment_experience": fake.random_int(min=0, max=10),
        "risk_tolerance": random.choice(["low", "medium", "high"]),
        "investment_goal": random.choice(["retirement", "wealth_growth", "emergency_fund"]),
        "time_horizon": random.choice(["SHORT_TERM", "MEDIUM_TERM", "LONG_TERM"]),
    }
    defaults.update(kwargs)
    return defaults


def portfolio_allocation_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate portfolio allocation data for investment recommendations.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with portfolio allocation matching PortfolioAllocation schema:
            - asset_class: Asset class type
            - allocation_percentage: Allocation percentage (0-100)
            - expected_return: Expected annual return percentage
            - risk_level: Risk profile
            - description: Asset class description

    Example:
        >>> allocation = portfolio_allocation_factory(asset_class="MUTUAL_FUNDS")
        >>> allocation['allocation_percentage']
        40.0
    """
    asset_class = kwargs.get('asset_class', random.choice([
        "CASH", "FIXED_INCOME", "MUTUAL_FUNDS",
        "DIGITAL_GOLD", "STOCKS", "BONDS"
    ]))

    # Asset class specific data
    asset_data = {
        "CASH": {"return": (3, 5), "risk": "CONSERVATIVE"},
        "FIXED_INCOME": {"return": (5, 8), "risk": "CONSERVATIVE"},
        "MUTUAL_FUNDS": {"return": (8, 15), "risk": "MODERATE"},
        "DIGITAL_GOLD": {"return": (5, 12), "risk": "MODERATE"},
        "STOCKS": {"return": (10, 25), "risk": "AGGRESSIVE"},
        "BONDS": {"return": (6, 10), "risk": "CONSERVATIVE"},
    }

    data = asset_data[asset_class]

    defaults = {
        "asset_class": asset_class,
        "allocation_percentage": round(fake.pyfloat(left_digits=2, right_digits=0, positive=True, min_value=5, max_value=50), 2),
        "expected_return": round(fake.pyfloat(left_digits=2, right_digits=0, positive=True, min_value=data['return'][0], max_value=data['return'][1]), 2),
        "risk_level": data['risk'],
        "description": fake.sentence(),
    }
    defaults.update(kwargs)
    return defaults


def robo_advisory_response_factory(**kwargs) -> Dict[str, Any]:
    """
    Generate complete robo-advisory response with all components.

    Args:
        **kwargs: Override default values

    Returns:
        Dict with complete robo-advisory response:
            - user_id: User identifier
            - risk_assessment: Risk assessment result
            - portfolio_allocation: List of portfolio allocations
            - investment_recommendations: List of recommendations
            - monthly_investment_amount: Monthly investment amount
            - expected_annual_return: Expected annual return
            - recommended_investment_products: List of recommended products

    Example:
        >>> response = robo_advisory_response_factory()
        >>> len(response['portfolio_allocation'])
        4
    """
    risk_profile = random.choice(["CONSERVATIVE", "MODERATE", "AGGRESSIVE"])

    # Generate portfolio based on risk profile
    if risk_profile == "CONSERVATIVE":
        allocations = [
            portfolio_allocation_factory(asset_class="CASH", allocation_percentage=30),
            portfolio_allocation_factory(asset_class="FIXED_INCOME", allocation_percentage=40),
            portfolio_allocation_factory(asset_class="BONDS", allocation_percentage=30),
        ]
        expected_return = 6.5
    elif risk_profile == "MODERATE":
        allocations = [
            portfolio_allocation_factory(asset_class="CASH", allocation_percentage=15),
            portfolio_allocation_factory(asset_class="FIXED_INCOME", allocation_percentage=25),
            portfolio_allocation_factory(asset_class="MUTUAL_FUNDS", allocation_percentage=40),
            portfolio_allocation_factory(asset_class="DIGITAL_GOLD", allocation_percentage=20),
        ]
        expected_return = 12.0
    else:  # AGGRESSIVE
        allocations = [
            portfolio_allocation_factory(asset_class="MUTUAL_FUNDS", allocation_percentage=30),
            portfolio_allocation_factory(asset_class="STOCKS", allocation_percentage=50),
            portfolio_allocation_factory(asset_class="DIGITAL_GOLD", allocation_percentage=20),
        ]
        expected_return = 18.0

    defaults = {
        "user_id": f"user_{fake.uuid4()[:8]}",
        "risk_assessment": {
            "risk_profile": risk_profile,
            "risk_score": round(fake.pyfloat(left_digits=2, right_digits=0, positive=True, min_value=30, max_value=80), 2),
            "description": fake.paragraph(),
            "suitable_asset_classes": [alloc['asset_class'] for alloc in allocations],
        },
        "portfolio_allocation": allocations,
        "investment_recommendations": [
            fake.sentence() for _ in range(3)
        ],
        "monthly_investment_amount": round(fake.pyfloat(left_digits=7, right_digits=0, positive=True, min_value=1000000, max_value=20000000), 2),
        "expected_annual_return": round(expected_return + fake.pyfloat(left_digits=1, right_digits=2, positive=True, min_value=-2, max_value=2), 2),
        "recommended_investment_products": [
            {
                "product_id": f"prod_{fake.uuid4()[:8]}",
                "product_name": fake.company(),
                "minimum_investment": round(fake.pyfloat(left_digits=6, right_digits=0, positive=True, min_value=100000, max_value=5000000), 2),
                "expected_return": round(fake.pyfloat(left_digits=2, right_digits=0, positive=True, min_value=5, max_value=20), 2),
            }
            for _ in range(fake.random_int(min=2, max=5))
        ]
    }
    defaults.update(kwargs)
    return defaults
