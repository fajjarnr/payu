package id.payu.account.entity;

import id.payu.account.multitenancy.TenantAware;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@TenantAware
public class Profile {

    @Id
    private UUID id; // Same as User ID

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "nik", unique = true, length = 16)
    private String nik;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "birth_place")
    private String birthPlace;

    @Column(name = "gender")
    private String gender;

    @Column(name = "address")
    private String address;

    @Type(JsonType.class)
    @Column(name = "additional_data", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> additionalData = new HashMap<>();
}
