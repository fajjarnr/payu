package id.payu.notification.dto;

import id.payu.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request DTO for sending notification.
 */
@Schema(description = "Request to send a notification through a specific channel")
public record SendNotificationRequest(
        @Schema(
            description = "User ID to send the notification to",
            required = true,
            example = "user-123",
            minLength = 1
        )
        @NotBlank(message = "User ID is required") String userId,

        @Schema(
            description = "NotificationEntity channel to use for delivery",
            required = true,
            example = "PUSH",
            enumeration = { "PUSH", "SMS", "EMAIL", "IN_APP" }
        )
        @NotNull(message = "Channel is required") NotificationChannel channel,

        @Schema(
            description = "Recipient address depending on channel: device token (PUSH), phone number (SMS), email address (EMAIL), user ID (IN_APP)",
            required = true,
            example = "+6281234567890",
            minLength = 1
        )
        @NotBlank(message = "Recipient is required") String recipient,

        @Schema(
            description = "NotificationEntity title or subject line",
            required = true,
            example = "Transfer Successful",
            minLength = 1,
            maxLength = 200
        )
        @NotBlank(message = "Title is required") String title,

        @Schema(
            description = "NotificationEntity body content. For templated notifications, this is optional as the template defines the body.",
            example = "Your transfer of Rp 100.000 was successful.",
            maxLength = 2000
        )
        String body,

        @Schema(
            description = "Optional template ID for pre-configured notification templates. If provided, 'data' must contain template variables.",
            example = "monthly-statement"
        )
        String templateId,

        @Schema(
            description = "JSON string containing template variables or additional metadata for the notification. Used with templateId for dynamic content.",
            example = "{\"transactionId\": \"tx-456\", \"amount\": 100000, \"currency\": \"IDR\"}",
            format = "json"
        )
        String data // JSON
) {
}
