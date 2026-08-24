package id.payu.partner.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartnerDTO {
    
    public Long id;
    
    @NotBlank
    public String name;
    
    @NotBlank
    @JsonAlias("partnerType")
    public String type;
    
    @NotBlank
    @Email
    public String email;
    
    public String phone;
    
    public boolean active;

    public String clientId;
    public String clientSecret;
    public String publicKey;

    // ADR-0035 dual-control fields (read-only from API)
    public String status;
    public String makerId;
    public String checkerId;
    public Instant requestedAt;
    public Instant decidedAt;
    public String rejectionReason;

    public PartnerDTO() {
    }

    public PartnerDTO(Long id, String name, String type, String email, String phone, boolean active, String clientId, String clientSecret, String publicKey) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.email = email;
        this.phone = phone;
        this.active = active;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.publicKey = publicKey;
    }
}
