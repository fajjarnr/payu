package id.payu.partner.adapter.persistence.entity;

import id.payu.partner.domain.*;

import id.payu.security.annotation.Sensitive;
import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import id.payu.security.annotation.SensitivityLevel;

@Entity
@Table(name = "partners",
       indexes = @Index(name = "idx_partner_tenant_id", columnList = "tenant_id"))
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class PartnerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 64)
    private String partnerCode;

    @NotBlank
    private String name;

    @NotBlank
    private String type;

    @NotBlank
    @Email
    @Sensitive
    private String email;

    @Sensitive
    private String phone;
    
    @Sensitive(value = SensitivityLevel.CRITICAL)
    private String apiKey;

    @Sensitive
    private String clientId;

    @Sensitive(value = SensitivityLevel.CRITICAL)
    private String clientSecret;

    @Sensitive(value = SensitivityLevel.CRITICAL)
    private String publicKey;

    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PartnerStatus status = PartnerStatus.PENDING_VERIFICATION;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    /**
     * Tenant identifier for multi-tenancy data isolation.
     * Each partner belongs to a tenant scope (typically their own clientId or org grouping).
     */
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public PartnerEntity() {}

    public PartnerEntity(String name, String type, String email, String phone, String apiKey) {
        this.name = name;
        this.type = type;
        this.email = email;
        this.phone = phone;
        this.apiKey = apiKey;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public boolean isActive() { return active || status == PartnerStatus.ACTIVE; }
    public void setActive(boolean active) { this.active = active; }
    public PartnerStatus getStatus() { return status; }
    public void setStatus(PartnerStatus status) { this.status = status; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
