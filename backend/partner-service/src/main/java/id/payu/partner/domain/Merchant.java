package id.payu.partner.domain;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a merchant registered on the platform.
 * Merchants can generate dynamic QRIS codes for payment collection.
 */
@Entity
@Table(name = "merchants",
       indexes = {
           @Index(name = "idx_merchant_partner_id", columnList = "partner_id"),
           @Index(name = "idx_merchant_merchant_code", columnList = "merchant_code", unique = true),
           @Index(name = "idx_merchant_status", columnList = "status"),
           @Index(name = "idx_merchant_tenant_id", columnList = "tenant_id")
       })
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class Merchant {

    public enum MerchantStatus {
        PENDING_REVIEW,
        ACTIVE,
        SUSPENDED,
        TERMINATED
    }

    public enum MerchantCategory {
        FOOD_BEVERAGE,
        RETAIL,
        SERVICES,
        ENTERTAINMENT,
        TRANSPORTATION,
        HEALTH,
        EDUCATION,
        OTHERS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @NotBlank
    @Column(name = "merchant_code", nullable = false, unique = true, length = 20)
    private String merchantCode;

    @NotBlank
    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MerchantCategory category;

    @NotBlank
    @Column(nullable = false, length = 300)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 10)
    private String postalCode;

    @Column(name = "pic_name", length = 200)
    private String picName;

    @Column(name = "pic_phone", length = 20)
    private String picPhone;

    @Column(name = "pic_email", length = 200)
    private String picEmail;

    @Column(name = "settlement_account_id", length = 64)
    private String settlementAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MerchantStatus status;

    @Column(name = "static_qr_code", length = 500)
    private String staticQrCode;

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Merchant() {
    }

    public Merchant(Partner partner, String merchantCode, String businessName,
                    MerchantCategory category, String address) {
        this.partner = partner;
        this.merchantCode = merchantCode;
        this.businessName = businessName;
        this.category = category;
        this.address = address;
        this.status = MerchantStatus.PENDING_REVIEW;
    }

    // Domain methods

    public boolean isActive() {
        return status == MerchantStatus.ACTIVE;
    }

    public void activate() {
        if (this.status != MerchantStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only pending merchants can be activated");
        }
        this.status = MerchantStatus.ACTIVE;
    }

    public void suspend() {
        if (this.status != MerchantStatus.ACTIVE) {
            throw new IllegalStateException("Only active merchants can be suspended");
        }
        this.status = MerchantStatus.SUSPENDED;
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Partner getPartner() { return partner; }
    public void setPartner(Partner partner) { this.partner = partner; }

    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String merchantCode) { this.merchantCode = merchantCode; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public MerchantCategory getCategory() { return category; }
    public void setCategory(MerchantCategory category) { this.category = category; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getPicName() { return picName; }
    public void setPicName(String picName) { this.picName = picName; }

    public String getPicPhone() { return picPhone; }
    public void setPicPhone(String picPhone) { this.picPhone = picPhone; }

    public String getPicEmail() { return picEmail; }
    public void setPicEmail(String picEmail) { this.picEmail = picEmail; }

    public String getSettlementAccountId() { return settlementAccountId; }
    public void setSettlementAccountId(String settlementAccountId) { this.settlementAccountId = settlementAccountId; }

    public MerchantStatus getStatus() { return status; }
    public void setStatus(MerchantStatus status) { this.status = status; }

    public String getStaticQrCode() { return staticQrCode; }
    public void setStaticQrCode(String staticQrCode) { this.staticQrCode = staticQrCode; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
