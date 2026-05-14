package id.payu.integration.adapter.camel.route;

import id.payu.integration.adapter.camel.transformer.SoapTransformer;
import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageDirection;
import id.payu.integration.domain.model.MessageStatus;
import id.payu.integration.domain.model.MessageType;
import id.payu.integration.domain.service.MessageProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Camel routes for SOAP Web Service integration.
 * Handles SOAP requests to legacy systems.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SoapRouteBuilder extends RouteBuilder {

    private final SoapTransformer soapTransformer;
    private final MessageProcessingService messageProcessingService;

    @Override
    public void configure() throws Exception {

        // Error handler
        onException(Exception.class)
            .log(LoggingLevel.ERROR, "Error processing SOAP request: ${exception.message}")
            .to("direct:soap-error-handler")
            .handled(true);

        // Route: SOAP request handler
        from("direct:soap-request")
            .routeId("soap-request-route")
            .log(LoggingLevel.INFO, "Processing SOAP request to: ${header.SoapEndpoint}")
            .process(exchange -> {
                String endpoint = exchange.getIn().getHeader("SoapEndpoint", String.class);
                String operation = exchange.getIn().getHeader("SoapOperation", String.class);
                String payload = exchange.getIn().getBody(String.class);
                String messageId = exchange.getIn().getHeader("MessageId", String.class);

                // Create message record if not exists
                if (messageId == null) {
                    IntegrationMessage message = IntegrationMessage.builder()
                            .messageId(UUID.randomUUID().toString())
                            .type(MessageType.SOAP)
                            .direction(MessageDirection.OUTBOUND)
                            .sourceSystem("PAYU_CORE")
                            .targetSystem(extractHost(endpoint))
                            .rawPayload(payload)
                            .businessReference(operation)
                            .status(MessageStatus.SENDING)
                            .build();

                    messageProcessingService.createMessage(
                            MessageType.SOAP,
                            MessageDirection.OUTBOUND,
                            "PAYU_CORE",
                            extractHost(endpoint),
                            payload,
                            null,
                            operation
                    );

                    exchange.getIn().setHeader("MessageId", message.getMessageId());
                }

                // Wrap payload in SOAP envelope if needed
                if (!payload.trim().startsWith("<soap")) {
                    String soapMessage = soapTransformer.wrapSoap11(payload, operation);
                    exchange.getIn().setBody(soapMessage);
                }

                exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "text/xml;charset=UTF-8");
                exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
                exchange.getIn().setHeader(Exchange.HTTP_URI, endpoint);
            })
            .toD("${header.SoapEndpoint}?throwExceptionOnFailure=true")
            .log(LoggingLevel.INFO, "SOAP response received")
            .process(exchange -> {
                String response = exchange.getIn().getBody(String.class);
                String messageId = exchange.getIn().getHeader("MessageId", String.class);

                // Check for SOAP Fault
                if (soapTransformer.hasFault(response)) {
                    Map<String, String> fault = soapTransformer.extractFault(response);
                    log.error("SOAP Fault received: {}", fault);

                    if (messageId != null) {
                        messageProcessingService.markFailed(messageId,
                                "SOAP Fault: " + fault.getOrDefault("message", "Unknown error"));
                    }

                    throw new SoapIntegrationException("SOAP Fault: " + fault.getOrDefault("message", "Unknown error"));
                }

                // Unwrap SOAP envelope
                String unwrappedResponse = soapTransformer.unwrapSoap(response);
                exchange.getIn().setBody(unwrappedResponse);

                // Mark message as sent
                if (messageId != null) {
                    messageProcessingService.markSent(messageId);
                }
            });

        // Route: SOAP response handler for async callbacks
        from("direct:soap-response")
            .routeId("soap-response-route")
            .log(LoggingLevel.DEBUG, "Processing SOAP response")
            .process(exchange -> {
                String response = exchange.getIn().getBody(String.class);

                if (soapTransformer.hasFault(response)) {
                    Map<String, String> fault = soapTransformer.extractFault(response);
                    log.error("SOAP Fault in response: {}", fault);
                    exchange.getIn().setHeader("HasSoapFault", true);
                    exchange.getIn().setHeader("SoapFaultCode", fault.get("code"));
                    exchange.getIn().setHeader("SoapFaultMessage", fault.get("message"));
                } else {
                    String unwrapped = soapTransformer.unwrapSoap(response);
                    exchange.getIn().setBody(unwrapped);
                    exchange.getIn().setHeader("HasSoapFault", false);
                }
            });

        // Route: Error handler
        from("direct:soap-error-handler")
            .routeId("soap-error-handler-route")
            .process(exchange -> {
                Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                String messageId = exchange.getIn().getHeader("MessageId", String.class);

                log.error("SOAP request failed: {}", exception.getMessage(), exception);

                if (messageId != null) {
                    messageProcessingService.markFailed(messageId, exception.getMessage());
                }

                // Prepare error response
                exchange.getIn().setBody(Map.of(
                        "error", "SOAP_REQUEST_FAILED",
                        "message", exception.getMessage(),
                        "timestamp", java.time.Instant.now().toString()
                ));
            })
            .marshal().json();

        // Route: HTTP request handler (generic)
        from("direct:http-request")
            .routeId("http-request-route")
            .log(LoggingLevel.INFO, "Processing HTTP ${header.HttpMethod} request to: ${header.HttpUrl}")
            .process(exchange -> {
                String url = exchange.getIn().getHeader("HttpUrl", String.class);
                String method = exchange.getIn().getHeader("HttpMethod", String.class);
                @SuppressWarnings("unchecked")
                Map<String, String> headers = exchange.getIn().getHeader("HttpHeaders", Map.class);

                exchange.getIn().setHeader(Exchange.HTTP_URI, url);
                exchange.getIn().setHeader(Exchange.HTTP_METHOD, method);

                if (headers != null) {
                    headers.forEach((key, value) -> exchange.getIn().setHeader(key, value));
                }
            })
            .toD("${header.HttpUrl}?throwExceptionOnFailure=true")
            .log(LoggingLevel.INFO, "HTTP response received with status: ${header.CamelHttpResponseCode}");
    }

    private String extractHost(String endpoint) {
        try {
            java.net.URL url = new java.net.URL(endpoint);
            return url.getHost();
        } catch (Exception e) {
            log.warn("Failed to extract host from endpoint: {}", endpoint, e);
            return "UNKNOWN";
        }
    }

    /**
     * Exception for SOAP integration errors.
     */
    public static class SoapIntegrationException extends RuntimeException {
        public SoapIntegrationException(String message) {
            super(message);
        }
    }
}
