package id.payu.integration.domain;

import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageDirection;
import id.payu.integration.domain.model.MessageStatus;
import id.payu.integration.domain.model.MessageType;
import id.payu.integration.domain.repository.IntegrationMessageRepository;
import id.payu.integration.application.service.MessageProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageProcessingService.
 */
@ExtendWith(MockitoExtension.class)
public class MessageProcessingServiceTest {

    @Mock
    private IntegrationMessageRepository messageRepository;

    @InjectMocks
    private MessageProcessingService messageProcessingService;

    private IntegrationMessage testMessage;

    @BeforeEach
    void setUp() {
        testMessage = IntegrationMessage.builder()
                .messageId("test-id")
                .type(MessageType.SWIFT_MT103)
                .direction(MessageDirection.INBOUND)
                .sourceSystem("SWIFT")
                .targetSystem("PAYU")
                .rawPayload("test payload")
                .status(MessageStatus.RECEIVED)
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    @Test
    void testCreateMessage() {
        when(messageRepository.save(any(IntegrationMessage.class))).thenReturn(testMessage);

        IntegrationMessage result = messageProcessingService.createMessage(
                MessageType.SWIFT_MT103,
                MessageDirection.INBOUND,
                "SWIFT",
                "PAYU",
                "test payload",
                null,
                null
        );

        assertNotNull(result);
        assertEquals(MessageType.SWIFT_MT103, result.getType());
        assertEquals(MessageDirection.INBOUND, result.getDirection());
        assertEquals(MessageStatus.RECEIVED, result.getStatus());
        verify(messageRepository).save(any(IntegrationMessage.class));
    }

    @Test
    void testMarkSent() {
        when(messageRepository.findById("test-id")).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(IntegrationMessage.class))).thenReturn(testMessage);

        messageProcessingService.markSent("test-id");

        assertEquals(MessageStatus.SENT, testMessage.getStatus());
        assertNotNull(testMessage.getProcessedAt());
        verify(messageRepository).save(testMessage);
    }

    @Test
    void testMarkFailed() {
        when(messageRepository.findById("test-id")).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(IntegrationMessage.class))).thenReturn(testMessage);

        messageProcessingService.markFailed("test-id", "Test error");

        assertEquals(MessageStatus.FAILED, testMessage.getStatus());
        assertEquals("Test error", testMessage.getErrorMessage());
        assertNotNull(testMessage.getProcessedAt());
        verify(messageRepository).save(testMessage);
    }

    @Test
    void testRetryMessage() {
        testMessage.setStatus(MessageStatus.FAILED);
        when(messageRepository.findById("test-id")).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(IntegrationMessage.class))).thenReturn(testMessage);

        boolean result = messageProcessingService.retryMessage("test-id");

        assertTrue(result);
        assertEquals(1, testMessage.getRetryCount());
        assertEquals(MessageStatus.RECEIVED, testMessage.getStatus());
        assertNotNull(testMessage.getLastRetryAt());
        verify(messageRepository).save(testMessage);
    }

    @Test
    void testRetryMessageExceedsMax() {
        testMessage.setStatus(MessageStatus.FAILED);
        testMessage.setRetryCount(3);
        when(messageRepository.findById("test-id")).thenReturn(Optional.of(testMessage));

        boolean result = messageProcessingService.retryMessage("test-id");

        assertFalse(result);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void retryMessageRejectsNonFailedMessage() {
        testMessage.setStatus(MessageStatus.SENT);
        when(messageRepository.findById("test-id")).thenReturn(Optional.of(testMessage));

        boolean result = messageProcessingService.retryMessage("test-id");

        assertFalse(result);
        assertEquals(MessageStatus.SENT, testMessage.getStatus());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void cancelMessagePersistsCancellation() {
        when(messageRepository.findById("test-id")).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(testMessage)).thenReturn(testMessage);

        messageProcessingService.cancelMessage("test-id");

        assertEquals(MessageStatus.CANCELLED, testMessage.getStatus());
        assertNotNull(testMessage.getProcessedAt());
        verify(messageRepository).save(testMessage);
    }

    @Test
    void testGetMessageNotFound() {
        when(messageRepository.findById("unknown-id")).thenReturn(Optional.empty());

        assertThrows(MessageProcessingService.MessageNotFoundException.class, () ->
                messageProcessingService.getMessageStatus("unknown-id"));
    }

    @Test
    void testCanRetry() {
        testMessage.setRetryCount(2);
        testMessage.setMaxRetries(3);

        assertTrue(testMessage.canRetry());

        testMessage.setRetryCount(3);
        assertFalse(testMessage.canRetry());
    }
}
