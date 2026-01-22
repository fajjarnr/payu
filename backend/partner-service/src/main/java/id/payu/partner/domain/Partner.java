package id.payu.partner.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "partners")
public class Partner extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    public String name;

    @NotBlank
    public String type; // e.g., "MERCHANT", "BANK", "PAYMENT_GATEWAY"

    @NotBlank
    @Email
    public String email;

    public String phone;

    public String apiKey;

    public boolean active;

    @CreationTimestamp
    public LocalDateTime createdAt;

    @UpdateTimestamp
    public LocalDateTime updatedAt;

    public Partner() {
    }

    public Partner(String name, String type, String email, String phone, String apiKey) {
        this.name = name;
        this.type = type;
        this.email = email;
        this.phone = phone;
        this.apiKey = apiKey;
        this.active = true;
    }
}
