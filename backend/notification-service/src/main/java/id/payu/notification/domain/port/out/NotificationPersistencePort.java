package id.payu.notification.domain.port.out;

import id.payu.notification.adapter.persistence.entity.NotificationEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for NotificationEntity persistence.
 */
public interface NotificationPersistencePort {

    NotificationEntity save(NotificationEntity notification);

    Optional<NotificationEntity> findById(UUID id);

    List<NotificationEntity> findByUserId(String userId, int limit);

    List<NotificationEntity> findAll(int limit);

    List<NotificationEntity> findPendingScheduledBefore(LocalDateTime dateTime);
}
