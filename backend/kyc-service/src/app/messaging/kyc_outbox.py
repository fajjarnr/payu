"""Transactional outbox for KYC events (ARCH-KYC-002).

Event rows are inserted in the same DB transaction as the verification
mutation; a background publisher wraps them in a CloudEvents 1.0.2 envelope
and publishes to ``payu.kyc.<event>.v1`` topics. Delivery is at-least-once;
consumers must be idempotent.
"""
import asyncio
import json
import uuid
from datetime import datetime

from sqlalchemy import Column, DateTime, Integer, JSON, String, select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker
from structlog import get_logger

from app.database import Base

logger = get_logger(__name__)

MAX_RETRIES = 3


class KycOutboxEntity(Base):
    __tablename__ = "kyc_outbox"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    aggregate_id = Column(String, nullable=False, index=True)
    event_type = Column(String, nullable=False, index=True)
    destination_topic = Column(String, nullable=False)
    payload = Column(JSON, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    published_at = Column(DateTime, nullable=True, index=True)
    retry_count = Column(Integer, default=0, nullable=False)


class KycOutboxService:
    """Adds an outbox row to the caller's session — commit happens with the mutation."""

    @staticmethod
    def create_event(session: AsyncSession, aggregate_id: str, event_type: str,
                     destination_topic: str, payload: dict) -> KycOutboxEntity:
        entity = KycOutboxEntity(
            aggregate_id=aggregate_id,
            event_type=event_type,
            destination_topic=destination_topic,
            payload=payload,
        )
        session.add(entity)
        return entity


class KycOutboxPublisher:
    """Background publisher: unpublished outbox rows -> Kafka (CloudEvents 1.0.2)."""

    def __init__(self, kafka_producer=None, session_maker: async_sessionmaker | None = None,
                 poll_interval_sec: float = 2.0):
        from app.config import get_settings
        from app.messaging.kafka_producer import KafkaProducerService

        settings = get_settings()
        self.settings = settings
        self.kafka_producer = kafka_producer or KafkaProducerService()
        self.session_maker = session_maker
        self.poll_interval_sec = poll_interval_sec
        self._task = None

    def _envelope(self, entity: KycOutboxEntity) -> dict:
        return {
            "specversion": "1.0.2",
            "id": entity.id,
            "source": "/kyc-service/verifications",
            "type": entity.event_type,
            "subject": entity.aggregate_id,
            "time": datetime.utcnow().isoformat() + "Z",
            "datacontenttype": "application/json",
            "data": entity.payload,
        }

    async def publish_pending(self, batch_size: int = 50) -> int:
        if self.session_maker is None:
            from app.database import async_session_maker
            self.session_maker = async_session_maker
        published = 0
        async with self.session_maker() as session:
            result = await session.execute(
                select(KycOutboxEntity)
                .where(KycOutboxEntity.published_at.is_(None))
                .where(KycOutboxEntity.retry_count < MAX_RETRIES)
                .order_by(KycOutboxEntity.created_at.asc())
                .limit(batch_size)
            )
            for entity in result.scalars().all():
                try:
                    await self.kafka_producer.publish_event(entity.destination_topic, self._envelope(entity))
                    entity.published_at = datetime.utcnow()
                    published += 1
                except Exception as e:  # noqa: BLE001 — Kafka outage must not kill the loop
                    entity.retry_count += 1
                    if entity.retry_count >= MAX_RETRIES:
                        logger.error(
                            "KYC outbox event failed permanently — archived in place for replay",
                            outbox_id=entity.id, topic=entity.destination_topic, error=str(e),
                        )
                    else:
                        logger.warning(
                            "KYC outbox publish failed, will retry",
                            outbox_id=entity.id, topic=entity.destination_topic,
                            retry=entity.retry_count, error=str(e),
                        )
            await session.commit()
        return published

    async def _loop(self):
        while True:
            try:
                await self.publish_pending()
            except Exception as e:  # noqa: BLE001
                logger.error("KYC outbox publisher loop error", error=str(e))
            await asyncio.sleep(self.poll_interval_sec)

    def start(self):
        if self._task is None or self._task.done():
            self._task = asyncio.create_task(self._loop())
            logger.info("KYC outbox publisher started")

    def stop(self):
        if self._task is not None:
            self._task.cancel()
            self._task = None
            logger.info("KYC outbox publisher stopped")
