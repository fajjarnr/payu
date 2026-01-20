from pydantic import BaseModel, Field, field_validator
from typing import Optional, List
from datetime import datetime
from decimal import Decimal
from enum import Enum


class TransactionType(str, Enum):
    TRANSFER = "TRANSFER"
    PAYMENT = "PAYMENT"
    TOPUP = "TOPUP"
    BILL_PAYMENT = "BILL_PAYMENT"
    QR_PAYMENT = "QR_PAYMENT"
    WITHDRAWAL = "WITHDRAWAL"


class TransactionCategory(str, Enum):
    FOOD = "FOOD"
    TRANSPORT = "TRANSPORT"
    SHOPPING = "SHOPPING"
    BILLS = "BILLS"
    ENTERTAINMENT = "ENTERTAINMENT"
    HEALTH = "HEALTH"
    EDUCATION = "EDUCATION"
    TRANSFER = "TRANSFER"
    OTHER = "OTHER"


class RecommendationType(str, Enum):
    SAVINGS_GOAL = "SAVINGS_GOAL"
    BUDGET_ALERT = "BUDGET_ALERT"
    SPENDING_TREND = "SPENDING_TREND"
    NEW_FEATURE = "NEW_FEATURE"
    PROMOTION = "PROMOTION"
    INVESTMENT = "INVESTMENT"


class TransactionInsight(BaseModel):
    total_transactions: int
    total_amount: Decimal
    average_transaction: Decimal
    transaction_type: str
    category: str
    period_start: datetime
    period_end: datetime


class SpendingPattern(BaseModel):
    category: str
    amount: Decimal
    percentage: float
    transaction_count: int
    trend: str = Field(default="stable", description="increasing, decreasing, stable")


class UserMetricsResponse(BaseModel):
    user_id: str
    total_transactions: int
    total_amount: Decimal
    average_transaction: Decimal
    last_transaction_date: Optional[datetime]
    account_age_days: int
    kyc_status: Optional[str]


class SpendingTrendResponse(BaseModel):
    period: str
    total_spending: Decimal
    spending_by_category: List[SpendingPattern]
    month_over_month_change: Optional[float]
    top_merchants: List[dict]


class CashFlowAnalysis(BaseModel):
    period: str
    income: Decimal
    expenses: Decimal
    net_cash_flow: Decimal
    income_by_source: List[dict]
    expenses_by_category: List[SpendingPattern]


class Recommendation(BaseModel):
    recommendation_id: str
    recommendation_type: RecommendationType
    title: str
    description: str
    action_url: Optional[str]
    priority: int = Field(default=0, description="Higher is more important")
    metadata: dict = Field(default_factory=dict)


class GetRecommendationsResponse(BaseModel):
    user_id: str
    recommendations: List[Recommendation]


class GetAnalyticsRequest(BaseModel):
    user_id: str = Field(..., description="User ID")
    period_days: int = Field(default=30, description="Period in days")


class GetSpendingTrendsRequest(BaseModel):
    user_id: str
    period_days: int = 30
    group_by: str = Field(default="category", description="category, merchant, or day")


class ErrorResponse(BaseModel):
    detail: str
    error_code: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)
