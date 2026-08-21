package id.payu.compliance.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceCheck {

    @Column(name = "check_id")
    private String checkId;

    @Enumerated(EnumType.STRING)
    @Column(name = "standard")
    private ComplianceStandard standard;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ComplianceCheckResult status;

    @Column(name = "details")
    private String details;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;
}
