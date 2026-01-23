package id.payu.partner.dto.snap;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class PaymentStatusResponse {
    @JsonProperty("responseCode")
    public String responseCode;
    
    @JsonProperty("responseMessage")
    public String responseMessage;
    
    @JsonProperty("partnerReferenceNo")
    public String partnerReferenceNo;
    
    @JsonProperty("referenceNo")
    public String referenceNo;
    
    @JsonProperty("amount")
    public Amount amount;
    
    @JsonProperty("beneficiaryAccountNo")
    public String beneficiaryAccountNo;
    
    @JsonProperty("status")
    public String status;
    
    @JsonProperty("transactionTimestamp")
    public String transactionTimestamp;

    public PaymentStatusResponse() {}

    public PaymentStatusResponse(String responseCode, String responseMessage, String partnerReferenceNo, 
                                String referenceNo, BigDecimal amountValue, String currency,
                                String status, String beneficiaryAccountNo, String transactionTimestamp) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.partnerReferenceNo = partnerReferenceNo;
        this.referenceNo = referenceNo;
        if (amountValue != null && currency != null) {
            this.amount = new Amount(amountValue, currency);
        }
        this.beneficiaryAccountNo = beneficiaryAccountNo;
        this.status = status;
        this.transactionTimestamp = transactionTimestamp;
    }

    public static class Amount {
        @JsonProperty("value")
        public BigDecimal value;
        
        @JsonProperty("currency")
        public String currency;

        public Amount() {}

        public Amount(BigDecimal value, String currency) {
            this.value = value;
            this.currency = currency;
        }
    }
}
