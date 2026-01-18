package id.payu.simulator.dukcapil.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents Indonesian citizen data from Dukcapil.
 * NIK (Nomor Induk Kependudukan) is the primary identifier.
 */
@Entity
@Table(name = "citizens")
public class Citizen extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "nik", nullable = false, unique = true, length = 16)
    public String nik;

    @Column(name = "full_name", nullable = false, length = 100)
    public String fullName;

    @Column(name = "birth_place", length = 50)
    public String birthPlace;

    @Column(name = "birth_date")
    public LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    public Gender gender;

    @Column(name = "blood_type", length = 5)
    public String bloodType;

    @Column(name = "address", length = 255)
    public String address;

    @Column(name = "rt", length = 5)
    public String rt;

    @Column(name = "rw", length = 5)
    public String rw;

    @Column(name = "village", length = 50)
    public String village;

    @Column(name = "district", length = 50)
    public String district;

    @Column(name = "city", length = 50)
    public String city;

    @Column(name = "province", length = 50)
    public String province;

    @Enumerated(EnumType.STRING)
    @Column(name = "religion")
    public Religion religion;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status")
    public MaritalStatus maritalStatus;

    @Column(name = "occupation", length = 50)
    public String occupation;

    @Column(name = "nationality", length = 10)
    public String nationality = "WNI";

    @Column(name = "photo_hash", length = 64)
    public String photoHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public CitizenStatus status = CitizenStatus.VALID;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    public enum Gender {
        MALE, FEMALE
    }

    public enum Religion {
        ISLAM, KRISTEN, KATOLIK, HINDU, BUDDHA, KONGHUCU, OTHER
    }

    public enum MaritalStatus {
        SINGLE, MARRIED, DIVORCED, WIDOWED
    }

    public enum CitizenStatus {
        VALID,
        INVALID,
        BLOCKED,
        DECEASED,
        NOT_FOUND
    }

    // Finder methods
    public static Citizen findByNik(String nik) {
        return find("nik", nik).firstResult();
    }

    public static boolean existsByNik(String nik) {
        return count("nik", nik) > 0;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
