from typing import Dict, Any, Optional
from datetime import datetime, timedelta
from decimal import Decimal
from enum import Enum
from structlog import get_logger
import asyncio

from app.models.schemas import FraudScore, FraudRiskLevel, FraudDetectionResult

logger = get_logger(__name__)


class FraudDetectionEngine:
    def __init__(self):
        self.fraud_patterns = {
            "high_amount_threshold": 100000000.0,
            "unusual_time_hours": [0, 1, 2, 3, 4, 5],
            "velocity_threshold": 5,
            "velocity_window_minutes": 5,
            "new_account_threshold_days": 7,
            "location_change_threshold_km": 500
        }

        self.risk_weights = {
            "amount_anomaly": 0.25,
            "velocity_check": 0.30,
            "behavioral_pattern": 0.20,
            "location_anomaly": 0.15,
            "account_age": 0.10
        }

    async def calculate_fraud_score(
        self,
        transaction_data: Dict[str, Any],
        user_history: Optional[Dict[str, Any]] = None
    ) -> FraudDetectionResult:
        user_id = str(transaction_data.get("user_id", transaction_data.get("sender_account_id", "unknown")))
        transaction_id = str(transaction_data.get("transaction_id", "unknown"))
        log = logger.bind(user_id=user_id, transaction_id=transaction_id)

        log.info("Calculating fraud score")

        risk_factors = {}
        total_risk_score = 0.0

        amount_risk = self._calculate_amount_risk(transaction_data)
        risk_factors["amount_anomaly"] = amount_risk
        total_risk_score += amount_risk * self.risk_weights["amount_anomaly"]

        velocity_risk = await self._calculate_velocity_risk(transaction_data, user_history)
        risk_factors["velocity_check"] = velocity_risk
        total_risk_score += velocity_risk * self.risk_weights["velocity_check"]

        behavioral_risk = await self._calculate_behavioral_risk(transaction_data, user_history)
        risk_factors["behavioral_pattern"] = behavioral_risk
        total_risk_score += behavioral_risk * self.risk_weights["behavioral_pattern"]

        location_risk = self._calculate_location_risk(transaction_data)
        risk_factors["location_anomaly"] = location_risk
        total_risk_score += location_risk * self.risk_weights["location_anomaly"]

        account_age_risk = self._calculate_account_age_risk(transaction_data, user_history)
        risk_factors["account_age"] = account_age_risk
        total_risk_score += account_age_risk * self.risk_weights["account_age"]

        risk_score = min(max(total_risk_score, 0.0), 100.0)
        risk_level = self._determine_risk_level(risk_score)
        is_blocked = risk_level in [FraudRiskLevel.HIGH, FraudRiskLevel.CRITICAL]
        recommended_action = self._get_recommended_action(risk_level)

        fraud_score = FraudScore(
            transaction_id=transaction_data.get("transaction_id"),
            user_id=user_id,
            risk_score=round(risk_score, 2),
            risk_level=risk_level,
            risk_factors=risk_factors,
            is_suspicious=risk_score >= 50.0,
            recommended_action=recommended_action,
            scored_at=datetime.utcnow()
        )

        result = FraudDetectionResult(
            fraud_score=fraud_score,
            is_blocked=is_blocked,
            requires_review=risk_level in [FraudRiskLevel.MEDIUM, FraudRiskLevel.HIGH],
            rule_triggers=self._identify_triggered_rules(risk_factors)
        )

        log.info("Fraud score calculated", risk_score=risk_score, risk_level=risk_level, is_blocked=is_blocked)

        return result

    def _calculate_amount_risk(self, transaction_data: Dict[str, Any]) -> float:
        amount = float(transaction_data.get("amount", 0))
        transaction_type = transaction_data.get("type", "TRANSFER")

        if amount > self.fraud_patterns["high_amount_threshold"]:
            return min(100.0, (amount / self.fraud_patterns["high_amount_threshold"]) * 40)

        if transaction_type == "QRIS_PAYMENT" and amount > 50000000.0:
            return 50.0

        return 0.0

    async def _calculate_velocity_risk(
        self,
        transaction_data: Dict[str, Any],
        user_history: Optional[Dict[str, Any]] = None
    ) -> float:
        if not user_history:
            return 0.0

        recent_transactions = user_history.get("recent_transactions", [])
        velocity_window = timedelta(minutes=self.fraud_patterns["velocity_window_minutes"])
        from datetime import datetime
        now = datetime.utcnow()

        count_in_window = sum(
            1 for txn in recent_transactions
            if datetime.fromisoformat(txn["timestamp"].replace('Z', '+00:00')) >= now - velocity_window
        )

        if count_in_window >= self.fraud_patterns["velocity_threshold"]:
            excess = count_in_window - self.fraud_patterns["velocity_threshold"]
            return min(100.0, excess * 20)

        if count_in_window >= self.fraud_patterns["velocity_threshold"] - 1:
            return 30.0

        return 0.0

    async def _calculate_behavioral_risk(
        self,
        transaction_data: Dict[str, Any],
        user_history: Optional[Dict[str, Any]] = None
    ) -> float:
        if not user_history:
            return 20.0

        amount = float(transaction_data.get("amount", 0))
        transaction_type = transaction_data.get("type", "TRANSFER")

        recent_transactions = user_history.get("recent_transactions", [])
        same_type_transactions = [
            txn for txn in recent_transactions
            if txn["type"] == transaction_type
        ]

        if not same_type_transactions:
            return 10.0

        avg_amount = sum(txn["amount"] for txn in same_type_transactions) / len(same_type_transactions)

        if avg_amount > 0:
            deviation = abs(amount - avg_amount) / avg_amount
            if deviation > 5.0:
                return 80.0
            elif deviation > 2.0:
                return 50.0
            elif deviation > 1.0:
                return 30.0

        new_recipient = transaction_data.get("recipient_id") not in [
            txn.get("recipient_id") for txn in recent_transactions
        ]

        if new_recipient and amount > 10000000.0:
            return 20.0

        return 0.0

    def _calculate_location_risk(self, transaction_data: Dict[str, Any]) -> float:
        metadata = transaction_data.get("metadata", {})

        if not metadata:
            return 0.0

        ip_address = metadata.get("ip_address")
        device_id = metadata.get("device_id")

        if not ip_address and not device_id:
            return 0.0

        suspicious_ips = metadata.get("suspicious_ips", [])
        if ip_address in suspicious_ips:
            return 100.0

        last_known_ip = metadata.get("last_known_ip")
        if last_known_ip and ip_address != last_known_ip:
            return 15.0

        return 0.0

    def _calculate_account_age_risk(
        self,
        transaction_data: Dict[str, Any],
        user_history: Optional[Dict[str, Any]] = None
    ) -> float:
        if not user_history:
            return 0.0

        account_created_at = user_history.get("account_created_at")
        if not account_created_at:
            return 50.0

        account_age = datetime.utcnow() - datetime.fromisoformat(account_created_at)

        if account_age.days < 1:
            return 70.0
        elif account_age.days < self.fraud_patterns["new_account_threshold_days"]:
            return 40.0

        return 0.0

    def _determine_risk_level(self, risk_score: float) -> FraudRiskLevel:
        if risk_score >= 80:
            return FraudRiskLevel.CRITICAL
        elif risk_score >= 60:
            return FraudRiskLevel.HIGH
        elif risk_score >= 40:
            return FraudRiskLevel.MEDIUM
        elif risk_score >= 20:
            return FraudRiskLevel.LOW
        elif risk_score >= 10:
            return FraudRiskLevel.MINIMAL
        else:
            return FraudRiskLevel.MINIMAL

    def _get_recommended_action(self, risk_level: FraudRiskLevel) -> str:
        actions = {
            FraudRiskLevel.CRITICAL: "BLOCK - Immediately block transaction and flag for security review",
            FraudRiskLevel.HIGH: "BLOCK - Block transaction and require additional verification",
            FraudRiskLevel.MEDIUM: "REVIEW - Require manual review before processing",
            FraudRiskLevel.LOW: "MONITOR - Allow transaction but flag for monitoring",
            FraudRiskLevel.MINIMAL: "ALLOW - Process normally"
        }
        return actions.get(risk_level, "ALLOW - Process normally")

    def _identify_triggered_rules(self, risk_factors: Dict[str, float]) -> list[str]:
        triggered = []

        for factor, score in risk_factors.items():
            if score >= 40:
                triggered.append(f"High {factor.replace('_', ' ').title()} detected")
            elif score >= 20:
                triggered.append(f"Moderate {factor.replace('_', ' ').title()} detected")

        return triggered

    async def batch_score_transactions(
        self,
        transactions: list[Dict[str, Any]]
    ) -> list[FraudDetectionResult]:
        log = logger.info("Batch scoring transactions", count=len(transactions))

        tasks = [self.calculate_fraud_score(txn) for txn in transactions]
        results = await asyncio.gather(*tasks, return_exceptions=True)

        valid_results = []
        for i, result in enumerate(results):
            if isinstance(result, Exception):
                logger.error("Error scoring transaction", transaction_index=i, error=str(result))
            else:
                valid_results.append(result)

        return valid_results

    def explain_fraud_score(self, fraud_result: FraudDetectionResult) -> str:
        score = fraud_result.fraud_score
        factors = fraud_result.fraud_score.risk_factors
        triggers = fraud_result.rule_triggers

        explanation = f"Fraud Risk Score: {score.risk_level.value} ({score.risk_score}/100)\n\n"

        if triggers:
            explanation += "Triggered Rules:\n"
            for trigger in triggers:
                explanation += f"  - {trigger}\n"
            explanation += "\n"

        explanation += "Risk Factors:\n"
        for factor, value in factors.items():
            explanation += f"  - {factor.replace('_', ' ').title()}: {value:.1f}%\n"

        explanation += f"\nRecommended Action: {score.recommended_action}"

        return explanation
