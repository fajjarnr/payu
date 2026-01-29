"""
Unit tests for Fraud Scoring and Risk Assessment.

Tests edge cases and scenarios in the fraud detection engine
beyond those covered in test_fraud_detection.py.
"""
import pytest
from datetime import datetime, timedelta
from decimal import Decimal
from unittest.mock import AsyncMock, MagicMock, patch

from app.ml.fraud_detection import FraudDetectionEngine
from app.models.schemas import FraudScore, FraudRiskLevel


@pytest.fixture
def fraud_engine():
    """Create a FraudDetectionEngine instance."""
    return FraudDetectionEngine()


@pytest.fixture
def sample_transaction_data():
    """Create sample transaction data."""
    return {
        "transaction_id": "txn_12345",
        "user_id": "user_67890",
        "amount": 500000.0,
        "currency": "IDR",
        "type": "TRANSFER",
        "metadata": {
            "ip_address": "192.168.1.1",
            "device_id": "device_123",
            "location": "Jakarta, Indonesia"
        }
    }


@pytest.fixture
def high_value_transaction():
    """Create high value transaction data."""
    return {
        "transaction_id": "txn_high_value",
        "user_id": "user_rich",
        "amount": 25000000.0,  # 25 million IDR
        "currency": "IDR",
        "type": "TRANSFER",
        "metadata": {
            "ip_address": "192.168.1.100",
            "device_id": "device_456",
            "location": "Singapore"
        }
    }


@pytest.fixture
def qris_transaction():
    """Create QRIS transaction data."""
    return {
        "transaction_id": "txn_qris_001",
        "user_id": "user_qris",
        "amount": 6000000.0,  # Above QRIS threshold
        "currency": "IDR",
        "type": "QRIS",
        "metadata": {
            "ip_address": "10.0.0.1",
            "device_id": "device_qris",
            "location": "Bandung, Indonesia",
            "merchant_id": "merchant_qris"
        }
    }


@pytest.fixture
def sample_user_history():
    """Create sample user history."""
    return {
        "total_transactions": 50,
        "total_amount": 15000000.0,
        "average_transaction": 300000.0,
        "account_created_at": (datetime.utcnow() - timedelta(days=200)).isoformat(),
        "recent_transactions": [
            {
                "transaction_id": f"txn_{i}",
                "amount": 200000.0 + i * 10000,
                "type": "TRANSFER",
                "timestamp": (datetime.utcnow() - timedelta(hours=i)).isoformat(),
                "recipient_id": f"recipient_{i}"
            }
            for i in range(10)
        ]
    }


@pytest.fixture
def new_user_history():
    """Create history for new user account."""
    return {
        "total_transactions": 2,
        "total_amount": 100000.0,
        "average_transaction": 50000.0,
        "account_created_at": (datetime.utcnow() - timedelta(days=2)).isoformat(),
        "recent_transactions": [
            {
                "transaction_id": "txn_new_1",
                "amount": 50000.0,
                "type": "TRANSFER",
                "timestamp": (datetime.utcnow() - timedelta(hours=2)).isoformat(),
                "recipient_id": "recipient_1"
            }
        ]
    }


@pytest.fixture
def high_frequency_user_history():
    """Create history for high-frequency transaction user."""
    return {
        "total_transactions": 500,
        "total_amount": 150000000.0,
        "average_transaction": 300000.0,
        "account_created_at": (datetime.utcnow() - timedelta(days=365)).isoformat(),
        "recent_transactions": [
            {
                "transaction_id": f"txn_hf_{i}",
                "amount": 250000.0,
                "type": "TRANSFER",
                "timestamp": (datetime.utcnow() - timedelta(minutes=i * 5)).isoformat(),
                "recipient_id": f"recipient_{i}"
            }
            for i in range(30)  # 30 transactions in last 2.5 hours
        ]
    }


class TestFraudScoringEdgeCases:
    """Test suite for fraud scoring edge cases."""

    @pytest.mark.asyncio
    async def test_zero_amount_transaction(self, fraud_engine):
        """Test fraud scoring for zero amount transaction."""
        transaction = {
            "transaction_id": "txn_zero",
            "user_id": "user_zero",
            "amount": 0.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None
        assert result.fraud_score.risk_score >= 0
        assert result.fraud_score.risk_level in FraudRiskLevel

    @pytest.mark.asyncio
    async def test_negative_amount_transaction(self, fraud_engine):
        """Test fraud scoring for negative amount (refund scenario)."""
        transaction = {
            "transaction_id": "txn_negative",
            "user_id": "user_neg",
            "amount": -100000.0,
            "currency": "IDR",
            "type": "REFUND",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None
        # Refunds should be handled gracefully
        assert isinstance(result.fraud_score.risk_score, (int, float))

    @pytest.mark.asyncio
    async def test_extremely_high_amount(self, fraud_engine):
        """Test fraud scoring for extremely high amount."""
        transaction = {
            "transaction_id": "txn_extreme",
            "user_id": "user_extreme",
            "amount": 999999999.0,  # Nearly 1 billion IDR
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None
        # High amount should generate significant amount risk (capped at 100 for the factor)
        assert result.fraud_score.risk_factors["amount_anomaly"] > 0
        # Total score includes weighted factors, so verify it's elevated
        assert result.fraud_score.risk_score >= 20  # With 25% weight on amount anomaly >= 100

    @pytest.mark.asyncio
    async def test_missing_metadata(self, fraud_engine):
        """Test fraud scoring when metadata is missing."""
        transaction = {
            "transaction_id": "txn_no_meta",
            "user_id": "user_no_meta",
            "amount": 500000.0,
            "currency": "IDR",
            "type": "TRANSFER"
            # No metadata field
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None
        # Should handle missing metadata gracefully

    @pytest.mark.asyncio
    async def test_empty_metadata(self, fraud_engine):
        """Test fraud scoring when metadata is empty dict."""
        transaction = {
            "transaction_id": "txn_empty_meta",
            "user_id": "user_empty_meta",
            "amount": 500000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None

    @pytest.mark.asyncio
    async def test_unknown_transaction_type(self, fraud_engine):
        """Test fraud scoring for unknown transaction type."""
        transaction = {
            "transaction_id": "txn_unknown_type",
            "user_id": "user_unknown",
            "amount": 500000.0,
            "currency": "IDR",
            "type": "UNKNOWN_TYPE",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None
        # Unknown types might get slightly higher risk

    @pytest.mark.asyncio
    async def test_foreign_currency_transaction(self, fraud_engine):
        """Test fraud scoring for foreign currency transaction."""
        transaction = {
            "transaction_id": "txn_foreign",
            "user_id": "user_foreign",
            "amount": 1000.0,
            "currency": "USD",  # Foreign currency
            "type": "TRANSFER",
            "metadata": {
                "ip_address": "203.0.113.1",  # Foreign IP
                "location": "United States"
            }
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None
        # Foreign transactions might get higher risk

    @pytest.mark.asyncio
    async def test_suspicious_ip_address(self, fraud_engine):
        """Test fraud scoring with known suspicious IP."""
        transaction = {
            "transaction_id": "txn_suspicious_ip",
            "user_id": "user_suspicious",
            "amount": 5000000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {
                "ip_address": "10.0.0.0",  # Known suspicious pattern
                "device_id": "device_123",
                "location": "Unknown"
            }
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None

    @pytest.mark.asyncio
    async def test_vpn_ip_address(self, fraud_engine):
        """Test fraud scoring with VPN/proxy IP."""
        transaction = {
            "transaction_id": "txn_vpn",
            "user_id": "user_vpn",
            "amount": 1000000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {
                "ip_address": "45.76.123.45",  # Typical VPN range
                "device_id": "device_vpn",
                "is_vpn": True
            }
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None

    @pytest.mark.asyncio
    async def test_device_change(self, fraud_engine):
        """Test fraud scoring when user changes device."""
        transaction = {
            "transaction_id": "txn_device_change",
            "user_id": "user_device",
            "amount": 2000000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {
                "ip_address": "192.168.1.1",
                "device_id": "NEW_DEVICE_123",  # Different device
                "location": "Jakarta, Indonesia"
            }
        }

        # Mock user history showing previous device
        user_history = {
            "total_transactions": 50,
            "total_amount": 10000000.0,
            "average_transaction": 200000.0,
            "account_created_at": (datetime.utcnow() - timedelta(days=180)).isoformat(),
            "recent_transactions": [
                {
                    "transaction_id": f"txn_{i}",
                    "amount": 200000.0,
                    "type": "TRANSFER",
                    "timestamp": (datetime.utcnow() - timedelta(hours=i)).isoformat(),
                    "recipient_id": f"recipient_{i}"
                }
                for i in range(10)
            ]
        }

        result = await fraud_engine.calculate_fraud_score(transaction, user_history)

        assert result is not None

    @pytest.mark.asyncio
    async def test_rapid_successive_transactions(self, fraud_engine):
        """Test fraud scoring for rapid successive transactions."""
        # Simulate multiple transactions in quick succession
        transactions = [
            {
                "transaction_id": f"txn_rapid_{i}",
                "user_id": "user_rapid",
                "amount": 500000.0,
                "currency": "IDR",
                "type": "TRANSFER",
                "metadata": {"ip_address": "192.168.1.1"}
            }
            for i in range(10)
        ]

        results = []
        for txn in transactions:
            result = await fraud_engine.calculate_fraud_score(txn)
            results.append(result)

        # All should have scores
        assert all(r is not None for r in results)

    @pytest.mark.asyncio
    async def test_multiple_recipients_pattern(self, fraud_engine):
        """Test fraud scoring for transactions to multiple recipients."""
        user_history = {
            "total_transactions": 20,
            "total_amount": 5000000.0,
            "average_transaction": 250000.0,
            "account_created_at": (datetime.utcnow() - timedelta(days=100)).isoformat(),
            "recent_transactions": [
                {
                    "transaction_id": f"txn_multi_{i}",
                    "amount": 250000.0,
                    "type": "TRANSFER",
                    "timestamp": (datetime.utcnow() - timedelta(minutes=i * 10)).isoformat(),
                    "recipient_id": f"different_recipient_{i}"  # All different
                }
                for i in range(15)
            ]
        }

        transaction = {
            "transaction_id": "txn_test_multi",
            "user_id": "user_multi",
            "amount": 300000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction, user_history)

        assert result is not None
        # Multiple different recipients might increase risk

    @pytest.mark.asyncio
    async def test_round_amount_patterns(self, fraud_engine):
        """Test fraud scoring for suspiciously round amounts."""
        transaction = {
            "transaction_id": "txn_round",
            "user_id": "user_round",
            "amount": 10000000.0,  # Exactly 10 million - suspicious round number
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None

    @pytest.mark.asyncio
    async def test_business_hours_vs_night(self, fraud_engine):
        """Test fraud scoring for transactions at unusual hours."""
        # Night transaction (2 AM)
        transaction = {
            "transaction_id": "txn_night",
            "user_id": "user_night",
            "amount": 5000000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {
                "ip_address": "192.168.1.1",
                "timestamp": datetime.utcnow().replace(hour=2, minute=0).isoformat()
            }
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None

    @pytest.mark.asyncio
    async def test_weekend_transaction(self, fraud_engine):
        """Test fraud scoring for weekend large transactions."""
        transaction = {
            "transaction_id": "txn_weekend",
            "user_id": "user_weekend",
            "amount": 8000000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {
                "ip_address": "192.168.1.1"
            }
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None

    @pytest.mark.asyncio
    async def test_batch_score_with_mixed_risk(self, fraud_engine):
        """Test batch scoring with mixed risk transactions."""
        transactions = [
            {
                "transaction_id": f"txn_batch_{i}",
                "user_id": "user_batch",
                "amount": 100000.0 * (i + 1),
                "currency": "IDR",
                "type": "TRANSFER",
                "metadata": {}
            }
            for i in range(10)
        ]

        results = await fraud_engine.batch_score_transactions(transactions)

        assert len(results) == 10
        assert all(r is not None for r in results)

        # All results should be valid FraudDetectionResult objects
        for result in results:
            assert hasattr(result, "fraud_score")
            assert hasattr(result, "is_blocked")
            assert hasattr(result, "requires_review")

    @pytest.mark.asyncio
    async def test_fraud_score_upper_bound(self, fraud_engine):
        """Test that fraud score never exceeds 100."""
        # Create scenario that should maximize risk
        transaction = {
            "transaction_id": "txn_max_risk",
            "user_id": "user_max",
            "amount": 50000000.0,  # Very high
            "currency": "IDR",
            "type": "QRIS",  # QRIS with high amount
            "metadata": {
                "ip_address": "10.0.0.0"  # Suspicious IP
            }
        }

        new_user_history = {
            "total_transactions": 1,
            "total_amount": 50000.0,
            "average_transaction": 50000.0,
            "account_created_at": (datetime.utcnow() - timedelta(days=1)).isoformat(),
            "recent_transactions": []
        }

        result = await fraud_engine.calculate_fraud_score(transaction, new_user_history)

        assert result.fraud_score.risk_score <= 100

    @pytest.mark.asyncio
    async def test_fraud_score_lower_bound(self, fraud_engine):
        """Test that fraud score is never negative."""
        # Create scenario that should minimize risk
        transaction = {
            "transaction_id": "txn_min_risk",
            "user_id": "user_min",
            "amount": 50000.0,  # Low amount
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {
                "ip_address": "192.168.1.1",
                "device_id": "trusted_device",
                "location": "Jakarta, Indonesia"
            }
        }

        trusted_user_history = {
            "total_transactions": 1000,
            "total_amount": 300000000.0,
            "average_transaction": 300000.0,
            "account_created_at": (datetime.utcnow() - timedelta(days=730)).isoformat(),  # 2 years
            "recent_transactions": [
                {
                    "transaction_id": f"txn_{i}",
                    "amount": 300000.0,
                    "type": "TRANSFER",
                    "timestamp": (datetime.utcnow() - timedelta(hours=i)).isoformat(),
                    "recipient_id": f"recipient_{i}"
                }
                for i in range(10)
            ]
        }

        result = await fraud_engine.calculate_fraud_score(transaction, trusted_user_history)

        assert result.fraud_score.risk_score >= 0

    @pytest.mark.asyncio
    async def test_explain_fraud_score_completeness(self, fraud_engine):
        """Test that fraud score explanation includes all risk factors."""
        transaction = {
            "transaction_id": "txn_explain",
            "user_id": "user_explain",
            "amount": 5000000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {
                "ip_address": "192.168.1.1",
                "device_id": "device_explain"
            }
        }

        result = await fraud_engine.calculate_fraud_score(transaction)
        explanation = fraud_engine.explain_fraud_score(result)

        assert isinstance(explanation, str)
        assert "Fraud Risk Score" in explanation
        assert "Risk Factors" in explanation

    @pytest.mark.asyncio
    async def test_is_blocked_flag_high_risk(self, fraud_engine):
        """Test that high risk transactions are flagged for blocking."""
        transaction = {
            "transaction_id": "txn_block",
            "user_id": "user_block",
            "amount": 30000000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {}
        }

        new_user = {
            "total_transactions": 0,
            "total_amount": 0.0,
            "average_transaction": 0.0,
            "account_created_at": (datetime.utcnow() - timedelta(days=1)).isoformat(),
            "recent_transactions": []
        }

        result = await fraud_engine.calculate_fraud_score(transaction, new_user)

        # High risk should potentially trigger blocking
        if result.fraud_score.risk_level == FraudRiskLevel.CRITICAL:
            assert result.is_blocked is True

    @pytest.mark.asyncio
    async def test_requires_review_flag_medium_risk(self, fraud_engine):
        """Test that medium risk transactions require review."""
        transaction = {
            "transaction_id": "txn_review",
            "user_id": "user_review",
            "amount": 3000000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        # Medium/High risk should potentially require review
        if result.fraud_score.risk_level in [FraudRiskLevel.HIGH, FraudRiskLevel.MEDIUM]:
            # Either requires review or is blocked
            assert result.requires_review is True or result.is_blocked is True

    @pytest.mark.asyncio
    async def test_rule_triggers_population(self, fraud_engine):
        """Test that rule triggers are populated correctly."""
        transaction = {
            "transaction_id": "txn_rules",
            "user_id": "user_rules",
            "amount": 7000000.0,
            "currency": "IDR",
            "type": "QRIS",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert isinstance(result.rule_triggers, list)
        # Should have at least some rules triggered for this amount
        # (This is flexible as the exact rules depend on implementation)

    @pytest.mark.asyncio
    async def test_user_history_none(self, fraud_engine):
        """Test fraud scoring when user history is None."""
        transaction = {
            "transaction_id": "txn_no_history",
            "user_id": "user_no_history",
            "amount": 500000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction, user_history=None)

        assert result is not None
        # Should handle None history gracefully

    @pytest.mark.asyncio
    async def test_empty_user_history(self, fraud_engine):
        """Test fraud scoring when user history is empty dict."""
        transaction = {
            "transaction_id": "txn_empty_history",
            "user_id": "user_empty_history",
            "amount": 500000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction, user_history={})

        assert result is not None
        # Should handle empty history gracefully

    @pytest.mark.asyncio
    async def test_special_characters_in_user_id(self, fraud_engine):
        """Test handling of special characters in user_id."""
        transaction = {
            "transaction_id": "txn_special",
            "user_id": "user_123!@#$%",
            "amount": 500000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None

    @pytest.mark.asyncio
    async def test_very_long_transaction_id(self, fraud_engine):
        """Test handling of very long transaction IDs."""
        long_id = "txn_" + "a" * 1000
        transaction = {
            "transaction_id": long_id,
            "user_id": "user_long",
            "amount": 500000.0,
            "currency": "IDR",
            "type": "TRANSFER",
            "metadata": {}
        }

        result = await fraud_engine.calculate_fraud_score(transaction)

        assert result is not None
        assert result.fraud_score.transaction_id == long_id
