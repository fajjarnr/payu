import pytest
import sys

sys.path.insert(0, "/home/ubuntu/payu/backend/analytics-service/src")  # noqa: E402
from unittest.mock import AsyncMock  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402
from fastapi import WebSocket  # noqa: E402

from app.main import app  # noqa: E402
from app.websocket.connection_manager import manager  # noqa: E402


@pytest.fixture
def reset_manager():
    """Reset the global manager for each test"""
    manager.active_connections.clear()
    yield manager
    manager.active_connections.clear()


@pytest.fixture
def client():
    """Create a test client"""
    with TestClient(app) as c:
        yield c


def test_websocket_connect_and_ping(reset_manager, client):
    """Test WebSocket connection and ping/pong"""
    user_id = "test_user_123"

    with client.websocket_connect(f"/ws/dashboard/{user_id}") as websocket:
        websocket.send_json({"type": "ping"})
        data = websocket.receive_json()
        assert data["type"] == "pong"


def test_websocket_multiple_clients_same_user(reset_manager, client):
    """Test multiple WebSocket connections for the same user"""
    user_id = "test_user_123"

    connections = []
    for i in range(3):
        ws = client.websocket_connect(f"/ws/dashboard/{user_id}")
        ws.__enter__()
        connections.append(ws)

    assert manager.get_user_connection_count(user_id) == 3

    for ws in connections:
        ws.__exit__(None, None, None)

    assert manager.get_user_connection_count(user_id) == 0


def test_websocket_disconnect(reset_manager, client):
    """Test WebSocket disconnect behavior"""
    user_id = "test_user_123"

    with client.websocket_connect(f"/ws/dashboard/{user_id}") as websocket:
        assert manager.get_user_connection_count(user_id) == 1
        websocket.send_json({"type": "ping"})
        data = websocket.receive_json()
        assert data["type"] == "pong"

    assert manager.get_user_connection_count(user_id) == 0


def test_websocket_invalid_user_id(client):
    """Test WebSocket with invalid user_id (should still connect, user_id is just a string)"""
    with client.websocket_connect("/ws/dashboard/") as _:
        pass


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
    assert response.json()["status"] == "healthy"


def test_ready_endpoint(client):
    """Test readiness check endpoint"""
    response = client.get("/ready")
    assert response.status_code == 200
    assert response.json()["status"] == "ready"
