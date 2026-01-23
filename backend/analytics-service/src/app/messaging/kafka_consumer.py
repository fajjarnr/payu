import asyncio
import json
from aiokafka import AIOKafkaConsumer
from aiokafka.errors import KafkaError
from sqlalchemy import select
from uuid import uuid4
from datetime import datetime, timedelta
from structlog import get_logger
from typing import Dict, Any

from app.config import get_settings
from app.database import (
    async_session_maker,
    TransactionAnalyticsEntity,
    WalletBalanceEntity,
    UserActivityEntity,
    UserMetricsEntity,
    FraudScoreEntity
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

        async with async_session_maker() as session:
            if topic == "payu.transactions.completed":
                await self._handle_transaction_completed(session, message)
            elif topic == "payu.transactions.initiated":
                await self._handle_transaction_initiated(session, message)
                await self._handle_fraud_detection(session, message)
            elif topic == "payu.wallet.balance.changed":
                await self._handle_wallet_balance_changed(session, message)
            elif topic == "payu.kyc.verified":
                await self._handle_kyc_verified(session, message)

            await session.commit()

    async def _handle_transaction_completed(self, session, message):
        event_id = str(uuid4())
        user_id = message.get('user_id')
        amount = float(message.get('amount', 0))
        transaction_type = message.get('type', 'TRANSFER')
        transaction_id = message.get('transaction_id')
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
                ).model_dump()
            }
        )

        await manager.broadcast_to_user(dashboard_event.model_dump(), user_id, DashboardEventType.TRANSACTION_COMPLETED.value)

        logger.info(
            "Transaction analytics recorded",
            transaction_id=transaction_id,
            amount=amount
        )

    async def _handle_transaction_initiated(self, session, message):
        event_id = str(uuid4())
        user_id = message.get('user_id')
        amount = float(message.get('amount', 0))

        entity = TransactionAnalyticsEntity(
            event_id=event_id,
            user_id=user_id,
            transaction_id=message.get('transaction_id'),
            amount=amount,
            currency=message.get('currency', 'IDR'),
            transaction_type=message.get('type', 'TRANSFER'),
            category=message.get('category', 'OTHER'),
            status='PENDING',
            timestamp=datetime.utcnow()
        )

        session.add(entity)

    async def _handle_wallet_balance_changed(self, session, message):
        event_id = str(uuid4())
        user_id = message.get('user_id')
        wallet_id = message.get('wallet_id')
        balance = float(message.get('balance', 0))
        change_amount = float(message.get('change_amount', 0))
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
                ).model_dump()
            }
        )

        await manager.broadcast_to_user(dashboard_event.model_dump(), user_id, DashboardEventType.WALLET_BALANCE_CHANGED.value)

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
                total_amount=0.0,
                average_transaction=0.0,
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
                ).model_dump()
            }
        )

        await manager.broadcast_to_user(dashboard_event.model_dump(), user_id)

    async def _update_user_metrics(self, session, user_id: str, amount: float):
        metrics = await session.execute(
            select(UserMetricsEntity).where(UserMetricsEntity.user_id == user_id)
        )
        user_metrics = metrics.scalar_one_or_none()
        timestamp = datetime.utcnow()

        if user_metrics:
            user_metrics.total_transactions += 1
            user_metrics.total_amount += amount
            user_metrics.average_transaction = user_metrics.total_amount / user_metrics.total_transactions
            user_metrics.last_transaction_date = timestamp

            dashboard_event = DashboardEvent(
                event_type=DashboardEventType.USER_METRICS_UPDATED,
                user_id=user_id,
                timestamp=timestamp,
                data={
                    "metrics": UserMetricsUpdatedEvent(
                        total_transactions=user_metrics.total_transactions,
                        total_amount=float(user_metrics.total_amount),
                        average_transaction=float(user_metrics.average_transaction),
                        last_transaction_date=timestamp
                    ).model_dump()
                }
            )

            await manager.broadcast_to_user(dashboard_event.model_dump(), user_id, DashboardEventType.USER_METRICS_UPDATED.value)
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

            user_history = {
                "total_transactions": user_metrics.total_transactions if user_metrics else 0,
                "total_amount": float(user_metrics.total_amount) if user_metrics else 0.0,
                "average_transaction": float(user_metrics.average_transaction) if user_metrics else 0.0,
                "account_created_at": "2025-01-01T00:00:00",
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
