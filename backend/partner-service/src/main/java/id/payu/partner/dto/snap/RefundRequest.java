package id.payu.partner.dto.snap;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class RefundRequest {
    @JsonProperty("partnerRefundNo")
    public String partnerRefundNo;
    
    @JsonProperty("amount")
    public Amount amount;
    
    @JsonProperty("reason")
    public String reason;

    public static class Amount {
        @JsonProperty("value")
        public BigDecimal value;
        
        @JsonProperty("currency")
        public String currency;
    }
}
