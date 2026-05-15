package id.payu.notification.application.service;

import id.payu.notification.adapter.persistence.entity.NotificationEntity;
import id.payu.notification.domain.NotificationChannel;
import id.payu.notification.dto.SendNotificationRequest;
import id.payu.notification.adapter.sender.EmailSender;
import id.payu.notification.adapter.sender.PushSender;
import id.payu.notification.adapter.sender.SmsSender;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.notification.domain.NotificationStatus;

/**
 * Service for managing notifications.
 */
@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Inject
    EmailSender emailSender;

    @Inject
    PushSender pushSender;

    @Inject
    SmsSender smsSender;

    @Transactional
    public NotificationEntity send(SendNotificationRequest request) {
        LOG.infof("Sending notification: channel=%s, recipient=%s",
                request.channel(), request.recipient());

        NotificationEntity notification = new NotificationEntity();
        notification.userId = request.userId();
        notification.channel = request.channel();
        notification.recipient = request.recipient();
        notification.title = request.title();
        notification.body = request.body();
        notification.templateId = request.templateId();
        notification.data = request.data();
        notification.status = NotificationStatus.PENDING;

        notification.persist();

        // Send based on channel
        try {
            notification.status = NotificationStatus.SENDING;

            boolean success = switch (request.channel()) {
                case EMAIL -> emailSender.send(notification);
                case SMS -> smsSender.send(notification);
                case PUSH, IN_APP -> pushSender.send(notification);
            };

            if (success) {
                notification.status = NotificationStatus.SENT;
                notification.sentAt = LocalDateTime.now();
                LOG.infof("NotificationEntity sent: id=%s", notification.id);
            } else {
                handleFailedNotification(notification, "Send failed");
            }
        } catch (Exception e) {
            handleFailedNotification(notification, e.getMessage());
        }

        notification.persist();
        return notification;
    }

    public Optional<NotificationEntity> getById(UUID id) {
        return NotificationEntity.findByIdOptional(id);
    }

    public List<NotificationEntity> getByUserId(String userId, int limit) {
        return NotificationEntity.find("userId = ?1 ORDER BY createdAt DESC", userId)
                .page(0, limit)
                .list();
    }

    public List<NotificationEntity> getAllNotifications(int limit) {
        return NotificationEntity.find("ORDER BY createdAt DESC")
                .page(0, Math.min(limit, 100))
                .list();
    }

    @Transactional
    public void markAsRead(UUID id) {
        NotificationEntity.<NotificationEntity>findByIdOptional(id).ifPresent(n -> {
            n.status = NotificationStatus.READ;
            n.readAt = LocalDateTime.now();
            n.persist();
        });
    }

    /**
     * BUG-BE-025 fix: Scheduled job to retry sending pending notifications
     */
    @io.quarkus.scheduler.Scheduled(every = "1m")
    @Transactional
    public void retryPendingNotifications() {
        List<NotificationEntity> pendingNotifications = NotificationEntity.find("status = ?1 and scheduledAt <= ?2",
                NotificationStatus.PENDING, LocalDateTime.now()).list();

        for (NotificationEntity notification : pendingNotifications) {
            LOG.infof("Retrying notification %s (attempt %d of %d)",
                    notification.id, notification.retryCount + 1, MAX_RETRY_ATTEMPTS);

            try {
                notification.status = NotificationStatus.SENDING;
                boolean success = switch (notification.channel) {
                    case EMAIL -> emailSender.send(notification);
                    case SMS -> smsSender.send(notification);
                    case PUSH, IN_APP -> pushSender.send(notification);
                };

                if (success) {
                    notification.status = NotificationStatus.SENT;
                    notification.sentAt = LocalDateTime.now();
                    LOG.infof("NotificationEntity retry successful: id=%s", notification.id);
                } else {
                    handleFailedNotification(notification, "Send failed");
                }
            } catch (Exception e) {
                handleFailedNotification(notification, e.getMessage());
            }

            notification.persist();
        }
    }

    private void handleFailedNotification(NotificationEntity notification, String errorReason) {
        LOG.errorf("Failed to send notification: %s", errorReason);
        notification.status = NotificationStatus.FAILED;
        notification.failureReason = errorReason;
        notification.retryCount++;

        if (notification.retryCount < MAX_RETRY_ATTEMPTS) {
            notification.status = NotificationStatus.PENDING;
            notification.scheduledAt = LocalDateTime.now().plusMinutes(
                    (long) Math.pow(2, notification.retryCount) // Exponential backoff: 2, 4, 8 min
            );
            LOG.infof("NotificationEntity %s scheduled for retry %d at %s",
                    notification.id, notification.retryCount, notification.scheduledAt);
        }
    }

    /**
     * Send transaction notification.
     */
    @Transactional
    public void sendTransactionNotification(String userId, String email, String title, String body) {
        SendNotificationRequest request = new SendNotificationRequest(
                userId,
                NotificationChannel.EMAIL,
                email,
                title,
                body,
                "transaction",
                null);
        send(request);
    }
}
