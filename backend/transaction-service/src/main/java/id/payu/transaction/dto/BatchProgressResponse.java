package id.payu.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for batch progress.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProgressResponse {

    private UUID batchId;
    private int progressPercentage;
}
