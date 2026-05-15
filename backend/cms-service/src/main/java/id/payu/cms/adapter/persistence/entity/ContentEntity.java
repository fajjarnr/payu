package id.payu.cms.adapter.persistence.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.cms.domain.entity.ContentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Content entity for CMS (banners, promos, alerts)
 */
@Entity
@Table(name = "cms_contents", indexes = {
    @Index(name = "idx_cms_content_type", columnList = "content_type"),
    @Index(name = "idx_cms_status", columnList = "status"),
    @Index(name = "idx_cms_dates", columnList = "start_date, end_date")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType; // BANNER, PROMO, ALERT, POPUP

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "action_type", length = 50)
    private String actionType; // LINK, DEEP_LINK, DISMISS

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "priority")
    private Integer priority; // Higher = shown first

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ContentStatus status = ContentStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "targeting_rules", columnDefinition = "JSONB")
    private Map<String, Object> targetingRules; // User segment, location, device

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private Map<String, Object> metadata; // Custom fields

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Check if content is currently active based on dates
     */
    public boolean isActive() {
        if (status != ContentStatus.ACTIVE) {
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
     * Check if content matches targeting criteria
     */
    public boolean matchesTargeting(String userSegment, String userLocation, String deviceType) {
        if (targetingRules == null || targetingRules.isEmpty()) {
            return true;
        }

        // Check user segment
        if (targetingRules.containsKey("segment") &&
            !targetingRules.get("segment").equals(userSegment)) {
            return false;
        }

        // Check location
        if (targetingRules.containsKey("location") &&
            !targetingRules.get("location").equals(userLocation)) {
            return false;
        }

        // Check device
        if (targetingRules.containsKey("device") &&
            !targetingRules.get("device").equals(deviceType)) {
            return false;
        }

        return true;
    }
}
