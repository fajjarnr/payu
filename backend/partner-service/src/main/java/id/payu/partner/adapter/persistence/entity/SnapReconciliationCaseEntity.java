package id.payu.partner.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * PARTNER-PROD-005: a reconciliation case for a SNAP payment/refund whose
 * wallet ledger movements do not match the partner record (or vice versa).
 * Immutable after detection; ops resolves via reversal-only correction.
 */
@Entity
@Table(name = "snap_reconciliation_cases")
public class SnapReconciliationCaseEntity {

    public static final String STATUS_OPEN = "OPEN";
    public static final String TYPE_PAYMENT = "PAYMENT";
    public static final String TYPE_REFUND = "REFUND";
    public static final String TYPE_WALLET_MOVEMENT = "WALLET_MOVEMENT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_type", nullable = false, length = 32)
    private String referenceType;

    @Column(name = "reference_id", nullable = false, length = 128)
    private String referenceId;

    @Column(name = "detail", length = 1024)
    private String detail;

    @Column(name = "status", nullable = false, length = 16)
    private String status = STATUS_OPEN;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    protected SnapReconciliationCaseEntity() {
    }

    public SnapReconciliationCaseEntity(String referenceType, String referenceId, String detail) {
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public String getReferenceType() { return referenceType; }
    public String getReferenceId() { return referenceId; }
    public String getDetail() { return detail; }
    public String getStatus() { return status; }
    public Instant getDetectedAt() { return detectedAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    public void resolve() {
        this.status = "RESOLVED";
        this.resolvedAt = Instant.now();
    }
}
