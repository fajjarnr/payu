package id.payu.simulator.biller.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a customer account at a biller (e.g., PLN customer, PDAM subscriber).
 */
@Entity
@Table(name = "biller_accounts")
public class BillerAccount extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "biller_code", nullable = false)
    public String billerCode;

    @Column(name = "customer_number", nullable = false)
    public String customerNumber;

    @Column(name = "customer_name", nullable = false)
    public String customerName;

    @Column(name = "outstanding_amount")
    public BigDecimal outstandingAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AccountStatus status;

    public static BillerAccount findByBillerAndCustomer(String billerCode, String customerNumber) {
        return find("billerCode = ?1 and customerNumber = ?2", billerCode, customerNumber).firstResult();
    }
}
