package id.payu.support.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_training", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"agent_id", "training_module_id"})
})
public class AgentTraining extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    public SupportAgent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_module_id", nullable = false)
    public TrainingModule trainingModule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public CompletionStatus status;

    @Column
    public Integer score;

    @Column
    public LocalDateTime startedAt;

    @Column
    public LocalDateTime completedAt;

    @Column
    public String notes;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

    public enum CompletionStatus {
        NOT_STARTED,
        IN_PROGRESS,
        PASSED,
        FAILED
    }

    public AgentTraining() {
        this.createdAt = LocalDateTime.now();
        this.status = CompletionStatus.NOT_STARTED;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
