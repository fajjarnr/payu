package id.payu.transaction.dto;

import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request to create a Virtual Account for payment collection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVirtualAccountRequest {

    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotNull(message = "Partner ID is required")
    private UUID partnerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000", message = "Minimum amount is 1000")
    private BigDecimal amount;

    private String currency;

    private String description;

    @Sensitive
    private String customerName;

    @Sensitive
    private String customerEmail;

    @Sensitive
    private String customerPhone;

    private String externalId;

    private String callbackUrl;

    /** Expiry in hours (default: 24) */
    private Integer expiryHours;
}
