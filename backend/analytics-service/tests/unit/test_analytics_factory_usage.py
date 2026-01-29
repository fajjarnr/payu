"""
Example test demonstrating factory pattern usage for Analytics Service.

This file shows how to use the factory functions to generate test data
for analytics, fraud detection, and robo-advisory testing.

Benefits:
- Realistic transaction data variation
- Easier fraud scenario testing
- Reduced test code duplication
- Better edge case coverage
"""

import sys
import pytest
from datetime import datetime, timedelta

sys.path.insert(0, "/home/ubuntu/payu/backend/analytics-service/src")

from tests.factories import (
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


@pytest.mark.unit
class TestTransactionFactoryUsage:
    """Demonstrate transaction factory usage"""

    def test_transaction_factory_generates_unique_transactions(self):
        """Test that transaction_factory generates unique data"""
        txn1 = transaction_factory()
        txn2 = transaction_factory()

        assert txn1["transaction_id"] != txn2["transaction_id"]
        assert txn1["user_id"] != txn2["user_id"]

    def test_transaction_factory_with_different_types(self):
        """Test generating different transaction types"""
        transfer = transaction_factory(type="TRANSFER")
        payment = transaction_factory(type="PAYMENT")
        qris = transaction_factory(type="QR_PAYMENT")

        assert transfer["type"] == "TRANSFER"
        assert payment["type"] == "PAYMENT"
        assert qris["type"] == "QR_PAYMENT"

    def test_transaction_factory_for_fraud_scenarios(self):
        """Test generating transactions for fraud testing"""
        # High-value transaction (potential fraud)
        high_value = transaction_factory(
            amount=25000000.0,
            type="TRANSFER",
            metadata={"location": "Singapore", "ip_address": "203.0.113.1"},
        )
        assert high_value["amount"] == 25000000.0

        # Low-value transaction (normal)
        low_value = transaction_factory(amount=50000.0, type="PAYMENT")
        assert low_value["amount"] == 50000.0

    def test_transaction_factory_with_metadata(self):
        """Test transaction with rich metadata for fraud analysis"""
        txn = transaction_factory(
            metadata={
                "ip_address": "192.168.1.1",
                "device_id": "device_123",
                "location": "Jakarta, Indonesia",
                "is_vpn": False,
            }
        )

        assert txn["metadata"]["ip_address"] == "192.168.1.1"
        assert txn["metadata"]["is_vpn"] is False


@pytest.mark.unit
class TestUserHistoryFactoryUsage:
    """Demonstrate user history factory for fraud analysis"""

    def test_user_history_factory_for_trusted_users(self):
        """Test generating history for trusted/established users"""
        trusted_user = user_history_factory(
            total_transactions=500,
            total_amount=150000000.0,
            account_created_at=(datetime.utcnow() - timedelta(days=730)).isoformat(),
        )

        assert trusted_user["total_transactions"] == 500
        assert len(trusted_user["recent_transactions"]) > 0

    def test_user_history_factory_for_new_users(self):
        """Test generating history for new users (higher fraud risk)"""
        new_user = user_history_factory(
            total_transactions=2, total_amount=100000.0, num_recent=1
        )

        assert new_user["total_transactions"] == 2
        assert len(new_user["recent_transactions"]) == 1

    def test_user_history_factory_for_high_frequency_users(self):
        """Test generating history for high-frequency transaction users"""
        high_freq_user = user_history_factory(total_transactions=300, num_recent=30)

        assert len(high_freq_user["recent_transactions"]) == 30


@pytest.mark.unit
class TestFraudScoreFactoryUsage:
    """Demonstrate fraud score factory for testing"""

    def test_fraud_score_factory_all_risk_levels(self):
        """Test generating fraud scores for all risk levels"""
        minimal = fraud_score_factory(risk_level="MINIMAL")
        low = fraud_score_factory(risk_level="LOW")
        medium = fraud_score_factory(risk_level="MEDIUM")
        high = fraud_score_factory(risk_level="HIGH")
        critical = fraud_score_factory(risk_level="CRITICAL")

        assert minimal["risk_level"] == "MINIMAL"
        assert minimal["risk_score"] < 20

        assert critical["risk_level"] == "CRITICAL"
        assert critical["risk_score"] >= 85

    def test_fraud_score_factory_with_custom_scores(self):
        """Test generating fraud scores with specific values"""
        score = fraud_score_factory(
            risk_score=75.5, risk_level="HIGH", is_suspicious=True
        )

        assert score["risk_score"] == 75.5
        assert score["is_suspicious"] is True

    def test_fraud_detection_result_factory(self):
        """Test complete fraud detection result"""
        result = fraud_detection_result_factory(risk_level="CRITICAL")

        assert result["fraud_score"]["risk_level"] == "CRITICAL"
        assert result["is_blocked"] is True
        assert isinstance(result["rule_triggers"], list)


@pytest.mark.unit
class TestAnalyticsFactoryUsage:
    """Demonstrate analytics factory usage"""

    def test_user_metrics_factory(self):
        """Test generating user metrics"""
        metrics = user_metrics_factory()

        assert metrics["total_transactions"] > 0
        assert metrics["account_age_days"] > 0
        assert metrics["kyc_status"] in ["VERIFIED", "PENDING", "UNVERIFIED"]

    def test_spending_pattern_factory(self):
        """Test generating spending patterns"""
        food_pattern = spending_pattern_factory(category="FOOD")
        transport_pattern = spending_pattern_factory(category="TRANSPORT")

        assert food_pattern["category"] == "FOOD"
        assert transport_pattern["category"] == "TRANSPORT"
        assert food_pattern["trend"] in ["increasing", "decreasing", "stable"]

    def test_recommendation_factory(self):
        """Test generating recommendations"""
        savings_rec = recommendation_factory(recommendation_type="SAVINGS_GOAL")
        investment_rec = recommendation_factory(recommendation_type="INVESTMENT")

        assert savings_rec["recommendation_type"] == "SAVINGS_GOAL"
        assert investment_rec["recommendation_type"] == "INVESTMENT"
        assert savings_rec["priority"] >= 1


@pytest.mark.unit
class TestRoboAdvisoryFactoryUsage:
    """Demonstrate robo-advisory factory usage"""

    def test_risk_assessment_factory(self):
        """Test generating risk assessment data"""
        conservative = risk_assessment_factory(risk_tolerance="low")
        aggressive = risk_assessment_factory(risk_tolerance="high")

        assert conservative["risk_tolerance"] == "low"
        assert aggressive["risk_tolerance"] == "high"
        assert conservative["age"] >= 18

    def test_portfolio_allocation_factory(self):
        """Test generating portfolio allocations"""
        cash = portfolio_allocation_factory(asset_class="CASH")
        stocks = portfolio_allocation_factory(asset_class="STOCKS")

        assert cash["asset_class"] == "CASH"
        assert stocks["asset_class"] == "STOCKS"
        assert 0 <= cash["allocation_percentage"] <= 100
        assert 0 <= stocks["allocation_percentage"] <= 100

    def test_robo_advisory_response_factory(self):
        """Test complete robo-advisory response"""
        response = robo_advisory_response_factory()

        assert "risk_assessment" in response
        assert "portfolio_allocation" in response
        assert len(response["portfolio_allocation"]) > 0
        assert response["monthly_investment_amount"] > 0


@pytest.mark.unit
class TestFactoryPatternBenefits:
    """Demonstrate benefits of using factory patterns"""

    def test_factory_reduces_code_duplication(self):
        """Show how factories reduce test code duplication"""
        # Without factory - verbose
        txn1 = {
            "transaction_id": "txn_001",
            "user_id": "user_001",
            "amount": 500000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "category": "FOOD",
        }

        # With factory - concise
        txn2 = transaction_factory(type="TRANSFER", category="FOOD")

        # Factory is simpler and generates unique data
        assert "transaction_id" in txn1
        assert "transaction_id" in txn2

    def test_factory_enables_batch_generation(self):
        """Test generating multiple test instances easily"""
        # Generate 20 transactions for bulk testing
        transactions = [transaction_factory() for _ in range(20)]

        txn_ids = [t["transaction_id"] for t in transactions]
        assert len(set(txn_ids)) == 20  # All unique

    def test_factory_simplifies_edge_case_testing(self):
        """Test that factories make edge case testing easier"""
        # Edge case: Zero amount transaction
        zero_txn = transaction_factory(amount=0.0)
        assert zero_txn["amount"] == 0.0

        # Edge case: Extremely high amount
        extreme_txn = transaction_factory(amount=999999999.0)
        assert extreme_txn["amount"] == 999999999.0

        # Edge case: New user with no history
        new_user = user_history_factory(
            total_transactions=0, total_amount=0.0, num_recent=0
        )
        assert new_user["total_transactions"] == 0
        assert len(new_user["recent_transactions"]) == 0

    def test_factory_generates_realistic_test_data(self):
        """Test that factories generate realistic Indonesian data"""
        user = user_metrics_factory()

        # Account age should be realistic (1 month to 2 years)
        assert 30 <= user["account_age_days"] <= 730

        # Transaction amounts should be realistic
        txn = transaction_factory()
        assert 1000 <= txn["amount"] <= 50000000

    @pytest.mark.asyncio
    async def test_factory_integration_with_service_tests(self):
        """Test using factory data with actual service tests"""

        # Generate test transaction using factory
        test_txn = transaction_factory(
            amount=5000000.0, type="TRANSFER", metadata={"ip_address": "192.168.1.1"}
        )

        # Generate test user history using factory
        test_history = user_history_factory(
            total_transactions=50, total_amount=15000000.0
        )

        # Verify data structure is compatible with service
        assert "transaction_id" in test_txn
        assert "amount" in test_txn
        assert "recent_transactions" in test_history
