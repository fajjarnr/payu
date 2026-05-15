package id.payu.wallet.adapter.persistence.entity;

import id.payu.wallet.multitenancy.TenantAware;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for escrow transactions.
 * Persists payment holding state for marketplace/partner flows.
 */
@Entity
@Table(name = "escrow_transactions", indexes = {
    @Index(name = "idx_escrow_buyer", columnList = "buyer_account_id"),
    @Index(name = "idx_escrow_seller", columnList = "seller_account_id"),
    @Index(name = "idx_escrow_partner", columnList = "partner_id"),
    @Index(name = "idx_escrow_status", columnList = "status"),
    @Index(name = "idx_escrow_external_ref", columnList = "external_reference_id"),
    @Index(name = "idx_escrow_expires", columnList = "expires_at"),
    @Index(name = "idx_escrow_tenant", columnList = "tenant_id")
})
@TenantAware
public class EscrowTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "buyer_account_id", nullable = false, length = 128)
    private String buyerAccountId;

    @Column(name = "seller_account_id", nullable = false, length = 128)
    private String sellerAccountId;

    @Column(name = "partner_id", length = 128)
    private String partnerId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "fee_amount", precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EscrowStatus status;

    @Column(name = "external_reference_id", length = 128)
    private String externalReferenceId;

    @Column(length = 512)
    private String description;

    @Column(name = "reservation_id", length = 64)
    private String reservationId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "held_at")
    private LocalDateTime heldAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_reason", length = 512)
    private String refundReason;

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public EscrowTransactionEntity() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getBuyerAccountId() { return buyerAccountId; }
    public void setBuyerAccountId(String buyerAccountId) { this.buyerAccountId = buyerAccountId; }
    public String getSellerAccountId() { return sellerAccountId; }
    public void setSellerAccountId(String sellerAccountId) { this.sellerAccountId = sellerAccountId; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public EscrowStatus getStatus() { return status; }
    public void setStatus(EscrowStatus status) { this.status = status; }
    public String getExternalReferenceId() { return externalReferenceId; }
    public void setExternalReferenceId(String externalReferenceId) { this.externalReferenceId = externalReferenceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getHeldAt() { return heldAt; }
    public void setHeldAt(LocalDateTime heldAt) { this.heldAt = heldAt; }
    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
