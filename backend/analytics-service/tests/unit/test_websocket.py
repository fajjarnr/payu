import pytest
import sys

sys.path.insert(0, "/home/ubuntu/payu/backend/analytics-service/src")  # noqa: E402
from unittest.mock import AsyncMock, Mock
from datetime import datetime

# type: ignore  # Duplicate module name from e2e/tests

from app.websocket.connection_manager import manager
from app.models.schemas import (
    DashboardEventType,
    DashboardEvent,
    TransactionCompletedEvent,
    WalletBalanceChangedEvent,
    KycVerifiedEvent,
)


@pytest.fixture
def reset_manager():
    """Reset the global manager for each test"""
    manager.active_connections.clear()
    manager.user_subscriptions.clear()
    yield manager
    manager.active_connections.clear()
    manager.user_subscriptions.clear()


@pytest.mark.asyncio
async def test_connect_websocket(reset_manager):
    """Test connecting a WebSocket client"""
    mock_websocket = AsyncMock(spec=Mock)
    mock_websocket.accept = AsyncMock()

    user_id = "test_user_123"
    await manager.connect(mock_websocket, user_id, {"TRANSACTION_COMPLETED"})

    assert mock_websocket.accept.called
    assert user_id in manager.active_connections
    assert mock_websocket in manager.active_connections[user_id]
    assert manager.get_user_connection_count(user_id) == 1
    assert (
        "TRANSACTION_COMPLETED" in manager.user_subscriptions[user_id][mock_websocket]
    )


@pytest.mark.asyncio
async def test_disconnect_websocket(reset_manager):
    """Test disconnecting a WebSocket client"""
    mock_websocket = AsyncMock(spec=Mock)
    mock_websocket.accept = AsyncMock()
    mock_websocket.send_json = AsyncMock()

    user_id = "test_user_123"
    await manager.connect(mock_websocket, user_id, {"TRANSACTION_COMPLETED"})
    assert manager.get_user_connection_count(user_id) == 1

    manager.disconnect(mock_websocket, user_id)

    assert user_id not in manager.active_connections
    assert user_id not in manager.user_subscriptions
    assert manager.get_user_connection_count(user_id) == 0


@pytest.mark.asyncio
async def test_multiple_connections_same_user(reset_manager):
    """Test multiple WebSocket connections for the same user"""
    mock_ws1 = AsyncMock(spec=Mock)
    mock_ws1.accept = AsyncMock()
    mock_ws2 = AsyncMock(spec=Mock)
    mock_ws2.accept = AsyncMock()

    user_id = "test_user_123"
    await manager.connect(mock_ws1, user_id, {"TRANSACTION_COMPLETED"})
    await manager.connect(mock_ws2, user_id, {"WALLET_BALANCE_CHANGED"})

    assert manager.get_user_connection_count(user_id) == 2
    assert mock_ws1 in manager.active_connections[user_id]
    assert mock_ws2 in manager.active_connections[user_id]
    assert "TRANSACTION_COMPLETED" in manager.user_subscriptions[user_id][mock_ws1]
    assert "WALLET_BALANCE_CHANGED" in manager.user_subscriptions[user_id][mock_ws2]


@pytest.mark.asyncio
async def test_send_personal_message(reset_manager):
    """Test sending a personal message to a specific WebSocket client"""
    mock_websocket = AsyncMock(spec=Mock)
    mock_websocket.accept = AsyncMock()
    mock_websocket.send_json = AsyncMock()

    user_id = "test_user_123"
    await manager.connect(mock_websocket, user_id, {"TRANSACTION_COMPLETED"})

    message = {"type": "test", "data": "hello"}
    await manager.send_personal_message(message, mock_websocket)

    assert mock_websocket.send_json.called
    sent_message = mock_websocket.send_json.call_args[0][0]
    assert sent_message == message


@pytest.mark.asyncio
async def test_broadcast_to_user(reset_manager):
    """Test broadcasting a message to all WebSocket connections of a specific user"""
    mock_ws1 = AsyncMock(spec=Mock)
    mock_ws1.accept = AsyncMock()
    mock_ws1.send_json = AsyncMock()
    mock_ws2 = AsyncMock(spec=Mock)
    mock_ws2.accept = AsyncMock()
    mock_ws2.send_json = AsyncMock()

    user_id = "test_user_123"
    await manager.connect(mock_ws1, user_id, {"TRANSACTION_COMPLETED"})
    await manager.connect(mock_ws2, user_id, {"TRANSACTION_COMPLETED"})

    message = {"type": "transaction_completed", "data": {"amount": 100}}
    await manager.broadcast_to_user(message, user_id, "TRANSACTION_COMPLETED")

    assert mock_ws1.send_json.called
    assert mock_ws2.send_json.called
    assert mock_ws1.send_json.call_args[0][0] == message
    assert mock_ws2.send_json.call_args[0][0] == message


@pytest.mark.asyncio
async def test_broadcast_to_user_with_event_filtering(reset_manager):
    """Test that events are filtered based on subscriptions"""
    mock_ws1 = AsyncMock(spec=Mock)
    mock_ws1.accept = AsyncMock()
    mock_ws1.send_json = AsyncMock()
    mock_ws2 = AsyncMock(spec=Mock)
    mock_ws2.accept = AsyncMock()
    mock_ws2.send_json = AsyncMock()

    user_id = "test_user_123"
    await manager.connect(mock_ws1, user_id, {"TRANSACTION_COMPLETED"})
    await manager.connect(mock_ws2, user_id, {"WALLET_BALANCE_CHANGED"})

    message = {"type": "transaction_completed", "event_type": "TRANSACTION_COMPLETED"}
    await manager.broadcast_to_user(message, user_id, "TRANSACTION_COMPLETED")

    assert mock_ws1.send_json.called
    assert not mock_ws2.send_json.called


@pytest.mark.asyncio
async def test_update_user_subscriptions(reset_manager):
    """Test updating user subscriptions"""
    mock_websocket = AsyncMock(spec=Mock)
    mock_websocket.accept = AsyncMock()

    user_id = "test_user_123"
    await manager.connect(mock_websocket, user_id, {"TRANSACTION_COMPLETED"})

    new_events = {"WALLET_BALANCE_CHANGED", "KYC_VERIFIED"}
    manager.update_user_subscriptions(user_id, mock_websocket, new_events)

    assert manager.user_subscriptions[user_id][mock_websocket] == new_events


@pytest.mark.asyncio
async def test_broadcast_with_all_subscriptions(reset_manager):
    """Test that all events are sent when subscribed to 'all'"""
    mock_websocket = AsyncMock(spec=Mock)
    mock_websocket.accept = AsyncMock()
    mock_websocket.send_json = AsyncMock()

    user_id = "test_user_123"
    await manager.connect(mock_websocket, user_id, {"all"})

    message = {"type": "test", "event_type": "TRANSACTION_COMPLETED"}
    await manager.broadcast_to_user(message, user_id, "TRANSACTION_COMPLETED")

    assert mock_websocket.send_json.called


@pytest.mark.asyncio
async def test_broadcast_to_all(reset_manager):
    """Test broadcasting a message to all connected WebSocket clients"""
    mock_ws1 = AsyncMock(spec=Mock)
    mock_ws1.accept = AsyncMock()
    mock_ws1.send_json = AsyncMock()
    mock_ws2 = AsyncMock(spec=Mock)
    mock_ws2.accept = AsyncMock()
    mock_ws2.send_json = AsyncMock()

    user_id_1 = "test_user_123"
    user_id_2 = "test_user_456"

    await manager.connect(mock_ws1, user_id_1, {"TRANSACTION_COMPLETED"})
    await manager.connect(mock_ws2, user_id_2, {"WALLET_BALANCE_CHANGED"})

    message = {"type": "system", "data": "maintenance_scheduled"}
    await manager.broadcast_to_all(message)

    assert mock_ws1.send_json.called
    assert mock_ws2.send_json.called


@pytest.mark.asyncio
async def test_get_active_connections_count(reset_manager):
    """Test getting the total number of active connections"""
    mock_ws1 = AsyncMock(spec=Mock)
    mock_ws1.accept = AsyncMock()
    mock_ws2 = AsyncMock(spec=Mock)
    mock_ws2.accept = AsyncMock()
    mock_ws3 = AsyncMock(spec=Mock)
    mock_ws3.accept = AsyncMock()

    await manager.connect(mock_ws1, "user_1", {"TRANSACTION_COMPLETED"})
    await manager.connect(mock_ws2, "user_2", {"WALLET_BALANCE_CHANGED"})
    await manager.connect(mock_ws3, "user_2", {"KYC_VERIFIED"})

    assert manager.get_active_connections_count() == 3


def test_dashboard_event_schema():
    """Test DashboardEvent schema validation"""
    event = DashboardEvent(
        event_type=DashboardEventType.TRANSACTION_COMPLETED,
        user_id="test_user_123",
        timestamp=datetime.utcnow(),
        data={
            "transaction": {
                "transaction_id": "txn_123",
                "amount": 1000.0,
                "currency": "IDR",
                "transaction_type": "TRANSFER",
                "category": "OTHER",
            }
        },
    )

    assert event.event_type == DashboardEventType.TRANSACTION_COMPLETED
    assert event.user_id == "test_user_123"
    assert "transaction" in event.data


def test_transaction_completed_event_schema():
    """Test TransactionCompletedEvent schema validation"""
    tx_event = TransactionCompletedEvent(
        transaction_id="txn_123",
        amount=1000.0,
        currency="IDR",
        transaction_type="TRANSFER",
        category="OTHER",
        recipient_id="user_456",
    )

    assert tx_event.transaction_id == "txn_123"
    assert tx_event.amount == 1000.0
    assert tx_event.currency == "IDR"
    assert tx_event.recipient_id == "user_456"


def test_wallet_balance_changed_event_schema():
    """Test WalletBalanceChangedEvent schema validation"""
    wallet_event = WalletBalanceChangedEvent(
        wallet_id="wallet_123",
        balance=50000.0,
        currency="IDR",
        change_amount=1000.0,
        change_type="CREDIT",
    )

    assert wallet_event.wallet_id == "wallet_123"
    assert wallet_event.balance == 50000.0
    assert wallet_event.change_amount == 1000.0
    assert wallet_event.change_type == "CREDIT"


def test_kyc_verified_event_schema():
    """Test KycVerifiedEvent schema validation"""
    kyc_event = KycVerifiedEvent(user_id="test_user_123", kyc_status="VERIFIED")

    assert kyc_event.user_id == "test_user_123"
    assert kyc_event.kyc_status == "VERIFIED"


@pytest.mark.asyncio
async def test_dashboard_event_broadcast(reset_manager):
    """Test broadcasting a dashboard event to a user"""
    mock_websocket = AsyncMock(spec=Mock)
    mock_websocket.accept = AsyncMock()
    mock_websocket.send_json = AsyncMock()

    user_id = "test_user_123"
    await manager.connect(
        mock_websocket, user_id, {DashboardEventType.TRANSACTION_COMPLETED.value}
    )

    event = DashboardEvent(
        event_type=DashboardEventType.TRANSACTION_COMPLETED,
        user_id=user_id,
        timestamp=datetime.utcnow(),
        data={
            "transaction": TransactionCompletedEvent(
                transaction_id="txn_123",
                amount=1000.0,
                currency="IDR",
                transaction_type="TRANSFER",
                category="OTHER",
            ).model_dump()
        },
    )

    await manager.broadcast_to_user(
        event.model_dump(), user_id, DashboardEventType.TRANSACTION_COMPLETED.value
    )

    assert mock_websocket.send_json.called
    sent_message = mock_websocket.send_json.call_args[0][0]
    assert sent_message["event_type"] == DashboardEventType.TRANSACTION_COMPLETED
    assert sent_message["user_id"] == user_id
    assert "transaction" in sent_message["data"]
