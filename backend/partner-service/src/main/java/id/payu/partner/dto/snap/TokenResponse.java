package id.payu.partner.dto.snap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenResponse {
    @JsonProperty("accessToken")
    public String accessToken;
    
    @JsonProperty("tokenType")
    public String tokenType;
    
    @JsonProperty("expiresIn")
    public String expiresIn;
    
    @JsonProperty("additionalInfo")
    public Object additionalInfo;
    
    public TokenResponse() {}

    public TokenResponse(String accessToken, String tokenType, String expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }
}
