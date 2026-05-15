package id.payu.notification.domain.port.in;

import id.payu.notification.domain.Notification;
import id.payu.notification.domain.NotificationChannel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port for Notification use cases.
 */
public interface NotificationUseCase {

    Notification send(String userId, NotificationChannel channel, String recipient,
                      String title, String body, String templateId, String data);

    Optional<Notification> getById(UUID id);

    List<Notification> getByUserId(String userId, int limit);

    List<Notification> getAllNotifications(int limit);

    void markAsRead(UUID id);

    void sendTransactionNotification(String userId, String email, String title, String body);
}
