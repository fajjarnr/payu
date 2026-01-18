package id.payu.simulator.qris.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a QRIS merchant.
 */
@Entity
@Table(name = "merchants")
public class Merchant extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "merchant_id", nullable = false, unique = true, length = 20)
    public String merchantId;

    @Column(name = "merchant_name", nullable = false, length = 100)
    public String merchantName;

    @Column(name = "merchant_city", length = 50)
    public String merchantCity;

    @Enumerated(EnumType.STRING)
    @Column(name = "merchant_category")
    public MerchantCategory merchantCategory;

    @Column(name = "nmid", length = 20)
    public String nmid; // National Merchant ID

    @Column(name = "terminal_id", length = 20)
    public String terminalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public MerchantStatus status = MerchantStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public enum MerchantCategory {
        FOOD_BEVERAGE,
        RETAIL,
        ELECTRONICS,
        FASHION,
        HEALTH,
        TRANSPORT,
        ENTERTAINMENT,
        UTILITIES,
        OTHER
    }

    public enum MerchantStatus {
        ACTIVE,
        INACTIVE,
        BLOCKED,
        SUSPENDED
    }

    // Finder methods
    public static Merchant findByMerchantId(String merchantId) {
        return find("merchantId", merchantId).firstResult();
    }
}
