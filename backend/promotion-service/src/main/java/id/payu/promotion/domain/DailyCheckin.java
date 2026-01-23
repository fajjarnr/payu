package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_checkins", indexes = {
    @Index(name = "idx_checkin_account", columnList = "accountId"),
    @Index(name = "idx_checkin_date", columnList = "checkinDate")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"accountId", "checkinDate"})
})
public class DailyCheckin extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "account_id", nullable = false)
    public String accountId;

    @Column(name = "checkin_date", nullable = false)
    public LocalDate checkinDate;

    @Column(name = "streak_count", nullable = false)
    public Integer streakCount;

    @Column(name = "points_earned", nullable = false)
    public Integer pointsEarned;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
