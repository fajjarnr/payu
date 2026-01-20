import json
import asyncio
from aiokafka import AIOKafkaProducer
from aiokafka.errors import KafkaError
from structlog import get_logger
from typing import Dict, Any

from app.config import get_settings

logger = get_logger(__name__)
settings = get_settings()


class KafkaProducerService:
    def __init__(self):
        self.producer: AIOKafkaProducer = None
        self._lock = asyncio.Lock()
        logger.info("Kafka producer service initialized")

    async def _get_producer(self) -> AIOKafkaProducer:
        async with self._lock:
            if self.producer is None:
                self.producer = AIOKafkaProducer(
                    bootstrap_servers=settings.kafka_bootstrap_servers,
                    value_serializer=lambda v: json.dumps(v).encode('utf-8'),
                    request_timeout_ms=10000,
                    max_block_ms=5000
                )
                await self.producer.start()
                logger.info("Kafka producer started")
            return self.producer

    async def publish_event(self, topic: str, event: Dict[str, Any]):
        try:
            producer = await self._get_producer()

            await producer.send_and_wait(topic, event)

            logger.info(
                "Kafka event published",
                topic=topic,
                event_type=event.get('type', 'unknown'),
                verification_id=event.get('verification_id')
            )

        except KafkaError as e:
            logger.error("Failed to publish Kafka event", topic=topic, exc_info=e)
            raise

    async def close(self):
        async with self._lock:
            if self.producer is not None:
                await self.producer.stop()
                self.producer = None
                logger.info("Kafka producer stopped")
