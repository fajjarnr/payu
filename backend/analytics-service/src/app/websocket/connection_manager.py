import json
from typing import Dict, List
from fastapi import WebSocket, WebSocketDisconnect
from structlog import get_logger

logger = get_logger(__name__)


class ConnectionManager:
    def __init__(self):
        self.active_connections: Dict[str, List[WebSocket]] = {}

    async def connect(self, websocket: WebSocket, user_id: str):
        await websocket.accept()
        if user_id not in self.active_connections:
            self.active_connections[user_id] = []
        self.active_connections[user_id].append(websocket)
        logger.info("WebSocket client connected", user_id=user_id, connections_per_user=len(self.active_connections[user_id]))

    def disconnect(self, websocket: WebSocket, user_id: str):
        if user_id in self.active_connections:
            self.active_connections[user_id].remove(websocket)
            if not self.active_connections[user_id]:
                del self.active_connections[user_id]
        logger.info("WebSocket client disconnected", user_id=user_id)

    async def send_personal_message(self, message: dict, websocket: WebSocket):
        await websocket.send_json(message)

    async def broadcast_to_user(self, message: dict, user_id: str):
        if user_id in self.active_connections:
            for connection in self.active_connections[user_id]:
                try:
                    await connection.send_json(message)
                except Exception as e:
                    logger.error("Failed to send WebSocket message", user_id=user_id, error=str(e))
                    self.active_connections[user_id].remove(connection)

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


manager = ConnectionManager()