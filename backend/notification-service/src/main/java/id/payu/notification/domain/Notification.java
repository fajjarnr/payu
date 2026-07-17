package id.payu.notification.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pure Domain Model for Notification.
 * Independent of JPA / Panache or persistence details.
 */
public class Notification {

    private UUID id;
    private String userId;
    private NotificationChannel channel;
    private String recipient;
    private String title;
    private String body;
    private String templateId;
    private String data;
    private NotificationStatus status;
    private String failureReason;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime scheduledAt;
    private String idempotencyKey;

    public Notification() {
    }

    public Notification(UUID id, String userId, NotificationChannel channel, String recipient,
                        String title, String body, String templateId, String data,
                        NotificationStatus status, String failureReason, int retryCount,
                        LocalDateTime createdAt, LocalDateTime sentAt, LocalDateTime readAt,
                        LocalDateTime scheduledAt, String idempotencyKey) {
        this.id = id;
        this.userId = userId;
        this.channel = channel;
        this.recipient = recipient;
        this.title = title;
        this.body = body;
        this.templateId = templateId;
        this.data = data;
        this.status = status;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
        this.readAt = readAt;
        this.scheduledAt = scheduledAt;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
