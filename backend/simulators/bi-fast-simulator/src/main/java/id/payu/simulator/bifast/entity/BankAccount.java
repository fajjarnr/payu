package id.payu.simulator.bifast.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
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

    public enum AccountStatus {
        ACTIVE,
        BLOCKED,
        DORMANT,
        TIMEOUT  // Special status to simulate timeout scenarios
    }

    // Finder methods
    public static BankAccount findByBankAndAccount(String bankCode, String accountNumber) {
        return find("bankCode = ?1 and accountNumber = ?2", bankCode, accountNumber).firstResult();
    }

    public static long countByBankCode(String bankCode) {
        return count("bankCode", bankCode);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
