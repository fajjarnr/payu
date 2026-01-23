package id.payu.transaction.adapter.client;

import id.payu.transaction.domain.port.out.RgsServicePort;
import id.payu.transaction.dto.RgsTransferRequest;
import id.payu.transaction.dto.RgsTransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class RgsServiceAdapter implements RgsServicePort {

    @Value("${services.rgs.url:http://localhost:9002}")
    private String rgsServiceUrl;

    private final RestTemplate restTemplate;

    @Override
    public RgsTransferResponse initiateTransfer(RgsTransferRequest request) {
        String url = rgsServiceUrl + "/v1/rgs/transfer";
        return restTemplate.postForObject(url, request, RgsTransferResponse.class);
    }

    @Override
    public RgsTransferResponse checkStatus(String referenceNumber) {
        String url = rgsServiceUrl + "/v1/rgs/status/" + referenceNumber;
        return restTemplate.getForObject(url, RgsTransferResponse.class);
    }
}
