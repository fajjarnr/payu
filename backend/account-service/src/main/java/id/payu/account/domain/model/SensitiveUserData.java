package id.payu.account.domain.model;

import id.payu.security.crypto.EncryptionService;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Example entity with field-level encryption for sensitive PII data
 */
@Entity
@Table(name = "sensitive_user_data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveUserData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * NIK (National ID) - Encrypted at rest
     * Uses @ColumnTransformer for transparent encryption/decryption
     */
    @Column(name = "nik", nullable = false, length = 512)
    @ColumnTransformer(
            read = "PGP_SYM_DECRYPT(nik::bytea, ${encryption.key})",
            write = "PGP_SYM_ENCRYPT(?, ${encryption.key})"
    )
    private String nik;

    /**
     * Tax ID - Encrypted at rest
     */
    @Column(name = "tax_id", length = 512)
    @ColumnTransformer(
            read = "PGP_SYM_DECRYPT(tax_id::bytea, ${encryption.key})",
            write = "PGP_SYM_ENCRYPT(?, ${encryption.key})"
    )
    private String taxId;

    /**
     * Mother's Maiden Name - Encrypted at rest
     * Used for security verification
     */
    @Column(name = "mother_maiden_name", length = 512)
    @ColumnTransformer(
            read = "PGP_SYM_DECRYPT(mother_maiden_name::bytea, ${encryption.key})",
            write = "PGP_SYM_ENCRYPT(?, ${encryption.key})"
    )
    private String motherMaidenName;

    /**
     * Address - Stored as JSONB with selective field encryption
     */
    @Column(name = "address", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private AddressData address;

    /**
     * Phone numbers - Encrypted
     */
    @Column(name = "phone_primary", length = 512)
    @ColumnTransformer(
            read = "PGP_SYM_DECRYPT(phone_primary::bytea, ${encryption.key})",
            write = "PGP_SYM_ENCRYPT(?, ${encryption.key})"
    )
    private String phonePrimary;

    @Column(name = "phone_secondary", length = 512)
    @ColumnTransformer(
            read = "PGP_SYM_DECRYPT(phone_secondary::bytea, ${encryption.key})",
            write = "PGP_SYM_ENCRYPT(?, ${encryption.key})"
    )
    private String phoneSecondary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    /**
     * Address data stored as JSONB
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressData {
        private String street;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }

    /**
     * Repository interface for SensitiveUserData
     */
    @Repository
    public interface SensitiveUserDataRepository extends JpaRepository<SensitiveUserData, Long> {

        Optional<SensitiveUserData> findByUserId(Long userId);

        @Query("SELECT s FROM SensitiveUserData s WHERE " +
               "PGP_SYM_DECRYPT(s.nik::bytea, :key) = :nik")
        Optional<SensitiveUserData> findByNik(@Param("nik") String nik, @Param("key") String key);
    }
}
