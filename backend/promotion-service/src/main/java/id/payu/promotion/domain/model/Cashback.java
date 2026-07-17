package id.payu.promotion.domain.model;

import id.payu.promotion.domain.CashbackStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Cashback {
    private UUID id; private String accountId; private String transactionId;
    private BigDecimal cashbackAmount; private BigDecimal transactionAmount; private BigDecimal percentage;
    private String merchantCode; private String categoryCode; private String cashbackCode;
    private CashbackStatus status; private LocalDateTime creditedAt; private LocalDateTime expiryDate; private LocalDateTime createdAt;
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public String getAccountId(){return accountId;} public void setAccountId(String v){accountId=v;}
    public String getTransactionId(){return transactionId;} public void setTransactionId(String v){transactionId=v;}
    public BigDecimal getCashbackAmount(){return cashbackAmount;} public void setCashbackAmount(BigDecimal v){cashbackAmount=v;}
    public BigDecimal getTransactionAmount(){return transactionAmount;} public void setTransactionAmount(BigDecimal v){transactionAmount=v;}
    public BigDecimal getPercentage(){return percentage;} public void setPercentage(BigDecimal v){percentage=v;}
    public String getMerchantCode(){return merchantCode;} public void setMerchantCode(String v){merchantCode=v;}
    public String getCategoryCode(){return categoryCode;} public void setCategoryCode(String v){categoryCode=v;}
    public String getCashbackCode(){return cashbackCode;} public void setCashbackCode(String v){cashbackCode=v;}
    public CashbackStatus getStatus(){return status;} public void setStatus(CashbackStatus v){status=v;}
    public LocalDateTime getCreditedAt(){return creditedAt;} public void setCreditedAt(LocalDateTime v){creditedAt=v;}
    public LocalDateTime getExpiryDate(){return expiryDate;} public void setExpiryDate(LocalDateTime v){expiryDate=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}
