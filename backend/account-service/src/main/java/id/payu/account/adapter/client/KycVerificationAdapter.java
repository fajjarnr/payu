package id.payu.account.adapter.client;

import id.payu.account.domain.port.out.KycVerificationPort;
import id.payu.account.dto.DukcapilResponse;
import id.payu.account.dto.VerifyNikRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KycVerificationAdapter implements KycVerificationPort {
    
    private final GatewayClient gatewayClient;

    @Override
    public DukcapilResponse verifyNik(String nik, String fullName) {
         VerifyNikRequest request = new VerifyNikRequest(
             nik,
             fullName,
             "UNKNOWN",
             "2000-01-01"
         );
         return gatewayClient.verifyNik(request);
    }
}
