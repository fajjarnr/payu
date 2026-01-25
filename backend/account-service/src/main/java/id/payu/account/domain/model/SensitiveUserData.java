package id.payu.account.domain.model;

import jakarta.persistence.*;
import jakarta.persistence.Convert;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Example entity with field-level encryption for sensitive PII data
 */
@Entity
@Table(name = "sensitive_user_data")
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
    @Convert(converter = AddressDataConverter.class)
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

    // Constructors
    public SensitiveUserData() {
    }

    public SensitiveUserData(Long id, Long userId, String nik, String taxId,
                            String motherMaidenName, AddressData address,
                            String phonePrimary, String phoneSecondary,
                            java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.nik = nik;
        this.taxId = taxId;
        this.motherMaidenName = motherMaidenName;
        this.address = address;
        this.phonePrimary = phonePrimary;
        this.phoneSecondary = phoneSecondary;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNik() {
        return nik;
    }

    public void setNik(String nik) {
        this.nik = nik;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getMotherMaidenName() {
        return motherMaidenName;
    }

    public void setMotherMaidenName(String motherMaidenName) {
        this.motherMaidenName = motherMaidenName;
    }

    public AddressData getAddress() {
        return address;
    }

    public void setAddress(AddressData address) {
        this.address = address;
    }

    public String getPhonePrimary() {
        return phonePrimary;
    }

    public void setPhonePrimary(String phonePrimary) {
        this.phonePrimary = phonePrimary;
    }

    public String getPhoneSecondary() {
        return phoneSecondary;
    }

    public void setPhoneSecondary(String phoneSecondary) {
        this.phoneSecondary = phoneSecondary;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private String nik;
        private String taxId;
        private String motherMaidenName;
        private AddressData address;
        private String phonePrimary;
        private String phoneSecondary;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder nik(String nik) {
            this.nik = nik;
            return this;
        }

        public Builder taxId(String taxId) {
            this.taxId = taxId;
            return this;
        }

        public Builder motherMaidenName(String motherMaidenName) {
            this.motherMaidenName = motherMaidenName;
            return this;
        }

        public Builder address(AddressData address) {
            this.address = address;
            return this;
        }

        public Builder phonePrimary(String phonePrimary) {
            this.phonePrimary = phonePrimary;
            return this;
        }

        public Builder phoneSecondary(String phoneSecondary) {
            this.phoneSecondary = phoneSecondary;
            return this;
        }

        public Builder createdAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(java.time.LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public SensitiveUserData build() {
            return new SensitiveUserData(id, userId, nik, taxId, motherMaidenName,
                    address, phonePrimary, phoneSecondary, createdAt, updatedAt);
        }
    }

    /**
     * Address data stored as JSONB
     */
    public static class AddressData {
        private String street;
        private String city;
        private String state;
        private String postalCode;
        private String country;

        public AddressData() {
        }

        public AddressData(String street, String city, String state,
                          String postalCode, String country) {
            this.street = street;
            this.city = city;
            this.state = state;
            this.postalCode = postalCode;
            this.country = country;
        }

        // Getters and Setters
        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }
}
