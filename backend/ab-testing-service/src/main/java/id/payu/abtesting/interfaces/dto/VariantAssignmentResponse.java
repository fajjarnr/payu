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
        VariantAssignmentResponse response = new VariantAssignmentResponse();
        response.setExperimentKey(assignment.getExperimentKey());
        response.setVariant(assignment.getVariant());
        response.setConfig(assignment.getConfig());
        return response;
    }

    public String getExperimentKey() { return experimentKey; }
    public void setExperimentKey(String experimentKey) { this.experimentKey = experimentKey; }
    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
}
