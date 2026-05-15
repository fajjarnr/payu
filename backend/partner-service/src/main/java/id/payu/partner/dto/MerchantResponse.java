package id.payu.partner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "MerchantEntity details")
public class MerchantResponse {

    private Long id;
    private String merchantCode;
    private String businessName;
    private String businessType;
    private String category;
    private String address;
    private String city;
    private String postalCode;
    private String picName;
    private String picPhone;
    private String picEmail;
    private String settlementAccountId;
    private String status;
    private String staticQrCode;
    private LocalDateTime createdAt;

    public MerchantResponse() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String merchantCode) { this.merchantCode = merchantCode; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStaticQrCode() { return staticQrCode; }
    public void setStaticQrCode(String staticQrCode) { this.staticQrCode = staticQrCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
