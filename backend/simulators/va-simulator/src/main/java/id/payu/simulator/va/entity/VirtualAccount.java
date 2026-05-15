package id.payu.simulator.va.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Simulated Virtual Account entity for testing.
 * Mirrors the real VA entity in transaction-service for deterministic testing.
 */
@Entity
@Table(name = "simulated_va",
       indexes = {
           @Index(name = "idx_sim_va_number", columnList = "va_number", unique = true),
           @Index(name = "idx_sim_va_status", columnList = "status"),
           @Index(name = "idx_sim_va_bank", columnList = "bank_code")
       })
public class VirtualAccount extends PanacheEntity {

    @Column(name = "va_number", nullable = false, unique = true, length = 30)
    public String vaNumber;

    @Column(name = "bank_code", nullable = false, length = 10)
    public String bankCode;

    @Column(name = "bank_name", length = 50)
    public String bankName;

    @Column(name = "partner_id", nullable = false)
    public String partnerId;

    @Column(name = "external_id", length = 200)
    public String externalId;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(nullable = false, length = 3)
    public String currency;

    @Column(length = 500)
    public String description;

    @Column(name = "customer_name", length = 200)
    public String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public VaStatus status;

    @Column(name = "callback_url", length = 500)
    public String callbackUrl;

    @Column(name = "paid_amount", precision = 19, scale = 2)
    public BigDecimal paidAmount;

    @Column(name = "paid_at")
    public Instant paidAt;

    @Column(name = "payment_reference", length = 100)
    public String paymentReference;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    public VirtualAccount() {
    }

    public VirtualAccount(String vaNumber, String bankCode, String bankName,
                          String partnerId, BigDecimal amount, String currency,
                          Instant expiresAt) {
        this.vaNumber = vaNumber;
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.partnerId = partnerId;
        this.amount = amount;
        this.currency = currency != null ? currency : "IDR";
        this.status = VaStatus.PENDING;
        this.expiresAt = expiresAt;
    }

    public boolean isPending() {
        return status == VaStatus.PENDING && expiresAt.isAfter(Instant.now());
    }

    public void markPaid(BigDecimal paidAmount, String paymentReference) {
        this.status = VaStatus.PAID;
        this.paidAmount = paidAmount;
        this.paymentReference = paymentReference;
        this.paidAt = Instant.now();
    }

    public void markExpired() {
        if (this.status == VaStatus.PENDING) {
            this.status = VaStatus.EXPIRED;
        }
    }

    public static VirtualAccount findByVaNumber(String vaNumber) {
        return find("vaNumber", vaNumber).firstResult();
    }
}
