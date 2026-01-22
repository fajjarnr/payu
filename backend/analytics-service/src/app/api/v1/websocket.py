from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query
from structlog import get_logger

from app.websocket.connection_manager import manager
from app.models.schemas import (
    DashboardEventType,
    DashboardEvent,
    TransactionCompletedEvent,
    WalletBalanceChangedEvent,
    KycVerifiedEvent,
    UserMetricsUpdatedEvent
)

logger = get_logger(__name__)
websocket_router = APIRouter(prefix="/ws", tags=["WebSocket"])


@websocket_router.websocket("/dashboard/{user_id}")
async def dashboard_websocket(
    websocket: WebSocket,
    user_id: str,
    events: str = Query("all", description="Comma-separated list of event types to subscribe to")
):
    await manager.connect(websocket, user_id)
    
    requested_events = [e.strip() for e in events.split(",")]
    logger.info("Dashboard WebSocket connection established", user_id=user_id, requested_events=requested_events)
    
    try:
        while True:
            data = await websocket.receive_json()
            
            logger.debug("WebSocket message received", user_id=user_id, data=data)
            
            if data.get("type") == "ping":
                await manager.send_personal_message({"type": "pong"}, websocket)
            
    except WebSocketDisconnect:
        manager.disconnect(websocket, user_id)
        logger.info("Dashboard WebSocket disconnected", user_id=user_id)
    except Exception as e:
        logger.error("WebSocket error", user_id=user_id, error=str(e), exc_info=e)
        manager.disconnect(websocket, user_id)