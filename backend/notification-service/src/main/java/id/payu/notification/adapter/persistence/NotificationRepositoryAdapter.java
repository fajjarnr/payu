package id.payu.notification.adapter.persistence;

import id.payu.notification.adapter.persistence.entity.NotificationEntity;
import id.payu.notification.domain.Notification;
import id.payu.notification.domain.NotificationStatus;
import id.payu.notification.domain.port.out.NotificationRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing NotificationRepositoryPort using Panache entities.
 */
@ApplicationScoped
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    @Inject
    NotificationMapper mapper;

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity;
        if (notification.getId() != null) {
            Optional<NotificationEntity> existingOpt = NotificationEntity.findByIdOptional(notification.getId());
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
                mapper.updateEntity(notification, entity);
            } else {
                entity = mapper.toEntity(notification);
            }
        } else {
            entity = mapper.toEntity(notification);
        }

        entity.persist();
        notification.setId(entity.id);
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(entity.createdAt);
        }
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return NotificationEntity.<NotificationEntity>findByIdOptional(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Notification> findByUserId(String userId, int limit) {
        return NotificationEntity.<NotificationEntity>find("userId = ?1 ORDER BY createdAt DESC", userId)
                .page(0, limit)
                .list()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Notification> findAll(int limit) {
        return NotificationEntity.<NotificationEntity>find("ORDER BY createdAt DESC")
                .page(0, Math.min(limit, 100))
                .list()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(String idempotencyKey) {
        return NotificationEntity.<NotificationEntity>find("idempotencyKey = ?1", idempotencyKey)
                .firstResultOptional()
                .map(mapper::toDomain);
    }

    @Override
    public List<Notification> findPendingNotificationsToRetry(LocalDateTime now) {
        return NotificationEntity.<NotificationEntity>find("status = ?1 and scheduledAt <= ?2",
                        NotificationStatus.PENDING, now)
                .list()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
