package id.payu.partner.dto.snap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenRequest {
    @JsonProperty("grantType")
    public String grantType;
    
    @JsonProperty("additionalInfo")
    public Object additionalInfo;
}
