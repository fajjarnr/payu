import json
from typing import Dict, List, Set
from fastapi import WebSocket, WebSocketDisconnect
from structlog import get_logger

logger = get_logger(__name__)


class ConnectionManager:
    def __init__(self):
        self.active_connections: Dict[str, List[WebSocket]] = {}
        self.user_subscriptions: Dict[str, Dict[WebSocket, Set[str]]] = {}

    async def connect(self, websocket: WebSocket, user_id: str, subscribed_events: Set[str] = None):
        await websocket.accept()
        if user_id not in self.active_connections:
            self.active_connections[user_id] = []
        self.active_connections[user_id].append(websocket)
        
        if user_id not in self.user_subscriptions:
            self.user_subscriptions[user_id] = {}
        self.user_subscriptions[user_id][websocket] = subscribed_events or set()
        
        logger.info("WebSocket client connected", user_id=user_id, connections_per_user=len(self.active_connections[user_id]), subscribed_events=list(subscribed_events or []))

    def disconnect(self, websocket: WebSocket, user_id: str):
        if user_id in self.active_connections:
            self.active_connections[user_id].remove(websocket)
            if not self.active_connections[user_id]:
                del self.active_connections[user_id]
        
        if user_id in self.user_subscriptions and websocket in self.user_subscriptions[user_id]:
            del self.user_subscriptions[user_id][websocket]
            if not self.user_subscriptions[user_id]:
                del self.user_subscriptions[user_id]
        
        logger.info("WebSocket client disconnected", user_id=user_id)

    async def send_personal_message(self, message: dict, websocket: WebSocket):
        await websocket.send_json(message)

    async def broadcast_to_user(self, message: dict, user_id: str, event_type: str = None):
        if user_id not in self.active_connections:
            return
        
        for connection in self.active_connections[user_id]:
            try:
                if event_type:
                    user_events = self.user_subscriptions.get(user_id, {}).get(connection, set())
                    if "all" not in user_events and event_type not in user_events:
                        logger.debug("Skipping event due to subscription filter", user_id=user_id, event_type=event_type, subscribed_events=list(user_events))
                        continue
                await connection.send_json(message)
            except Exception as e:
                logger.error("Failed to send WebSocket message", user_id=user_id, error=str(e))
                self.active_connections[user_id].remove(connection)
                if user_id in self.user_subscriptions and connection in self.user_subscriptions[user_id]:
                    del self.user_subscriptions[user_id][connection]

    async def broadcast_to_all(self, message: dict):
        for user_id, connections in self.active_connections.items():
            for connection in connections:
                try:
                    await connection.send_json(message)
                except Exception as e:
                    logger.error("Failed to broadcast WebSocket message", user_id=user_id, error=str(e))
                    connections.remove(connection)

    def get_active_connections_count(self) -> int:
        return sum(len(connections) for connections in self.active_connections.values())

    def get_user_connection_count(self, user_id: str) -> int:
        return len(self.active_connections.get(user_id, []))

    def update_user_subscriptions(self, user_id: str, websocket: WebSocket, events: Set[str]):
        if user_id in self.user_subscriptions:
            self.user_subscriptions[user_id][websocket] = events
        logger.info("User subscriptions updated", user_id=user_id, events=list(events))


manager = ConnectionManager()