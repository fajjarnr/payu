package id.payu.wallet.dto;

import jakarta.validation.constraints.*;

public class GetLedgerEntriesRequest {
    
    @NotNull(message = "Account ID is required")
    private String accountId;
    
    private Integer page = 0;
    
    @Min(value = 0, message = "Page cannot be negative")
    private Integer size = 20;

    public GetLedgerEntriesRequest() {
    }

    public GetLedgerEntriesRequest(String accountId, Integer page, Integer size) {
        this.accountId = accountId;
        this.page = page;
        this.size = size;
    }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
