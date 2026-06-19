package id.payu.account.adapter.persistence.entity;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import id.payu.security.annotation.Sensitive;
import id.payu.security.converter.EncryptedStringConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Comment;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Profile entity representing user profile data.
 *
 * <p><b>Security:</b> NIK (Nomor Induk Kependudukan - Indonesian ID Number)
 * is encrypted at rest using field-level encryption via {@link EncryptedStringConverter}.
 * This ensures compliance with Indonesian UU PDP (Personal Data Protection Law) requirements.</p>
 */
@Entity
@Table(name = "profiles")
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class ProfileEntity {

    @Id
    private UUID id; // Same as User ID

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private UserEntity user;

    @Column(name = "full_name", nullable = false)
    @Sensitive
    private String fullName;

    /**
     * NIK (Nomor Induk Kependudukan) encrypted at rest using AES-GCM (256-bit key).
     * The EncryptedStringConverter handles automatic encryption/decryption.
     *
     * @see <a href="https://www.dpr.go.id/jdih/jdih-ppuu">UU PDP No. 27 of 2022</a>
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Comment("NIK encrypted at rest for UU PDP compliance")
    @Column(name = "nik", unique = true, length = 512, nullable = false)
    @Sensitive
    private String nik;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "birth_place")
    private String birthPlace;

    @Column(name = "gender")
    private String gender;

    @Column(name = "address")
    @Sensitive
    private String address;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_data", columnDefinition = "jsonb")
    private Map<String, Object> additionalData = new HashMap<>();

    // Constructors
    public ProfileEntity() {
    }

    public ProfileEntity(UUID id, String tenantId, UserEntity user, String fullName, String nik,
                   LocalDate birthDate, String birthPlace, String gender, String address,
                   Map<String, Object> additionalData) {
        this.id = id;
        this.tenantId = tenantId;
        this.user = user;
        this.fullName = fullName;
        this.nik = nik;
        this.birthDate = birthDate;
        this.birthPlace = birthPlace;
        this.gender = gender;
        this.address = address;
        this.additionalData = additionalData != null ? additionalData : new HashMap<>();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNik() {
        return nik;
    }

    public void setNik(String nik) {
        this.nik = nik;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData != null ? additionalData : new HashMap<>();
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String tenantId;
        private UserEntity user;
        private String fullName;
        private String nik;
        private LocalDate birthDate;
        private String birthPlace;
        private String gender;
        private String address;
        private Map<String, Object> additionalData = new HashMap<>();

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder user(UserEntity user) {
            this.user = user;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder nik(String nik) {
            this.nik = nik;
            return this;
        }

        public Builder birthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Builder birthPlace(String birthPlace) {
            this.birthPlace = birthPlace;
            return this;
        }

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder additionalData(Map<String, Object> additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public ProfileEntity build() {
            return new ProfileEntity(id, tenantId, user, fullName, nik, birthDate,
                    birthPlace, gender, address, additionalData);
        }
    }
}
