"""
Unit tests for Kafka Producer Service
Tests cover event publishing, error handling, and connection management
"""
import pytest
import sys
from unittest.mock import MagicMock, AsyncMock, patch

sys.path.insert(0, "/home/ubuntu/payu/backend/kyc-service/src")

from app.messaging.kafka_producer import KafkaProducerService
from aiokafka.errors import KafkaError


@pytest.mark.unit
class TestKafkaProducerService:
    """Unit tests for Kafka producer service"""

    @pytest.fixture
    def kafka_producer(self):
        """Create Kafka producer service instance"""
        return KafkaProducerService()

    @pytest.mark.asyncio
    async def test_publish_event_success(self, kafka_producer):
        """Test successful event publishing"""
        mock_producer = AsyncMock()
        mock_producer.send_and_wait = AsyncMock()

        with patch.object(kafka_producer, '_get_producer', return_value=mock_producer):
            await kafka_producer.publish_event(
                topic="test.topic",
                event={"type": "test", "verification_id": "123"}
            )

            mock_producer.send_and_wait.assert_called_once()

    @pytest.mark.asyncio
    async def test_publish_event_kafka_error(self, kafka_producer):
        """Test event publishing with Kafka error"""
        mock_producer = AsyncMock()
        mock_producer.send_and_wait.side_effect = KafkaError("Kafka connection failed")

        with patch.object(kafka_producer, '_get_producer', return_value=mock_producer):
            with pytest.raises(KafkaError):
                await kafka_producer.publish_event(
                    topic="test.topic",
                    event={"type": "test"}
                )

    @pytest.mark.asyncio
    async def test_get_producer_creates_new_producer(self, kafka_producer):
        """Test that _get_producer creates a new producer when None exists"""
        mock_producer_instance = AsyncMock()
        mock_producer_instance.start = AsyncMock()

        with patch('app.messaging.kafka_producer.AIOKafkaProducer') as MockProducer:
            MockProducer.return_value = mock_producer_instance

            producer = await kafka_producer._get_producer()

            assert producer is not None
            mock_producer_instance.start.assert_called_once()

    @pytest.mark.asyncio
    async def test_get_producer_reuses_existing_producer(self, kafka_producer):
        """Test that _get_producer reuses existing producer"""
        mock_producer = AsyncMock()

        with patch.object(kafka_producer, '_lock'):
            kafka_producer.producer = mock_producer

            producer = await kafka_producer._get_producer()

            assert producer == mock_producer

    @pytest.mark.asyncio
    async def test_close_stops_producer(self, kafka_producer):
        """Test closing the producer"""
        mock_producer = AsyncMock()
        mock_producer.stop = AsyncMock()
        kafka_producer.producer = mock_producer

        await kafka_producer.close()

        mock_producer.stop.assert_called_once()
        assert kafka_producer.producer is None

    @pytest.mark.asyncio
    async def test_close_with_no_producer(self, kafka_producer):
        """Test closing when producer is None"""
        kafka_producer.producer = None

        await kafka_producer.close()

        # Should not raise any exception
        assert kafka_producer.producer is None

    @pytest.mark.asyncio
    async def test_publish_event_with_large_payload(self, kafka_producer):
        """Test event publishing with large payload"""
        mock_producer = AsyncMock()
        mock_producer.send_and_wait = AsyncMock()

        large_event = {
            "type": "large_test",
            "verification_id": "123",
            "data": "x" * 10000  # Large payload
        }

        with patch.object(kafka_producer, '_get_producer', return_value=mock_producer):
            await kafka_producer.publish_event(
                topic="test.topic",
                event=large_event
            )

            mock_producer.send_and_wait.assert_called_once()

    @pytest.mark.asyncio
    async def test_publish_event_to_multiple_topics(self, kafka_producer):
        """Test publishing events to multiple topics"""
        mock_producer = AsyncMock()
        mock_producer.send_and_wait = AsyncMock()

        topics = ["topic1", "topic2", "topic3"]

        with patch.object(kafka_producer, '_get_producer', return_value=mock_producer):
            for topic in topics:
                await kafka_producer.publish_event(
                    topic=topic,
                    event={"type": "test"}
                )

            assert mock_producer.send_and_wait.call_count == len(topics)

    @pytest.mark.asyncio
    async def test_producer_initialization(self, kafka_producer):
        """Test that producer service initializes correctly"""
        assert kafka_producer.producer is None
        assert hasattr(kafka_producer, '_lock')

    @pytest.mark.asyncio
    async def test_concurrent_publish_events(self, kafka_producer):
        """Test concurrent event publishing"""
        import asyncio

        mock_producer = AsyncMock()
        mock_producer.send_and_wait = AsyncMock()

        async def publish_event(topic):
            await kafka_producer.publish_event(
                topic=topic,
                event={"type": "test"}
            )

        with patch.object(kafka_producer, '_get_producer', return_value=mock_producer):
            await asyncio.gather(
                publish_event("topic1"),
                publish_event("topic2"),
                publish_event("topic3")
            )

    @pytest.mark.asyncio
    async def test_publish_event_without_type(self, kafka_producer):
        """Test publishing event without type field"""
        mock_producer = AsyncMock()
        mock_producer.send_and_wait = AsyncMock()

        event = {"verification_id": "123", "status": "pending"}

        with patch.object(kafka_producer, '_get_producer', return_value=mock_producer):
            await kafka_producer.publish_event(
                topic="test.topic",
                event=event
            )

            mock_producer.send_and_wait.assert_called_once()

    @pytest.mark.asyncio
    async def test_close_idempotent(self, kafka_producer):
        """Test that close can be called multiple times safely"""
        mock_producer = AsyncMock()
        mock_producer.stop = AsyncMock()
        kafka_producer.producer = mock_producer

        await kafka_producer.close()
        await kafka_producer.close()  # Second close

        # Should only stop once
        mock_producer.stop.assert_called_once()
