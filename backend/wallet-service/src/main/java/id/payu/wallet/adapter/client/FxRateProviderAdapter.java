package id.payu.wallet.adapter.client;

import id.payu.wallet.domain.model.FxRateInfo;
import id.payu.wallet.domain.port.out.FxRateProviderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class FxRateProviderAdapter implements FxRateProviderPort {

    private final RestTemplate restTemplate;
    private final String fxServiceUrl;

    public FxRateProviderAdapter(RestTemplate restTemplate,
                                @Value("${fx.service.url:http://localhost:8086/fx-api}") String fxServiceUrl) {
        this.restTemplate = restTemplate;
        this.fxServiceUrl = fxServiceUrl;
    }

    @Override
    public Optional<FxRateInfo> getCurrentRate(String fromCurrency, String toCurrency) {
        try {
            String url = fxServiceUrl + "/v1/rates/" + fromCurrency + "/" + toCurrency;
            id.payu.wallet.adapter.client.FxRateResponse response = restTemplate.getForObject(
                    url, id.payu.wallet.adapter.client.FxRateResponse.class);

            if (response == null) {
                return Optional.empty();
            }

            var info = new FxRateInfo(
                    response.getId(),
                    response.getFromCurrency(),
                    response.getToCurrency(),
                    response.getRate(),
                    response.getInverseRate(),
                    response.getValidFrom(),
                    response.getValidUntil()
            );

            return Optional.of(info);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
