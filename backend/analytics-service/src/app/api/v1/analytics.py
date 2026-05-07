from fastapi import APIRouter, Depends, Header, Request, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from structlog import get_logger
from typing import Optional
from datetime import datetime
from slowapi.util import get_remote_address

from app.database import get_db_session
from app.models.schemas import (
    GetAnalyticsRequest,
    GetSpendingTrendsRequest,
    UserMetricsResponse,
    GetRecommendationsResponse,
    GetRoboAdvisoryRequest,
    GetFraudScoreRequest,
    FraudDetectionResult,
)
from app.services.analytics_service import AnalyticsService
from app.ml.robo_advisory import RoboAdvisoryEngine
from app.ml.fraud_detection import FraudDetectionEngine
from app.api.responses import ApiResponse
from app.api.idempotency import get_cached_result, cache_result
from app.api.auth import require_auth

logger = get_logger(__name__)
analytics_router = APIRouter(prefix="/analytics", tags=["Analytics"])


@analytics_router.get("/user/{user_id}/metrics")
async def get_user_metrics(
    request: Request, user_id: str,
    db: AsyncSession = Depends(get_db_session),
    auth: dict = Depends(require_auth)
):
    """BUG-SECURITY-016/017 FIX: Get user analytics metrics with Auth & IDOR check."""
    # Validate ownership
    if user_id != auth.get("sub") and user_id != auth.get("account_id"):
        raise HTTPException(status_code=403, detail="Forbidden: You can only access your own metrics")
    log = logger.bind(
        user_id=user_id, request_id=getattr(request.state, "request_id", None)
    )
    log.info("Fetching user metrics")

    try:
        service = AnalyticsService(db)
        metrics = await service.get_user_metrics(user_id)

        if not metrics:
            return ApiResponse.error(
                code="ANA_VAL_001",
                message="User not found",
                request_id=getattr(request.state, "request_id", None),
            ).model_dump()

        response_data = UserMetricsResponse(
            user_id=metrics.user_id,
            total_transactions=metrics.total_transactions,
            total_amount=metrics.total_amount,
            average_transaction=metrics.average_transaction,
            last_transaction_date=metrics.last_transaction_date,
            account_age_days=metrics.account_age_days,
            kyc_status=metrics.kyc_status,
        )

        return ApiResponse.success(
            data=response_data.model_dump(),
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()
    except Exception as e:
        log.error("Failed to fetch user metrics", exc_info=e)
        return ApiResponse.error(
            code="ANA_SYS_001",
            message="Failed to fetch user metrics",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()


@analytics_router.post("/spending/trends")
async def get_spending_trends(
    request: Request,
    request_data: GetSpendingTrendsRequest,
    db: AsyncSession = Depends(get_db_session),
    auth: dict = Depends(require_auth)
):
    """BUG-SECURITY-016/017 FIX: Get user spending trends with Auth & IDOR check."""
    # Validate ownership
    user_id = request_data.user_id
    if user_id != auth.get("sub") and user_id != auth.get("account_id"):
        raise HTTPException(status_code=403, detail="Forbidden: You can only access your own trends")
    log = logger.bind(
        user_id=request_data.user_id,
        request_id=getattr(request.state, "request_id", None),
    )
    log.info("Fetching spending trends")

    try:
        service = AnalyticsService(db)
        trends = await service.get_spending_trends(
            user_id=request_data.user_id,
            period_days=request_data.period_days,
            group_by=request_data.group_by,
        )

        return ApiResponse.success(
            data=trends, request_id=getattr(request.state, "request_id", None)
        ).model_dump()
    except Exception as e:
        log.error("Failed to fetch spending trends", exc_info=e)
        return ApiResponse.error(
            code="ANA_SYS_002",
            message="Failed to fetch spending trends",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()


@analytics_router.post("/cashflow")
async def get_cash_flow_analysis(
    request: Request,
    request_data: GetAnalyticsRequest,
    db: AsyncSession = Depends(get_db_session),
    auth: dict = Depends(require_auth)
):
    """BUG-SECURITY-016/017 FIX: Get user cash flow with Auth & IDOR check."""
    # Validate ownership
    user_id = request_data.user_id
    if user_id != auth.get("sub") and user_id != auth.get("account_id"):
        raise HTTPException(status_code=403, detail="Forbidden: You can only access your own cashflow")
    log = logger.bind(
        user_id=request_data.user_id,
        request_id=getattr(request.state, "request_id", None),
    )
    log.info("Fetching cash flow analysis")

    try:
        service = AnalyticsService(db)
        analysis = await service.get_cash_flow_analysis(
            user_id=request_data.user_id, period_days=request_data.period_days
        )

        return ApiResponse.success(
            data=analysis, request_id=getattr(request.state, "request_id", None)
        ).model_dump()
    except Exception as e:
        log.error("Failed to fetch cash flow analysis", exc_info=e)
        return ApiResponse.error(
            code="ANA_SYS_003",
            message="Failed to fetch cash flow analysis",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()


@analytics_router.get("/user/{user_id}/recommendations")
async def get_recommendations(
    request: Request, user_id: str,
    db: AsyncSession = Depends(get_db_session),
    auth: dict = Depends(require_auth)
):
    """BUG-SECURITY-016/017 FIX: Get personalized recommendations with Auth & IDOR check."""
    # Validate ownership
    if user_id != auth.get("sub") and user_id != auth.get("account_id"):
        raise HTTPException(status_code=403, detail="Forbidden: You can only access your own recommendations")
    log = logger.bind(
        user_id=user_id, request_id=getattr(request.state, "request_id", None)
    )
    log.info("Fetching recommendations")

    try:
        service = AnalyticsService(db)
        recommendations = await service.get_recommendations(user_id)

        response_data = GetRecommendationsResponse(
            user_id=user_id, recommendations=recommendations
        )

        return ApiResponse.success(
            data=response_data.model_dump(),
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()
    except Exception as e:
        log.error("Failed to fetch recommendations", exc_info=e)
        return ApiResponse.error(
            code="ANA_SYS_004",
            message="Failed to fetch recommendations",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()


@analytics_router.post("/robo-advisory")
async def get_robo_advisory(
    request: Request,
    request_data: GetRoboAdvisoryRequest,
    idempotency_key: Optional[str] = Header(None, alias="Idempotency-Key"),
    auth: dict = Depends(require_auth)
):
    """
    BUG-SECURITY-016/017 FIX: Generate robo-advisory with Auth & IDOR check.
    Supports idempotency for safe retries.
    """
    # Validate ownership
    user_id = request_data.user_id
    if user_id != auth.get("sub") and user_id != auth.get("account_id"):
        raise HTTPException(status_code=403, detail="Forbidden: You can only access your own robo-advisory")
    log = logger.bind(
        user_id=request_data.user_id,
        request_id=getattr(request.state, "request_id", None),
        idempotency_key=idempotency_key,
    )
    log.info("Generating robo-advisory recommendations")

    # Check idempotency cache
    if idempotency_key:
        cached = await get_cached_result(
            idempotency_key=idempotency_key,
            request_path=str(request.url.path),
            request_body=await request.body() if hasattr(request, "body") else None,
        )
        if cached:
            log.info("Returning cached robo-advisory result")
            return ApiResponse.success(
                data=cached, request_id=getattr(request.state, "request_id", None)
            ).model_dump()

    try:
        robo_advisory_engine = RoboAdvisoryEngine()
        advisory = robo_advisory_engine.generate_robo_advisory(
            user_id=request_data.user_id,
            questions=request_data.risk_questions,
            monthly_investment_amount=request_data.monthly_investment_amount,
        )

        response_data = advisory

        # Store result for idempotency
        if idempotency_key:
            await cache_result(
                idempotency_key=idempotency_key,
                request_path=str(request.url.path),
                result=response_data,
            )

        return ApiResponse.success(
            data=response_data, request_id=getattr(request.state, "request_id", None)
        ).model_dump()
    except Exception as e:
        log.error("Failed to generate robo-advisory recommendations", exc_info=e)
        return ApiResponse.error(
            code="ANA_SYS_005",
            message="Failed to generate robo-advisory recommendations",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()


@analytics_router.post("/fraud/score")
async def calculate_fraud_score(
    request: Request,
    request_data: GetFraudScoreRequest,
    idempotency_key: Optional[str] = Header(None, alias="Idempotency-Key"),
    auth: dict = Depends(require_auth)
):
    """
    BUG-SECURITY-016/017 FIX: Calculate fraud score with Auth & IDOR check.
    Supports idempotency for safe retries.
    Rate limit: 100 requests per minute per IP.
    """
    # Validate ownership
    user_id = request_data.user_id
    if user_id != auth.get("sub") and user_id != auth.get("account_id"):
        raise HTTPException(status_code=403, detail="Forbidden: You can only calculate fraud score for your own transactions")
    log = logger.bind(
        transaction_id=request_data.transaction_id,
        user_id=request_data.user_id,
        request_id=getattr(request.state, "request_id", None),
        idempotency_key=idempotency_key,
    )
    log.info("Calculating fraud score")

    # Apply rate limiting manually since we're using APIRouter
    limiter = request.app.state.limiter
    try:
        await limiter.check(request, get_remote_address(request), "100/minute")
    except Exception:
        return ApiResponse.error(
            code="ANA_RAT_001",
            message="Rate limit exceeded. Please try again later.",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()

    # Check idempotency cache
    if idempotency_key:
        cached = await get_cached_result(
            idempotency_key=idempotency_key,
            request_path=str(request.url.path),
            request_body=await request.body() if hasattr(request, "body") else None,
        )
        if cached:
            log.info("Returning cached fraud score result")
            return ApiResponse.success(
                data=cached, request_id=getattr(request.state, "request_id", None)
            ).model_dump()

    try:
        fraud_engine = FraudDetectionEngine()
        transaction_data = {
            "transaction_id": request_data.transaction_id,
            "user_id": request_data.user_id,
            "amount": request_data.amount,
            "currency": request_data.currency,
            "type": request_data.transaction_type,
            "metadata": request_data.metadata,
        }

        result = await fraud_engine.calculate_fraud_score(transaction_data)
        response_data = result

        # Store result for idempotency
        if idempotency_key:
            await cache_result(
                idempotency_key=idempotency_key,
                request_path=str(request.url.path),
                result=response_data,
            )

        return ApiResponse.success(
            data=response_data, request_id=getattr(request.state, "request_id", None)
        ).model_dump()
    except Exception as e:
        log.error("Failed to calculate fraud score", exc_info=e)
        return ApiResponse.error(
            code="ANA_SYS_006",
            message="Failed to calculate fraud score",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()


@analytics_router.get("/fraud/transaction/{transaction_id}")
async def get_transaction_fraud_score(
    request: Request, transaction_id: str,
    db: AsyncSession = Depends(get_db_session),
    auth: dict = Depends(require_auth)
):
    """BUG-SECURITY-016/017 FIX: Get stored fraud score with Auth & IDOR check."""
    log = logger.bind(
        transaction_id=transaction_id,
        request_id=getattr(request.state, "request_id", None),
    )
    log.info("Fetching transaction fraud score")

    try:
        from sqlalchemy import select
        from app.database import FraudScoreEntity

        query = select(FraudScoreEntity).where(
            FraudScoreEntity.transaction_id == transaction_id
        )
        result = await db.execute(query)
        fraud_entity = result.scalar_one_or_none()

        if not fraud_entity:
            return ApiResponse.error(
                code="ANA_VAL_002",
                message="Fraud score not found",
                request_id=getattr(request.state, "request_id", None),
            ).model_dump()

        # Validate ownership
        if fraud_entity.user_id != auth.get("sub") and fraud_entity.user_id != auth.get("account_id"):
            raise HTTPException(status_code=403, detail="Forbidden: You can only access your own fraud scores")

        response_data = FraudDetectionResult(
            fraud_score={
                "transaction_id": fraud_entity.transaction_id,
                "user_id": fraud_entity.user_id,
                "risk_score": fraud_entity.risk_score,
                "risk_level": fraud_entity.risk_level,
                "risk_factors": fraud_entity.risk_factors,
                "is_suspicious": fraud_entity.is_suspicious,
                "recommended_action": fraud_entity.recommended_action,
                "scored_at": fraud_entity.scored_at,
            },
            is_blocked=fraud_entity.is_blocked,
            requires_review=fraud_entity.requires_review,
            rule_triggers=fraud_entity.rule_triggers,
        )

        return ApiResponse.success(
            data=response_data.model_dump(),
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()
    except Exception as e:
        log.error("Failed to fetch fraud score", exc_info=e)
        return ApiResponse.error(
            code="ANA_SYS_007",
            message="Failed to fetch fraud score",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()


@analytics_router.get("/fraud/user/{user_id}/high-risk")
async def get_user_high_risk_transactions(
    request: Request, user_id: str,
    db: AsyncSession = Depends(get_db_session),
    auth: dict = Depends(require_auth)
):
    """BUG-SECURITY-016/017 FIX: Get high-risk transactions with Auth & IDOR check."""
    # Validate ownership
    if user_id != auth.get("sub") and user_id != auth.get("account_id"):
        raise HTTPException(status_code=403, detail="Forbidden: You can only access your own high-risk transactions")
    log = logger.bind(
        user_id=user_id, request_id=getattr(request.state, "request_id", None)
    )
    log.info("Fetching high-risk transactions")

    try:
        from sqlalchemy import select
        from app.database import FraudScoreEntity
        from datetime import timedelta

        cutoff_date = datetime.utcnow() - timedelta(days=30)

        query = (
            select(FraudScoreEntity)
            .where(FraudScoreEntity.user_id == user_id)
            .where(FraudScoreEntity.is_suspicious.is_(True))
            .where(FraudScoreEntity.scored_at > cutoff_date)
            .order_by(FraudScoreEntity.scored_at.desc())
        )
        result = await db.execute(query)
        high_risk_transactions = result.scalars().all()

        response_data = [
            {
                "transaction_id": txn.transaction_id,
                "risk_score": txn.risk_score,
                "risk_level": txn.risk_level,
                "scored_at": txn.scored_at.isoformat() if txn.scored_at else None,
            }
            for txn in high_risk_transactions
        ]

        return ApiResponse.success(
            data=response_data, request_id=getattr(request.state, "request_id", None)
        ).model_dump()
    except Exception as e:
        log.error("Failed to fetch high-risk transactions", exc_info=e)
        return ApiResponse.error(
            code="ANA_SYS_008",
            message="Failed to fetch high-risk transactions",
            request_id=getattr(request.state, "request_id", None),
        ).model_dump()

@analytics_router.get("/")
async def get_analytics_root(request: Request):
    """Get analytics service status and available endpoints."""
    return ApiResponse.create_success(
        data={
            "status": "operational",
            "service": "analytics-service",
            "version": "1.0.0",
            "endpoints": [
                "GET /analytics",
                "GET /analytics/metrics",
                "GET /analytics/user/{user_id}/metrics",
                "POST /analytics/spending/trends",
                "POST /analytics/cashflow",
                "GET /analytics/user/{user_id}/recommendations",
                "POST /analytics/robo-advisory",
                "POST /analytics/fraud/score",
                "GET /analytics/fraud/transaction/{transaction_id}",
                "GET /analytics/fraud/user/{user_id}/high-risk",
            ]
        },
        request_id=getattr(request.state, "request_id", None),
    ).model_dump()


@analytics_router.get("/metrics")
async def get_analytics_metrics(request: Request):
    """Get analytics service operational metrics and available endpoints."""
    return ApiResponse.success(
        data={
            "status": "operational",
            "service": "analytics-service",
            "version": "1.0.0",
            "endpoints": [
                "GET /analytics/metrics",
                "GET /analytics/user/{user_id}/metrics",
                "POST /analytics/spending/trends",
                "POST /analytics/cashflow",
                "GET /analytics/user/{user_id}/recommendations",
                "POST /analytics/robo-advisory",
                "POST /analytics/fraud/score",
                "GET /analytics/fraud/transaction/{transaction_id}",
                "GET /analytics/fraud/user/{user_id}/high-risk",
            ]
        },
        request_id=getattr(request.state, "request_id", None),
    ).model_dump()
