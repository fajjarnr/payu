package id.payu.simulator.qris.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a QRIS payment transaction.
 */
@Entity
@Table(name = "qris_payments")
public class QrisPayment extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "qr_id", nullable = false, unique = true, length = 50)
    public String qrId;

    @Column(name = "reference_number", nullable = false, unique = true, length = 50)
    public String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    public Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(name = "qr_type", nullable = false)
    public QrType qrType = QrType.DYNAMIC;

    @Column(name = "amount", precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(name = "tip_amount", precision = 19, scale = 2)
    public BigDecimal tipAmount;

    @Column(name = "currency", length = 3)
    public String currency = "IDR";

    @Column(name = "qr_content", length = 2000)
    public String qrContent;

    @Column(name = "qr_image_base64", columnDefinition = "TEXT")
    public String qrImageBase64;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payer_name", length = 100)
    public String payerName;

    @Column(name = "payer_account", length = 50)
    public String payerAccount;

    @Column(name = "payer_bank", length = 50)
    public String payerBank;

    @Column(name = "failure_reason", length = 255)
    public String failureReason;

    @Column(name = "webhook_url", length = 500)
    public String webhookUrl;

    @Column(name = "webhook_sent")
    public boolean webhookSent = false;

    @Column(name = "expires_at", nullable = false)
    public LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid_at")
    public LocalDateTime paidAt;

    public enum QrType {
        STATIC,   // Fixed QR, amount entered by payer
        DYNAMIC   // Generated per transaction, amount included
    }

    public enum PaymentStatus {
        PENDING,
        PAID,
        EXPIRED,
        FAILED,
        CANCELLED,
        REFUNDED
    }

    // Finder methods
    public static QrisPayment findByQrId(String qrId) {
        return find("qrId", qrId).firstResult();
    }

    public static QrisPayment findByReference(String referenceNumber) {
        return find("referenceNumber", referenceNumber).firstResult();
    }

    public static List<QrisPayment> findExpiredPending() {
        return list("status = ?1 and expiresAt < ?2", 
                    PaymentStatus.PENDING, LocalDateTime.now());
    }

    public static List<QrisPayment> findPendingWebhooks() {
        return list("webhookSent = false and status in (?1, ?2, ?3)", 
                    PaymentStatus.PAID, PaymentStatus.FAILED, PaymentStatus.EXPIRED);
    }

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void markAsPaid(String payerName, String payerAccount, String payerBank) {
        this.status = PaymentStatus.PAID;
        this.payerName = payerName;
        this.payerAccount = payerAccount;
        this.payerBank = payerBank;
        this.paidAt = LocalDateTime.now();
    }

    public void markAsExpired() {
        this.status = PaymentStatus.EXPIRED;
        this.failureReason = "QR code has expired";
    }

    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }
}
