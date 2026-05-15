package id.payu.notification.domain.port.out;

import id.payu.notification.domain.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for Notification persistence.
 */
public interface NotificationPersistencePort {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    List<Notification> findByUserId(String userId, int limit);

    List<Notification> findAll(int limit);

    List<Notification> findPendingScheduledBefore(LocalDateTime dateTime);
}
