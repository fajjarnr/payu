package id.payu.notification.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification entity using Panache.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "userId"),
        @Index(name = "idx_notification_status", columnList = "status")
})
public class Notification extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false)
    public String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public NotificationChannel channel;

    @Column(nullable = false)
    public String recipient; // Email, phone number, or device token

    @Column(nullable = false)
    public String title;

    @Column(length = 2000)
    public String body;

    public String templateId;

    @Column(columnDefinition = "TEXT")
    public String data; // JSON data for template

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public NotificationStatus status;

    public String failureReason;

    public int retryCount;

    @Column(updatable = false)
    public LocalDateTime createdAt;

    public LocalDateTime sentAt;

    public LocalDateTime readAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
    }

    public enum NotificationStatus {
        PENDING,
        SENDING,
        SENT,
        DELIVERED,
        READ,
        FAILED
    }
}
