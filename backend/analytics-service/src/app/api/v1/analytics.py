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
    GetRecommendationsResponse,
    GetRoboAdvisoryRequest,
    RoboAdvisoryResponse,
    GetFraudScoreRequest,
    FraudDetectionResult
)
from app.services.analytics_service import AnalyticsService
from app.ml.robo_advisory import RoboAdvisoryEngine
from app.ml.fraud_detection import FraudDetectionEngine

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


@analytics_router.post("/robo-advisory", response_model=RoboAdvisoryResponse)
async def get_robo_advisory(request: GetRoboAdvisoryRequest):
    log = logger.bind(user_id=request.user_id)
    log.info("Generating robo-advisory recommendations")

    try:
        robo_advisory_engine = RoboAdvisoryEngine()
        advisory = robo_advisory_engine.generate_robo_advisory(
            user_id=request.user_id,
            questions=request.risk_questions,
            monthly_investment_amount=request.monthly_investment_amount
        )

        return advisory
    except Exception as e:
        log.error("Failed to generate robo-advisory recommendations", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "ANA_SYS_005", "detail": str(e)}
        )


@analytics_router.post("/fraud/score", response_model=FraudDetectionResult)
async def calculate_fraud_score(request: GetFraudScoreRequest):
    log = logger.bind(transaction_id=request.transaction_id, user_id=request.user_id)
    log.info("Calculating fraud score")

    try:
        fraud_engine = FraudDetectionEngine()
        transaction_data = {
            "transaction_id": request.transaction_id,
            "user_id": request.user_id,
            "amount": request.amount,
            "currency": request.currency,
            "type": request.transaction_type,
            "metadata": request.metadata
        }

        result = await fraud_engine.calculate_fraud_score(transaction_data)

        return result
    except Exception as e:
        log.error("Failed to calculate fraud score", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "ANA_SYS_006", "detail": str(e)}
        )


@analytics_router.get("/fraud/transaction/{transaction_id}", response_model=FraudDetectionResult)
async def get_transaction_fraud_score(
    transaction_id: str,
    db: AsyncSession = Depends(get_db_session)
):
    log = logger.bind(transaction_id=transaction_id)
    log.info("Fetching transaction fraud score")

    try:
        from sqlalchemy import select
        from app.database import FraudScoreEntity

        query = select(FraudScoreEntity).where(FraudScoreEntity.transaction_id == transaction_id)
        result = await db.execute(query)
        fraud_entity = result.scalar_one_or_none()

        if not fraud_entity:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"error_code": "ANA_VAL_002", "detail": "Fraud score not found"}
            )

        return FraudDetectionResult(
            fraud_score={
                "transaction_id": fraud_entity.transaction_id,
                "user_id": fraud_entity.user_id,
                "risk_score": fraud_entity.risk_score,
                "risk_level": fraud_entity.risk_level,
                "risk_factors": fraud_entity.risk_factors,
                "is_suspicious": fraud_entity.is_suspicious,
                "recommended_action": fraud_entity.recommended_action,
                "scored_at": fraud_entity.scored_at
            },
            is_blocked=fraud_entity.is_blocked,
            requires_review=fraud_entity.requires_review,
            rule_triggers=fraud_entity.rule_triggers
        )
    except HTTPException:
        raise
    except Exception as e:
        log.error("Failed to fetch fraud score", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "ANA_SYS_007", "detail": str(e)}
        )


@analytics_router.get("/fraud/user/{user_id}/high-risk", response_model=list)
async def get_user_high_risk_transactions(
    user_id: str,
    db: AsyncSession = Depends(get_db_session)
):
    log = logger.bind(user_id=user_id)
    log.info("Fetching high-risk transactions")

    try:
        from sqlalchemy import select
        from app.database import FraudScoreEntity
        from datetime import timedelta

        cutoff_date = datetime.utcnow() - timedelta(days=30)

        query = (
            select(FraudScoreEntity)
            .where(FraudScoreEntity.user_id == user_id)
            .where(FraudScoreEntity.is_suspicious == True)
            .where(FraudScoreEntity.scored_at > cutoff_date)
            .order_by(FraudScoreEntity.scored_at.desc())
        )
        result = await db.execute(query)
        high_risk_transactions = result.scalars().all()

        return [
            {
                "transaction_id": txn.transaction_id,
                "risk_score": txn.risk_score,
                "risk_level": txn.risk_level,
                "scored_at": txn.scored_at
            }
            for txn in high_risk_transactions
        ]
    except Exception as e:
        log.error("Failed to fetch high-risk transactions", exc_info=e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error_code": "ANA_SYS_008", "detail": str(e)}
        )
