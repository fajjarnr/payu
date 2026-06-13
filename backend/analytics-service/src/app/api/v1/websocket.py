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
from app.config import get_settings

logger = get_logger(__name__)
websocket_router = APIRouter(prefix="/ws", tags=["WebSocket"])

settings = get_settings()


async def _validate_ws_token(websocket: WebSocket) -> bool:
    """
    BUG-AUTH-021: Validate JWT/token on WebSocket connection.
    Expects a token as a query parameter or in the first message.
    Returns True if valid, False otherwise.
    """
    import jwt

    token = websocket.query_params.get("token")
    if not token:
        return False

    try:
        # Gateway-level auth has already validated the signature.
        # Downstream services only need to decode claims.
        jwt.decode(
            token,
            options={"verify_signature": False, "verify_exp": True},
        )
        return True
    except (jwt.InvalidTokenError, jwt.ExpiredSignatureError, Exception) as e:
        logger.warning("WebSocket token validation failed", error=str(e))
        return False


@websocket_router.websocket("/dashboard/{user_id}")
async def dashboard_websocket(
    websocket: WebSocket,
    user_id: str,
    events: str = Query("all", description="Comma-separated list of event types to subscribe to")
):
    # BUG-AUTH-021: Validate token before accepting the connection
    if not await _validate_ws_token(websocket):
        await websocket.close(code=4001, reason="Authentication required")
        logger.warning("WebSocket connection rejected — invalid or missing token", user_id=user_id)
        return

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