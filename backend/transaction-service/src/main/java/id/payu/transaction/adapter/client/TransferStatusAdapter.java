package id.payu.transaction.adapter.client;

import id.payu.transaction.domain.port.out.TransferStatusPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class TransferStatusAdapter implements TransferStatusPort {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public TransferStatusAdapter(RestTemplate restTemplate,
                                 @Value("${payu.bifast.base-url:http://localhost:9000}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getLatestTransactionStatus(String referenceNo) {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(baseUrl + "/snap/v1.0/transfer/status?referenceNo=" + referenceNo, Map.class);
            if (resp.getBody() != null) {
                Object status = resp.getBody().get("latestTransactionStatus");
                if (status != null) return status.toString();
                Object txStatus = resp.getBody().get("transactionStatus");
                if (txStatus != null) return txStatus.toString();
            }
            return "01"; // PENDING fallback
        } catch (Exception e) {
            return "01";
        }
    }
}
