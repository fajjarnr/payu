"""
Test Data Factories for Analytics Service

This module provides factory functions for generating test data using Faker.
Factories help create realistic test data variations for analytics, fraud detection,
and robo-advisory testing.

Usage:
    from tests.factories import (
        transaction_factory,
        user_metrics_factory,
        fraud_score_factory,
        risk_assessment_factory
    )

    # Generate random transaction
    txn = transaction_factory()

    # Generate with overrides
    high_value_txn = transaction_factory(amount=25000000.0)
"""

from .analytics_factory import (
    transaction_factory,
    user_history_factory,
    user_metrics_factory,
    fraud_score_factory,
    fraud_detection_result_factory,
    spending_pattern_factory,
    recommendation_factory,
    risk_assessment_factory,
    portfolio_allocation_factory,
    robo_advisory_response_factory,
)

__all__ = [
    "transaction_factory",
    "user_history_factory",
    "user_metrics_factory",
    "fraud_score_factory",
    "fraud_detection_result_factory",
    "spending_pattern_factory",
    "recommendation_factory",
    "risk_assessment_factory",
    "portfolio_allocation_factory",
    "robo_advisory_response_factory",
]
