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

@Entity
@Table(name = "agent_training", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"agent_id", "training_module_id"})
})
@EntityListeners(AuditingEntityListener.class)
// BUG-ARCH-005 FIX: Replaced @Data with @Getter @Setter to avoid Lombok-generated equals/hashCode on JPA entities
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTraining {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private SupportAgent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_module_id", nullable = false)
    private TrainingModule trainingModule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CompletionStatus status = CompletionStatus.NOT_STARTED;

    @Column
    private Integer score;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(length = 1000)
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum CompletionStatus {
        NOT_STARTED,
        IN_PROGRESS,
        PASSED,
        FAILED
    }
}
