package id.payu.partner.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to generate a dynamic QR code for payment")
public class CreateQrPaymentRequest {

    @NotNull
    @DecimalMin(value = "100", message = "Minimum amount is 100")
    @Schema(description = "Payment amount", example = "50000.00")
    private BigDecimal amount;

    @Schema(description = "Currency code (default: IDR)", example = "IDR")
    private String currency = "IDR";

    @Schema(description = "Payment description", example = "Kopi Susu 2x")
    private String description;

    @Schema(description = "Expiry in minutes (default: 30)", example = "30")
    private Integer expiryMinutes = 30;

    public CreateQrPaymentRequest() {
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getExpiryMinutes() { return expiryMinutes; }
    public void setExpiryMinutes(Integer expiryMinutes) { this.expiryMinutes = expiryMinutes; }
}
