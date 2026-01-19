package id.payu.transaction.adapter.client;

import id.payu.transaction.domain.port.out.QrisServicePort;
import id.payu.transaction.dto.QrisPaymentRequest;
import id.payu.transaction.dto.QrisPaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class QrisServiceAdapter implements QrisServicePort {

    @Value("${services.qris.url:http://localhost:9001}")
    private String qrisServiceUrl;

    private final RestTemplate restTemplate;

    @Override
    public QrisPaymentResponse processPayment(QrisPaymentRequest request) {
        String url = qrisServiceUrl + "/v1/qris/pay";
        return restTemplate.postForObject(url, request, QrisPaymentResponse.class);
    }

    @Override
    public QrisPaymentResponse checkStatus(String transactionId) {
        String url = qrisServiceUrl + "/v1/qris/status/" + transactionId;
        return restTemplate.getForObject(url, QrisPaymentResponse.class);
    }
}
