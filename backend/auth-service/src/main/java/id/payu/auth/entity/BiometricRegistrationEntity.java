package id.payu.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_registrations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiometricRegistrationEntity {

    @Id
    @Column(name = "registration_id")
    private String registrationId;

    @Column(nullable = false)
    private String username;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "device_type", nullable = false)
    private String deviceType;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "active")
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
