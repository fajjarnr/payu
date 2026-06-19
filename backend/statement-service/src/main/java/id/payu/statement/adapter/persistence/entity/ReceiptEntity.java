package id.payu.statement.adapter.persistence.entity;

import id.payu.security.annotation.Sensitive;
import id.payu.statement.domain.model.ReceiptStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.security.annotation.SensitivityLevel;

/**
 * JPA Entity for Receipt persistence.
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
@Entity
@Table(name = "receipts", indexes = {
    @Index(name = "idx_receipts_transaction_id", columnList = "transaction_id", unique = true),
    @Index(name = "idx_receipts_customer_id", columnList = "customer_id"),
    @Index(name = "idx_receipts_status", columnList = "status"),
    @Index(name = "idx_receipts_expiry_date", columnList = "expiry_date")
})
@EntityListeners(AuditingEntityListener.class)
// BUG-ARCH-005 FIX: Replaced @Data with @Getter @Setter to avoid Lombok-generated equals/hashCode on JPA entities
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, length = 100, unique = true)
    private String transactionId;

    @Sensitive
    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    // Sender info
    @Sensitive
    @Column(name = "sender_name", nullable = false, length = 200)
    private String senderName;

    @Sensitive(value = SensitivityLevel.HIGH)
    @Column(name = "sender_account_number", nullable = false, length = 50)
    private String senderAccountNumber;

    @Column(name = "sender_bank_name", nullable = false, length = 100)
    private String senderBankName;

    // Recipient info
    @Sensitive
    @Column(name = "recipient_name", nullable = false, length = 200)
    private String recipientName;

    @Sensitive(value = SensitivityLevel.HIGH)
    @Column(name = "recipient_account_number", nullable = false, length = 50)
    private String recipientAccountNumber;

    @Column(name = "recipient_bank_name", nullable = false, length = 100)
    private String recipientBankName;

    @Column(name = "transaction_timestamp", nullable = false)
    private LocalDateTime transactionTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReceiptStatus status;

    @Column(name = "reference_number", nullable = false, length = 100)
    private String referenceNumber;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "access_count")
    @Builder.Default
    private Integer accessCount = 0;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Version
    private Long version;

}
