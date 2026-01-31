package id.payu.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_known_devices")
@Data
@NoArgsConstructor
public class UserKnownDeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "username", nullable = false)
    private UserRiskProfileEntity userRiskProfile;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @UpdateTimestamp
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;
}
