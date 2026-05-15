package id.payu.partner.adapter.persistence.entity;

import id.payu.partner.domain.*;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Outbound webhook subscription for partner event notifications.
 * Partners register URLs to receive real-time event notifications
 * (e.g., payment.completed, payment.failed) via HTTP POST with HMAC signature.
 */
@Entity
@Table(name = "webhook_subscriptions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"partner_id", "url"}))
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class WebhookSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerEntity partner;

    @NotBlank
    @Column(nullable = false, length = 2048)
    private String url;

    /**
     * Comma-separated event types this subscription listens to.
     * Examples: "payment.completed,payment.failed,payment.refunded"
     * Use "*" to subscribe to all events.
     */
    @NotBlank
    @Column(nullable = false, length = 1024)
    private String events;

    /**
     * HMAC-SHA256 secret for signing webhook payloads.
     * Generated at registration time, shared with partner.
     */
    @NotBlank
    @Column(nullable = false, length = 512)
    private String secret;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Maximum number of delivery attempts before giving up.
     */
    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 5;

    /**
     * Tenant identifier for multi-tenancy isolation.
     * Auto-populated by TenantEntityListener.
     */
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WebhookSubscriptionEntity() {}

    public WebhookSubscriptionEntity(PartnerEntity partner, String url, String events, String secret) {
        this.partner = partner;
        this.url = url;
        this.events = events;
        this.secret = secret;
        this.active = true;
        this.maxRetries = 5;
    }

    // --- Domain Methods ---

    /**
     * Check if this subscription should receive the given event type.
     */
    public boolean matchesEvent(String eventType) {
        if (!active) return false;
        if ("*".equals(events)) return true;
        for (String event : events.split(",")) {
            if (event.trim().equalsIgnoreCase(eventType)) return true;
        }
        return false;
    }

    // --- Getters/Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PartnerEntity getPartner() { return partner; }
    public void setPartner(PartnerEntity partner) { this.partner = partner; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getEvents() { return events; }
    public void setEvents(String events) { this.events = events; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
