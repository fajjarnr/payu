package id.payu.transaction.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a Virtual Account (VA) payment collection.
 * Banks: BCA, BNI, Mandiri, Permata VA numbers for payment collection.
 * Lifecycle: PENDING → PAID → EXPIRED
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "virtual_accounts",
       indexes = {
           @Index(name = "idx_va_number", columnList = "va_number", unique = true),
           @Index(name = "idx_va_partner_id", columnList = "partner_id"),
           @Index(name = "idx_va_status", columnList = "status"),
           @Index(name = "idx_va_expires_at", columnList = "expires_at"),
           @Index(name = "idx_va_external_id", columnList = "partner_id, external_id")
       })
public class VirtualAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "va_number", nullable = false, unique = true, length = 30)
    private String vaNumber;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "external_id", length = 200)
    private String externalId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 500)
    private String description;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_email", length = 200)
    private String customerEmail;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VaStatus status;

    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    @Column(name = "paid_amount", precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = VaStatus.PENDING;
        }
        if (this.currency == null) {
            this.currency = "IDR";
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Domain methods

    public boolean isPending() {
        return status == VaStatus.PENDING && expiresAt.isAfter(Instant.now());
    }

    public void markPaid(BigDecimal paidAmount, String paymentReference) {
        if (this.status != VaStatus.PENDING) {
            throw new IllegalStateException("VA is not pending: " + this.status);
        }
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

    public enum VaStatus {
        PENDING,
        PAID,
        EXPIRED,
        CANCELLED
    }

    public enum BankCode {
        BCA("BCA", "002", "1234"),
        BNI("BNI", "009", "8800"),
        MANDIRI("MANDIRI", "008", "7700"),
        PERMATA("PERMATA", "013", "5500");

        private final String name;
        private final String code;
        private final String prefix;

        BankCode(String name, String code, String prefix) {
            this.name = name;
            this.code = code;
            this.prefix = prefix;
        }

        public String getBankName() { return name; }
        public String getBankCode() { return code; }
        public String getPrefix() { return prefix; }

        public static BankCode fromCode(String code) {
            for (BankCode bc : values()) {
                if (bc.name().equalsIgnoreCase(code) || bc.code.equals(code)) {
                    return bc;
                }
            }
            throw new IllegalArgumentException("Unknown bank code: " + code);
        }
    }
}
