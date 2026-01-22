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

    public PartnerDTO() {
    }

    public PartnerDTO(Long id, String name, String type, String email, String phone, boolean active) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.email = email;
        this.phone = phone;
        this.active = active;
    }
}
