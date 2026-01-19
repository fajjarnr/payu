package id.payu.account.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private UUID id;
    private String externalId;
    private String username;
    private String email;
    private String phoneNumber;
    private String fullName;
    private String nik;
    private UserStatus status;
    private KycStatus kycStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum UserStatus {
        ACTIVE, LOCKED, SUSPENDED, PENDING_VERIFICATION
    }

    public enum KycStatus {
        NOT_STARTED, PENDING, APPROVED, REJECTED
    }
}
