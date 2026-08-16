from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import declarative_base
from sqlalchemy import Column, String, DateTime, JSON, Float, Boolean, Text, LargeBinary, text as sa_text
from datetime import datetime
from typing import Optional

from app.config import get_settings
import structlog

logger = structlog.get_logger(__name__)
settings = get_settings()

engine = None
async_session_maker = None

Base = declarative_base()


class KycVerificationEntity(Base):
    __tablename__ = "kyc_verifications"

    verification_id = Column(String, primary_key=True)
    user_id = Column(String, nullable=False, index=True)
    verification_type = Column(String, nullable=False, default="FULL_KYC")
    status = Column(String, nullable=False, default="PENDING", index=True)

    ktp_image_url = Column(String, nullable=True)
    selfie_image_url = Column(String, nullable=True)
    ktp_image_data = Column(LargeBinary, nullable=True)

    ktp_ocr_result = Column(JSON, nullable=True)
    liveness_result = Column(JSON, nullable=True)
    face_match_result = Column(JSON, nullable=True)
    dukcapil_result = Column(JSON, nullable=True)

    rejection_reason = Column(Text, nullable=True)

    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)
    completed_at = Column(DateTime, nullable=True)


async def init_db():
    global engine, async_session_maker
    engine = create_async_engine(
        settings.database_url,
        echo=False,
        pool_pre_ping=True,
        pool_size=10,
        max_overflow=20
    )
    async_session_maker = async_sessionmaker(
        engine,
        class_=AsyncSession,
        expire_on_commit=False
    )
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
        # KYC-FACE-001: ktp_image_data column added later; ALTER existing tables
        # (Postgres) idempotently, skip unsupported (SQLite) without failing.
        try:
            await conn.execute(
                sa_text(
                    "ALTER TABLE kyc_verifications "
                    "ADD COLUMN IF NOT EXISTS ktp_image_data BYTEA"
                )
            )
        except Exception as exc:  # SQLite has no ADD COLUMN IF NOT EXISTS
            logger.info("ktp_image_data migration skipped", error=str(exc))
    logger.info("Database connection pool created")


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
