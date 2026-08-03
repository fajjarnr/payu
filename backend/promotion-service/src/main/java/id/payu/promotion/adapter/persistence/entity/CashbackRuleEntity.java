package id.payu.promotion.adapter.persistence.entity;

import id.payu.promotion.domain.model.CashbackType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "cashback_rules", indexes = {
        @Index(name = "idx_cashback_rules_active", columnList = "active"),
        @Index(name = "idx_cashback_rules_validity", columnList = "valid_from,valid_until")
})
public class CashbackRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_id", nullable = false, unique = true, length = 50)
    private String ruleId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "cashback_type", nullable = false, length = 20)
    private CashbackType cashbackType;

    @Column(name = "cashback_amount", precision = 19, scale = 4)
    private BigDecimal cashbackAmount;

    @Column(name = "cashback_percentage", precision = 5, scale = 2)
    private BigDecimal cashbackPercentage;

    @Column(name = "max_cashback", precision = 19, scale = 4)
    private BigDecimal maxCashback;

    @Column(name = "min_amount", precision = 19, scale = 4)
    private BigDecimal minAmount;

    @Column(name = "exact_amount", precision = 19, scale = 4)
    private BigDecimal exactAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tiered_cashback", columnDefinition = "JSONB")
    private Map<BigDecimal, BigDecimal> tieredCashback = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_merchant_codes", columnDefinition = "JSONB")
    private Set<String> applicableMerchantCodes = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_categories", columnDefinition = "JSONB")
    private Set<String> applicableCategories = new HashSet<>();

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CashbackType getCashbackType() { return cashbackType; }
    public void setCashbackType(CashbackType cashbackType) { this.cashbackType = cashbackType; }
    public BigDecimal getCashbackAmount() { return cashbackAmount; }
    public void setCashbackAmount(BigDecimal cashbackAmount) { this.cashbackAmount = cashbackAmount; }
    public BigDecimal getCashbackPercentage() { return cashbackPercentage; }
    public void setCashbackPercentage(BigDecimal cashbackPercentage) { this.cashbackPercentage = cashbackPercentage; }
    public BigDecimal getMaxCashback() { return maxCashback; }
    public void setMaxCashback(BigDecimal maxCashback) { this.maxCashback = maxCashback; }
    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }
    public BigDecimal getExactAmount() { return exactAmount; }
    public void setExactAmount(BigDecimal exactAmount) { this.exactAmount = exactAmount; }
    public Map<BigDecimal, BigDecimal> getTieredCashback() { return tieredCashback; }
    public void setTieredCashback(Map<BigDecimal, BigDecimal> tieredCashback) { this.tieredCashback = tieredCashback; }
    public Set<String> getApplicableMerchantCodes() { return applicableMerchantCodes; }
    public void setApplicableMerchantCodes(Set<String> applicableMerchantCodes) { this.applicableMerchantCodes = applicableMerchantCodes; }
    public Set<String> getApplicableCategories() { return applicableCategories; }
    public void setApplicableCategories(Set<String> applicableCategories) { this.applicableCategories = applicableCategories; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }
}
