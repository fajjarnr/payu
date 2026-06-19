package id.payu.notification.adapter.persistence.entity;

import id.payu.notification.domain.NotificationChannel;
import id.payu.notification.domain.NotificationStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * NotificationEntity entity using Panache.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "userId"),
        @Index(name = "idx_notification_status", columnList = "status")
})
public class NotificationEntity extends PanacheEntityBase {

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

    /** BUG-BE-025: Scheduled retry time for failed notifications */
    public LocalDateTime scheduledAt;

    /** IDEM-003: Client-supplied idempotency key to prevent duplicate sends */
    @Column(length = 255)
    public String idempotencyKey;
    @Version
    private Long version;


    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
    }
}
