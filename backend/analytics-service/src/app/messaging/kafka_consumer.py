import asyncio
import json
from aiokafka import AIOKafkaConsumer
from aiokafka.errors import KafkaError
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert
from uuid import uuid4
from datetime import datetime, timedelta
from decimal import Decimal, ROUND_HALF_EVEN
from structlog import get_logger
from typing import Dict, Any

from app.config import get_settings
from app.database import (
    async_session_maker,
    TransactionAnalyticsEntity,
    WalletBalanceEntity,
    UserActivityEntity,
    UserMetricsEntity,
    FraudScoreEntity,
    ProcessedAnalyticsEventEntity,
)
from app.websocket.connection_manager import manager
from app.models.schemas import (
    DashboardEventType,
    DashboardEvent,
    TransactionCompletedEvent,
    WalletBalanceChangedEvent,
    KycVerifiedEvent,
    UserMetricsUpdatedEvent,
    FraudDetectionResult
)
from app.ml.fraud_detection import FraudDetectionEngine

logger = get_logger(__name__)
settings = get_settings()
MONEY_QUANTUM = Decimal("0.0001")


def _to_money(value: Any) -> Decimal:
    return Decimal(str(value or 0)).quantize(MONEY_QUANTUM, rounding=ROUND_HALF_EVEN)


def _unpack_event(
    topic: str, message: Dict[str, Any]
) -> tuple[Dict[str, Any], str, str, str]:
    is_cloud_event = (
        isinstance(message.get("data"), dict)
        and (str(message.get("specversion") or "").startswith("1.0") or "source" in message)
    )
    payload = message.get("data") if is_cloud_event else message
    if not isinstance(payload, dict):
        payload = {}

    source = str(message.get("source") or topic)
    event_id = (
        message.get("id")
        or message.get("event_id")
        or message.get("eventId")
        or payload.get("event_id")
        or payload.get("eventId")
        or payload.get("verification_id")
        or payload.get("transactionId")
        or payload.get("transaction_id")
    )
    if not event_id:
        raise ValueError(f"Event identity missing for topic {topic}")

    event_type = str(message.get("type") or topic)
    return dict(payload), source, str(event_id), event_type


async def _claim_event(
    session, source: str, event_id: str, topic: str, event_type: str
) -> bool:
    statement = (
        insert(ProcessedAnalyticsEventEntity)
        .values(
            source=source,
            event_id=event_id,
            topic=topic,
            event_type=event_type,
        )
        .on_conflict_do_nothing(index_elements=["source", "event_id"])
    )
    result = await session.execute(statement)
    return result.rowcount == 1


class KafkaConsumerService:
    def __init__(self):
        self.consumer: AIOKafkaConsumer | None = None
        self._running = False
        self._task: asyncio.Task | None = None
        self.fraud_engine = FraudDetectionEngine()

    async def start(self):
        if self._running:
            logger.warning("Kafka consumer already running")
            return

        self._running = True
        self._task = asyncio.create_task(self._consume_loop())

        logger.info("Kafka consumer service started")

    async def stop(self):
        self._running = False

        if self._task:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass

        if self.consumer:
            await self.consumer.stop()

        logger.info("Kafka consumer service stopped")

    async def _consume_loop(self):
        try:
            self.consumer = AIOKafkaConsumer(
                *settings.kafka_topics,
                bootstrap_servers=settings.kafka_bootstrap_servers,
                group_id=settings.kafka_consumer_group,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                auto_offset_reset='latest'
            )

            await self.consumer.start()
            logger.info("Kafka consumer connected", topics=settings.kafka_topics)

            async for msg in self.consumer:
                try:
                    await self._process_message(msg.topic, msg.value or {})
                except Exception as e:
                    logger.error(
                        "Failed to process message",
                        topic=msg.topic,
                        error=str(e),
                        exc_info=e
                    )

        except KafkaError as e:
            logger.error("Kafka consumer error", exc_info=e)
        except asyncio.CancelledError:
            logger.info("Kafka consumer loop cancelled")
        except Exception as e:
            logger.error("Unexpected error in consumer loop", exc_info=e)

    async def _process_message(self, topic: str, message: Dict[str, Any] | None):
        logger.info("Processing Kafka message", topic=topic)

        if not message:
            logger.warning("Empty message received", topic=topic)
            return

        try:
            payload, source, event_id, event_type = _unpack_event(topic, message)
        except ValueError as exc:
            logger.error("Skipping Kafka message without event identity", error=str(exc))
            return

        async with async_session_maker() as session:
            if not await _claim_event(session, source, event_id, topic, event_type):
                logger.info(
                    "Skipping duplicate analytics event",
                    source=source,
                    event_id=event_id,
                    topic=topic,
                )
                return

            if topic == "payu.transaction.completed.v1":
                await self._handle_transaction_completed(session, payload, event_id)
            elif topic == "payu.transaction.initiated.v1":
                await self._handle_transaction_initiated(session, payload, event_id)
                await self._handle_fraud_detection(session, payload)
            elif topic == "payu.wallet.balance-changed.v1":
                await self._handle_wallet_balance_changed(session, payload, event_id)
            elif topic == "payu.kyc.verified.v1":
                await self._handle_kyc_verified(session, payload)

            await session.commit()

    async def _handle_transaction_completed(self, session, message, event_id=None):
        event_id = event_id or message.get('event_id') or message.get('eventId') or message.get('transaction_id') or message.get('transactionId')
        if not event_id:
            raise ValueError("Transaction completed event identity missing")
        user_id = message.get('user_id') or message.get('senderAccountId') or message.get('accountId')
        amount = _to_money(message.get('amount', 0))
        transaction_type = message.get('type', 'TRANSFER')
        transaction_id = message.get('transaction_id') or message.get('transactionId')
        timestamp = datetime.utcnow()

        entity = TransactionAnalyticsEntity(
            event_id=event_id,
            user_id=user_id,
            transaction_id=transaction_id,
            amount=amount,
            currency=message.get('currency', 'IDR'),
            transaction_type=transaction_type,
            category=message.get('category', 'OTHER'),
            status='COMPLETED',
            recipient_id=message.get('recipient_id'),
            merchant_id=message.get('merchant_id'),
            metadata=message.get('metadata'),
            timestamp=timestamp
        )

        session.add(entity)

        await self._update_user_metrics(session, user_id, amount)

        dashboard_event = DashboardEvent(
            event_type=DashboardEventType.TRANSACTION_COMPLETED,
            user_id=user_id,
            timestamp=timestamp,
            data={
                "transaction": TransactionCompletedEvent(
                    transaction_id=transaction_id,
                    amount=amount,
                    currency=message.get('currency', 'IDR'),
                    transaction_type=transaction_type,
                    category=message.get('category', 'OTHER'),
                    recipient_id=message.get('recipient_id'),
                    merchant_id=message.get('merchant_id')
                ).model_dump(mode="json")
            }
        )

        await manager.broadcast_to_user(dashboard_event.model_dump(mode="json"), user_id, DashboardEventType.TRANSACTION_COMPLETED.value)

        logger.info(
            "Transaction analytics recorded",
            transaction_id=transaction_id,
            amount=amount
        )

    async def _handle_transaction_initiated(self, session, message, event_id=None):
        event_id = event_id or message.get('event_id') or message.get('eventId') or message.get('transaction_id') or message.get('transactionId')
        if not event_id:
            raise ValueError("Transaction initiated event identity missing")
        user_id = message.get('user_id') or message.get('senderAccountId') or message.get('accountId')
        amount = _to_money(message.get('amount', 0))

        entity = TransactionAnalyticsEntity(
            event_id=event_id,
            user_id=user_id,
            transaction_id=message.get('transaction_id') or message.get('transactionId'),
            amount=amount,
            currency=message.get('currency', 'IDR'),
            transaction_type=message.get('type', 'TRANSFER'),
            category=message.get('category', 'OTHER'),
            status='PENDING',
            timestamp=datetime.utcnow()
        )

        session.add(entity)

    async def _handle_wallet_balance_changed(self, session, message, event_id=None):
        event_id = event_id or message.get('event_id') or message.get('eventId') or message.get('wallet_id') or message.get('walletId')
        if not event_id:
            raise ValueError("Wallet balance event identity missing")
        user_id = message.get('user_id') or message.get('accountId')
        wallet_id = message.get('wallet_id') or message.get('walletId') or user_id
        balance = _to_money(message.get('balance', message.get('newBalance', 0)))
        change_amount = _to_money(message.get('change_amount', 0))
        change_type = message.get('change_type', 'CREDIT')
        timestamp = datetime.utcnow()

        entity = WalletBalanceEntity(
            event_id=event_id,
            user_id=user_id,
            wallet_id=wallet_id,
            balance=balance,
            currency=message.get('currency', 'IDR'),
            change_amount=change_amount,
            change_type=change_type,
            event_metadata=message.get("metadata"),
            timestamp=timestamp
        )

        session.add(entity)

        dashboard_event = DashboardEvent(
            event_type=DashboardEventType.WALLET_BALANCE_CHANGED,
            user_id=user_id,
            timestamp=timestamp,
            data={
                "wallet_balance": WalletBalanceChangedEvent(
                    wallet_id=wallet_id,
                    balance=balance,
                    currency=message.get('currency', 'IDR'),
                    change_amount=change_amount,
                    change_type=change_type
                ).model_dump(mode="json")
            }
        )

        await manager.broadcast_to_user(dashboard_event.model_dump(mode="json"), user_id, DashboardEventType.WALLET_BALANCE_CHANGED.value)

    async def _handle_kyc_verified(self, session, message):
        user_id = message.get('user_id')
        timestamp = datetime.utcnow()

        metrics = await session.execute(
            select(UserMetricsEntity).where(UserMetricsEntity.user_id == user_id)
        )
        user_metrics = metrics.scalar_one_or_none()

        if user_metrics:
            user_metrics.kyc_status = 'VERIFIED'
            user_metrics.updated_at = timestamp
        else:
            new_metrics = UserMetricsEntity(
                user_id=user_id,
                total_transactions=0,
                total_amount=Decimal("0.0000"),
                average_transaction=Decimal("0.0000"),
                kyc_status='VERIFIED',
                account_age_days=0
            )
            session.add(new_metrics)

        dashboard_event = DashboardEvent(
            event_type=DashboardEventType.KYC_VERIFIED,
            user_id=user_id,
            timestamp=timestamp,
            data={
                "kyc": KycVerifiedEvent(
                    user_id=user_id,
                    kyc_status='VERIFIED'
                ).model_dump(mode="json")
            }
        )

        await manager.broadcast_to_user(dashboard_event.model_dump(mode="json"), user_id)

    async def _update_user_metrics(self, session, user_id: str, amount: Decimal):
        amount = _to_money(amount)
        metrics = await session.execute(
            select(UserMetricsEntity).where(UserMetricsEntity.user_id == user_id)
        )
        user_metrics = metrics.scalar_one_or_none()
        timestamp = datetime.utcnow()

        if user_metrics:
            user_metrics.total_transactions += 1
            user_metrics.total_amount = _to_money(user_metrics.total_amount) + amount
            user_metrics.average_transaction = _to_money(
                user_metrics.total_amount / user_metrics.total_transactions
            )
            user_metrics.last_transaction_date = timestamp

            dashboard_event = DashboardEvent(
                event_type=DashboardEventType.USER_METRICS_UPDATED,
                user_id=user_id,
                timestamp=timestamp,
                data={
                    "metrics": UserMetricsUpdatedEvent(
                        total_transactions=user_metrics.total_transactions,
                        total_amount=user_metrics.total_amount,
                        average_transaction=user_metrics.average_transaction,
                        last_transaction_date=timestamp
                    ).model_dump(mode="json")
                }
            )

            await manager.broadcast_to_user(dashboard_event.model_dump(mode="json"), user_id, DashboardEventType.USER_METRICS_UPDATED.value)
        else:
            new_metrics = UserMetricsEntity(
                user_id=user_id,
                total_transactions=1,
                total_amount=amount,
                average_transaction=amount,
                last_transaction_date=timestamp,
                account_age_days=0,
                kyc_status=None
            )
            session.add(new_metrics)

    async def _handle_fraud_detection(self, session, message):
        transaction_id = message.get('transactionId')
        user_id = message.get('senderAccountId')

        try:
            user_history = await self._get_user_history(session, user_id)

            fraud_result = await self.fraud_engine.calculate_fraud_score(
                transaction_data=message,
                user_history=user_history
            )

            fraud_entity = FraudScoreEntity(
                score_id=str(uuid4()),
                transaction_id=transaction_id,
                user_id=user_id,
                risk_score=fraud_result.fraud_score.risk_score,
                risk_level=fraud_result.fraud_score.risk_level.value,
                risk_factors=fraud_result.fraud_score.risk_factors,
                is_suspicious=fraud_result.fraud_score.is_suspicious,
                recommended_action=fraud_result.fraud_score.recommended_action,
                is_blocked=fraud_result.is_blocked,
                requires_review=fraud_result.requires_review,
                rule_triggers=fraud_result.rule_triggers,
                scored_at=datetime.utcnow()
            )

            session.add(fraud_entity)

            if fraud_result.is_blocked:
                logger.warning(
                    "Transaction blocked due to high fraud risk",
                    transaction_id=transaction_id,
                    user_id=user_id,
                    risk_score=fraud_result.fraud_score.risk_score
                )

            if fraud_result.requires_review:
                logger.info(
                    "Transaction flagged for manual review",
                    transaction_id=transaction_id,
                    user_id=user_id,
                    risk_score=fraud_result.fraud_score.risk_score
                )

        except Exception as e:
            logger.error(
                "Failed to calculate fraud score",
                transaction_id=transaction_id,
                error=str(e),
                exc_info=e
            )

    async def _get_user_history(self, session, user_id: str) -> Dict[str, Any]:
        from sqlalchemy import select, func

        try:
            metrics = await session.execute(
                select(UserMetricsEntity).where(UserMetricsEntity.user_id == user_id)
            )
            user_metrics = metrics.scalar_one_or_none()

            recent_txns = await session.execute(
                select(TransactionAnalyticsEntity)
                .where(TransactionAnalyticsEntity.user_id == user_id)
                .where(TransactionAnalyticsEntity.timestamp > datetime.utcnow() - timedelta(hours=24))
                .order_by(TransactionAnalyticsEntity.timestamp.desc())
                .limit(50)
            )
            recent_transactions = recent_txns.scalars().all()

            # ANA-HISTORY-001: derive account age from the user's earliest known
            # transaction instead of a hardcoded date that always reads "old".
            first_txn_at = await session.scalar(
                select(func.min(TransactionAnalyticsEntity.timestamp)).where(
                    TransactionAnalyticsEntity.user_id == user_id
                )
            )

            user_history = {
                "total_transactions": user_metrics.total_transactions if user_metrics else 0,
                "total_amount": user_metrics.total_amount if user_metrics else Decimal("0.0000"),
                "average_transaction": user_metrics.average_transaction if user_metrics else Decimal("0.0000"),
                "account_created_at": first_txn_at.isoformat() if first_txn_at else None,
                "recent_transactions": [
                    {
                        "transaction_id": txn.transaction_id,
                        "amount": txn.amount,
                        "type": txn.transaction_type,
                        "timestamp": txn.timestamp.isoformat(),
                        "recipient_id": txn.recipient_id
                    }
                    for txn in recent_transactions
                ]
            }

            return user_history

        except Exception as e:
            logger.error("Failed to fetch user history", user_id=user_id, error=str(e))
            return {}
