package id.payu.abtesting.interfaces.dto;

import id.payu.abtesting.domain.service.ExperimentService.VariantAssignment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for variant assignment response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantAssignmentResponse {

    private String experimentKey;
    private String variant;
    private Map<String, Object> config;

    /**
     * Convert domain object to DTO
     */
    public static VariantAssignmentResponse fromDomain(VariantAssignment assignment) {
        return VariantAssignmentResponse.builder()
                .experimentKey(assignment.getExperimentKey())
                .variant(assignment.getVariant())
                .config(assignment.getConfig())
                .build();
    }
}
