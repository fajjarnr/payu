package id.payu.support.domain.model;

import id.payu.support.domain.TrainingCategory;
import id.payu.support.domain.TrainingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Domain model representing a Training Module.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingModule {

    private Long id;
    private String code;
    private String title;
    private String description;
    private TrainingCategory category;
    private Integer durationMinutes;

    @Builder.Default
    private TrainingStatus status = TrainingStatus.DRAFT;

    @Builder.Default
    private boolean mandatory = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
