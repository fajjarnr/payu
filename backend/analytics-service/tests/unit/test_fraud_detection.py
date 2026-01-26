import pytest
import sys

sys.path.insert(0, "/home/ubuntu/payu/backend/analytics-service/src")  # noqa: E402
from datetime import datetime, timedelta

from app.models.schemas import FraudRiskLevel, FraudDetectionResult
from app.ml.fraud_detection import FraudDetectionEngine


@pytest.mark.unit
class TestFraudDetectionEngine:
    """Unit tests for Fraud Detection Engine"""

    @pytest.fixture
    def fraud_engine(self):
        return FraudDetectionEngine()

    def test_engine_initialization(self, fraud_engine):
        """Test fraud engine initialization"""
        assert fraud_engine.fraud_patterns is not None
        assert fraud_engine.risk_weights is not None
        assert "high_amount_threshold" in fraud_engine.fraud_patterns
        assert "amount_anomaly" in fraud_engine.risk_weights

    def test_low_risk_transaction(self, fraud_engine):
        """Test scoring for low-risk transaction"""
        transaction_data = {
            "transaction_id": "txn-001",
            "user_id": "user-123",
            "amount": 50000.0,
            "type": "TRANSFER",
            "currency": "IDR",
            "metadata": {},
        }

        result = fraud_engine._calculate_amount_risk(transaction_data)

        assert result == 0.0

    async def test_high_amount_risk(self, fraud_engine):
        """Test risk scoring for high-amount transaction"""
        transaction_data = {
            "transaction_id": "txn-002",
            "user_id": "user-123",
            "amount": 200000000.0,
            "type": "TRANSFER",
            "currency": "IDR",
            "metadata": {},
        }

        amount_risk = fraud_engine._calculate_amount_risk(transaction_data)
        assert amount_risk > 0

    def test_qris_high_amount_risk(self, fraud_engine):
        """Test QRIS payment with high amount"""
        transaction_data = {
            "transaction_id": "txn-003",
            "user_id": "user-123",
            "amount": 75000000.0,
            "type": "QRIS_PAYMENT",
            "currency": "IDR",
            "metadata": {},
        }

        amount_risk = fraud_engine._calculate_amount_risk(transaction_data)
        assert amount_risk == 50.0

    async def test_velocity_risk_high_frequency(self, fraud_engine):
        """Test velocity risk with high transaction frequency"""
        base_time = datetime.utcnow()

        user_history = {
            "recent_transactions": [
                {
                    "transaction_id": f"txn-{i}",
                    "amount": 50000.0,
                    "type": "TRANSFER",
                    "timestamp": (base_time - timedelta(seconds=i * 30)).isoformat(),
                    "recipient_id": f"recipient-{i}",
                }
                for i in range(6)
            ]
        }

        transaction_data = {
            "transaction_id": "txn-new",
            "user_id": "user-123",
            "amount": 50000.0,
            "type": "TRANSFER",
        }

        velocity_risk = await fraud_engine._calculate_velocity_risk(
            transaction_data, user_history
        )
        assert velocity_risk >= 20.0

    async def test_velocity_risk_normal_frequency(self, fraud_engine):
        """Test velocity risk with normal transaction frequency"""
        user_history = {
            "recent_transactions": [
                {
                    "transaction_id": f"txn-{i}",
                    "amount": 50000.0,
                    "type": "TRANSFER",
                    "timestamp": (datetime.utcnow() - timedelta(hours=i)).isoformat(),
                    "recipient_id": f"recipient-{i}",
                }
                for i in range(3)
            ]
        }

        transaction_data = {
            "transaction_id": "txn-new",
            "user_id": "user-123",
            "amount": 50000.0,
            "type": "TRANSFER",
        }

        velocity_risk = await fraud_engine._calculate_velocity_risk(
            transaction_data, user_history
        )
        assert velocity_risk < 20.0

    async def test_behavioral_risk_high_deviation(self, fraud_engine):
        """Test behavioral risk with high amount deviation"""
        user_history = {
            "recent_transactions": [
                {
                    "transaction_id": f"txn-{i}",
                    "amount": 10000.0,
                    "type": "TRANSFER",
                    "timestamp": (datetime.utcnow() - timedelta(hours=i)).isoformat(),
                    "recipient_id": f"recipient-{i}",
                }
                for i in range(10)
            ]
        }

        transaction_data = {
            "transaction_id": "txn-new",
            "user_id": "user-123",
            "amount": 50000.0,
            "type": "TRANSFER",
        }

        behavioral_risk = await fraud_engine._calculate_behavioral_risk(
            transaction_data, user_history
        )
        assert behavioral_risk > 0

    async def test_account_age_risk_new_account(self, fraud_engine):
        """Test account age risk for new account"""
        user_history = {
            "account_created_at": (datetime.utcnow() - timedelta(hours=2)).isoformat(),
            "recent_transactions": [],
        }

        transaction_data = {
            "transaction_id": "txn-new",
            "user_id": "user-123",
            "amount": 100000.0,
            "type": "TRANSFER",
        }

        account_age_risk = fraud_engine._calculate_account_age_risk(
            transaction_data, user_history
        )
        assert account_age_risk >= 40.0

    async def test_account_age_risk_old_account(self, fraud_engine):
        """Test account age risk for established account"""
        user_history = {
            "account_created_at": (datetime.utcnow() - timedelta(days=365)).isoformat(),
            "recent_transactions": [],
        }

        transaction_data = {
            "transaction_id": "txn-new",
            "user_id": "user-123",
            "amount": 100000.0,
            "type": "TRANSFER",
        }

        account_age_risk = fraud_engine._calculate_account_age_risk(
            transaction_data, user_history
        )
        assert account_age_risk == 0.0

    def test_location_risk_suspicious_ip(self, fraud_engine):
        """Test location risk with suspicious IP"""
        transaction_data = {
            "transaction_id": "txn-new",
            "user_id": "user-123",
            "amount": 100000.0,
            "type": "TRANSFER",
            "metadata": {
                "ip_address": "192.168.1.100",
                "suspicious_ips": ["192.168.1.100"],
            },
        }

        location_risk = fraud_engine._calculate_location_risk(transaction_data)
        assert location_risk == 100.0

    def test_location_risk_new_ip(self, fraud_engine):
        """Test location risk with new IP address"""
        transaction_data = {
            "transaction_id": "txn-new",
            "user_id": "user-123",
            "amount": 100000.0,
            "type": "TRANSFER",
            "metadata": {
                "ip_address": "192.168.1.200",
                "last_known_ip": "192.168.1.100",
            },
        }

        location_risk = fraud_engine._calculate_location_risk(transaction_data)
        assert location_risk > 0

    def test_location_risk_no_metadata(self, fraud_engine):
        """Test location risk with no metadata"""
        transaction_data = {
            "transaction_id": "txn-new",
            "user_id": "user-123",
            "amount": 100000.0,
            "type": "TRANSFER",
        }

        location_risk = fraud_engine._calculate_location_risk(transaction_data)
        assert location_risk == 0.0

    def test_risk_level_determination(self, fraud_engine):
        """Test risk level determination based on scores"""
        assert fraud_engine._determine_risk_level(0) == FraudRiskLevel.MINIMAL
        assert fraud_engine._determine_risk_level(5) == FraudRiskLevel.MINIMAL
        assert fraud_engine._determine_risk_level(15) == FraudRiskLevel.MINIMAL
        assert fraud_engine._determine_risk_level(25) == FraudRiskLevel.LOW
        assert fraud_engine._determine_risk_level(45) == FraudRiskLevel.MEDIUM
        assert fraud_engine._determine_risk_level(70) == FraudRiskLevel.HIGH
        assert fraud_engine._determine_risk_level(90) == FraudRiskLevel.CRITICAL

    def test_recommended_action_critical(self, fraud_engine):
        """Test recommended action for critical risk"""
        action = fraud_engine._get_recommended_action(FraudRiskLevel.CRITICAL)
        assert "BLOCK" in action
        assert "Immediately" in action

    def test_recommended_action_high(self, fraud_engine):
        """Test recommended action for high risk"""
        action = fraud_engine._get_recommended_action(FraudRiskLevel.HIGH)
        assert "BLOCK" in action
        assert "verification" in action

    def test_recommended_action_medium(self, fraud_engine):
        """Test recommended action for medium risk"""
        action = fraud_engine._get_recommended_action(FraudRiskLevel.MEDIUM)
        assert "REVIEW" in action
        assert "manual" in action

    def test_recommended_action_low(self, fraud_engine):
        """Test recommended action for low risk"""
        action = fraud_engine._get_recommended_action(FraudRiskLevel.LOW)
        assert "MONITOR" in action

    def test_recommended_action_minimal(self, fraud_engine):
        """Test recommended action for minimal risk"""
        action = fraud_engine._get_recommended_action(FraudRiskLevel.MINIMAL)
        assert "ALLOW" in action

    async def test_full_fraud_score_calculation_low_risk(self, fraud_engine):
        """Test full fraud score calculation for low-risk transaction"""
        transaction_data = {
            "transaction_id": "txn-001",
            "user_id": "user-123",
            "amount": 10000.0,
            "type": "TRANSFER",
            "currency": "IDR",
        }

        result = await fraud_engine.calculate_fraud_score(transaction_data)

        assert isinstance(result, FraudDetectionResult)
        assert result.fraud_score.transaction_id == "txn-001"
        assert result.fraud_score.risk_score < 50
        assert not result.is_blocked
        assert not result.requires_review

    async def test_full_fraud_score_calculation_high_risk(self, fraud_engine):
        """Test full fraud score calculation for high-risk transaction"""
        user_history = {
            "account_created_at": (datetime.utcnow() - timedelta(days=2)).isoformat(),
            "recent_transactions": [
                {
                    "transaction_id": f"txn-{i}",
                    "amount": 50000.0,
                    "type": "TRANSFER",
                    "timestamp": (datetime.utcnow() - timedelta(minutes=i)).isoformat(),
                    "recipient_id": f"recipient-{i}",
                }
                for i in range(6)
            ],
        }

        transaction_data = {
            "transaction_id": "txn-high",
            "user_id": "user-123",
            "amount": 250000000.0,
            "type": "TRANSFER",
            "currency": "IDR",
        }

        result = await fraud_engine.calculate_fraud_score(
            transaction_data, user_history
        )

        assert isinstance(result, FraudDetectionResult)
        assert result.fraud_score.risk_score >= 40
        assert result.fraud_score.risk_factors is not None
        assert len(result.fraud_score.risk_factors) > 0

    async def test_fraud_score_boundary_values(self, fraud_engine):
        """Test fraud score at boundary values"""
        transaction_data = {
            "transaction_id": "txn-boundary",
            "user_id": "user-123",
            "amount": 100000000.0,
            "type": "TRANSFER",
            "currency": "IDR",
        }

        result = await fraud_engine.calculate_fraud_score(transaction_data)

        assert 0 <= result.fraud_score.risk_score <= 100

    def test_explain_fraud_score(self, fraud_engine):
        """Test fraud score explanation generation"""
        from app.models.schemas import FraudScore

        fraud_score = FraudScore(
            transaction_id="txn-001",
            user_id="user-123",
            risk_score=75.5,
            risk_level=FraudRiskLevel.HIGH,
            risk_factors={
                "amount_anomaly": 40.0,
                "velocity_check": 50.0,
                "behavioral_pattern": 30.0,
            },
            is_suspicious=True,
            recommended_action="BLOCK - Block transaction and require additional verification",
            scored_at=datetime.utcnow(),
        )

        fraud_result = FraudDetectionResult(
            fraud_score=fraud_score,
            is_blocked=True,
            requires_review=False,
            rule_triggers=[
                "High Amount Anomaly detected",
                "High Velocity Check detected",
            ],
        )

        explanation = fraud_engine.explain_fraud_score(fraud_result)

        assert "Fraud Risk Score" in explanation
        assert "HIGH" in explanation
        assert "75.5" in explanation
        assert "Risk Factors" in explanation
        assert "Amount Anomaly" in explanation
        assert "BLOCK" in explanation

    async def test_batch_score_transactions(self, fraud_engine):
        """Test batch scoring of multiple transactions"""
        transactions = [
            {
                "transaction_id": f"txn-{i}",
                "user_id": "user-123",
                "amount": 10000.0 * i,
                "type": "TRANSFER",
                "currency": "IDR",
            }
            for i in range(1, 6)
        ]

        results = await fraud_engine.batch_score_transactions(transactions)

        assert len(results) == 5
        assert all(isinstance(r, FraudDetectionResult) for r in results)
        assert all(r.fraud_score.risk_score >= 0 for r in results)

    def test_risk_factors_sum_to_total(self, fraud_engine):
        """Test that risk weights sum to 1.0"""
        total_weight = sum(fraud_engine.risk_weights.values())
        assert abs(total_weight - 1.0) < 0.001

    async def test_no_user_history_scenario(self, fraud_engine):
        """Test transaction scoring without user history"""
        transaction_data = {
            "transaction_id": "txn-new-user",
            "user_id": "user-999",
            "amount": 50000.0,
            "type": "TRANSFER",
            "currency": "IDR",
        }

        result = await fraud_engine.calculate_fraud_score(transaction_data)

        assert isinstance(result, FraudDetectionResult)
        assert result.fraud_score.risk_score >= 0
