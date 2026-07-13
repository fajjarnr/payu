package id.payu.integration.application.service;

import id.payu.integration.application.port.in.IntegrationUseCase;
import id.payu.integration.application.port.out.MessagePublisherPort;
import id.payu.integration.domain.model.*;
import id.payu.integration.application.service.MessageProcessingService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Application service implementing integration use cases.
 * Orchestrates Camel routes and domain services.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationService implements IntegrationUseCase {

    private final MessageProcessingService messageProcessingService;
    private final MessagePublisherPort messagePublisherPort;
    // BUG-INT-HEX-001: ProducerTemplate moved to MessagePublisherAdapter. App uses port only.

    @Override
    @CircuitBreaker(name = "integration", fallbackMethod = "processSwiftMessageFallback")
    @Retry(name = "integration")
    @Transactional
    public String processSwiftMessage(String swiftMessage, String messageType) {
        log.info("Processing SWIFT message of type: {}", messageType);

        MessageType type = parseSwiftMessageType(messageType);

        IntegrationMessage message = messageProcessingService.createMessage(
                type,
                MessageDirection.INBOUND,
                "SWIFT_NETWORK",
                "PAYU_CORE",
                swiftMessage,
                UUID.randomUUID().toString(),
                extractBusinessReference(swiftMessage)
        );

        try {
            // Send to Camel route for processing
            messagePublisherPort.routeInternal("direct:swift-inbound", message, java.util.Map.of());
            return message.getMessageId();
        } catch (Exception e) {
            log.error("Failed to process SWIFT message", e);
            messageProcessingService.markFailed(message.getMessageId(), e.getMessage());
            throw new IntegrationException("SWIFT processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    @CircuitBreaker(name = "integration", fallbackMethod = "generateOjkReportFallback")
    @Retry(name = "integration")
    @Transactional
    public String generateOjkReport(String reportType, LocalDate date) {
        log.info("Generating OJK report of type: {} for date: {}", reportType, date);

        String reportContent = generateReportContent(reportType, date);
        MessageType type = reportType.toUpperCase().contains("XML") ? MessageType.OJK_XML : MessageType.OJK_CSV;

        IntegrationMessage message = messageProcessingService.createMessage(
                type,
                MessageDirection.OUTBOUND,
                "PAYU_CORE",
                "OJK_REPORTING",
                reportContent,
                UUID.randomUUID().toString(),
                "OJK_" + reportType + "_" + date.format(DateTimeFormatter.BASIC_ISO_DATE)
        );

        try {
            String route = type == MessageType.OJK_XML ? "direct:ojk-xml-report" : "direct:ojk-csv-report";
            messagePublisherPort.routeInternal(route, message, java.util.Map.of("reportDate", date.toString()));
            return message.getMessageId();
        } catch (Exception e) {
            log.error("Failed to generate OJK report", e);
            messageProcessingService.markFailed(message.getMessageId(), e.getMessage());
            throw new IntegrationException("OJK report generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    @CircuitBreaker(name = "integration", fallbackMethod = "sendSoapRequestFallback")
    @Retry(name = "integration")
    @Transactional
    public String sendSoapRequest(String endpoint, String operation, String payload) {
        log.info("Sending SOAP request to: {} operation: {}", endpoint, operation);

        IntegrationMessage message = messageProcessingService.createMessage(
                MessageType.SOAP,
                MessageDirection.OUTBOUND,
                "PAYU_CORE",
                extractHostFromEndpoint(endpoint),
                payload,
                UUID.randomUUID().toString(),
                operation
        );

        try {
            String response = messagePublisherPort.routeInternal(
                    "direct:soap-request",
                    message,
                    Map.of(
                            "SoapEndpoint", endpoint,
                            "SoapOperation", operation,
                            "MessageId", message.getMessageId()
                    )
            );

            messageProcessingService.markSent(message.getMessageId());
            return response;
        } catch (Exception e) {
            log.error("Failed to send SOAP request", e);
            messageProcessingService.markFailed(message.getMessageId(), e.getMessage());
            throw new IntegrationException("SOAP request failed: " + e.getMessage(), e);
        }
    }

    @Override
    @CircuitBreaker(name = "integration", fallbackMethod = "sendHttpRequestFallback")
    @Retry(name = "integration")
    @Transactional
    public String sendHttpRequest(String url, String method, Map<String, String> headers, String body) {
        log.info("Sending HTTP {} request to: {}", method, url);

        IntegrationMessage message = messageProcessingService.createMessage(
                MessageType.HTTP_JSON,
                MessageDirection.OUTBOUND,
                "PAYU_CORE",
                extractHostFromEndpoint(url),
                body,
                UUID.randomUUID().toString(),
                method + "_" + url
        );

        try {
            String response = messagePublisherPort.routeInternal(
                    "direct:http-request",
                    message,
                    Map.of(
                            "HttpUrl", url,
                            "HttpMethod", method,
                            "HttpHeaders", headers,
                            "MessageId", message.getMessageId()
                    )
            );

            messageProcessingService.markSent(message.getMessageId());
            return response;
        } catch (Exception e) {
            log.error("Failed to send HTTP request", e);
            messageProcessingService.markFailed(message.getMessageId(), e.getMessage());
            throw new IntegrationException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationMessage getMessageStatus(String messageId) {
        return messageProcessingService.getMessage(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found: " + messageId));
    }

    @Override
    @CircuitBreaker(name = "integration", fallbackMethod = "retryMessageFallback")
    @Retry(name = "integration")
    @Transactional
    public boolean retryMessage(String messageId) {
        return messageProcessingService.retryMessage(messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationMessage> getMessagesByStatus(MessageStatus status) {
        return messageProcessingService.getMessagesByStatus(status);
    }

    @Override
    @Transactional
    public void cancelMessage(String messageId) {
        messageProcessingService.cancelMessage(messageId);
    }

    private MessageType parseSwiftMessageType(String messageType) {
        return switch (messageType.toUpperCase()) {
            case "MT103" -> MessageType.SWIFT_MT103;
            case "MT202" -> MessageType.SWIFT_MT202;
            case "MT940" -> MessageType.SWIFT_MT940;
            default -> throw new IllegalArgumentException("Unsupported SWIFT message type: " + messageType);
        };
    }

    // ═══════════════════════════════════════════════════════
    //  Resilience Fallback Methods
    // ═══════════════════════════════════════════════════════

    private String processSwiftMessageFallback(String swiftMessage, String messageType, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for processSwiftMessage: {}", ex.getMessage());
        throw new RuntimeException("Integration service temporarily unavailable", ex);
    }

    private String generateOjkReportFallback(String reportType, LocalDate date, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for generateOjkReport: {}", ex.getMessage());
        throw new RuntimeException("Integration service temporarily unavailable", ex);
    }

    private String sendSoapRequestFallback(String endpoint, String operation, String payload, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for sendSoapRequest: {}", ex.getMessage());
        throw new RuntimeException("Integration service temporarily unavailable", ex);
    }

    private String sendHttpRequestFallback(String url, String method, Map<String, String> headers, String body, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for sendHttpRequest: {}", ex.getMessage());
        throw new RuntimeException("Integration service temporarily unavailable", ex);
    }

    private boolean retryMessageFallback(String messageId, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for retryMessage: {}", ex.getMessage());
        throw new RuntimeException("Integration service temporarily unavailable", ex);
    }

    private String extractBusinessReference(String swiftMessage) {
        // Extract transaction reference from SWIFT message
        // In production, this would parse the SWIFT message format
        if (swiftMessage != null && swiftMessage.length() > 20) {
            return "SWIFT_" + swiftMessage.substring(0, 20).replaceAll("[^a-zA-Z0-9]", "_");
        }
        return "SWIFT_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String extractHostFromEndpoint(String endpoint) {
        try {
            java.net.URL url = new java.net.URL(endpoint);
            return url.getHost();
        } catch (Exception e) {
            log.warn("Failed to extract host from endpoint: {}", endpoint, e);
            return "UNKNOWN";
        }
    }

    private String generateReportContent(String reportType, LocalDate date) {
        // In production, this would generate actual report content
        return String.format("Report Type: %s, Date: %s", reportType, date);
    }

    /**
     * Exception for integration failures.
     */
    public static class IntegrationException extends RuntimeException {
        public IntegrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception for message not found.
     */
    public static class MessageNotFoundException extends RuntimeException {
        public MessageNotFoundException(String message) {
            super(message);
        }
    }
}
