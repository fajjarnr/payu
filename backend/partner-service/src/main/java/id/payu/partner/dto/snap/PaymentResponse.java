package id.payu.partner.dto.snap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentResponse {
    @JsonProperty("responseCode")
    public String responseCode;
    
    @JsonProperty("responseMessage")
    public String responseMessage;
    
    @JsonProperty("partnerReferenceNo")
    public String partnerReferenceNo;
    
    @JsonProperty("referenceNo")
    public String referenceNo;
    
    @JsonProperty("additionalInfo")
    public Object additionalInfo;
    
    public PaymentResponse() {}

    public PaymentResponse(String responseCode, String responseMessage, String partnerReferenceNo, String referenceNo) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.partnerReferenceNo = partnerReferenceNo;
        this.referenceNo = referenceNo;
    }
}
