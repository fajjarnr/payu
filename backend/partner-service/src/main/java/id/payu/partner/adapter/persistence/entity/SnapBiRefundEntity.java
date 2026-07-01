package id.payu.partner.adapter.persistence.entity;

import id.payu.partner.domain.*;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * BUG-BE-182 FIX: JPA entity replacing in-memory ConcurrentHashMap for SNAP BI refunds.
 * Ensures refund records survive service restarts.
 */
@Entity
@Table(name = "snap_bi_refunds",
       indexes = {
           @Index(name = "idx_snap_refund_payu_ref", columnList = "payu_refund_no", unique = true),
           @Index(name = "idx_snap_refund_payment_ref", columnList = "payu_reference_no")
       })
public class SnapBiRefundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payu_refund_no", nullable = false, unique = true, length = 64)
    private String payuRefundNo;

    @Column(name = "partner_id", nullable = false, length = 64)
    private String partnerId;

    @Column(name = "payu_reference_no", nullable = false, length = 64)
    private String payuReferenceNo;

    @Column(name = "partner_reference_no", length = 64)
    private String partnerReferenceNo;

    @Column(name = "partner_refund_no", length = 64)
    private String partnerRefundNo;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount; // AUDIT-042

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SnapBiRefundEntity() {
    }

    public SnapBiRefundEntity(String payuRefundNo, String partnerId, String payuReferenceNo,
                        String partnerReferenceNo, String partnerRefundNo,
                        BigDecimal amount, String currency, String reason, String status) {
        this.payuRefundNo = payuRefundNo;
        this.partnerId = partnerId;
        this.payuReferenceNo = payuReferenceNo;
        this.partnerReferenceNo = partnerReferenceNo;
        this.partnerRefundNo = partnerRefundNo;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
        this.status = status;
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPayuRefundNo() { return payuRefundNo; }
    public void setPayuRefundNo(String payuRefundNo) { this.payuRefundNo = payuRefundNo; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getPayuReferenceNo() { return payuReferenceNo; }
    public void setPayuReferenceNo(String payuReferenceNo) { this.payuReferenceNo = payuReferenceNo; }

    public String getPartnerReferenceNo() { return partnerReferenceNo; }
    public void setPartnerReferenceNo(String partnerReferenceNo) { this.partnerReferenceNo = partnerReferenceNo; }

    public String getPartnerRefundNo() { return partnerRefundNo; }
    public void setPartnerRefundNo(String partnerRefundNo) { this.partnerRefundNo = partnerRefundNo; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
