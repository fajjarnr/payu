package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "badges", indexes = {
    @Index(name = "idx_badge_active", columnList = "isActive"),
    @Index(name = "idx_badge_requirement", columnList = "requirementType, requirementValue")
})
public class Badge extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, unique = true, length = 100)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(name = "icon_url", length = 500)
    public String iconUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false, length = 50)
    public RequirementType requirementType;

    @Column(name = "requirement_value", nullable = false, precision = 19, scale = 4)
    public BigDecimal requirementValue;

    @Column(name = "points_reward", nullable = false)
    public Integer pointsReward;

    @Column(length = 50)
    public String category;

    @Column(name = "is_active", nullable = false)
    public Boolean isActive;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RequirementType {
        TRANSACTION_COUNT,
        TOTAL_AMOUNT,
        STREAK_DAYS,
        REFERRED_USERS,
        LEVEL_REACHED
    }
}
