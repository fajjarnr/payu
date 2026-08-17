package id.payu.account.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryRequest {

    @NotBlank(message = "Bank code is required")
    @Size(max = 10, message = "Bank code must not exceed 10 characters")
    private String bankCode;

    @NotBlank(message = "AccountEntity number is required")
    @Size(min = 10, max = 20, message = "AccountEntity number must be between 10 and 20 characters")
    @Pattern(regexp = "^[0-9]+$", message = "AccountEntity number must contain only digits")
    private String accountNumber;

    @Size(max = 100, message = "Nickname must not exceed 100 characters")
    private String nickname;
}
