package id.payu.support.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_agents")
public class SupportAgent extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String employeeId;

    @Column(nullable = false)
    public String name;

    @Column(unique = true, nullable = false)
    public String email;

    @Column(nullable = false)
    public String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AgentLevel level;

    @Column(nullable = false)
    public boolean active;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

    public enum AgentLevel {
        JUNIOR,
        SENIOR,
        TEAM_LEAD,
        MANAGER
    }

    public SupportAgent() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
        this.level = AgentLevel.JUNIOR;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
