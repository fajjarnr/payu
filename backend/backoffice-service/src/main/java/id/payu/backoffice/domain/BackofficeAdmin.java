package id.payu.backoffice.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "backoffice_admins", indexes = {
        @Index(name = "idx_admin_email", columnList = "email", unique = true),
        @Index(name = "idx_admin_username", columnList = "username", unique = true),
        @Index(name = "idx_admin_status", columnList = "status")
})
public class BackofficeAdmin extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, unique = true, length = 50)
    public String username;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(length = 255)
    public String passwordHash;

    @Column(length = 100)
    public String firstName;

    @Column(length = 100)
    public String lastName;

    @Column(length = 20)
    public String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AdminStatus status;

    @Column(length = 50)
    public String department;

    @Column(columnDefinition = "JSONB")
    public String permissions;

    @Column(nullable = false)
    public String createdBy;

    @Column(updatable = false)
    public LocalDateTime createdAt;

    public LocalDateTime lastLoginAt;

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
