package id.payu.partner.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * BUG-BE-182 FIX: JPA entity replacing in-memory ConcurrentHashMap for SNAP BI payments.
 * Ensures payment records survive service restarts.
 */
@Entity
@Table(name = "snap_bi_payments",
       indexes = {
           @Index(name = "idx_snap_payment_partner_ref", columnList = "partner_id, partner_reference_no"),
           @Index(name = "idx_snap_payment_payu_ref", columnList = "payu_reference_no", unique = true)
       })
public class SnapBiPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payu_reference_no", nullable = false, unique = true, length = 64)
    private String payuReferenceNo;

    @Column(name = "partner_id", nullable = false, length = 64)
    private String partnerId;

    @Column(name = "partner_reference_no", length = 64)
    private String partnerReferenceNo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "beneficiary_account_no", length = 64)
    private String beneficiaryAccountNo;

    @Column(name = "beneficiary_bank_code", length = 20)
    private String beneficiaryBankCode;

    @Column(name = "source_account_no", length = 64)
    private String sourceAccountNo;

    @Column(nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SnapBiPayment() {
    }

    public SnapBiPayment(String payuReferenceNo, String partnerId, String partnerReferenceNo,
                         BigDecimal amount, String currency, String beneficiaryAccountNo,
                         String beneficiaryBankCode, String sourceAccountNo, String status) {
        this.payuReferenceNo = payuReferenceNo;
        this.partnerId = partnerId;
        this.partnerReferenceNo = partnerReferenceNo;
        this.amount = amount;
        this.currency = currency;
        this.beneficiaryAccountNo = beneficiaryAccountNo;
        this.beneficiaryBankCode = beneficiaryBankCode;
        this.sourceAccountNo = sourceAccountNo;
        this.status = status;
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPayuReferenceNo() { return payuReferenceNo; }
    public void setPayuReferenceNo(String payuReferenceNo) { this.payuReferenceNo = payuReferenceNo; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getPartnerReferenceNo() { return partnerReferenceNo; }
    public void setPartnerReferenceNo(String partnerReferenceNo) { this.partnerReferenceNo = partnerReferenceNo; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getBeneficiaryAccountNo() { return beneficiaryAccountNo; }
    public void setBeneficiaryAccountNo(String beneficiaryAccountNo) { this.beneficiaryAccountNo = beneficiaryAccountNo; }

    public String getBeneficiaryBankCode() { return beneficiaryBankCode; }
    public void setBeneficiaryBankCode(String beneficiaryBankCode) { this.beneficiaryBankCode = beneficiaryBankCode; }

    public String getSourceAccountNo() { return sourceAccountNo; }
    public void setSourceAccountNo(String sourceAccountNo) { this.sourceAccountNo = sourceAccountNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
