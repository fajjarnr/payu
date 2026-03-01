package id.payu.integration.application.port.in;

import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Primary port (input) for integration operations.
 * Defines the use cases supported by the integration layer.
 */
public interface IntegrationUseCase {

    /**
     * Process an incoming SWIFT message.
     *
     * @param swiftMessage Raw SWIFT MT message
     * @param messageType SWIFT message type (MT103, MT202, etc.)
     * @return Message ID for tracking
     */
    String processSwiftMessage(String swiftMessage, String messageType);

    /**
     * Generate OJK regulatory report.
     *
     * @param reportType Type of report (DAILY, MONTHLY, etc.)
     * @param date Report date
     * @return Message ID for tracking
     */
    String generateOjkReport(String reportType, LocalDate date);

    /**
     * Send SOAP request to legacy system.
     *
     * @param endpoint SOAP endpoint URL
     * @param operation SOAP operation name
     * @param payload SOAP payload
     * @return Response message
     */
    String sendSoapRequest(String endpoint, String operation, String payload);

    /**
     * Send HTTP request to external system.
     *
     * @param url Target URL
     * @param method HTTP method
     * @param headers HTTP headers
     * @param body Request body
     * @return Response body
     */
    String sendHttpRequest(String url, String method, java.util.Map<String, String> headers, String body);

    /**
     * Get message processing status.
     *
     * @param messageId Message ID
     * @return Integration message with status
     */
    IntegrationMessage getMessageStatus(String messageId);

    /**
     * Retry a failed message.
     *
     * @param messageId Message ID
     * @return true if retry was queued
     */
    boolean retryMessage(String messageId);

    /**
     * Get messages by status.
     *
     * @param status Message status
     * @return List of messages
     */
    List<IntegrationMessage> getMessagesByStatus(MessageStatus status);

    /**
     * Cancel a pending message.
     *
     * @param messageId Message ID
     */
    void cancelMessage(String messageId);
}
