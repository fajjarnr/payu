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


class DashboardEventType(str, Enum):
    TRANSACTION_COMPLETED = "TRANSACTION_COMPLETED"
    TRANSACTION_INITIATED = "TRANSACTION_INITIATED"
    WALLET_BALANCE_CHANGED = "WALLET_BALANCE_CHANGED"
    KYC_VERIFIED = "KYC_VERIFIED"
    USER_METRICS_UPDATED = "USER_METRICS_UPDATED"


class DashboardEvent(BaseModel):
    event_type: DashboardEventType
    user_id: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    data: dict = Field(default_factory=dict)


class TransactionCompletedEvent(BaseModel):
    transaction_id: str
    amount: float
    currency: str = "IDR"
    transaction_type: str
    category: str = "OTHER"
    recipient_id: Optional[str] = None
    merchant_id: Optional[str] = None


class WalletBalanceChangedEvent(BaseModel):
    wallet_id: str
    balance: float
    currency: str = "IDR"
    change_amount: float
    change_type: str = "CREDIT"


class KycVerifiedEvent(BaseModel):
    user_id: str
    kyc_status: str = "VERIFIED"


class UserMetricsUpdatedEvent(BaseModel):
    total_transactions: int
    total_amount: float
    average_transaction: float
    last_transaction_date: Optional[datetime] = None


class WebSocketConnectionRequest(BaseModel):
    user_id: str
    dashboard_type: str = "general"


class RiskProfile(str, Enum):
    CONSERVATIVE = "CONSERVATIVE"
    MODERATE = "MODERATE"
    AGGRESSIVE = "AGGRESSIVE"


class InvestmentTimeHorizon(str, Enum):
    SHORT_TERM = "SHORT_TERM"
    MEDIUM_TERM = "MEDIUM_TERM"
    LONG_TERM = "LONG_TERM"


class AssetClass(str, Enum):
    CASH = "CASH"
    FIXED_INCOME = "FIXED_INCOME"
    MUTUAL_FUNDS = "MUTUAL_FUNDS"
    DIGITAL_GOLD = "DIGITAL_GOLD"
    STOCKS = "STOCKS"
    BONDS = "BONDS"


class PortfolioAllocation(BaseModel):
    asset_class: AssetClass
    allocation_percentage: float = Field(ge=0, le=100, description="Allocation in percentage")
    expected_return: float = Field(ge=0, description="Expected annual return in percentage")
    risk_level: RiskProfile
    description: str


class RiskAssessmentQuestions(BaseModel):
    age: int = Field(ge=18, le=100, description="User's age")
    monthly_income: float = Field(gt=0, description="Monthly income in IDR")
    monthly_expenses: float = Field(gt=0, description="Monthly expenses in IDR")
    total_savings: float = Field(ge=0, description="Total savings in IDR")
    investment_experience: int = Field(ge=0, le=10, description="Years of investment experience (0-10)")
    risk_tolerance: str = Field(..., description="Risk tolerance: low, medium, or high")
    investment_goal: str = Field(..., description="Investment goal: retirement, wealth_growth, or emergency_fund")
    time_horizon: InvestmentTimeHorizon


class RiskAssessmentResult(BaseModel):
    risk_profile: RiskProfile
    risk_score: float = Field(ge=0, le=100, description="Risk score out of 100")
    description: str
    suitable_asset_classes: List[AssetClass]


class RoboAdvisoryResponse(BaseModel):
    user_id: str
    risk_assessment: RiskAssessmentResult
    portfolio_allocation: List[PortfolioAllocation]
    investment_recommendations: List[str]
    monthly_investment_amount: float
    expected_annual_return: float
    recommended_investment_products: List[dict]


class GetRoboAdvisoryRequest(BaseModel):
    user_id: str
    risk_questions: RiskAssessmentQuestions
    monthly_investment_amount: float = Field(gt=0, description="Monthly amount to invest in IDR")
