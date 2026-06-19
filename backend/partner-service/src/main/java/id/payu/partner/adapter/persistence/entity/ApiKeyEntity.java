package id.payu.partner.adapter.persistence.entity;

import id.payu.partner.domain.*;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a managed API key for partner authentication.
 * <p>
 * Keys are stored as SHA-256 hashes — the plain-text key is only returned once
 * at creation or rotation. Supports rotation with configurable grace period
 * and explicit revocation.
 */
@Entity
@Table(name = "api_keys",
       indexes = {
           @Index(name = "idx_api_key_hash", columnList = "key_hash", unique = true),
           @Index(name = "idx_api_key_partner", columnList = "partner_id"),
           @Index(name = "idx_api_key_prefix", columnList = "key_prefix"),
           @Index(name = "idx_api_key_status", columnList = "status")
       })
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class ApiKeyEntity {

    /**
     * Sandbox mode flag for test environment.
     * When true, requests using this key are routed to simulators.
     */
    @Column(name = "sandbox", nullable = false)
    private Boolean sandbox = false;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerEntity partner;

    /**
     * Key prefix for identification (e.g., "payu_live_", "payu_test_").
     * Stored in plain text for quick lookups.
     */
    @NotBlank
    @Column(name = "key_prefix", nullable = false, length = 32)
    private String keyPrefix;

    /**
     * SHA-256 hash of the full API key. Never store the plain-text key.
     */
    @NotBlank
    @Column(name = "key_hash", nullable = false, length = 64, unique = true)
    private String keyHash;

    /**
     * Last 4 characters of the key for display (e.g., "...xK7m").
     */
    @Column(name = "key_suffix", length = 8)
    private String keySuffix;

    @Column(length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private KeyStatus status = KeyStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private KeyEnvironment environment = KeyEnvironment.LIVE;

    /**
     * Rate plan name linked to this key (e.g., "standard", "premium", "enterprise").
     */
    @Column(name = "rate_plan", length = 64)
    private String ratePlan;

    /**
     * Requests per minute allowed for this key.
     */
    @Column(name = "rate_limit_rpm")
    private Integer rateLimitRpm;

    /**
     * Requests per day allowed for this key.
     */
    @Column(name = "rate_limit_rpd")
    private Integer rateLimitRpd;

    /**
     * When the key expires (null = never expires).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * When a rotated key's grace period ends.
     */
    @Column(name = "grace_period_ends_at")
    private LocalDateTime gracePeriodEndsAt;

    /**
     * When the key was last used successfully.
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * When the key was revoked.
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ApiKeyEntity() {}

    public ApiKeyEntity(PartnerEntity partner, String keyPrefix, String keyHash,
                        String keySuffix, KeyEnvironment environment) {
        this.partner = partner;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.keySuffix = keySuffix;
        this.environment = environment;
        this.status = KeyStatus.ACTIVE;
        this.sandbox = (environment == KeyEnvironment.SANDBOX);
    }

    public ApiKeyEntity(PartnerEntity partner, String keyPrefix, String keyHash,
                        String keySuffix, KeyEnvironment environment, Boolean sandbox) {
        this.partner = partner;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.keySuffix = keySuffix;
        this.environment = environment;
        this.status = KeyStatus.ACTIVE;
        this.sandbox = sandbox != null ? sandbox : (environment == KeyEnvironment.SANDBOX);
    }

    // --- Domain Methods ---

    /**
     * Check if this key is usable for authentication.
     */
    public boolean isUsable() {
        if (status == KeyStatus.REVOKED || status == KeyStatus.EXPIRED) {
            return false;
        }
        if (status == KeyStatus.ROTATED) {
            // Still valid during grace period
            return gracePeriodEndsAt != null && LocalDateTime.now().isBefore(gracePeriodEndsAt);
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return status == KeyStatus.ACTIVE;
    }

    /**
     * Revoke this key immediately.
     */
    public void revoke(String reason) {
        this.status = KeyStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
        this.revokedReason = reason;
    }

    /**
     * Mark this key as rotated with a grace period.
     */
    public void markRotated(int gracePeriodDays) {
        this.status = KeyStatus.ROTATED;
        this.gracePeriodEndsAt = LocalDateTime.now().plusDays(gracePeriodDays);
    }

    /**
     * Record a successful authentication using this key.
     */
    public void recordUsage() {
        this.lastUsedAt = LocalDateTime.now();
    }

    // --- Getters/Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PartnerEntity getPartner() { return partner; }
    public void setPartner(PartnerEntity partner) { this.partner = partner; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    public String getKeySuffix() { return keySuffix; }
    public void setKeySuffix(String keySuffix) { this.keySuffix = keySuffix; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public KeyStatus getStatus() { return status; }
    public void setStatus(KeyStatus status) { this.status = status; }

    public KeyEnvironment getEnvironment() { return environment; }
    public void setEnvironment(KeyEnvironment environment) { this.environment = environment; }

    public String getRatePlan() { return ratePlan; }
    public void setRatePlan(String ratePlan) { this.ratePlan = ratePlan; }

    public Integer getRateLimitRpm() { return rateLimitRpm; }
    public void setRateLimitRpm(Integer rateLimitRpm) { this.rateLimitRpm = rateLimitRpm; }

    public Integer getRateLimitRpd() { return rateLimitRpd; }
    public void setRateLimitRpd(Integer rateLimitRpd) { this.rateLimitRpd = rateLimitRpd; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getGracePeriodEndsAt() { return gracePeriodEndsAt; }
    public void setGracePeriodEndsAt(LocalDateTime gracePeriodEndsAt) { this.gracePeriodEndsAt = gracePeriodEndsAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public String getRevokedReason() { return revokedReason; }
    public void setRevokedReason(String revokedReason) { this.revokedReason = revokedReason; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getSandbox() { return sandbox; }
    public void setSandbox(Boolean sandbox) { this.sandbox = sandbox; }

    /**
     * Check if this key is a sandbox key.
     */
    public boolean isSandbox() {
        return sandbox != null && sandbox;
    }
}
