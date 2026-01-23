package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_levels", indexes = {
    @Index(name = "idx_user_level_account", columnList = "accountId"),
    @Index(name = "idx_user_level_level", columnList = "level")
})
public class UserLevel extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "account_id", nullable = false, unique = true)
    public String accountId;

    @Column(nullable = false)
    public Integer level;

    @Column(nullable = false)
    public Integer xp;

    @Column(name = "level_name", length = 100)
    public String levelName;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (level == null) {
            level = 1;
        }
        if (xp == null) {
            xp = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
