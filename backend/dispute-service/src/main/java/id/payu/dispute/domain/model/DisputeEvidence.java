package id.payu.dispute.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Value Object representing evidence attached to a dispute.
 *
 * <p>Immutable value object that contains information about uploaded evidence files.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeEvidence {

    private UUID id;
    private String fileName;
    private String fileUrl;
    private String uploadedBy;
    private Instant uploadedAt;

    /**
     * Creates a new evidence item.
     *
     * @param fileName   the name of the file
     * @param fileUrl    the URL where the file is stored
     * @param uploadedBy who uploaded the evidence (CUSTOMER, MERCHANT, SYSTEM)
     * @return a new DisputeEvidence instance
     */
    public static DisputeEvidence create(String fileName, String fileUrl, String uploadedBy) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("File URL cannot be null or empty");
        }
        if (uploadedBy == null || uploadedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Uploaded by cannot be null or empty");
        }

        return DisputeEvidence.builder()
                .id(UUID.randomUUID())
                .fileName(fileName)
                .fileUrl(fileUrl)
                .uploadedBy(uploadedBy)
                .uploadedAt(Instant.now())
                .build();
    }
}
