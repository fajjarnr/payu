from sqlalchemy import select, func, and_, desc
from sqlalchemy.ext.asyncio import AsyncSession
from datetime import datetime, timedelta
from decimal import Decimal
from structlog import get_logger
from typing import List, Dict, Any

from app.database import TransactionAnalyticsEntity, WalletBalanceEntity, UserMetricsEntity
from app.models.schemas import (
    SpendingPattern,
    SpendingTrendResponse,
    CashFlowAnalysis,
    TransactionCategory
)
from app.ml.recommendation_engine import RecommendationEngine

logger = get_logger(__name__)


class AnalyticsService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.recommendation_engine = RecommendationEngine()

    async def get_user_metrics(self, user_id: str) -> UserMetricsEntity | None:
        result = await self.db.execute(
            select(UserMetricsEntity).where(UserMetricsEntity.user_id == user_id)
        )
        return result.scalar_one_or_none()

    async def get_spending_trends(
        self,
        user_id: str,
        period_days: int = 30,
        group_by: str = "category"
    ) -> SpendingTrendResponse:
        end_date = datetime.utcnow()
        start_date = end_date - timedelta(days=period_days)

        if group_by == "category":
            result = await self.db.execute(
                select(
                    TransactionAnalyticsEntity.category,
                    func.sum(TransactionAnalyticsEntity.amount).label('total_amount'),
                    func.count(TransactionAnalyticsEntity.event_id).label('transaction_count')
                )
                .where(
                    and_(
                        TransactionAnalyticsEntity.user_id == user_id,
                        TransactionAnalyticsEntity.status == 'COMPLETED',
                        TransactionAnalyticsEntity.timestamp >= start_date,
                        TransactionAnalyticsEntity.timestamp <= end_date,
                        TransactionAnalyticsEntity.transaction_type == 'PAYMENT'
                    )
                )
                .group_by(TransactionAnalyticsEntity.category)
                .order_by(desc('total_amount'))
            )

            total_amount = Decimal(str(result.column_descriptions[0]['type'].__name__))
            spending_by_category = []
            
            for row in result:
                category = row.category
                amount = Decimal(str(row.total_amount))
                count = row.transaction_count
                percentage = float(amount / total_amount * 100) if total_amount > 0 else 0.0

                spending_by_category.append(
                    SpendingPattern(
                        category=category,
                        amount=amount,
                        percentage=percentage,
                        transaction_count=count,
                        trend="stable"
                    )
                )

            total_spending = Decimal(sum(float(p.amount) for p in spending_by_category))

            mom_change = await self._calculate_mom_change(user_id, period_days)

            top_merchants = await self._get_top_merchants(user_id, period_days)

            return SpendingTrendResponse(
                period=f"{period_days} days",
                total_spending=total_spending,
                spending_by_category=spending_by_category,
                month_over_month_change=mom_change,
                top_merchants=top_merchants
            )

    async def get_cash_flow_analysis(
        self,
        user_id: str,
        period_days: int = 30
    ) -> CashFlowAnalysis:
        end_date = datetime.utcnow()
        start_date = end_date - timedelta(days=period_days)

        income_result = await self.db.execute(
            select(func.sum(TransactionAnalyticsEntity.amount))
            .where(
                and_(
                    TransactionAnalyticsEntity.user_id == user_id,
                    TransactionAnalyticsEntity.status == 'COMPLETED',
                    TransactionAnalyticsEntity.timestamp >= start_date,
                    TransactionAnalyticsEntity.timestamp <= end_date,
                    TransactionAnalyticsEntity.change_type == 'CREDIT'
                )
            )
        )

        expenses_result = await self.db.execute(
            select(func.sum(TransactionAnalyticsEntity.amount))
            .where(
                and_(
                    TransactionAnalyticsEntity.user_id == user_id,
                    TransactionAnalyticsEntity.status == 'COMPLETED',
                    TransactionAnalyticsEntity.timestamp >= start_date,
                    TransactionAnalyticsEntity.timestamp <= end_date,
                    TransactionAnalyticsEntity.change_type == 'DEBIT'
                )
            )
        )

        income = Decimal(str(income_result.scalar() or 0))
        expenses = Decimal(str(expenses_result.scalar() or 0))
        net_cash_flow = income - expenses

        income_by_source = await self._get_income_by_source(user_id, period_days)
        expenses_by_category = await self._get_expenses_by_category(user_id, period_days)

        return CashFlowAnalysis(
            period=f"{period_days} days",
            income=income,
            expenses=expenses,
            net_cash_flow=net_cash_flow,
            income_by_source=income_by_source,
            expenses_by_category=expenses_by_category
        )

    async def get_recommendations(self, user_id: str) -> List[Dict[str, Any]]:
        user_metrics = await self.get_user_metrics(user_id)
        spending_trends = await self.get_spending_trends(user_id, 30)

        return self.recommendation_engine.generate_recommendations(
            user_metrics,
            spending_trends
        )

    async def _calculate_mom_change(self, user_id: str, period_days: int) -> float:
        current_end = datetime.utcnow()
        current_start = current_end - timedelta(days=period_days)
        previous_end = current_start
        previous_start = previous_end - timedelta(days=period_days)

        current_total = await self.db.execute(
            select(func.sum(TransactionAnalyticsEntity.amount))
            .where(
                and_(
                    TransactionAnalyticsEntity.user_id == user_id,
                    TransactionAnalyticsEntity.status == 'COMPLETED',
                    TransactionAnalyticsEntity.timestamp >= current_start,
                    TransactionAnalyticsEntity.timestamp <= current_end
                )
            )
        )

        previous_total = await self.db.execute(
            select(func.sum(TransactionAnalyticsEntity.amount))
            .where(
                and_(
                    TransactionAnalyticsEntity.user_id == user_id,
                    TransactionAnalyticsEntity.status == 'COMPLETED',
                    TransactionAnalyticsEntity.timestamp >= previous_start,
                    TransactionAnalyticsEntity.timestamp <= previous_end
                )
            )
        )

        current = float(current_total.scalar() or 0)
        previous = float(previous_total.scalar() or 0)

        if previous == 0:
            return 0.0

        return ((current - previous) / previous) * 100

    async def _get_top_merchants(self, user_id: str, period_days: int) -> List[Dict]:
        end_date = datetime.utcnow()
        start_date = end_date - timedelta(days=period_days)

        result = await self.db.execute(
            select(
                TransactionAnalyticsEntity.merchant_id,
                func.sum(TransactionAnalyticsEntity.amount).label('total_amount'),
                func.count(TransactionAnalyticsEntity.event_id).label('transaction_count')
            )
            .where(
                and_(
                    TransactionAnalyticsEntity.user_id == user_id,
                    TransactionAnalyticsEntity.status == 'COMPLETED',
                    TransactionAnalyticsEntity.timestamp >= start_date,
                    TransactionAnalyticsEntity.timestamp <= end_date,
                    TransactionAnalyticsEntity.merchant_id.isnot(None)
                )
            )
            .group_by(TransactionAnalyticsEntity.merchant_id)
            .order_by(desc('total_amount'))
            .limit(5)
        )

        return [
            {
                "merchant_id": row.merchant_id,
                "total_amount": float(row.total_amount),
                "transaction_count": row.transaction_count
            }
            for row in result
        ]

    async def _get_income_by_source(self, user_id: str, period_days: int) -> List[Dict]:
        return []

    async def _get_expenses_by_category(self, user_id: str, period_days: int) -> List[SpendingPattern]:
        trends = await self.get_spending_trends(user_id, period_days)
        return trends.spending_by_category
