package id.payu.notification.dto;

import id.payu.notification.domain.Notification;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for notification.
 */
public record NotificationResponse(
        UUID id,
        String userId,
        String channel,
        String recipient,
        String title,
        String body,
        String status,
        LocalDateTime createdAt,
        LocalDateTime sentAt,
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
