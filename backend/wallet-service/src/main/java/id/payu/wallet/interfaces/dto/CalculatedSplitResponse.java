package id.payu.wallet.interfaces.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for calculated split.
 */
public class CalculatedSplitResponse {

    private UUID stakeholderId;
    private String accountId;
    private String name;
    private BigDecimal amount;

    public UUID getStakeholderId() {
        return stakeholderId;
    }

    public void setStakeholderId(UUID stakeholderId) {
        this.stakeholderId = stakeholderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
