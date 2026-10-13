from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query
from datetime import datetime
from structlog import get_logger
import base64
import hashlib
import hmac
import json

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


async def _validate_ws_token(websocket: WebSocket) -> str | None:
    """
    BUG-AUTH-021: Validate JWT/token on WebSocket connection.
    Expects a token as a query parameter or in the first message.
    Returns the verified subject on success, None otherwise — callers MUST
    bind the returned subject to the path identity before connecting.
    """
    from jose.exceptions import JWTError

    from app.jwt_auth import verify_jwt

    token = websocket.query_params.get("token")
    if not token:
        return None

    try:
        # Cryptographically verify against Keycloak JWKS (AI-AUTH-001):
        # never authorize on an unverified decode, even behind the gateway.
        payload = await verify_jwt(token)
        return payload.get("sub") or None
    except JWTError as e:
        logger.warning("WebSocket token validation failed", error=str(e))
        return None
    except Exception as e:
        logger.warning("WebSocket token validation failed", error=str(e))
        return None


def _b64url_decode(value: str) -> bytes:
    return base64.urlsafe_b64decode(value + "=" * (-len(value) % 4))


async def _validate_ws_ticket(ticket: str, account_id: str) -> bool:
    """
    WEB-OS-008: validate a BFF-issued ticket for the analytics WebSocket.

    The web-app BFF signs tickets because browser JS cannot read the httpOnly
    session cookie needed to mint a JWT. ticket = base64url(payload).hmac_hex
    with payload {"sub", "account_id", "exp"} and secret = settings.ws_ticket_secret.
    Fails closed on empty secret (ticket auth disabled).
    """
    secret = get_settings().ws_ticket_secret
    if not secret or not ticket:
        return False
    if "." not in ticket:
        return False
    payload_b64, sig = ticket.rsplit(".", 1)
    expected = hmac.new(secret.encode(), payload_b64.encode(), hashlib.sha256).hexdigest()
    if not hmac.compare_digest(expected, sig):
        logger.warning("WebSocket ticket rejected — bad signature")
        return False
    try:
        payload = json.loads(_b64url_decode(payload_b64))
    except Exception:
        logger.warning("WebSocket ticket rejected — malformed payload")
        return False
    if int(payload.get("exp", 0)) < int(datetime.utcnow().timestamp()):
        logger.warning("WebSocket ticket rejected — expired")
        return False
    if payload.get("account_id") != account_id:
        logger.warning("WebSocket ticket rejected — account mismatch")
        return False
    return True


@websocket_router.websocket("/dashboard/{user_id}")
async def dashboard_websocket(
    websocket: WebSocket,
    user_id: str,
    events: str = Query("all", description="Comma-separated list of event types to subscribe to")
):
    # BUG-AUTH-021: Validate token before accepting the connection
    subject = await _validate_ws_token(websocket)
    if subject is None or subject != user_id:
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


@websocket_router.websocket("/analytics/{account_id}")
async def analytics_websocket(
    websocket: WebSocket,
    account_id: str,
    ticket: str = Query(None, description="BFF-issued ticket (browser session)"),
    token: str = Query(None, description="Keycloak JWT (server-to-server clients)"),
):
    """
    WEB-OS-008: real-time analytics stream consumed by the web-app hook
    `useAnalyticsWebSocket` (wss://<ws-host>/ws/analytics/<accountId>).

    Auth: `?ticket=` signed by the web-app BFF from the httpOnly session, or
    `?token=` Keycloak JWT. Payload contract = DashboardEvent dump:
    {"event_type": "WALLET_BALANCE_CHANGED", "user_id", "timestamp", "data"}.
    """
    if ticket:
        authorized = await _validate_ws_ticket(ticket, account_id)
    elif token:
        subject = await _validate_ws_token(websocket)
        authorized = subject is not None and subject == account_id
    else:
        authorized = False
    if not authorized:
        await websocket.close(code=4001, reason="Authentication required")
        logger.warning("Analytics WebSocket connection rejected", account_id=account_id)
        return

    events = {DashboardEventType.WALLET_BALANCE_CHANGED.value}
    await manager.connect(websocket, account_id, events)

    await manager.send_personal_message({
        "type": "connection_established",
        "user_id": account_id,
        "subscribed_events": list(events),
        "timestamp": datetime.utcnow().isoformat()
    }, websocket)

    logger.info("Analytics WebSocket connection established", account_id=account_id)

    try:
        while True:
            data = await websocket.receive_json()
            if data.get("type") == "ping":
                await manager.send_personal_message({
                    "type": "pong",
                    "timestamp": datetime.utcnow().isoformat()
                }, websocket)
            elif data.get("type") == "subscribe":
                requested = {e.strip() for e in data.get("events", "").split(",") if e.strip()}
                requested = {e for e in requested if e in DashboardEventType._value2member_map_}
                manager.update_user_subscriptions(account_id, websocket, requested | events)
                await manager.send_personal_message({
                    "type": "subscription_updated",
                    "subscribed_events": list(requested | events),
                    "timestamp": datetime.utcnow().isoformat()
                }, websocket)
    except WebSocketDisconnect:
        manager.disconnect(websocket, account_id)
        logger.info("Analytics WebSocket disconnected", account_id=account_id)
    except Exception as e:
        logger.error("WebSocket error", account_id=account_id, error=str(e), exc_info=e)
        manager.disconnect(websocket, account_id)
