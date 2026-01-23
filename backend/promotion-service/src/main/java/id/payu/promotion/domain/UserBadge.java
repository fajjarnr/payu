package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_badges", indexes = {
    @Index(name = "idx_user_badge_account", columnList = "accountId")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"accountId", "badgeId"})
})
public class UserBadge extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "account_id", nullable = false)
    public String accountId;

    @Column(name = "badge_id", nullable = false)
    public UUID badgeId;

    @Column(name = "earned_at", nullable = false)
    public LocalDateTime earnedAt;

    @PrePersist
    void onCreate() {
        earnedAt = LocalDateTime.now();
    }
}
