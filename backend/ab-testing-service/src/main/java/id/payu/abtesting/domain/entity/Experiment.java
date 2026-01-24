package id.payu.abtesting.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Experiment entity for A/B testing framework
 */
@Entity
@Table(name = "ab_experiments", indexes = {
    @Index(name = "idx_ab_status", columnList = "status"),
    @Index(name = "idx_ab_dates", columnList = "start_date, end_date")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experiment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "key", nullable = false, unique = true, length = 100)
    private String key; // Unique identifier for frontend

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExperimentStatus status = ExperimentStatus.DRAFT;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "traffic_split", nullable = false)
    private Integer trafficSplit; // Percentage for variant B (0-100)

    @Column(name = "variant_a_config", columnDefinition = "JSONB")
    private Map<String, Object> variantAConfig; // Control configuration

    @Column(name = "variant_b_config", columnDefinition = "JSONB")
    private Map<String, Object> variantBConfig; // Test configuration

    @Column(name = "targeting_rules", columnDefinition = "JSONB")
    private Map<String, Object> targetingRules;

    @Column(name = "metrics", columnDefinition = "JSONB")
    private Map<String, Object> metrics; // Conversion rates, engagement

    @Column(name = "confidence_level")
    private Double confidenceLevel; // Statistical significance

    @Column(name = "winner", length = 50)
    private String winner; // CONTROL, VARIANT_B, INCONCLUSIVE

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    public enum ExperimentStatus {
        DRAFT,      // Not yet started
        RUNNING,    // Currently active
        PAUSED,     // Temporarily stopped
        COMPLETED,  // Finished with winner
        CANCELLED   // Stopped without conclusion
    }

    /**
     * Check if experiment is currently running
     */
    public boolean isRunning() {
        if (status != ExperimentStatus.RUNNING) {
            return false;
        }

        LocalDate now = LocalDate.now();
        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && now.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    /**
     * Get variant for a user based on user ID hashing
     */
    public String getVariantForUser(UUID userId) {
        // Consistent bucket assignment based on user ID
        int hash = userId.hashCode();
        int bucket = Math.abs(hash % 100);

        return bucket < trafficSplit ? "VARIANT_B" : "CONTROL";
    }

    /**
     * Calculate conversion rate for a variant
     */
    public double getConversionRate(String variant) {
        if (metrics == null) {
            return 0.0;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> variantMetrics = (Map<String, Object>) metrics.get(variant);
        if (variantMetrics == null) {
            return 0.0;
        }

        int conversions = ((Number) variantMetrics.getOrDefault("conversions", 0)).intValue();
        int participants = ((Number) variantMetrics.getOrDefault("participants", 0)).intValue();

        return participants > 0 ? (double) conversions / participants : 0.0;
    }
}
