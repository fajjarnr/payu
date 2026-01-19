package id.payu.transaction.adapter.client;

import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.dto.ReserveBalanceRequest;
import id.payu.transaction.dto.ReserveBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalletServiceAdapter implements WalletServicePort {

    @Value("${services.wallet.url:http://localhost:8004}")
    private String walletServiceUrl;

    private final RestTemplate restTemplate;

    @Override
    public ReserveBalanceResponse reserveBalance(UUID accountId, String transactionId, BigDecimal amount) {
        String url = walletServiceUrl + "/v1/wallet/reserve";
        ReserveBalanceRequest request = ReserveBalanceRequest.builder()
                .transactionId(transactionId)
                .amount(amount)
                .build();
        return restTemplate.postForObject(url, request, ReserveBalanceResponse.class);
    }

    @Override
    public void commitBalance(UUID accountId, String transactionId, BigDecimal amount) {
        String url = walletServiceUrl + "/v1/wallet/commit";
        ReserveBalanceRequest request = ReserveBalanceRequest.builder()
                .transactionId(transactionId)
                .amount(amount)
                .build();
        restTemplate.postForObject(url, request, Void.class);
    }

    @Override
    public void releaseBalance(UUID accountId, String transactionId, BigDecimal amount) {
        String url = walletServiceUrl + "/v1/wallet/release";
        ReserveBalanceRequest request = ReserveBalanceRequest.builder()
                .transactionId(transactionId)
                .amount(amount)
                .build();
        restTemplate.postForObject(url, request, Void.class);
    }
}
