package id.payu.partner.dto.snap;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class PaymentRequest {
    @JsonProperty("partnerReferenceNo")
    public String partnerReferenceNo;
    
    @JsonProperty("amount")
    public Amount amount;
    
    @JsonProperty("beneficiaryAccountNo")
    public String beneficiaryAccountNo;
    
    @JsonProperty("beneficiaryBankCode")
    public String beneficiaryBankCode;
    
    @JsonProperty("sourceAccountNo")
    public String sourceAccountNo;
    
    @JsonProperty("additionalInfo")
    public Object additionalInfo;

    public static class Amount {
        @JsonProperty("value")
        public BigDecimal value;
        
        @JsonProperty("currency")
        public String currency;
    }
}
