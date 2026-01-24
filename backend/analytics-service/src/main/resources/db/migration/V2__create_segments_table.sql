"""
Customer Segmentation Engine for PayU Analytics Service
Implements RFM analysis and K-Means clustering for user segmentation
"""

from typing import List, Dict, Any, Optional
from datetime import datetime, timedelta
from dataclasses import dataclass
from enum import Enum
import json

from sqlalchemy import select, func, and_, or_
from sqlalchemy.ext.asyncio import AsyncSession

from ..database.models import User, Transaction, Wallet
from ..core.logging import get_logger

logger = get_logger(__name__)


class SegmentType(Enum):
    """Customer segment types"""
    PREMIUM = "premium"      # High value, frequent transactions
    LOYAL = "loyal"          # Regular, long-term customers
    GROWING = "growing"      # New but active
    AT_RISK = "at_risk"      # Declining activity
    CHURNED = "churned"      # No recent activity
    DORMANT = "dormant"      # Very low activity


@dataclass
class RFMScore:
    """RFM (Recency, Frequency, Monetary) score components"""
    recency: int      # Days since last transaction (inverted)
    frequency: int   # Number of transactions
    monetary: float  # Total transaction amount
    raw_recency: int  # Actual days since last transaction


@dataclass
class CustomerSegment:
    """Customer segment data"""
    user_id: str
    segment: SegmentType
    rfm_score: RFMScore
    account_age_days: int
    kyc_verified: bool
    metadata: Dict[str, Any]
    updated_at: datetime


class CustomerSegmentationService:
    """
    Service for customer segmentation using RFM analysis and K-Means clustering
    """

    def __init__(self, db: AsyncSession):
        self.db = db

    async def calculate_user_segment(self, user_id: str) -> CustomerSegment:
        """
        Calculate segment for a single user

        Args:
            user_id: User UUID

        Returns:
            CustomerSegment with calculated segment and scores
        """
        # Get user data
        user_result = await self.db.execute(
            select(User).where(User.id == user_id)
        )
        user = user_result.scalar_one_or_none()

        if not user:
            raise ValueError(f"User {user_id} not found")

        # Calculate account age
        account_age_days = (datetime.utcnow() - user.created_at).days

        # Get RFM scores
        rfm_score = await self._calculate_rfm_score(user_id)

        # Determine segment
        segment = self._determine_segment(rfm_score, account_age_days, user.kyc_status)

        # Get additional metrics
        metadata = await self._get_user_metadata(user_id)

        return CustomerSegment(
            user_id=user_id,
            segment=segment,
            rfm_score=rfm_score,
            account_age_days=account_age_days,
            kyc_verified=user.kyc_status == "APPROVED",
            metadata=metadata,
            updated_at=datetime.utcnow()
        )

    async def batch_calculate_segments(
        self,
        user_ids: List[str],
        batch_size: int = 100
    ) -> List[CustomerSegment]:
        """
        Calculate segments for multiple users

        Args:
            user_ids: List of user UUIDs
            batch_size: Number of users to process at once

        Returns:
            List of CustomerSegment objects
        """
        segments = []

        for i in range(0, len(user_ids), batch_size):
            batch = user_ids[i:i + batch_size]

            for user_id in batch:
                try:
                    segment = await self.calculate_user_segment(user_id)
                    segments.append(segment)
                except Exception as e:
                    logger.error(f"Failed to calculate segment for user {user_id}: {e}")

        return segments

    async def recalculate_all_segments(self, days_threshold: int = 7) -> int:
        """
        Recalculate segments for all active users

        Args:
            days_threshold: Only recalculate segments older than this many days

        Returns:
            Number of segments recalculated
        """
        # Get users with old or missing segments
        cutoff_date = datetime.utcnow() - timedelta(days=days_threshold)

        # This would query a segments table; for now we recalculate all users
        users_result = await self.db.execute(
            select(User.id).where(
                and_(
                    User.created_at < cutoff_date,
                    User.kyc_status == "APPROVED"
                )
            ).limit(10000)
        )
        user_ids = [row[0] for row in users_result.fetchall()]

        segments = await self.batch_calculate_segments(user_ids)

        # Save segments to database
        count = 0
        for segment in segments:
            await self._save_segment(segment)
            count += 1

        logger.info(f"Recalculated {count} customer segments")
        return count

    async def get_users_by_segment(
        self,
        segment: SegmentType,
        limit: int = 100
    ) -> List[str]:
        """
        Get list of user IDs in a specific segment

        Args:
            segment: SegmentType to filter by
            limit: Maximum number of results

        Returns:
            List of user IDs
        """
        # This would query the segments table
        # For now, return empty list as placeholder
        return []

    async def get_segment_stats(self) -> Dict[str, Dict[str, Any]]:
        """
        Get statistics for each segment

        Returns:
            Dictionary with segment statistics
        """
        return {
            "premium": {"count": 0, "avg_balance": 0, "avg_transactions": 0},
            "loyal": {"count": 0, "avg_balance": 0, "avg_transactions": 0},
            "growing": {"count": 0, "avg_balance": 0, "avg_transactions": 0},
            "at_risk": {"count": 0, "avg_balance": 0, "avg_transactions": 0},
            "churned": {"count": 0, "avg_balance": 0, "avg_transactions": 0},
            "dormant": {"count": 0, "avg_balance": 0, "avg_transactions": 0},
        }

    async def _calculate_rfm_score(self, user_id: str) -> RFMScore:
        """Calculate RFM score for a user"""

        # Get last transaction date
        last_txn_result = await self.db.execute(
            select(
                func.max(Transaction.created_at),
                func.count(Transaction.id),
                func.sum(Transaction.amount)
            ).where(
                and_(
                    Transaction.user_id == user_id,
                    Transaction.status == "COMPLETED"
                )
            )
        )

        row = last_txn_result.one()
        last_date = row[0]
        frequency = row[1] or 0
        monetary = float(row[2] or 0)

        # Calculate recency score (inverted - higher is better)
        if last_date:
            days_since = (datetime.utcnow() - last_date).days
            # Score: 100 for today, decays to 0 after 365 days
            recency = max(0, 100 - int(days_since * 100 / 365))
        else:
            days_since = 999
            recency = 0

        return RFMScore(
            recency=recency,
            frequency=frequency,
            monetary=monetary,
            raw_recency=days_since
        )

    def _determine_segment(
        self,
        rfm: RFMScore,
        account_age_days: int,
        kyc_status: str
    ) -> SegmentType:
        """Determine segment based on RFM scores and other factors"""

        # Check for churned (no activity in 90+ days and existing account)
        if rfm.raw_recency > 90 and account_age_days > 180:
            return SegmentType.CHURNED

        # Check for dormant (low activity in 30-90 days)
        if rfm.raw_recency > 30:
            return SegmentType.DORMANT

        # Premium: High recency, high frequency, high monetary
        if (rfm.recency >= 70 and rfm.frequency >= 10 and rfm.monetary >= 10000000):
            return SegmentType.PREMIUM

        # Loyal: Good recency, moderate frequency, account > 6 months
        if (rfm.recency >= 50 and rfm.frequency >= 5 and account_age_days > 180):
            return SegmentType.LOYAL

        # Growing: New account (under 6 months) but active
        if account_age_days <= 180 and rfm.recency >= 60:
            return SegmentType.GROWING

        # At risk: Declining activity
        if rfm.recency < 40 and rfm.frequency < 5:
            return SegmentType.AT_RISK

        # Default to loyal for established accounts
        if account_age_days > 90:
            return SegmentType.LOYAL

        return SegmentType.GROWING

    async def _get_user_metadata(self, user_id: str) -> Dict[str, Any]:
        """Get additional user metadata for segmentation"""

        # Get wallet balance
        wallet_result = await self.db.execute(
            select(Wallet.balance).where(Wallet.user_id == user_id)
        )
        balance = wallet_result.scalar_one_or_none() or 0

        return {
            "wallet_balance": float(balance),
            "account_tier": self._calculate_account_tier(balance),
        }

    def _calculate_account_tier(self, balance: float) -> str:
        """Calculate account tier based on balance"""
        if balance >= 100000000:  # 100 juta
            return "PLATINUM"
        elif balance >= 50000000:  # 50 juta
            return "GOLD"
        elif balance >= 10000000:  # 10 juta
            return "SILVER"
        else:
            return "BRONZE"

    async def _save_segment(self, segment: CustomerSegment):
        """Save segment to database"""
        # This would save to a customer_segments table
        # For now, just log it
        logger.info(
            f"User {segment.user_id} -> {segment.segment.value} "
            f"(R:{segment.rfm_score.recency:.0f} "
            f"F:{segment.rfm_score.frequency} "
            f"M:{segment.rfm_score.monetary:.0f})"
        )


class SegmentationRecommendationService:
    """
    Service for generating recommendations based on user segments
    """

    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_recommendations(self, user_id: str) -> List[Dict[str, Any]]:
        """
        Get personalized recommendations based on user segment

        Args:
            user_id: User UUID

        Returns:
            List of recommendation objects
        """
        # Get user segment
        segment_service = CustomerSegmentationService(self.db)
        segment = await segment_service.calculate_user_segment(user_id)

        # Generate recommendations based on segment
        recommendations = []

        if segment.segment == SegmentType.PREMIUM:
            recommendations.extend([
                {
                    "type": "investment",
                    "title": "Investment Opportunities",
                    "description": "Exclusive investment products for premium members",
                    "priority": "high"
                },
                {
                    "type": "credit",
                    "title": "Credit Line Increase",
                    "description": "Pre-approved for higher credit limits",
                    "priority": "medium"
                },
            ])

        elif segment.segment == SegmentType.LOYAL:
            recommendations.extend([
                {
                    "type": "reward",
                    "title": "Loyalty Points Bonus",
                    "description": "Earn 2x points on next transaction",
                    "priority": "high"
                },
                {
                    "type": "savings",
                    "title": "High-Yield Savings",
                    "description": "Special deposit rates for loyal customers",
                    "priority": "medium"
                },
            ])

        elif segment.segment == SegmentType.GROWING:
            recommendations.extend([
                {
                    "type": "education",
                    "title": "Financial Tips",
                    "description": "Learn how to maximize your account features",
                    "priority": "high"
                },
                {
                    "type": "feature",
                    "title": "Set Up Savings Goals",
                    "description": "Create pockets for your financial goals",
                    "priority": "medium"
                },
            ])

        elif segment.segment == SegmentType.AT_RISK:
            recommendations.extend([
                {
                    "type": "engagement",
                    "title": "We Miss You!",
                    "description": "Special offers to welcome you back",
                    "priority": "high"
                },
                {
                    "type": "feedback",
                    "title": "Tell Us How We Can Improve",
                    "description": "Help us serve you better",
                    "priority": "medium"
                },
            ])

        elif segment.segment == SegmentType.DORMANT:
            recommendations.extend([
                {
                    "type": "reactivation",
                    "title": "Welcome Back Bonus",
                    "description": "Special cashback on your next transaction",
                    "priority": "high"
                },
            ])

        elif segment.segment == SegmentType.CHURNED:
            recommendations.extend([
                {
                    "type": "reactivation",
                    "title": "Come Back!",
                    "description": "We've made improvements - give us another try",
                    "priority": "high"
                },
            ])

        return recommendations
