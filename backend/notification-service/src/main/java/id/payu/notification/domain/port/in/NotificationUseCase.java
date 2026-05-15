package id.payu.notification.domain.port.in;

import id.payu.notification.adapter.persistence.entity.NotificationEntity;
import id.payu.notification.domain.NotificationChannel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port for NotificationEntity use cases.
 */
public interface NotificationUseCase {

    NotificationEntity send(String userId, NotificationChannel channel, String recipient,
                      String title, String body, String templateId, String data);

    Optional<NotificationEntity> getById(UUID id);

    List<NotificationEntity> getByUserId(String userId, int limit);

    List<NotificationEntity> getAllNotifications(int limit);

    void markAsRead(UUID id);

    void sendTransactionNotification(String userId, String email, String title, String body);
}
