package id.payu.compliance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceCheck {

    @Column(name = "check_id", nullable = false)
    private String checkId;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_standard", nullable = false)
    private ComplianceStandard standard;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ComplianceCheckResult status;

    @Column(name = "details")
    private String details;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;
}
