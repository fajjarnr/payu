package id.payu.support.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "training_modules")
public class TrainingModule extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String code;

    @Column(nullable = false)
    public String title;

    @Column
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TrainingCategory category;

    @Column(nullable = false)
    public int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TrainingStatus status;

    @Column(nullable = false)
    public boolean mandatory;

    @OneToMany(mappedBy = "trainingModule", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<AgentTraining> agentTrainings;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

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

    public TrainingModule() {
        this.createdAt = LocalDateTime.now();
        this.status = TrainingStatus.DRAFT;
        this.mandatory = false;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
