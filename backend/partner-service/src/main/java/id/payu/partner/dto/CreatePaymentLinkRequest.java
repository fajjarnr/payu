package id.payu.partner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to create a payment link")
public class CreatePaymentLinkRequest {

    @NotNull
    @DecimalMin(value = "1000", message = "Minimum amount is 1000")
    @Schema(description = "Payment amount", example = "150000.00")
    private BigDecimal amount;

    @Schema(description = "Currency code (default: IDR)", example = "IDR")
    private String currency = "IDR";

    @NotBlank
    @Size(max = 500)
    @Schema(description = "Payment description", example = "Invoice #INV-2026-001")
    private String description;

    @Schema(description = "Customer name", example = "John Doe")
    private String customerName;

    @Schema(description = "Customer email for receipt", example = "john@example.com")
    private String customerEmail;

    @Schema(description = "Partner's external reference ID", example = "order-123")
    private String externalId;

    @Schema(description = "Webhook callback URL for payment status", example = "https://partner.com/webhook")
    private String callbackUrl;

    @Schema(description = "Redirect URL after payment", example = "https://partner.com/success")
    private String redirectUrl;

    @Schema(description = "Expiry duration in hours (default: 24)", example = "24")
    private Integer expiryHours = 24;

    public CreatePaymentLinkRequest() {
    }

    // Getters and setters

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }

    public Integer getExpiryHours() { return expiryHours; }
    public void setExpiryHours(Integer expiryHours) { this.expiryHours = expiryHours; }
}
