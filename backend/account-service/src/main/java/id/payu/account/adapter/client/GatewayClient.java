package id.payu.account.adapter.client;

import id.payu.account.dto.VerifyNikRequest;
import id.payu.account.dto.VerifyNikResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for communicating with Dukcapil simulator through API Gateway.
 * Provides circuit breaker and retry capabilities through resilience patterns.
 */
@FeignClient(
    name = "gateway-client",
    url = "${payu.gateway.url:http://localhost:8080}"
)
public interface GatewayClient {

    /**
     * Verify NIK with Dukcapil simulator.
     * Returns detailed citizen data for comprehensive verification.
     *
     * @param request the verification request
     * @return detailed verification response
     */
    @PostMapping("/api/v1/simulator/dukcapil/verify")
    VerifyNikResponse verifyNik(@RequestBody VerifyNikRequest request);
}
