package id.payu.billing.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPlan {
    private UUID id;
    private String partnerId;
    private String planName;
    private String description;
    private BillingInterval billingInterval;
    private BigDecimal price;
    private String currency;
    private int trialDays;
    private int gracePeriodDays;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String tenantId;
    private Long version;

    public void deactivate() {
        active = false;
        updatedAt = LocalDateTime.now();
    }
}
