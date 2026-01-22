from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query
from datetime import datetime
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
    requested_events = set([e.strip() for e in events.split(",")])
    if "all" in requested_events:
        requested_events = set([evt.value for evt in DashboardEventType])
    
    await manager.connect(websocket, user_id, requested_events)
    
    await manager.send_personal_message({
        "type": "connection_established",
        "user_id": user_id,
        "subscribed_events": list(requested_events),
        "timestamp": datetime.utcnow().isoformat()
    }, websocket)
    
    logger.info("Dashboard WebSocket connection established", user_id=user_id, requested_events=list(requested_events))
    
    try:
        while True:
            data = await websocket.receive_json()
            
            logger.debug("WebSocket message received", user_id=user_id, data=data)
            
            if data.get("type") == "ping":
                await manager.send_personal_message({
                    "type": "pong",
                    "timestamp": datetime.utcnow().isoformat()
                }, websocket)
            elif data.get("type") == "subscribe":
                new_events_str = data.get("events", "all")
                new_events = set([e.strip() for e in new_events_str.split(",")])
                if "all" in new_events:
                    new_events = set([evt.value for evt in DashboardEventType])
                
                manager.update_user_subscriptions(user_id, websocket, new_events)
                
                await manager.send_personal_message({
                    "type": "subscription_updated",
                    "subscribed_events": list(new_events),
                    "timestamp": datetime.utcnow().isoformat()
                }, websocket)
                logger.info("WebSocket subscription updated", user_id=user_id, requested_events=list(new_events))
            
    except WebSocketDisconnect:
        manager.disconnect(websocket, user_id)
        logger.info("Dashboard WebSocket disconnected", user_id=user_id)
    except Exception as e:
        logger.error("WebSocket error", user_id=user_id, error=str(e), exc_info=e)
        manager.disconnect(websocket, user_id)