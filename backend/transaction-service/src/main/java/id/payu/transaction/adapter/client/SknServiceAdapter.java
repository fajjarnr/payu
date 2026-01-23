package id.payu.transaction.adapter.client;

import id.payu.transaction.domain.port.out.SknServicePort;
import id.payu.transaction.dto.SknTransferRequest;
import id.payu.transaction.dto.SknTransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class SknServiceAdapter implements SknServicePort {

    @Value("${services.skn.url:http://localhost:9001}")
    private String sknServiceUrl;

    private final RestTemplate restTemplate;

    @Override
    public SknTransferResponse initiateTransfer(SknTransferRequest request) {
        String url = sknServiceUrl + "/v1/skn/transfer";
        return restTemplate.postForObject(url, request, SknTransferResponse.class);
    }

    @Override
    public SknTransferResponse checkStatus(String referenceNumber) {
        String url = sknServiceUrl + "/v1/skn/status/" + referenceNumber;
        return restTemplate.getForObject(url, SknTransferResponse.class);
    }
}
