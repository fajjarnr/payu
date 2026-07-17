package id.payu.notification.adapter.persistence;

import id.payu.notification.adapter.persistence.entity.NotificationEntity;
import id.payu.notification.domain.Notification;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper between Notification domain model and NotificationEntity persistence model.
 */
@ApplicationScoped
public class NotificationMapper {

    public Notification toDomain(NotificationEntity entity) {
        if (entity == null) {
            return null;
        }

        Notification domain = new Notification();
        domain.setId(entity.id);
        domain.setUserId(entity.userId);
        domain.setChannel(entity.channel);
        domain.setRecipient(entity.recipient);
        domain.setTitle(entity.title);
        domain.setBody(entity.body);
        domain.setTemplateId(entity.templateId);
        domain.setData(entity.data);
        domain.setStatus(entity.status);
        domain.setFailureReason(entity.failureReason);
        domain.setRetryCount(entity.retryCount);
        domain.setCreatedAt(entity.createdAt);
        domain.setSentAt(entity.sentAt);
        domain.setReadAt(entity.readAt);
        domain.setScheduledAt(entity.scheduledAt);
        domain.setIdempotencyKey(entity.idempotencyKey);

        return domain;
    }

    public NotificationEntity toEntity(Notification domain) {
        if (domain == null) {
            return null;
        }

        NotificationEntity entity = new NotificationEntity();
        updateEntity(domain, entity);
        return entity;
    }

    public void updateEntity(Notification domain, NotificationEntity entity) {
        if (domain == null || entity == null) {
            return;
        }

        if (domain.getId() != null) {
            entity.id = domain.getId();
        }
        entity.userId = domain.getUserId();
        entity.channel = domain.getChannel();
        entity.recipient = domain.getRecipient();
        entity.title = domain.getTitle();
        entity.body = domain.getBody();
        entity.templateId = domain.getTemplateId();
        entity.data = domain.getData();
        entity.status = domain.getStatus();
        entity.failureReason = domain.getFailureReason();
        entity.retryCount = domain.getRetryCount();
        if (domain.getCreatedAt() != null) {
            entity.createdAt = domain.getCreatedAt();
        }
        entity.sentAt = domain.getSentAt();
        entity.readAt = domain.getReadAt();
        entity.scheduledAt = domain.getScheduledAt();
        entity.idempotencyKey = domain.getIdempotencyKey();
    }
}
