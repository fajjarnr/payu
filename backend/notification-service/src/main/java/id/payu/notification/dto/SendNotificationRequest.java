package id.payu.notification.dto;

import id.payu.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for sending notification.
 */
public record SendNotificationRequest(
        @NotBlank(message = "User ID is required") String userId,

        @NotNull(message = "Channel is required") NotificationChannel channel,

        @NotBlank(message = "Recipient is required") String recipient,

        @NotBlank(message = "Title is required") String title,

        String body,

        String templateId,

        String data // JSON
) {
}
