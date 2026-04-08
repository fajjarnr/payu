package id.payu.auth.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_risk_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class UserRiskProfileEntity {

    @Id
    @Column(name = "username")
    @EqualsAndHashCode.Include
    @ToString.Include
    private String username;

    @Column(name = "failed_attempts")
    private int failedAttempts;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "userRiskProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<UserKnownDeviceEntity> knownDevices = new HashSet<>();

    @OneToMany(mappedBy = "userRiskProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<UserKnownIpEntity> knownIps = new HashSet<>();

    public void addKnownDevice(String deviceId) {
        UserKnownDeviceEntity device = new UserKnownDeviceEntity();
        device.setDeviceId(deviceId);
        device.setUserRiskProfile(this);
        this.knownDevices.add(device);
    }

    public void addKnownIp(String ipAddress) {
        UserKnownIpEntity ip = new UserKnownIpEntity();
        ip.setIpAddress(ipAddress);
        ip.setUserRiskProfile(this);
        this.knownIps.add(ip);
    }
}
