package id.payu.support.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "training_modules")
@EntityListeners(AuditingEntityListener.class)
// BUG-ARCH-005 FIX: Replaced @Data with @Getter @Setter to avoid Lombok-generated equals/hashCode on JPA entities
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainingCategory category;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TrainingStatus status = TrainingStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private boolean mandatory = false;

    @OneToMany(mappedBy = "trainingModule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AgentTraining> agentTrainings = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum TrainingCategory {
        ONBOARDING,
        PRODUCT_KNOWLEDGE,
        COMPLIANCE,
        SYSTEMS,
        COMMUNICATION,
        DISPUTE_RESOLUTION,
        SECURITY
    }

    public enum TrainingStatus {
        DRAFT,
        ACTIVE,
        ARCHIVED
    }
}
