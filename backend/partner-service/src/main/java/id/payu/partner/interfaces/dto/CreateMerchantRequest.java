package id.payu.partner.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to onboard a new merchant")
public class CreateMerchantRequest {

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Business name", example = "Warung Kopi Nusantara")
    private String businessName;

    @Schema(description = "Business type", example = "F&B")
    private String businessType;

    @NotNull
    @Schema(description = "MerchantEntity category", example = "FOOD_BEVERAGE")
    private String category;

    @NotBlank
    @Size(max = 300)
    @Schema(description = "Business address", example = "Jl. Sudirman No. 1, Jakarta")
    private String address;

    @Schema(description = "City", example = "Jakarta")
    private String city;

    @Schema(description = "Postal code", example = "12190")
    private String postalCode;

    @Schema(description = "Contact person name", example = "Budi Santoso")
    private String picName;

    @Schema(description = "Contact person phone", example = "08123456789")
    private String picPhone;

    @Schema(description = "Contact person email", example = "budi@warungkopi.com")
    private String picEmail;

    @Schema(description = "Settlement wallet/account ID", example = "acc-123")
    private String settlementAccountId;

    public CreateMerchantRequest() {
    }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getPicName() { return picName; }
    public void setPicName(String picName) { this.picName = picName; }

    public String getPicPhone() { return picPhone; }
    public void setPicPhone(String picPhone) { this.picPhone = picPhone; }

    public String getPicEmail() { return picEmail; }
    public void setPicEmail(String picEmail) { this.picEmail = picEmail; }

    public String getSettlementAccountId() { return settlementAccountId; }
    public void setSettlementAccountId(String settlementAccountId) { this.settlementAccountId = settlementAccountId; }
}
