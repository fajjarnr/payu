package id.payu.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Domain model representing a User Risk Profile.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRiskProfile {

    private String username;
    private int failedAttempts;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private Set<String> knownDevices = new HashSet<>();

    @Builder.Default
    private Set<String> knownIps = new HashSet<>();

    public void addKnownDevice(String deviceId) {
        if (this.knownDevices == null) {
            this.knownDevices = new HashSet<>();
        }
        this.knownDevices.add(deviceId);
    }

    public void addKnownIp(String ipAddress) {
        if (this.knownIps == null) {
            this.knownIps = new HashSet<>();
        }
        this.knownIps.add(ipAddress);
    }
}
