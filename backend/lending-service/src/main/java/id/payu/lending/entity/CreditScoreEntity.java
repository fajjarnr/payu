package id.payu.lending.entity;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.lending.domain.model.RiskCategory;

@Entity
@Table(name = "credit_scores")
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class CreditScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", nullable = false)
    private id.payu.lending.domain.model.RiskCategory riskCategory;

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CreditScoreEntity() {}

    public CreditScoreEntity(UUID id, UUID userId, BigDecimal score, 
                             id.payu.lending.domain.model.RiskCategory riskCategory, 
                             LocalDateTime lastCalculatedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.score = score;
        this.riskCategory = riskCategory;
        this.lastCalculatedAt = lastCalculatedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public id.payu.lending.domain.model.RiskCategory getRiskCategory() { return riskCategory; }
    public void setRiskCategory(id.payu.lending.domain.model.RiskCategory riskCategory) { this.riskCategory = riskCategory; }
    public LocalDateTime getLastCalculatedAt() { return lastCalculatedAt; }
    public void setLastCalculatedAt(LocalDateTime lastCalculatedAt) { this.lastCalculatedAt = lastCalculatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    @Version
    private Long version;


    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private BigDecimal score;
        private id.payu.lending.domain.model.RiskCategory riskCategory;
        private LocalDateTime lastCalculatedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder score(BigDecimal score) { this.score = score; return this; }
        public Builder riskCategory(id.payu.lending.domain.model.RiskCategory riskCategory) { this.riskCategory = riskCategory; return this; }
        public Builder lastCalculatedAt(LocalDateTime lastCalculatedAt) { this.lastCalculatedAt = lastCalculatedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public CreditScoreEntity build() {
            return new CreditScoreEntity(id, userId, score, riskCategory, lastCalculatedAt, createdAt, updatedAt);
        }
    }
}
