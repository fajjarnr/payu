import pytest
import asyncio
from unittest.mock import AsyncMock, Mock, patch
from datetime import datetime
from uuid import uuid4

from app.messaging.kafka_consumer import KafkaConsumerService
from app.models.schemas import DashboardEventType


@pytest.fixture
def kafka_consumer():
    """Create a Kafka consumer service instance"""
    return KafkaConsumerService()


@pytest.fixture
def mock_session():
    """Mock database session"""
    session = AsyncMock()
    session.commit = AsyncMock()
    session.add = Mock()
    return session


@pytest.mark.asyncio
async def test_kafka_consumer_start(kafka_consumer):
    """Test starting the Kafka consumer"""
    with patch('app.messaging.kafka_consumer.AIOKafkaConsumer') as mock_consumer_class:
        mock_consumer = AsyncMock()
        mock_consumer_class.return_value = mock_consumer
        
        await kafka_consumer.start()
        
        assert kafka_consumer._running is True
        assert kafka_consumer._task is not None


@pytest.mark.asyncio
async def test_kafka_consumer_stop(kafka_consumer):
    """Test stopping the Kafka consumer"""
    kafka_consumer._running = True
    kafka_consumer._task = asyncio.create_task(asyncio.sleep(1))
    kafka_consumer.consumer = AsyncMock()
    kafka_consumer.consumer.stop = AsyncMock()
    
    await kafka_consumer.stop()
    
    assert kafka_consumer._running is False


@pytest.mark.asyncio
async def test_process_transaction_completed_message(kafka_consumer, mock_session):
    """Test processing a transaction completed Kafka message"""
    message = {
        'user_id': 'test_user_123',
        'transaction_id': 'txn_456',
        'amount': 5000.0,
        'currency': 'IDR',
        'type': 'TRANSFER',
        'category': 'FOOD',
        'recipient_id': 'recipient_789',
        'merchant_id': 'merchant_101'
    }
    
    with patch.object(kafka_consumer, '_update_user_metrics', new_callable=AsyncMock) as mock_update_metrics, \
         patch.object(kafka_consumer, 'manager') as mock_manager:
        mock_manager.broadcast_to_user = AsyncMock()
        
        await kafka_consumer._handle_transaction_completed(mock_session, message)
        
        mock_update_metrics.assert_called_once_with(mock_session, 'test_user_123', 5000.0)
        mock_session.add.assert_called_once()
        mock_manager.broadcast_to_user.assert_called_once()
        
        call_args = mock_manager.broadcast_to_user.call_args
        broadcast_msg = call_args[0][0]
        assert broadcast_msg['event_type'] == DashboardEventType.TRANSACTION_COMPLETED


@pytest.mark.asyncio
async def test_process_wallet_balance_changed_message(kafka_consumer, mock_session):
    """Test processing a wallet balance changed Kafka message"""
    message = {
        'user_id': 'test_user_123',
        'wallet_id': 'wallet_456',
        'balance': 100000.0,
        'currency': 'IDR',
        'change_amount': 5000.0,
        'change_type': 'CREDIT'
    }
    
    with patch.object(kafka_consumer, 'manager') as mock_manager:
        mock_manager.broadcast_to_user = AsyncMock()
        
        await kafka_consumer._handle_wallet_balance_changed(mock_session, message)
        
        mock_session.add.assert_called_once()
        mock_manager.broadcast_to_user.assert_called_once()
        
        call_args = mock_manager.broadcast_to_user.call_args
        broadcast_msg = call_args[0][0]
        assert broadcast_msg['event_type'] == DashboardEventType.WALLET_BALANCE_CHANGED


@pytest.mark.asyncio
async def test_process_kyc_verified_message(kafka_consumer, mock_session):
    """Test processing a KYC verified Kafka message"""
    message = {
        'user_id': 'test_user_123'
    }
    
    with patch('app.messaging.kafka_consumer.select') as mock_select:
        mock_result = AsyncMock()
        mock_result.scalar_one_or_none.return_value = None
        mock_session.execute = AsyncMock(return_value=mock_result)
        
        with patch.object(kafka_consumer, 'manager') as mock_manager:
            mock_manager.broadcast_to_user = AsyncMock()
            
            await kafka_consumer._handle_kyc_verified(mock_session, message)
            
            mock_session.add.assert_called_once()
            mock_manager.broadcast_to_user.assert_called_once()
            
            call_args = mock_manager.broadcast_to_user.call_args
            broadcast_msg = call_args[0][0]
            assert broadcast_msg['event_type'] == DashboardEventType.KYC_VERIFIED


@pytest.mark.asyncio
async def test_update_user_metrics_existing_user(kafka_consumer, mock_session):
    """Test updating metrics for an existing user"""
    from app.database import UserMetricsEntity
    
    mock_metrics = Mock()
    mock_metrics.total_transactions = 10
    mock_metrics.total_amount = 100000.0
    
    mock_result = AsyncMock()
    mock_result.scalar_one_or_none.return_value = mock_metrics
    mock_session.execute = AsyncMock(return_value=mock_result)
    
    with patch.object(kafka_consumer, 'manager') as mock_manager:
        mock_manager.broadcast_to_user = AsyncMock()
        
        await kafka_consumer._update_user_metrics(mock_session, 'test_user_123', 5000.0)
        
        assert mock_metrics.total_transactions == 11
        assert mock_metrics.total_amount == 105000.0
        mock_manager.broadcast_to_user.assert_called_once()


@pytest.mark.asyncio
async def test_update_user_metrics_new_user(kafka_consumer, mock_session):
    """Test creating metrics for a new user"""
    mock_result = AsyncMock()
    mock_result.scalar_one_or_none.return_value = None
    mock_session.execute = AsyncMock(return_value=mock_result)
    
    with patch.object(kafka_consumer, 'manager') as mock_manager:
        mock_manager.broadcast_to_user = AsyncMock()
        
        await kafka_consumer._update_user_metrics(mock_session, 'test_user_123', 5000.0)
        
        mock_session.add.assert_called_once()


@pytest.mark.asyncio
async def test_process_transaction_initiated_message(kafka_consumer, mock_session):
    """Test processing a transaction initiated Kafka message"""
    message = {
        'user_id': 'test_user_123',
        'transaction_id': 'txn_456',
        'amount': 5000.0,
        'currency': 'IDR',
        'type': 'PAYMENT',
        'category': 'BILLS'
    }
    
    await kafka_consumer._handle_transaction_initiated(mock_session, message)
    
    mock_session.add.assert_called_once()
    
    call_args = mock_session.add.call_args
    entity = call_args[0][0]
    assert entity.status == 'PENDING'
