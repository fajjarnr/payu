import pytest
import sys
from datetime import datetime

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")  # noqa: E402

from unittest.mock import AsyncMock
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool
from app.database import Base
from app.messaging.kyc_outbox import KycOutboxEntity, KycOutboxService, KycOutboxPublisher


@pytest.fixture
async def outbox_db():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:", poolclass=StaticPool)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    maker = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    yield maker
    await engine.dispose()


@pytest.mark.unit
class TestKycOutboxService:
    """ARCH-KYC-002: outbox rows created in the same DB transaction as the mutation"""

    async def test_create_event_adds_row_to_session(self, outbox_db):
        async with outbox_db() as session:
            KycOutboxService.create_event(
                session,
                aggregate_id="verify_123",
                event_type="kyc.verified",
                destination_topic="payu.kyc.verified.v1",
                payload={"user_id": "u1", "status": "VERIFIED"},
            )
            await session.commit()

        async with outbox_db() as session:
            result = await session.execute(select(KycOutboxEntity))
            rows = result.scalars().all()
            assert len(rows) == 1
            row = rows[0]
            assert row.event_type == "kyc.verified"
            assert row.destination_topic == "payu.kyc.verified.v1"
            assert row.payload["user_id"] == "u1"
            assert row.published_at is None
            assert row.retry_count == 0


@pytest.mark.unit
class TestKycOutboxPublisher:
    """ARCH-KYC-002: CloudEvents 1.0.2 envelope, mark-after-publish, retry ceiling"""

    async def test_publishes_cloud_event_and_marks_published(self, outbox_db):
        async with outbox_db() as session:
            KycOutboxService.create_event(
                session, "verify_1", "kyc.verified", "payu.kyc.verified.v1", {"user_id": "u1"}
            )
            await session.commit()

        mock_kafka = AsyncMock()
        publisher = KycOutboxPublisher(kafka_producer=mock_kafka, session_maker=outbox_db)

        await publisher.publish_pending(batch_size=10)

        mock_kafka.publish_event.assert_awaited_once()
        topic, envelope = mock_kafka.publish_event.await_args.args
        assert topic == "payu.kyc.verified.v1"
        assert envelope["specversion"] == "1.0.2"
        assert envelope["type"] == "kyc.verified"
        assert envelope["data"]["user_id"] == "u1"
        assert envelope["id"]

        async with outbox_db() as session:
            result = await session.execute(select(KycOutboxEntity))
            row = result.scalars().one()
            assert row.published_at is not None

    async def test_retries_then_archives_after_max_retries(self, outbox_db):
        async with outbox_db() as session:
            KycOutboxService.create_event(
                session, "verify_2", "kyc.failed", "payu.kyc.failed.v1", {"reason": "x"}
            )
            await session.commit()

        async def boom(topic, event):
            raise RuntimeError("kafka down")

        mock_kafka = AsyncMock()
        mock_kafka.publish_event.side_effect = boom
        publisher = KycOutboxPublisher(kafka_producer=mock_kafka, session_maker=outbox_db)

        for _ in range(4):
            await publisher.publish_pending(batch_size=10)

        async with outbox_db() as session:
            result = await session.execute(select(KycOutboxEntity))
            row = result.scalars().one()
            assert row.published_at is None
            assert row.retry_count == 3  # max retries reached, archived in place

    async def test_idempotent_mark_only_when_published(self, outbox_db):
        async with outbox_db() as session:
            KycOutboxService.create_event(
                session, "verify_3", "kyc.ktp-uploaded", "payu.kyc.ktp-uploaded.v1", {}
            )
            await session.commit()

        mock_kafka = AsyncMock()
        publisher = KycOutboxPublisher(kafka_producer=mock_kafka, session_maker=outbox_db)

        await publisher.publish_pending(batch_size=10)
        await publisher.publish_pending(batch_size=10)

        assert mock_kafka.publish_event.await_count == 1
