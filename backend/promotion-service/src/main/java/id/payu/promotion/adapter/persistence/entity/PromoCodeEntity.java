package id.payu.promotion.adapter.persistence.entity;

import id.payu.promotion.domain.model.DiscountType;
import id.payu.promotion.domain.model.PromoStatus;
import id.payu.promotion.domain.model.UsageType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "promo_codes", indexes = {
        @Index(name = "idx_promo_codes_code", columnList = "code"),
        @Index(name = "idx_promo_codes_status", columnList = "status"),
        @Index(name = "idx_promo_codes_expiry", columnList = "expiry_date")
})
public class PromoCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 20)
    private UsageType usageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PromoStatus status;

    @Column(name = "minimum_amount", precision = 19, scale = 4)
    private BigDecimal minimumAmount;

    @Column(name = "max_discount_amount", precision = 19, scale = 4)
    private BigDecimal maxDiscountAmount;

    @Column(name = "max_usage_count")
    private Integer maxUsageCount;

    @Column(name = "current_usage_count", nullable = false)
    private int currentUsageCount;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "excluded_partner_ids", columnDefinition = "JSONB")
    private Set<String> excludedPartnerIds = new HashSet<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public UsageType getUsageType() { return usageType; }
    public void setUsageType(UsageType usageType) { this.usageType = usageType; }
    public PromoStatus getStatus() { return status; }
    public void setStatus(PromoStatus status) { this.status = status; }
    public BigDecimal getMinimumAmount() { return minimumAmount; }
    public void setMinimumAmount(BigDecimal minimumAmount) { this.minimumAmount = minimumAmount; }
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }
    public Integer getMaxUsageCount() { return maxUsageCount; }
    public void setMaxUsageCount(Integer maxUsageCount) { this.maxUsageCount = maxUsageCount; }
    public int getCurrentUsageCount() { return currentUsageCount; }
    public void setCurrentUsageCount(int currentUsageCount) { this.currentUsageCount = currentUsageCount; }
    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }
    public Set<String> getExcludedPartnerIds() { return excludedPartnerIds; }
    public void setExcludedPartnerIds(Set<String> excludedPartnerIds) { this.excludedPartnerIds = excludedPartnerIds; }
}
