package id.payu.integration.interfaces.dto;

import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageDirection;
import id.payu.integration.domain.model.MessageStatus;
import id.payu.integration.domain.model.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for IntegrationMessage response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Integration message response")
public class IntegrationMessageResponse {

    @Schema(description = "Message ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String messageId;

    @Schema(description = "Message type", example = "SWIFT_MT103")
    private MessageType type;

    @Schema(description = "Message direction", example = "INBOUND")
    private MessageDirection direction;

    @Schema(description = "Source system", example = "SWIFT_NETWORK")
    private String sourceSystem;

    @Schema(description = "Target system", example = "PAYU_CORE")
    private String targetSystem;

    @Schema(description = "Correlation ID for tracking", example = "550e8400-e29b-41d4-a716-446655440001")
    private String correlationId;

    @Schema(description = "Business reference", example = "TXN-2024-001")
    private String businessReference;

    @Schema(description = "Processing status", example = "SENT")
    private MessageStatus status;

    @Schema(description = "Error message if failed")
    private String errorMessage;

    @Schema(description = "Retry count", example = "0")
    private Integer retryCount;

    @Schema(description = "Maximum retries allowed", example = "3")
    private Integer maxRetries;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Processing completion timestamp")
    private LocalDateTime processedAt;

    @Schema(description = "Last retry timestamp")
    private LocalDateTime lastRetryAt;

    /**
     * Convert domain entity to response DTO.
     */
    public static IntegrationMessageResponse from(IntegrationMessage message) {
        return IntegrationMessageResponse.builder()
                .messageId(message.getMessageId())
                .type(message.getType())
                .direction(message.getDirection())
                .sourceSystem(message.getSourceSystem())
                .targetSystem(message.getTargetSystem())
                .correlationId(message.getCorrelationId())
                .businessReference(message.getBusinessReference())
                .status(message.getStatus())
                .errorMessage(message.getErrorMessage())
                .retryCount(message.getRetryCount())
                .maxRetries(message.getMaxRetries())
                .createdAt(message.getCreatedAt())
                .processedAt(message.getProcessedAt())
                .lastRetryAt(message.getLastRetryAt())
                .build();
    }
}
