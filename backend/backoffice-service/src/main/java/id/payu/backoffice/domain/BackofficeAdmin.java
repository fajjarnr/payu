package id.payu.backoffice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "backoffice_admins", indexes = {
        @Index(name = "idx_admin_email", columnList = "email", unique = true),
        @Index(name = "idx_admin_username", columnList = "username", unique = true),
        @Index(name = "idx_admin_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackofficeAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 255)
    private String passwordHash;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminStatus status;

    @Column(length = 50)
    private String department;

    @Column(columnDefinition = "JSONB")
    private String permissions;

    @Column(nullable = false)
    private String createdBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = AdminStatus.ACTIVE;
        }
    }

    public enum AdminStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return firstName != null ? firstName : username;
    }
}
