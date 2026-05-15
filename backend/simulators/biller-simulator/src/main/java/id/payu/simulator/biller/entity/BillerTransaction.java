package id.payu.simulator.biller.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Records a payment transaction processed by the biller simulator.
 */
@Entity
@Table(name = "biller_transactions")
public class BillerTransaction extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "biller_code", nullable = false)
    public String billerCode;

    @Column(name = "customer_number", nullable = false)
    public String customerNumber;

    @Column(nullable = false)
    public BigDecimal amount;

    @Column(name = "reference_number", unique = true, nullable = false)
    public String referenceNumber;

    @Column(name = "biller_transaction_id", unique = true)
    public String billerTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TransactionStatus status;

    @Column(name = "failure_reason")
    public String failureReason;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "completed_at")
    public Instant completedAt;

    public static BillerTransaction findByReference(String referenceNumber) {
        return find("referenceNumber", referenceNumber).firstResult();
    }
}
