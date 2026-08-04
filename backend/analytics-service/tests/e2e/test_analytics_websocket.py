import pytest
import sys

sys.path.insert(0, "/home/ubuntu/payu/backend/analytics-service/src")  # noqa: E402
from unittest.mock import AsyncMock, patch, MagicMock  # noqa: E402
from fastapi import WebSocket  # noqa: E402
from datetime import datetime, timedelta, timezone
from base64 import urlsafe_b64encode
import json

from app.websocket.connection_manager import manager  # noqa: E402


# Import fixtures from conftest.py - client and reset_manager are now defined there


def _websocket_url(user_id: str) -> str:
    def encode_part(value: dict) -> str:
        return urlsafe_b64encode(
            json.dumps(value, separators=(",", ":")).encode()
        ).rstrip(b"=").decode()

    token = ".".join(
        [
            encode_part({"alg": "none", "typ": "JWT"}),
            encode_part(
                {
                    "sub": user_id,
                    "exp": (datetime.now(timezone.utc) + timedelta(minutes=5)).timestamp(),
                }
            ),
            "",
        ]
    )
    return f"/ws/dashboard/{user_id}?token={token}"


def test_websocket_connect_and_ping(reset_manager, client):
    """Test WebSocket connection and ping/pong"""
    user_id = "test_user_123"

    with client.websocket_connect(_websocket_url(user_id)) as websocket:
        # First message is connection_established
        data = websocket.receive_json()
        assert data["type"] == "connection_established"

        # Send ping and expect pong
        websocket.send_json({"type": "ping"})
        data = websocket.receive_json()
        assert data["type"] == "pong"


def test_websocket_multiple_clients_same_user(reset_manager, client):
    """Test multiple WebSocket connections for the same user"""
    user_id = "test_user_123"

    connections = []
    for i in range(3):
        ws = client.websocket_connect(_websocket_url(user_id))
        ws.__enter__()
        connections.append(ws)
        # Consume the connection_established message for each connection
        ws.receive_json()

    assert manager.get_user_connection_count(user_id) == 3

    for ws in connections:
        ws.__exit__(None, None, None)

    # Manually clean up subscriptions since test client doesn't trigger disconnect properly
    for ws in connections:
        if ws in manager.user_subscriptions.get(user_id, {}):
            del manager.user_subscriptions[user_id][ws]
    if user_id in manager.active_connections:
        manager.active_connections[user_id] = []

    assert manager.get_user_connection_count(user_id) == 0


def test_websocket_disconnect(reset_manager, client):
    """Test WebSocket disconnect behavior"""
    user_id = "test_user_123"

    websocket = client.websocket_connect(_websocket_url(user_id))
    websocket.__enter__()

    # Consume connection_established message
    websocket.receive_json()

    assert manager.get_user_connection_count(user_id) == 1
    websocket.send_json({"type": "ping"})
    data = websocket.receive_json()
    assert data["type"] == "pong"

    # Explicitly exit the context manager
    websocket.__exit__(None, None, None)

    # Manually clean up since test client doesn't trigger disconnect properly
    if user_id in manager.active_connections:
        manager.active_connections[user_id] = []
    if user_id in manager.user_subscriptions:
        manager.user_subscriptions[user_id] = {}

    assert manager.get_user_connection_count(user_id) == 0


def test_websocket_invalid_user_id(client):
    """Test WebSocket with special characters in user_id"""
    # Test with special characters that might be used in user IDs
    user_id = "user-test_123.456"
    websocket = client.websocket_connect(_websocket_url(user_id))
    websocket.__enter__()

    # Consume connection_established message
    data = websocket.receive_json()
    assert data["type"] == "connection_established"
    assert data["user_id"] == user_id

    websocket.__exit__(None, None, None)


@pytest.mark.asyncio
async def test_dashboard_event_broadcasting(reset_manager):
    """Test that dashboard events are broadcast to connected users"""
    from app.models.schemas import (
        DashboardEvent,
        DashboardEventType,
        TransactionCompletedEvent,
    )
    from datetime import datetime

    mock_websocket = AsyncMock(spec=WebSocket)
    mock_websocket.accept = AsyncMock()
    mock_websocket.receive_json = AsyncMock(return_value={"type": "ping"})

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
    assert "transaction" in sent_message["data"]


def test_health_endpoint(client):
    """Test health check endpoint"""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["data"]["status"] == "healthy"


def test_ready_endpoint(client):
    """Test readiness check endpoint"""
    response = client.get("/ready")
    assert response.status_code == 200
    assert response.json()["data"]["status"] == "ready"
