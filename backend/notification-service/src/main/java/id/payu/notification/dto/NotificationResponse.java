package id.payu.notification.dto;

import id.payu.notification.domain.Notification;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for notification.
 */
@Schema(description = "Notification details including delivery status and timestamps")
public record NotificationResponse(
        @Schema(
            description = "Unique notification identifier",
            example = "123e4567-e89b-12d3-a456-426614174000"
        )
        UUID id,

        @Schema(
            description = "User ID who received the notification",
            example = "user-123"
        )
        String userId,

        @Schema(
            description = "Channel used for delivery",
            example = "PUSH",
            enumeration = { "PUSH", "SMS", "EMAIL", "IN_APP" }
        )
        String channel,

        @Schema(
            description = "Recipient address (masked for sensitive channels)",
            example = "+628****567890"
        )
        String recipient,

        @Schema(
            description = "Notification title or subject",
            example = "Transfer Successful"
        )
        String title,

        @Schema(
            description = "Notification body content",
            example = "Your transfer of Rp 100.000 was successful."
        )
        String body,

        @Schema(
            description = "Current delivery status",
            example = "DELIVERED",
            enumeration = { "PENDING", "SENDING", "SENT", "DELIVERED", "READ", "FAILED" }
        )
        String status,

        @Schema(
            description = "Timestamp when notification was created",
            example = "2026-01-31T10:30:00"
        )
        LocalDateTime createdAt,

        @Schema(
            description = "Timestamp when notification was sent to provider",
            example = "2026-01-31T10:30:05"
        )
        LocalDateTime sentAt,

        @Schema(
            description = "Timestamp when notification was read (in-app only)",
            example = "2026-01-31T10:35:00"
        )
        LocalDateTime readAt) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.id,
                n.userId,
                n.channel.name(),
                n.recipient,
                n.title,
                n.body,
                n.status.name(),
                n.createdAt,
                n.sentAt,
                n.readAt);
    }
}
