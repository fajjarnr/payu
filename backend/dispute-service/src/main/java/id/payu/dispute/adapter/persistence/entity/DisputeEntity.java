package id.payu.dispute.adapter.persistence.entity;

import id.payu.dispute.domain.model.DisputeResolutionType;
import id.payu.dispute.domain.model.DisputeStatus;
import id.payu.security.annotation.Sensitive;
import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import id.payu.security.annotation.SensitivityLevel;

/**
 * JPA Entity for Dispute persistence.
 */
// BUG-ARCH-005 FIX: Replaced @Data with @Getter @Setter to avoid Lombok-generated equals/hashCode on JPA entities
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "disputes")
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class DisputeEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Sensitive(value = SensitivityLevel.HIGH)
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Sensitive(value = SensitivityLevel.HIGH)
    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "disputed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal disputedAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DisputeStatus status;

    @Column(name = "investigation_id", length = 50)
    private String investigationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", length = 20)
    private DisputeResolutionType resolutionType;

    @Column(name = "resolution", length = 1000)
    private String resolution;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "escalation_reason", length = 500)
    private String escalationReason;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "investigation_started_at")
    private Instant investigationStartedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "dispute_id")
    @Builder.Default
    private List<DisputeEvidenceEntity> evidenceList = new ArrayList<>();

    @Version
    @Column(name = "version")
    private Long version;
}
