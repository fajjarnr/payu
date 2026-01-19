package id.payu.transaction.adapter.client;

import id.payu.transaction.domain.port.out.BifastServicePort;
import id.payu.transaction.dto.BifastTransferRequest;
import id.payu.transaction.dto.BifastTransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class BifastServiceAdapter implements BifastServicePort {

    @Value("${services.bifast.url:http://localhost:9000}")
    private String bifastServiceUrl;

    private final RestTemplate restTemplate;

    @Override
    public BifastTransferResponse initiateTransfer(BifastTransferRequest request) {
        String url = bifastServiceUrl + "/v1/bifast/transfer";
        return restTemplate.postForObject(url, request, BifastTransferResponse.class);
    }

    @Override
    public BifastTransferResponse checkStatus(String referenceNumber) {
        String url = bifastServiceUrl + "/v1/bifast/status/" + referenceNumber;
        return restTemplate.getForObject(url, BifastTransferResponse.class);
    }
}
