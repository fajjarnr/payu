package id.payu.account.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beneficiary {
    private UUID id;
    private UUID userId;
    private String tenantId;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String nickname;
    private BeneficiaryStatus status;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
