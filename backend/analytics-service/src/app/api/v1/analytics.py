from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from structlog import get_logger
from typing import List

from app.database import get_db_session
from app.models.schemas import (
    GetAnalyticsRequest,
    GetSpendingTrendsRequest,
    UserMetricsResponse,
    SpendingTrendResponse,
    CashFlowAnalysis,
    GetRecommendationsResponse
)
from app.services.analytics_service import AnalyticsService

logger = get_logger(__name__)
analytics_router = APIRouter(prefix="/analytics", tags=["Analytics"])


@analytics_router.get("/user/{user_id}/metrics", response_model=UserMetricsResponse)
async def get_user_metrics(
    user_id: str,
    db: AsyncSession = Depends(get_db_session)
):
    log = logger.bind(user_id=user_id)
    log.info("Fetching user metrics")

    try:
        service = AnalyticsService(db)
        metrics = await service.get_user_metrics(user_id)

        if not metrics:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"error_code": "ANA_VAL_001", "detail": "User not found"}
            )

        return UserMetricsResponse(
            user_id=metrics.user_id,
            total_transactions=metrics.total_transactions,
            total_amount=metrics.total_amount,
            average_transaction=metrics.average_transaction,
            last_transaction_date=metrics.last_transaction_date,
            account_age_days=metrics.account_age_days,
            kyc_status=metrics.kyc_status
        )
    except HTTPException:
        raise
    except Exception as e:
        log.error("Failed to fetch user metrics", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "ANA_SYS_001", "detail": str(e)}
        )


@analytics_router.post("/spending/trends", response_model=SpendingTrendResponse)
async def get_spending_trends(
    request: GetSpendingTrendsRequest,
    db: AsyncSession = Depends(get_db_session)
):
    log = logger.bind(user_id=request.user_id)
    log.info("Fetching spending trends")

    try:
        service = AnalyticsService(db)
        trends = await service.get_spending_trends(
            user_id=request.user_id,
            period_days=request.period_days,
            group_by=request.group_by
        )

        return trends
    except Exception as e:
        log.error("Failed to fetch spending trends", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "ANA_SYS_002", "detail": str(e)}
        )


@analytics_router.post("/cashflow", response_model=CashFlowAnalysis)
async def get_cash_flow_analysis(
    request: GetAnalyticsRequest,
    db: AsyncSession = Depends(get_db_session)
):
    log = logger.bind(user_id=request.user_id)
    log.info("Fetching cash flow analysis")

    try:
        service = AnalyticsService(db)
        analysis = await service.get_cash_flow_analysis(
            user_id=request.user_id,
            period_days=request.period_days
        )

        return analysis
    except Exception as e:
        log.error("Failed to fetch cash flow analysis", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "ANA_SYS_003", "detail": str(e)}
        )


@analytics_router.get("/user/{user_id}/recommendations", response_model=GetRecommendationsResponse)
async def get_recommendations(
    user_id: str,
    db: AsyncSession = Depends(get_db_session)
):
    log = logger.bind(user_id=user_id)
    log.info("Fetching recommendations")

    try:
        service = AnalyticsService(db)
        recommendations = await service.get_recommendations(user_id)

        return GetRecommendationsResponse(
            user_id=user_id,
            recommendations=recommendations
        )
    except Exception as e:
        log.error("Failed to fetch recommendations", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "ANA_SYS_004", "detail": str(e)}
        )
