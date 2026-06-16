package id.payu.integration.adapter.camel.route;

import id.payu.integration.adapter.camel.transformer.SwiftTransformer;
import id.payu.integration.adapter.camel.validator.SwiftValidator;
import id.payu.integration.application.service.IntegrationService;
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
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Camel routes for SWIFT message processing.
 * Handles inbound and outbound SWIFT MT messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SwiftRouteBuilder extends RouteBuilder {

    private final SwiftValidator swiftValidator;
    private final SwiftTransformer swiftTransformer;
    private final MessageProcessingService messageProcessingService;

    @Override
    public void configure() throws Exception {

        // Global error handler for SWIFT routes
        onException(Exception.class)
            .log(LoggingLevel.ERROR, "Error processing SWIFT message: ${exception.message}")
            .to("direct:swift-error-handler")
            .handled(true);

        // Route: SWIFT inbound from Artemis
        from("jms:queue:payu.integration.commands")
            .routeId("swift-inbound-jms-route")
            .log(LoggingLevel.INFO, "Received SWIFT message from Artemis")
            .process(exchange -> {
                String payload = exchange.getIn().getBody(String.class);
                String messageType = swiftValidator.detectMessageType(payload);
                exchange.getIn().setHeader("SwiftMessageType", messageType);
            })
            .to("direct:swift-process");

        // Route: SWIFT inbound from REST API
        from("direct:swift-inbound")
            .routeId("swift-inbound-direct-route")
            .log(LoggingLevel.INFO, "Processing SWIFT message from direct endpoint")
            .to("direct:swift-process");

        // Route: SWIFT processing pipeline
        from("direct:swift-process")
            .routeId("swift-process-route")
            .process(exchange -> {
                IntegrationMessage message = exchange.getIn().getBody(IntegrationMessage.class);
                if (message == null) {
                    // Create message from raw payload
                    String payload = exchange.getIn().getBody(String.class);
                    String messageTypeStr = exchange.getIn().getHeader("SwiftMessageType", String.class);
                    MessageType type = parseMessageType(messageTypeStr);

                    message = IntegrationMessage.builder()
                            .type(type)
                            .direction(MessageDirection.INBOUND)
                            .sourceSystem("SWIFT_NETWORK")
                            .targetSystem("PAYU_CORE")
                            .rawPayload(payload)
                            .status(MessageStatus.RECEIVED)
                            .build();
                }
                exchange.getIn().setBody(message);
            })
            .bean(messageProcessingService, "validateMessage(${body.messageId})")
            .process(exchange -> {
                IntegrationMessage message = exchange.getIn().getBody(IntegrationMessage.class);
                SwiftValidator.ValidationResult validation = swiftValidator.validate(message);
                if (!validation.valid()) {
                    throw new IntegrationService.IntegrationException(
                            "SWIFT validation failed: " + validation.getErrorMessage(), null);
                }
            })
            .bean(messageProcessingService, "transformMessage(${body.messageId}, ${body.rawPayload})")
            .process(exchange -> {
                IntegrationMessage message = exchange.getIn().getBody(IntegrationMessage.class);
                String transformed = swiftTransformer.toInternalFormat(message);
                message.setTransformedPayload(transformed);
                exchange.getIn().setBody(message);
            })
            .marshal().json(JsonLibrary.Jackson)
            .to(String.format("kafka:payu.integration.swift-processed.v1?brokers=%s",
                    System.getenv().getOrDefault("KAFKA_BOOTSTRAP", "localhost:9092")))
            .process(exchange -> {
                IntegrationMessage message = exchange.getIn().getBody(IntegrationMessage.class);
                messageProcessingService.markSent(message.getMessageId());
            })
            .log(LoggingLevel.INFO, "SWIFT message processed successfully: ${body.messageId}");

        // Route: SWIFT outbound to network
        from("direct:swift-outbound")
            .routeId("swift-outbound-route")
            .log(LoggingLevel.INFO, "Sending SWIFT message to network")
            .process(exchange -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> internalData = exchange.getIn().getBody(Map.class);
                String swiftMessage = swiftTransformer.fromInternalFormat(internalData);
                exchange.getIn().setBody(swiftMessage);
                exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "text/plain");
            })
            .toD("${exchangeProperty.swiftEndpoint:https://swift-gateway.payu.fajjjar.my.id/api/v1/messages}")
            .log(LoggingLevel.INFO, "SWIFT message sent successfully");

        // Route: Error handler
        from("direct:swift-error-handler")
            .routeId("swift-error-handler-route")
            .process(exchange -> {
                Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                String messageId = exchange.getIn().getHeader("MessageId", String.class);
                if (messageId != null) {
                    messageProcessingService.markFailed(messageId, exception.getMessage());
                }
                log.error("SWIFT processing error for message {}: {}", messageId, exception.getMessage());
            })
            .to(String.format("kafka:payu.integration.swift-errors.v1?brokers=%s",
                    System.getenv().getOrDefault("KAFKA_BOOTSTRAP", "localhost:9092")));
    }

    private MessageType parseMessageType(String messageTypeStr) {
        if (messageTypeStr == null) return MessageType.SWIFT_MT103;
        return switch (messageTypeStr.toUpperCase()) {
            case "MT103" -> MessageType.SWIFT_MT103;
            case "MT202" -> MessageType.SWIFT_MT202;
            case "MT940" -> MessageType.SWIFT_MT940;
            default -> MessageType.SWIFT_MT103;
        };
    }
}
