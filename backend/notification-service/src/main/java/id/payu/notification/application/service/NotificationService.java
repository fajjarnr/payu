package id.payu.notification.application.service;

import id.payu.notification.adapter.sender.EmailSender;
import id.payu.notification.adapter.sender.PushSender;
import id.payu.notification.adapter.sender.SmsSender;
import id.payu.notification.domain.Notification;
import id.payu.notification.domain.NotificationChannel;
import id.payu.notification.domain.NotificationStatus;
import id.payu.notification.domain.port.in.NotificationUseCase;
import id.payu.notification.domain.port.out.NotificationRepositoryPort;
import id.payu.notification.dto.SendNotificationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing notifications using pure domain model and repository port.
 */
@ApplicationScoped
public class NotificationService implements NotificationUseCase {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Inject
    NotificationRepositoryPort repositoryPort;

    @Inject
    EmailSender emailSender;

    @Inject
    PushSender pushSender;

    @Inject
    SmsSender smsSender;

    @Override
    public Notification send(String userId, NotificationChannel channel, String recipient,
                             String title, String body, String templateId, String data) {
        SendNotificationRequest request = new SendNotificationRequest(
                userId, channel, recipient, title, body, templateId, data, null);
        return send(request, null);
    }

    @Transactional
    public Notification send(SendNotificationRequest request, String idempotencyKey) {
        LOG.infof("Sending notification: channel=%s, recipient=%s",
                request.channel(), request.recipient());

        // IDEM-003: Check for existing notification with same idempotency key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Notification> existing = repositoryPort.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                LOG.infof("Idempotent request detected: key=%s, returning existing notification id=%s",
                        idempotencyKey, existing.get().getId());
                return existing.get();
            }
        }

        Notification notification = new Notification();
        notification.setUserId(request.userId());
        notification.setChannel(request.channel());
        notification.setRecipient(request.recipient());
        notification.setTitle(request.title());
        notification.setBody(request.body());
        notification.setTemplateId(request.templateId());
        notification.setData(request.data());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setIdempotencyKey(idempotencyKey);

        notification = repositoryPort.save(notification);

        // Send based on channel
        try {
            notification.setStatus(NotificationStatus.SENDING);

            boolean success = switch (request.channel()) {
                case EMAIL -> emailSender.send(notification);
                case SMS -> smsSender.send(notification);
                case PUSH, IN_APP -> pushSender.send(notification);
            };

            if (success) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                LOG.infof("Notification sent: id=%s", notification.getId());
            } else {
                handleFailedNotification(notification, "Send failed");
            }
        } catch (Exception e) {
            handleFailedNotification(notification, e.getMessage());
        }

        return repositoryPort.save(notification);
    }

    @Override
    public Optional<Notification> getById(UUID id) {
        return repositoryPort.findById(id);
    }

    @Override
    public List<Notification> getByUserId(String userId, int limit) {
        return repositoryPort.findByUserId(userId, limit);
    }

    @Override
    public List<Notification> getAllNotifications(int limit) {
        return repositoryPort.findAll(limit);
    }

    @Override
    @Transactional
    public void markAsRead(UUID id) {
        repositoryPort.findById(id).ifPresent(n -> {
            n.setStatus(NotificationStatus.READ);
            n.setReadAt(LocalDateTime.now());
            repositoryPort.save(n);
        });
    }

    /**
     * BUG-BE-025 fix: Scheduled job to retry sending pending notifications
     */
    @io.quarkus.scheduler.Scheduled(every = "1m")
    @Transactional
    public void retryPendingNotifications() {
        List<Notification> pendingNotifications = repositoryPort.findPendingNotificationsToRetry(LocalDateTime.now());

        for (Notification notification : pendingNotifications) {
            LOG.infof("Retrying notification %s (attempt %d of %d)",
                    notification.getId(), notification.getRetryCount() + 1, MAX_RETRY_ATTEMPTS);

            try {
                notification.setStatus(NotificationStatus.SENDING);
                boolean success = switch (notification.getChannel()) {
                    case EMAIL -> emailSender.send(notification);
                    case SMS -> smsSender.send(notification);
                    case PUSH, IN_APP -> pushSender.send(notification);
                };

                if (success) {
                    notification.setStatus(NotificationStatus.SENT);
                    notification.setSentAt(LocalDateTime.now());
                    LOG.infof("Notification retry successful: id=%s", notification.getId());
                } else {
                    handleFailedNotification(notification, "Send failed");
                }
            } catch (Exception e) {
                handleFailedNotification(notification, e.getMessage());
            }

            repositoryPort.save(notification);
        }
    }

    private void handleFailedNotification(Notification notification, String errorReason) {
        LOG.errorf("Failed to send notification: %s", errorReason);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setFailureReason(errorReason);
        notification.setRetryCount(notification.getRetryCount() + 1);

        if (notification.getRetryCount() < MAX_RETRY_ATTEMPTS) {
            notification.setStatus(NotificationStatus.PENDING);
            notification.setScheduledAt(LocalDateTime.now().plusMinutes(
                    (long) Math.pow(2, notification.getRetryCount()) // Exponential backoff: 2, 4, 8 min
            ));
            LOG.infof("Notification %s scheduled for retry %d at %s",
                    notification.getId(), notification.getRetryCount(), notification.getScheduledAt());
        }
    }

    @Override
    @Transactional
    public void sendTransactionNotification(String userId, String email, String title, String body) {
        SendNotificationRequest request = new SendNotificationRequest(
                userId,
                NotificationChannel.EMAIL,
                email,
                title,
                body,
                "transaction",
                null,
                null);
        send(request, null);
    }
}
