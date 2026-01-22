package id.payu.compliance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "audit_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_standard", nullable = false)
    private ComplianceStandard standard;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "compliance_checks", joinColumns = @JoinColumn(name = "audit_report_id"))
    @Builder.Default
    private List<ComplianceCheck> checks = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false)
    private ComplianceCheckResult overallStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;
}
