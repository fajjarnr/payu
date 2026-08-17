package id.payu.account.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneLookupResponse {

    private String accountName;
    private String maskedAccountNumber;
    private boolean found;
    private String message;

    public static PhoneLookupResponse found(String accountName, String maskedAccountNumber) {
        return PhoneLookupResponse.builder()
                .accountName(accountName)
                .maskedAccountNumber(maskedAccountNumber)
                .found(true)
                .message("AccountEntity found")
                .build();
    }

    public static PhoneLookupResponse notFound() {
        return PhoneLookupResponse.builder()
                .accountName(null)
                .maskedAccountNumber(null)
                .found(false)
                .message("AccountEntity not found for this phone number")
                .build();
    }
}
