package id.payu.abtesting.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for variant assignment request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantAssignmentRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;
}
