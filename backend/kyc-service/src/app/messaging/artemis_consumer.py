import stomp
import json
from structlog import get_logger
from app.config import get_settings
from app.database import async_session_maker
from app.services.kyc_service import KycService
import asyncio

logger = get_logger(__name__)
settings = get_settings()

class ArtemisListener(stomp.ConnectionListener):
    def __init__(self, loop):
        self.loop = loop

    def on_error(self, frame):
        logger.error("Received an error frame from Artemis STOMP", body=frame.body)

    def on_message(self, frame):
        logger.info("Received message from Artemis STOMP", body=frame.body)
        # Run async handler in the main event loop
        asyncio.run_coroutine_threadsafe(self.handle_message(frame.body), self.loop)

    async def handle_message(self, body):
        try:
            data = json.loads(body)
            user_id = data.get("user_id") or data.get("userId")
            verification_type = data.get("verification_type") or data.get("verificationType") or "FULL_KYC"
            if not user_id:
                logger.warn("Message missing user_id - ignoring")
                return

            logger.info("Triggering KYC verification via command", user_id=user_id, verification_type=verification_type)
            if async_session_maker is None:
                logger.error("Database session maker is not initialized")
                return

            async with async_session_maker() as session:
                kyc_service = KycService(session)
                await kyc_service.create_verification(user_id, verification_type)
        except Exception as e:
            logger.error("Failed to process KYC command", exc_info=e)

class ArtemisConsumerService:
    def __init__(self):
        self.conn = None
        self._listener = None

    def start(self):
        try:
            loop = asyncio.get_running_loop()
            self.conn = stomp.Connection(
                [(settings.artemis_host, settings.artemis_stomp_port)],
                heartbeats=(
                    settings.artemis_heartbeat_send_ms,
                    settings.artemis_heartbeat_receive_ms,
                ),
            )
            self._listener = ArtemisListener(loop)
            self.conn.set_listener('ArtemisListener', self._listener)
            self.conn.connect(settings.artemis_username, settings.artemis_password, wait=True)
            # ActiveMQ Artemis queue destination naming for STOMP is /queue/<name> or just queue name
            # Normally Artemis mapping handles destination directly or with prefix. Default is queue name.
            self.conn.subscribe(destination=settings.artemis_kyc_queue, id=1, ack='auto')
            logger.info("Connected to Artemis STOMP", host=settings.artemis_host, port=settings.artemis_stomp_port, queue=settings.artemis_kyc_queue)
        except Exception as e:
            logger.error("Failed to start Artemis consumer service", exc_info=e)

    def stop(self):
        try:
            if self.conn and self.conn.is_connected():
                self.conn.disconnect()
                logger.info("Disconnected from Artemis STOMP")
        except Exception as e:
            logger.error("Error during Artemis consumer shutdown", exc_info=e)
