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

    String processSwiftMessage(String swiftMessage, String messageType);

    String generateOjkReport(String reportType, LocalDate date);

    String sendSoapRequest(String endpoint, String operation, String payload);

    String sendHttpRequest(String url, String method, java.util.Map<String, String> headers, String body);

    IntegrationMessage getMessageStatus(String messageId);

    boolean retryMessage(String messageId);

    List<IntegrationMessage> getMessagesByStatus(MessageStatus status);

    void cancelMessage(String messageId);
}
