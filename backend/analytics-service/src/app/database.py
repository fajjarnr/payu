from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import declarative_base
from sqlalchemy import (
    Column,
    String,
    DateTime,
    Float,
    Integer,
    BigInteger,
    JSON,
    Index,
    Text,
    Boolean,
)
from datetime import datetime

from app.config import get_settings
import structlog

logger = structlog.get_logger(__name__)
settings = get_settings()

engine = None
async_session_maker = None

Base = declarative_base()


class TransactionAnalyticsEntity(Base):
    __tablename__ = "transaction_analytics"

    event_id = Column(String, primary_key=True)
    user_id = Column(String, nullable=False, index=True)
    transaction_id = Column(String, nullable=False)
    amount = Column(Float, nullable=False)
    currency = Column(String, default="IDR")
    transaction_type = Column(String, nullable=False, index=True)
    category = Column(String, nullable=True, index=True)
    status = Column(String, nullable=False)
    recipient_id = Column(String, nullable=True)
    merchant_id = Column(String, nullable=True)
    event_metadata = Column("metadata", JSON, nullable=True)

    timestamp = Column(DateTime, nullable=False, index=True)

    __table_args__ = (
        Index("idx_transactions_user_time", "user_id", "timestamp"),
        Index("idx_transactions_type_time", "transaction_type", "timestamp"),
    )


class WalletBalanceEntity(Base):
    __tablename__ = "wallet_balance_history"

    event_id = Column(String, primary_key=True)
    user_id = Column(String, nullable=False, index=True)
    wallet_id = Column(String, nullable=False)
    balance = Column(Float, nullable=False)
    currency = Column(String, default="IDR")
    change_amount = Column(Float, nullable=False)
    change_type = Column(String, nullable=False)

    timestamp = Column(DateTime, nullable=False, index=True)

    __table_args__ = (Index("idx_wallet_balance_user_time", "user_id", "timestamp"),)


class UserActivityEntity(Base):
    __tablename__ = "user_activity_analytics"

    event_id = Column(String, primary_key=True)
    user_id = Column(String, nullable=False, index=True)
    activity_type = Column(String, nullable=False, index=True)
    session_id = Column(String, nullable=True)
    device_type = Column(String, nullable=True)
    ip_address = Column(String, nullable=True)
    user_agent = Column(Text, nullable=True)
    duration_seconds = Column(Integer, nullable=True)
    event_metadata = Column("metadata", JSON, nullable=True)

    timestamp = Column(DateTime, nullable=False, index=True)

    __table_args__ = (Index("idx_user_activity_user_time", "user_id", "timestamp"),)


class UserMetricsEntity(Base):
    __tablename__ = "user_metrics"

    user_id = Column(String, primary_key=True)
    total_transactions = Column(BigInteger, default=0)
    total_amount = Column(Float, default=0.0)
    average_transaction = Column(Float, default=0.0)
    last_transaction_date = Column(DateTime, nullable=True)
    account_age_days = Column(Integer, default=0)
    kyc_status = Column(String, nullable=True)

    updated_at = Column(
        DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False
    )

    __table_args__ = (Index("idx_user_metrics_kyc", "kyc_status"),)


class RecommendationEntity(Base):
    __tablename__ = "recommendations"

    recommendation_id = Column(String, primary_key=True)
    user_id = Column(String, nullable=False, index=True)
    recommendation_type = Column(String, nullable=False, index=True)
    title = Column(String, nullable=False)
    description = Column(Text, nullable=True)
    action_url = Column(String, nullable=True)
    priority = Column(Integer, default=0)
    is_dismissed = Column(Boolean, default=False, index=True)
    meta_data = Column("metadata", JSON, nullable=True)

    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    expires_at = Column(DateTime, nullable=True)
    dismissed_at = Column(DateTime, nullable=True)

    __table_args__ = (
        Index("idx_recommendations_user_created", "user_id", "created_at"),
    )


class FraudScoreEntity(Base):
    __tablename__ = "fraud_scores"

    score_id = Column(String, primary_key=True)
    transaction_id = Column(String, nullable=False, index=True)
    user_id = Column(String, nullable=False, index=True)
    risk_score = Column(Float, nullable=False)
    risk_level = Column(String, nullable=False, index=True)
    risk_factors = Column(JSON, nullable=True)
    is_suspicious = Column(Boolean, default=False, index=True)
    recommended_action = Column(String, nullable=True)
    is_blocked = Column(Boolean, default=False, index=True)
    requires_review = Column(Boolean, default=False)
    rule_triggers = Column(JSON, nullable=True)

    scored_at = Column(DateTime, default=datetime.utcnow, nullable=False, index=True)

    __table_args__ = (
        Index("idx_fraud_scores_user_time", "user_id", "scored_at"),
        Index("idx_fraud_scores_risk_level", "risk_level"),
        Index("idx_fraud_scores_suspicious", "is_suspicious"),
    )


async def init_db():
    global engine, async_session_maker
    engine = create_async_engine(
        settings.database_url,
        echo=False,
        pool_pre_ping=True,
        pool_size=10,
        max_overflow=20,
    )
    async_session_maker = async_sessionmaker(
        engine, class_=AsyncSession, expire_on_commit=False
    )

    await _create_hypertables()

    logger.info("Database connection pool created")


async def _create_hypertables():
    async with async_session_maker() as session:
        from sqlalchemy import text

        create_timescale = text(
            """
            SELECT create_hypertable('transaction_analytics', 'timestamp',
                chunk_time_interval => interval '7 days');
        """
        )

        create_wallet_hypertable = text(
            """
            SELECT create_hypertable('wallet_balance_history', 'timestamp',
                chunk_time_interval => interval '7 days');
        """
        )

        create_activity_hypertable = text(
            """
            SELECT create_hypertable('user_activity_analytics', 'timestamp',
                chunk_time_interval => interval '7 days');
        """
        )

        create_fraud_hypertable = text(
            """
            SELECT create_hypertable('fraud_scores', 'scored_at',
                chunk_time_interval => interval '7 days');
        """
        )

        try:
            await session.execute(create_timescale)
            await session.execute(create_wallet_hypertable)
            await session.execute(create_activity_hypertable)
            await session.execute(create_fraud_hypertable)
            await session.commit()
            logger.info("TimescaleDB hypertables created")
        except Exception as e:
            await session.rollback()
            logger.warning("TimescaleDB hypertables may already exist", error=str(e))


async def close_db():
    global engine
    if engine:
        await engine.dispose()
        logger.info("Database connection pool closed")


async def get_db_session() -> AsyncSession:
    if async_session_maker is None:
        raise RuntimeError("Database not initialized")
    async with async_session_maker() as session:
        yield session
