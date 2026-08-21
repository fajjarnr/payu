package id.payu.compliance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceCheck {

    private String checkId;

    private ComplianceStandard standard;

    private String description;

    private ComplianceCheckResult status;

    private String details;

    private LocalDateTime checkedAt; // ponytail: pure domain, JPA @Embeddable/@Column lives only in adapter entity ComplianceCheckEmbeddable
}
