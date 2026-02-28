package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Value object representing a calculated split amount for a stakeholder.
 */
public class CalculatedSplit {

    private UUID stakeholderId;
    private String accountId;
    private String name;
    private BigDecimal amount;

    public CalculatedSplit() {
    }

    public CalculatedSplit(UUID stakeholderId, String accountId, String name, BigDecimal amount) {
        this.stakeholderId = stakeholderId;
        this.accountId = accountId;
        this.name = name;
        this.amount = amount;
    }

    // Getters and Setters
    public UUID getStakeholderId() { return stakeholderId; }
    public void setStakeholderId(UUID stakeholderId) { this.stakeholderId = stakeholderId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
