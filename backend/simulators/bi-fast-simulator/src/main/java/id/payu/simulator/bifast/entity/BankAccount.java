package id.payu.simulator.bifast.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Represents a test bank account for BI-FAST simulation.
 */
@Entity
@Table(name = "bank_accounts")
public class BankAccount extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "bank_code", nullable = false, length = 10)
    public String bankCode;

    @Column(name = "account_number", nullable = false, length = 20)
    public String accountNumber;

    @Column(name = "account_name", nullable = false, length = 100)
    public String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    // Finder methods
    public static BankAccount findByBankAndAccount(String bankCode, String accountNumber) {
        return find("bankCode = ?1 and accountNumber = ?2", canonicalBankCode(bankCode), accountNumber).firstResult();
    }

    public static String canonicalBankCode(String bankCode) {
        if (bankCode == null) {
            return null;
        }
        return switch (bankCode.trim().toUpperCase(Locale.ROOT)) {
            case "002" -> "BRI";
            case "008" -> "MANDIRI";
            case "009" -> "BNI";
            case "011" -> "DANAMON";
            case "013" -> "PERMATA";
            case "014" -> "BCA";
            case "022" -> "CIMB";
            case "028" -> "OCBC";
            default -> bankCode.trim().toUpperCase(Locale.ROOT);
        };
    }

    public static long countByBankCode(String bankCode) {
        return count("bankCode", bankCode);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
