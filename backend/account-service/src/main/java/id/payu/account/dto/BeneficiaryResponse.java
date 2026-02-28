package id.payu.account.dto;

import id.payu.account.entity.Beneficiary;
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
public class BeneficiaryResponse {

    private UUID id;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String nickname;
    private String status;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;

    public static BeneficiaryResponse from(Beneficiary beneficiary) {
        if (beneficiary == null) {
            return null;
        }
        return BeneficiaryResponse.builder()
                .id(beneficiary.getId())
                .bankCode(beneficiary.getBankCode())
                .accountNumber(beneficiary.getAccountNumber())
                .accountName(beneficiary.getAccountName())
                .nickname(beneficiary.getNickname())
                .status(beneficiary.getStatus() != null ? beneficiary.getStatus().name() : null)
                .verifiedAt(beneficiary.getVerifiedAt())
                .createdAt(beneficiary.getCreatedAt())
                .build();
    }
}
