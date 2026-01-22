package id.payu.partner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class PartnerDTO {
    
    public Long id;
    
    @NotBlank
    public String name;
    
    @NotBlank
    public String type;
    
    @NotBlank
    @Email
    public String email;
    
    public String phone;
    
    public boolean active;

    public String clientId;
    public String clientSecret;
    public String publicKey;

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
