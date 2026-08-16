import pytest
import sys

sys.path.insert(0, "/home/ubuntu/payu/backend/analytics-service/src")  # noqa: E402
import asyncio
from unittest.mock import AsyncMock, Mock, patch, MagicMock

from app.messaging.kafka_consumer import KafkaConsumerService, _unpack_event
from app.models.schemas import DashboardEventType


@pytest.fixture
def kafka_consumer():
    """Create a Kafka consumer service instance"""
    return KafkaConsumerService()


@pytest.fixture
def mock_session():
    """Mock database session with proper async context manager support"""
    session = AsyncMock()
    session.commit = AsyncMock()
    session.add = Mock()
    session.execute = AsyncMock()

    # Setup scalars() chain for query results
    mock_scalars = MagicMock()
    mock_scalars.all = Mock(return_value=[])
    mock_scalars.first = Mock(return_value=None)
    session.scalars = MagicMock(return_value=mock_scalars)

    # Setup scalar_one_or_none for single result queries
    mock_result = AsyncMock()
    mock_result.scalar_one_or_none = AsyncMock(return_value=None)
    session.execute = AsyncMock(return_value=mock_result)

    return session


@pytest.mark.asyncio
async def test_kafka_consumer_start(kafka_consumer):
    """Test starting the Kafka consumer"""
    with patch("app.messaging.kafka_consumer.AIOKafkaConsumer") as mock_consumer_class:
        mock_consumer = AsyncMock()
        mock_consumer_class.return_value = mock_consumer

        await kafka_consumer.start()

        assert kafka_consumer._running is True
        assert kafka_consumer._task is not None


@pytest.mark.asyncio
async def test_kafka_consumer_stop(kafka_consumer):
    """Test stopping the Kafka consumer"""
    kafka_consumer._running = True
    kafka_consumer._task = asyncio.create_task(asyncio.sleep(1))
    kafka_consumer.consumer = AsyncMock()
    kafka_consumer.consumer.stop = AsyncMock()

    await kafka_consumer.stop()

    assert kafka_consumer._running is False


@pytest.mark.asyncio
async def test_process_transaction_completed_message(kafka_consumer, mock_session):
    """Test processing a transaction completed Kafka message"""
    message = {
        "user_id": "test_user_123",
        "transaction_id": "txn_456",
        "amount": 5000.0,
        "currency": "IDR",
        "type": "TRANSFER",
        "category": "FOOD",
        "recipient_id": "recipient_789",
        "merchant_id": "merchant_101",
    }

    # Setup mock for _update_user_metrics
    with patch.object(
        kafka_consumer, "_update_user_metrics", new_callable=AsyncMock
    ) as mock_update_metrics:
        # Mock the manager module's broadcast_to_user
        with patch("app.messaging.kafka_consumer.manager") as mock_manager:
            mock_manager.broadcast_to_user = AsyncMock()

            await kafka_consumer._handle_transaction_completed(mock_session, message)

            mock_update_metrics.assert_called_once_with(
                mock_session, "test_user_123", 5000.0
            )
            mock_session.add.assert_called_once()
            mock_manager.broadcast_to_user.assert_called_once()

            call_args = mock_manager.broadcast_to_user.call_args
            broadcast_msg = call_args[0][0]
            assert broadcast_msg["event_type"] == DashboardEventType.TRANSACTION_COMPLETED


def test_unpack_cloud_event_preserves_identity_and_payload():
    payload, source, event_id, event_type = _unpack_event(
        "payu.transaction.completed.v1",
        {
            "specversion": "1.0.2",
            "id": "evt-123",
            "source": "transaction-service",
            "type": "id.payu.transaction.completed",
            "data": {"transactionId": "txn-456"},
        },
    )

    assert payload == {"transactionId": "txn-456"}
    assert source == "transaction-service"
    assert event_id == "evt-123"
    assert event_type == "id.payu.transaction.completed"


@pytest.mark.asyncio
async def test_replayed_cloud_event_is_processed_once(kafka_consumer, mock_session):
    event = {
        "specversion": "1.0.2",
        "id": "evt-replay-1",
        "source": "transaction-service",
        "type": "id.payu.transaction.completed",
        "data": {"transactionId": "txn-456"},
    }
    first_claim = MagicMock(rowcount=1)
    duplicate_claim = MagicMock(rowcount=0)
    mock_session.execute.side_effect = [first_claim, duplicate_claim]
    session_context = MagicMock()
    session_context.__aenter__ = AsyncMock(return_value=mock_session)
    session_context.__aexit__ = AsyncMock(return_value=False)

    with patch("app.messaging.kafka_consumer.async_session_maker", return_value=session_context):
        with patch.object(
            kafka_consumer, "_handle_transaction_completed", new_callable=AsyncMock
        ) as handler:
            await kafka_consumer._process_message("payu.transaction.completed.v1", event)
            await kafka_consumer._process_message("payu.transaction.completed.v1", event)

    handler.assert_awaited_once_with(
        mock_session,
        {"transactionId": "txn-456"},
        "evt-replay-1",
    )


@pytest.mark.asyncio
async def test_get_user_history_derives_account_created_at(kafka_consumer, mock_session):
    """ANA-HISTORY-001: account age derives from earliest transaction, not a hardcoded date."""
    from datetime import datetime
    from app.database import UserMetricsEntity, TransactionAnalyticsEntity

    metrics = UserMetricsEntity(
        user_id="u-1",
        total_transactions=5,
        total_amount=1000,
        average_transaction=200,
    )
    txn_entity = TransactionAnalyticsEntity(
        event_id="e-1",
        user_id="u-1",
        transaction_id="t-1",
        amount=200,
        transaction_type="TRANSFER",
        status="COMPLETED",
        timestamp=datetime(2026, 1, 1, 0, 0, 0),
    )

    metrics_result = AsyncMock()
    metrics_result.scalar_one_or_none = Mock(return_value=metrics)
    txn_result = MagicMock()
    scalars = MagicMock()
    scalars.all = Mock(return_value=[txn_entity])
    txn_result.scalars = Mock(return_value=scalars)

    mock_session.execute.side_effect = [metrics_result, txn_result]
    mock_session.scalar = AsyncMock(return_value=datetime(2026, 1, 1, 0, 0, 0))

    history = await kafka_consumer._get_user_history(mock_session, "u-1")

    assert history["account_created_at"] == "2026-01-01T00:00:00"
    assert history["recent_transactions"][0]["transaction_id"] == "t-1"


@pytest.mark.asyncio
async def test_process_wallet_balance_changed_message(kafka_consumer, mock_session):
    """Test processing a wallet balance changed Kafka message"""
    message = {
        "user_id": "test_user_123",
        "wallet_id": "wallet_456",
        "balance": 100000.0,
        "currency": "IDR",
        "change_amount": 5000.0,
        "change_type": "CREDIT",
    }

    # Mock the manager module's broadcast_to_user
    with patch("app.messaging.kafka_consumer.manager") as mock_manager:
        mock_manager.broadcast_to_user = AsyncMock()

        await kafka_consumer._handle_wallet_balance_changed(mock_session, message)

        mock_session.add.assert_called_once()
        mock_manager.broadcast_to_user.assert_called_once()

        call_args = mock_manager.broadcast_to_user.call_args
        broadcast_msg = call_args[0][0]
        assert broadcast_msg["event_type"] == DashboardEventType.WALLET_BALANCE_CHANGED


@pytest.mark.asyncio
async def test_process_kyc_verified_message(kafka_consumer, mock_session):
    """Test processing a KYC verified Kafka message"""
    message = {"user_id": "test_user_123"}

    with patch("app.messaging.kafka_consumer.select"):  # noqa: F841
        # Create a mock result where scalar_one_or_none is a Mock (not AsyncMock)
        # since it's not an async method
        mock_result = MagicMock()
        mock_result.scalar_one_or_none.return_value = None
        mock_session.execute = AsyncMock(return_value=mock_result)

        # Mock the manager module's broadcast_to_user
        with patch("app.messaging.kafka_consumer.manager") as mock_manager:
            mock_manager.broadcast_to_user = AsyncMock()

            await kafka_consumer._handle_kyc_verified(mock_session, message)

            mock_session.add.assert_called_once()
            mock_manager.broadcast_to_user.assert_called_once()

            call_args = mock_manager.broadcast_to_user.call_args
            broadcast_msg = call_args[0][0]
            assert broadcast_msg["event_type"] == DashboardEventType.KYC_VERIFIED


@pytest.mark.asyncio
async def test_update_user_metrics_existing_user(kafka_consumer, mock_session):
    """Test updating metrics for an existing user"""

    mock_metrics = Mock()
    mock_metrics.total_transactions = 10
    mock_metrics.total_amount = 100000.0

    # Create a mock result where scalar_one_or_none is a Mock (not AsyncMock)
    # since it's not an async method
    mock_result = MagicMock()
    mock_result.scalar_one_or_none.return_value = mock_metrics
    mock_session.execute = AsyncMock(return_value=mock_result)

    # Mock the manager module's broadcast_to_user
    with patch("app.messaging.kafka_consumer.manager") as mock_manager:
        mock_manager.broadcast_to_user = AsyncMock()

        await kafka_consumer._update_user_metrics(mock_session, "test_user_123", 5000.0)

        assert mock_metrics.total_transactions == 11
        assert mock_metrics.total_amount == 105000.0
        mock_manager.broadcast_to_user.assert_called_once()


@pytest.mark.asyncio
async def test_update_user_metrics_new_user(kafka_consumer, mock_session):
    """Test creating metrics for a new user"""
    # Create a mock result where scalar_one_or_none is a Mock (not AsyncMock)
    # since it's not an async method
    mock_result = MagicMock()
    mock_result.scalar_one_or_none.return_value = None
    mock_session.execute = AsyncMock(return_value=mock_result)

    # Mock the manager module's broadcast_to_user
    with patch("app.messaging.kafka_consumer.manager") as mock_manager:
        mock_manager.broadcast_to_user = AsyncMock()

        await kafka_consumer._update_user_metrics(mock_session, "test_user_123", 5000.0)

        mock_session.add.assert_called_once()


@pytest.mark.asyncio
async def test_process_transaction_initiated_message(kafka_consumer, mock_session):
    """Test processing a transaction initiated Kafka message"""
    message = {
        "user_id": "test_user_123",
        "transaction_id": "txn_456",
        "amount": 5000.0,
        "currency": "IDR",
        "type": "PAYMENT",
        "category": "BILLS",
    }

    await kafka_consumer._handle_transaction_initiated(mock_session, message)

    mock_session.add.assert_called_once()

    call_args = mock_session.add.call_args
    entity = call_args[0][0]
    assert entity.status == "PENDING"
