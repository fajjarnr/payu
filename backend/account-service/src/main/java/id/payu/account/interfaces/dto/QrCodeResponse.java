package id.payu.account.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeResponse {

    private String qrData;
    private String qrImageBase64;
    private String accountId;
    private String expiresAt;
}
