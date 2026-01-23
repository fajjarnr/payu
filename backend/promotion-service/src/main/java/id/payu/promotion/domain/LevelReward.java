package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "level_rewards")
public class LevelReward extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, unique = true)
    public Integer level;

    @Column(name = "points_reward", nullable = false)
    public Integer pointsReward;

    @Column(name = "bonus_description", columnDefinition = "TEXT")
    public String bonusDescription;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
