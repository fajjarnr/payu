package id.payu.partner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payment link details")
public class PaymentLinkResponse {

    @Schema(description = "Payment link ID")
    private Long id;

    @Schema(description = "Unique slug for the payment link URL")
    private String slug;

    @Schema(description = "Full payment URL", example = "https://pay.payu.fajjjar.my.id/pay/abc123")
    private String paymentUrl;

    @Schema(description = "Payment amount")
    private BigDecimal amount;

    @Schema(description = "Currency code")
    private String currency;

    @Schema(description = "Payment description")
    private String description;

    @Schema(description = "Payment link status")
    private String status;

    @Schema(description = "Customer name")
    private String customerName;

    @Schema(description = "Customer email")
    private String customerEmail;

    @Schema(description = "PartnerEntity's external reference ID")
    private String externalId;

    @Schema(description = "Redirect URL after payment")
    private String redirectUrl;

    @Schema(description = "Payment method used (if paid)")
    private String paymentMethod;

    @Schema(description = "Payment reference (if paid)")
    private String paymentReference;

    @Schema(description = "Timestamp when payment was made")
    private LocalDateTime paidAt;

    @Schema(description = "Expiry timestamp")
    private LocalDateTime expiresAt;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    public PaymentLinkResponse() {
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
