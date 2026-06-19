package id.payu.support.adapter.persistence.entity;

import id.payu.support.domain.TrainingCategory;
import id.payu.support.domain.TrainingStatus;
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
public class TrainingModuleEntity {

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
    private List<AgentTrainingEntity> agentTrainings = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Version
    private Long version;


    @LastModifiedDate
    private LocalDateTime updatedAt;
}
